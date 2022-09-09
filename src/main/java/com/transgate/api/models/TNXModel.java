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
    ArrayList inflows, outflows;

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
}
