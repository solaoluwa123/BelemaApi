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
public class TerminalModel {
    String terminal_id, merchant_id, merchant_name, route_mode, owner_id, owner_name;
    String acquiring_institution_id, acquiring_institution_name, cbn_bank_code;
    String terminal_type, date_time;
    int id, edit_flag, delete_flag, create_flag;

    public String getTerminal_id() {
        return terminal_id;
    }

    public void setTerminal_id(String terminal_id) {
        this.terminal_id = terminal_id;
    }

    public String getMerchant_id() {
        return merchant_id;
    }

    public void setMerchant_id(String merchant_id) {
        this.merchant_id = merchant_id;
    }

    public String getMerchant_name() {
        return merchant_name;
    }

    public void setMerchant_name(String merchant_name) {
        this.merchant_name = merchant_name;
    }

    public String getRoute_mode() {
        return route_mode;
    }

    public void setRoute_mode(String route_mode) {
        this.route_mode = route_mode;
    }

    public String getAcquiring_institution_id() {
        return acquiring_institution_id;
    }

    public void setAcquiring_institution_id(String acquiring_institution_id) {
        this.acquiring_institution_id = acquiring_institution_id;
    }

    public String getAcquiring_institution_name() {
        return acquiring_institution_name;
    }

    public void setAcquiring_institution_name(String acquiring_institution_name) {
        this.acquiring_institution_name = acquiring_institution_name;
    }

    public String getCbn_bank_code() {
        return cbn_bank_code;
    }

    public void setCbn_bank_code(String cbn_bank_code) {
        this.cbn_bank_code = cbn_bank_code;
    }

    public String getTerminal_type() {
        return terminal_type;
    }

    public void setTerminal_type(String terminal_type) {
        this.terminal_type = terminal_type;
    }

    public String getDate_time() {
        return date_time;
    }

    public void setDate_time(String date_time) {
        this.date_time = date_time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEdit_flag() {
        return edit_flag;
    }

    public void setEdit_flag(int edit_flag) {
        this.edit_flag = edit_flag;
    }

    public int getDelete_flag() {
        return delete_flag;
    }

    public void setDelete_flag(int delete_flag) {
        this.delete_flag = delete_flag;
    }

    public int getCreate_flag() {
        return create_flag;
    }

    public void setCreate_flag(int create_flag) {
        this.create_flag = create_flag;
    }

    public String getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(String owner_id) {
        this.owner_id = owner_id;
    }

    public String getOwner_name() {
        return owner_name;
    }

    public void setOwner_name(String owner_name) {
        this.owner_name = owner_name;
    }
    
}
