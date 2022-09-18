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
import com.transgate.api.util.TransactionsCodeInterpreter;
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
    
    TransactionsCodeInterpreter transactionsCodeInterpreter = new TransactionsCodeInterpreter();
    
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
    
    @Override
    public ResponseEntity SearchTransactions(String message_type,
            String bin,
            String processing_code,
            String min_amount,
            String max_amount,
            String system_trace_number,
            String response_code,
            String start_date,
            String end_date,
            String retrieval_ref_number,
            String acquirer_institution_id,
            String pan,
            String terminal_id,
            String merchant_id,
            String location_name_address,
            String ncs_date_time,
            String approval_code
    ) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery = !message_type.equals("") 
                    || !bin.equals("") 
                    || !processing_code.equals("")
                    || !system_trace_number.equals("")
                    || !response_code.equals("")
                    || !retrieval_ref_number.equals("")
                    || !start_date.equals("")
                    || !end_date.equals("")
                    || !acquirer_institution_id.equals("")
                    || !pan.equals("")
                    || !terminal_id.equals("")
                    || !merchant_id.equals("")
                    || !location_name_address.equals("")
                    || !ncs_date_time.equals("")
                    || !approval_code.equals("")
                    || (!min_amount.equals("") && Double.parseDouble(min_amount) > 0)
                    || (!max_amount.equals("") && Double.parseDouble(max_amount) > 0)
                    ? "WHERE" : "";
            
            if (!message_type.equals("")) {
                whereQuery+=" message_type = '" + message_type + "'";
            }
            if (!bin.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" bin = " + bin;
            }
            if (!processing_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" processing_code = " + processing_code;
            }
            if (!system_trace_number.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" system_trace_number LIKE '%" + system_trace_number+"%'";
            }
            if (!response_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" response_code = " + response_code+"";
            }
            if (!retrieval_ref_number.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" retrieval_ref_number LIKE '%" + retrieval_ref_number+"%'";
            }
            if (!acquirer_institution_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" acquirer_institution_id LIKE '%" + acquirer_institution_id+"%'";
            }
            if (!pan.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" pan LIKE '%" + pan+"%'";
            }
            if (!terminal_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" terminal_id LIKE '%" + terminal_id+"%'";
            }
            if (!merchant_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" merchant_id LIKE '%" + merchant_id+"%'";
            }
            if (!location_name_address.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" location_name_address LIKE '%" + location_name_address+"%'";
            }
            if (!ncs_date_time.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" ncs_date_time LIKE '%" + ncs_date_time+"%'";
            }
            if (!approval_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" approval_code LIKE '%" + approval_code+"%'";
            }
            if ((!min_amount.equals("") && Double.parseDouble(min_amount) > 0)) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" amount >= " + min_amount;
            }
            if ((!max_amount.equals("") && Double.parseDouble(max_amount) > 0)) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" amount <= " + max_amount;
            }
            if (!start_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" transaction_date >= '" + start_date + "'";
            }
            if (!end_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" transaction_date < '" + end_date + "'";
            }
            String SQL;
            List<CardsTransactionModel> transactions;
            SQL = "SELECT * FROM sparkpay.transactions "
                +whereQuery
                + " ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM sparkpay.transactions a " + whereQuery;
            Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
            totalValue = totalValue != null ? totalValue : 0;
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched transactions");
            networkResponse.setData((ArrayList) transactions);
            
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
            tnx.setStatus_code_message(transactionsCodeInterpreter.GetResponse(rs.getString("response_code")));
            return tnx;
        }
    }
}
