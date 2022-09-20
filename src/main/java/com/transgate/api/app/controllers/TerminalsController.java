/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.interfaces.TerminalsInterface;
import com.transgate.api.models.TerminalModel;
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
public class TerminalsController {
    @Autowired
    private TerminalsInterface TerminalsInterface;
    
    @Autowired
    GenericInterface GenericInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    Validators validators = new Validators();
    
    @RequestMapping(value = "/cards/terminals", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.Get();
    }
    
    @RequestMapping(value = "/cards/terminals/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetApprovals(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.GetApprovals();
    }
    
    @RequestMapping(value = "/cards/terminals", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody TerminalModel terminalModel) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.Create(
                terminalModel.getTerminal_id(), terminalModel.getMerchant_id(), terminalModel.getMerchant_name(),
                terminalModel.getRoute_mode(), terminalModel.getAcquiring_institution_id(), terminalModel.getAcquiring_institution_name(), 
                terminalModel.getCbn_bank_code(), terminalModel.getTerminal_type(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/terminals/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") String id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "terminal_id", "sparkpay.terminals", "Terminal");
    }
    
    @RequestMapping(value = "/cards/terminals/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Approve(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") String id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.ApprovalHelper(sessiontoken, id, "terminal_id", "sparkpay.terminals", "Terminal", type);
    }
}
