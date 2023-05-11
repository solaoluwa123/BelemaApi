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
public interface CardsTransactionsInterface {
    public ResponseEntity Get();
    public ResponseEntity Get(String startDate, String endDate, int page, int limit, boolean isCurrent);
    public ResponseEntity Get(int id);
    public ResponseEntity GetByTerminal(String terminalid, int page, int limit);
    public ResponseEntity GetByMerchant(String merchantid, int page, int limit);
    public ResponseEntity GetByMerchant(String merchantid, String startDate, String endDate, int page, int limit, boolean isCurrent);
    public ResponseEntity GetByFI(String institution, int page, int limit);
    public ResponseEntity GetByFI(String institution, String startDate, String endDate, int page, int limit, boolean isCurrent);
    public ResponseEntity GetByPTSP(String ptsp, int page, int limit);
    public ResponseEntity GetByPTSP(String ptsp, String startDate, String endDate, int page, int limit, boolean isCurrent);
    public ResponseEntity GetByTerminalOwner(String owner, int page, int limit);
    public ResponseEntity GetByTerminalOwner(String owner, String startDate, String endDate, int page, int limit, boolean isCurrent);
    public ResponseEntity SearchTransactions(String message_type,
            String bin,
            String processing_code,
            String min_amount,
            String max_amount,
            String system_trace_number,
            String response_code,
            String start_date,
            String end_date,
            String retrieval_ref_number,
            String acquirer_institution_id,
            String pan,
            String terminal_id,
            String merchant_id,
            String location_name_address,
            String approval_code, 
            int page, 
            int limit,
            boolean isCurrent);
    
    public ResponseEntity SearchDisputes(
            String terminal_id,
            String system_trace_number,
            String retrieval_ref_number,
            String transaction_response_code,
            String dispute_status,
            String date_logged,
            String date_resolved,
            String merchantsasIds,
            int page,
            int limit
        );
    
    public ResponseEntity LogDispute(String sessiontoken, String terminalid, String rrn, String stan, String proof_of_debit_uri, String username);
    
    public ResponseEntity LogDisputesBulk(String sessiontoken, String records, String username);
    
    public ResponseEntity GetDisputes(String institutioncode, String startDate, String endDate, int page, int limit);
    
    public ResponseEntity GetOneDispute(String uniqueId);
    
    public ResponseEntity GetArbitratedDisputes(String institutioncode);
    
    public ResponseEntity GetDisputesByMerchant(String merchantid, String startDate, String endDate, int page, int limit);
    
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, int status, String proof_of_reject_uri, String username);
    
    public ResponseEntity UpdateCardsDisputesNUBAN();
    
    public ResponseEntity UpdateDisputesData();
}
