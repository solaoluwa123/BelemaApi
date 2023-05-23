/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.transgate.api.interfaces.GenericInterface;
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
    
    Validators validators = new Validators();
    
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
            @RequestParam("start") String start, @RequestParam("end") String end) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSettlements(start, end);
    }
    
    @RequestMapping(value = "/smartdets", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSmartDets(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSmartDets(start, end);
    }
    
    @RequestMapping(value = "/settlements/institution/{institution}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSettlements(@RequestHeader(value = "Authorization") String header, 
            @RequestParam("start") String start, @RequestParam("end") String end, @PathVariable("institution") String institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSettlements(institution, start, end);
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
    
}
