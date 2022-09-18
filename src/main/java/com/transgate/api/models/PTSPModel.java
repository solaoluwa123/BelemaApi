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
public class PTSPModel {
    private String ptsp_id, ptsp_name, ptsp_date;
    private int id, edit_flag, create_flag, delete_flag;

    public String getPtsp_id() {
        return ptsp_id;
    }

    public void setPtsp_id(String ptsp_id) {
        this.ptsp_id = ptsp_id;
    }

    public String getPtsp_name() {
        return ptsp_name;
    }

    public void setPtsp_name(String ptsp_name) {
        this.ptsp_name = ptsp_name;
    }

    public String getPtsp_date() {
        return ptsp_date;
    }

    public void setPtsp_date(String ptsp_date) {
        this.ptsp_date = ptsp_date;
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

    public int getCreate_flag() {
        return create_flag;
    }

    public void setCreate_flag(int create_flag) {
        this.create_flag = create_flag;
    }

    public int getDelete_flag() {
        return delete_flag;
    }

    public void setDelete_flag(int delete_flag) {
        this.delete_flag = delete_flag;
    }
    
}
