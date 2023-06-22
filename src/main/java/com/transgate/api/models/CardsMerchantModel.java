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
public class CardsMerchantModel {
    private String merchant_id, merchant_name, merchant_category_code, date_created, merchant_country, merchant_state;
    private int id, delete_flag, edit_flag, create_flag;
    private ArrayList ptsps;
    private String ptsps_as_string;
    private double merchant_service_charge, msc_cap;
    
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

    public String getMerchant_category_code() {
        return merchant_category_code;
    }

    public void setMerchant_category_code(String merchant_category_code) {
        this.merchant_category_code = merchant_category_code;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
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

    public String getMerchant_country() {
        return merchant_country;
    }

    public void setMerchant_country(String merchant_country) {
        this.merchant_country = merchant_country;
    }

    public String getMerchant_state() {
        return merchant_state;
    }

    public void setMerchant_state(String merchant_state) {
        this.merchant_state = merchant_state;
    }

    public ArrayList getPtsps() {
        return ptsps;
    }

    public void setPtsps(ArrayList ptsps) {
        this.ptsps = ptsps;
    }

    public String getPtsps_as_string() {
        return ptsps_as_string;
    }

    public void setPtsps_as_string(String ptsps_as_string) {
        this.ptsps_as_string = ptsps_as_string;
    }

    public double getMerchant_service_charge() {
        return merchant_service_charge;
    }

    public void setMerchant_service_charge(double merchant_service_charge) {
        this.merchant_service_charge = merchant_service_charge;
    }

    public double getMsc_cap() {
        return msc_cap;
    }

    public void setMsc_cap(double msc_cap) {
        this.msc_cap = msc_cap;
    }
    
}
