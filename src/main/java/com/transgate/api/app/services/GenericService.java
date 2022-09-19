/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.models.GenericModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.util.ResponseManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
public class GenericService implements GenericInterface {
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
    
    private boolean CheckPendingAction(int id, String table) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM "+table+" WHERE id = ? AND edit_flag = 1 OR delete_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    public int CheckItemExit(String id, String table, String column) {
        int totalRows = 0;
        try {
            String SQL = "SELECT COUNT(*) FROM "+table+" WHERE "+column+" = ?";
            totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);
            return totalRows;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return -1;
        }
    }
    
    @Override
    public ResponseEntity GetBanks() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> banks;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_bank_code ORDER BY name ASC";
            banks = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Banks");
            networkResponse.setData((ArrayList) banks);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSKR() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> skr;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_send_key_request ORDER BY id ASC";
            skr = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Send Key Request");
            networkResponse.setData((ArrayList) skr);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTransactionDirection() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> skr;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_trasaction_direction ORDER BY id ASC";
            skr = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transaction Direction");
            networkResponse.setData((ArrayList) skr);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetStates() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> states;
            SQL = "SELECT * FROM sparkpayweb_db.states ORDER BY id ASC";
            states = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All States");
            networkResponse.setData((ArrayList) states);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetResponseCodes() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> states;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_response_codes ORDER BY id ASC";
            states = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Response codes");
            networkResponse.setData((ArrayList) states);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity DeleteHelper(String sessiontoken, int id, String table, String entity) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "DELETE FROM "+table+" WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseDeleted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckPendingAction(id, table);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage(entity+" in pending for approval");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "UPDATE "+table+" SET delete_flag = 1 WHERE id = ?";
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
    
    class GenericMapper implements RowMapper<GenericModel> {

        @Override
        public GenericModel mapRow(ResultSet rs, int arg1) throws SQLException {
            GenericModel bank = new GenericModel();
            bank.setId(rs.getInt("id"));
            bank.setName(rs.getString("name") != null ? rs.getString("name") : "");
            bank.setCode(rs.getString("code") != null ? rs.getString("code") : "");
            bank.setDate_created(hasColumn(rs, "date_created") ? rs.getString("date_created") : null);
            return bank;
        }
    }
    
    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }
}
