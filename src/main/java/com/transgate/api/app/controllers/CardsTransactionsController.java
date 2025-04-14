/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.CardsTransactionsInterface;
import com.transgate.api.interfaces.UsersInterface;
import com.transgate.api.models.CardsDisputeModel;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.app.services.Validators;
import java.util.Optional;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Makintola
 */
@RestController
public class CardsTransactionsController {
    
    @Autowired
    private UsersInterface usersInterface;
    
    @Autowired
    private CardsTransactionsInterface CardsTransactionsInterface;
    
    ResponseManager responseManager = new ResponseManager();
    private Logger logger = Logger.getLogger(CardsTransactionsController.class.getName());
    private final Validators validators;
    // Constructor injection for RestCall
    public CardsTransactionsController(Validators validators) {
        this.validators = validators;
    }
    
    @RequestMapping(value = "/cards/transactions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.Get();
    }
    
    @RequestMapping(value = "/cards/transactions-by-date", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.Get(startDate, endDate, page, limit, isCurrent);
    }
    
    @RequestMapping(value = "/cards/transactions/institution/{institution}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByFI(@RequestHeader(value = "Authorization") String header,
            @PathVariable("institution") String institution,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByFI(institution, page, limit);
    }
    
    @RequestMapping(value = "/cards/transactions-by-date/institution/{institution}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByFI(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @PathVariable("institution") String institution,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByFI(institution, startDate, endDate, page, limit, isCurrent);
    }
    
    @RequestMapping(value = "/cards/transactions/merchant/{merchant}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByMerchant(@RequestHeader(value = "Authorization") String header,
            @PathVariable("merchant") String merchant,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByMerchant(merchant, page, limit);
    }
    
    @RequestMapping(value = "/cards/transactions-by-date/merchant/{merchant}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByMerchant(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @PathVariable("merchant") String merchant,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByMerchant(merchant, startDate, endDate, page, limit, isCurrent);
    }
    
    @RequestMapping(value = "/cards/transactions/terminal/{terminal}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByTerminal(@RequestHeader(value = "Authorization") String header,
            @PathVariable("terminal") String terminal,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByTerminal(terminal, page, limit);
    }
    
    @RequestMapping(value = "/cards/transactions/ptsp/{ptsp}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByPTSP(@RequestHeader(value = "Authorization") String header,
            @PathVariable("ptsp") String ptsp,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByPTSP(ptsp, page, limit);
    }
    
    @RequestMapping(value = "/cards/transactions-by-date/ptsp/{ptsp}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByPTSP(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @PathVariable("ptsp") String ptsp,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByPTSP(ptsp, startDate, endDate, page, limit, isCurrent);
    }
    
    @RequestMapping(value = "/cards/transactions/terminal-owner/{owner}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByTerminalOwner(@RequestHeader(value = "Authorization") String header,
            @PathVariable("owner") String owner,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByTerminalOwner(owner, page, limit);
    }
    
