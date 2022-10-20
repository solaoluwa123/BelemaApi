/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.interfaces.MerchantsInterface;
import com.transgate.api.models.CardsMerchantModel;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class MerchantsController {
    @Autowired
    private MerchantsInterface MerchantsInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    Validators validators = new Validators();
    
    @Autowired
    GenericInterface GenericInterface;
    
    @RequestMapping(value = "/cards/merchants", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return MerchantsInterface.Get();
    }
    
    @RequestMapping(value = "/cards/merchants/institution/{institution}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByInstitution(@RequestHeader(value = "Authorization") String header,
            @PathVariable("institution") String institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return MerchantsInterface.GetByInstitution(institution);
    }
    
    @RequestMapping(value = "/cards/merchants/ptsp/{ptsp}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByPTSP(@RequestHeader(value = "Authorization") String header,
            @PathVariable("ptsp") String ptsp) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return MerchantsInterface.GetByPTSP(ptsp);
    }
    
    @RequestMapping(value = "/cards/merchants/terminal-owner/{owner}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByTerminalOwner(@RequestHeader(value = "Authorization") String header,
            @PathVariable("owner") String owner) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return MerchantsInterface.GetByTerminalOwner(owner);
    }
    
    @RequestMapping(value = "/cards/merchants/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetApprovals(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return MerchantsInterface.GetApprovals();
    }
    
    @RequestMapping(value = "/cards/merchants", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody CardsMerchantModel model) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        List<String> ptspsArray = new ArrayList<>(Arrays.asList(model.getPtsps_as_string().split(",")));
        return MerchantsInterface.Create(model.getMerchant_id(), model.getMerchant_name(),
                model.getMerchant_state(), model.getMerchant_country(), model.getMerchant_category_code(), (ArrayList) ptspsArray, sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/merchants/{merchant_id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody CardsMerchantModel model,
            @PathVariable("merchant_id") String merchant_id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        List<String> ptspsArray = new ArrayList<>(Arrays.asList(model.getPtsps_as_string().split(",")));
        return MerchantsInterface.Edit(merchant_id, model.getMerchant_name(),
                model.getMerchant_state(), model.getMerchant_country(), model.getMerchant_category_code(), (ArrayList) ptspsArray, sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/merchants/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "postxnprocessor.tbl_merchants", "Merchant");
    }
    
    @RequestMapping(value = "/cards/merchants/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Approve(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.ApprovalHelper(sessiontoken, id, "postxnprocessor.tbl_merchants", "Merchant", type);
    }
    
    @RequestMapping(value = "/cards/merchants/reject/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Reject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.RejectHelper(sessiontoken, id, "postxnprocessor.tbl_merchants", "Merchant", type);
    }
    
    @RequestMapping(value = "/cards/merchants/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchMerchants(
            @RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken,  
            @RequestParam("start_date") String start_date, 
            @RequestParam("end_date") String end_date,
            @RequestParam("merchant_name") String merchant_name, 
            @RequestParam("merchant_id") String merchant_id, 
            @RequestParam("merchant_category_code") String merchant_category_code
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return MerchantsInterface.SearchMerchants(
            start_date,
            end_date,
            merchant_name,
            merchant_id,
            merchant_category_code); 
    }
}
