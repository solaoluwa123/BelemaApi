package com.transgate.api.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Live monitoring payload for one financial institution.
 */
public class LiveMonitoringInstitutionModel {
    private String name;
    private String institutionCode;
    private String shortName;
    private List<LiveMonitoringTimePointModel> timeSeries = new ArrayList<>();
    private int inflowSuccess;
    private int inflowFailure;
    private int outflowSuccess;
    private int outflowFailure;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public List<LiveMonitoringTimePointModel> getTimeSeries() {
        return timeSeries;
    }

    public void setTimeSeries(List<LiveMonitoringTimePointModel> timeSeries) {
        this.timeSeries = timeSeries;
    }

    public int getInflowSuccess() {
        return inflowSuccess;
    }

    public void setInflowSuccess(int inflowSuccess) {
        this.inflowSuccess = inflowSuccess;
    }

    public int getInflowFailure() {
        return inflowFailure;
    }

    public void setInflowFailure(int inflowFailure) {
        this.inflowFailure = inflowFailure;
    }

    public int getOutflowSuccess() {
        return outflowSuccess;
    }

    public void setOutflowSuccess(int outflowSuccess) {
        this.outflowSuccess = outflowSuccess;
    }

    public int getOutflowFailure() {
        return outflowFailure;
    }

    public void setOutflowFailure(int outflowFailure) {
        this.outflowFailure = outflowFailure;
    }
}
