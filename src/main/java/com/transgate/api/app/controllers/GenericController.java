/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.util.ResponseManager;
import com.transgate.api.app.services.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.transgate.api.interfaces.GenericInterface;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Makintola
 */
@RestController
public class GenericController {
    @Autowired
    private GenericInterface GenericInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    private Logger logger = Logger.getLogger(GenericInterface.class.getName());
    private final Validators validators;
    // Constructor injection for RestCall
    public GenericController(Validators validators) {
        this.validators = validators;
    }
    
    @RequestMapping(value = "/cards/banks", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetBanks(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetBanks();
    }
    
    @RequestMapping(value = "/cards/skr", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSKR(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSKR();
    }
    
    @RequestMapping(value = "/cards/tnxdirection", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionDirection(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetTransactionDirection();
    }
    
    @RequestMapping(value = "/cards/states", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetStates(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetStates();
    }
    
    @RequestMapping(value = "/cards/response-codes", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetResponseCodes(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetResponseCodes();
    }
    
    @RequestMapping(value = "/cards/terminal-types", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTerminalTypes(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetTerminalTypes();
    }
    
    @RequestMapping(value = "/settlements", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSettlements(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, 
            @RequestParam("end") String end,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSettlements(start, end, page, limit);
    }
    
    @RequestMapping(value = "/smartdets", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSmartDets(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSmartDets(start, end);
    }
    
    @RequestMapping(value = "/settlementsummary", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSettlementSummary(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSettlementSummary(start, end);
    }
    
    @RequestMapping(value = "/cards/settlementsummary", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetCardsSettlementSummary(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetCardsSettlementSummary(start, end);
    }
    
    @RequestMapping(value = "/settlements/institution/{institution}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSettlements(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, 
            @RequestParam("end") String end, 
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @PathVariable("institution") String institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSettlements(institution, start, end, page, limit);
    }
    
    @RequestMapping(value = "/settlements/institution/search/{institution}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchSettlementsByInstitution(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, 
            @RequestParam("end") String end, 
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @PathVariable("institution") String institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.SearchSettlementsByInstitution(institution, start, end, page, limit);
    }
    
    @RequestMapping(value = "/cards/settlements", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetCardsSettlements(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetCardsSettlements(start, end);
    }
    
    @RequestMapping(value = "/cards/settlements/acq/{acq}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetCardsSettlementsByAcquirer(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end, @PathVariable("acq") String acq) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetCardsSettlementsByAcquirer(acq, start, end);
    }
    
    @RequestMapping(value = "/cards/settlements/iss/{iss}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetCardsSettlements(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end, @PathVariable("iss") String iss) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetCardsSettlementsByIssuer(iss, start, end);
    }
    
    @RequestMapping(value = "/cards/settlements/merchant/{merchant}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSettlementsByMerchant(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end, @PathVariable("merchant") String merchant) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSettlementsByMerchant(merchant, start, end);
    }
    
    @RequestMapping(value = "/cards/settlements/ptsp/{ptsp}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetCardsSettlementsByPTSP(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end, @PathVariable("ptsp") String ptsp) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetCardsSettlementsByPTSP(ptsp, start, end);
    }
    
    @RequestMapping(value = "/cardpayments/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetCardPayments(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate, 
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("response_code") String response_code, 
            @RequestParam("transaction_id") String transaction_id, 
            @RequestParam("merchant_name") String merchant_name
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetCardPayments(institutioncode, startDate, endDate, page, limit, response_code, transaction_id, merchant_name.replaceAll("space", " "));
    }
    
    @RequestMapping(value = "/nuspayments/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetCardNUSPayments(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate, 
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("response_code") String response_code, 
            @RequestParam("transaction_id") String transaction_id, 
            @RequestParam("merchant_name") String merchant_name
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetCardNUSPayments(institutioncode, startDate, endDate, page, limit, response_code, transaction_id, merchant_name.replaceAll("space", " "));
    }
    
    @RequestMapping(value = "/gapspayments", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetGapsPayments(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate, 
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isSettled") String isSettled, 
            @RequestParam("merchant_id") String merchant_id, 
            @RequestParam("reference") String reference
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetGapsPayments(merchant_id, startDate, endDate, page, limit, isSettled, reference);
    }
    
    @RequestMapping(value = "/transactions-for-update", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTNXStatusChange(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate, 
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("session_id") String session_id, 
            @RequestParam("requested_by") String requested_by, 
            @RequestParam("approved_by") String approved_by, 
            @RequestParam("current_status") String current_status, 
            @RequestParam("new_status") String new_status, 
            @RequestParam("status") String status
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetTNXStatusChange(session_id, startDate, endDate, page, limit, requested_by, approved_by, current_status, new_status, status);
    }
    
}
