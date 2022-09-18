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
public interface NodesInterface {
    public ResponseEntity Get();
    public ResponseEntity GetApprovals();
    public ResponseEntity Create(String station_name, int local_port, int acquiring_institution_id, String kek, String send_key_request, String cbn_bank_code, String key_check_value, String transaction_direction, String remoteIP, int remote_port, String sessiontoken);
    public ResponseEntity Delete(String sessiontoken, int id);
}
