/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.TransactionsInterface;
import com.transgate.api.models.ChannelsTnxValueModel;
import com.transgate.api.models.DisputeModel;
import com.transgate.api.models.DisputeTypeModel;
import com.transgate.api.models.FullTransactionModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.TNXModel;
import com.transgate.api.models.TransactionHalfModel;
import com.transgate.api.models.TransactionModel;
import com.transgate.api.models.TransactionSummaryModel;
import com.transgate.api.util.DateUtil;
import com.transgate.api.util.ResponseCodeInterpreter;
import com.transgate.api.util.ResponseManager;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.json.JSONArray;
import org.json.JSONException;
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
    DateUtil dateUtil = new DateUtil();
    
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
        String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id = ? AND a.source_institution_code = ?";
        
        List<FullTransactionModel> transactions = jdbcTemplate.query(SQL, new Object[]{sessionId, source}, new FullTransactionMapper());
        return transactions;
    }
    
    public List<FullTransactionModel> GetTransactionFromHistory(String sessionId, String source) {
        String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id = ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?)";
        
        List<FullTransactionModel> transactions = jdbcTemplate.query(SQL, new Object[]{sessionId, source, source}, new FullTransactionMapper());
        return transactions;
    }
    
    public List<FullTransactionModel> GetTransaction(String sessionId, String amount, String source, String responsecode) {
        String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
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
    public ResponseEntity Get(String institutioncode, String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<FullTransactionModel> transactions;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                + "FROM " + table + " a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.source_institution_code = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destination_institution_code = c.code "
                + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
                + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM "+ table +" a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.source_institution_code = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destination_institution_code = c.code "
                + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
                + "ORDER BY a.id DESC";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            Double totalValue = tValue != null ? tValue.doubleValue() : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Transactions By Institution: " + institutioncode);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Get(String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<FullTransactionModel> transactions;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                + "FROM "+table+" a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.source_institution_code = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destination_institution_code = c.code "
                + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? "
                + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM "+table+" a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.source_institution_code = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destination_institution_code = c.code "
                + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? "
                + "ORDER BY a.id DESC";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            Double totalValue = tValue != null ? tValue.doubleValue() : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
//            SQL = "SELECT MIN(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
//            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
//            SQL = "SELECT MAX(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
//            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions");
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity SearchTransactions(String session_id,
            String channel_code,
            String response_code,
            String source_institution_code,
            String destination_institution_code,
            String minAmount,
            String maxAmount,
            String originator_account_name,
            String beneficiary_account_name,
            String startDate,
            String endDate, 
            int page, 
            int limit,
            boolean isCurrent,
            String userInstitutionCode
    ) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery = "";
            if (userInstitutionCode.equals("-1")) {
                whereQuery = "WHERE";
            } else if (source_institution_code.equals("") && destination_institution_code.equals("")) {
                whereQuery = "WHERE (a.source_institution_code = "+userInstitutionCode+" OR a.destination_institution_code = "+userInstitutionCode+")";
            } else {
                whereQuery = "WHERE";
            }
//            String whereQuery = !session_id.equals("") 
//                    || !channel_code.equals("") 
//                    || !response_code.equals("") 
//                    || !source_institution_code.equals("")
//                    || !destination_institution_code.equals("")
//                    || !originator_account_name.equals("")
//                    || !beneficiary_account_name.equals("")
//                    || !startDate.equals("")
//                    || !endDate.equals("")
//                    || (!minAmount.equals("") && Double.parseDouble(minAmount) > 0)
//                    || (!maxAmount.equals("") && Double.parseDouble(maxAmount) > 0)
//                    ? "WHERE" : "";
            
            if (!session_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.session_id = '" + session_id + "'";
            }
            if (!channel_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.channel_code = '" + channel_code + "'";
            }
            if (!response_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                if (response_code.equals("111")) whereQuery+=" a.response_code != 00";
                else whereQuery+=" a.response_code = " + response_code;
            }
//            if (!response_code.equals("") && !response_code.equals("00")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
//                whereQuery+=" a.response_code != '00'";
//            }
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
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<FullTransactionModel> transactions;
            if (isCurrent) {
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code " + whereQuery
                    + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code " + whereQuery
                    + " ORDER BY a.id DESC";
            } else {
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code " + whereQuery
                    + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code " + whereQuery
                    + " ORDER BY a.id DESC";
            }
            LocalTime currentTime = LocalTime.now();
            int hour = currentTime.getHour();
            if (isCurrent && transactions.size() < 1 && hour >= 12) {
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code " + whereQuery
                    + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code " + whereQuery
                    + " ORDER BY a.id DESC";
            }
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            Double totalValue = tValue != null ? tValue.doubleValue() : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
//            SQL = "SELECT MIN(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
//            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
//            SQL = "SELECT MAX(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
//            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
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
    public ResponseEntity GetFTTimeAverage(String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
                    + "FROM "+table+" a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.response_code = '00'";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            
            String table_ = isCurrent ? "ajiswitch_db.tbl_name_enquiries" : "ajiswitch_db.tbl_name_enquiries_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
                    + "FROM "+table_+" a "
                    + "WHERE a.transactiondate BETWEEN ? AND ? AND a.response_code = '00'";
            
            List<Map<String, Object>> summary_ = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            
            summary.addAll(summary_);
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transaction Duration Average");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFTTimeAverage(String institutioncode, String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
                    + "FROM "+table+" a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.response_code = '00' AND a.source_institution_code = ? ";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            
            String table_ = isCurrent ? "ajiswitch_db.tbl_name_enquiries" : "ajiswitch_db.tbl_name_enquiries_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
                    + "FROM "+table_+" a "
                    + "WHERE a.transactiondate BETWEEN ? AND ? AND a.response_code = '00' AND a.destination_institution_code = ? ";
            
            List<Map<String, Object>> summary_ = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            
            summary.addAll(summary_);
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transaction Duration Average");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSuccessTNXVolume(String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
//            SQL = "SELECT COUNT(a.id) as volume, a.transaction_date_time as label "
//                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.response_code = '00'"
//                    + "GROUP BY CAST(a.transaction_date_time as DATE) "
//                    + "ORDER BY a.transaction_date_time DESC";
//            
//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
//            
            SQL = "SELECT COUNT(a.id) as volume, a.transaction_date_time as label "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND a.response_code = '00'"
                    + "GROUP BY CAST(a.transaction_date_time as DATE) "
                    + "ORDER BY a.transaction_date_time DESC";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            
//            summary.addAll(summary_);
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Successfull Transactions Summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSuccessTNXVolume(String institutioncode, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
//            SQL = "SELECT COUNT(a.id) as volume, a.transaction_date_time as label "
//                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.response_code = '00' AND a.source_institution_code = ? "
//                    + "GROUP BY CAST(a.transaction_date_time as DATE) "
//                    + "ORDER BY a.transaction_date_time DESC";
//            
//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            
            SQL = "SELECT COUNT(a.id) as volume, a.transaction_date_time as label "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND a.response_code = '00' AND a.source_institution_code = ? "
                    + "GROUP BY CAST(a.transaction_date_time as DATE) "
                    + "ORDER BY a.transaction_date_time DESC";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            
//            summary.addAll(summary_);
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Successfull Transactions Summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTop6ResponseCodesTNX(String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM "+table+" a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ?"
                    + "GROUP BY a.response_code "
                    + "LIMIT 6";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Top 6 Response Codes Summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTop6ResponseCodesTNX(String institutioncode, String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM "+table+" a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ? "
                    + "GROUP BY a.response_code "
                    + "LIMIT 6";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Top 6 Response Codes Summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTransactionsVolumeByChannels(String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.channel_code as label "
                    + "FROM "+table+" a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ?"
                    + "GROUP BY a.channel_code "
                    + "LIMIT 6";
            
            List<ChannelsTnxValueModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionChannelsSummaryMapper());
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions Volumes by Channel");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTransactionsVolumeByChannels(String institutioncode, String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.channel_code as label "
                    + "FROM "+table+" a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ? "
                    + "GROUP BY a.channel_code "
                    + "LIMIT 6";
            
            List<ChannelsTnxValueModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode}, new TransactionChannelsSummaryMapper());
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions Volumes by Channel");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
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
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.source_institution_code";
            
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
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
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.source_institution_code = ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";
            
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "WHERE a.source_institution_code != ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";

            List<TransactionSummaryModel> summaryOthers = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            summary.add(summaryOthers.get(0));
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.destination_institution_code = ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";
            
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
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
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.destination_institution_code "
                    + "ORDER BY a.destination_institution_code";
                
                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.destination_institution_code "
                    + "ORDER BY a.destination_institution_code";
            }
            else {
                SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.source_institution_code "
                    + "ORDER BY a.source_institution_code";
                
                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
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
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.destination_institution_code = ? AND a.transaction_date_time BETWEEN ? AND ?";
                
                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.destination_institution_code = ? AND a.transaction_date_time BETWEEN ? AND ?";
            }
            else {
                SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.response_code = '00' AND a.source_institution_code = ? AND a.transaction_date_time BETWEEN ? AND ?";
                
                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
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
    public ResponseEntity SearchTransactionsForSessionIds(String sessionids) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL = "SELECT a.session_id, a.originator_account_name, a.originator_account_number, a.originator_kyc, a.beneficiary_account_name, a.beneficiary_account_number, a.beneficiary_kyc, a.name_enquiry_ref, a.txn_duration, a.response_date_time, a.response_code, a.transaction_date_time, a.amount, "
                    + "b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id IN ("+sessionids+")";
            List<TransactionHalfModel> transactions = jdbcTemplate.query(SQL, new TransactionHalfMapper());
            SQL = "SELECT a.session_id, a.originator_account_name, a.originator_account_number, a.originator_kyc, a.beneficiary_account_name, a.beneficiary_account_number, a.beneficiary_kyc, a.name_enquiry_ref, a.txn_duration, a.response_date_time, a.response_code, a.transaction_date_time, a.amount, "
                    + "b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id IN ("+sessionids+")";
            List<TransactionHalfModel> transactions_s = jdbcTemplate.query(SQL, new TransactionHalfMapper());
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions For Uploaded Session IDs");
            transactions.addAll(transactions_s);
            networkResponse.setData((ArrayList) transactions);
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
                            + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                            + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                            + "ON a.source_institution_code = b.code "
                            + "WHERE a.response_code = '00' AND a.source_institution_code = ? "
                            + "AND a.transaction_date_time BETWEEN ? AND ?"
                            + "GROUP BY MONTH(a.transaction_date_time)";
                    break;
                case "day":
                default: 
                    SQL = "SELECT a.transaction_date_time as label, COUNT(a.id) as volume, SUM(a.amount) as value, b.name, b.shortName, b.color, b.code "
                            + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                            + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                            + "ON a.source_institution_code = b.code "
                            + "WHERE a.response_code = '00' AND a.source_institution_code = ? "
                            + "AND a.transaction_date_time BETWEEN ? AND ? "
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
            SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
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
            String meta = "{\"totalValue\": " +totalValue+ "}";
           
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
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.id = ? ";
                transactions = jdbcTemplate.query(SQL, new Object[]{id}, new FullTransactionMapper());
            }
            else {
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
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
//                SQL = "SELECT MIN(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
//                String minDate = jdbcTemplate.queryForObject(SQL, String.class);
//                SQL = "SELECT MAX(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
//                String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " +totalValue+ "}";
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
            SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
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
    public ResponseEntity GetBySessionId(String id, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<FullTransactionModel> transactions;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers a " : "ajiswitch_db.tbl_creditfundtransfer_hist_s a ";
            SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM "+table
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
    public ResponseEntity GetDisputes(String institution, int page, int limit) {
        return GetDisputes(0, 0, institution, page, limit);
    }
    
    @Override
    public ResponseEntity GetDisputes(int id) {
        return GetDisputes(id, 0, null, 0, 0);
    }
    
    @Override
    public ResponseEntity GetSettlements(int id) {
        return GetDisputes(id, 1, null, 0, 0);
    }
    
    @Override
    public ResponseEntity GetSettlements(String institution) {
        return GetDisputes(0, 1, institution, 0, 0);
    }
    
    @Override
    public ResponseEntity GetDisputes(int id, int status, String institutioncode, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String code = institutioncode != null ? institutioncode : "";
            String SQL, SQL2;
            List<DisputeModel> transactions;
            Double totalValue;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> agg = null;
            if (id > 0) {
                    SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                        + "FROM tbl_disputes dispute "
                            + "LEFT JOIN tbl_financial_institution_contacts a "
                            + "ON dispute.loggedBy = a.email_address "
                    + "WHERE dispute.id = ?";
                transactions = jdbcTemplate.query(SQL, new Object[]{id}, new DisputeTransactionMapper());
            }
            else {
                switch (code) {
                    case "":
                    case "-1":
                        if (status == 0){
                            SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN tbl_financial_institution_contacts a "
                                + "ON dispute.loggedBy = a.email_address "
                                + "WHERE dispute.resolved = 0 || (dispute.status = 1 AND dispute.resolved = 1) "
                                + "ORDER BY dispute.id DESC LIMIT ? OFFSET ?";
                            
                            SQL2 = "SELECT SUM(dispute.amount) as totalValue, COUNT(dispute.id) as totalRecords "
                                + "FROM tbl_disputes dispute "
                                + "WHERE dispute.resolved = 0 || (dispute.status = 1 AND dispute.resolved = 1)";
                        }
                        else {
                            SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN tbl_financial_institution_contacts a "
                                + "ON dispute.loggedBy = a.email_address "
                                + "WHERE dispute.resolved = 1 "
                                + "ORDER BY dispute.id DESC LIMIT ? OFFSET ?";
                            
                            SQL2 = "SELECT SUM(dispute.amount) as totalValue, COUNT(dispute.id) as totalRecords "
                                + "FROM tbl_disputes dispute "
                                + "WHERE dispute.resolved = 1";
                        }
                        break;
                    default:
                        if (status == 0){
                            SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN tbl_financial_institution_contacts a "
                                + "ON dispute.loggedBy = a.email_address "
                                + "WHERE ((dispute.status = 1 AND dispute.resolved = 1) || dispute.resolved = 0) AND (dispute.ownerInstitution = " + code + " OR dispute.destInstitution = " + code+") "
                                + "ORDER BY dispute.id DESC LIMIT ? OFFSET ?";
                            
                            SQL2 = "SELECT SUM(dispute.amount) as totalValue, COUNT(dispute.id) as totalRecords "
                                + "FROM tbl_disputes dispute "
                                + "WHERE ((dispute.status = 1 AND dispute.resolved = 1) || dispute.resolved = 0) AND (dispute.ownerInstitution = " + code + " OR dispute.destInstitution = " + code+")";
                        }
                        else {
                            SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN tbl_financial_institution_contacts a "
                                + "ON dispute.loggedBy = a.email_address "
                                + "WHERE dispute.ownerInstitution = " + code + " OR dispute.destInstitution = " + code+""
                                + " ORDER BY dispute.id DESC LIMIT ? OFFSET ?";
                            
                            SQL2 = "SELECT SUM(dispute.amount) as totalValue, COUNT(dispute.id) as totalRecords "
                                + "FROM tbl_disputes dispute "
                                + "WHERE dispute.ownerInstitution = " + code + " OR dispute.destInstitution = " + code+"";
                        }
                        break;
                }
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new DisputeTransactionMapper());
                agg = jdbcTemplate.queryForList(SQL2);
            }
            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            totalValue = tValue != null ? tValue.doubleValue() : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            
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
    public ResponseEntity SearchDisputes(
            String sessionid,
            String response_code,
            String source_bank,
            String beneficiary_bank,
            String dispute_status,
            String date_logged,
            String date_resolved,
            String timeline_date,
            int page,
            int limit
        ) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String start_date_logged = !date_logged.equals("") ? date_logged.substring(0, 10) : "";
            String end_date_logged = !date_logged.equals("") ? date_logged.substring(11, date_logged.length()) : "";
            String start_date_resolved = !date_resolved.equals("") ? date_resolved.substring(0, 10) : "";
            String end_date_resolved = !date_resolved.equals("") ? date_resolved.substring(11, date_resolved.length()) : "";
            String start_timeline_date = !timeline_date.equals("") ? timeline_date.substring(0, 10) : "";
            String end_timeline_date = !timeline_date.equals("") ? timeline_date.substring(11, timeline_date.length()) : "";
            String SQL;
            Double totalValue;
            List<Map<String, Object>> agg;
            List<DisputeModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            String whereQuery = !sessionid.equals("")
                    || !response_code.equals("")
                    || !source_bank.equals("")
                    || !beneficiary_bank.equals("")
                    || !dispute_status.equals("")
                    || !start_date_logged.equals("")
                    || !end_date_logged.equals("")
                    || !start_date_resolved.equals("")
                    || !end_date_resolved.equals("")
                    || !start_timeline_date.equals("")
                    || !end_timeline_date.equals("")
                    ? "WHERE" : "";
            
            if (!sessionid.equals("")) {
                whereQuery+=" a.transactionSessionid = '" + sessionid + "'";
            }
            if (!source_bank.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.ownerInstitution = '" + source_bank + "'";
            }
            if (!beneficiary_bank.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.destInstitution = '" + beneficiary_bank + "'";
            }
            switch(dispute_status){
                case "-1":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" a.status = -1 AND a.resolved = 0";
                break;
                case "0":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" a.status = 0 AND a.resolved = 0";
                break;
                case "1":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" a.status = 1 AND a.resolved = 1";
                break;
                case "2":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" a.status = 0 AND a.resolved = 1";
                break;
//                case "":
//                    whereQuery = whereQuery.equals("") ? "WHERE" : !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
//                    whereQuery+=" a.status != -1 AND a.resolved != 1";
//                    break;
                default:
                    break;
            }
            if (!start_date_logged.equals("") && !end_date_logged.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.date_created BETWEEN '" + start_date_logged + "' AND '" + end_date_logged + "'";
            }
            if (!start_date_resolved.equals("") && !end_date_resolved.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.date_modified BETWEEN '" + start_date_resolved + "' AND '" + end_date_resolved + "'";
            }
            if (!start_timeline_date.equals("") && !end_timeline_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.timeline_date BETWEEN '" + start_timeline_date + "' AND '" + end_timeline_date + "'";
            }
            SQL = "SELECT a.id, a.transactionSessionid as session_id, a.transactionid, a.amount, a.originator_account_name, a.beneficiary_account_name, a.transaction_date_time, a.ownerInstitutionName as srcInstitutionName, a.destInstitutionName, a.loggedBy, a.resolvedBy, a.ownerInstitution, a.destInstitution, a.type, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_reject_uri, b.financial_institution_code "
                                + "FROM tbl_disputes a "
                                + "LEFT JOIN tbl_financial_institution_contacts b "
                                + "ON a.loggedBy = b.email_address "
                                + whereQuery
                                + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new DisputeTransactionMapper());

            SQL = "SELECT "
                    + "SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM tbl_disputes a "
//                    + "LEFT JOIN sparkpay.transaction_hist_s b "
//                    + "ON a.id = b.id " 
                    + whereQuery;
            agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            totalValue = tValue != null ? tValue.doubleValue() : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + 1 +", \"limit\": " + null +"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched Disputes Results");
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
    public ResponseEntity LogDisputesBulk(String sessiontoken, String records, String sourceInstitution, String username){
        try {
            
            JSONArray jsonRecords = new JSONArray(records);
            int found = 0;
            int recorded = 0;
            
            for (int i = 0; i < jsonRecords.length(); i++) {
                String sessionId = jsonRecords.getJSONObject(i).getString("sessionid");
                boolean sessionIdExist = CheckSessionId(sessionId);
                if (!sessionIdExist) {
                    List<FullTransactionModel> getTransaction = GetTransactionFromHistory(sessionId, sourceInstitution);
                    if (getTransaction.size() > 0) {
                        found++;
                        if (getTransaction.get(0).getSrcResponsecode().equals("00")) {
                            String SQL;
                            int additionalDays = dateUtil.getDisputeTimeLineDate();
                            SQL = "INSERT into tbl_disputes(transactionSessionid, transactionid, amount, originator_account_name, beneficiary_account_name, transaction_date_time, loggedBy, ownerInstitution, destInstitution, ownerInstitutionName, destInstitutionName, status, date_created, timeline_date) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', now(), ADDDATE(now(), ?))";
                            int retval = jdbcTemplate.update(SQL, new Object[]{sessionId, getTransaction.get(0).getId(), getTransaction.get(0).getSrcAmount(), getTransaction.get(0).getSrcAccountName(), getTransaction.get(0).getDestAccountName(), getTransaction.get(0).getTransactiondate(), username, getTransaction.get(0).getSrcInstitutioncode(), getTransaction.get(0).getDestInstitutioncode(), getTransaction.get(0).getSrcInstitutionName(), getTransaction.get(0).getDestInstitutionName(), additionalDays});
                            if (retval > 0) 
                                recorded++;
                        }
                    }
                }
            }
            
            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Total Records: " + jsonRecords.length() + "\nValid Records: " + found + "\nRecorded: " + recorded);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        } catch (JSONException ex) {
            Logger.getLogger(TransactionsService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
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
            List<FullTransactionModel> getTransaction = GetTransactionFromHistory(sessionId, sourceInstitution);
            if (getTransaction.size() > 0) {
                if (!getTransaction.get(0).getSrcResponsecode().equals("00")) {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(404);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Declined Transaction cannot be logged for dispute");
                    return responseManager.ResponseOk(networkResponse);
                }
                String SQL;
//                int userrole = GetUserRole(username, sessiontoken);
                int additionalDays = dateUtil.getDisputeTimeLineDate();
                SQL = "INSERT into tbl_disputes(transactionSessionid, transactionid, amount, originator_account_name, beneficiary_account_name, transaction_date_time, loggedBy, ownerInstitution, destInstitution, ownerInstitutionName, destInstitutionName, status, date_created, timeline_date) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', now(), ADDDATE(now(), ?))";
                int retval = jdbcTemplate.update(SQL, new Object[]{sessionId, getTransaction.get(0).getId(), getTransaction.get(0).getSrcAmount(), getTransaction.get(0).getSrcAccountName(), getTransaction.get(0).getDestAccountName(), getTransaction.get(0).getTransactiondate(), username, getTransaction.get(0).getSrcInstitutioncode(), getTransaction.get(0).getDestInstitutioncode(), getTransaction.get(0).getSrcInstitutionName(), getTransaction.get(0).getDestInstitutionName(), additionalDays});
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
            ResponseCodeInterpreter responseCodeInterpreter = new ResponseCodeInterpreter();
            DisputeModel response = new DisputeModel();            
            response.setId(rs.getInt("id"));
            response.setTransactionId(rs.getInt("transactionid"));
            response.setType(rs.getString("type"));
            response.setLoggedBy(rs.getString("loggedBy"));
            response.setResolvedBy(rs.getString("resolvedBy"));
            response.setStatus(rs.getInt("status"));
            response.setResolved(rs.getInt("resolved"));
            response.setDateModified(rs.getString("date_modified"));
            response.setDateCreated(rs.getString("date_created"));
            response.setTimeline_date(rs.getString("timeline_date"));
            response.setSrcSessionid(rs.getString("session_id"));
//            response.setSrcAccountNumber(rs.getString("originator_account_number"));
            response.setSrcAccountName(rs.getString("originator_account_name"));
//            response.setSrcKycLevel(rs.getString("originator_kyc"));
//            response.setSrcBvn(rs.getString("originator_bvn"));
            response.setSrcAmount(rs.getString("amount"));
            response.setSrcInstitutioncode(rs.getString("ownerInstitution"));
//            response.setDestSessionId(rs.getString("session_id"));
//            response.setSrcResponsecode(rs.getString("response_code"));
//            response.setResponseCodeDefinition(responseCodeInterpreter.InterpreteCode(rs.getString("response_code") == null || rs.getString("response_code").equals("null") ? "" : rs.getString("response_code")));
//            response.setDestAccountNumber(rs.getString("beneficiary_account_number"));
            response.setDestAccountName(rs.getString("beneficiary_account_name"));
//            response.setDestKycLevel(rs.getString("beneficiary_kyc"));
//            response.setDestBvn(rs.getString("beneficiary_bvn"));
//            response.setDestAmount(rs.getString("amount"));
            response.setDestInstitutioncode(rs.getString("destInstitution"));
//            response.setDestResponseCode(rs.getString("response_code"));
//            response.setNarration(rs.getString("narration"));
            response.setTransactiondate(rs.getString("transaction_date_time"));
//            response.setUsername(rs.getString("name_enquiry_ref"));
            response.setSrcInstitutionName(rs.getString("srcInstitutionName"));
            response.setDestInstitutionName(rs.getString("destInstitutionName"));
            response.setProof_of_reject_uri(rs.getString("proof_of_reject_uri"));
            response.setLoggingInstitution(rs.getString("financial_institution_code"));
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
    
    class TransactionChannelsSummaryMapper implements RowMapper<ChannelsTnxValueModel> {
        @Override
        public ChannelsTnxValueModel mapRow(ResultSet rs, int arg1) throws SQLException {
            ChannelsTnxValueModel response = new ChannelsTnxValueModel();
            response.setVolume(rs.getInt("volume"));
            response.setChannel(rs.getString("label"));
            switch (rs.getString("label")) {
                case "1":
                    response.setChannelCode("Bank Teller");
                    break;
                case "2":
                    response.setChannelCode("Internet Banking");
                    break;
                case "3":
                    response.setChannelCode("Mobile Phone");
                    break;
                case "4":
                    response.setChannelCode("POS Terminals");
                    break;
                case "5":
                    response.setChannelCode("ATM");
                    break;
                case "6":
                    response.setChannelCode("Vendor/Merchant Portal");
                    break;
                case "7":
                    response.setChannelCode("3rd Party Platform");
                    break;
                case "8":
                    response.setChannelCode("USSD");
                    break;
                case "9":
                    response.setChannelCode("Other Channel");
                    break;
                case "10":
                    response.setChannelCode("Social Media");
                    break;
                case "11":
                    response.setChannelCode("Agency Banking");
                    break;
                case "12":
                    response.setChannelCode("NQR");
                    break;
                default:
                    response.setChannelCode("");
                    break;
            }
            return response;
        }
    }
    
    class FullTransactionMapper implements RowMapper<FullTransactionModel> {
        @Override
        public FullTransactionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            FullTransactionModel response = new FullTransactionModel();            
            ResponseCodeInterpreter responseCodeInterpreter = new ResponseCodeInterpreter();
            response.setId(rs.getInt("id"));
            response.setSrcSessionid(rs.getString("session_id"));
            response.setPaymentReference(rs.getString("payment_reference"));
            response.setSrcAccountNumber(rs.getString("originator_account_number"));
            response.setSrcAccountName(rs.getString("originator_account_name"));
            response.setSrcKycLevel(rs.getString("originator_kyc"));
            response.setSrcBvn(rs.getString("originator_bvn"));
            response.setSrcAmount(rs.getString("amount"));
            response.setSrcInstitutioncode(rs.getString("source_institution_code"));
            response.setDestSessionId(rs.getString("session_id"));
            response.setSrcResponsecode(rs.getString("response_code"));
            response.setResponseCodeDefinition(responseCodeInterpreter.InterpreteCode(rs.getString("response_code") == null || rs.getString("response_code").equals("null") ? "" : rs.getString("response_code")));
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
            response.setTxnDuration(rs.getString("txn_duration"));
            response.setResponsedatetime(rs.getString("response_date_time"));
            switch (rs.getString("channel_code")) {
                case "1":
                    response.setChannelCode("Bank Teller");
                    break;
                case "2":
                    response.setChannelCode("Internet Banking");
                    break;
                case "3":
                    response.setChannelCode("Mobile Phone");
                    break;
                case "4":
                    response.setChannelCode("POS Terminals");
                    break;
                case "5":
                    response.setChannelCode("ATM");
                    break;
                case "6":
                    response.setChannelCode("Vendor/Merchant Portal");
                    break;
                case "7":
                    response.setChannelCode("3rd Party Platform");
                    break;
                case "8":
                    response.setChannelCode("USSD");
                    break;
                case "9":
                    response.setChannelCode("Other Channel");
                    break;
                case "10":
                    response.setChannelCode("Social Media");
                    break;
                case "11":
                    response.setChannelCode("Agency Banking");
                    break;
                case "12":
                    response.setChannelCode("NQR");
                    break;
                default:
                    response.setChannelCode("");
                    break;
            }
            return response;
        }
    }
    
    @Override
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, String username, int status, String proof_of_reject_uri) {
        try {
            String SQL;
//            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
//            switch (userrole) {
//                case 1:
//                case 3:
                    int resolved = status == 0 ? 0 : 1;
                    SQL = "UPDATE tbl_disputes SET resolvedBy = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, id});
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
            ResponseCodeInterpreter responseCodeInterpreter = new ResponseCodeInterpreter();
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
            response.setResponseCodeDefinition(responseCodeInterpreter.InterpreteCode(rs.getString("response_code") == null || rs.getString("response_code").equals("null") ? "" : rs.getString("response_code")));
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
    
    class TransactionHalfMapper implements RowMapper<TransactionHalfModel> {
        @Override
        public TransactionHalfModel mapRow(ResultSet rs, int arg1) throws SQLException {
            TransactionHalfModel response = new TransactionHalfModel();    
            ResponseCodeInterpreter responseCodeInterpreter = new ResponseCodeInterpreter();
            response.setSessionid(rs.getString("session_id"));
            response.setSrcAccountName(rs.getString("originator_account_name"));
            response.setSrcAccountNumber(rs.getString("originator_account_number"));
            response.setSrcAccountKYC(rs.getString("originator_kyc"));
            response.setSrcAccountBank(rs.getString("srcInstitutionName"));
            response.setDestAccountName(rs.getString("beneficiary_account_name"));
            response.setDestAccountNumber(rs.getString("beneficiary_account_number"));
            response.setDestAccountKYC(rs.getString("beneficiary_kyc"));
            response.setDestAccountBank(rs.getString("destInstitutionName"));
            response.setAmount(rs.getString("amount"));
            response.setResponsecode(rs.getString("response_code"));
            response.setResponsecodedefinition(responseCodeInterpreter.InterpreteCode(rs.getString("response_code") == null || rs.getString("response_code").equals("null") ? "" : rs.getString("response_code")));
            response.setTransactiondate(rs.getString("transaction_date_time"));
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
    
    @Override
    public ResponseEntity GetCommissions(String institutionCode, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> commissions;
            if (institutionCode.equals("-1") || institutionCode.equals("000013")) {
                SQL = "SELECT a.*, b.institution_name "
                    + "FROM ajiswitch_db.tbl_commission_paid a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.institution_code = b.institution_code "
                    + "WHERE a.generation_date >= ? AND a.generation_date < ? "
                    + "ORDER BY a.generation_date DESC";
                commissions = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
                SQL = "SELECT COUNT(id) as totalRecords, SUM(commission) as totalValue "
                    + "FROM ajiswitch_db.tbl_commission_paid "
                    + "WHERE generation_date >= ? AND generation_date < ?";
                
                List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
                Map<String, Object> row = agg.get(0);
                BigDecimal tValue = (BigDecimal) row.get("totalValue");
                Double totalValue = tValue != null ? tValue.doubleValue() : 0;
                Long tRecords = (Long) row.get("totalRecords");
                int totalRecords = tRecords != null ? tRecords.intValue() : 0;
                String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +"}";
                networkResponse.setMeta(meta);
            } else {
                SQL = "SELECT a.*, b.institution_name "
                    + "FROM ajiswitch_db.tbl_commission_paid a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.institution_code = b.institution_code "
                    + "WHERE a.institution_code = ?"
                    + "AND a.generation_date >= ? AND a.generation_date < ? "
                    + "ORDER BY a.generation_date DESC";
                commissions = jdbcTemplate.queryForList(SQL, new Object[]{institutionCode, startDate, endDate});
                SQL = "SELECT COUNT(id) as totalRecords, SUM(commission) as totalValue "
                    + "FROM ajiswitch_db.tbl_commission_paid "
                    + "WHERE institution_code = ?"
                    + "AND generation_date >= ? AND generation_date < ?";
                
                List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{institutionCode, startDate, endDate});
                Map<String, Object> row = agg.get(0);
                BigDecimal tValue = (BigDecimal) row.get("totalValue");
                Double totalValue = tValue != null ? tValue.doubleValue() : 0;
                Long tRecords = (Long) row.get("totalRecords");
                int totalRecords = tRecords != null ? tRecords.intValue() : 0;
                String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +"}";
                networkResponse.setMeta(meta);
            }
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All commission");
            networkResponse.setData((ArrayList) commissions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
}
