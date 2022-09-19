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
public class RouteModel {
    int id, edit_flag, create_flag, delete_flag, card_bin;
    String source_acq_id, destination_bin, date_created;

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

    public int getCreate_flag() {
        return create_flag;
    }

    public void setCreate_flag(int create_flag) {
        this.create_flag = create_flag;
    }

    public String getSource_acq_id() {
        return source_acq_id;
    }

    public void setSource_acq_id(String source_acq_id) {
        this.source_acq_id = source_acq_id;
    }

    public String getDestination_bin() {
        return destination_bin;
    }

    public void setDestination_bin(String destination_bin) {
        this.destination_bin = destination_bin;
    }

    public int getCard_bin() {
        return card_bin;
    }

    public void setCard_bin(int card_bin) {
        this.card_bin = card_bin;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public int getDelete_flag() {
        return delete_flag;
    }

    public void setDelete_flag(int delete_flag) {
        this.delete_flag = delete_flag;
    }
    
    
}
