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
public class TransactionModel {
    int id;
    String srcSessionid, srcAccountNumber, srcAccountName, srcKycLevel, srcBvn, srcAmount, srcInstitutioncode, destSessionId, srcResponsecode, destAccountNumber;
    String destAccountName, destKycLevel, destBvn, destAmount, destInstitutioncode, destResponseCode, narration, transactiondate, username;
    String responseCodeDefinition;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSrcSessionid() {
        return srcSessionid;
    }

    public void setSrcSessionid(String srcSessionid) {
        this.srcSessionid = srcSessionid;
    }

    public String getSrcAccountNumber() {
        return srcAccountNumber;
    }

    public void setSrcAccountNumber(String srcAccountNumber) {
        this.srcAccountNumber = srcAccountNumber;
    }

    public String getSrcAccountName() {
        return srcAccountName;
    }

    public void setSrcAccountName(String srcAccountName) {
        this.srcAccountName = srcAccountName;
    }

    public String getSrcKycLevel() {
        return srcKycLevel;
    }

    public void setSrcKycLevel(String srcKycLevel) {
        this.srcKycLevel = srcKycLevel;
    }

    public String getSrcBvn() {
        return srcBvn;
    }

    public void setSrcBvn(String srcBvn) {
        this.srcBvn = srcBvn;
    }

    public String getSrcAmount() {
        return srcAmount;
    }

    public void setSrcAmount(String srcAmount) {
        this.srcAmount = srcAmount;
    }

    public String getSrcInstitutioncode() {
        return srcInstitutioncode;
    }

    public void setSrcInstitutioncode(String srcInstitutioncode) {
        this.srcInstitutioncode = srcInstitutioncode;
    }

    public String getDestSessionId() {
        return destSessionId;
    }

    public void setDestSessionId(String destSessionId) {
        this.destSessionId = destSessionId;
    }

    public String getSrcResponsecode() {
        return srcResponsecode;
    }

    public void setSrcResponsecode(String srcResponsecode) {
        this.srcResponsecode = srcResponsecode;
    }

    public String getDestAccountNumber() {
        return destAccountNumber;
    }

    public void setDestAccountNumber(String destAccountNumber) {
        this.destAccountNumber = destAccountNumber;
    }

    public String getDestAccountName() {
        return destAccountName;
    }

    public void setDestAccountName(String destAccountName) {
        this.destAccountName = destAccountName;
    }

    public String getDestKycLevel() {
        return destKycLevel;
    }

    public void setDestKycLevel(String destKycLevel) {
        this.destKycLevel = destKycLevel;
    }

    public String getDestBvn() {
        return destBvn;
    }

    public void setDestBvn(String destBvn) {
        this.destBvn = destBvn;
    }

    public String getDestAmount() {
        return destAmount;
    }

    public void setDestAmount(String destAmount) {
        this.destAmount = destAmount;
    }

    public String getDestInstitutioncode() {
        return destInstitutioncode;
    }

    public void setDestInstitutioncode(String destInstitutioncode) {
        this.destInstitutioncode = destInstitutioncode;
    }

    public String getDestResponseCode() {
        return destResponseCode;
    }

    public void setDestResponseCode(String destResponseCode) {
        this.destResponseCode = destResponseCode;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public String getTransactiondate() {
        return transactiondate;
    }

    public void setTransactiondate(String transactiondate) {
        this.transactiondate = transactiondate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getResponseCodeDefinition() {
        return responseCodeDefinition;
    }

    public void setResponseCodeDefinition(String responseCodeDefinition) {
        this.responseCodeDefinition = responseCodeDefinition;
    }
    
}
