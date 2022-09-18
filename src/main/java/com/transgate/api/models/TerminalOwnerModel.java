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
public class TerminalOwnerModel {
    private String terminal_owner_id, terminal_owner_name, terminal_date;
    private int id, delete_flag, edit_flag, create_flag;

    public String getTerminal_owner_id() {
        return terminal_owner_id;
    }

    public void setTerminal_owner_id(String terminal_owner_id) {
        this.terminal_owner_id = terminal_owner_id;
    }

    public String getTerminal_owner_name() {
        return terminal_owner_name;
    }

    public void setTerminal_owner_name(String terminal_owner_name) {
        this.terminal_owner_name = terminal_owner_name;
    }

    public String getTerminal_date() {
        return terminal_date;
    }

    public void setTerminal_date(String terminal_date) {
        this.terminal_date = terminal_date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
    
}
