/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

import org.springframework.http.ResponseEntity;

/**
 *
 * @author Makintola
 */
public interface UsersInterface {
    public ResponseEntity Login(String username, String password);
    
    public ResponseEntity GetUsers();
    
    public ResponseEntity GetUserById(String sessiontoken, int userid);
    
    public ResponseEntity GetRoles();
    
    public ResponseEntity SendPasswordRecoveryCode(String email);
    
    public ResponseEntity ResetPassword(String code, String password, String token);
    
    public ResponseEntity Create(String sessiontoken, String creator, String username, String firstname, String surname, String phone_number, String email_address, int roleid, String password);

    public ResponseEntity Delete(String sessiontoken, int userid, String username);
    
    public ResponseEntity Edit(String sessiontoken, int userid, String firstname, String surname, String phone_number, int roleid, String username);
    
    public ResponseEntity UserApprovals(String sessiontoken, int id, String actionType, String username);
    
    public ResponseEntity GetUsersForActions();
    
    public ResponseEntity ClearUserSession(String sessiontoken, String username);
}
