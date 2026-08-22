/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

import com.transgate.api.models.NetworkResponse;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Makintola
 */
public interface UsersInterface {
    public ResponseEntity Login(String username, String password);
    
    public boolean LoginExternal(String username, String password);
    
    public boolean LoginExternal2(String username, String sessionToken);
    
    public ResponseEntity SetUp2FA(String sessiontoken, String username, int enable);
    
    public ResponseEntity Login2FA(String sessiontoken, String username, String password);
    
    public ResponseEntity GetUsers(boolean systemUsers);
    
    public ResponseEntity GetUserById(String sessiontoken, int userid);
    
    public ResponseEntity GetRoles();
    
    public ResponseEntity SendPasswordRecoveryCode(String email);
    
    public ResponseEntity ResetPassword(String code, String password, String token);
    
    public ResponseEntity ActivateAccount(String code, String password, String token);
    
    public ResponseEntity Create(String sessiontoken, String creator, String username, String firstname, String surname, String phone_number, String email_address, int roleid, String password);
    
    public ResponseEntity CreateOther(String sessiontoken, String creator, String username, String firstname, String surname, String phone_number, String email_address, int roleid, String password, String institutionid_as_string, String institutionname);

    public ResponseEntity Delete(String sessiontoken, int userid, String username);
    
    public ResponseEntity Edit(String sessiontoken, int userid, String firstname, String surname, String phone_number, int roleid, String username, String email_address);
    
    public ResponseEntity UpdateNames(String sessiontoken, String firstname, String surname, String phone_number, String username);
    
    public ResponseEntity UpdatePassword(String sessiontoken, String security, String session_token, String username);
    
    public ResponseEntity Reset2FA(String sessiontoken, int userid, String email_address);
    
    public ResponseEntity UserApprovals(String sessiontoken, int id, String actionType, String username, boolean isContact, String financialInstitutionCode);
    
    public ResponseEntity GetUsersForActions(boolean systemUsers);
    
    public ResponseEntity GetContactsForActions();
    
    public ResponseEntity ClearUserSession(String sessiontoken, String username);
}
