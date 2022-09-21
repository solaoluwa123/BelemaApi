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
public interface TerminalOwnersInterface {
    public ResponseEntity Get();
    public ResponseEntity Get(String terminal_owner_id);
    public ResponseEntity Get(int id);
    public ResponseEntity GetApprovals();
    public ResponseEntity Create(String terminal_owner_id, String terminal_owner_name, String sessiontoken);
    public ResponseEntity Edit(String terminal_owner_id, String terminal_owner_name, String sessiontoken);
}
