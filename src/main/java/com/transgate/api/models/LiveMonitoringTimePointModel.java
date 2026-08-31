package com.transgate.api.models;

/**
 * One time bucket in a live monitoring chart.
 */
public class LiveMonitoringTimePointModel {
    private String time;
    private double inflow;
    private double outflow;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getInflow() {
        return inflow;
    }

    public void setInflow(double inflow) {
        this.inflow = inflow;
    }

    public double getOutflow() {
        return outflow;
    }

    public void setOutflow(double outflow) {
        this.outflow = outflow;
    }
}
