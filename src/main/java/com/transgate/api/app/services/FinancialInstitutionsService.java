/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.FinancialInstitutionsInterface;
import com.transgate.api.interfaces.UsersInterface;
import com.transgate.api.models.FinancialInstitutionModel;
import com.transgate.api.models.InstitutionTypesModel;
import com.transgate.api.models.LoginResponse;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.UserModel;
import com.transgate.api.util.Constants;
import com.transgate.api.util.Randomizer;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 *
 * @author Makintola
 */
@Service
public class FinancialInstitutionsService implements FinancialInstitutionsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;
    
    @Autowired
    private UsersInterface usersInterface;

    ResponseManager responseManager = new ResponseManager();
    Randomizer randomizer = new Randomizer();
    Validators validators = new Validators();
    
    private boolean CheckExistingContact(String email_address, String institution) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_financial_institution_contacts WHERE financial_institution_code = ? AND email_address = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{institution, email_address}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckInstitutionActionPending(String code, String actionType) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_financial_institutions_pendings WHERE code = ? AND actionType = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{code, actionType}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private List<FinancialInstitutionModel> GetInstitutionsFromPendings(int id, String actionType) {
        try {
            String SQL;
            SQL = "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, n.date_created, "
                    + "a.shortName, a.color, a.businessType, a.actionType, a.note, a.business_address, b.name as businessTypeName "
                    + "FROM tbl_nodes_pendings n "
                    + "LEFT JOIN tbl_financial_institutions_pendings a "
                    + "ON n.institution_code = a.code "
                    + "LEFT JOIN tbl_institution_types b "
                    + "ON a.businessType = b.id "
                    + "WHERE n.id = ? AND a.actionType = ?";
            List<FinancialInstitutionModel> institution = jdbcTemplate.query(SQL, new Object[]{id, actionType}, new FinancialInstitutionMapper2());
            return institution;
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return null;
        }
    }
    
    private int CheckCodeAndName(String name, String code) {
        int totalRows = 0;
        try {
            String SQL = "SELECT COUNT(*) FROM tbl_financial_institutions WHERE name = ? OR code = ?";
            totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{name, code}, int.class);
            return totalRows;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return -1;
        }
    }
    
    private int GetUserRole(String username, String session_token) {
        try {
            int role;

            String SQL = "SELECT role FROM tbl_user_details WHERE email_address = ? OR username = ? AND deleted = 0 AND session_token = ?";
            role = jdbcTemplate.queryForObject(SQL, new Object[]{username, username, session_token}, int.class);
            return role;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage() + "------------");
            return -100;
        }
    }
    
    private boolean CheckContactPending(String email_address, String actionType) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_financial_institution_contacts_operations WHERE  email_address = ? AND actionType = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{email_address, actionType}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutionsForActions() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, n.date_created, "
                    + "a.shortName, a.color, a.businessType, a.actionType, a.note, a.business_address, b.name as businessTypeName "
                    + "FROM tbl_nodes_pendings n "
                    + "LEFT JOIN tbl_financial_institutions_pendings a "
                    + "ON n.institution_code = a.code "
                    + "LEFT JOIN tbl_institution_types b "
                    + "ON a.businessType = b.id "
                    + "ORDER BY n.id DESC";
            List<FinancialInstitutionModel> financialInstitutionModel = jdbcTemplate.query(SQL, new FinancialInstitutionMapper2());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institutions");
            networkResponse.setData((ArrayList) financialInstitutionModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutionByCode(String sessiontoken, String code) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, n.is_active as status, n.date_created, "
                    + "a.shortName, a.color, a.businessType, a.business_address, a.date_updated, b.name as businessTypeName, "
                    + "c.charge_amount, c.vat "
                    + "FROM ajiswitch_db.tbl_nodes n "
                    + "LEFT JOIN ajiswitch_db.tbl_charges c "
                    + "ON c.institution_code = n.institution_code "
                    + "LEFT JOIN tbl_financial_institutions a "
                    + "ON n.institution_code = a.code "
                    + "LEFT JOIN tbl_institution_types b "
                    + "ON a.businessType = b.id "
                    + "WHERE n.institution_code = ?";
            
            List<FinancialInstitutionModel> financialInstitutionModel = jdbcTemplate.query(SQL, new Object[]{code}, new FinancialInstitutionMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Get financial institution " + code + " detail");
            networkResponse.setData((ArrayList) financialInstitutionModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutions() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, n.is_active as status, n.date_created, "
                    + "a.shortName, a.color, a.businessType, a.business_address, a.date_updated, b.name as businessTypeName, "
                    + "c.charge_amount, c.vat "
                    + "FROM ajiswitch_db.tbl_nodes n "
                    + "LEFT JOIN ajiswitch_db.tbl_charges c "
                    + "ON c.institution_code = n.institution_code "
                    + "LEFT JOIN tbl_financial_institutions a "
                    + "ON n.institution_code = a.code "
                    + "LEFT JOIN tbl_institution_types b "
                    + "ON a.businessType = b.id "
                    + "ORDER BY n.id DESC";
            List<FinancialInstitutionModel> financialInstitutionModel = jdbcTemplate.query(SQL, new FinancialInstitutionMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institutions");
            networkResponse.setData((ArrayList) financialInstitutionModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Create(String sessiontoken, String name, String shortName, int port, String publickeylocation, String publickeylocationLinux, String cbn_bank_account, String color, String code, String business_address, int businessType, String creator, String password, float charge_amount, float vat, String hashKey) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            int retval, retvalLinux;
            int checkCodeAndName = CheckCodeAndName(name, code);
            if (checkCodeAndName == 0) {
                int userrole = GetUserRole(creator, sessiontoken);
                switch (userrole) {
                    case 1:
                        SQL = "INSERT into ajiswitch_db.tbl_charges(institution_code, charge_amount, vat, date_created, is_active) VALUES(?, ?, ?, now(), '1')";
                        jdbcTemplate.update(SQL, new Object[]{code, charge_amount, vat});
                        SQL = "INSERT into ajiswitch_db.tbl_token_users(institution_name, password) VALUES(?, ENCODE(?, ?))";
                        jdbcTemplate.update(SQL, new Object[]{code, password, Constants.SQL_ENCODE_STRING});
                        SQL = "INSERT into tbl_financial_institutions(code, name, shortName, color, businessType, business_address) VALUES(?, ?, ?, ?, ?, ?)";
                        jdbcTemplate.update(SQL, new Object[]{code, name, shortName, color, businessType, business_address});
                        SQL = "INSERT into ajiswitch_db.tbl_nodes(port_number, is_active, publickeylocation, institution_code, institution_name, date_created, cbn_bank_account, hashkey) VALUES(?, 1, ?, ?, ?, now(), ?, ?)";
                        retval = jdbcTemplate.update(SQL, new Object[]{port, publickeylocation, code, name, cbn_bank_account, hashKey});
                        SQL = "INSERT into ajiswitch_db.tbl_nodes_temp(port_number, is_active, publickeylocation, institution_code, institution_name, date_created, cbn_bank_account, hashkey) VALUES(?, 1, ?, ?, ?, now(), ?, ?)";
                        retvalLinux = jdbcTemplate.update(SQL, new Object[]{port, publickeylocationLinux, code, name, cbn_bank_account, hashKey});
                        if (retval > 0 && retvalLinux > 0) 
                            return responseManager.ResponseAccepted();
                        else 
                            return responseManager.ResponseInternalServerError();
                    case 2:
                        boolean checkPendingAction = CheckInstitutionActionPending(code, "create");
                        if (checkPendingAction) {
                            networkResponse = new NetworkResponse();
                            networkResponse.setCode(200);
                            networkResponse.setStatus("failed");
                            networkResponse.setMessage("Institution already pending create");
                            return responseManager.ResponseOk(networkResponse);
                        }
                        SQL = "INSERT INTO tbl_financial_institutions_pendings(code, name, shortName, color, businessType, actionType, note, business_address) VALUES(?, ?, ?, ?, ?, 'create', 'Create financial institution', ?)";
                        jdbcTemplate.update(SQL, new Object[]{code, name, shortName, color, businessType, business_address});
                        SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created) VALUES(?, 1, ?, ?, ?, now())";
                        retval = jdbcTemplate.update(SQL, new Object[]{port, publickeylocation, code, name});
                        if (retval > 0) 
                            return responseManager.ResponseAccepted();
                        else 
                            return responseManager.ResponseInternalServerError();
                    default:
                        return responseManager.ResponseUnathorized();
                }
            }
            else {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Financial Institution with name or code already exist");
                return responseManager.ResponseOk(networkResponse);
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutionsTypes() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.name, a.date_created "
                    + "FROM tbl_institution_types a "
                    + "ORDER BY a.id DESC";
            List<InstitutionTypesModel> institutionTypesModel = jdbcTemplate.query(SQL, new InstitutionTypesMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institution Types");
            networkResponse.setData((ArrayList) institutionTypesModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutionsTypeById(String sessiontoken, int id) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.name, a.date_created "
                    + "FROM tbl_institution_types a "
                    + "WHERE a.id = ?";
            List<InstitutionTypesModel> institutionTypesModel = jdbcTemplate.query(SQL, new Object[]{id}, new InstitutionTypesMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Financial Institution Type");
            networkResponse.setData((ArrayList) institutionTypesModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Delete(String sessiontoken, String code, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE ajiswitch_db.tbl_nodes SET is_active = -1 WHERE institution_code = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{code});
                    if (retVal > 0){
                        ActivateDeactivateContacts("deactivate", code);
                        return responseManager.ResponseAccepted();
                    }
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckInstitutionActionPending(code, "deactivate");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Institution already pending deactivation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetFinancialInstitutionByCode(sessiontoken, code);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    FinancialInstitutionModel institution = networkResponse != null ? (FinancialInstitutionModel) networkResponse.getData().get(0) : new FinancialInstitutionModel();
                    SQL = "INSERT INTO tbl_financial_institutions_pendings(name, shortName, color, code, businessType, actionType, note, business_address) VALUES(?, ?, ?, ?, ?, 'deactivate', 'Deactivate financial institution', ?)";
                    jdbcTemplate.update(SQL, new Object[]{institution.getName(), institution.getShortName(), institution.getColor(), institution.getCode(), institution.getBusinessType(), institution.getBusiness_address()});
                    SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created) VALUES(?, ?, ?, ?, ?, now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{institution.getPort_number(), institution.getStatus(), institution.getPublickeylocation(), code, institution.getName()});
                    if (retVal > 0) 
                        return responseManager.ResponseAccepted();
                    else 
                        return responseManager.ResponseBadRequest();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Activate(String sessiontoken, String code, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE ajiswitch_db.tbl_nodes SET is_active = 1 WHERE institution_code = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{code});
                    if (retVal > 0){
                        ActivateDeactivateContacts("activated", code);
                        return responseManager.ResponseAccepted();
                    }
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckInstitutionActionPending(code, "activate");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Institution already pending activation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetFinancialInstitutionByCode(sessiontoken, code);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    FinancialInstitutionModel institution = networkResponse != null ? (FinancialInstitutionModel) networkResponse.getData().get(0) : new FinancialInstitutionModel();
                    SQL = "INSERT INTO tbl_financial_institutions_pendings(name, shortName, color, code, businessType, actionType, note, business_address) VALUES(?, ?, ?, ?, ?, 'activate', 'Activate financial institution', ?)";
                    jdbcTemplate.update(SQL, new Object[]{institution.getName(), institution.getShortName(), institution.getColor(), institution.getCode(), institution.getBusinessType(), institution.getBusiness_address()});
                    SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created) VALUES(?, ?, ?, ?, ?, now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{institution.getPort_number(), institution.getStatus(), institution.getPublickeylocation(), code, institution.getName()});
                    if (retVal > 0) 
                        return responseManager.ResponseAccepted();
                    else 
                        return responseManager.ResponseBadRequest();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Edit(String sessiontoken, String code, String name, String shortName, int port, String publickeylocation, String color, String business_address, int businessType, String editor, float charge_amount, float vat) {
        try {
            String SQL;
            int userrole = GetUserRole(editor, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE tbl_financial_institutions SET name = ?, shortName = ?, color = ?, businessType = ?, business_address = ? WHERE code = ?";
                    jdbcTemplate.update(SQL, new Object[]{name, shortName, color, businessType, business_address, code});
                    SQL = "UPDATE ajiswitch_db.tbl_charges SET charge_amount = ?, vat = ? WHERE institution_code = ?";
                    jdbcTemplate.update(SQL, new Object[]{charge_amount, vat, code});
                    SQL = "UPDATE ajiswitch_db.tbl_nodes SET institution_name = ? WHERE institution_code = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{name, code});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseInternalServerError();
                case 2:
                    boolean checkPendingAction = CheckInstitutionActionPending(code, "edit");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Institution already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetFinancialInstitutionByCode(sessiontoken, code);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    FinancialInstitutionModel institution = networkResponse != null ? (FinancialInstitutionModel) networkResponse.getData().get(0) : new FinancialInstitutionModel();
                    responseEntity = GetFinancialInstitutionsTypeById(sessiontoken, businessType);
                    networkResponse = (NetworkResponse) responseEntity.getBody();
                    InstitutionTypesModel typeModel = networkResponse != null ? (InstitutionTypesModel) networkResponse.getData().get(0) : new InstitutionTypesModel();
                    SQL = "INSERT INTO tbl_financial_institutions_pendings(name, shortName, color, code, businessType, actionType, note, business_address) VALUES(?, ?, ?, ?, ?, 'edit', ?, ?)";
                    String note = institution.getName().equals(name) ? "" : "Change institution name from " + institution.getName() + " to " + name;
                    note = institution.getColor().equals(color) ? note : !note.equals("") ? note + ", change color from " + institution.getColor() + " to " + color : "Change color from " + institution.getColor() + " to " + color;
                    note = institution.getShortName().equals(shortName) ? note : !note.equals("") ? note + ", change short name from " + institution.getShortName() + " to " + shortName : "Change short name from " + institution.getShortName() + " to " + shortName;
                    note = institution.getBusiness_address().equals(business_address) ? note : !note.equals("") ? note + ", change address from " + institution.getBusiness_address() + " to " + business_address : "Change address from " + institution.getBusiness_address() + " to " + business_address;
                    note = institution.getBusinessType() == businessType ? note : !note.equals("") ? note + ", change type from " + institution.getBusinessTypeName() + " to " + typeModel.getName() : "Change type from " + institution.getBusinessTypeName() + " to " + typeModel.getName();
                    jdbcTemplate.update(SQL, new Object[]{name, shortName, color, institution.getCode(), businessType, note, business_address});
                    SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created) VALUES(?, ?, ?, ?, ?, now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{port, institution.getStatus(), publickeylocation, code, name});
                    if (retVal > 0) 
                        return responseManager.ResponseAccepted();
                    else 
                        return responseManager.ResponseInternalServerError();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    private void ActivateDeactivateContacts(String action, String institution) {
        int isEnabled = action.equals("deactivate") ? -1 : 1;
        String SQL = "UPDATE sparkpayweb_db.tbl_users a "
                + "LEFT JOIN tbl_financial_institution_contacts b "
                + "ON a.username = b.email_address "
                + "SET a.enabled = ? "
                + "WHERE b.financial_institution_code = ?";
        jdbcTemplate.update(SQL, new Object[]{isEnabled, institution});
    }
    
    @Override
    public ResponseEntity FinancialInstitutionApprovals(String sessiontoken, int id, String actionType, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            int retVal2;
            if (userrole == 1 || userrole == 3) {
                List<FinancialInstitutionModel> institutions = GetInstitutionsFromPendings(id, actionType);
                if (institutions.size() == 1) {
                    switch (actionType) {
                        case "deactivate":
                        case "activate":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = ?";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id, actionType});
                            SQL = "UPDATE ajiswitch_db.tbl_nodes SET is_active = ? WHERE institution_code = ?";
                            int acInt = actionType.equals("deactivate") ? -1 : 1;
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{acInt, institutions.get(0).getCode()});
                            if (retVal > 0 && retVal2 > 0){
                                ActivateDeactivateContacts(actionType, institutions.get(0).getCode());
                                return responseManager.ResponseAccepted();
                            }
                            else
                                return responseManager.ResponseInternalServerError();
                        case "edit":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = 'edit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "UPDATE tbl_financial_institutions SET name = ?, shortName = ?, color = ?, businessType = ?, business_address = ? WHERE code = ?";
                            jdbcTemplate.update(SQL, new Object[]{institutions.get(0).getName(), institutions.get(0).getShortName(), institutions.get(0).getColor(), institutions.get(0).getBusinessType(), institutions.get(0).getBusiness_address(), institutions.get(0).getCode()});
                            SQL = "UPDATE ajiswitch_db.tbl_nodes SET port_number = ?, publickeylocation = ?, institution_name = ? WHERE institution_code = ?";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{institutions.get(0).getPort_number(), institutions.get(0).getPublickeylocation(), institutions.get(0).getName(), institutions.get(0).getCode()});
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "create":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = 'create'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "INSERT into tbl_financial_institutions(name, shortName, color, code, businessType, business_address) VALUES(?, ?, ?, ?, ?, ?)";
                            jdbcTemplate.update(SQL, new Object[]{institutions.get(0).getName(), institutions.get(0).getShortName(), institutions.get(0).getColor(), institutions.get(0).getCode(), institutions.get(0).getBusinessType(), institutions.get(0).getBusiness_address()});
                            SQL = "INSERT into ajiswitch_db.tbl_nodes(port_number, is_active, publickeylocation, institution_code, institution_name, date_created) VALUES(?, 1, ?, ?, ?, now())";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{institutions.get(0).getPort_number(), institutions.get(0).getPublickeylocation(), institutions.get(0).getCode(), institutions.get(0).getName()});
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        default:
                            return responseManager.ResponseBadRequest();
                    }
                }
                else {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(200);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Not found");
                    return responseManager.ResponseNotFound(networkResponse);
                }
            }
            else
                return responseManager.ResponseUnathorized();
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity FinancialInstitutionReject(String sessiontoken, int id, String actionType, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            int retVal2;
            if (userrole == 1 || userrole == 3) {
                List<FinancialInstitutionModel> institutions = GetInstitutionsFromPendings(id, actionType);
                if (institutions.size() == 1) {
                    switch (actionType) {
                        case "deactivate":
                        case "activate":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = ?";
//                            SQL = "DELETE FROM tbl_financial_institutions_pendings WHERE id = ? AND actionType = ?";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id, actionType});
//                            SQL = "UPDATE tbl_financial_institutions SET status = ? WHERE code = ?";
//                            int acInt = actionType.equals("deactivate") ? -1 : 1;
//                            retVal2 = jdbcTemplate.update(SQL, new Object[]{acInt, institutions.get(0).getCode()});
                            if (retVal > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "edit":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = 'edit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
//                            SQL = "UPDATE tbl_financial_institutions SET name = ?, businessType = ?, business_address = ? WHERE code = ?";
//                            retVal2 = jdbcTemplate.update(SQL, new Object[]{institutions.get(0).getName(), institutions.get(0).getBusinessType(), institutions.get(0).getBusiness_address(), institutions.get(0).getCode()});
                            if (retVal > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "create":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = 'create'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
//                            SQL = "INSERT into tbl_financial_institutions(name, code, businessType, date_created, business_address) VALUES(?, ?, ?, now(), ?)";
//                            retVal2 = jdbcTemplate.update(SQL, new Object[]{institutions.get(0).getName(), institutions.get(0).getCode(), institutions.get(0).getBusinessType(), institutions.get(0).getBusiness_address()});
                            if (retVal > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        default:
                            return responseManager.ResponseBadRequest();
                    }
                }
                else {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(200);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Not found");
                    return responseManager.ResponseNotFound(networkResponse);
                }
            }
            else
                return responseManager.ResponseUnathorized();
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetAllContacts(String sessiontoken) {
        return GetAllContacts(sessiontoken, "");
    }
    
    @Override
    public ResponseEntity GetAllContacts(String sessiontoken, String code) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<UserModel> contacts;
            if (!code.equals("")) {
                SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "WHERE a.financial_institution_code = ?";
                contacts = jdbcTemplate.query(SQL, new Object[]{code}, new ContactMapper());
            }
            else {
                SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "ORDER BY a.id DESC";
                contacts = jdbcTemplate.query(SQL, new ContactMapper());
            }
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(!code.equals("") ? "Institution contacts" : "All institution contacts");
            networkResponse.setData((ArrayList) contacts);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetAllContactsForActions(String sessiontoken) {
        return GetAllContactsForActions(sessiontoken, "");
    }
    
    @Override
    public ResponseEntity GetAllContactsForActions(String sessiontoken, String code) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<UserModel> contacts;
            if (!code.equals("")) {
                SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.actionType, a.note, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts_operations a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "WHERE a.financial_institution_code = ?";
                contacts = jdbcTemplate.query(SQL, new Object[]{code}, new ContactMapper2());
            }
            else {
                SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.actionType, a.note, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts_operations a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "ORDER BY a.id DESC";
                contacts = jdbcTemplate.query(SQL, new ContactMapper2());
            }
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(!code.equals("") ? "Institution contacts" : "All institution contacts");
            networkResponse.setData((ArrayList) contacts);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    public ResponseEntity GetContactByEmail(String sessiontoken, String email) {
        UserModel response = new UserModel();
        try {
            String SQL;
            SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "WHERE a.email_address = ?";
            List<UserModel> details = jdbcTemplate.query(SQL, new Object[]{email}, new ContactMapper());
            if (details.size() > 0) {
                response.setCode(200);
                response.setStatus("success");
                response.setMessage("Contact detail");
                response.setId(details.get(0).getId());
                response.setInstitution(details.get(0).getInstitution());
                response.setFirstname(details.get(0).getFirstname());
                response.setSurname(details.get(0).getSurname());
                response.setPhone_number(details.get(0).getPhone_number());
                response.setEmail_address(details.get(0).getEmail_address());
                response.setDate_created(details.get(0).getDate_created());
                response.setInstitutionName(details.get(0).getInstitutionName());
                return responseManager.ResponseOk(response);
            }
            else {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Contact not found");
                return responseManager.ResponseNotFound(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid id");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetContactById(String sessiontoken, int id) {
        UserModel response = new UserModel();
        try {
            String SQL;
            SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "WHERE a.id = ?";
            List<UserModel> details = jdbcTemplate.query(SQL, new Object[]{id}, new ContactMapper());
            if (details.size() > 0) {
                response.setCode(200);
                response.setStatus("success");
                response.setMessage("Contact detail");
                response.setId(details.get(0).getId());
                response.setInstitution(details.get(0).getInstitution());
                response.setFirstname(details.get(0).getFirstname());
                response.setSurname(details.get(0).getSurname());
                response.setPhone_number(details.get(0).getPhone_number());
                response.setEmail_address(details.get(0).getEmail_address());
                response.setDate_created(details.get(0).getDate_created());
                response.setInstitutionName(details.get(0).getInstitutionName());
                return responseManager.ResponseOk(response);
            }
            else {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Contact not found");
                return responseManager.ResponseNotFound(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid id");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity CreateContact(String sessiontoken, String creator, String institution, String firstname, String surname, String phone_number, String email_address, String security){
        try {
            boolean userExist = CheckExistingContact(email_address, institution);
            if (userExist) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Email address already exit");
                return responseManager.ResponseOk(networkResponse);
            }
            String SQL;
            int userrole = GetUserRole(creator, sessiontoken);
            ResponseEntity CreateLoginForContact = usersInterface.Create(sessiontoken, creator, email_address, firstname, surname, phone_number, email_address, 4, security);
            LoginResponse response = (LoginResponse) CreateLoginForContact.getBody();
            if (response.getStatus().equals("success")) {
                switch (userrole) {
                    case 1:
                    case 2:
                        SQL = "INSERT into tbl_financial_institution_contacts(financial_institution_code, firstname, surname, phone_number, email_address, date_created) VALUES(?, ?, ?, ?, ?, now())";
                        int retval = jdbcTemplate.update(SQL, new Object[]{institution, firstname, surname, phone_number, email_address});
                        if (retval > 0) 
                            return responseManager.ResponseAccepted();
                        else 
                            return responseManager.ResponseBadRequest();
                    case 3:
                        boolean userPending = CheckContactPending(email_address, "create");
                        if (userPending) {
                            NetworkResponse networkResponse = new NetworkResponse();
                            networkResponse.setCode(200);
                            networkResponse.setStatus("failed");
                            networkResponse.setMessage("Contact with email - " + email_address + " is already pending for creation");
                            return responseManager.ResponseOk(networkResponse);
                        }
                        SQL = "INSERT INTO tbl_financial_institution_contacts_operations(financial_institution_code, firstname, surname, phone_number, email_address, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, 'create', 'Create contact', now())";
                        retval = jdbcTemplate.update(SQL, new Object[]{institution, firstname, surname, phone_number, email_address});
                        if (retval > 0) 
                            return responseManager.ResponseAccepted();
                        else
                            return responseManager.ResponseInternalServerError();
                    default:
                        return responseManager.ResponseUnathorized();
                }
            }
            else {
                return responseManager.ResponseInternalServerError();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity DeleteContact(String sessiontoken, String email, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "DELETE FROM sparkpayweb_db.tbl_users WHERE username = ?";
                    jdbcTemplate.update(SQL, new Object[]{email});
                    SQL = "DELETE FROM tbl_user_details WHERE username = ? || email_address = ?";
                    jdbcTemplate.update(SQL, new Object[]{email, email});
                    SQL = "DELETE FROM tbl_financial_institution_contacts WHERE email_address = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{email});
                    if (retVal > 0)
                        return responseManager.ResponseDeleted();
                    else
                        return responseManager.ResponseInternalServerError();
//                case 2:
//                    ResponseEntity responseEntity = GetContactByEmail(sessiontoken, email);
//                    UserModel contact = responseEntity != null ? (UserModel) responseEntity.getBody() : new UserModel();
//                    if (contact.getCode() == 404) {
//                        NetworkResponse networkResponse = new NetworkResponse();
//                        networkResponse.setCode(contact.getCode());
//                        networkResponse.setStatus(contact.getStatus());
//                        networkResponse.setMessage(contact.getMessage());
//                        return responseManager.ResponseNotFound(networkResponse);
//                    }
//                    boolean userPending = CheckContactPending(contact.getEmail_address(), "delete");
//                    if (userPending) {
//                        NetworkResponse networkResponse = new NetworkResponse();
//                        networkResponse.setCode(200);
//                        networkResponse.setStatus("failed");
//                        networkResponse.setMessage("Contact already pending for delete");
//                        return responseManager.ResponseOk(networkResponse);
//                    }
//                    SQL = "INSERT INTO tbl_financial_institution_contacts_operations(financial_institution_code, firstname, surname, phone_number, email_address, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, 'delete', 'Delete contact', now())";
//                    retVal = jdbcTemplate.update(SQL, new Object[]{contact.getInstitution(), contact.getFirstname(), contact.getSurname(), contact.getPhone_number(), contact.getEmail_address()});
//                    if (retVal > 0) 
//                        return responseManager.ResponseDeleted();
//                    else
//                        return responseManager.ResponseInternalServerError();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity EditContact(String sessiontoken, int id, String firstname, String surname, String phone_number, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE tbl_financial_institution_contacts SET firstname = ?, surname = ?, phone_number = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{firstname, surname, phone_number, id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseInternalServerError();
                case 2:
                    ResponseEntity responseEntity = GetContactById(sessiontoken, id);
                    UserModel contact = responseEntity != null ? (UserModel) responseEntity.getBody() : new UserModel();
                    if (contact.getCode() == 404) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(contact.getCode());
                        networkResponse.setStatus(contact.getStatus());
                        networkResponse.setMessage(contact.getMessage());
                        return responseManager.ResponseNotFound(networkResponse);
                    }
                    boolean userPending = CheckContactPending(contact.getEmail_address(), "edit");
                    if (userPending) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Contact already pending for edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    String note = contact.getFirstname().equals(firstname) || firstname == null ? "" : "Change firstname from " + contact.getFirstname() + " to " + firstname;
                    note = contact.getSurname().equals(surname) || surname == null ? note : !note.equals("") ? note + ", change surname from " + contact.getSurname() + " to " + surname : "Change surname from " + contact.getSurname() + " to " + surname;
                    note = contact.getPhone_number().equals(phone_number) || phone_number == null ? note : !note.equals("") ? note + ", change phone number from " + contact.getPhone_number() + " to " + phone_number : "Change phone number from " + contact.getPhone_number() + " to " + phone_number;
                    SQL = "INSERT INTO tbl_financial_institution_contacts_operations(financial_institution_code, firstname, surname, phone_number, email_address, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, 'edit', ?, now())";
                    jdbcTemplate.update(SQL, new Object[]{contact.getInstitution(), contact.getFirstname(), contact.getSurname(), contact.getPhone_number(), contact.getEmail_address(), note});
                    SQL = "INSERT INTO tbl_user_details_operations(username, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, 'edit', ?, now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{contact.getEmail_address(), firstname, surname, phone_number, contact.getEmail_address(), 4, note});
                    if (retVal > 0) 
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseInternalServerError();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class ContactMapper implements RowMapper<UserModel> {
        @Override
        public UserModel mapRow(ResultSet rs, int arg1) throws SQLException {
            UserModel response = new UserModel();
            response.setId(rs.getInt("id"));
            response.setInstitution(rs.getString("financial_institution_code"));
            response.setFirstname(rs.getString("firstname"));
            response.setSurname(rs.getString("surname"));
            response.setPhone_number(rs.getString("phone_number"));
            response.setEmail_address(rs.getString("email_address"));
            response.setDate_created(rs.getString("date_created"));
            response.setInstitutionName(rs.getString("institutionname"));
            return response;
        }
    }
    
    class ContactMapper2 implements RowMapper<UserModel> {
        @Override
        public UserModel mapRow(ResultSet rs, int arg1) throws SQLException {
            UserModel response = new UserModel();
            response.setId(rs.getInt("id"));
            response.setInstitution(rs.getString("financial_institution_code"));
            response.setFirstname(rs.getString("firstname"));
            response.setSurname(rs.getString("surname"));
            response.setPhone_number(rs.getString("phone_number"));
            response.setEmail_address(rs.getString("email_address"));
            response.setDate_created(rs.getString("date_created"));
            response.setInstitutionName(rs.getString("institutionname"));
            response.setActionType(rs.getString("actionType"));
            response.setNote(rs.getString("note"));
            return response;
        }
    }
    
    class FinancialInstitutionMapper implements RowMapper<FinancialInstitutionModel> {
        @Override
        public FinancialInstitutionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            FinancialInstitutionModel response = new FinancialInstitutionModel();
            
            response.setId(rs.getInt("id"));
            response.setName(rs.getString("name"));
            response.setPort_number(rs.getInt("port_number"));
            response.setPublickeylocation(rs.getString("publickeylocation"));
            response.setShortName(rs.getString("shortName"));
            response.setCode(rs.getString("code"));
            response.setColor(rs.getString("color"));
            response.setBusinessType(rs.getInt("businessType"));
            response.setBusiness_address(rs.getString("business_address"));
            response.setDate_created(rs.getString("date_created"));
            response.setBusinessTypeName(rs.getString("businessTypeName"));
            response.setStatus(rs.getString("status"));
            response.setDate_updated(rs.getString("date_updated"));
            response.setCharge_amount(rs.getFloat("charge_amount"));
            response.setVat(rs.getFloat("vat"));
            return response;
        }
    }
    
    class FinancialInstitutionMapper2 implements RowMapper<FinancialInstitutionModel> {
        @Override
        public FinancialInstitutionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            FinancialInstitutionModel response = new FinancialInstitutionModel();
            
            response.setId(rs.getInt("id"));
            response.setName(rs.getString("name"));
            response.setPort_number(rs.getInt("port_number"));
            response.setPublickeylocation(rs.getString("publickeylocation"));
            response.setShortName(rs.getString("shortName"));
            response.setCode(rs.getString("code"));
            response.setColor(rs.getString("color"));
            response.setBusinessType(rs.getInt("businessType"));
            response.setBusiness_address(rs.getString("business_address"));
            response.setDate_created(rs.getString("date_created"));
            response.setBusinessTypeName(rs.getString("businessTypeName"));
            response.setActionType(rs.getString("actionType"));
            response.setNote(rs.getString("note"));
            return response;
        }
    }
    
    class InstitutionTypesMapper implements RowMapper<InstitutionTypesModel> {
        @Override
        public InstitutionTypesModel mapRow(ResultSet rs, int arg1) throws SQLException {
            InstitutionTypesModel response = new InstitutionTypesModel();
            
            response.setId(rs.getInt("id"));
            response.setName(rs.getString("name"));
            response.setDate_created(rs.getString("date_created"));
            return response;
        }
    }
}
