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
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("jdbcTemplate")
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
    
    private boolean CheckItemPendingEdit(int id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_terminal_owners WHERE id = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckItemPendingEdit(String id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_terminal_owners WHERE terminal_owner_id = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

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
    public ResponseEntity Get(int id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<TerminalOwnerModel> terminalOwners;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_terminal_owners "
                + "WHERE id = ?";
            terminalOwners = jdbcTemplate.query(SQL, new Object[]{id}, new TerminalOwnersMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Terminal Owner by ID: " + id);
            networkResponse.setData((ArrayList) terminalOwners);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Get(String id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<TerminalOwnerModel> terminalOwners;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_terminal_owners "
                + "WHERE terminal_owner_id = ?";
            terminalOwners = jdbcTemplate.query(SQL, new Object[]{id}, new TerminalOwnersMapper());
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Terminal Owner " + id);
            networkResponse.setData((ArrayList) terminalOwners);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Create(String terminal_owner_id, String terminal_owner_name, String sessiontoken) {
        try {
            String SQL;
            int retval;
            int userrole = GetUserRole(sessiontoken);
            int create_flag = 0;
            if (userrole != 1 && userrole != 2) {
                return responseManager.ResponseUnathorized();
            }
            if (userrole == 1) 
                create_flag = 1;
            
            SQL = "INSERT into sparkpayweb_db.tbl_terminal_owners"
                    + "(terminal_owner_id, terminal_owner_name, terminal_date, create_flag) VALUES(?, ?, now(), ?)";
            retval = jdbcTemplate.update(SQL, new Object[]{terminal_owner_id, terminal_owner_name, create_flag});
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
    public ResponseEntity Get() {
        return Get(false);
    }
    
    @Override
    public ResponseEntity GetApprovals() {
        return Get(true);
    }
    
    @Override
    public ResponseEntity Edit(String terminal_owner_id, String terminal_owner_name, String sessiontoken) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE sparkpayweb_db.tbl_terminal_owners SET terminal_owner_name = ? WHERE terminal_owner_id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{terminal_owner_name, terminal_owner_id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckItemPendingEdit(terminal_owner_id);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Terminal Owner already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = Get(terminal_owner_id);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    TerminalOwnerModel model = networkResponse != null ? (TerminalOwnerModel) networkResponse.getData().get(0) : new TerminalOwnerModel();
                    SQL = "INSERT into sparkpayweb_db.tbl_terminal_owners_bkp"
                        + "(id, terminal_owner_id, terminal_owner_name, terminal_date) "
                        + "VALUES(?, ?, ?, now())";
                    jdbcTemplate.update(SQL, new Object[]{model.getId(), terminal_owner_id, terminal_owner_name});
                    SQL = "UPDATE sparkpayweb_db.tbl_terminal_owners SET edit_flag = 1 WHERE terminal_owner_id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{terminal_owner_id});
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
    public ResponseEntity SearchTerminalOwners(
           String start_date,
           String end_date,
           String terminal_owner_id,
           String terminal_owner_name){

        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery =  !start_date.equals("")
                    || !end_date.equals("")
                    || !terminal_owner_id.equals("")
                    || !terminal_owner_name.equals("")
                    ? "WHERE" : "";


            if (!terminal_owner_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" terminal_owner_id LIKE '%" + terminal_owner_id+"%'";
            }

            if (!terminal_owner_name.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" terminal_owner_name LIKE '%" + terminal_owner_name+"%'";
            }

            if (!start_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" terminal_date >= '" + start_date + "'";
            }
            if (!end_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery + "";
                whereQuery+=" terminal_date < '" + end_date + "'";
            }
            String SQL;
            String allowedRows = " AND delete_flag = ? AND edit_flag = ? AND create_flag = ? ";
            List<TerminalOwnerModel> terminalOwners;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_terminal_owners "
                +whereQuery + allowedRows 
                + " ORDER BY terminal_date DESC";
            terminalOwners = jdbcTemplate.query(SQL, new Object[]{"0", "0", "1"}, new TerminalOwnersMapper());

            SQL = "SELECT MIN(terminal_date) from sparkpayweb_db.tbl_terminal_owners";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(terminal_date) from sparkpayweb_db.tbl_terminal_owners";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{ \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched Terminal Owners");
            networkResponse.setData((ArrayList) terminalOwners);

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }


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
