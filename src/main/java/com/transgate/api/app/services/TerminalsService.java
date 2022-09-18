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
    
    public ResponseEntity Get(boolean pending) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<TerminalModel> terminals;
            if (!pending) {
                SQL = "SELECT * FROM sparkpay.terminals "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpay.terminals "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
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
