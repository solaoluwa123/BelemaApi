/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.CardChartInterface;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.app.services.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author roqeeb
 */
@RestController
@RequestMapping("cards")
public class CardChartsController {
    @Autowired
    private CardChartInterface transactionsInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    private final Validators validators;

    // Constructor injection for RestCall
    public CardChartsController(Validators validators) {
        this.validators = validators;
    }
        
    @RequestMapping(value = "/successful-transactions-count", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSuccessTNXVolume(@RequestHeader(value = "Authorization") String header,
                                              @RequestParam("startDate") String startDate,
                                              @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetSuccessTNXVolume(startDate, endDate);
    }
        
    @RequestMapping(value = "/successful-transaction-count/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetSuccessTNXVolumeInstitution(@RequestHeader(value = "Authorization") String header,
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetSuccessTNXVolume(institutioncode, startDate, endDate);
    }
        
    @RequestMapping(value = "/top-failed-response-codes", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTop6ResponseCodesTNX(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTop6ResponseCodesTNX(startDate, endDate, isCurrent);
    }
        
    @RequestMapping(value = "/top-failed-response-codes/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTop6ResponseCodesTNXInstitution(@RequestHeader(value = "Authorization") String header,
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTop6ResponseCodesTNX(institutioncode, startDate, endDate, isCurrent);
    }
        
    @RequestMapping(value = "/transactions-by-merchant", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolumeByChannels(@RequestHeader(value = "Authorization") String header,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsVolumeByChannels(startDate, endDate, isCurrent);
    }
        
    @RequestMapping(value = "/transactions-by-channels/institution/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTransactionsVolumeByChannelsInstitution(@RequestHeader(value = "Authorization") String header,
            @PathVariable ("institutioncode") String institutioncode,
            @RequestParam("startDate") String startDate, 
            @RequestParam("endDate") String endDate,
            @RequestParam("isCurrent") boolean isCurrent) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return transactionsInterface.GetTransactionsVolumeByChannels(institutioncode, startDate, endDate, isCurrent);
    }

}