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
public interface MerchantsInterface {
    public ResponseEntity Get();
    public ResponseEntity GetApprovals();
    public ResponseEntity Create(String merchant_id, String merchant_name, String merchant_state, String merchant_country, String merchant_category_code, String sessiontoken);
}
