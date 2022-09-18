/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.transgate.api.interfaces.GenericInterface;

/**
 *
 * @author Makintola
 */
@RestController
public class GenericController {
    @Autowired
    private GenericInterface GenericInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    Validators validators = new Validators();
    
    @RequestMapping(value = "/cards/banks", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetBanks(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetBanks();
    }
    
    @RequestMapping(value = "/cards/skr", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSKR(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetSKR();
    }
    
    @RequestMapping(value = "/cards/tnxdirection", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionDirection(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetTransactionDirection();
    }
    
    @RequestMapping(value = "/cards/states", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetStates(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetStates();
    }
    
    @RequestMapping(value = "/cards/response-codes", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetResponseCodes(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.GetResponseCodes();
    }
    
}
