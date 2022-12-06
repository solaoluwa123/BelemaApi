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
    String srcSessionid, srcAmount, transactiondate, responseCodeDefinition, srcResponsecode;

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
    
}
