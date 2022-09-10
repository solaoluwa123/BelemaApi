/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.RoutesInterface;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.RouteModel;
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
public class RoutesService implements RoutesInterface {
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
            List<RouteModel> routes;
            SQL = "SELECT * FROM sparkpay.transaction_route ORDER BY id DESC";
            routes = jdbcTemplate.query(SQL, new RoutesMapper());
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ncs_date_time) from sparkpay.transactions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Routes");
            networkResponse.setData((ArrayList) routes);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class RoutesMapper implements RowMapper<RouteModel> {

        @Override
        public RouteModel mapRow(ResultSet rs, int arg1) throws SQLException {
            RouteModel route = new RouteModel();
            route.setId(rs.getInt("id"));
            route.setSource_acq_id(rs.getString("source_acq_id"));
            route.setDestination_bin(rs.getString("destination_bin"));
            route.setCard_bin(rs.getString("card_bin"));
            route.setDate_created(rs.getString("date_created"));
            route.setDelete_flag(rs.getInt("delete_flag"));
            route.setEdit_flag(rs.getInt("edit_flag"));
            route.setCreate_flag(rs.getInt("create_flag"));
            return route;
        }
    }
}
