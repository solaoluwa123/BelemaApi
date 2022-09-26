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
public class TNXModel {
    ArrayList inflows, outflows, successVolumes, failedVolumes, totalVolumes;
    String total;

    public ArrayList getInflows() {
        return inflows;
    }

    public void setInflows(ArrayList inflows) {
        this.inflows = inflows;
    }

    public ArrayList getOutflows() {
        return outflows;
    }

    public void setOutflows(ArrayList outflows) {
        this.outflows = outflows;
    }

    public ArrayList getSuccessVolumes() {
        return successVolumes;
    }

    public void setSuccessVolumes(ArrayList successVolumes) {
        this.successVolumes = successVolumes;
    }

    public ArrayList getFailedVolumes() {
        return failedVolumes;
    }

    public void setFailedVolumes(ArrayList failedVolumes) {
        this.failedVolumes = failedVolumes;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public ArrayList getTotalVolumes() {
        return totalVolumes;
    }

    public void setTotalVolumes(ArrayList totalVolumes) {
        this.totalVolumes = totalVolumes;
    }
    
}
