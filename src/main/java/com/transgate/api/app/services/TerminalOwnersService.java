/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.TerminalOwnersInterface;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.TerminalOwnerModel;
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
public class TerminalOwnersService implements TerminalOwnersInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    public ResponseEntity Get(boolean pending) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<TerminalOwnerModel> terminalOwners;
            if (!pending) {
                SQL = "SELECT * FROM sparkpayweb_db.tbl_terminal_owners "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpayweb_db.tbl_terminal_owners "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            terminalOwners = jdbcTemplate.query(SQL, new TerminalOwnersMapper());
            SQL = "SELECT MIN(terminal_date) from sparkpayweb_db.tbl_terminal_owners";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(terminal_date) from sparkpayweb_db.tbl_terminal_owners";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Terminal Owners");
            networkResponse.setData((ArrayList) terminalOwners);
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
    
    class TerminalOwnersMapper implements RowMapper<TerminalOwnerModel> {

        @Override
        public TerminalOwnerModel mapRow(ResultSet rs, int arg1) throws SQLException {
            TerminalOwnerModel terminal = new TerminalOwnerModel();
            terminal.setId(rs.getInt("id"));
            terminal.setTerminal_owner_id(rs.getString("terminal_owner_id"));
            terminal.setTerminal_owner_name(rs.getString("terminal_owner_name"));
            terminal.setTerminal_date(rs.getString("terminal_date"));
            terminal.setDelete_flag(rs.getInt("delete_flag"));
            terminal.setEdit_flag(rs.getInt("edit_flag"));
            terminal.setCreate_flag(rs.getInt("create_flag"));
            return terminal;
        }
    }
}
