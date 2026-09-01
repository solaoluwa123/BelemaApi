/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transgate.api.interfaces.TransactionsInterface;
import com.transgate.api.models.DisputeModel;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.SessionActorResolver;
import com.transgate.api.app.services.Validators;
import com.transgate.api.app.services.LiveTransactionStreamHub;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 *
 * @author Makintola
 */
@RestController
public class TransactionsController {

    @Autowired
    private TransactionsInterface transactionsInterface;

    @Autowired
    private SessionActorResolver sessionActorResolver;

    @Autowired
    private LiveTransactionStreamHub liveTransactionStreamHub;

    ResponseManager responseManager = new ResponseManager();

    private final Validators validators;
    private Logger logger = Logger.getLogger(TransactionsInterface.class.getName());

    // Constructor injection for RestCall
    public TransactionsController(Validators validators) {
        this.validators = validators;
    }

    /**
     * For Third Party Vendor (role 4), force the caller's own FI code.
     * Returns empty Optional when the caller is not a vendor.
     * Returns a ResponseEntity error when the vendor has no FI linked.
     */
    private Optional<ResponseEntity> vendorInstitutionGate(String sessiontoken, String requestedCode) {
        Optional<SessionActorResolver.Actor> actorOpt = sessionActorResolver.resolve(sessiontoken);
        if (actorOpt.isEmpty() || !actorOpt.get().isThirdPartyVendor()) {
            return Optional.empty();
        }
        SessionActorResolver.Actor actor = actorOpt.get();
        if (!actor.hasInstitutionCode()) {
            return Optional.of(responseManager.ResponseBadRequest(
                    "Your account is not linked to an institution."));
        }
        String mine = actor.institutionCode().trim();
        if (requestedCode != null && !requestedCode.isBlank() && !mine.equals(requestedCode.trim())) {
            return Optional.of(responseManager.ResponseForbidden(
                    "Third Party Vendors may only access transactions for their own institution."));
        }
        return Optional.empty();
    }

