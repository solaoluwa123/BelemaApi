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
public class CardsDisputeModel extends CardsTransactionModel {
    String logged_by, date_modified, date_created, type;
    int transaction_id, status, resolved;
    String resolved_by, records, timeline_date, proof_of_reject_uri, proof_of_debit_uri, cardholder_acct_nuban, cardholder_acct_number;
    String result, arbitrated_by, date_arbitrated, arbitration_closed_by, arbitrated_proof_uri, arbitration_closed_date;

    public String getLogged_by() {
        return logged_by;
    }

    public void setLogged_by(String logged_by) {
        this.logged_by = logged_by;
    }

    public String getDate_modified() {
        return date_modified;
    }

    public void setDate_modified(String date_modified) {
        this.date_modified = date_modified;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(int transaction_id) {
        this.transaction_id = transaction_id;
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

    public String getResolved_by() {
        return resolved_by;
    }

    public void setResolved_by(String resolved_by) {
        this.resolved_by = resolved_by;
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

    public String getCardholder_acct_nuban() {
        return cardholder_acct_nuban;
    }

    public void setCardholder_acct_nuban(String cardholder_acct_nuban) {
        this.cardholder_acct_nuban = cardholder_acct_nuban;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getProof_of_debit_uri() {
        return proof_of_debit_uri;
    }

    public void setProof_of_debit_uri(String proof_of_debit_uri) {
        this.proof_of_debit_uri = proof_of_debit_uri;
    }

    public String getArbitrated_by() {
        return arbitrated_by;
    }

    public void setArbitrated_by(String arbitrated_by) {
        this.arbitrated_by = arbitrated_by;
    }

    public String getDate_arbitrated() {
        return date_arbitrated;
    }

    public void setDate_arbitrated(String date_arbitrated) {
        this.date_arbitrated = date_arbitrated;
    }

    public String getArbitration_closed_by() {
        return arbitration_closed_by;
    }

    public void setArbitration_closed_by(String arbitration_closed_by) {
        this.arbitration_closed_by = arbitration_closed_by;
    }

    public String getArbitrated_proof_uri() {
        return arbitrated_proof_uri;
    }

    public void setArbitrated_proof_uri(String arbitrated_proof_uri) {
        this.arbitrated_proof_uri = arbitrated_proof_uri;
    }

    public String getArbitration_closed_date() {
        return arbitration_closed_date;
    }

    public void setArbitration_closed_date(String arbitration_closed_date) {
        this.arbitration_closed_date = arbitration_closed_date;
    }
    
}
