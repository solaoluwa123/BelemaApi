/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.CardsTransactionsInterface;
import com.transgate.api.models.CardsDisputeModel;
import com.transgate.api.models.CardsTransactionModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.util.DateUtil;
import com.transgate.api.util.Formatter;
import com.transgate.api.util.Mailers;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.RestCall;
import com.transgate.api.util.TransactionsCodeInterpreter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
public class CardsTransactionsService implements CardsTransactionsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    TransactionsCodeInterpreter transactionsCodeInterpreter = new TransactionsCodeInterpreter();
    DateUtil dateUtil = new DateUtil();
    RestCall restCall = new RestCall();
    Formatter formatter = new Formatter();
    Mailers mailers = new Mailers();
    
    String _temp_date = "2023-01-01 00:00:00";
    
    private int GetUserRole(String session_token) {
        try {
            int role;

            String SQL = "SELECT role FROM tbl_user_details WHERE deleted = 0 AND session_token = ?";
            role = jdbcTemplate.queryForObject(SQL, new Object[]{session_token}, int.class);
            return role;
        } catch (DataAccessException ex) {
//            System.out.println("error>>>>" + ex.getMessage() + "------------");
            return -100;
        }
    }
    
    @Override
    public ResponseEntity Get(String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<CardsTransactionModel> transactions;
            String table = isCurrent ? "sparkpay.transactions a " : "sparkpay.transaction_hist_s a ";
            SQL = "SELECT a.*, b.station_name FROM "+table
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.ncs_date_time >= ? AND a.ncs_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM "+table
                    + "WHERE a.ncs_date_time >= ? AND a.ncs_date_time <= ?";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
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
            totalValue = totalValue != null ? totalValue / 100 : 0;
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
    public ResponseEntity Get(int id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsTransactionModel> transactions;
            SQL = "SELECT * FROM sparkpay.transactions WHERE id = ? ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new Object[]{id}, new CardsTransactionsMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Transaction by id: " + id);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    public List<CardsTransactionModel> GetTransaction(String terminalid, String rrn, String stan, boolean isCurrent) {
        String SQL;
        List<CardsTransactionModel> transactions;
        if (isCurrent)
            SQL = "SELECT a.*, b.station_name FROM sparkpay.transactions a LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id WHERE a.terminal_id = ? AND a.retrieval_ref_number = ? AND a.system_trace_number = ? ORDER BY a.id DESC";
        else
            SQL = "SELECT a.*, b.station_name FROM sparkpay.transaction_hist_s a LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id WHERE a.terminal_id = ? AND a.retrieval_ref_number = ? AND a.system_trace_number = ? ORDER BY a.id DESC";
        transactions = jdbcTemplate.query(SQL, new Object[]{terminalid, rrn, stan}, new CardsTransactionsMapperDefault());
        return transactions;
    }
    
    @Override
    public ResponseEntity GetByTerminalOwner(String owner, String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT terminal_id FROM sparkpay.terminals WHERE owner_id = ?";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{owner});
            StringBuilder inString = new StringBuilder("(");
            for (final Map<String, Object> row : rows) {
                inString.append("'").append(row.get("terminal_id")).append("'");
                inString.append(",");
            }
            inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            String table = isCurrent ? "sparkpay.transactions a " : "sparkpay.transaction_hist_s a ";
            SQL = "SELECT a.*, b.station_name FROM "+table+" "
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.terminal_id IN "+inString.toString()+" "
                    + "AND a.ncs_date_time >= ? AND a.ncs_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM "+table+" WHERE a.terminal_id IN "+inString.toString()+" "
                    + "AND ncs_date_time >= ? AND ncs_date_time <= ? ";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Terminal Owner: " + owner);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByTerminalOwner(String owner, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT terminal_id FROM sparkpay.terminals WHERE owner_id = ?";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{owner});
            StringBuilder inString = new StringBuilder("(");
            for (final Map<String, Object> row : rows) {
                inString.append("'").append(row.get("terminal_id")).append("'");
                inString.append(",");
            }
            inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            SQL = "SELECT * FROM sparkpay.transactions WHERE terminal_id IN "+inString.toString()+" ORDER BY id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM sparkpay.transactions a WHERE a.terminal_id IN "+inString.toString();
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Terminal Owner: " + owner);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByPTSP(String ptsp, String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT merchant_id FROM sparkpayweb_db.tbl_map_merchants_ptsps WHERE ptsp_id = ?";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{ptsp});
            StringBuilder inString = new StringBuilder("(");
            for (final Map<String, Object> row : rows) {
                inString.append("'").append(row.get("merchant_id")).append("'");
                inString.append(",");
            }
            inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            String table = isCurrent ? "sparkpay.transactions a " : "sparkpay.transaction_hist_s a ";
            SQL = "SELECT a.*, b.station_name FROM " + table
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.merchant_id IN "+inString.toString()+" "
                    + "AND a.ncs_date_time >= ? AND a.ncs_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM "+table+" WHERE a.merchant_id IN "+inString.toString()+" "
                    + "AND ncs_date_time >= ? AND ncs_date_time <= ? ";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by PTSP: " + ptsp);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByPTSP(String ptsp, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT merchant_id FROM sparkpayweb_db.tbl_map_merchants_ptsps WHERE ptsp_id = ?";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{ptsp});
            StringBuilder inString = new StringBuilder("(");
            for (final Map<String, Object> row : rows) {
                inString.append("'").append(row.get("merchant_id")).append("'");
                inString.append(",");
            }
            inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            SQL = "SELECT * FROM sparkpay.transactions WHERE merchant_id IN "+inString.toString()+" ORDER BY id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM sparkpay.transactions a WHERE a.merchant_id IN "+inString.toString();
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by PTSP: " + ptsp);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByTerminal(String terminalid, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            SQL = "SELECT * FROM sparkpay.transactions WHERE terminal_id = ? ORDER BY id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{terminalid, limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM sparkpay.transactions a WHERE a.terminal_id = ?";
            Double totalValue = jdbcTemplate.queryForObject(SQL, new Object[]{terminalid}, Double.class);
            totalValue = totalValue != null ? totalValue / 100 : 0;
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions WHERE terminal_id = ?";
            String minDate = jdbcTemplate.queryForObject(SQL, new Object[]{terminalid}, String.class);
            SQL = "SELECT MAX(ncs_date_time) from sparkpay.transactions WHERE terminal_id = ?";
            String maxDate = jdbcTemplate.queryForObject(SQL, new Object[]{terminalid}, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Terminal: " + terminalid);
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByMerchant(String merchantid, String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsTransactionModel> transactions;
            List<String> merchantIds = new ArrayList<>(Arrays.asList(merchantid.split(",")));
            StringBuilder inString = new StringBuilder("(");
            for (int i = 0; i < merchantIds.size(); i++) {
                inString.append("'").append(merchantIds.get(i)).append("'");
                inString.append(",");
            }
            inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");
            int offset = page > 1 ? (page - 1) * limit : 0;
            String table = isCurrent ? "sparkpay.transactions a " : "sparkpay.transaction_hist_s a ";
            SQL = "SELECT a.*, b.station_name FROM "+table+" LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE merchant_id IN "+inString.toString()+" "
                    + "AND a.ncs_date_time >= ? AND a.ncs_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM "+table+" WHERE a.merchant_id IN "+inString.toString()+" "
                    + "AND ncs_date_time >= ? AND ncs_date_time <= ? ";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Merchant: " + merchantid);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByMerchant(String merchantid, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsTransactionModel> transactions;
            List<String> merchantIds = new ArrayList<>(Arrays.asList(merchantid.split(",")));
            StringBuilder inString = new StringBuilder("(");
            for (int i = 0; i < merchantIds.size(); i++) {
                inString.append("'").append(merchantIds.get(i)).append("'");
                inString.append(",");
            }
            inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");
            int offset = page > 1 ? (page - 1) * limit : 0;
            SQL = "SELECT * FROM sparkpay.transactions WHERE merchant_id IN "+inString.toString()+" ORDER BY id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM sparkpay.transactions a WHERE a.merchant_id IN "+inString.toString();
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Merchant: " + merchantid);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByFI(String institution, String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            String table = isCurrent ? "sparkpay.transactions a " : "sparkpay.transaction_hist_s a ";
            SQL = "SELECT a.*, b.station_name FROM "+table
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE (a.ncs_date_time >= ? AND a.ncs_date_time <= ?) AND (a.acquirer_institution_id = ? OR a.destination_acquiring_institution_id = ?) "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institution, institution, limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM "+table
                    + "WHERE (ncs_date_time >= ? AND ncs_date_time <= ?) AND (a.acquirer_institution_id = ? OR destination_acquiring_institution_id = ?)";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institution, institution});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Institution: " + institution);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByFI(String institution, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            SQL = "SELECT * FROM sparkpay.transactions WHERE acquirer_institution_id = ? OR destination_acquiring_institution_id = ? ORDER BY id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{institution, institution, limit, offset}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                + "FROM sparkpay.transactions a WHERE a.acquirer_institution_id = ? OR destination_acquiring_institution_id = ?";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{institution, institution});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Institution: " + institution);
            networkResponse.setData((ArrayList) transactions);
            
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
            String destination_acquiring_institution_id,
            String pan,
            String rrn,
            String terminal_id,
            String merchant_id,
            String location_name_address,
            String approval_code, 
            int page, 
            int limit,
            boolean isCurrent
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
                    || !destination_acquiring_institution_id.equals("")
                    || !pan.equals("")
                    || !rrn.equals("")
                    || !terminal_id.equals("")
                    || !merchant_id.equals("")
                    || !location_name_address.equals("")
                    || !approval_code.equals("")
                    || (!min_amount.equals("") && Double.parseDouble(min_amount) > 0)
                    || (!max_amount.equals("") && Double.parseDouble(max_amount) > 0)
                    ? "WHERE" : "";
            
            if (!message_type.equals("")) {
                whereQuery+=" a.message_type = '" + message_type + "'";
            }
            if (!bin.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.bin = " + bin;
            }
            if (!processing_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.processing_code = " + processing_code;
            }
            if (!system_trace_number.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.system_trace_number = '" + system_trace_number+"'";
            }
            if (!response_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                if (response_code.equals("111"))
                    whereQuery+=" a.response_code != 00";
                else                    
                    whereQuery+=" a.response_code = " + response_code+"";
            }
            if (!retrieval_ref_number.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.retrieval_ref_number = '" + retrieval_ref_number+"'";
            }
            if (!acquirer_institution_id.equals("")) {
                if (acquirer_institution_id.equals(destination_acquiring_institution_id)) {
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" (a.acquirer_institution_id = '" + acquirer_institution_id+"' OR a.destination_acquiring_institution_id = '" + destination_acquiring_institution_id+"')";
                } else {
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" a.destination_acquiring_institution_id = '" + acquirer_institution_id+"'";
                }
            } else {
                if (!destination_acquiring_institution_id.equals("")) {
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" a.destination_acquiring_institution_id = '" + destination_acquiring_institution_id+"'";
                }
            }
            if (!pan.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.pan = '" + pan+"'";
            }
            if (!rrn.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.retrieval_ref_number = '" + rrn+"'";
            }
            if (!terminal_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.terminal_id IN (" + terminal_id+")";
            }
            if (!merchant_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.merchant_id IN (" + merchant_id+")";
            }
            if (!location_name_address.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.location_name_address = '" + location_name_address+"'";
            }
            if (!approval_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.approval_code = '" + approval_code+"'";
            }
            if ((!min_amount.equals("") && Double.parseDouble(min_amount) > 0)) {
//                String minAmount = min_amount + "00";
                Double minAmount = Double.parseDouble(min_amount) * 100;
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.amount LIKE '%" + minAmount.toString().replace(".0", "")+"'";
            }
            if ((!max_amount.equals("") && Double.parseDouble(max_amount) > 0)) {
//                String maxAmount = max_amount + "00";
                Double maxAmount = Double.parseDouble(max_amount) * 100;
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.amount LIKE '%" + maxAmount.toString().replace(".0", "")+"'";
            }
            if (!start_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.ncs_date_time >= '" + start_date + "'";
            }
            if (!end_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.ncs_date_time < '" + end_date + "'";
            }
            String SQL;
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            if (isCurrent) {
                SQL = "SELECT a.*, b.station_name FROM sparkpay.transactions a "
                        + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    +whereQuery
                    + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpay.transactions a " + whereQuery;
            } else {
                SQL = "SELECT a.*, b.station_name FROM sparkpay.transaction_hist_s a "
                        + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    +whereQuery
                    + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpay.transaction_hist_s a " + whereQuery;
            }
            LocalTime currentTime = LocalTime.now();
            int hour = currentTime.getHour();
            if (isCurrent && transactions.size() < 1 && hour >= 12) {
                SQL = "SELECT a.*, b.station_name FROM sparkpay.transaction_hist_s a "
                        + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    +whereQuery
                    + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpay.transaction_hist_s a " + whereQuery;
            }
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
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
    
    private boolean CheckDisputeExist(String terminalid, String rrn, String stan) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_disputes WHERE terminal_id = ? AND retrieval_ref_number = ? AND system_trace_number = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{terminalid, rrn, stan}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    @Override
    public ResponseEntity LogDisputesBulk(String sessiontoken, String records, String username){
        try {
            
            JSONArray jsonRecords = new JSONArray(records);
            int found = 0;
            int recorded = 0;
            int userrole = GetUserRole(sessiontoken);
            
            for (int i = 0; i < jsonRecords.length(); i++) {
                String terminalid = jsonRecords.getJSONObject(i).getString("terminalid");
                String rrn = jsonRecords.getJSONObject(i).getString("rrn");
                String stan = jsonRecords.getJSONObject(i).getString("stan");
                boolean sessionIdExist = CheckDisputeExist(terminalid, rrn, stan);
                if (!sessionIdExist) {
                    List<CardsTransactionModel> getTransaction = GetTransaction(terminalid, rrn, stan, false);
                    if (getTransaction.size() > 0) {
                        String tnxDate = getTransaction.get(0).getNcs_date_time();
                        int daysAgo = dateUtil.daysAgo(tnxDate);
                        if (getTransaction.get(0).getResponse_code().equals("00") && (daysAgo <= 120 || userrole == 8)) {
                            found++;
                            String SQL;
                            int additionalDays = dateUtil.getDisputeTimeLineDate();
                            String unique_log_code = terminalid + stan + rrn;
                            String nuban = "";
    //                        String nuban = getTransaction.get(0).getCardholder_acct_number().length() < 18 || getTransaction.get(0).getCardholder_acct_number() == null 
    //                                ? "" : 
    //                                restCall.getNuban(formatter.FormatCardHolderAcctNum(getTransaction.get(0).getCardholder_acct_number()));
                            String disputeType = !getTransaction.get(0).getResponse_code().equals("00") ? "habari" : "institution"; 
                            SQL = "INSERT into sparkpayweb_db.tbl_disputes(id, unique_log_code, terminal_id, merchant_id, system_trace_number, retrieval_ref_number, logged_by, owner_institution, type, status, date_created, timeline_date, cardholder_acct_nuban, message_type, pan, amount, destination_acquiring_institution_id, acquirer_institution_id, bin, ncs_date_time, response_code, cardholder_acct_number) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', now(), ADDDATE(now(), ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                            int retval = jdbcTemplate.update(SQL, new Object[]{getTransaction.get(0).getId(), unique_log_code, terminalid, getTransaction.get(0).getMerchant_id(), stan, rrn, username, getTransaction.get(0).getAcquirer_institution_id(), disputeType, additionalDays, nuban, getTransaction.get(0).getMessage_type(), getTransaction.get(0).getPan(), getTransaction.get(0).getRawAmount(), getTransaction.get(0).getDestination_acquiring_institution_id(), getTransaction.get(0).getAcquirer_institution_id(), getTransaction.get(0).getBin(), getTransaction.get(0).getNcs_date_time(), getTransaction.get(0).getResponse_code(), getTransaction.get(0).getCardholder_acct_number()});
    //                        SQL = "INSERT into sparkpayweb_db.tbl_disputes(id, unique_log_code, terminal_id, merchant_id, system_trace_number, retrieval_ref_number, logged_by, owner_institution, type, status, date_created, timeline_date, cardholder_acct_nuban) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', now(), ADDDATE(now(), ?), ?)";
    //                        int retval = jdbcTemplate.update(SQL, new Object[]{getTransaction.get(0).getId(), unique_log_code, terminalid, getTransaction.get(0).getMerchant_id(), stan, rrn, username, getTransaction.get(0).getAcquirer_institution_id(), disputeType, additionalDays, nuban});
                            if (userrole == 8) {
                                SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = ?, status = '0', resolved = '0', date_modified = now() WHERE terminal_id = ? AND retrieval_ref_number = ? AND system_trace_number = ?";
                                jdbcTemplate.update(SQL, new Object[]{username, terminalid, rrn, stan});
                            }
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
            Logger.getLogger(CardsTransactionsService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    @Override
    public ResponseEntity UpdateCardsDisputesNUBAN(){
        try {
            String SQL = "SELECT a.id, a.cardholder_acct_number, a.date_created FROM sparkpayweb_db.tbl_disputes a "
                    + "WHERE a.date_created > ? AND (a.cardholder_acct_nuban IS NULL || a.cardholder_acct_nuban = '') "
                    + "ORDER BY date_created ASC "
                    + "LIMIT 1";
            
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{_temp_date});
            if (agg.size() > 0) {
                Map<String, Object> row = agg.get(0);
                String cardholder_acct_number = (String) row.get("cardholder_acct_number");
                int _id = (int) row.get("id");
                _temp_date = (String) row.get("date_created").toString();
                String nuban = cardholder_acct_number != null && cardholder_acct_number.length() > 17 ? restCall.getNuban(formatter.FormatCardHolderAcctNum(cardholder_acct_number)) : cardholder_acct_number;
//                System.out.println("Last Dispute Updated Date: " + _temp_date + " Old Acct: " + cardholder_acct_number + " NUBAN: " + nuban);
                SQL = "UPDATE sparkpayweb_db.tbl_disputes SET cardholder_acct_nuban = ? WHERE id = ?";
                int update = jdbcTemplate.update(SQL, new Object[]{nuban, _id});
                if (update > 0) {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(200);
                    networkResponse.setStatus("success");
                    networkResponse.setMessage("Row: "+_id + " updated! Select Number: " + cardholder_acct_number + ", NUBAN: " + nuban);
                    return responseManager.ResponseOk(networkResponse);
                } else {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(200);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Error updating row: " + _id);
                    return responseManager.ResponseOk(networkResponse);
                }
            } else {
                return responseManager.ResponseDeleted();
            }
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        } catch (JSONException ex) {
            Logger.getLogger(CardsTransactionsService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    
    @Override
    public ResponseEntity UpdateDisputesData(){
        try {
            String SQL = "SELECT a.id, "
                    + "b.message_type, b.pan, b.amount, b.destination_acquiring_institution_id, b.acquirer_institution_id, b.bin, b.ncs_date_time, b.response_code, b.cardholder_acct_number "
                    + "FROM sparkpayweb_db.tbl_disputes a "
                    + "LEFT JOIN sparkpay.transaction_hist_s b "
                    + "ON a.id = b.id "
                    + "WHERE a.ncs_date_time IS NULL LIMIT 100";
            List<Map<String, Object>> disputes = jdbcTemplate.queryForList(SQL);
            if (disputes.size() > 0) {
                for (int i = 0; i < disputes.size(); i++) {
                    String updateSQL = "UPDATE sparkpayweb_db.tbl_disputes "
                        + "SET message_type = ?, pan = ?, amount = ?, destination_acquiring_institution_id = ?, acquirer_institution_id = ?, bin = ?, ncs_date_time = ?, response_code = ?, cardholder_acct_number = ? "
                        + "WHERE id = ?";
                
                    jdbcTemplate.update(updateSQL, new Object[]{
                        disputes.get(i).get("message_type"),
                        disputes.get(i).get("pan"),
                        disputes.get(i).get("amount"),
                        disputes.get(i).get("destination_acquiring_institution_id"),
                        disputes.get(i).get("acquirer_institution_id"),
                        disputes.get(i).get("bin"),
                        disputes.get(i).get("ncs_date_time"),
                        disputes.get(i).get("response_code"),
                        disputes.get(i).get("cardholder_acct_number"),
                        disputes.get(i).get("id")
                    });
                }
                return responseManager.ResponseAccepted();
            }
            else {
                return responseManager.ResponseDeleted();
            }
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    
    @Override
    public ResponseEntity LogDispute(String sessiontoken, String terminalid, String rrn, String stan, String proof_of_debit_uri, String username, boolean isExternal){
        try {
            
            boolean sessionIdExist = CheckDisputeExist(terminalid, rrn, stan);
            String unique_log_code = terminalid + stan + rrn;
            if (sessionIdExist) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Cannot log dispute with same details twice\nID: "+unique_log_code);
                return responseManager.ResponseOk(networkResponse);
            }
            List<CardsTransactionModel> getTransaction = GetTransaction(terminalid, rrn, stan, false);
            if (getTransaction.size() > 0) {
                int userrole = GetUserRole(sessiontoken);
                String SQL;
                if (!getTransaction.get(0).getResponse_code().equals("00")) {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(200);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Only completely processed or approved transactions can be logged for dispute");
                    return responseManager.ResponseOk(networkResponse);
                }
                int additionalDays = dateUtil.getDisputeTimeLineDate();
                String nuban = "";
                String tnxDate = getTransaction.get(0).getNcs_date_time();
                int daysAgo = dateUtil.daysAgo(tnxDate);
                if (daysAgo > 120 && userrole != 8) {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(200);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Transaction occured more than 120 days ago and cannot be logged");
                    return responseManager.ResponseOk(networkResponse);
                } 
//                String nuban = getTransaction.get(0).getCardholder_acct_number().length() < 18 || getTransaction.get(0).getCardholder_acct_number() == null 
//                        ? "" : 
//                        restCall.getNuban(formatter.FormatCardHolderAcctNum(getTransaction.get(0).getCardholder_acct_number()));
                String disputeType = !getTransaction.get(0).getResponse_code().equals("00") ? "habari" : "institution"; 
                SQL = "INSERT into sparkpayweb_db.tbl_disputes(id, unique_log_code, terminal_id, merchant_id, system_trace_number, retrieval_ref_number, logged_by, owner_institution, type, status, date_created, timeline_date, proof_of_debit_uri, cardholder_acct_nuban, message_type, pan, amount, destination_acquiring_institution_id, acquirer_institution_id, bin, ncs_date_time, response_code, cardholder_acct_number) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', now(), ADDDATE(now(), ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                int retval = jdbcTemplate.update(SQL, new Object[]{getTransaction.get(0).getId(), unique_log_code, terminalid, getTransaction.get(0).getMerchant_id(), stan, rrn, username, getTransaction.get(0).getAcquirer_institution_id(), disputeType, additionalDays, proof_of_debit_uri, nuban, getTransaction.get(0).getMessage_type(), getTransaction.get(0).getPan(), getTransaction.get(0).getRawAmount(), getTransaction.get(0).getDestination_acquiring_institution_id(), getTransaction.get(0).getAcquirer_institution_id(), getTransaction.get(0).getBin(), getTransaction.get(0).getNcs_date_time(), getTransaction.get(0).getResponse_code(), getTransaction.get(0).getCardholder_acct_number()});
                if (userrole == 8 || isExternal) {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = ?, status = '0', resolved = '0', date_modified = now() WHERE terminal_id = ? AND retrieval_ref_number = ? AND system_trace_number = ?";
                    jdbcTemplate.update(SQL, new Object[]{username, terminalid, rrn, stan});
                }
                if (retval > 0) {
                    SQL = "SELECT ptsp_id FROM sparkpayweb_db.tbl_map_merchants_ptsps WHERE merchant_id = ?";
                    String ptspid = jdbcTemplate.queryForObject(SQL, new Object[]{getTransaction.get(0).getMerchant_id()}, String.class);
                    SQL = "SELECT user_email FROM tbl_map_card_users_institution WHERE institution_id = ? LIMIT 3";
                    
                    List<Map<String, Object>> ptspUsers = jdbcTemplate.queryForList(SQL, new Object[]{ptspid});
                    if (ptspUsers.size() > 0) {
                        try {
                            ptspUsers.forEach(row -> {
                                String message = "<html><body>Dear Team, <br/><br/>Please be informed that a new dispute has been logged against your institution. Please login to sparkpay and find the dispute under the unique log code "+unique_log_code
                                        + "<br/><br/>Sparkpay,"
                                        + "<br/>Cheers</body><html>";
                                mailers.SendMailWithHabariOkHttpClient(
                                        "New Dispute Log",
                                        "no-reply@habaripay.com",
                                        (String) row.get("user_email"),
                                        message
                                );
                            });
                        } catch(Exception e) {
                            System.out.println("mailer error: " + e.toString());
                        }
                    }
                    return responseManager.ResponseAccepted();
                }
                else 
                    return responseManager.ResponseInternalServerError();
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
    
    @Override
    public ResponseEntity GetDisputesByMerchant(String merchantid, String startDate, String endDate, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            Double totalValue;
            List<CardsDisputeModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<String> merchantIds = new ArrayList<>(Arrays.asList(merchantid.split(",")));
            StringBuilder inString = new StringBuilder("(");
            for (int i = 0; i < merchantIds.size(); i++) {
                inString.append("'").append(merchantIds.get(i)).append("'");
                inString.append(",");
            }
            inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");
            SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                    + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                    + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                    + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                    + "WHERE ((a.status = 1 AND a.resolved = 1) || a.resolved = 0) AND a.merchant_id IN "+inString.toString()+" AND a.response_code = '00' ORDER BY a.date_created DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsDisputesMapper());

            SQL = "SELECT "
                    + "SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpayweb_db.tbl_disputes a "
//                    + "LEFT JOIN sparkpay.transaction_hist_s b "
//                    + "ON a.id = b.id "
                    + "WHERE ((a.status = 1 AND a.resolved = 1) || a.resolved = 0) AND a.merchant_id IN "+inString.toString()+" AND a.response_code = '00'";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Disputes by merchant: " + merchantid);
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetArbitratedDisputes(String institutioncode) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            Double totalValue;
            List<CardsDisputeModel> transactions;
            switch(institutioncode) {
                case "":
                case "-1":
                    SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                            + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                            + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number, a.arbitrated_proof_uri, a.arbitration_closed_date, a.arbitration_closed_by "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE a.status < -1 ORDER BY a.date_created DESC";
                    transactions = jdbcTemplate.query(SQL, new CardsTransactionsDisputesMapper());

                    SQL = "SELECT "
                            + "SUM(a.amount) as totalValue "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE a.status < -1";
                    totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
                    break;
                default:
                    SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                            + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                            + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number, a.arbitrated_proof_uri, a.arbitration_closed_date, a.arbitration_closed_by "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE a.status < -1 AND a.destination_acquiring_institution_id = ? ORDER BY a.date_created DESC";
                    transactions = jdbcTemplate.query(SQL, new Object[]{institutioncode}, new CardsTransactionsDisputesMapper());

                    SQL = "SELECT "
                            + "SUM(a.amount) as totalValue "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE a.status < -1 AND a.destination_acquiring_institution_id = ?";
                    totalValue = jdbcTemplate.queryForObject(SQL, new Object[]{institutioncode}, Double.class);
                    break;
            }
            
            totalValue = totalValue != null ? totalValue / 100 : 0;
            String meta = "{\"totalValue\": " +totalValue+ "}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Arbitrated Disputes by Institution: " + institutioncode);
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetDisputes(String uniqueIds) {
        NetworkResponse networkResponse = new NetworkResponse();
        List<String> idArray = new ArrayList<>(Arrays.asList(uniqueIds.split(",")));
        if (idArray.size() > 10) {
            networkResponse.setCode(400);
            networkResponse.setMessage("Rquest size must be lesser or equal to 10");
            networkResponse.setStatus("bad request");
            return responseManager.ResponseOk(networkResponse);
        }
        try {    
             String placeholders = idArray.stream()
                                 .map(id -> "?")
                                 .collect(Collectors.joining(","));
            String SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                            + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                            + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                            + "FROM sparkpayweb_db.tbl_disputes a "
                            + "WHERE a.unique_log_code IN (" + placeholders + ")";
            List<CardsDisputeModel> transactions = jdbcTemplate.query(SQL, idArray.toArray(), new CardsTransactionsDisputesMapper());
                        
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Found total of: " + transactions.size() + " disputes");
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetOneDispute(String uniqueId) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                            + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                            + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                            + "FROM sparkpayweb_db.tbl_disputes a "
                            + "WHERE a.unique_log_code = ?";
            
            List<CardsDisputeModel> transactions = jdbcTemplate.query(SQL, new Object[]{uniqueId}, new CardsTransactionsDisputesMapper());
                        
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Found dispute: " + uniqueId);
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetDisputes(String institutioncode, String startDate, String endDate, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            Double totalValue;
            List<Map<String, Object>> agg;
            List<CardsDisputeModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            switch(institutioncode) {
                case "":
                case "-1":
                    SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                            + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                            + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE a.resolved = 0 || (a.status = 1 AND a.resolved = 1) ORDER BY a.date_created DESC LIMIT ? OFFSET ?";
                    transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsDisputesMapper());

                    SQL = "SELECT "
                            + "SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE a.resolved = 0 || (a.status = 1 AND a.resolved = 1)";
                    agg = jdbcTemplate.queryForList(SQL);
                    break;
                case "000000":
                    SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                            + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                            + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE (a.resolved = 0 || (a.status = 1 AND a.resolved = 1)) AND a.response_code != '00' ORDER BY a.date_created DESC LIMIT ? OFFSET ?";
                    transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsDisputesMapper());

                    SQL = "SELECT "
                            + "SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE (a.resolved = 0 || (a.status = 1 AND a.resolved = 1)) AND a.response_code != '00'";
                    agg = jdbcTemplate.queryForList(SQL);
                    break;
                default:
                    SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                            + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                            + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE ((a.status = 1 AND a.resolved = 1) || a.resolved = 0) AND a.destination_acquiring_institution_id = ? ORDER BY a.date_created DESC LIMIT ? OFFSET ?";
                    transactions = jdbcTemplate.query(SQL, new Object[]{institutioncode, limit, offset}, new CardsTransactionsDisputesMapper());

                    SQL = "SELECT "
                            + "SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                            + "FROM sparkpayweb_db.tbl_disputes a "
//                            + "LEFT JOIN sparkpay.transaction_hist_s b "
//                            + "ON a.id = b.id "
                            + "WHERE ((a.status = 1 AND a.resolved = 1) || a.resolved = 0) AND a.destination_acquiring_institution_id = ?";
                    agg = jdbcTemplate.queryForList(SQL, new Object[]{institutioncode});
                    break;
            }
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            totalValue = tValue != null ? tValue/100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Disputes by Institution: " + institutioncode);
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
            String terminal_id,
            String system_trace_number,
            String retrieval_ref_number,
            String transaction_response_code,
            String dispute_status,
            String dispute_type,
            String date_logged,
            String date_resolved,
            String timeline_date,
            String merchantsasIds,
            String pan,
            String uniquelogid,
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
            List<CardsDisputeModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            String whereQuery = !terminal_id.equals("")
                    || !system_trace_number.equals("")
                    || !retrieval_ref_number.equals("")
                    || !transaction_response_code.equals("")
                    || !dispute_status.equals("")
                    || !dispute_type.equals("")
                    || !start_date_logged.equals("")
                    || !end_date_logged.equals("")
                    || !start_date_resolved.equals("")
                    || !end_date_resolved.equals("")
                    || !start_timeline_date.equals("")
                    || !end_timeline_date.equals("")
                    || !merchantsasIds.equals("")
                    || !pan.equals("")
                    || !uniquelogid.equals("")
                    ? "WHERE" : "";
            
            if (!merchantsasIds.equals("")) {
                List<String> merchantIds = new ArrayList<>(Arrays.asList(merchantsasIds.split(",")));
                StringBuilder inString = new StringBuilder("(");
                for (int i = 0; i < merchantIds.size(); i++) {
                    inString.append("'").append(merchantIds.get(i)).append("'");
                    inString.append(",");
                }
                inString = inString.deleteCharAt(inString.length() - 1);
                if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
                if (inString.toString().equals(""))
                    inString = inString.append("(-1");
                inString = inString.append(")");
                whereQuery+=" a.merchant_id IN " + inString;
            }
            if (!terminal_id.equals("")) {
                whereQuery+=" a.terminal_id = '" + terminal_id + "'";
            }
            if (!uniquelogid.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.unique_log_code = '" + uniquelogid + "'";
            }
            if (!pan.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.pan = '" + pan + "'";
            }
            if (!system_trace_number.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.system_trace_number = '" + system_trace_number + "'";
            }
            if (!retrieval_ref_number.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.retrieval_ref_number = '" + retrieval_ref_number + "'";
            }
            if (!transaction_response_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.response_code = '" + transaction_response_code + "'";
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
                default:
                    break;
            }
            switch(dispute_type) {
                case "charge-back":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" a.logged_by != a.resolved_by";
                    break;
                case "air":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                    whereQuery+=" a.logged_by = a.resolved_by";
                    break;
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
            SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                    + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                    + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                    + "FROM sparkpayweb_db.tbl_disputes a "
//                    + "LEFT JOIN sparkpay.transaction_hist_s b "
//                    + "ON a.id = b.id " 
                    + whereQuery
                    + " ORDER BY a.date_created DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsDisputesMapper());

            SQL = "SELECT "
                    + "SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpayweb_db.tbl_disputes a "
//                    + "LEFT JOIN sparkpay.transaction_hist_s b "
//                    + "ON a.id = b.id " 
                    + whereQuery;
            agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            totalValue = tValue != null ? tValue/100 : 0;
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
    
//    @Override
//    public ResponseEntity ApproveSettlement(String sessiontoken, int id, int status, String proof_of_reject_uri, String username) {
//        try {
//            String SQL;
//            int retVal;
//            int resolved = status == 0 ? 0 : 1;
//            if (status == -2)  {
//                SQL = "UPDATE sparkpayweb_db.tbl_disputes SET arbitrated_by = ?, status = ?, date_arbitrated = now() WHERE id = ?";
//                retVal = jdbcTemplate.update(SQL, new Object[]{username, status, id});
//            } else if (status < -2) {
//                SQL = "UPDATE sparkpayweb_db.tbl_disputes SET arbitration_closed_by = ?, arbitrated_proof_uri = ?, status = ?, arbitration_closed_date = now() WHERE id = ?";
//                retVal = jdbcTemplate.update(SQL, new Object[]{username, proof_of_reject_uri, status, id});
//            }
//            else {
//                SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";
//               retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, id});
//            }
//            if (retVal > 0)
//                return responseManager.ResponseAccepted();
//            else
//                return responseManager.ResponseBadRequest();
//        } catch (DataAccessException ex) {
//            System.out.println("error>>>>" + ex.getMessage());
//            return responseManager.ResponseInternalServerError();
//        }
//    }
    
    @Override
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, int status, String proof_of_reject_uri, String selectedDisputes, String type, String username) {
        try {
            String SQL;
            int retVal = 0;
            int resolved = status == 0 ? 0 : 1;
            if (type.equals("bulk")) {
                String[] idS = selectedDisputes.split(",");
                for(String _id: idS) {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";
                    int _retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, _id});
                    retVal = retVal + _retVal;
                }
            } else {
                if (status == -2)  {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET arbitrated_by = ?, status = ?, date_arbitrated = now() WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{username, status, id});
                } else if (status < -2) {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET arbitration_closed_by = ?, arbitrated_proof_uri = ?, status = ?, arbitration_closed_date = now() WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{username, proof_of_reject_uri, status, id});
                }
                else {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, id});
                }
            }
            String action = status == 0 ? "accepted" : status == -1 ? "arbitrated" : "rejected";
            if (retVal > 0) {
                if (type.equals("bulk"))
                    return responseManager.ResponseAccepted("Total of " + retVal+ " dispute has been " + action);
                else
                    return responseManager.ResponseAccepted();
            }
            else
                return responseManager.ResponseBadRequest();
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class CardsTransactionsDisputesMapper implements RowMapper<CardsDisputeModel> {
        @Override
        public CardsDisputeModel mapRow(ResultSet rs, int arg1) throws SQLException {
            CardsDisputeModel tnx = new CardsDisputeModel();
            tnx.setId(rs.getInt("id"));
            tnx.setLogged_by(rs.getString("logged_by"));
            tnx.setResolved_by(rs.getString("resolved_by"));
            tnx.setTransaction_id(rs.getInt("id"));
            tnx.setStatus(rs.getInt("status"));
            tnx.setResolved(rs.getInt("resolved"));
            tnx.setDate_modified(rs.getString("date_modified"));
            tnx.setDate_created(rs.getString("date_created"));
            tnx.setTimeline_date(rs.getString("timeline_date"));
            tnx.setMessage_type(rs.getString("message_type"));
            tnx.setPan(rs.getString("pan"));
            Double amount = rs.getString("amount") != null && rs.getString("amount") != "" ? Double.parseDouble(rs.getString("amount")) / 100 : 0.00;
            tnx.setAmount(amount.toString());
            tnx.setSystem_trace_number(rs.getString("system_trace_number"));
            tnx.setRetrieval_ref_number(rs.getString("retrieval_ref_number"));
            tnx.setAcquirer_institution_id(rs.getString("acquirer_institution_id"));
            tnx.setDestination_acquiring_institution_id(rs.getString("destination_acquiring_institution_id"));
            tnx.setTerminal_id(rs.getString("terminal_id"));
            tnx.setMerchant_id(rs.getString("merchant_id"));
            tnx.setBin(rs.getString("bin"));
            tnx.setNcs_date_time(rs.getString("ncs_date_time"));
            tnx.setProof_of_debit_uri(rs.getString("proof_of_debit_uri"));
            tnx.setProof_of_reject_uri(rs.getString("proof_of_reject_uri"));
            tnx.setArbitrated_by(rs.getString("arbitrated_by"));
            tnx.setDate_arbitrated(rs.getString("date_arbitrated"));
            tnx.setCardholder_acct_nuban(rs.getString("cardholder_acct_nuban"));
            tnx.setResponse_code(rs.getString("response_code"));
            tnx.setCardholder_acct_number(rs.getString("cardholder_acct_number") != null && rs.getString("cardholder_acct_number").length() > 17 ? formatter.FormatCardHolderAcctNum(rs.getString("cardholder_acct_number")) : "");
            tnx.setStatus_code_message(rs.getString("response_code") != null && rs.getString("response_code").toLowerCase() != "null" && rs.getString("response_code") != "" ? transactionsCodeInterpreter.GetResponse(rs.getString("response_code")) : "");
            if (hasColumn(rs, "arbitration_closed_date"))
                tnx.setArbitration_closed_by(rs.getString("arbitration_closed_date"));
            if (hasColumn(rs, "arbitrated_proof_uri"))
                tnx.setArbitrated_proof_uri(rs.getString("arbitrated_proof_uri"));
            if (hasColumn(rs, "arbitration_closed_by"))
                tnx.setArbitration_closed_by(rs.getString("arbitration_closed_by"));
            if (rs.getInt("status") == -1) tnx.setResult("PENDING");
            else if (rs.getInt("status") == 1) tnx.setResult("REJECTED");
            else if (rs.getInt("status") == 0 && rs.getInt("resolved") == 0) tnx.setResult("ACCEPTED");
            else if (rs.getInt("status") == 0 && rs.getInt("resolved") == 1) tnx.setResult("RESOLVED");
            return tnx;
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
            tnx.setRawAmount(rs.getString("amount"));
            Double amount = rs.getString("amount") != null && rs.getString("amount") != "" ? Double.parseDouble(rs.getString("amount")) / 100 : 0.00;
            tnx.setAmount(amount.toString());
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
            tnx.setCardholder_acct_number(rs.getString("cardholder_acct_number") != null && rs.getString("cardholder_acct_number").length() > 17 ? formatter.FormatCardHolderAcctNum(rs.getString("cardholder_acct_number")) : rs.getString("cardholder_acct_number"));
            tnx.setStatus_code_message(transactionsCodeInterpreter.GetResponse(rs.getString("response_code")));
            tnx.setDestination_acquiring_institution_name(rs.getString("station_name") != null ? rs.getString("station_name") : "");
            tnx.setIsTxnReversed(rs.getString("isTxnReversed"));
//            tnx.setDestination_acquiring_institution_name(hasColumn(rs, "station_name") ? rs.getString("station_name") : "");
            return tnx;
        }
    }
    
    class CardsTransactionsMapperDefault implements RowMapper<CardsTransactionModel> {

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
            tnx.setRawAmount(rs.getString("amount"));
            Double amount = rs.getString("amount") != null && rs.getString("amount") != "" ? Double.parseDouble(rs.getString("amount")) / 100 : 0.00;
            tnx.setAmount(amount.toString());
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
            tnx.setCardholder_acct_number(rs.getString("cardholder_acct_number"));
            tnx.setStatus_code_message(transactionsCodeInterpreter.GetResponse(rs.getString("response_code")));
            tnx.setDestination_acquiring_institution_name(rs.getString("station_name") != null ? rs.getString("station_name") : "");
            tnx.setIsTxnReversed(rs.getString("isTxnReversed"));
//            tnx.setDestination_acquiring_institution_name(hasColumn(rs, "station_name") ? rs.getString("station_name") : "");
            return tnx;
        }
    }
    
    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
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
