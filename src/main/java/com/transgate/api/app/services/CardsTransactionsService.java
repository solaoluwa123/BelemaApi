/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.CardsTransactionsInterface;
import com.transgate.api.models.CardsTransactionModel;
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
public class CardsTransactionsService implements CardsTransactionsInterface {
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
            List<CardsTransactionModel> transactions;
            SQL = "SELECT * FROM sparkpay.transactions ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM sparkpay.transactions a";
            Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
            totalValue = totalValue != null ? totalValue : 0;
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ncs_date_time) from sparkpay.transactions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions");
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class CardsTransactionsMapper implements RowMapper<CardsTransactionModel> {

        @Override
        public CardsTransactionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            CardsTransactionModel tnx = new CardsTransactionModel();
            tnx.setId(rs.getInt("id"));
            tnx.setMessage_type(rs.getString("message_type"));
            tnx.setBin(rs.getString("bin"));
            tnx.setProcessing_code(rs.getString("processing_code"));
            tnx.setSystem_trace_number(rs.getString("system_trace_number"));
            tnx.setResponse_code(rs.getString("response_code"));
            tnx.setTransaction_date(rs.getString("transaction_date"));
            tnx.setTransaction_time(rs.getString("transaction_time"));
            tnx.setAmount(rs.getString("amount"));
            tnx.setRetrieval_ref_number(rs.getString("retrieval_ref_number"));
            tnx.setAcquirer_institution_id(rs.getString("acquirer_institution_id"));
            tnx.setPan(rs.getString("pan"));
            tnx.setTerminal_id(rs.getString("terminal_id"));
            tnx.setMerchant_id(rs.getString("merchant_id"));
            tnx.setLocation_name_address(rs.getString("location_name_address"));
            tnx.setNcs_date_time(rs.getString("ncs_date_time"));
            tnx.setDestination_acquiring_institution_id(rs.getString("destination_acquiring_institution_id"));
            tnx.setEncrypted_expiry_date(rs.getString("encrypted_expiry_date"));
            tnx.setEncrypted_pan(rs.getString("encrypted_pan"));
            tnx.setApproval_code(rs.getString("approval_code"));
            return tnx;
        }
    }
}
