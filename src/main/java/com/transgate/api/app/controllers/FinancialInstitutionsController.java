/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import com.transgate.api.interfaces.FinancialInstitutionsInterface;
import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.models.FinancialInstitutionModel;
import com.transgate.api.models.UserModel;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
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
public class FinancialInstitutionsController {
    
    @Autowired
    private FinancialInstitutionsInterface financialInstitutionsInterface;
    
    ResponseManager responseManager = new ResponseManager();
    
    Validators validators = new Validators();
    
    @Autowired
    GenericInterface GenericInterface;
    
    @RequestMapping(value = "/financial-institutions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Get(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetFinancialInstitutions();
    }
    
    @RequestMapping(value = "/financial-institutions/{code}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetFinancialInstitutionByCode(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("code") String code) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetFinancialInstitutionByCode(sessiontoken, code);
    }
    
    @RequestMapping(value = "/financial-institutions", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Create(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody FinancialInstitutionModel financialInstitutionModel) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.Create(sessiontoken, financialInstitutionModel.getName(), financialInstitutionModel.getShortName(), financialInstitutionModel.getColor(), financialInstitutionModel.getCode(), financialInstitutionModel.getBusiness_address(), financialInstitutionModel.getBusinessType(), financialInstitutionModel.getCreated_by());
    }
    
    @RequestMapping(value = "/financial-institutions/types", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetTypes(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetFinancialInstitutionsTypes();
    }
    
    @RequestMapping(value = "/financial-institutions/types/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetFinancialInstitutionsTypeById(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetFinancialInstitutionsTypeById(sessiontoken, id);
    }
    
    @RequestMapping(value = "/financial-institutions/{code}/{username}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity Delete(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("code") String code, @PathVariable("username") String username) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.Delete(sessiontoken, code, username);
    }
    
    @RequestMapping(value = "/financial-institutions/{code}/{username}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity Activate(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("code") String code, @PathVariable("username") String username) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.Activate(sessiontoken, code, username);
    }
    
    @RequestMapping(value = "/financial-institutions", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody FinancialInstitutionModel institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.Edit(sessiontoken, institution.getCode(), institution.getName(), institution.getShortName(), institution.getColor(), institution.getBusiness_address(), institution.getBusinessType(), institution.getCreated_by());
    }
    
    @RequestMapping(value = "/financial-institutions/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetWalletsForActions(@RequestHeader(value = "Authorization") String header) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetFinancialInstitutionsForActions();
    }
    
    @RequestMapping(value = "/financial-institutions/approval", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity UserApprovals(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody FinancialInstitutionModel institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.FinancialInstitutionApprovals(sessiontoken, institution.getId(), institution.getActionType(), institution.getCreated_by());
    }
    
    @RequestMapping(value = "/financial-institutions/reject/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity Reject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return GenericInterface.DeleteHelper(sessiontoken, id, "transgateweb_db.tbl_financial_institutions_pendings", "Accounts Institution");
    }
    
    @RequestMapping(value = "/financial-institutions/reject", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity FinancialInstitutionReject(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody FinancialInstitutionModel institution) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.FinancialInstitutionReject(sessiontoken, institution.getId(), institution.getActionType(), institution.getCreated_by());
    }
    
    @RequestMapping(value = "/financial-institutions/contacts", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity CreateContact(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.CreateContact(sessiontoken, user.getRole(), user.getInstitution(), user.getFirstname(), user.getSurname(), user.getPhone_number(), user.getEmail_address(), user.getSecurity());
    }
    
    @RequestMapping(value = "/financial-institutions/contacts/{id}/{username}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity DeleteContact(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable("id") int id, @PathVariable("username") String username) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.DeleteContact(sessiontoken, id, username);
    }
    
    @RequestMapping(value = "/financial-institutions/contacts/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetContactById(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken, @PathVariable ("id") int id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetContactById(sessiontoken, id);
    }
    
    @RequestMapping(value = "/financial-institutions/contacts", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity Edit(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @RequestBody UserModel user) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.EditContact(sessiontoken, user.getId(), user.getFirstname(), user.getSurname(), user.getPhone_number(), user.getUsername());
    }
    
    @RequestMapping(value = "/financial-institutions/contacts", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetContacts(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetAllContacts(sessiontoken);
    }
    
    @RequestMapping(value = "/financial-institutions/contacts/institution/{code}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetContactsByInstitution(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable ("code") String code) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetAllContacts(code);
    }
    
    @RequestMapping(value = "/financial-institutions/contacts/get/actions", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetContactsForAction(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetAllContactsForActions(sessiontoken);
    }
    
    @RequestMapping(value = "/financial-institutions/contacts/institution/get/actions/{code}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity GetContactsByInstitutionForAction(@RequestHeader(value = "Authorization") String header, @RequestHeader(value = "auth-token") String sessiontoken,
            @PathVariable ("code") String code) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return financialInstitutionsInterface.GetAllContactsForActions(code);
    }
}