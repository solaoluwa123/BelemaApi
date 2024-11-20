/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.interfaces.RoutesInterface;
import com.transgate.api.models.RouteModel;
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
public class RoutesController {
    @Autowired
    private RoutesInterface RoutesInterface;
    
    @Autowired
    GenericInterface GenericInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    private final Validators validators;
    // Constructor injection for RestCall
    public RoutesController(Validators validators) {
        this.validators = validators;
    }
    
    @RequestMapping(value = "/cards/routes", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return RoutesInterface.Get();
    }
    
    @RequestMapping(value = "/cards/routes/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetApprovals(@RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return RoutesInterface.GetApprovals();
    }
    
    @RequestMapping(value = "/cards/routes", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody RouteModel routeModel) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return RoutesInterface.Create(
                routeModel.getSource_acq_id(), routeModel.getDestination_bin(), routeModel.getCard_bin(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/routes/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody RouteModel routeModel,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return RoutesInterface.Edit(
                id, routeModel.getSource_acq_id(), routeModel.getDestination_bin(), routeModel.getCard_bin(), sessiontoken
            );
    }
    
    @RequestMapping(value = "/cards/routes/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "sparkpay.transaction_route", "Route");
    }
    
    @RequestMapping(value = "/cards/routes/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Approve(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.ApprovalHelper(sessiontoken, id, "sparkpay.transaction_route", "Route", type);
    }
    
    @RequestMapping(value = "/cards/routes/reject/{type}/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Reject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("type") String type) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.RejectHelper(sessiontoken, id, "sparkpay.transaction_route", "Route", type);
    }
    
    @RequestMapping(value = "/cards/routes/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchRoutes(
            @RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken,  
            @RequestParam("start_date") String start_date, 
            @RequestParam("end_date") String end_date,
            @RequestParam("source_acq_id") String source_acq_id, 
            @RequestParam("destination_bin") String destination_bin
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return RoutesInterface.SearchRoutes(
            start_date,
            end_date,
            source_acq_id,
            destination_bin); 
    }
}
