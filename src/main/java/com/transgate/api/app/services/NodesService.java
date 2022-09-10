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
    
    @Override
    public ResponseEntity Get() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<NodeModel> transactions;
            SQL = "SELECT * FROM sparkpay.station_pcis ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new NodesMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Nodes");
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
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
