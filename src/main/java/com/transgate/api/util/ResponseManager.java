/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;
import com.transgate.api.models.NetworkResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Makintola
 */
public class ResponseManager {
    
    public ResponseEntity ResponseOk(Object body) {
        return new ResponseEntity(body, HttpStatus.OK);
    }
    
    public ResponseEntity InvalidAuthorizationHeader() {
        NetworkResponse networkResponse = new NetworkResponse();
        networkResponse.setCode(403);
        networkResponse.setStatus("forbidden");
        networkResponse.setMessage("Invalid value in header");
        return new ResponseEntity(networkResponse, HttpStatus.FORBIDDEN);
    }
    
    public ResponseEntity ResponseAccepted() {
        NetworkResponse networkResponse = new NetworkResponse();
        networkResponse.setCode(200);
        networkResponse.setStatus("success");
        networkResponse.setMessage("Request Accepted");
        return new ResponseEntity(networkResponse, HttpStatus.ACCEPTED);
    }
    
    public ResponseEntity ResponseAccepted(String message) {
        NetworkResponse networkResponse = new NetworkResponse();
        networkResponse.setCode(200);
        networkResponse.setStatus("success");
        networkResponse.setMessage(message);
        return new ResponseEntity(networkResponse, HttpStatus.ACCEPTED);
    }
    
    public ResponseEntity ResponseNotFound(Object body) {
        return new ResponseEntity(body, HttpStatus.NOT_FOUND);
    }
    
    public ResponseEntity ResponseInternalServerError() {
        NetworkResponse networkResponse = new NetworkResponse();
        networkResponse.setCode(500);
        networkResponse.setStatus("error");
        networkResponse.setMessage("Unknown error has occured");
        return new ResponseEntity(networkResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public ResponseEntity ResponseUnathorized() {
        NetworkResponse networkResponse = new NetworkResponse();
        networkResponse.setCode(401);
        networkResponse.setStatus("unathorized");
        networkResponse.setMessage("Unathorized request");
        return new ResponseEntity(networkResponse, HttpStatus.UNAUTHORIZED);
    }
    
    public ResponseEntity ResponseBadRequest() {
        NetworkResponse networkResponse = new NetworkResponse();
        networkResponse.setCode(400);
        networkResponse.setStatus("error");
        networkResponse.setMessage("Bad request");
        return new ResponseEntity(networkResponse, HttpStatus.BAD_REQUEST);
    }
    
    public ResponseEntity ResponseDeleted() {
        NetworkResponse networkResponse = new NetworkResponse();
        networkResponse.setCode(200);
        networkResponse.setStatus("success");
        networkResponse.setMessage("Deleted");
        return new ResponseEntity(networkResponse, HttpStatus.ACCEPTED);
    }
}
