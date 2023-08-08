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
public interface TerminalsInterface {
    public ResponseEntity Get();
    public ResponseEntity GetApprovals();
    public ResponseEntity Get(String id, String column);
    public ResponseEntity Get(String id);
    public ResponseEntity GetForMerchant(String merchantid);
    public ResponseEntity GetForPTSP(String ptsp);
    public ResponseEntity Create(String terminal_id, String merchant_id, String merchant_name, String sessiontoken);
    public ResponseEntity Edit(String terminal_id, String merchant_id, String merchant_name, String sessiontoken);
    public ResponseEntity SearchTerminals(
           String start_date,
           String end_date,
           String terminal_id,
           String merchant_id,
           String merchant_name); 
}
