/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.models;

import java.math.BigDecimal;

/**
 *
 * @author Makintola
 */
public class WalletModel {
    String creator, walletname, walletnumber, date_created, date_updated, financialInstitutionCode, financialInstitutionName, baseAmount;
    int id, wallettype;
    BigDecimal amount;
    BigDecimal balance, lien;
    String actionType, assignnee, note;
    String walletTypeName, status;
    String balanceAsString;

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getWalletname() {
        return walletname;
    }

    public void setWalletname(String walletname) {
        this.walletname = walletname;
    }

    public String getWalletnumber() {
        return walletnumber;
    }

    public void setWalletnumber(String walletnumber) {
        this.walletnumber = walletnumber;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public String getDate_updated() {
        return date_updated;
    }

    public void setDate_updated(String date_updated) {
        this.date_updated = date_updated;
    }

    public String getFinancialInstitutionCode() {
        return financialInstitutionCode;
    }

    public void setFinancialInstitutionCode(String financialInstitutionCode) {
        this.financialInstitutionCode = financialInstitutionCode;
    }

    public String getFinancialInstitutionName() {
        return financialInstitutionName;
    }

    public void setFinancialInstitutionName(String financialInstitutionName) {
        this.financialInstitutionName = financialInstitutionName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWallettype() {
        return wallettype;
    }

    public void setWallettype(int wallettype) {
        this.wallettype = wallettype;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getAssignnee() {
        return assignnee;
    }

    public void setAssignnee(String assignnee) {
        this.assignnee = assignnee;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getWalletTypeName() {
        return walletTypeName;
    }

    public void setWalletTypeName(String walletTypeName) {
        this.walletTypeName = walletTypeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getLien() {
        return lien;
    }

    public void setLien(BigDecimal lien) {
        this.lien = lien;
    }

    public String getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(String baseAmount) {
        this.baseAmount = baseAmount;
    }
}
