/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.models;

/**
 *
 * @author Olawoyin Samson
 */
public class CardsTransactionModel{
    int id;
    String message_type, bin, processing_code, system_trace_number;
    String response_code, transaction_date, transaction_time, amount;
    String retrieval_ref_number, acquirer_institution_id, pan, terminal_id;
    String merchant_id, location_name_address, ncs_date_time, destination_acquiring_institution_id;
    String encrypted_pan, encrypted_expiry_date, approval_code;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getProcessing_code() {
        return processing_code;
    }

    public void setProcessing_code(String processing_code) {
        this.processing_code = processing_code;
    }

    public String getSystem_trace_number() {
        return system_trace_number;
    }

    public void setSystem_trace_number(String system_trace_number) {
        this.system_trace_number = system_trace_number;
    }

    public String getResponse_code() {
        return response_code;
    }

    public void setResponse_code(String response_code) {
        this.response_code = response_code;
    }

    public String getTransaction_date() {
        return transaction_date;
    }

    public void setTransaction_date(String transaction_date) {
        this.transaction_date = transaction_date;
    }

    public String getTransaction_time() {
        return transaction_time;
    }

    public void setTransaction_time(String transaction_time) {
        this.transaction_time = transaction_time;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getRetrieval_ref_number() {
        return retrieval_ref_number;
    }

    public void setRetrieval_ref_number(String retrieval_ref_number) {
        this.retrieval_ref_number = retrieval_ref_number;
    }

    public String getAcquirer_institution_id() {
        return acquirer_institution_id;
    }

    public void setAcquirer_institution_id(String acquirer_institution_id) {
        this.acquirer_institution_id = acquirer_institution_id;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getTerminal_id() {
        return terminal_id;
    }

    public void setTerminal_id(String terminal_id) {
        this.terminal_id = terminal_id;
    }

    public String getMerchant_id() {
        return merchant_id;
    }

    public void setMerchant_id(String merchant_id) {
        this.merchant_id = merchant_id;
    }

    public String getLocation_name_address() {
        return location_name_address;
    }

    public void setLocation_name_address(String location_name_address) {
        this.location_name_address = location_name_address;
    }

    public String getNcs_date_time() {
        return ncs_date_time;
    }

    public void setNcs_date_time(String ncs_date_time) {
        this.ncs_date_time = ncs_date_time;
    }

    public String getDestination_acquiring_institution_id() {
        return destination_acquiring_institution_id;
    }

    public void setDestination_acquiring_institution_id(String destination_acquiring_institution_id) {
        this.destination_acquiring_institution_id = destination_acquiring_institution_id;
    }

    public String getEncrypted_pan() {
        return encrypted_pan;
    }

    public void setEncrypted_pan(String encrypted_pan) {
        this.encrypted_pan = encrypted_pan;
    }

    public String getEncrypted_expiry_date() {
        return encrypted_expiry_date;
    }

    public void setEncrypted_expiry_date(String encrypted_expiry_date) {
        this.encrypted_expiry_date = encrypted_expiry_date;
    }

    public String getApproval_code() {
        return approval_code;
    }

    public void setApproval_code(String approval_code) {
        this.approval_code = approval_code;
    }
    
    
}
