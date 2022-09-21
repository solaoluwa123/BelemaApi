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
public interface PTSPsInterface {
    public ResponseEntity Get();
    public ResponseEntity Get(String ptsp_id);
    public ResponseEntity Get(int id);
    public ResponseEntity GetApprovals();
    public ResponseEntity Create(String ptsp_id, String ptsp_name, String sessiontoken);
    public ResponseEntity Edit(String ptsp_id, String ptsp_name, String sessiontoken);
}
