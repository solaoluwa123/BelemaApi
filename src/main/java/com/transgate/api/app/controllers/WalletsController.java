/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.models.WalletModel;
import com.transgate.api.util.ResponseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.transgate.api.interfaces.WalletsInterface;
import com.transgate.api.util.Validators;
import java.math.BigDecimal;

/**
 *
 * @author Makintola
 */
@RestController
public class WalletsController {
    
    @Autowired
    private WalletsInterface walletInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    Validators validators = new Validators();
    
    @RequestMapping(value = "/wallets/create", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody WalletModel wallet) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return walletInterface.Create(sessiontoken, wallet.getWalletname(), wallet.getFinancialInstitutionCode(), wallet.getCreator(), wallet.getWallettype());
    }
    
    @RequestMapping(value = "/wallets/get", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetAll(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return walletInterface.GetWallets();
    }
    
    @RequestMapping(value = "/wallets/get/{institutioncode}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetAll(@RequestHeader(value = "Authorization") String header, @PathVariable ("institutioncode") String institutioncode) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return walletInterface.GetWallets(institutioncode);
    }
    
    @RequestMapping(value = "/wallet/{walletnumber}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetWalletByNumber(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("walletnumber") String walletnumber) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return walletInterface.GetWalletByNumber(walletnumber);
    }
    
    @RequestMapping(value = "/wallet/activity/{walletnumber}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetWalletActivity(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("walletnumber") String walletnumber) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return walletInterface.GetWalletActivity(walletnumber);
    }
    
    @RequestMapping(value = "/wallets/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetWalletsForActions(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return walletInterface.GetWalletsForActions();
    }
    
    @RequestMapping(value = "/wallets/initiate-debit-credit", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity InitiateDebitCreditWallet(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody WalletModel wallet) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String walletnumber = wallet.getWalletnumber();
        String actionType = wallet.getActionType();
        BigDecimal amount = wallet.getAmount();
        String fundby = wallet.getCreator();
        double walletbalance = wallet.getBalance().doubleValue();
        if (!actionType.equals("cr") && !actionType.equals("dr"))
            return responseManager.ResponseBadRequest();
        if (amount.doubleValue() < 100 || (actionType.equals("dr") && walletbalance < 100))
            return responseManager.ResponseUnathorized();
        if (amount.doubleValue() > walletbalance && actionType.equals("dr"))
            return responseManager.ResponseBadRequest();
        return walletInterface.InitiateDebitCreditWallet(sessiontoken, walletnumber, actionType, amount, fundby);
    }
    
    @RequestMapping(value = "/wallets/map-wallet-to-user", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity MapWalletToUser(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody WalletModel wallet) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }

        return walletInterface.MapWalletToUser(sessiontoken, wallet.getWalletnumber(), wallet.getAssignnee(), wallet.getCreator());
    }
    
    @RequestMapping(value = "/wallets/{walletnumber}/{username}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity DeleteWallet(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("walletnumber") String walletnumber, @PathVariable("username") String username) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }

        return walletInterface.DeleteWallet(sessiontoken, walletnumber, username);
    }
    
    @RequestMapping(value = "/wallets/edit", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity EditWallet(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody WalletModel wallet) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }

        return walletInterface.EditWallet(sessiontoken, wallet.getWalletnumber(), wallet.getWalletname(), wallet.getFinancialInstitutionCode(), wallet.getCreator());
    }
    
    @RequestMapping(value = "/wallets/approval", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity WalletApprovals(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody WalletModel wallet) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }

        return walletInterface.WalletApprovals(sessiontoken, wallet.getId(), wallet.getActionType(), wallet.getCreator());
    }
}
