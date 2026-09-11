package com.transgate.api.models;

/**
 * One time bucket in a live monitoring chart.
 * {@code inflow}/{@code outflow} are success rates (%); count fields are volumes.
 */
public class LiveMonitoringTimePointModel {
    private String time;
    private double inflow;
    private double outflow;
    private long inflowTotal;
    private long inflowSuccessCount;
    private long outflowTotal;
    private long outflowSuccessCount;

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

    public long getInflowTotal() {
        return inflowTotal;
    }

    public void setInflowTotal(long inflowTotal) {
        this.inflowTotal = inflowTotal;
    }

    public long getInflowSuccessCount() {
        return inflowSuccessCount;
    }

    public void setInflowSuccessCount(long inflowSuccessCount) {
        this.inflowSuccessCount = inflowSuccessCount;
    }

    public long getOutflowTotal() {
        return outflowTotal;
    }

    public void setOutflowTotal(long outflowTotal) {
        this.outflowTotal = outflowTotal;
    }

    public long getOutflowSuccessCount() {
        return outflowSuccessCount;
    }

    public void setOutflowSuccessCount(long outflowSuccessCount) {
        this.outflowSuccessCount = outflowSuccessCount;
    }
}
