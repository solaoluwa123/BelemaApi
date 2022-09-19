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
public interface CardsFinancialInstitutionsInterface {
    public ResponseEntity Get();
    public ResponseEntity GetApprovals();
    public ResponseEntity Create(String acquirer_id, String institution_name, String issuer_id, String bank_code, String sessiontoken);
}
