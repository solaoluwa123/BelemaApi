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
    
    public List<FullTransactionModel> GetTransaction(String sessionId, String amount, String destination) {
        String SQL = "SELECT a.id, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, "
                    + "a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destInstitutioncode = c.code "
                    + "WHERE a.srcSessionid = ?";
        
        List<FullTransactionModel> transactions = jdbcTemplate.query(SQL, new Object[]{sessionId}, new FullTransactionMapper());
        return transactions;
    }
    
    @Override
    public ResponseEntity Get() {
        return Get(0);
    }
    
    @Override
    public ResponseEntity SearchTransactions(String srcSessionid,
            String srcInstitutioncode,
            String destInstitutioncode,
            String minAmount,
            String maxAmount,
            String srcAccountName,
            String destAccountName,
            String startDate,
            String endDate
    ) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery = !srcSessionid.equals("") 
                    || !srcInstitutioncode.equals("") 
                    || !srcInstitutioncode.equals("")
                    || !destInstitutioncode.equals("")
                    || !srcAccountName.equals("")
                    || !destAccountName.equals("")
                    || !startDate.equals("")
                    || !endDate.equals("")
                    || (!minAmount.equals("") && Double.parseDouble(minAmount) > 0)
                    || (!maxAmount.equals("") && Double.parseDouble(maxAmount) > 0)
                    ? "WHERE" : "";
            
            if (!srcSessionid.equals("")) {
                whereQuery+=" a.srcSessionid = '" + srcSessionid + "'";
            }
            if (!srcInstitutioncode.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.srcInstitutioncode = " + srcInstitutioncode;
            }
            if (!destInstitutioncode.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.destInstitutioncode = " + destInstitutioncode;
            }
            if (!srcAccountName.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.srcAccountName LIKE '%" + srcAccountName+"%'";
            }
            if (!destAccountName.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.destAccountName LIKE '%" + destAccountName+"%'";
            }
            if ((!minAmount.equals("") && Double.parseDouble(minAmount) > 0)) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.srcAmount >= " + minAmount;
            }
            if ((!maxAmount.equals("") && Double.parseDouble(maxAmount) > 0)) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.srcAmount <= " + maxAmount;
            }
            if (!startDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.transactiondate >= '" + startDate + "'";
            }
            if (!endDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.transactiondate < '" + endDate + "'";
            }
            String SQL;
            List<FullTransactionModel> transactions;
            SQL = "SELECT a.id, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, "
                + "a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, b.name as srcInstitutionName, c.name as destInstitutionName "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.srcInstitutioncode = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destInstitutioncode = c.code " + whereQuery
                + " ORDER BY a.id DESC";
            transactions = jdbcTemplate.query(SQL, new FullTransactionMapper());

            SQL = "SELECT SUM(a.srcAmount) as totalValue "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.srcInstitutioncode = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destInstitutioncode = c.code " + whereQuery
                + " ORDER BY a.id DESC";
            Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
            totalValue = totalValue != null ? totalValue : 0;
            SQL = "SELECT MIN(transactiondate) from ajiswitch_db.tbl_creditfundtransfers";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(transactiondate) from ajiswitch_db.tbl_creditfundtransfers";
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
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.srcAmount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "WHERE a.srcResponsecode = '00' AND a.transactiondate BETWEEN ? AND ? "
                    + "GROUP BY a.srcInstitutioncode";
            
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.destAmount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.destInstitutioncode = b.code "
                    + "WHERE a.destResponseCode = '00' AND a.transactiondate BETWEEN ? AND ? "
                    + "GROUP BY a.destInstitutioncode";
            
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
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.srcAmount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "WHERE a.srcInstitutioncode = ? AND a.srcResponsecode = '00' AND a.transactiondate BETWEEN ? AND ?";
            
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.destAmount) as value, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.destInstitutioncode = b.code "
                    + "WHERE a.destInstitutioncode = ? AND a.destResponseCode = '00' AND a.transactiondate BETWEEN ? AND ?";
            
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
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
    public ResponseEntity GetTransactionsRates(String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            if (startDate.contains("T"))
                startDate = startDate.replace("T", " ");
            if (endDate.contains("T"))
                endDate = endDate.replace("T", " ");
            String SQL;
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.srcResponsecode = '00'";
//            int totalSuccessFul = jdbcTemplate.queryForObject(SQL, int.class);
            SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgatepay_db.tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "WHERE a.srcResponsecode = '00' AND a.transactiondate BETWEEN ? AND ? "
                    + "GROUP BY a.srcInstitutioncode "
                    + "ORDER BY a.srcInstitutioncode";
            
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
            
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.srcResponsecode != '00'";
//            int totalFailures = jdbcTemplate.queryForObject(SQL, int.class);
            SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgatepay_db.tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "WHERE a.transactiondate BETWEEN ? AND ? "
                    + "GROUP BY a.srcInstitutioncode "
                    + "ORDER BY a.srcInstitutioncode";
            
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
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
    public ResponseEntity GetTransactionsRates(String institutioncode, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.srcResponsecode = '00'";
//            int totalSuccessFul = jdbcTemplate.queryForObject(SQL, int.class);
            SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgatepay_db.tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "WHERE a.srcResponsecode = '00' AND a.srcInstitutioncode = ? AND a.transactiondate BETWEEN ? AND ?";
            
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.srcResponsecode != '00'";
//            int totalFailures = jdbcTemplate.queryForObject(SQL, int.class);
            SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgatepay_db.tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "WHERE a.srcInstitutioncode = ? AND a.transactiondate BETWEEN ? AND ?";
            
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            
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
                    SQL = "SELECT a.transactiondate as label, COUNT(a.id) as volume, SUM(a.srcAmount) as value, b.name, b.shortName, b.color, b.code "
                            + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                            + "LEFT JOIN transgatepay_db.tbl_financial_institutions b "
                            + "ON a.srcInstitutioncode = b.code "
                            + "WHERE a.srcResponsecode = '00' AND a.srcInstitutioncode = ? "
                            + "AND a.transactiondate BETWEEN ? AND ?"
                            + "GROUP BY MONTH(a.transactiondate)";
                    break;
                case "day":
                default: 
                    SQL = "SELECT a.transactiondate as label, COUNT(a.id) as volume, SUM(a.srcAmount) as value, b.name, b.shortName, b.color, b.code "
                            + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                            + "LEFT JOIN transgatepay_db.tbl_financial_institutions b "
                            + "ON a.srcInstitutioncode = b.code "
                            + "WHERE a.srcResponsecode = '00' AND a.srcInstitutioncode = ? "
                            + "AND a.transactiondate BETWEEN ? AND ?"
                            + "GROUP BY CAST(a.transactiondate as DATE)";
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
            SQL = "SELECT a.id, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, "
                + "a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, b.name as srcInstitutionName, c.name as destInstitutionName "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.srcInstitutioncode = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destInstitutioncode = c.code "
                + "WHERE a.srcInstitutioncode = ? OR a.destInstitutioncode = ? ORDER BY a.id DESC";
            transactions = jdbcTemplate.query(SQL, new Object[]{institutioncode, institutioncode}, new FullTransactionMapper());
            
            SQL = "SELECT SUM(a.srcAmount) as totalValue "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                + "ON a.srcInstitutioncode = b.code "
                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                + "ON a.destInstitutioncode = c.code "
                + "WHERE a.srcInstitutioncode = ? OR a.destInstitutioncode = ? ORDER BY a.id DESC";
            Double totalValue = jdbcTemplate.queryForObject(SQL, new Object[]{institutioncode, institutioncode}, Double.class);
            totalValue = totalValue != null ? totalValue : 0;
            SQL = "SELECT MIN(transactiondate) from ajiswitch_db.tbl_creditfundtransfers";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(transactiondate) from ajiswitch_db.tbl_creditfundtransfers";
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
                SQL = "SELECT a.id, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, "
                    + "a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destInstitutioncode = c.code "
                    + "WHERE a.id = ? ";
                transactions = jdbcTemplate.query(SQL, new Object[]{id}, new FullTransactionMapper());
            }
            else {
                SQL = "SELECT a.id, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, "
                    + "a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destInstitutioncode = c.code "
                    + "ORDER BY a.id DESC";
                transactions = jdbcTemplate.query(SQL, new FullTransactionMapper());
                
                SQL = "SELECT SUM(a.srcAmount) as totalValue "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.srcInstitutioncode = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destInstitutioncode = c.code ";
                Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
                totalValue = totalValue != null ? totalValue : 0;
                SQL = "SELECT MIN(transactiondate) from ajiswitch_db.tbl_creditfundtransfers";
                String minDate = jdbcTemplate.queryForObject(SQL, String.class);
                SQL = "SELECT MAX(transactiondate) from ajiswitch_db.tbl_creditfundtransfers";
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
//            String appendQueryTwo = institutioncode == null ? "" : institutioncode.equals("-1") ? "" : " AND a.srcInstitutioncode = " + institutioncode + " OR a.destInstitutioncode = " + institutioncode;
            String SQL;
            List<DisputeModel> transactions;
            Double totalValue = 0.00;
            if (id > 0) {
                SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                        + "a.id as transactionid, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, "
                        + "b.name as srcInstitutionName, c.name as destInstitutionName "
                        + "FROM tbl_disputes dispute "
                        + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                        + "ON dispute.transactionSessionid = a.srcSessionid "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                        + "ON a.srcInstitutioncode = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                        + "ON a.destInstitutioncode = c.code "
                    + "WHERE dispute.id = ?";
                transactions = jdbcTemplate.query(SQL, new Object[]{id}, new DisputeTransactionMapper());
            }
            else {
                switch (code) {
                    case "":
                    case "-1":
                        if (status == 0){
                            SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                                + "a.id as transactionid, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, "
                                + "b.name as srcInstitutionName, c.name as destInstitutionName "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.srcSessionid "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                                + "ON a.srcInstitutioncode = b.code "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                                + "ON a.destInstitutioncode = c.code WHERE dispute.resolved = 0"
                                + " ORDER BY a.id DESC ";
                            
                            String SQL2 = "SELECT SUM(a.srcAmount) as totalValue "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.srcSessionid "
                                + "WHERE dispute.resolved = 0";
                            totalValue = jdbcTemplate.queryForObject(SQL2, Double.class);
                        }
                        else {
                            SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                                + "a.id as transactionid, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, "
                                + "b.name as srcInstitutionName, c.name as destInstitutionName "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.srcSessionid "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                                + "ON a.srcInstitutioncode = b.code "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                                + "ON a.destInstitutioncode = c.code WHERE dispute.resolved = 1"
                                + " ORDER BY a.id DESC ";
                            
                            String SQL2 = "SELECT SUM(a.srcAmount) as totalValue "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.srcSessionid "
                                + "WHERE dispute.resolved = 1";
                            totalValue = jdbcTemplate.queryForObject(SQL2, Double.class);
                        }
                        break;
                    default:
                        if (status == 0){
                            SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                                + "a.id as transactionid, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, "
                                + "b.name as srcInstitutionName, c.name as destInstitutionName "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.srcSessionid "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                                + "ON a.srcInstitutioncode = b.code "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                                + "ON a.destInstitutioncode = c.code WHERE dispute.resolved = 0 AND (a.srcInstitutioncode = " + code + " OR a.destInstitutioncode = " + code+")"
                                + " ORDER BY a.id DESC ";
                            
                            String SQL2 = "SELECT SUM(a.srcAmount) as totalValue "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.srcSessionid "
                                + "WHERE dispute.resolved = 0 AND (a.srcInstitutioncode = " + code + " OR a.destInstitutioncode = " + code+")";
                            totalValue = jdbcTemplate.queryForObject(SQL2, Double.class);
                        }
                        else {
                            SQL = "SELECT dispute.id, dispute.loggedBy, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, "
                                + "a.id as transactionid, a.srcSessionid, a.srcAccountNumber, a.srcAccountName, a.srcKycLevel, a.srcBvn, a.srcAmount, a.srcInstitutioncode, a.destSessionId, a.srcResponsecode, a.destAccountNumber, a.destAccountName, a.destKycLevel, a.destBvn, a.destAmount, a.destInstitutioncode, a.destResponseCode, a.narration, a.transactiondate, a.username, "
                                + "b.name as srcInstitutionName, c.name as destInstitutionName "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.srcSessionid "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                                + "ON a.srcInstitutioncode = b.code "
                                + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                                + "ON a.destInstitutioncode = c.code WHERE dispute.resolved = 1 AND (a.srcInstitutioncode = " + code + " OR a.destInstitutioncode = " + code + ")"
                                + " ORDER BY a.id DESC ";
                            
                            String SQL2 = "SELECT SUM(a.srcAmount) as totalValue "
                                + "FROM tbl_disputes dispute "
                                + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
                                + "ON dispute.transactionSessionid = a.srcSessionid "
                                + "WHERE dispute.resolved = 1 AND (a.srcInstitutioncode = " + code + " OR a.destInstitutioncode = " + code + ")";
                            totalValue = jdbcTemplate.queryForObject(SQL2, Double.class);
                        }
                        break;
                }
                transactions = jdbcTemplate.query(SQL, new DisputeTransactionMapper());
            }
            totalValue = totalValue != null ? totalValue : 0;
            SQL = "SELECT MIN(transactiondate) from ajiswitch_db.tbl_creditfundtransfers";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(transactiondate) from ajiswitch_db.tbl_creditfundtransfers";
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
    public ResponseEntity LogDispute(String sessiontoken, String sessionId, String amount, String wallet, String destinationInstitution, String type, String username){
        try {
            
            boolean sessionIdExist = CheckSessionId(sessionId);
            if (sessionIdExist) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Cannot log dispute with same session ID twice");
                return responseManager.ResponseOk(networkResponse);
            }
            
            List<FullTransactionModel> getTransaction = GetTransaction(sessionId, amount, destinationInstitution);
            if (getTransaction.size() > 0) {
                String SQL;
                int userrole = GetUserRole(username, sessiontoken);
                SQL = "INSERT into tbl_disputes(transactionSessionid, loggedBy, ownerInstitution, type, status, date_created) VALUES(?, ?, ?, '-1', NULL, now())";
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
            response.setSrcSessionid(rs.getString("srcSessionid"));
            response.setSrcAccountNumber(rs.getString("srcAccountNumber"));
            response.setSrcAccountName(rs.getString("srcAccountName"));
            response.setSrcKycLevel(rs.getString("srcKycLevel"));
            response.setSrcBvn(rs.getString("srcBvn"));
            response.setSrcAmount(rs.getString("srcAmount"));
            response.setSrcInstitutioncode(rs.getString("srcInstitutioncode"));
            response.setDestSessionId(rs.getString("destSessionId"));
            response.setSrcResponsecode(rs.getString("srcResponsecode"));
            response.setDestAccountNumber(rs.getString("destAccountNumber"));
            response.setDestAccountName(rs.getString("destAccountName"));
            response.setDestKycLevel(rs.getString("destKycLevel"));
            response.setDestBvn(rs.getString("destBvn"));
            response.setDestAmount(rs.getString("destAmount"));
            response.setDestInstitutioncode(rs.getString("destInstitutioncode"));
            response.setDestResponseCode(rs.getString("destResponseCode"));
            response.setNarration(rs.getString("narration"));
            response.setTransactiondate(rs.getString("transactiondate"));
            response.setUsername(rs.getString("username"));
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
            response.setCode(rs.getString("code"));
            response.setName(rs.getString("name"));
            response.setShortName(rs.getString("shortName"));
            response.setColor(rs.getString("color"));
            response.setVolume(ColumnExistinRS(rs, "volume") ? rs.getString("volume") : "");
            response.setValue(ColumnExistinRS(rs, "value") ? rs.getString("value") : "0");
            return response;
        }
    }
    
    class FullTransactionMapper implements RowMapper<FullTransactionModel> {
        @Override
        public FullTransactionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            FullTransactionModel response = new FullTransactionModel();            
            response.setId(rs.getInt("id"));
            response.setSrcSessionid(rs.getString("srcSessionid"));
            response.setSrcAccountNumber(rs.getString("srcAccountNumber"));
            response.setSrcAccountName(rs.getString("srcAccountName"));
            response.setSrcKycLevel(rs.getString("srcKycLevel"));
            response.setSrcBvn(rs.getString("srcBvn"));
            response.setSrcAmount(rs.getString("srcAmount"));
            response.setSrcInstitutioncode(rs.getString("srcInstitutioncode"));
            response.setDestSessionId(rs.getString("destSessionId"));
            response.setSrcResponsecode(rs.getString("srcResponsecode"));
            response.setDestAccountNumber(rs.getString("destAccountNumber"));
            response.setDestAccountName(rs.getString("destAccountName"));
            response.setDestKycLevel(rs.getString("destKycLevel"));
            response.setDestBvn(rs.getString("destBvn"));
            response.setDestAmount(rs.getString("destAmount"));
            response.setDestInstitutioncode(rs.getString("destInstitutioncode"));
            response.setDestResponseCode(rs.getString("destResponseCode"));
            response.setNarration(rs.getString("narration"));
            response.setTransactiondate(rs.getString("transactiondate"));
            response.setUsername(rs.getString("username"));
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
            response.setSrcSessionid(rs.getString("srcSessionid"));
            response.setSrcAccountNumber(rs.getString("srcAccountNumber"));
            response.setSrcAccountName(rs.getString("srcAccountName"));
            response.setSrcKycLevel(rs.getString("srcKycLevel"));
            response.setSrcBvn(rs.getString("srcBvn"));
            response.setSrcAmount(rs.getString("srcAmount"));
            response.setSrcInstitutioncode(rs.getString("srcInstitutioncode"));
            response.setDestSessionId(rs.getString("destSessionId"));
            response.setSrcResponsecode(rs.getString("srcResponsecode"));
            response.setDestAccountNumber(rs.getString("destAccountNumber"));
            response.setDestAccountName(rs.getString("destAccountName"));
            response.setDestKycLevel(rs.getString("destKycLevel"));
            response.setDestBvn(rs.getString("destBvn"));
            response.setDestAmount(rs.getString("destAmount"));
            response.setDestInstitutioncode(rs.getString("destInstitutioncode"));
            response.setDestResponseCode(rs.getString("destResponseCode"));
            response.setNarration(rs.getString("narration"));
            response.setTransactiondate(rs.getString("transactiondate"));
            response.setUsername(rs.getString("username"));
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
