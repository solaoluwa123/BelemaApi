/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.models;

/**
 *
 * @author Makintola
 */
public class NodeModel {
    int id, local_port, remote_port, delete_flag, edit_flag, create_flag;
    String station_name, kek, send_key_request, cbn_bank_code, acquiring_institution_id;
    String date_time, key_check_value, transaction_direction;
    String remoteIP;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLocal_port() {
        return local_port;
    }

    public void setLocal_port(int local_port) {
        this.local_port = local_port;
    }

    public String getAcquiring_institution_id() {
        return acquiring_institution_id;
    }

    public void setAcquiring_institution_id(String acquiring_institution_id) {
        this.acquiring_institution_id = acquiring_institution_id;
    }

    public int getRemote_port() {
        return remote_port;
    }

    public void setRemote_port(int remote_port) {
        this.remote_port = remote_port;
    }

    public int getDelete_flag() {
        return delete_flag;
    }

    public void setDelete_flag(int delete_flag) {
        this.delete_flag = delete_flag;
    }

    public int getEdit_flag() {
        return edit_flag;
    }

    public void setEdit_flag(int edit_flag) {
        this.edit_flag = edit_flag;
    }

    public int getCreate_flag() {
        return create_flag;
    }

    public void setCreate_flag(int create_flag) {
        this.create_flag = create_flag;
    }

    public String getStation_name() {
        return station_name;
    }

    public void setStation_name(String station_name) {
        this.station_name = station_name;
    }

    public String getKek() {
        return kek;
    }

    public void setKek(String kek) {
        this.kek = kek;
    }

    public String getSend_key_request() {
        return send_key_request;
    }

    public void setSend_key_request(String send_key_request) {
        this.send_key_request = send_key_request;
    }

    public String getCbn_bank_code() {
        return cbn_bank_code;
    }

    public void setCbn_bank_code(String cbn_bank_code) {
        this.cbn_bank_code = cbn_bank_code;
    }

    public String getDate_time() {
        return date_time;
    }

    public void setDate_time(String date_time) {
        this.date_time = date_time;
    }

    public String getKey_check_value() {
        return key_check_value;
    }

    public void setKey_check_value(String key_check_value) {
        this.key_check_value = key_check_value;
    }

    public String getTransaction_direction() {
        return transaction_direction;
    }

    public void setTransaction_direction(String transaction_direction) {
        this.transaction_direction = transaction_direction;
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }
    
}   

