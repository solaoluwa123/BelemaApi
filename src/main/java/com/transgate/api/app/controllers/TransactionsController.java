/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.TransactionsInterface;
import com.transgate.api.models.DisputeModel;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
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
public class TransactionsController {
    @Autowired
    private TransactionsInterface transactionsInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    Validators validators = new Validators();
    
    @RequestMapping(value = "/transactions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.Get();
    }
        
    @RequestMapping(value = "/transactions-summary", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolume(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsVolume(startDate, endDate);
    }
        
    @RequestMapping(value = "/transactions-summary/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolumeInstitution(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsVolume(institutioncode, startDate, endDate);
    }
        
    @RequestMapping(value = "/transactions-rates", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsRates(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsRates(startDate, endDate, false);
    }
        
    @RequestMapping(value = "/transactions-rates/inward", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsRatesInward(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsRates(startDate, endDate, true);
    }
        
    @RequestMapping(value = "/transactions-rates/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsRates(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsRates(institutioncode, startDate, endDate, false);
    }
        
    @RequestMapping(value = "/transactions-rates/inward/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsRatesInward(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsRates(institutioncode, startDate, endDate, true);
    }
        
    @RequestMapping(value = "/transactions-trend/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetInsitutionTnxTrend(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,  
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate, 
            @RequestParam("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetInsitutionTnxTrend(institutioncode, type, startDate, endDate);
    }
    
    @RequestMapping(value = "/transactions/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetOne(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.Get(id);
    }
    
    @RequestMapping(value = "/transactions/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetInstitutionTransactions(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.Get(institutioncode);
    }
    
    @RequestMapping(value = "/transactions/disputes/create", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody DisputeModel dispute) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.LogDispute(sessiontoken, dispute.getSrcSessionid(), dispute.getSrcAmount(), dispute.getSrcAccountNumber(), dispute.getSrcInstitutioncode(), dispute.getType(), dispute.getUsername());
    }
    
    @RequestMapping(value = "/transactions/disputes/approve", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity ApproveSettlement(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody DisputeModel dispute) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.ApproveSettlement(sessiontoken, dispute.getId(), dispute.getUsername(), dispute.getStatus());
    }
    
    @RequestMapping(value = "/transactions/disputes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputes(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetDisputes(institutioncode);
    }
    
    @RequestMapping(value = "/transactions/disputes/get/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputesOne(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("id") int id) {
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
    public ResponseEntity GetSettlementsOne(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetSettlements(id);
    }
    
    @RequestMapping(value = "/transactions/settlements/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSettlements(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetSettlements(institutioncode);
    }
    
    @RequestMapping(value = "/transactions/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchTransactions(
            @RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken,  
            @RequestParam("srcSessionid") String srcSessionid, 
            @RequestParam("srcAccountName") String srcAccountName, 
            @RequestParam("destAccountName") String destAccountName, 
            @RequestParam("srcInstitutioncode") String srcInstitutioncode, 
            @RequestParam("destInstitutioncode") String destInstitutioncode, 
            @RequestParam("minAmount") String minAmount, 
            @RequestParam("maxAmount") String maxAmount, 
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.SearchTransactions(srcSessionid,
            srcInstitutioncode,
            destInstitutioncode,
            minAmount,
            maxAmount,
            srcAccountName,
            destAccountName,
            startDate,
            endDate);
    }
}
