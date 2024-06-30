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
public class FinancialInstitutionModel {
    int id, businessType, port_number;
    String name, shortName, code, color, business_address, date_created, businessTypeName, created_by, actionType, note, status, date_updated;
    String publickeylocation, cbn_bank_account, switch_code, publickeylocationLinux, password, hashKey;
    float vat, charge_amount;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBusinessType() {
        return businessType;
    }

    public void setBusinessType(int businessType) {
        this.businessType = businessType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
    
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
    
    public String getBusiness_address() {
        return business_address;
    }

    public void setBusiness_address(String business_address) {
        this.business_address = business_address;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public String getBusinessTypeName() {
        return businessTypeName;
    }

    public void setBusinessTypeName(String businessTypeName) {
        this.businessTypeName = businessTypeName;
    }

    public String getCreated_by() {
        return created_by;
    }

    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate_updated() {
        return date_updated;
    }

    public void setDate_updated(String date_updated) {
        this.date_updated = date_updated;
    }

    public String getPublickeylocation() {
        return publickeylocation;
    }

    public void setPublickeylocation(String publickeylocation) {
        this.publickeylocation = publickeylocation;
    }

    public int getPort_number() {
        return port_number;
    }

    public void setPort_number(int port_number) {
        this.port_number = port_number;
    }

    public String getCbn_bank_account() {
        return cbn_bank_account;
    }

    public void setCbn_bank_account(String cbn_bank_account) {
        this.cbn_bank_account = cbn_bank_account;
    }

    public String getSwitch_code() {
        return switch_code;
    }

    public void setSwitch_code(String switch_code) {
        this.switch_code = switch_code;
    }

    public String getPublickeylocationLinux() {
        return publickeylocationLinux;
    }

    public void setPublickeylocationLinux(String publickeylocationLinux) {
        this.publickeylocationLinux = publickeylocationLinux;
    }

    public float getVat() {
        return vat;
    }

    public void setVat(float vat) {
        this.vat = vat;
    }

    public float getCharge_amount() {
        return charge_amount;
    }

    public void setCharge_amount(float charge_amount) {
        this.charge_amount = charge_amount;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }
    
}
