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
public interface FinancialInstitutionsInterface {
    
    public ResponseEntity GetFinancialInstitutions();

    public ResponseEntity Create(String sessiontoken, String name, String code, String business_address, int businessType, String creator);
    
    public ResponseEntity GetFinancialInstitutionsTypes();
    
    public ResponseEntity Delete(String sessiontoken, String code, String username);
    
    public ResponseEntity Activate(String sessiontoken, String code, String username);
    
    public ResponseEntity Edit(String sessiontoken, String code, String name, String business_address, int businessType, String editor);
    
    public ResponseEntity GetFinancialInstitutionByCode(String sessiontoken, String code);
    
    public ResponseEntity GetFinancialInstitutionsTypeById(String sessiontoken, int id);
    
    public ResponseEntity FinancialInstitutionApprovals(String sessiontoken, int id, String actionType, String username);
    
    public ResponseEntity FinancialInstitutionReject(String sessiontoken, int id, String actionType, String username);
    
    public ResponseEntity GetFinancialInstitutionsForActions();
    
    public ResponseEntity CreateContact(String sessiontoken, String creator, String institution, String firstname, String surname, String phone_number, String email_address, String security);
    
    public ResponseEntity DeleteContact(String sessiontoken, int id, String username);
    
    public ResponseEntity GetContactById(String sessiontoken, int id);
    
    public ResponseEntity EditContact(String sessiontoken, int id, String firstname, String surname, String phone_number, String username);
    
    public ResponseEntity GetAllContacts(String sessiontoken);
    
    public ResponseEntity GetAllContacts(String sessiontoken, String code);
    
    public ResponseEntity GetAllContactsForActions(String sessiontoken);
    
    public ResponseEntity GetAllContactsForActions(String sessiontoken, String code);
}
