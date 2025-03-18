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
    public ResponseEntity GetSettlements(String startDate, String endDate, int page, int limit);
    public ResponseEntity GetCardsSettlementsByPTSP(String ptsp, String startDate, String endDate);
    public ResponseEntity GetSettlements(String institution, String startDate, String endDate, int page, int limit);
    public ResponseEntity SearchSettlementsByInstitution(String institution, String startDate, String endDate, int page, int limit);
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
    public ResponseEntity GetSmartDets(String startDate, String endDate);
    public ResponseEntity GetSettlementSummary(String startDate, String endDate);
    public ResponseEntity GetCardsSettlementSummary(String startDate, String endDate);
    public ResponseEntity GetCardPayments(String institutionCode, String startDate, String endDate, int page, int limit, String response_code, String transaction_id, String merchant_name);
    public ResponseEntity GetGapsPayments(String merchant_id, String startDate, String endDate, int page, int limit, String isSettled, String reference);
    public ResponseEntity GetTNXStatusChange(String session_id, String startDate, String endDate, int page, int limit, String requested_by, String approved_by, String current_status, String new_status, String status);
    public ResponseEntity GetCardNUSPayments(String institutionCode, String startDate, String endDate, int page, int limit, String response_code, String transaction_id, String merchant_name);
}
