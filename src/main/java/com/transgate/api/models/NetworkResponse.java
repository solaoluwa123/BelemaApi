/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.models;

import java.util.ArrayList;

/**
 *
 * @author Makintola
 */
public class NetworkResponse {
    int code;
    String status, message;
    ArrayList data;
    String meta;
    TNXModel tnxModel;
    
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ArrayList getData() {
        return data;
    }

    public void setData(ArrayList data) {
        this.data = data;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public TNXModel getTnxModel() {
        return tnxModel;
    }

    public void setTnxModel(TNXModel tnxModel) {
        this.tnxModel = tnxModel;
    }
       
}
