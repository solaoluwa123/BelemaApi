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
    
    public ResponseEntity GetTransactionsVolume();
    
    public ResponseEntity GetTransactionsVolume(String institutioncode);
    
    public ResponseEntity Get(int id);
    
    public ResponseEntity Get(String institutioncode);
    
    public ResponseEntity LogDispute(String sessiontoken, String sessionId, String amount, String wallet, String destinationInstitution, String type, String username);
    
    public ResponseEntity GetDisputes(String institutioncode);
    
    public ResponseEntity GetDisputes(int id);
    
    public ResponseEntity GetDisputes(int id, String status, String institutioncode);
    
    public ResponseEntity GetSettlements(int id);
    
    public ResponseEntity GetSettlements(String institution);
    
    public ResponseEntity GetDisputeTypes();
    
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, String username, String status);
    
    public ResponseEntity SearchTransactions(String srcSessionid,
            String srcInstitutioncode,
            String destInstitutioncode,
            String minAmount,
            String maxAmount,
            String srcAccountName,
            String destAccountName,
            String startDate,
            String endDate);
}
