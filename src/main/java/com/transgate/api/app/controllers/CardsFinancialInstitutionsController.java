/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.CardsFinancialInstitutionsInterface;
import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.models.CardsFinancialInstitutionModel;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Makintola
 */
@RestController
public class CardsFinancialInstitutionsController {
    @Autowired
    private CardsFinancialInstitutionsInterface CardsFinancialInstitutionsInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    Validators validators = new Validators();
    
    @Autowired
    GenericInterface GenericInterface;
    
    @RequestMapping(value = "/cards/financial-institutions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsFinancialInstitutionsInterface.Get();
    }
    
    @RequestMapping(value = "/cards/financial-institutions/merchant/{merchant}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByMerchant(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("merchant") String merchant) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsFinancialInstitutionsInterface.GetByMerchantUser(merchant);
    }
    
    @RequestMapping(value = "/cards/financial-institutions/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetApprovals(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsFinancialInstitutionsInterface.GetApprovals();
    }
    
    @RequestMapping(value = "/cards/financial-institutions", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody CardsFinancialInstitutionModel model) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsFinancialInstitutionsInterface.Create(
                model.getAcquirer_id(), model.getInstitution_name(), model.getIssuer_id(), model.getBank_code(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/financial-institutions/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody CardsFinancialInstitutionModel model,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsFinancialInstitutionsInterface.Edit(
                id, model.getAcquirer_id(), model.getInstitution_name(), model.getIssuer_id(), model.getBank_code(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/financial-institutions/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "sparkpayweb_db.tbl_financial_institutions", "Financial Institution");
    }
    
    @RequestMapping(value = "/cards/financial-institutions/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Approve(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.ApprovalHelper(sessiontoken, id, "sparkpayweb_db.tbl_financial_institutions", "Financial Institution", type);
    }
    
    @RequestMapping(value = "/cards/financial-institutions/reject/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Reject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.RejectHelper(sessiontoken, id, "sparkpayweb_db.tbl_financial_institutions", "Financial Institution", type);
    }
    
    @RequestMapping(value = "/cards/financial-institutions/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchFinancialInstitutions(
            @RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken,  
            @RequestParam("start_date") String start_date, 
            @RequestParam("end_date") String end_date,
            @RequestParam("acquirer_id") String acquirer_id, 
            @RequestParam("institution_name") String institution_name,
            @RequestParam("issuer_id") String issuer_id
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsFinancialInstitutionsInterface.SearchCardsFinancialInstitutions(
            start_date,
            end_date,
            acquirer_id,
            institution_name,
            issuer_id); 
    }
}