    private String vendorInstitutionOrNull(String sessiontoken) {
        return sessionActorResolver.resolve(sessiontoken)
                .filter(SessionActorResolver.Actor::isThirdPartyVendor)
                .filter(SessionActorResolver.Actor::hasInstitutionCode)
                .map(a -> a.institutionCode().trim())
                .orElse(null);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Accepts legacy {@code srcSessionid} CSV plus FE shapes {@code sessionIds} / {@code records}.
     */
    private String resolveSessionIdsCsv(JsonNode body) {
        if (body == null || body.isNull() || body.isMissingNode()) {
            return null;
        }
        for (String key : new String[]{"srcSessionid", "srcSessionId", "sessionid", "sessionId"}) {
            if (body.hasNonNull(key) && body.get(key).isTextual()) {
                String text = body.get(key).asText().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        JsonNode idsNode = body.get("sessionIds");
        if (idsNode == null) {
            idsNode = body.get("sessionids");
        }
        if (idsNode != null && idsNode.isArray()) {
            List<String> ids = new ArrayList<>();
            for (JsonNode n : idsNode) {
                if (n != null && !n.isNull()) {
                    String text = n.asText("").trim();
                    if (!text.isEmpty()) {
                        ids.add(text);
                    }
                }
            }
            if (!ids.isEmpty()) {
                return ids.stream().collect(Collectors.joining(","));
            }
        }
        if (body.hasNonNull("records")) {
            JsonNode records = body.get("records");
            if (records.isArray()) {
                List<String> ids = new ArrayList<>();
                for (JsonNode n : records) {
                    String text = n.asText("").trim();
                    if (!text.isEmpty()) {
                        ids.add(text);
                    }
                }
                if (!ids.isEmpty()) {
                    return String.join(",", ids);
                }
            } else if (records.isTextual()) {
                String raw = records.asText().trim();
                if (raw.startsWith("[")) {
                    try {
                        JsonNode arr = objectMapper.readTree(raw);
                        if (arr.isArray()) {
                            List<String> ids = new ArrayList<>();
                            for (JsonNode n : arr) {
                                String text = n.asText("").trim();
                                if (!text.isEmpty()) {
                                    ids.add(text);
                                }
                            }
                            if (!ids.isEmpty()) {
                                return String.join(",", ids);
                            }
                        }
                    } catch (Exception ignored) {
                        // fall through to raw CSV
                    }
                }
                if (!raw.isEmpty()) {
                    return raw;
                }
            }
        }
        return null;
    }

    private ResponseEntity missingSessionIds() {
        return responseManager.ResponseBadRequest("No valid session IDs provided.");
    }

    @RequestMapping(value = "/transactions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        // Role 4 → GET /transactions/institution/{theirCode}
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            logger.info("Routing Third Party Vendor to institution transactions: " + vendorCode);
            return transactionsInterface.Get(vendorCode);
        }
        Optional<SessionActorResolver.Actor> actorOpt = sessionActorResolver.resolve(sessiontoken);
        if (actorOpt.isPresent() && actorOpt.get().isThirdPartyVendor()) {
            return responseManager.ResponseBadRequest("Your account is not linked to an institution.");
        }
        return transactionsInterface.Get();
    }

    @RequestMapping(value = "/transactions-by-date", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {

        logger.info("Received GET /transactions-by-date request with parameters: "
                + "startDate=" + startDate + ", endDate=" + endDate
                + ", page=" + page + ", limit=" + limit + ", isCurrent=" + isCurrent);

        try {
            if (!validators.validHeader().equals(header)) {
                return responseManager.InvalidAuthorizationHeader();
            }

            String vendorCode = vendorInstitutionOrNull(sessiontoken);
            if (vendorCode != null) {
                logger.info("Routing Third Party Vendor /transactions-by-date to institution " + vendorCode);
                return transactionsInterface.Get(vendorCode, startDate, endDate, page, limit, isCurrent);
            }
            Optional<SessionActorResolver.Actor> actorOpt = sessionActorResolver.resolve(sessiontoken);
            if (actorOpt.isPresent() && actorOpt.get().isThirdPartyVendor()) {
                return responseManager.ResponseBadRequest("Your account is not linked to an institution.");
            }

            return transactionsInterface.Get(startDate, endDate, page, limit, isCurrent);
        } catch (Exception ex) {
            logger.info("Exception occurred while processing /transactions-by-date: " + ex.getMessage());
            throw ex;
        }
    }
    
    
    private ResponseEntity vendorMissingInstitutionOrNull(String sessiontoken) {
        Optional<SessionActorResolver.Actor> actorOpt = sessionActorResolver.resolve(sessiontoken);
        if (actorOpt.isPresent() && actorOpt.get().isThirdPartyVendor() && !actorOpt.get().hasInstitutionCode()) {
            return responseManager.ResponseBadRequest("Your account is not linked to an institution.");
        }
        return null;
    }

    @RequestMapping(value = "/transactions-by-date-only", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity getTransactionsByDateOnly(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {

        logger.info("Received GET /transactions-by-date-only");
        try {
            if (!validators.validHeader().equals(header)) {
                return responseManager.InvalidAuthorizationHeader();
            }
            String vendorCode = vendorInstitutionOrNull(sessiontoken);
            if (vendorCode != null) {
                return transactionsInterface.getInstitutionTransactionsByDateOnly(
                        vendorCode, startDate, endDate, page, limit, isCurrent);
            }
            ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
            if (missing != null) {
                return missing;
            }
            return transactionsInterface.getTransactionsByDateOnly(startDate, endDate, page, limit, isCurrent);
        } catch (Exception ex) {
            logger.info("Exception occurred while processing /transactions-by-date-only: " + ex.getMessage());
            throw ex;
        }
    }

    @RequestMapping(value = "/transactions-summary", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolume(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetTransactionsVolume(vendorCode, startDate, endDate);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetTransactionsVolume(startDate, endDate);
    }

    @RequestMapping(value = "/successful-transaction-count", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSuccessTNXVolume(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetSuccessTNXVolume(vendorCode, startDate, endDate);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetSuccessTNXVolume(startDate, endDate);
    }

    @RequestMapping(value = "/successful-transaction-count/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSuccessTNXVolumeInstitution(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetSuccessTNXVolume(code, startDate, endDate);
    }

    @RequestMapping(value = "/ft-average-time", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetFTTimeAverage(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetFTTimeAverage(vendorCode, startDate, endDate, isCurrent);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetFTTimeAverage(startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/ft-average-time/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetFTTimeAverageInstitution(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetFTTimeAverage(code, startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/top-failed-response-codes", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTop6ResponseCodesTNX(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetTop6ResponseCodesTNX(vendorCode, startDate, endDate, isCurrent);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetTop6ResponseCodesTNX(startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/top-failing-institutions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetFailedTnxCountByInstitutions(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetFailedTnxCountByInstitutions(vendorCode, startDate, endDate, isCurrent);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetFailedTnxCountByInstitutions(startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/top-failing-institutions/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetFailedTnxCountByInstitutionsByInstitution(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetFailedTnxCountByInstitutions(code, startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/top-failed-response-codes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTop6ResponseCodesTNXInstitution(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetTop6ResponseCodesTNX(code, startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/all-failed-response-codes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetAllResponseCodesTNXInstitution(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetAllResponseCodesTNXInstitution(code, startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/transactions-by-channels", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolumeByChannels(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetTransactionsVolumeByChannels(vendorCode, startDate, endDate, isCurrent);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetTransactionsVolumeByChannels(startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/transactions-by-channels/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolumeByChannelsInstitution(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetTransactionsVolumeByChannels(code, startDate, endDate, isCurrent);
    }

    @RequestMapping(value = "/transactions-summary/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolumeInstitution(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetTransactionsVolume(code, startDate, endDate);
    }

    @RequestMapping(value = "/transactions-rates", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsRates(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("institution") Optional<String> institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetTransactionsRates(startDate, endDate, false, vendorCode);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetTransactionsRates(startDate, endDate, false, institution.orElse(""));
    }

    @RequestMapping(value = "/transactions-rates/inward", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsRatesInward(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("institution") Optional<String> institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetTransactionsRates(startDate, endDate, true, vendorCode);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetTransactionsRates(startDate, endDate, true, institution.orElse(""));
    }

    @RequestMapping(value = "/transactions-rates/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsRates(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetTransactionsRates(code, startDate, endDate, false);
    }

    @RequestMapping(value = "/transactions-rates/inward/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsRatesInward(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetTransactionsRates(code, startDate, endDate, true);
    }

    @RequestMapping(value = "/transactions/live-monitoring", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetLiveMonitoring(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("institution") Optional<String> institution,
            @RequestParam(value = "bucketMinutes", defaultValue = "10") int bucketMinutes,
            @RequestParam(value = "limit", defaultValue = "8") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetLiveMonitoring(startDate, endDate, vendorCode, bucketMinutes, limit);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetLiveMonitoring(startDate, endDate, institution.orElse(""), bucketMinutes, limit);
    }

    @RequestMapping(value = "/transactions/live-feed", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetLiveTransactionFeed(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam(value = "since", required = false) String since,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam("institution") Optional<String> institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institution.orElse(null));
        if (denied.isPresent()) {
            return denied.get();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetLiveTransactionFeed(since, limit, vendorCode);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetLiveTransactionFeed(since, limit, institution.orElse(""));
    }

    @RequestMapping(value = "/transactions/live-stream", method = RequestMethod.GET, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter GetLiveTransactionStream(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam(value = "institution", required = false) Optional<String> institution) {
        if (!validators.validHeader().equals(header)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header");
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institution.orElse(null));
        if (denied.isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return liveTransactionStreamHub.subscribe(vendorCode);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Institution required");
        }
        return liveTransactionStreamHub.subscribe(institution.orElse(""));
    }

    @RequestMapping(value = "/transactions/status-summary", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetStatusSummary(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "isCurrent", defaultValue = "true") boolean isCurrent,
            @RequestParam("institution") Optional<String> institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetStatusSummary(startDate, endDate, isCurrent, vendorCode);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetStatusSummary(startDate, endDate, isCurrent, institution.orElse(""));
    }

    @RequestMapping(value = "/transactions/dashboard-compare", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDashboardCompare(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "isCurrent", defaultValue = "true") boolean isCurrent,
            @RequestParam("institution") Optional<String> institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            return transactionsInterface.GetDashboardCompare(startDate, endDate, isCurrent, vendorCode);
        }
        ResponseEntity missing = vendorMissingInstitutionOrNull(sessiontoken);
        if (missing != null) {
            return missing;
        }
        return transactionsInterface.GetDashboardCompare(startDate, endDate, isCurrent, institution.orElse(""));
    }

    @RequestMapping(value = "/transactions-trend/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetInsitutionTnxTrend(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String code = Optional.ofNullable(vendorInstitutionOrNull(sessiontoken)).orElse(institutioncode);
        return transactionsInterface.GetInsitutionTnxTrend(code, type, startDate, endDate);
    }

    @RequestMapping(value = "/transactions/{sessionid}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetOne(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable("sessionid") String sessionid) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetBySessionId(sessionid);
    }

    @RequestMapping(value = "/transactions-by-session-id/{sessionid}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetOneBySessionId(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("sessionid") String sessionid,
            @RequestParam("isCurrent") boolean isCurrent
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetBySessionId(sessionid, isCurrent);
    }

    @RequestMapping(value = "/transactions/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetInstitutionTransactions(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        String code = vendorCode != null ? vendorCode : institutioncode;
        return transactionsInterface.Get(code);
    }

    @RequestMapping(value = "/transactions-by-session-ids", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity SearchTransactionsForSessionIds(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody JsonNode body) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String sessionIds = resolveSessionIdsCsv(body);
        if (sessionIds == null || sessionIds.isBlank()) {
            return missingSessionIds();
        }
        return transactionsInterface.SearchTransactionsForSessionIds(sessionIds);
    }

    @RequestMapping(value = "/transactions-by-session-ids/with/date", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity SearchTransactionsForSessionIdsWithDate(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestBody JsonNode body) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String sessionIds = resolveSessionIdsCsv(body);
        if (sessionIds == null || sessionIds.isBlank()) {
            return missingSessionIds();
        }
        return transactionsInterface.SearchTransactionsForSessionIds(sessionIds, startDate, endDate);
    }

    @RequestMapping(value = "/transactions-by-date/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetInstitutionTransactions(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        String code = vendorCode != null ? vendorCode : institutioncode;
        return transactionsInterface.Get(code, startDate, endDate, page, limit, isCurrent);
    }
    
    @RequestMapping(value = "/transactions-by-date-only/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity getInstitutionTransactionsByDateOnly(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, institutioncode);
        if (denied.isPresent()) {
            return denied.get();
        }
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        String code = vendorCode != null ? vendorCode : institutioncode;
        return transactionsInterface.getInstitutionTransactionsByDateOnly(code, startDate, endDate, page, limit, isCurrent);
    }

    @RequestMapping(value = "/transactions/disputes/create", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody DisputeModel dispute) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.LogDispute(sessiontoken, dispute.getSrcSessionid(), dispute.getSrcAmount(), dispute.getSrcAccountNumber(), dispute.getSrcInstitutioncode(), dispute.getType(), dispute.getUsername());
    }

    @RequestMapping(value = "/transactions/disputes/create/bulk", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity CreateBulkDisputes(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody DisputeModel dispute) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.LogDisputesBulk(sessiontoken, dispute.getRecords(), dispute.getSrcInstitutioncode(), dispute.getUsername());
    }

    @RequestMapping(value = "/transactions/disputes/approve", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity ApproveSettlement(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody DisputeModel dispute) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.ApproveSettlement(sessiontoken, dispute.getId(), dispute.getUsername(), dispute.getStatus(), dispute.getProof_of_reject_uri(), dispute.getSelectedDisputes(), dispute.getType());
    }

    @RequestMapping(value = "/transactions/disputes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputes(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetDisputes(institutioncode, page, limit);
    }

    @RequestMapping(value = "/transactions/arbitrated-disputes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetArbitratedDisputes(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetArbitratedDisputes(institutioncode);
    }

    @RequestMapping(value = "/transactions/disputes/get/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputesOne(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetDisputes(id);
    }

    @RequestMapping(value = "/transactions/disputes/types/get", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputeTypes(@RequestHeader(value = "Authorization") String header) {
        return transactionsInterface.GetDisputeTypes();
    }

    @RequestMapping(value = "/transactions/settlements/get/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSettlementsOne(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetSettlements(id);
    }

    @RequestMapping(value = "/transactions/settlements/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSettlements(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetSettlements(institutioncode);
    }

    @RequestMapping(value = "/transactions/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchTransactions(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token", required = false) String sessiontoken,
            @RequestParam(value = "srcSessionid", required = false, defaultValue = "") String srcSessionid,
            @RequestParam(value = "channelCode", required = false, defaultValue = "") String channelCode,
            @RequestParam(value = "responseCode", required = false, defaultValue = "") String responseCode,
            @RequestParam(value = "srcAccountNumber", required = false, defaultValue = "") String srcAccountNumber,
            @RequestParam(value = "destAccountNumber", required = false, defaultValue = "") String destAccountNumber,
            @RequestParam(value = "srcInstitutioncode", required = false, defaultValue = "") String srcInstitutioncode,
            @RequestParam(value = "destInstitutioncode", required = false, defaultValue = "") String destInstitutioncode,
            @RequestParam(value = "minAmount", required = false, defaultValue = "") String minAmount,
            @RequestParam(value = "maxAmount", required = false, defaultValue = "") String maxAmount,
            @RequestParam(value = "startDate", required = false, defaultValue = "") String startDate,
            @RequestParam(value = "endDate", required = false, defaultValue = "") String endDate,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(value = "isCurrent", required = false, defaultValue = "true") boolean isCurrent,
            @RequestParam(value = "userInstitutionCode", required = false, defaultValue = "") String userInstitutionCode
    ) {
        // Log entry into the method without logging sensitive data
        logger.info("SearchTransactions called with parameters: srcSessionid=" + srcSessionid
                + ", channelCode=" + channelCode
                + ", responseCode=" + responseCode
                + ", srcInstitutioncode=" + srcInstitutioncode
                + ", destInstitutioncode=" + destInstitutioncode
                + ", minAmount=" + minAmount
                + ", maxAmount=" + maxAmount
                + ", srcAccountNumber=" + srcAccountNumber
                + ", destAccountNumber=" + destAccountNumber
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + ", page=" + page
                + ", limit=" + limit
                + ", isCurrent=" + isCurrent
                + ", userInstitutionCode=" + userInstitutionCode);

        // Optional: Log that header verification is in progress
        logger.info("Verifying Authorization header");

        // Validate header
        if (!validators.validHeader().equals(header)) {
            logger.warning("Invalid Authorization header received");
            return responseManager.InvalidAuthorizationHeader();
        }

        String scopedInstitution = userInstitutionCode;
        String vendorCode = vendorInstitutionOrNull(sessiontoken);
        if (vendorCode != null) {
            Optional<ResponseEntity> denied = vendorInstitutionGate(sessiontoken, userInstitutionCode);
            if (denied.isPresent()) {
                return denied.get();
            }
            scopedInstitution = vendorCode;
            logger.info("Forcing Third Party Vendor search scope to institution " + vendorCode);
        } else {
            Optional<SessionActorResolver.Actor> actorOpt = sessionActorResolver.resolve(sessiontoken);
            if (actorOpt.isPresent() && actorOpt.get().isThirdPartyVendor()) {
                return responseManager.ResponseBadRequest("Your account is not linked to an institution.");
            }
        }

        logger.info("Header validated. Proceeding to search transactions.");

        ResponseEntity response = transactionsInterface.SearchTransactions(
                srcSessionid,
                channelCode,
                responseCode,
                srcInstitutioncode,
                destInstitutioncode,
                minAmount,
                maxAmount,
                srcAccountNumber.replaceAll("space", " "),
                destAccountNumber.replaceAll("space", " "),
                startDate,
                endDate,
                page,
                limit,
                isCurrent,
                scopedInstitution);

        logger.info("SearchTransactions completed. Returning response.");
        return response;
    }

    @RequestMapping(value = "/transactions/disputes/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchTransactionsDisputes(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestParam("sessionid") Optional<String> sessionid,
            @RequestParam("response_code") Optional<String> response_code,
            @RequestParam("source_bank") Optional<String> source_bank,
            @RequestParam("beneficiary_bank") Optional<String> beneficiary_bank,
            @RequestParam("date_logged_range") Optional<String> date_logged_range,
            @RequestParam("date_resolved_range") Optional<String> date_resolved_range,
            @RequestParam("timeline_date_range") Optional<String> timeline_date_range,
            @RequestParam("dispute_status") Optional<String> dispute_status,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.SearchDisputes(
                sessionid.orElse(""),
                response_code.orElse(""),
                source_bank.orElse(""),
                beneficiary_bank.orElse(""),
                dispute_status.orElse(""),
                date_logged_range.orElse(""),
                date_resolved_range.orElse(""),
                timeline_date_range.orElse(""),
                page,
                limit
        );
    }

    @RequestMapping(value = "/commissions/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetCommissions(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetCommissions(institutioncode, startDate, endDate);
    }

    @RequestMapping(value = "/timeoutretries-by-date", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTimeoutRetries(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTimeoutRetries(startDate, endDate, page, limit);
    }

    @RequestMapping(value = "/timeoutretries/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchTimeoutRetries(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestParam("session_id") String session_id,
            @RequestParam("response_at_reprocess") String response_at_reprocess,
            @RequestParam("destination_institution_code") String destination_institution_code,
            @RequestParam("isProcessed") String isProcessed,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.SearchTimeoutRetries(session_id,
                response_at_reprocess,
                destination_institution_code,
                startDate,
                endDate,
                page,
                limit,
                isProcessed);
    }

    @RequestMapping(value = "/tsq-retries", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTsqRetries(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestParam(value = "session_id", required = false, defaultValue = "") String session_id,
            @RequestParam(value = "destination_institution_code", required = false, defaultValue = "") String destination_institution_code,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTsqRetries(session_id, destination_institution_code, page, limit);
    }

    @RequestMapping(value = "/tsq-retries/{sessionId}/reset-counter", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity ResetTsqRetryCounter(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("sessionId") String sessionId,
            @RequestParam("username") String username) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.ResetTsqRetryCounter(sessiontoken, username, sessionId);
    }

    @RequestMapping(value = "/transaction/status/change", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity RequestTransactionStatusChange(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody DisputeModel model) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        // New response code lives on srcResponsecode (not narration — narration is the admin reason).
        String newStatusCode = model.getSrcResponsecode();
        if (newStatusCode == null || newStatusCode.trim().isEmpty()) {
            newStatusCode = model.getDestResponseCode();
        }
        if (newStatusCode == null || newStatusCode.trim().isEmpty()) {
            newStatusCode = model.getType();
        }
        return transactionsInterface.RequestTransactionStatusChange(
                model.getSrcSessionid(),
                sessiontoken,
                model.getUsername(),
                newStatusCode == null ? "" : newStatusCode.trim());
    }

//    @RequestMapping(value = "/transaction/status/change/update", method = RequestMethod.POST, headers = "Accept=application/json")
//    public ResponseEntity ApproveTransactionStatusChange(@RequestHeader(value = "Authorization") String header,
//            @RequestHeader(value = "auth-token") String sessiontoken,
//            @RequestBody DisputeModel model) {
//        if (!validators.validHeader().equals(header)) {
//            return responseManager.InvalidAuthorizationHeader();
//        }
//        return transactionsInterface.UpdateTransactionStatusChange(model.getSrcSessionid(), sessiontoken, model.getUsername(), model.getNarration());
//    }
}
