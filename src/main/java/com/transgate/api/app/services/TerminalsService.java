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
    
    private boolean CheckItemPendingEdit(String terminal_id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpay.terminals WHERE id = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{terminal_id}, int.class);

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
    public ResponseEntity Get(String id, String column) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL, minDate, maxDate;
            List<TerminalModel> terminals = new ArrayList<>();
            if (column.equals("acquiring_institution_id")) {
                SQL = "SELECT bank_code FROM sparkpayweb_db.tbl_financial_institutions WHERE acquirer_id = ?";
                String bank_code = jdbcTemplate.queryForObject(SQL, new Object[]{id}, String.class);
                SQL = "SELECT * FROM sparkpay.terminals "
                + "WHERE cbn_bank_code = ?";
                terminals = jdbcTemplate.query(SQL, new Object[]{bank_code}, new TerminalsMapper());
                SQL = "SELECT MIN(date_time) from sparkpay.terminals "
                    + "WHERE cbn_bank_code = ?";
                minDate = jdbcTemplate.queryForObject(SQL, new Object[]{bank_code}, String.class);
                SQL = "SELECT MAX(date_time) from sparkpay.terminals "
                    + "WHERE cbn_bank_code = ?";
                maxDate = jdbcTemplate.queryForObject(SQL, new Object[]{bank_code}, String.class);
            }
            else {
                SQL = "SELECT * FROM sparkpay.terminals "
                    + "WHERE "+column+" = ?";
                terminals = jdbcTemplate.query(SQL, new Object[]{id}, new TerminalsMapper());
                SQL = "SELECT MIN(date_time) from sparkpay.terminals "
                    + "WHERE "+column+" = ?";
                minDate = jdbcTemplate.queryForObject(SQL, new Object[]{id}, String.class);
                SQL = "SELECT MAX(date_time) from sparkpay.terminals "
                    + "WHERE "+column+" = ?";
                maxDate = jdbcTemplate.queryForObject(SQL, new Object[]{id}, String.class);
            }
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Terminal by "+column+": "+id);
            networkResponse.setData((ArrayList) terminals);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    public ResponseEntity Get(String terminal_id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<TerminalModel> terminals;
            SQL = "SELECT * FROM sparkpay.terminals "
                + "WHERE terminal_id = ?";
            terminals = jdbcTemplate.query(SQL, new Object[]{terminal_id}, new TerminalsMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Terminal by Terminal ID: " + terminal_id);
            networkResponse.setData((ArrayList) terminals);
            
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
    
    @Override
    public ResponseEntity Edit(String terminal_id, String merchant_id, String merchant_name, 
            String route_mode, String acquiring_institution_id, String acquiring_institution_name,
            String cbn_bank_code, String terminal_type,
            String sessiontoken) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE sparkpay.terminals SET merchant_id = ?, merchant_name = ?, route_mode = ?, acquiring_institution_id = ?,"
                            + "acquiring_institution_name = ?, cbn_bank_code = ?, terminal_type = ? WHERE terminal_id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{merchant_id, merchant_name, route_mode, acquiring_institution_id,
                        acquiring_institution_name, cbn_bank_code, terminal_type, terminal_id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckItemPendingEdit(terminal_id);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Terminal already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = Get(terminal_id);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    TerminalModel model = networkResponse != null ? (TerminalModel) networkResponse.getData().get(0) : new TerminalModel();
                    SQL = "INSERT into sparkpayweb_db.terminals_bkp"
                        + "(id, terminal_id, merchant_id, merchant_name, route_mode,"
                        + "acquiring_institution_id, acquiring_institution_name, cbn_bank_code, terminal_type, date_time) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
                    jdbcTemplate.update(SQL, new Object[]{model.getId(), terminal_id, merchant_id, merchant_name, route_mode, acquiring_institution_id,
                        acquiring_institution_name, cbn_bank_code, terminal_type});
                    SQL = "UPDATE sparkpay.terminals SET edit_flag = 1 WHERE terminal_id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{terminal_id});
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
    public ResponseEntity SearchTerminals(
           String start_date,
           String end_date,
           String terminal_id,
           String merchant_id,
           String merchant_name){

        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery =  !start_date.equals("")
                    || !end_date.equals("")
                    || !terminal_id.equals("")
                    || !merchant_id.equals("")
                    || !merchant_name.equals("")
                    ? "WHERE" : "";


            if (!terminal_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" terminal_id LIKE '%" + terminal_id+"%'";
            }
            if (!merchant_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" merchant_id LIKE '%" + merchant_id+"%'";
            }
            if (!merchant_name.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" merchant_name LIKE '%" + merchant_name+"%'";
            }

            if (!start_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" date_time >= '" + start_date + "'";
            }
            if (!end_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" date_time < '" + end_date + "'";
            }
            String SQL;
            String allowedRows = " AND delete_flag = ? AND edit_flag = ? AND create_flag = ? ";
            List<TerminalModel> terminals;
            SQL = "SELECT * FROM sparkpay.terminals "
                +whereQuery + allowedRows 
                + " ORDER BY date_time DESC";
            terminals = jdbcTemplate.query(SQL, new Object[]{"0", "0", "1"}, new TerminalsMapper());

            SQL = "SELECT MIN(date_time) from sparkpay.terminals";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_time) from sparkpay.terminals";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{ \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched terminals");
            networkResponse.setData((ArrayList) terminals);

            return responseManager.ResponseOk(networkResponse);
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
