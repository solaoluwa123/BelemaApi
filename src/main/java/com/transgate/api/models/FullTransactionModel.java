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
public class FullTransactionModel extends TransactionModel {
    String srcInstitutionName, destInstitutionName, destNodeInstitutionName;

    public String getSrcInstitutionName() {
        return srcInstitutionName;
    }

    public void setSrcInstitutionName(String srcInstitutionName) {
        this.srcInstitutionName = srcInstitutionName;
    }

    public String getDestInstitutionName() {
        return destInstitutionName;
    }

    public void setDestInstitutionName(String destInstitutionName) {
        this.destInstitutionName = destInstitutionName;
    }

    public String getDestNodeInstitutionName() {
        return destNodeInstitutionName;
    }

    public void setDestNodeInstitutionName(String destNodeInstitutionName) {
        this.destNodeInstitutionName = destNodeInstitutionName;
    }
    
}
