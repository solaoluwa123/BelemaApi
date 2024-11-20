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
public class TransactionHalfModel {
    String srcSessionid, srcAccountName, destAccountName, srcAmount, transactiondate, responseCodeDefinition, srcResponsecode;
    String srcAccountKYC, destAccountKYC, srcAccountNumber, destAccountNumber, srcAccountBank, destAccountBank, destNodeInstitutionName;
    public String getSessionid() {  
        return srcSessionid;
    }

    public void setSessionid(String srcSessionid) {
        this.srcSessionid = srcSessionid;
    }

    public String getAmount() {
        return srcAmount;
    }

    public void setAmount(String srcAmount) {
        this.srcAmount = srcAmount;
    }

    public String getTransactiondate() {
        return transactiondate;
    }

    public void setTransactiondate(String transactiondate) {
        this.transactiondate = transactiondate;
    }

    public String getResponsecodedefinition() {
        return responseCodeDefinition;
    }

    public void setResponsecodedefinition(String responseCodeDefinition) {
        this.responseCodeDefinition = responseCodeDefinition;
    }

    public String getResponsecode() {
        return srcResponsecode;
    }

    public void setResponsecode(String srcResponsecode) {
        this.srcResponsecode = srcResponsecode;
    }    

    public String getSrcAccountName() {
        return srcAccountName;
    }

    public void setSrcAccountName(String srcAccountName) {
        this.srcAccountName = srcAccountName;
    }

    public String getDestAccountName() {
        return destAccountName;
    }

    public void setDestAccountName(String destAccountName) {
        this.destAccountName = destAccountName;
    }

    public String getSrcAccountKYC() {
        return srcAccountKYC;
    }

    public void setSrcAccountKYC(String srcAccountKYC) {
        this.srcAccountKYC = srcAccountKYC;
    }

    public String getDestAccountKYC() {
        return destAccountKYC;
    }

    public void setDestAccountKYC(String destAccountKYC) {
        this.destAccountKYC = destAccountKYC;
    }

    public String getSrcAccountNumber() {
        return srcAccountNumber;
    }

    public void setSrcAccountNumber(String srcAccountNumber) {
        this.srcAccountNumber = srcAccountNumber;
    }

    public String getDestAccountNumber() {
        return destAccountNumber;
    }

    public void setDestAccountNumber(String destAccountNumber) {
        this.destAccountNumber = destAccountNumber;
    }

    public String getSrcAccountBank() {
        return srcAccountBank;
    }

    public void setSrcAccountBank(String srcAccountBank) {
        this.srcAccountBank = srcAccountBank;
    }

    public String getDestAccountBank() {
        return destAccountBank;
    }

    public void setDestAccountBank(String destAccountBank) {
        this.destAccountBank = destAccountBank;
    }

    public String getDestNodeInstitutionName() {
        return destNodeInstitutionName;
    }

    public void setDestNodeInstitutionName(String destNodeInstitutionName) {
        this.destNodeInstitutionName = destNodeInstitutionName;
    }
    
}
