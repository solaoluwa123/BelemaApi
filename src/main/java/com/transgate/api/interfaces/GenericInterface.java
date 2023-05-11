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
public interface GenericInterface {
    public ResponseEntity GetBanks();
    public ResponseEntity GetSKR();
    public ResponseEntity GetTransactionDirection();
    public ResponseEntity GetStates();
    public ResponseEntity GetResponseCodes();
    public ResponseEntity GetSettlements(String startDate, String endDate);
    public ResponseEntity GetCardsSettlementsByPTSP(String ptsp, String startDate, String endDate);
    public ResponseEntity GetSettlements(String institution, String startDate, String endDate);
    public ResponseEntity GetSettlementsByMerchant(String merchant, String startDate, String endDate);
    public ResponseEntity GetCardsSettlements(String startDate, String endDate);
    public ResponseEntity GetCardsSettlementsByAcquirer(String acquirer, String startDate, String endDate);
    public ResponseEntity GetCardsSettlementsByIssuer(String issuer, String startDate, String endDate);
    public ResponseEntity DeleteHelper(String sessiontoken, int id, String table, String entity);
    public ResponseEntity DeleteHelper(String sessiontoken, String id, String column, String table, String entity);
    public ResponseEntity GetTerminalTypes();
    public ResponseEntity ApprovalHelper(String sessiontoken, int id, String table, String entity, String approvalType);
    public ResponseEntity ApprovalHelper(String sessiontoken, String id, String column, String table, String entity, String approvalType);
    public ResponseEntity RejectHelper(String sessiontoken, int id, String table, String entity, String approvalType);
    public ResponseEntity RejectHelper(String sessiontoken, String id, String column, String table, String entity, String approvalType);
    public ResponseEntity GetCardsSmartDets(String startDate, String endDate);
}
