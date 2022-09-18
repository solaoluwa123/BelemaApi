/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.MerchantsInterface;
import com.transgate.api.models.CardsMerchantModel;
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
public class MerchantsService implements MerchantsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    public ResponseEntity Get(boolean pending) {
         NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsMerchantModel> merchants;
            if (!pending) {
                SQL = "SELECT * FROM sparkpay.merchants "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpay.merchants "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            merchants = jdbcTemplate.query(SQL, new MerchantsMapper());
            SQL = "SELECT MIN(date_created) from sparkpay.merchants";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_created) from sparkpay.merchants";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Merchants");
            networkResponse.setData((ArrayList) merchants);
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
    
    class MerchantsMapper implements RowMapper<CardsMerchantModel> {

        @Override
        public CardsMerchantModel mapRow(ResultSet rs, int arg1) throws SQLException {
            CardsMerchantModel merchant = new CardsMerchantModel();
            merchant.setId(rs.getInt("id"));
            merchant.setMerchant_id(rs.getString("merchant_id"));
            merchant.setMerchant_name(rs.getString("merchant_name"));
            merchant.setMerchant_category_code(rs.getString("merchant_category_code"));
            merchant.setDate_created(rs.getString("date_created"));
            merchant.setMerchant_state(rs.getString("merchant_state"));
            merchant.setMerchant_country(rs.getString("merchant_country"));
            merchant.setDelete_flag(rs.getInt("delete_flag"));
            merchant.setEdit_flag(rs.getInt("edit_flag"));
            merchant.setCreate_flag(rs.getInt("create_flag"));
            return merchant;
        }
    }
}
