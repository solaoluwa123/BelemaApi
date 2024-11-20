/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.interfaces.TerminalOwnersInterface;
import com.transgate.api.models.TerminalOwnerModel;
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
public class TerminalOwnersController {
    @Autowired
    private TerminalOwnersInterface TerminalOwnersInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    private final Validators validators;
    // Constructor injection for RestCall
    public TerminalOwnersController(Validators validators) {
        this.validators = validators;
    }
    
    @Autowired
    GenericInterface GenericInterface;
    
    @RequestMapping(value = "/cards/terminal-owners", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalOwnersInterface.Get();
    }
    
    @RequestMapping(value = "/cards/terminal-owners/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetApprovals(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalOwnersInterface.GetApprovals();
    }
    
    @RequestMapping(value = "/cards/terminal-owners", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody TerminalOwnerModel model) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalOwnersInterface.Create(
                model.getTerminal_owner_id(), model.getTerminal_owner_name(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/terminal-owners/{owner_id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody TerminalOwnerModel model,
            @PathVariable("owner_id") String owner_id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalOwnersInterface.Edit(
                owner_id, model.getTerminal_owner_name(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/terminal-owners/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "sparkpayweb_db.tbl_terminal_owners", "Terminal Owner");
    }
    
    @RequestMapping(value = "/cards/terminal-owners/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Approve(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.ApprovalHelper(sessiontoken, id, "sparkpayweb_db.tbl_terminal_owners", "Terminal Owner", type);
    }
    
    @RequestMapping(value = "/cards/terminal-owners/reject/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Reject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.RejectHelper(sessiontoken, id, "sparkpayweb_db.tbl_terminal_owners", "Terminal Owner", type);
    }
    
    @RequestMapping(value = "/cards/terminal-owners/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchTerminalOwners(
            @RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken,  
            @RequestParam("start_date") String start_date, 
            @RequestParam("end_date") String end_date,
            @RequestParam("terminal_owner_id") String terminal_owner_id, 
            @RequestParam("terminal_owner_name") String terminal_owner_name
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return TerminalOwnersInterface.SearchTerminalOwners(
            start_date,
            end_date,
            terminal_owner_id,
            terminal_owner_name); 
    }
}
