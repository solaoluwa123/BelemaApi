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
    public ResponseEntity Get(int id);
    public ResponseEntity GetByTerminal(String terminalid);
    public ResponseEntity GetByMerchant(String merchantid);
    public ResponseEntity GetByFI(String institution);
    public ResponseEntity GetByPTSP(String ptsp);
    public ResponseEntity GetByTerminalOwner(String owner);
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
            String approval_code);
    
    public ResponseEntity LogDispute(String sessiontoken, String terminalid, String rrn, String stan, String username);
    
    public ResponseEntity GetDisputes(String institutioncode);
    
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, int status);
}
