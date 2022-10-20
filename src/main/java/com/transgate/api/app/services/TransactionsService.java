/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.TransactionsInterface;
import com.transgate.api.models.DisputeModel;
import com.transgate.api.models.DisputeTypeModel;
import com.transgate.api.models.FullTransactionModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.TNXModel;
import com.transgate.api.models.TransactionModel;
import com.transgate.api.models.TransactionSummaryModel;
import com.transgate.api.util.ResponseManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
public class TransactionsService implements TransactionsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    private int GetUserRole(String username, String session_token) {
        try {
            int role;

            String SQL = "SELECT role FROM tbl_user_details WHERE email_address = ? OR username = ? AND deleted = 0 AND session_token = ?";
            role = jdbcTemplate.queryForObject(SQL, new Object[]{username, username, session_token}, int.class);
            return role;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage() + "------------");
            return -100;
        }
    }
    
    private boolean CheckExistingUser(String email_address) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_users WHERE username = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{email_address}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckSessionId(String sessionid) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_disputes WHERE transactionSessionid = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{sessionid}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    public List<FullTransactionModel> GetTransaction(String sessionId, String amount, String source) {
        String SQL = "SELECT a.id, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id = ? AND a.source_institution_code = ?";
        
        List<FullTransactionModel> transactions = jdbcTemplate.query(SQL, new Object[]{sessionId, source}, new FullTransactionMapper());
        return transactions;
    }
    
    public List<FullTransactionModel> GetTransaction(String sessionId, String amount, String source, String responsecode) {
        String SQL = "SELECT a.id, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id = ? AND a.source_institution_code = ? AND a.response_code = ?";
        
        List<FullTransactionModel> transactions = jdbcTemplate.query(SQL, new Object[]{sessionId, source, responsecode}, new FullTransactionMapper());
        return transactions;
    }
    
    @Override
    public ResponseEntity Get() {
        return Get(0);
    }
    
    @Override
    public ResponseEntity SearchTransactions(String session_id,
            String source_institution_code,
            String destination_institution_code,
            String minAmount,
            String maxAmount,
            String originator_account_name,
            String beneficiary_account_name,
            String startDate,
            String endDate
    ) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery = !session_id.equals("") 
                    || !source_institution_code.equals("") 
                    || !source_institution_code.equals("")
                    || !destination_institution_code.equals("")
                    || !originator_account_name.equals("")
                    || !beneficiary_account_name.equals("")
                    || !startDate.equals("")
                    || !endDate.equals("")
                    || (!minAmount.equals("") && Double.parseDouble(minAmount) > 0)
                    || (!maxAmount.equals("") && Double.parseDouble(maxAmount) > 0)
                    ? "WHERE" : "";
            
            if (!session_id.equals("")) {
                whereQuery+=" a.session_id = '" + session_id + "'";
            }
            if (!source_institution_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.source_institution_code = " + source_institution_code;
            }
            if (!destination_institution_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.destination_institution_code = " + destination_institution_code;
            }
            if (!originator_account_name.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.originator_account_name LIKE '%" + originator_account_name+"%'";
            }
            if (!beneficiary_account_name.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.beneficiary_account_name LIKE '%" + beneficiary_account_name+"%'";
            }
            if ((!minAmount.equals("") && Double.parseDouble(minAmount) > 0)) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.amount >= " + minAmount;
            }
            if ((!maxAmount.equals("") && Double.parseDouble(maxAmount) > 0)) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.amount <= " + maxAmount;
            }
            if (!startDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.transaction_date_time >= '" + startDate + "'";
            }
            if (!endDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.transaction_date_time < '" + endDate + "'";
            }
            String SQL;
            List<FullTransactionModel> transactions;
            SQL = "SELECT a.id, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, b.name as srcInstitutionName, c.name as destInstitutionName "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.source_institution_code = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destination_institution_code = c.code " + whereQuery
                + " ORDER BY a.id DESC";
            transactions = jdbcTemplate.query(SQL, new FullTransactionMapper());

            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.source_institution_code = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destination_institution_code = c.code " + whereQuery
                + " ORDER BY a.id DESC";
            Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
            totalValue = totalValue != null ? totalValue : 0;
            SQL = "SELECT MIN(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
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
    
    @Override
    public ResponseEntity GetTransactionsVolume(String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.source_institution_code";
            
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.destination_institution_code";
            
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setInflows((ArrayList) summary_);
            tnxModel.setOutflows((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTransactionsVolume(String institutioncode, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.source_institution_code = ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";
            
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "WHERE a.source_institution_code != ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";

            List<TransactionSummaryModel> summaryOthers = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            summary.add(summaryOthers.get(0));
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.destination_institution_code = ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";
            
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "WHERE a.destination_institution_code != ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";
            List<TransactionSummaryModel> summary_Others = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            summary_.add(summary_Others.get(0));
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setInflows((ArrayList) summary_);
            tnxModel.setOutflows((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTransactionsRates(String startDate, String endDate, boolean inward) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            if (startDate.contains("T"))
                startDate = startDate.replace("T", " ");
            if (endDate.contains("T"))
                endDate = endDate.replace("T", " ");
            String SQL, SQL_;
            if (inward) {
                SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.destination_institution_code "
                    + "ORDER BY a.destination_institution_code";
                
                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.destination_institution_code "
                    + "ORDER BY a.destination_institution_code";
            }
            else {
                SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.source_institution_code "
                    + "ORDER BY a.source_institution_code";
                
                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.source_institution_code "
                    + "ORDER BY a.source_institution_code";
            }
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.response_code = '00'";
//            int totalSuccessFul = jdbcTemplate.queryForObject(SQL, int.class);
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
            
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.response_code != '00'";
//            int totalFailures = jdbcTemplate.queryForObject(SQL, int.class);
            
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL_, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSuccessVolumes((ArrayList) summary);
            tnxModel.setTotalVolumes((ArrayList) summary_);
            networkResponse.setTnxModel(tnxModel);
//            String meta = "{\"totalSuccessFul\": " +totalSuccessFul+ ", \"totalFailures\": " + totalFailures +"}";
//            networkResponse.setMeta(meta);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTransactionsRates(String institutioncode, String startDate, String endDate, boolean inward) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL, SQL_;
            if (inward) {
                SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.destination_institution_code = ? AND a.transaction_date_time BETWEEN ? AND ?";
                
                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.destination_institution_code = ? AND a.transaction_date_time BETWEEN ? AND ?";
            }
            else {
                SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.source_institution_code = ? AND a.transaction_date_time BETWEEN ? AND ?";
                
                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.source_institution_code = ? AND a.transaction_date_time BETWEEN ? AND ?";
            }
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.response_code = '00'";
//            int totalSuccessFul = jdbcTemplate.queryForObject(SQL, int.class);
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.response_code != '00'";
//            int totalFailures = jdbcTemplate.queryForObject(SQL, int.class);
            
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL_, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSuccessVolumes((ArrayList) summary);
            tnxModel.setTotalVolumes((ArrayList) summary_);
            networkResponse.setTnxModel(tnxModel);
//            String meta = "{\"totalSuccessFul\": " +totalSuccessFul+ ", \"totalFailures\": " + totalFailures +"}";
//            networkResponse.setMeta(meta);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetInsitutionTnxTrend(String institutioncode, String type, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL = "";
            List<Map<String, Object>> trend;
            switch(type) {
                case "month":
                    SQL = "SELECT a.transaction_date_time as label, COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                            + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                            + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                            + "ON a.source_institution_code = b.code "
                            + "WHERE a.response_code = '00' AND a.source_institution_code = ? "
                            + "AND a.transaction_date_time BETWEEN ? AND ?"
                            + "GROUP BY MONTH(a.transaction_date_time)";
                    break;
                case "day":
                default: 
                    SQL = "SELECT a.transaction_date_time as label, COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                            + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                            + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                            + "ON a.source_institution_code = b.code "
                            + "WHERE a.response_code = '00' AND a.source_institution_code = ? "
                            + "AND a.transaction_date_time BETWEEN ? AND ?"
                            + "GROUP BY CAST(a.transaction_date_time as DATE)";
                    break;
            }

            trend = jdbcTemplate.queryForList(SQL, new Object[]{institutioncode, startDate, endDate});
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions Trend");
            networkResponse.setData((ArrayList) trend);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Get(String institutioncode) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<FullTransactionModel> transactions;
            SQL = "SELECT a.id, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, b.name as srcInstitutionName, c.name as destInstitutionName "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.source_institution_code = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destination_institution_code = c.code "
                + "WHERE a.source_institution_code = ? OR a.destination_institution_code = ? ORDER BY a.id DESC";
            transactions = jdbcTemplate.query(SQL, new Object[]{institutioncode, institutioncode}, new FullTransactionMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.source_institution_code = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destination_institution_code = c.code "
                + "WHERE a.source_institution_code = ? OR a.destination_institution_code = ? ORDER BY a.id DESC";
            Double totalValue = jdbcTemplate.queryForObject(SQL, new Object[]{institutioncode, institutioncode}, Double.class);
            totalValue = totalValue != null ? totalValue : 0;
            SQL = "SELECT MIN(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
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
    public ResponseEntity Get(int id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<FullTransactionModel> transactions;
            if (id > 0) {
                SQL = "SELECT a.id, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.id = ? ";
                transactions = jdbcTemplate.query(SQL, new Object[]{id}, new FullTransactionMapper());
            }
            else {
                SQL = "SELECT a.id, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "ORDER BY a.id DESC";
                transactions = jdbcTemplate.query(SQL, new FullTransactionMapper());
                
                SQL = "SELECT SUM(a.amount) as totalValue "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code ";
                Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
                totalValue = totalValue != null ? totalValue : 0;
                SQL = "SELECT MIN(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
                String minDate = jdbcTemplate.queryForObject(SQL, String.class);
                SQL = "SELECT MAX(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
                String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
                networkResponse.setMeta(meta);
            
            }
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(id > 0 ? "Transaction" : "All transactions");
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetBySessionId(String id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<FullTransactionModel> transactions;
            SQL = "SELECT a.id, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id = ? ";
            transactions = jdbcTemplate.query(SQL, new Object[]{id}, new FullTransactionMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Transaction");
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetDisputes(String institution) {
        return GetDisputes(0, 0, institution);
    }
    
    @Override
    public ResponseEntity GetDisputes(int id) {
        return GetDisputes(id, 0, null);
    }
    
    @Override
    public ResponseEntity GetSettlements(int id) {
        return GetDisputes(id, 1, null);
    }
    
    @Override
    public ResponseEntity GetSettlements(String institution) {
        return GetDisputes(0, 1, institution);
    }
    
    @Override
    public ResponseEntity GetDisputes(int id, int status, String institutioncode) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
//            String appendQuery = status.equals("0") ? "dispute.status = 0 OR dispute.status = -1" : "dispute.status = 1";
            String code = institutioncode != null ? institutioncode : "";
//            String appendQueryTwo = institutioncode == null ? "" : institutioncode.equals("-1") ? "" : " AND a.source_institution_code = " + institutioncode + " OR a.destination_institution_code = " + institutioncode;
            String SQL;
            List<DisputeModel> transactions;
            Double totalValue = 0.00;
            if (id > 0) {
                SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                        + "a.id as transactionid, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, "
                        + "b.name as srcInstitutionName, c.name as destInstitutionName "
                        + "FROM tbl_disputes dispute "
                        + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                        + "ON dispute.transactionSessionid = a.session_id "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                        + "ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                        + "ON a.destination_institution_code = c.code "
                    + "WHERE dispute.id = ? ORDER BY dispute.id DESC";
                transactions = jdbcTemplate.query(SQL, new Object[]{id}, new DisputeTransactionMapper());
            }
            else {
                switch (code) {
                    case "":
                    case "-1":
                        if (status == 0){
                            SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                                + "a.id as transactionid, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, "
                                + "b.name as srcInstitutionName, c.name as destInstitutionName "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.session_id "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                                + "ON a.source_institution_code = b.code "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                                + "ON a.destination_institution_code = c.code"
                                + " ORDER BY dispute.id DESC ";
                            
                            String SQL2 = "SELECT SUM(a.amount) as totalValue "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.session_id";
                            totalValue = jdbcTemplate.queryForObject(SQL2, Double.class);
                        }
                        else {
                            SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                                + "a.id as transactionid, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, "
                                + "b.name as srcInstitutionName, c.name as destInstitutionName "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.session_id "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                                + "ON a.source_institution_code = b.code "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                                + "ON a.destination_institution_code = c.code WHERE dispute.resolved = 1"
                                + " ORDER BY dispute.id DESC ";
                            
                            String SQL2 = "SELECT SUM(a.amount) as totalValue "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.session_id "
                                + "WHERE dispute.resolved = 1";
                            totalValue = jdbcTemplate.queryForObject(SQL2, Double.class);
                        }
                        break;
                    default:
                        if (status == 0){
                            SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                                + "a.id as transactionid, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, "
                                + "b.name as srcInstitutionName, c.name as destInstitutionName "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.session_id "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                                + "ON a.source_institution_code = b.code "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                                + "ON a.destination_institution_code = c.code WHERE a.source_institution_code = " + code + " OR a.destination_institution_code = " + code+" "
                                + "ORDER BY dispute.id DESC ";
                            
                            String SQL2 = "SELECT SUM(a.amount) as totalValue "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.session_id "
                                + "WHERE a.source_institution_code = " + code + " OR a.destination_institution_code = " + code+" ";
                            totalValue = jdbcTemplate.queryForObject(SQL2, Double.class);
                        }
                        else {
                            SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                                + "a.id as transactionid, a.session_id, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, "
                                + "b.name as srcInstitutionName, c.name as destInstitutionName "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.session_id "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                                + "ON a.source_institution_code = b.code "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                                + "ON a.destination_institution_code = c.code WHERE dispute.resolved = 1 AND (a.source_institution_code = " + code + " OR a.destination_institution_code = " + code + ")"
                                + " ORDER BY dispute.id DESC ";
                            
                            String SQL2 = "SELECT SUM(a.amount) as totalValue "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.session_id "
                                + "WHERE dispute.resolved = 1 AND (a.source_institution_code = " + code + " OR a.destination_institution_code = " + code + ")";
                            totalValue = jdbcTemplate.queryForObject(SQL2, Double.class);
                        }
                        break;
                }
                transactions = jdbcTemplate.query(SQL, new DisputeTransactionMapper());
            }
            totalValue = totalValue != null ? totalValue : 0;
            SQL = "SELECT MIN(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            if (status == 0)
                networkResponse.setMessage(id > 0 ? "Dispute" : "All disputes");
            else if (status == 1)
                networkResponse.setMessage(id > 0 ? "Settlement" : "All settlements");
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetDisputeTypes() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<DisputeTypeModel> types;
            SQL = "SELECT a.id, a.type, a.value FROM tbl_dispute_types a ORDER BY a.type ASC";
            types = jdbcTemplate.query(SQL, new DisputeTypeMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All dispute types");
            networkResponse.setData((ArrayList) types);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity LogDispute(String sessiontoken, String sessionId, String amount, String wallet, String sourceInstitution, String type, String username){
        try {
            
            boolean sessionIdExist = CheckSessionId(sessionId);
            if (sessionIdExist) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Cannot log dispute with same session ID twice");
                return responseManager.ResponseOk(networkResponse);
            }
            
//            List<FullTransactionModel> getTransaction = GetTransaction(sessionId, amount, sourceInstitution);
            List<FullTransactionModel> getTransaction = GetTransaction(sessionId, amount, sourceInstitution);
            if (getTransaction.size() > 0) {
                if (!getTransaction.get(0).getSrcResponsecode().equals("00")) {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(404);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Declined Transaction cannot be logged for dispute");
                    return responseManager.ResponseOk(networkResponse);
                }
                String SQL;
                int userrole = GetUserRole(username, sessiontoken);
                SQL = "INSERT into tbl_disputes(transactionSessionid, loggedBy, ownerInstitution, status, date_created) VALUES(?, ?, ?, '-1', now())";
                int retval = jdbcTemplate.update(SQL, new Object[]{sessionId, username, getTransaction.get(0).getSrcInstitutioncode()});
                if (retval > 0) 
                    return responseManager.ResponseAccepted();
                else 
                    return responseManager.ResponseInternalServerError();
//                switch (userrole) {
//                    case 1:
//                        SQL = "INSERT into tbl_disputes(transactionSessionid, loggedBy, ownerInstitution, date_created) VALUES(?, ?, ?, now())";
//                        int retval = jdbcTemplate.update(SQL, new Object[]{sessionId, username, destinationInstitution});
//                        if (retval > 0) 
//                            return responseManager.ResponseAccepted();
//                        else 
//                            return responseManager.ResponseInternalServerError();
//                    case 2:
//                        SQL = "INSERT INTO tbl_user_details_operations(username, password, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, ?, 'create', 'Create user account', now())";
//                        retval = jdbcTemplate.update(SQL, new Object[]{username, hashPassword, firstname, surname, phone_number, email_address, roleid});
//                        if (retval > 0) 
//                            return responseManager.ResponseAccepted();
//                        else
//                            return responseManager.ResponseInternalServerError();
//                    default:
//                        return responseManager.ResponseUnathorized();
//                }
            } else {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(404);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Transaction not found");
                return responseManager.ResponseOk(networkResponse);
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class DisputeTransactionMapper implements RowMapper<DisputeModel> {
        @Override
        public DisputeModel mapRow(ResultSet rs, int arg1) throws SQLException {
            DisputeModel response = new DisputeModel();            
            response.setId(rs.getInt("id"));
            response.setTransactionId(rs.getInt("transactionid"));
            response.setType(rs.getString("type"));
            response.setLoggedBy(rs.getString("loggedBy"));
            response.setStatus(rs.getInt("status"));
            response.setResolved(rs.getInt("resolved"));
            response.setDateModified(rs.getString("date_modified"));
            response.setDateCreated(rs.getString("date_created"));
            response.setSrcSessionid(rs.getString("session_id"));
            response.setSrcAccountNumber(rs.getString("originator_account_number"));
            response.setSrcAccountName(rs.getString("originator_account_name"));
            response.setSrcKycLevel(rs.getString("originator_kyc"));
            response.setSrcBvn(rs.getString("originator_bvn"));
            response.setSrcAmount(rs.getString("amount"));
            response.setSrcInstitutioncode(rs.getString("source_institution_code"));
            response.setDestSessionId(rs.getString("session_id"));
            response.setSrcResponsecode(rs.getString("response_code"));
            response.setDestAccountNumber(rs.getString("beneficiary_account_number"));
            response.setDestAccountName(rs.getString("beneficiary_account_name"));
            response.setDestKycLevel(rs.getString("beneficiary_kyc"));
            response.setDestBvn(rs.getString("beneficiary_bvn"));
            response.setDestAmount(rs.getString("amount"));
            response.setDestInstitutioncode(rs.getString("destination_institution_code"));
            response.setDestResponseCode(rs.getString("response_code"));
            response.setNarration(rs.getString("narration"));
            response.setTransactiondate(rs.getString("transaction_date_time"));
            response.setUsername(rs.getString("name_enquiry_ref"));
            response.setSrcInstitutionName(rs.getString("srcInstitutionName"));
            response.setDestInstitutionName(rs.getString("destInstitutionName"));
            return response;
        }
    }
    
    class TransactionSummaryMapper implements RowMapper<TransactionSummaryModel> {
        @Override
        public TransactionSummaryModel mapRow(ResultSet rs, int arg1) throws SQLException {
            TransactionSummaryModel response = new TransactionSummaryModel();
//            response.setValue(rs.getString("value"));
//            response.setVolume(rs.getString("volume"));
            response.setCode(ColumnExistinRS(rs, "code") ? rs.getString("code") : "-1");
            response.setName(ColumnExistinRS(rs, "name") ? rs.getString("name") : "Other Banks");
            response.setShortName(ColumnExistinRS(rs, "shortName") ? rs.getString("shortName") : "Others");
            response.setColor(ColumnExistinRS(rs, "color") ? rs.getString("color") : "#4285F4");
            response.setVolume(ColumnExistinRS(rs, "volume") ? rs.getString("volume") : "0");
            response.setValue(ColumnExistinRS(rs, "value") ? rs.getString("value") : "0");
            return response;
        }
    }
    
    class FullTransactionMapper implements RowMapper<FullTransactionModel> {
        @Override
        public FullTransactionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            FullTransactionModel response = new FullTransactionModel();            
            response.setId(rs.getInt("id"));
            response.setSrcSessionid(rs.getString("session_id"));
            response.setSrcAccountNumber(rs.getString("originator_account_number"));
            response.setSrcAccountName(rs.getString("originator_account_name"));
            response.setSrcKycLevel(rs.getString("originator_kyc"));
            response.setSrcBvn(rs.getString("originator_bvn"));
            response.setSrcAmount(rs.getString("amount"));
            response.setSrcInstitutioncode(rs.getString("source_institution_code"));
            response.setDestSessionId(rs.getString("session_id"));
            response.setSrcResponsecode(rs.getString("response_code"));
            response.setDestAccountNumber(rs.getString("beneficiary_account_number"));
            response.setDestAccountName(rs.getString("beneficiary_account_name"));
            response.setDestKycLevel(rs.getString("beneficiary_kyc"));
            response.setDestBvn(rs.getString("beneficiary_bvn"));
            response.setDestAmount(rs.getString("amount"));
            response.setDestInstitutioncode(rs.getString("destination_institution_code"));
            response.setDestResponseCode(rs.getString("response_code"));
            response.setNarration(rs.getString("narration"));
            response.setTransactiondate(rs.getString("transaction_date_time"));
            response.setUsername(rs.getString("name_enquiry_ref"));
            response.setSrcInstitutionName(rs.getString("srcInstitutionName"));
            response.setDestInstitutionName(rs.getString("destInstitutionName"));
            return response;
        }
    }
    
    @Override
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, String username, int status) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
//            switch (userrole) {
//                case 1:
//                case 3:
                    int resolved = status == 0 ? 0 : 1;
                    SQL = "UPDATE tbl_disputes SET status = ?, resolved = ?, date_modified = now() WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{status, resolved, id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
//                default:
//                    return responseManager.ResponseUnathorized();
//            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class TransactionMapper implements RowMapper<TransactionModel> {
        @Override
        public TransactionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            TransactionModel response = new TransactionModel();            
            response.setId(rs.getInt("id"));
            response.setSrcSessionid(rs.getString("session_id"));
            response.setSrcAccountNumber(rs.getString("originator_account_number"));
            response.setSrcAccountName(rs.getString("originator_account_name"));
            response.setSrcKycLevel(rs.getString("originator_kyc"));
            response.setSrcBvn(rs.getString("originator_bvn"));
            response.setSrcAmount(rs.getString("amount"));
            response.setSrcInstitutioncode(rs.getString("source_institution_code"));
            response.setDestSessionId(rs.getString("session_id"));
            response.setSrcResponsecode(rs.getString("response_code"));
            response.setDestAccountNumber(rs.getString("beneficiary_account_number"));
            response.setDestAccountName(rs.getString("beneficiary_account_name"));
            response.setDestKycLevel(rs.getString("beneficiary_kyc"));
            response.setDestBvn(rs.getString("beneficiary_bvn"));
            response.setDestAmount(rs.getString("amount"));
            response.setDestInstitutioncode(rs.getString("destination_institution_code"));
            response.setDestResponseCode(rs.getString("response_code"));
            response.setNarration(rs.getString("narration"));
            response.setTransactiondate(rs.getString("transaction_date_time"));
            response.setUsername(rs.getString("name_enquiry_ref"));
            return response;
        }
    }
    
    class DisputeTypeMapper implements RowMapper<DisputeTypeModel> {
        @Override
        public DisputeTypeModel mapRow(ResultSet rs, int arg1) throws SQLException {
            DisputeTypeModel response = new DisputeTypeModel();            
            response.setId(rs.getInt("id"));
            response.setType(rs.getString("type"));
            response.setValue(rs.getString("value"));
            return response;
        }
    }
    
    public static boolean ColumnExistinRS(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }
}
