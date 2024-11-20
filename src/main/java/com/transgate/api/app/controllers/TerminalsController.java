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
import com.transgate.api.app.services.Validators;
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
public class TerminalsController {
    @Autowired
    private TerminalsInterface TerminalsInterface;
    
    @Autowired
    GenericInterface GenericInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    private final Validators validators;
    // Constructor injection for RestCall
    public TerminalsController(Validators validators) {
        this.validators = validators;
    }
    
    @RequestMapping(value = "/cards/terminals", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.Get();
    }
    
    @RequestMapping(value = "/cards/terminals/merchant/{merchantid}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByMerchant(@RequestHeader(value = "Authorization") String header,
            @PathVariable("merchantid") String merchantid) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.GetForMerchant(merchantid);
    }
    
    @RequestMapping(value = "/cards/terminals/institution/{institution}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByFI(@RequestHeader(value = "Authorization") String header,
            @PathVariable("institution") String institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.Get(institution, "acquiring_institution_id");
    }
    
    @RequestMapping(value = "/cards/terminals/ptsp/{ptsp}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByPTSP(@RequestHeader(value = "Authorization") String header,
            @PathVariable("ptsp") String ptsp) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.GetForPTSP(ptsp);
    }
    
    @RequestMapping(value = "/cards/terminals/terminal-owner/{owner_id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetByTerminalOwner(@RequestHeader(value = "Authorization") String header,
            @PathVariable("owner_id") String owner_id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.Get(owner_id, "owner_id");
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
                terminalModel.getTerminal_id(), terminalModel.getMerchant_id(), terminalModel.getMerchant_name(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/terminals/{terminal_id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody TerminalModel terminalModel,
            @PathVariable("terminal_id") String terminal_id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.Edit(
                terminal_id, terminalModel.getMerchant_id(), terminalModel.getMerchant_name(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/terminals/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") String id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "terminal_id", "postxnprocessor.terminal_parameters", "Terminal");
    }
    
    @RequestMapping(value = "/cards/terminals/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Approve(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") String id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.ApprovalHelper(sessiontoken, id, "terminal_id", "postxnprocessor.terminals", "Terminal", type);
    }
    
    @RequestMapping(value = "/cards/terminals/reject/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Reject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") String id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.RejectHelper(sessiontoken, id, "terminal_id", "postxnprocessor.terminals", "Terminal", type);
    }
    
    @RequestMapping(value = "/cards/terminals/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchTerminals(
            @RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken,  
            @RequestParam("start_date") String start_date, 
            @RequestParam("end_date") String end_date,
            @RequestParam("terminal_id") String terminal_id, 
            @RequestParam("merchant_id") String merchant_id, 
            @RequestParam("merchant_name") String merchant_name
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalsInterface.SearchTerminals(
            start_date,
            end_date,
            terminal_id,
            merchant_id,
            merchant_name); 
    }
}
