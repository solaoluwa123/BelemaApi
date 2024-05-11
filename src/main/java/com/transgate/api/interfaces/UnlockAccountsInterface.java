/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

/**
 *
 * @author Makintola
 */
public interface UnlockAccountsInterface {
    public void Unlock();
    public void ReduceLockTime();
    public void AutoPassDisputesForSettlement();
    public void SendAllAcceptedDisputes();
    public void SendDisputesReminders();
}
