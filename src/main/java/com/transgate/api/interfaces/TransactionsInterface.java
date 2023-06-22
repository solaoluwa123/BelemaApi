/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

import org.springframework.http.ResponseEntity;

/**
 *
 * @author Makintola
 */
public interface TransactionsInterface {
    public ResponseEntity Get();
    
    public ResponseEntity Get(String startDate, String endDate, int page, int limit, boolean isCurrent);
    
    public ResponseEntity GetBySessionId(String sessionid);
    
    public ResponseEntity GetBySessionId(String sessionid, boolean isCurrent);
    
    public ResponseEntity GetTransactionsVolume(String startDate, String endDate);
    
    public ResponseEntity GetFTTimeAverage(String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetFTTimeAverage(String institutioncode, String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetSuccessTNXVolume(String startDate, String endDate);
    
    public ResponseEntity GetSuccessTNXVolume(String institutioncode, String startDate, String endDate);
    
    public ResponseEntity GetTop6ResponseCodesTNX(String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetTop6ResponseCodesTNX(String institutioncode, String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetTransactionsVolumeByChannels(String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetTransactionsVolumeByChannels(String institutioncode, String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetTransactionsVolume(String institutioncode, String startDate, String endDate);
    
    public ResponseEntity GetTransactionsRates(String startDate, String endDate, boolean inward);
    
    public ResponseEntity GetTransactionsRates(String institutioncode, String startDate, String endDate, boolean inward);
    
    public ResponseEntity GetInsitutionTnxTrend(String institutioncode, String type, String startDate, String endDate);
    
    public ResponseEntity Get(int id);
    
    public ResponseEntity Get(String institutioncode);
    
    public ResponseEntity Get(String institutioncode, String startDate, String endDate, int page, int limit, boolean isCurrent);
    
    public ResponseEntity LogDispute(String sessiontoken, String sessionId, String amount, String wallet, String sourceInstitution, String type, String username);
    
    public ResponseEntity LogDisputesBulk(String sessiontoken, String records, String sourceInstitution, String username);
    
    public ResponseEntity GetDisputes(String institutioncode);
    
    public ResponseEntity GetDisputes(int id);
    
    public ResponseEntity GetDisputes(int id, int status, String institutioncode);
    
    public ResponseEntity GetSettlements(int id);
    
    public ResponseEntity GetSettlements(String institution);
    
    public ResponseEntity GetDisputeTypes();
    
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, String username, int status, String proof_of_reject_uri);
    
    public ResponseEntity SearchTransactionsForSessionIds(String sessionids);
    
    public ResponseEntity SearchTransactions(String srcSessionid,
            String channelCode,
            String responseCode,
            String srcInstitutioncode,
            String destInstitutioncode,
            String minAmount,
            String maxAmount,
            String srcAccountName,
            String destAccountName,
            String startDate,
            String endDate, 
            int page, 
            int limit,
            boolean isCurrent,
            String userInstitutionCode);
    
    public ResponseEntity GetCommissions(String institutionCode, String startDate, String endDate);
}
