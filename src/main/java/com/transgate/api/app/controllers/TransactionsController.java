/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.TransactionsInterface;
import com.transgate.api.models.DisputeModel;
import com.transgate.api.models.TransactionModel;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
import java.util.Optional;
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
    
    @RequestMapping(value = "/transactions-by-date", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.Get(startDate, endDate, page, limit, isCurrent);
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
        
    @RequestMapping(value = "/successful-transaction-count", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSuccessTNXVolume(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetSuccessTNXVolume(startDate, endDate);
    }
        
    @RequestMapping(value = "/successful-transaction-count/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSuccessTNXVolumeInstitution(@RequestHeader(value = "Authorization") String header,
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetSuccessTNXVolume(institutioncode, startDate, endDate);
    }
        
    @RequestMapping(value = "/ft-average-time", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetFTTimeAverage(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetFTTimeAverage(startDate, endDate, isCurrent);
    }
        
    @RequestMapping(value = "/ft-average-time/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetFTTimeAverageInstitution(@RequestHeader(value = "Authorization") String header,
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetFTTimeAverage(institutioncode, startDate, endDate, isCurrent);
    }
        
    @RequestMapping(value = "/top-failed-response-codes", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTop6ResponseCodesTNX(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTop6ResponseCodesTNX(startDate, endDate, isCurrent);
    }
        
    @RequestMapping(value = "/top-failed-response-codes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTop6ResponseCodesTNXInstitution(@RequestHeader(value = "Authorization") String header,
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTop6ResponseCodesTNX(institutioncode, startDate, endDate, isCurrent);
    }
        
    @RequestMapping(value = "/transactions-by-channels", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolumeByChannels(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsVolumeByChannels(startDate, endDate, isCurrent);
    }
        
    @RequestMapping(value = "/transactions-by-channels/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolumeByChannelsInstitution(@RequestHeader(value = "Authorization") String header,
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsVolumeByChannels(institutioncode, startDate, endDate, isCurrent);
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
    
    @RequestMapping(value = "/transactions/{sessionid}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetOne(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("sessionid") String sessionid) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetBySessionId(sessionid);
    }
    
    @RequestMapping(value = "/transactions-by-session-id/{sessionid}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetOneBySessionId(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("sessionid") String sessionid,
            @RequestParam("isCurrent") boolean isCurrent
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetBySessionId(sessionid, isCurrent);
    }
    
    @RequestMapping(value = "/transactions/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetInstitutionTransactions(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.Get(institutioncode);
    }
    
    @RequestMapping(value = "/transactions-by-session-ids", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity SearchTransactionsForSessionIds(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @RequestBody TransactionModel model) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.SearchTransactionsForSessionIds(model.getSrcSessionid());
    }
    
    @RequestMapping(value = "/transactions-by-date/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetInstitutionTransactions(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.Get(institutioncode, startDate, endDate, page, limit, isCurrent);
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
        return transactionsInterface.ApproveSettlement(sessiontoken, dispute.getId(), dispute.getUsername(), dispute.getStatus(), dispute.getProof_of_reject_uri());
    }
    
    @RequestMapping(value = "/transactions/disputes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetDisputes(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, 
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetDisputes(institutioncode, page, limit);
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
            @RequestParam("channelCode") String channelCode, 
            @RequestParam("responseCode") String responseCode, 
            @RequestParam("srcAccountName") String srcAccountName, 
            @RequestParam("destAccountName") String destAccountName, 
            @RequestParam("srcInstitutioncode") String srcInstitutioncode, 
            @RequestParam("destInstitutioncode") String destInstitutioncode, 
            @RequestParam("minAmount") String minAmount, 
            @RequestParam("maxAmount") String maxAmount, 
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam("isCurrent") boolean isCurrent,
            @RequestParam("userInstitutionCode") String userInstitutionCode
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.SearchTransactions(srcSessionid,
            channelCode,
            responseCode,
            srcInstitutioncode,
            destInstitutioncode,
            minAmount,
            maxAmount,
            srcAccountName.replaceAll("space", " "),
            destAccountName.replaceAll("space", " "),
            startDate,
            endDate,
            page,
            limit,
            isCurrent,
            userInstitutionCode);
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
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetCommissions(institutioncode, startDate, endDate);
    }
}
