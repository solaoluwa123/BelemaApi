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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
        return MerchantsInterface.Create(
                model.getMerchant_id(), model.getMerchant_name(),
                model.getMerchant_state(), model.getMerchant_country(), model.getMerchant_category_code(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/merchants/{merchant_id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody CardsMerchantModel model,
            @PathVariable("merchant_id") String merchant_id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return MerchantsInterface.Edit(
                merchant_id, model.getMerchant_name(),
                model.getMerchant_state(), model.getMerchant_country(), model.getMerchant_category_code(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/merchants/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "sparkpay.merchants", "Merchant");
    }
    
    @RequestMapping(value = "/cards/merchants/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Approve(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.ApprovalHelper(sessiontoken, id, "sparkpay.merchants", "Merchant", type);
    }
    
    @RequestMapping(value = "/cards/merchants/reject/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Reject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.RejectHelper(sessiontoken, id, "sparkpay.merchants", "Merchant", type);
    }
}
