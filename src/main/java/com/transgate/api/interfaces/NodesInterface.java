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
    public ResponseEntity Get(String id, String column);
    public ResponseEntity Get(int id);
    public ResponseEntity GetApprovals();
    public ResponseEntity Create(String station_name, int local_port, int acquiring_institution_id, String kek, String send_key_request, String cbn_bank_code, String key_check_value, String transaction_direction, String remoteIP, int remote_port, String sessiontoken);
    public ResponseEntity Edit(int id, String station_name, int local_port, int acquiring_institution_id, String kek, String send_key_request, String cbn_bank_code, String key_check_value, String transaction_direction, String remoteIP, int remote_port, String sessiontoken);
    public ResponseEntity Delete(String sessiontoken, int id);
    public ResponseEntity SearchNodes(
           String start_date,
           String end_date, String station_name,
           String local_port,
           String acquiring_institution_id,
           String cbn_bank_code); 
}
