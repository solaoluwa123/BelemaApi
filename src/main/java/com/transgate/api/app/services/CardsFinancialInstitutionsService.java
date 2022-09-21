/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.CardsFinancialInstitutionsInterface;
import com.transgate.api.models.CardsFinancialInstitutionModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.util.ResponseManager;
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
public class CardsFinancialInstitutionsService implements CardsFinancialInstitutionsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    private int GetUserRole(String session_token) {
        try {
            int role;

            String SQL = "SELECT role FROM tbl_user_details WHERE deleted = 0 AND session_token = ?";
            role = jdbcTemplate.queryForObject(SQL, new Object[]{session_token}, int.class);
            return role;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage() + "------------");
            return -100;
        }
    }
    
    private boolean CheckItemPendingAction(int id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_financial_institutions WHERE id = ? AND edit_flag = 1 OR delete_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckItemPendingAction(String id, String column) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_financial_institutions WHERE "+column+" = ? AND edit_flag = 1 OR delete_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckBankCodeExist(String code) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_financial_institutions WHERE bank_code = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{code}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    public ResponseEntity Get(boolean pending) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsFinancialInstitutionModel> cardFI;
            if (!pending) {
                SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            cardFI = jdbcTemplate.query(SQL, new CardFIMapper());
            SQL = "SELECT MIN(created) from sparkpayweb_db.tbl_financial_institutions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(created) from sparkpayweb_db.tbl_financial_institutions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institutions");
            networkResponse.setData((ArrayList) cardFI);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    public ResponseEntity Get(String id, String column) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsFinancialInstitutionModel> cardFI;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                + "WHERE "+column+" = ?";
            cardFI = jdbcTemplate.query(SQL, new Object[]{id}, new CardFIMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Financial Institutions by "+column+": " + id);
            networkResponse.setData((ArrayList) cardFI);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    public ResponseEntity Get(int id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsFinancialInstitutionModel> cardFI;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                + "WHERE id = ?";
            cardFI = jdbcTemplate.query(SQL, new Object[]{id}, new CardFIMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Financial Institutions by ID: " + id);
            networkResponse.setData((ArrayList) cardFI);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Get() {
        return Get(false);
    }
    
    @Override
    public ResponseEntity GetApprovals() {
        return Get(true);
    }
    
    @Override
    public ResponseEntity Create(String acquirer_id, String institution_name, String issuer_id, String bank_code, String sessiontoken) {
        try {
            String SQL;
            int retval;
            if (CheckBankCodeExist(bank_code)) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Bank code already in use");
                return responseManager.ResponseOk(networkResponse);
            }
            int userrole = GetUserRole(sessiontoken);
            int create_flag = 0;
            if (userrole != 1 && userrole != 2) {
                return responseManager.ResponseUnathorized();
            }
            if (userrole == 1) 
                create_flag = 1;
            
            SQL = "INSERT into sparkpayweb_db.tbl_financial_institutions"
                    + "(acquirer_id, institution_name, issuer_id, bank_code, created, create_flag) VALUES(?, ?, ?, ?, now(), ?)";
            retval = jdbcTemplate.update(SQL, new Object[]{acquirer_id, institution_name, issuer_id, bank_code, create_flag});
            if (retval > 0) 
                return responseManager.ResponseAccepted();
            else 
                return responseManager.ResponseInternalServerError();
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Edit(int id, String acquirer_id, String institution_name, String issuer_id, String bank_code, String sessiontoken) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE sparkpayweb_db.tbl_financial_institutions "
                            + "SET acquirer_id = ?, institution_name = ?, issuer_id = ?, bank_code = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{acquirer_id, institution_name, issuer_id, bank_code, id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckItemPendingAction(id);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Financial Institution already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "INSERT into sparkpayweb_db.tbl_financial_institutions_bkp"
                        + "(id, acquirer_id, institution_name, issuer_id, bank_code, created) "
                        + "VALUES(?, ?, ?, ?, ?,now())";
                    jdbcTemplate.update(SQL, new Object[]{id, acquirer_id, institution_name, issuer_id, bank_code});
                    SQL = "UPDATE sparkpayweb_db.tbl_financial_institutions SET edit_flag = 1 WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
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
    
    class CardFIMapper implements RowMapper<CardsFinancialInstitutionModel> {

        @Override
        public CardsFinancialInstitutionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            CardsFinancialInstitutionModel fi = new CardsFinancialInstitutionModel();
            fi.setId(rs.getInt("id"));
            fi.setAcquirer_id(rs.getString("acquirer_id"));
            fi.setInstitution_name(rs.getString("institution_name"));
            fi.setIssuer_id(rs.getString("issuer_id"));
            fi.setBank_code(rs.getString("bank_code"));
            fi.setCreated(rs.getString("created"));
            fi.setDelete_flag(rs.getInt("delete_flag"));
            fi.setEdit_flag(rs.getInt("edit_flag"));
            fi.setCreate_flag(rs.getInt("create_flag"));
            return fi;
        }
    }
}
