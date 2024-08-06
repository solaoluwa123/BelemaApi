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
public class DisputeModel extends FullTransactionModel {
    String type, loggedBy, dateModified, dateCreated;
    int transactionId, status, resolved;
    String resolvedBy, records, timeline_date, proof_of_reject_uri;
    String loggingInstitution;
    String selectedDisputes;
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(String loggedBy) {
        this.loggedBy = loggedBy;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getResolved() {
        return resolved;
    }

    public void setResolved(int resolved) {
        this.resolved = resolved;
    }
    
    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getRecords() {
        return records;
    }

    public void setRecords(String records) {
        this.records = records;
    }

    public String getTimeline_date() {
        return timeline_date;
    }

    public void setTimeline_date(String timeline_date) {
        this.timeline_date = timeline_date;
    }

    public String getProof_of_reject_uri() {
        return proof_of_reject_uri;
    }

    public void setProof_of_reject_uri(String proof_of_reject_uri) {
        this.proof_of_reject_uri = proof_of_reject_uri;
    }

    public String getLoggingInstitution() {
        return loggingInstitution;
    }

    public void setLoggingInstitution(String loggingInstitution) {
        this.loggingInstitution = loggingInstitution;
    }

    public String getSelectedDisputes() {
        return selectedDisputes;
    }

    public void setSelectedDisputes(String selectedDisputes) {
        this.selectedDisputes = selectedDisputes;
    }
    
}
