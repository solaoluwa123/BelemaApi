/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

import java.util.ArrayList;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Makintola
 */
public interface MerchantsInterface {
    public ResponseEntity Get();
    public ResponseEntity Get(String merchant_id);
    public ResponseEntity GetByInstitution(String institution);
    public ResponseEntity GetByPTSP(String ptsp);
    public ResponseEntity GetByTerminalOwner(String owner);
    public ResponseEntity Get(int id);
    public ResponseEntity GetApprovals();
    public ResponseEntity Create(String merchant_id, String merchant_name, String merchant_state, String merchant_country, String merchant_category_code, ArrayList ptsps, String sessiontoken);
    public ResponseEntity Edit(String merchant_id, String merchant_name, String merchant_state, String merchant_country, String merchant_category_code, ArrayList ptsps, String sessiontoken);
    public ResponseEntity SearchMerchants(
            String start_date,
            String end_date,
            String merchant_name,
            String merchant_id,
            String merchant_category_code);
}
