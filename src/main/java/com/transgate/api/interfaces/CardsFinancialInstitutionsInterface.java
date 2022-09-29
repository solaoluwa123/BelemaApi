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
    public ResponseEntity Get(String id, String column);
    public ResponseEntity Get(int id);
    public ResponseEntity GetApprovals();
    public ResponseEntity Create(String acquirer_id, String institution_name, String issuer_id, String bank_code, String sessiontoken);
    public ResponseEntity Edit(int id, String acquirer_id, String institution_name, String issuer_id, String bank_code, String sessiontoken);
    public ResponseEntity SearchCardsFinancialInstitutions(
           String start_date,
           String end_date,
           String acquirer_id,
           String institution_name,
           String issuer_id); 
}
