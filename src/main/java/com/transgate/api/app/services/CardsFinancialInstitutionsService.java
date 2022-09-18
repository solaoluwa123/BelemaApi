/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.CardsFinancialInstitutionsInterface;
import com.transgate.api.models.CardsFinancialInstitutionModel;
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
public class CardsFinancialInstitutionsService implements CardsFinancialInstitutionsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    public ResponseEntity Get(boolean pending) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsFinancialInstitutionModel> cardFI;
            if (!pending) {
                SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            cardFI = jdbcTemplate.query(SQL, new CardFIMapper());
            SQL = "SELECT MIN(created) from sparkpayweb_db.tbl_financial_institutions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(created) from sparkpayweb_db.tbl_financial_institutions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institutions");
            networkResponse.setData((ArrayList) cardFI);
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
    
    class CardFIMapper implements RowMapper<CardsFinancialInstitutionModel> {

        @Override
        public CardsFinancialInstitutionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            CardsFinancialInstitutionModel fi = new CardsFinancialInstitutionModel();
            fi.setId(rs.getInt("id"));
            fi.setAcquirer_id(rs.getString("acquirer_id"));
            fi.setInstitution_name(rs.getString("institution_name"));
            fi.setIssuer_id(rs.getString("issuer_id"));
            fi.setBank_code(rs.getString("bank_code"));
            fi.setCreated(rs.getString("created"));
            fi.setDelete_flag(rs.getInt("delete_flag"));
            fi.setEdit_flag(rs.getInt("edit_flag"));
            fi.setCreate_flag(rs.getInt("create_flag"));
            return fi;
        }
    }
}
