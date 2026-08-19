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
    int id, businessType, port_number, isProcessTSQ;
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

    public int getIsProcessTSQ() {
        return isProcessTSQ;
    }

    public void setIsProcessTSQ(int isProcessTSQ) {
        this.isProcessTSQ = isProcessTSQ;
    }

    int issettlementbank, neTimeout, ftTimeout, instWithWallet, wallettype, enableInward;
    String serverIP, url, urlTSQ, neEnvelope, neResponseStartTag, neResponseEndTag;
    String ftEnvelope, ftResponseStartTag, ftResponseEndTag, tsqEnvelope, tsqResponseStartTag, tsqResponseEndTag, walletname;

    public int getIssettlementbank() {
        return issettlementbank;
    }

    public void setIssettlementbank(int issettlementbank) {
        this.issettlementbank = issettlementbank;
    }

    public int getNeTimeout() {
        return neTimeout;
    }

    public void setNeTimeout(int neTimeout) {
        this.neTimeout = neTimeout;
    }

    public int getFtTimeout() {
        return ftTimeout;
    }

    public void setFtTimeout(int ftTimeout) {
        this.ftTimeout = ftTimeout;
    }

    public int getInstWithWallet() {
        return instWithWallet;
    }

    public void setInstWithWallet(int instWithWallet) {
        this.instWithWallet = instWithWallet;
    }

    public int getEnableInward() {
        return enableInward;
    }

    public void setEnableInward(int enableInward) {
        this.enableInward = enableInward;
    }

    public int getWallettype() {
        return wallettype;
    }

    public void setWallettype(int wallettype) {
        this.wallettype = wallettype;
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlTSQ() {
        return urlTSQ;
    }

    public void setUrlTSQ(String urlTSQ) {
        this.urlTSQ = urlTSQ;
    }

    public String getNeEnvelope() {
        return neEnvelope;
    }

    public void setNeEnvelope(String neEnvelope) {
        this.neEnvelope = neEnvelope;
    }

    public String getNeResponseStartTag() {
        return neResponseStartTag;
    }

    public void setNeResponseStartTag(String neResponseStartTag) {
        this.neResponseStartTag = neResponseStartTag;
    }

    public String getNeResponseEndTag() {
        return neResponseEndTag;
    }

    public void setNeResponseEndTag(String neResponseEndTag) {
        this.neResponseEndTag = neResponseEndTag;
    }

    public String getFtEnvelope() {
        return ftEnvelope;
    }

    public void setFtEnvelope(String ftEnvelope) {
        this.ftEnvelope = ftEnvelope;
    }

    public String getFtResponseStartTag() {
        return ftResponseStartTag;
    }

    public void setFtResponseStartTag(String ftResponseStartTag) {
        this.ftResponseStartTag = ftResponseStartTag;
    }

    public String getFtResponseEndTag() {
        return ftResponseEndTag;
    }

    public void setFtResponseEndTag(String ftResponseEndTag) {
        this.ftResponseEndTag = ftResponseEndTag;
    }

    public String getTsqEnvelope() {
        return tsqEnvelope;
    }

    public void setTsqEnvelope(String tsqEnvelope) {
        this.tsqEnvelope = tsqEnvelope;
    }

    public String getTsqResponseStartTag() {
        return tsqResponseStartTag;
    }

    public void setTsqResponseStartTag(String tsqResponseStartTag) {
        this.tsqResponseStartTag = tsqResponseStartTag;
    }

    public String getTsqResponseEndTag() {
        return tsqResponseEndTag;
    }

    public void setTsqResponseEndTag(String tsqResponseEndTag) {
        this.tsqResponseEndTag = tsqResponseEndTag;
    }

    public String getWalletname() {
        return walletname;
    }

    public void setWalletname(String walletname) {
        this.walletname = walletname;
    }
    
}
