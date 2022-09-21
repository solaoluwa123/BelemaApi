/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

import org.springframework.http.ResponseEntity;

/**
 *
 * @author Makintola
 */
public interface GenericInterface {
    public ResponseEntity GetBanks();
    public ResponseEntity GetSKR();
    public ResponseEntity GetTransactionDirection();
    public ResponseEntity GetStates();
    public ResponseEntity GetResponseCodes();
    public ResponseEntity DeleteHelper(String sessiontoken, int id, String table, String entity);
    public ResponseEntity DeleteHelper(String sessiontoken, String id, String column, String table, String entity);
    public ResponseEntity GetTerminalTypes();
    public ResponseEntity ApprovalHelper(String sessiontoken, int id, String table, String entity, String approvalType);
    public ResponseEntity ApprovalHelper(String sessiontoken, String id, String column, String table, String entity, String approvalType);
    public ResponseEntity RejectHelper(String sessiontoken, int id, String table, String entity, String approvalType);
    public ResponseEntity RejectHelper(String sessiontoken, String id, String column, String table, String entity, String approvalType);
}
