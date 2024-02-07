/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Makintola
 */
public interface WalletsInterface {

    public ResponseEntity Create(String sessiontoken, String walletname, String institutionCode, String creator, int type);

    public ResponseEntity GetWallets();

    public ResponseEntity GetWallets(String institutioncode);
    
    public ResponseEntity GetWalletActivity(String walletnumber, String startDate, String endDate, int page, int limit);

    public ResponseEntity InitiateDebitCreditWallet(String sessiontoken, String walletnumber, String actionType, BigDecimal amount, String fundby);
    
    public ResponseEntity MapWalletToUser(String sessiontoken, String walletnumber, String assignee, String username);
    
    public ResponseEntity DeleteWallet(String sessiontoken, String walletnumber, String username);
    
    public ResponseEntity EditWallet(String sessiontoken, String walletnumber, String walletname, String institutionCode, String editor);
    
    public ResponseEntity GetWalletsForActions();
    
    public ResponseEntity GetWalletByNumber(String walletnumber);
    
    public ResponseEntity WalletApprovals(String sessiontoken, int id, String actionType, String username);
}
