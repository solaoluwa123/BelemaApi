/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.TerminalsInterface;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.TerminalModel;
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
public class TerminalsService implements TerminalsInterface {
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
    
    private boolean CheckNodePendingAction(int id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpay.terminals WHERE id = ? AND edit_flag = 1 OR delete_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    public int CheckTerminalExit(String id, String table, String column) {
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
    
    public ResponseEntity Get(boolean pending) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<TerminalModel> terminals;
            if (!pending) {
                SQL = "SELECT * FROM sparkpay.terminals "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY date_time DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpay.terminals "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY date_time DESC";
            }
            terminals = jdbcTemplate.query(SQL, new TerminalsMapper());
            SQL = "SELECT MIN(date_time) from sparkpay.terminals";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_time) from sparkpay.terminals";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Terminals");
            networkResponse.setData((ArrayList) terminals);
            networkResponse.setMeta(meta);
            
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
    public ResponseEntity Create(String terminal_id, String merchant_id, String merchant_name, 
            String route_mode, String acquiring_institution_id, String acquiring_institution_name,
            String cbn_bank_code, String terminal_type,
            String sessiontoken) {
            NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int retval;
            int terminalIDExit = CheckTerminalExit(terminal_id, "sparkpay.terminals", "terminal_id");
            if (terminalIDExit == 0) {
                int userrole = GetUserRole(sessiontoken);
                int create_flag = 0;
                if (userrole != 1 && userrole != 2) {
                    return responseManager.ResponseUnathorized();
                }
                if (userrole == 1) 
                    create_flag = 1;

                SQL = "INSERT into sparkpay.terminals"
                        + "(terminal_id, merchant_id, merchant_name, route_mode,"
                        + "acquiring_institution_id, acquiring_institution_name, cbn_bank_code, terminal_type, create_flag, date_time) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
                retval = jdbcTemplate.update(SQL, new Object[]{terminal_id, merchant_id, merchant_name, route_mode, acquiring_institution_id,
                        acquiring_institution_name, cbn_bank_code, terminal_type, create_flag});
                if (retval > 0) 
                    return responseManager.ResponseAccepted();
                else 
                    return responseManager.ResponseInternalServerError();
            }
            else {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Terminal with ID "+terminal_id+" already exist");
                return responseManager.ResponseOk(networkResponse);
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class TerminalsMapper implements RowMapper<TerminalModel> {

        @Override
        public TerminalModel mapRow(ResultSet rs, int arg1) throws SQLException {
            TerminalModel terminal = new TerminalModel();
            terminal.setId(rs.getInt("id"));
            terminal.setTerminal_id(rs.getString("terminal_id"));
            terminal.setMerchant_id(rs.getString("merchant_id"));
            terminal.setMerchant_name(rs.getString("merchant_name"));
            terminal.setRoute_mode(rs.getString("route_mode"));
            terminal.setAcquiring_institution_id(rs.getString("acquiring_institution_id"));
            terminal.setAcquiring_institution_name(rs.getString("acquiring_institution_name"));
            terminal.setCbn_bank_code(rs.getString("cbn_bank_code"));
            terminal.setTerminal_type(rs.getString("terminal_type"));
            terminal.setDate_time(rs.getString("date_time"));
            terminal.setDelete_flag(rs.getInt("delete_flag"));
            terminal.setEdit_flag(rs.getInt("edit_flag"));
            terminal.setCreate_flag(rs.getInt("create_flag"));
            return terminal;
        }
    }
}