    @RequestMapping(value = "/cards/transactions-by-date/terminal-owner/{owner}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByTerminalOwner(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @PathVariable("owner") String owner,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetByTerminalOwner(owner, startDate, endDate, page, limit, isCurrent);
    }
    
@RequestMapping(value = "/cards/transactions/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
public ResponseEntity SearchTransactions(
        @RequestHeader(value = "Authorization") String header, 
        @RequestHeader(value = "auth-token") String sessiontoken,  
        @RequestParam("message_type") String message_type, 
        @RequestParam("bin") String bin, 
        @RequestParam("processing_code") String processing_code, 
        @RequestParam("system_trace_number") String system_trace_number, 
        @RequestParam("response_code") String response_code, 
        @RequestParam("min_amount") String min_amount, 
        @RequestParam("max_amount") String max_amount, 
        @RequestParam("start_date") String start_date, 
        @RequestParam("end_date") String end_date,
        @RequestParam("page") int page,
        @RequestParam("limit") int limit,
        @RequestParam("retrieval_ref_number") String retrieval_ref_number, 
        @RequestParam("acquirer_institution_id") String acquirer_institution_id, 
        @RequestParam("destination_acquiring_institution_id") String destination_acquiring_institution_id, 
        @RequestParam("pan") String pan, 
        @RequestParam("rrn") String rrn, 
        @RequestParam("terminal_id") String terminal_id, 
        @RequestParam("merchant_id") String merchant_id, 
        @RequestParam("location_name_address") String location_name_address,  
        @RequestParam("approval_code") String approval_code,  
        @RequestParam("isCurrent") boolean isCurrent 
) {
    logger.info("Entered SearchTransactions method for /cards/transactions/q/search");
    
    // Validate the Authorization header.
    logger.info("Validating Authorization header...");
    if (!validators.validHeader().equals(header)) {
        logger.info("Invalid Authorization header detected. Returning error response.");
        return responseManager.InvalidAuthorizationHeader();
    }
    
    logger.info("Authorization header is valid.");
    
    // Log incoming parameters (adjust if any are considered sensitive).
    logger.info("Request parameters: "
            + "message_type=" + message_type + ", "
            + "bin=" + bin + ", "
            + "processing_code=" + processing_code + ", "
            + "system_trace_number=" + system_trace_number + ", "
            + "response_code=" + response_code + ", "
            + "min_amount=" + min_amount + ", "
            + "max_amount=" + max_amount + ", "
            + "start_date=" + start_date + ", "
            + "end_date=" + end_date + ", "
            + "page=" + page + ", "
            + "limit=" + limit + ", "
            + "retrieval_ref_number=" + retrieval_ref_number + ", "
            + "acquirer_institution_id=" + acquirer_institution_id + ", "
            + "destination_acquiring_institution_id=" + destination_acquiring_institution_id + ", "
            + "pan=" + pan + ", "
            + "rrn=" + rrn + ", "
            + "terminal_id=" + terminal_id + ", "
            + "merchant_id=" + merchant_id + ", "
            + "location_name_address=" + location_name_address + ", "
            + "approval_code=" + approval_code + ", "
            + "isCurrent=" + isCurrent);
    
    // Log that we are about to call the downstream service.
    logger.info("Calling CardsTransactionsInterface.SearchTransactions with provided parameters.");
    
    ResponseEntity downstreamResponse = CardsTransactionsInterface.SearchTransactions(
            message_type,
            bin,
            processing_code,
            min_amount,
            max_amount,
            system_trace_number,
            response_code,
            start_date,
            end_date,
            retrieval_ref_number,
            acquirer_institution_id,
            destination_acquiring_institution_id,
            pan,
            rrn,
            terminal_id,
            merchant_id,
            location_name_address,
            approval_code,
            page,
            limit,
            isCurrent);
    
    logger.info("Exiting SearchTransactions method for /cards/transactions/q/search");
    return downstreamResponse;
}

    
//    @RequestMapping(value = "/cards/transactions/disputes/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
//    public ResponseEntity SearchTransactionsDisputes(
//            @RequestHeader(value = "Authorization") String header, 
//            @RequestHeader(value = "auth-token") Optional<String> sessiontoken,  
//            @RequestParam("system_trace_number") Optional<String> system_trace_number, 
//            @RequestParam("retrieval_ref_number") Optional<String> retrieval_ref_number, 
//            @RequestParam("terminal_id") String terminal_id,
//            @RequestParam("transaction_response_code") Optional<String> transaction_response_code,
//            @RequestParam("date_logged_range") Optional<String> date_logged_range,
//            @RequestParam("date_resolved_range") Optional<String> date_resolved_range,
//            @RequestParam("dispute_status") Optional<String> dispute_status
//    ) {
//        if (!validators.validHeader().equals(header)) {
//            return responseManager.InvalidAuthorizationHeader();
//        }
//        return CardsTransactionsInterface.SearchDisputes(terminal_id,
//            system_trace_number.orElse(""),
//            retrieval_ref_number.orElse(""),
//            transaction_response_code.orElse(""),
//            dispute_status.orElse(""),
//            date_logged_range.orElse(""),
//            date_resolved_range.orElse("")
//        );
//    }
    
    @RequestMapping(value = "/cards/transactions/disputes/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchTransactionsDisputesForMerchants(
            @RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") Optional<String> sessiontoken,  
            @RequestParam("system_trace_number") Optional<String> system_trace_number, 
            @RequestParam("retrieval_ref_number") Optional<String> retrieval_ref_number, 
            @RequestParam("terminal_id") String terminal_id,
            @RequestParam("transaction_response_code") Optional<String> transaction_response_code,
            @RequestParam("date_logged_range") Optional<String> date_logged_range,
            @RequestParam("date_resolved_range") Optional<String> date_resolved_range,
            @RequestParam("timeline_date_range") Optional<String> timeline_date_range,
            @RequestParam("dispute_status") Optional<String> dispute_status,
            @RequestParam("dispute_type") Optional<String> dispute_type,
            @RequestParam("merchantsasIds") Optional<String> merchantsasIds,
            @RequestParam("pan") Optional<String> pan,
            @RequestParam("uniquelogid") Optional<String> uniquelogid,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.SearchDisputes(terminal_id,
            system_trace_number.orElse(""),
            retrieval_ref_number.orElse(""),
            transaction_response_code.orElse(""),
            dispute_status.orElse(""),
            dispute_type.orElse(""),
            date_logged_range.orElse(""),
            date_resolved_range.orElse(""),
            timeline_date_range.orElse(""),
            merchantsasIds.orElse(""),
            pan.orElse(""),
            uniquelogid.orElse(""),
            page,
            limit
        );
    }
    
    @RequestMapping(value = "/cards/transactions/disputes/log", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity CreateDisputeExternal(@RequestHeader(value = "auth") String header, 
            @RequestHeader(value = "Authorization") String token, 
            @RequestHeader(value = "username") String username,
            @RequestBody CardsDisputeModel dispute) {
        if (validators.validHeaderExternal().equals(header) || usersInterface.LoginExternal(username, header.split(" ")[1])) {
            if (!validators.ValidateJSONWebToken(token, username)) {
                return responseManager.ResponseUnathorized();
            }
            return CardsTransactionsInterface.LogDispute(
                    token, 
                    dispute.getTerminal_id(), 
                    dispute.getRetrieval_ref_number(), 
                    dispute.getSystem_trace_number(), 
                    dispute.getProof_of_debit_uri(), 
                    dispute.getLogged_by(), 
                    true
            );
        }
        return responseManager.ResponseUnathorized();
    }
    
    @RequestMapping(value = "/cards/transactions/disputes/{uniqueid}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetOneDisputeExternal(@RequestHeader(value = "auth") String header, 
            @RequestHeader(value = "Authorization") String token,
            @RequestHeader(value = "username") String username,
            @PathVariable ("uniqueid") String uniqueid) {
        if (validators.validHeaderExternal().equals(header) || usersInterface.LoginExternal(username, header.split(" ")[1])) {
            if (!validators.ValidateJSONWebToken(token, username)) {
                return responseManager.ResponseUnathorized();
            }
            return CardsTransactionsInterface.GetOneDispute(uniqueid);
        }
        return responseManager.ResponseUnathorized();
    }
    
    @RequestMapping(value = "/cards/transactions/disputes", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputesExternal(@RequestHeader(value = "auth") String header, 
            @RequestHeader(value = "Authorization") String token,
            @RequestHeader(value = "username") String username,
            @RequestParam("search") String search) {
        if (validators.validHeaderExternal().equals(header) || usersInterface.LoginExternal(username, header.split(" ")[1])) {
            if (!validators.ValidateJSONWebToken(token, username)) {
                return responseManager.ResponseUnathorized();
            }
            return CardsTransactionsInterface.GetDisputes(search);
        }
        return responseManager.ResponseUnathorized();
    }
    
    @RequestMapping(value = "/cards/transactions/disputes/create", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity CreateDispute(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody CardsDisputeModel dispute) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.LogDispute(sessiontoken, dispute.getTerminal_id(), dispute.getRetrieval_ref_number(), dispute.getSystem_trace_number(), dispute.getProof_of_debit_uri(), dispute.getLogged_by(), false);
    }
    
    @RequestMapping(value = "/cards/transactions/disputes/create/bulk", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity CreateBulkDisputes(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody CardsDisputeModel dispute) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.LogDisputesBulk(sessiontoken, dispute.getRecords(), dispute.getLogged_by());
    }
    
    @RequestMapping(value = "/cards/transactions/arbitrated-disputes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetArbitratedDisputes(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetArbitratedDisputes(institutioncode);
    }
    
    @RequestMapping(value = "/cards/transactions/disputes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputes(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetDisputes(institutioncode, startDate, endDate, page, limit);
    }
    
    @RequestMapping(value = "/cards/transactions/disputes/merchant/{merchantid}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputesByMerchant(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("merchantid") String merchantid,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.GetDisputesByMerchant(merchantid, startDate, endDate, page, limit);
    }
    
    @RequestMapping(value = "/cards/transactions/disputes/approve", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity ApproveCardsSettlement(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody CardsDisputeModel dispute) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.ApproveSettlement(sessiontoken, dispute.getId(), dispute.getStatus(), dispute.getProof_of_reject_uri(), dispute.getSelectedDisputes(), dispute.getType(), dispute.getResolved_by());
    }
    
    @RequestMapping(value = "/app/crons/cards/disputes/update-nuban", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity UpdateCardsDisputesNUBAN() {
        return CardsTransactionsInterface.UpdateCardsDisputesNUBAN();
    }
    
    @RequestMapping(value = "/app/crons/cards/disputes/update-dispute-data", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity UpdateDisputesData() {
        return CardsTransactionsInterface.UpdateDisputesData();
    }
}