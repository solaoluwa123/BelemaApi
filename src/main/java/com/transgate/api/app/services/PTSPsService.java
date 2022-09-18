/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.PTSPsInterface;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.PTSPModel;
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
public class PTSPsService implements PTSPsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    public ResponseEntity Get(boolean pending) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<PTSPModel> ptsps;
            if (!pending) {
                SQL = "SELECT * FROM sparkpayweb_db.ptsp "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpayweb_db.ptsp "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            ptsps = jdbcTemplate.query(SQL, new PTSPsMapper());
            SQL = "SELECT MIN(ptsp_date) from sparkpayweb_db.ptsp";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ptsp_date) from sparkpayweb_db.ptsp";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All PTSPs");
            networkResponse.setData((ArrayList) ptsps);
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
    
    class PTSPsMapper implements RowMapper<PTSPModel> {

        @Override
        public PTSPModel mapRow(ResultSet rs, int arg1) throws SQLException {
            PTSPModel ptsp = new PTSPModel();
            ptsp.setId(rs.getInt("id"));
            ptsp.setPtsp_id(rs.getString("ptsp_id"));
            ptsp.setPtsp_name(rs.getString("ptsp_name"));
            ptsp.setPtsp_date(rs.getString("ptsp_date"));
            ptsp.setDelete_flag(rs.getInt("delete_flag"));
            ptsp.setEdit_flag(rs.getInt("edit_flag"));
            ptsp.setCreate_flag(rs.getInt("create_flag"));
            return ptsp;
        }
    }
}
