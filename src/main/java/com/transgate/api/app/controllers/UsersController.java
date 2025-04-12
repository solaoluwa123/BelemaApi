/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.interfaces.UnlockAccountsInterface;
import com.transgate.api.interfaces.UsersInterface;
import com.transgate.api.models.LoginRequest;
import com.transgate.api.models.LoginResponse;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.UserModel;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.app.services.Validators;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Makintola
 */
@RestController
public class UsersController {
    
    @Autowired
    private UsersInterface usersInterface;
    
    @Autowired
    private UnlockAccountsInterface unlockAccountsInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    private final Validators validators;
    // Constructor injection for RestCall
    public UsersController(Validators validators) {
        this.validators = validators;
    }
    
    @Autowired
    GenericInterface GenericInterface;
    
    private Logger logger = Logger.getLogger(UsersController.class.getName());
    
    @RequestMapping(value = "/users/crons/unlock", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Unlock() {
        unlockAccountsInterface.Unlock();
        return responseManager.ResponseAccepted();
    }
    
    @RequestMapping(value = "/users/crons/reducelocktime", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity ReduceLockTime() {
        unlockAccountsInterface.ReduceLockTime();
        return responseManager.ResponseAccepted();
    }
    
    @RequestMapping(value = "/app/crons/autopassdisputesforsettlement", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity AutoPassDisputesForSettlement() {
        unlockAccountsInterface.AutoPassDisputesForSettlement();
        return responseManager.ResponseAccepted();
    }
    
    @RequestMapping(value = "/app/crons/sendaccepteddisputes", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SendAllAcceptedDisputes() {
        unlockAccountsInterface.SendAllAcceptedDisputes();
        return responseManager.ResponseAccepted();
    }
    
    @RequestMapping(value = "/app/crons/senddisputesreminders", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity SendDisputesReminders() {
        unlockAccountsInterface.SendDisputesReminders();
        return responseManager.ResponseAccepted();
    }
    
    @RequestMapping(value = "/user/generate-token", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity SignUser(@RequestHeader(value = "auth") String header,
            @RequestBody LoginRequest login) {
        NetworkResponse response = new NetworkResponse();
        if (validators.validHeaderExternal().equals(header)) {
            String token = validators.GenerateJSONWebToken(login.getUsername());
            response.setMessage(token);
            response.setCode(200);
            response.setStatus("success");
            return responseManager.ResponseOk(response);
        } else {     
            boolean validUser = usersInterface.LoginExternal(login.getUsername(), header.split(" ")[1]);
            if (validUser) {
                String token = validators.GenerateJSONWebToken(login.getUsername());
                response.setMessage(token);
                response.setCode(200);
                response.setStatus("success");
                return responseManager.ResponseOk(response);
            }
        }
        return responseManager.InvalidAuthorizationHeader();
    }
    
    @RequestMapping(value = "/users/login", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity Login(@RequestHeader(value = "Authorization") String header,
            @RequestBody LoginRequest login) {
        logger.info(String.format("%s :: LoginUser() :: header --->   %s", logger.getName(), header)); 
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.Login(login.getUsername(), login.getPassword());
    }
    
    @RequestMapping(value = "/users/setup-2fa", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity SetUp2FA(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.SetUp2FA(sessiontoken, user.getUsername(), user.getId());
    }
    
    @RequestMapping(value = "/users/login-2fa", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity Login2FA(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody LoginRequest login) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.Login2FA(sessiontoken, login.getUsername(), login.getPassword());
    }
    
    @RequestMapping(value = "/users/resetpassword", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity ResetPassword(@RequestHeader(value = "Authorization") String header,
            @RequestBody LoginResponse login) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.ResetPassword(login.getUsername(), login.getSurname(), login.getFirstname());
    }
    
    @RequestMapping(value = "/users/activateaccount", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity ActivateAccount(@RequestHeader(value = "Authorization") String header,
            @RequestBody LoginResponse login) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.ActivateAccount(login.getUsername(), login.getSurname(), login.getFirstname());
    }
    
    @RequestMapping(value = "/users/recoverpassword", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity SendPasswordRecoveryCode(@RequestHeader(value = "Authorization") String header,
            @RequestBody LoginRequest login) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.SendPasswordRecoveryCode(login.getUsername());
    }
    
    @RequestMapping(value = "/users/logout", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity Logout(@RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody LoginRequest login) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.ClearUserSession(sessiontoken, login.getUsername());
    }
    
    @RequestMapping(value = "/users/get", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetUsers(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.GetUsers(true);
    }
    
    @RequestMapping(value = "/other-users/get", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetOtherUsers(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.GetUsers(false);
    }
    
    @RequestMapping(value = "/roles/get", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetRoles(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.GetRoles();
    }
    
    @RequestMapping(value = "/users/create", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.Create(sessiontoken, user.getRole(), user.getUsername(), user.getFirstname(), user.getSurname(), user.getPhone_number(), user.getEmail_address(), user.getRoleid(), user.getSecurity());
    }
    
    @RequestMapping(value = "/other-users/create", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity CreateOther(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.CreateOther(sessiontoken, user.getRole(), user.getUsername(), user.getFirstname(), user.getSurname(), user.getPhone_number(), user.getEmail_address(), user.getRoleid(), user.getSecurity(), user.getInstitutionid_as_string(), user.getInstitutionName());
    }
    
    @RequestMapping(value = "/users/{userid}/{username}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("userid") int userid, @PathVariable("username") String username) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.Delete(sessiontoken, userid, username);
    }
    
    @RequestMapping(value = "/users/{userid}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetUserById(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("userid") int userid) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.GetUserById(sessiontoken, userid);
    }
    
    @RequestMapping(value = "/users/edit", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.Edit(sessiontoken, user.getId(), user.getFirstname(), user.getSurname(), user.getPhone_number(), user.getRoleid(), user.getUsername());
    }
    
    @RequestMapping(value = "/users/update-names", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity UpdateNames(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.UpdateNames(sessiontoken, user.getFirstname(), user.getSurname(), user.getPhone_number(), user.getUsername());
    }
    
    @RequestMapping(value = "/users/update-password", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity UpdatePassword(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.UpdatePassword(sessiontoken, user.getSecurity(), user.getSession_token(), user.getUsername());
    }
    
    @RequestMapping(value = "/users/approval", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity UserApprovals(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.UserApprovals(sessiontoken, user.getId(), user.getActionType(), user.getUsername(), false);
    }
    
    @RequestMapping(value = "/users/contact/approval", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity ContactApprovals(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.UserApprovals(sessiontoken, user.getId(), user.getActionType(), user.getUsername(), true);
    }
    
    @RequestMapping(value = "/users/reject/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Reject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "transgateweb_db.tbl_user_details_operations", "Users");
    }
    
    @RequestMapping(value = "/contact/reject/{id}/{email}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity ContactReject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id,
            @PathVariable("email") String email) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        GenericInterface.DeleteHelper(sessiontoken, email, "email_address", "transgateweb_db.tbl_financial_institution_contacts_operations", "Contacts");
        return GenericInterface.DeleteHelper(sessiontoken, id, "transgateweb_db.tbl_user_details_operations", "Contacts");
    }
    
    @RequestMapping(value = "/users/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetUsersForActions(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.GetUsersForActions(true);
    }
    
    @RequestMapping(value = "/other-users/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetOtherUsersForActions(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.GetUsersForActions(false);
    }
    
    @RequestMapping(value = "/contacts/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetContactUsersForActions(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return usersInterface.GetContactsForActions();
    }
}