/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.NodesInterface;
import com.transgate.api.models.NodeModel;
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
public class NodesService implements NodesInterface {
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
    
    private boolean CheckNodePendingEdit(int id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpay.station_pcis WHERE id = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckNodePendingEdit(String id, String column) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpay.station_pcis WHERE "+column+" = ? AND edit_flag = 1";
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
            List<NodeModel> nodes;
            if (!pending) {
                SQL = "SELECT * FROM sparkpay.station_pcis "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpay.station_pcis "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            nodes = jdbcTemplate.query(SQL, new NodesMapper());
            SQL = "SELECT MIN(date_time) from sparkpay.station_pcis";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_time) from sparkpay.station_pcis";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Nodes");
            networkResponse.setData((ArrayList) nodes);
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
            List<NodeModel> nodes;
            SQL = "SELECT * FROM sparkpay.station_pcis "
                + "WHERE "+column+" = ?";
            nodes = jdbcTemplate.query(SQL, new Object[]{id}, new NodesMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Node by " + column+": "+id);
            networkResponse.setData((ArrayList) nodes);
            
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
            List<NodeModel> nodes;
            SQL = "SELECT * FROM sparkpay.station_pcis "
                + "WHERE id = ?";
            nodes = jdbcTemplate.query(SQL, new Object[]{id}, new NodesMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Node by ID: "+id);
            networkResponse.setData((ArrayList) nodes);
            
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
    public ResponseEntity Create(String station_name, int local_port, int acquiring_institution_id, 
            String kek, String send_key_request, String cbn_bank_code, String key_check_value, 
            String transaction_direction, String remoteIP, int remote_port, String sessiontoken
    ) {
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
            
            SQL = "INSERT into sparkpay.station_pcis"
                    + "(station_name, local_port, acquiring_institution_id, kek, send_key_request, cbn_bank_code, key_check_value, "
                    + "transaction_direction, remoteIP, remote_port, date_time, create_flag) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?)";
            retval = jdbcTemplate.update(SQL, new Object[]{station_name, local_port, acquiring_institution_id, kek, 
                        send_key_request, cbn_bank_code, key_check_value, transaction_direction, remoteIP, remote_port, create_flag});
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
    public ResponseEntity Delete(String sessiontoken, int id) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "DELETE FROM sparkpay.station_pcis WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseDeleted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckNodePendingEdit(id);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Nodes in pending for approval");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "UPDATE sparkpay.station_pcis SET delete_flag = 1 WHERE id = ?";
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
    
    @Override
    public ResponseEntity Edit(int id, String station_name, int local_port, int acquiring_institution_id, 
            String kek, String send_key_request, String cbn_bank_code, String key_check_value, 
            String transaction_direction, String remoteIP, int remote_port, String sessiontoken
    ) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE sparkpay.station_pcis "
                            + "SET station_name = ?, local_port = ?, acquiring_institution_id = ?, kek = ?, "
                            + "send_key_request = ?, cbn_bank_code = ?, key_check_value = ?, transaction_direction = ?, "
                            + "remoteIP = ?, remote_port = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{station_name, local_port, acquiring_institution_id, kek, 
                        send_key_request, cbn_bank_code, key_check_value, transaction_direction, remoteIP, remote_port, id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckNodePendingEdit(id);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Node already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "INSERT into sparkpayweb_db.station_pcis_bkp"
                        + "(id, station_name, local_port, acquiring_institution_id, kek, send_key_request, cbn_bank_code, key_check_value, "
                        + "transaction_direction, remoteIP, remote_port, date_time) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
                    jdbcTemplate.update(SQL, new Object[]{id, station_name, local_port, acquiring_institution_id, kek, 
                        send_key_request, cbn_bank_code, key_check_value, transaction_direction, remoteIP, remote_port});
                    SQL = "UPDATE sparkpay.station_pcis SET edit_flag = 1 WHERE id = ?";
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
    
    class NodesMapper implements RowMapper<NodeModel> {

        @Override
        public NodeModel mapRow(ResultSet rs, int arg1) throws SQLException {
            NodeModel node = new NodeModel();
            node.setId(rs.getInt("id"));
            node.setStation_name(rs.getString("station_name"));
            node.setLocal_port(rs.getInt("local_port"));
            node.setAcquiring_institution_id(rs.getInt("acquiring_institution_id"));
            node.setKek(rs.getString("kek"));
            node.setSend_key_request(rs.getString("send_key_request"));
            node.setCbn_bank_code(rs.getString("cbn_bank_code"));
            node.setDate_time(rs.getString("date_time"));
            node.setKey_check_value(rs.getString("key_check_value"));
            node.setTransaction_direction(rs.getString("transaction_direction"));
            node.setRemoteIP(rs.getString("remoteIP"));
            node.setRemote_port(rs.getInt("remote_port"));
            node.setDelete_flag(rs.getInt("delete_flag"));
            node.setEdit_flag(rs.getInt("edit_flag"));
            node.setCreate_flag(rs.getInt("create_flag"));
            return node;
        }
    }
}
