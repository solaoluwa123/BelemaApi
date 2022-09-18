/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.CardsTransactionsInterface;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
public class CardsTransactionsController {
    @Autowired
    private CardsTransactionsInterface CardsTransactionsInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    Validators validators = new Validators();
    
    @RequestMapping(value = "/cards/transactions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.Get();
    }
    
    @RequestMapping(value = "/cards/transactions/q/search", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SearchTransactions(
            @RequestHeader(value = "Authorization") String header, 
            @RequestHeader(value = "auth-token") String sessiontoken,  
            @RequestParam("message_type") String message_type, 
            @RequestParam("bin") String bin, 
            @RequestParam("processing_code") String processing_code, 
            @RequestParam("system_trace_number") String system_trace_number, 
            @RequestParam("response_code") String response_code, 
            @RequestParam("min_amount") String min_amount, 
            @RequestParam("max_amount") String max_amount, 
            @RequestParam("start_date") String start_date, 
            @RequestParam("end_date") String end_date,
            @RequestParam("retrieval_ref_number") String retrieval_ref_number, 
            @RequestParam("acquirer_institution_id") String acquirer_institution_id, 
            @RequestParam("pan") String pan, 
            @RequestParam("terminal_id") String terminal_id, 
            @RequestParam("merchant_id") String merchant_id, 
            @RequestParam("location_name_address") String location_name_address,  
            @RequestParam("approval_code") String approval_code 
    ) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return CardsTransactionsInterface.SearchTransactions(message_type,
            bin,
            processing_code,
            min_amount,
            max_amount,
            system_trace_number,
            response_code,
            start_date,
            end_date,
            retrieval_ref_number,
            acquirer_institution_id,
            pan,
            terminal_id,
            merchant_id,
            location_name_address,
            approval_code);
    }
}