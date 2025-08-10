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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author Makintola
 */
@Service
public class CardsTransactionsService implements CardsTransactionsInterface {

    @Autowired
    DataSource dataSource;

    @Autowired
    @Qualifier("jdbcTemplate")
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();

    TransactionsCodeInterpreter transactionsCodeInterpreter = new TransactionsCodeInterpreter();
    DateUtil dateUtil = new DateUtil();
    RestCall restCall = new RestCall();
    Formatter formatter = new Formatter();
    Mailers mailers = new Mailers();

    String _temp_date = "2023-01-01 00:00:00";
    private final Logger logger = Logger.getLogger(CardsTransactionsService.class.getName());

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

    private int GetUserRoleExternal(String session_token) {
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
            SQL = "SELECT a.*, b.station_name FROM " + table
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.ncs_date_time >= ? AND a.ncs_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new CardsTransactionsMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM " + table
                    + "WHERE a.ncs_date_time >= ? AND a.ncs_date_time <= ?";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            String meta = "{\"totalValue\": " + totalValue + ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";

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
        logger.info(String.format("GetTransaction() called: terminalid=%s, rrn=%s, stan=%s, isCurrent=%s",
                terminalid, rrn, stan, isCurrent));

        String SQL;
        if (isCurrent) {
            SQL = "SELECT a.*, b.station_name FROM sparkpay.transactions a "
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.terminal_id = ? AND a.retrieval_ref_number = ? AND a.system_trace_number = ? "
                    + "ORDER BY a.id DESC";
            logger.info(String.format("Using CURRENT transaction table. SQL: %s", SQL));
        } else {
            SQL = "SELECT a.*, b.station_name FROM sparkpay.transaction_hist_s a "
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.terminal_id = ? AND a.retrieval_ref_number = ? AND a.system_trace_number = ? "
                    + "ORDER BY a.id DESC";
            logger.info(String.format("Using HISTORY transaction table. SQL: %s", SQL));
        }

        logger.info(String.format("Executing query with params: terminalid=%s, rrn=%s, stan=%s", terminalid, rrn, stan));
        List<CardsTransactionModel> transactions = jdbcTemplate.query(SQL, new Object[]{terminalid, rrn, stan}, new CardsTransactionsMapperDefault());
        logger.info(String.format("GetTransaction() returned %d result(s)", transactions.size()));
        return transactions;
    }

    public List<CardsTransactionModel> getTransactionExternal(String terminalid, String rrn, String stan, String merchantid, boolean isCurrent) {
        logger.info(String.format("getTransactionExternal() called: terminalid=%s, rrn=%s, stan=%s, merchantid=%s, isCurrent=%s",
                terminalid, rrn, stan, merchantid, isCurrent));

        String SQL;
        if (isCurrent) {
            SQL = "SELECT a.*, b.station_name FROM sparkpay.transactions a "
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.terminal_id = ? AND a.retrieval_ref_number = ? AND a.system_trace_number = ? AND a.merchant_id = ? "
                    + "ORDER BY a.id DESC";
            logger.info(String.format("Using CURRENT transaction table. SQL: %s", SQL));
        } else {
            SQL = "SELECT a.*, b.station_name FROM sparkpay.transaction_hist_s a "
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.terminal_id = ? AND a.retrieval_ref_number = ? AND a.system_trace_number = ? AND a.merchant_id = ? "
                    + "ORDER BY a.id DESC";
            logger.info(String.format("Using HISTORY transaction table. SQL: %s", SQL));
        }

        logger.info(String.format("Executing query with params: terminalid=%s, rrn=%s, stan=%s, merchantid=%s", terminalid, rrn, stan, merchantid));
        List<CardsTransactionModel> transactions = jdbcTemplate.query(SQL, new Object[]{terminalid, rrn, stan, merchantid}, new CardsTransactionsMapperDefault());
        logger.info(String.format("getTransactionExternal() returned %d result(s)", transactions.size()));
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
            if (inString.toString().equals("(")) {
                inString = inString.deleteCharAt(inString.length() - 1);
            }
            if (inString.toString().equals("")) {
                inString = inString.append("(-1");
            }
            inString = inString.append(")");
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            String table = isCurrent ? "sparkpay.transactions a " : "sparkpay.transaction_hist_s a ";
            SQL = "SELECT a.*, b.station_name FROM " + table + " "
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.terminal_id IN " + inString.toString() + " "
                    + "AND a.ncs_date_time >= ? AND a.ncs_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new CardsTransactionsMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM " + table + " WHERE a.terminal_id IN " + inString.toString() + " "
                    + "AND ncs_date_time >= ? AND ncs_date_time <= ? ";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            if (inString.toString().equals("(")) {
                inString = inString.deleteCharAt(inString.length() - 1);
            }
            if (inString.toString().equals("")) {
                inString = inString.append("(-1");
            }
            inString = inString.append(")");
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            SQL = "SELECT * FROM sparkpay.transactions WHERE terminal_id IN " + inString.toString() + " ORDER BY id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpay.transactions a WHERE a.terminal_id IN " + inString.toString();
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            if (inString.toString().equals("(")) {
                inString = inString.deleteCharAt(inString.length() - 1);
            }
            if (inString.toString().equals("")) {
                inString = inString.append("(-1");
            }
            inString = inString.append(")");
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            String table = isCurrent ? "sparkpay.transactions a " : "sparkpay.transaction_hist_s a ";
            SQL = "SELECT a.*, b.station_name FROM " + table
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE a.merchant_id IN " + inString.toString() + " "
                    + "AND a.ncs_date_time >= ? AND a.ncs_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new CardsTransactionsMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM " + table + " WHERE a.merchant_id IN " + inString.toString() + " "
                    + "AND ncs_date_time >= ? AND ncs_date_time <= ? ";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            if (inString.toString().equals("(")) {
                inString = inString.deleteCharAt(inString.length() - 1);
            }
            if (inString.toString().equals("")) {
                inString = inString.append("(-1");
            }
            inString = inString.append(")");
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;
            SQL = "SELECT * FROM sparkpay.transactions WHERE merchant_id IN " + inString.toString() + " ORDER BY id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpay.transactions a WHERE a.merchant_id IN " + inString.toString();
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            String meta = "{\"totalValue\": " + totalValue + ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";

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
            if (inString.toString().equals("(")) {
                inString = inString.deleteCharAt(inString.length() - 1);
            }
            if (inString.toString().equals("")) {
                inString = inString.append("(-1");
            }
            inString = inString.append(")");
            int offset = page > 1 ? (page - 1) * limit : 0;
            String table = isCurrent ? "sparkpay.transactions a " : "sparkpay.transaction_hist_s a ";
            SQL = "SELECT a.*, b.station_name FROM " + table + " LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE merchant_id IN " + inString.toString() + " "
                    + "AND a.ncs_date_time >= ? AND a.ncs_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new CardsTransactionsMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM " + table + " WHERE a.merchant_id IN " + inString.toString() + " "
                    + "AND ncs_date_time >= ? AND ncs_date_time <= ? ";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            if (inString.toString().equals("(")) {
                inString = inString.deleteCharAt(inString.length() - 1);
            }
            if (inString.toString().equals("")) {
                inString = inString.append("(-1");
            }
            inString = inString.append(")");
            int offset = page > 1 ? (page - 1) * limit : 0;
            SQL = "SELECT * FROM sparkpay.transactions WHERE merchant_id IN " + inString.toString() + " ORDER BY id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpay.transactions a WHERE a.merchant_id IN " + inString.toString();
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            SQL = "SELECT a.*, b.station_name FROM " + table
                    + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                    + "WHERE (a.ncs_date_time >= ? AND a.ncs_date_time <= ?) AND (a.acquirer_institution_id = ? OR a.destination_acquiring_institution_id = ?) "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institution, institution, limit, offset}, new CardsTransactionsMapper());

            SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM " + table
                    + "WHERE (ncs_date_time >= ? AND ncs_date_time <= ?) AND (a.acquirer_institution_id = ? OR destination_acquiring_institution_id = ?)";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institution, institution});
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
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
            boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            // Log entry with parameters.
            logger.info("SearchTransactions invoked with parameters: "
                    + "message_type=" + message_type + ", "
                    + "bin=" + bin + ", "
                    + "processing_code=" + processing_code + ", "
                    + "system_trace_number=" + system_trace_number + ", "
                    + "response_code=" + response_code + ", "
                    + "min_amount=" + min_amount + ", "
                    + "max_amount=" + max_amount + ", "
                    + "start_date=" + start_date + ", "
                    + "end_date=" + end_date + ", "
                    + "retrieval_ref_number=" + retrieval_ref_number + ", "
                    + "acquirer_institution_id=" + acquirer_institution_id + ", "
                    + "destination_acquiring_institution_id=" + destination_acquiring_institution_id + ", "
                    + "pan=" + pan + ", "
                    + "rrn=" + rrn + ", "
                    + "terminal_id=" + terminal_id + ", "
                    + "merchant_id=" + merchant_id + ", "
                    + "location_name_address=" + location_name_address + ", "
                    + "approval_code=" + approval_code + ", "
                    + "page=" + page + ", "
                    + "limit=" + limit + ", "
                    + "isCurrent=" + isCurrent);

            // Build the WHERE clause using StringBuilder.
            StringBuilder whereClause = new StringBuilder();
            boolean firstCondition = true;

            if (!message_type.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.message_type = '").append(message_type).append("'");
            }
            if (!bin.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.bin = ").append(bin);
            }
            if (!processing_code.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.processing_code = ").append(processing_code);
            }
            if (!system_trace_number.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.system_trace_number = '").append(system_trace_number).append("'");
            }
            if (!response_code.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                if (response_code.equals("111")) {
                    whereClause.append("a.response_code != '00'");
                } else {
                    whereClause.append("a.response_code = ").append(response_code);
                }
            }
            if (!retrieval_ref_number.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.retrieval_ref_number = '").append(retrieval_ref_number).append("'");
            }
            if (!acquirer_institution_id.isEmpty()) {
                if (acquirer_institution_id.equals(destination_acquiring_institution_id)) {
                    if (firstCondition) {
                        whereClause.append(" WHERE ");
                        firstCondition = false;
                    } else {
                        whereClause.append(" AND ");
                    }
                    whereClause.append("(a.acquirer_institution_id = '").append(acquirer_institution_id)
                            .append("' OR a.destination_acquiring_institution_id = '").append(destination_acquiring_institution_id).append("')");
                } else {
                    if (firstCondition) {
                        whereClause.append(" WHERE ");
                        firstCondition = false;
                    } else {
                        whereClause.append(" AND ");
                    }
                    whereClause.append("a.destination_acquiring_institution_id = '").append(acquirer_institution_id).append("'");
                }
            } else {
                if (!destination_acquiring_institution_id.isEmpty()) {
                    if (firstCondition) {
                        whereClause.append(" WHERE ");
                        firstCondition = false;
                    } else {
                        whereClause.append(" AND ");
                    }
                    whereClause.append("a.destination_acquiring_institution_id = '").append(destination_acquiring_institution_id).append("'");
                }
            }
            if (!pan.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.pan = '").append(pan).append("'");
            }
            if (!rrn.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.retrieval_ref_number = '").append(rrn).append("'");
            }
            if (!terminal_id.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.terminal_id IN (").append(terminal_id).append(")");
            }
            if (!merchant_id.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.merchant_id IN (").append(merchant_id).append(")");
            }
            if (!location_name_address.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.location_name_address = '").append(location_name_address).append("'");
            }
            if (!approval_code.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.approval_code = '").append(approval_code).append("'");
            }
            if (!min_amount.isEmpty() && Double.parseDouble(min_amount) > 0) {
                Double minAmountVal = Double.parseDouble(min_amount) * 100;
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.amount LIKE '%").append(minAmountVal.toString().replace(".0", "")).append("'");
            }
            if (!max_amount.isEmpty() && Double.parseDouble(max_amount) > 0) {
                Double maxAmountVal = Double.parseDouble(max_amount) * 100;
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.amount LIKE '%").append(maxAmountVal.toString().replace(".0", "")).append("'");
            }
            if (!start_date.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.ncs_date_time >= '").append(start_date).append("'");
            }
            if (!end_date.isEmpty()) {
                if (firstCondition) {
                    whereClause.append(" WHERE ");
                    firstCondition = false;
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append("a.ncs_date_time < '").append(end_date).append("'");
            }

            String whereQuery = whereClause.toString();
            logger.info("Constructed WHERE clause: " + whereQuery);

            String SQL;
            List<CardsTransactionModel> transactions;
            int offset = page > 1 ? (page - 1) * limit : 0;

            // Determine which table to query based on the flag.
            if (isCurrent) {
                logger.info("Querying current transactions.");
                SQL = "SELECT a.*, b.station_name FROM sparkpay.transactions a "
                        + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                        + whereQuery
                        + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                logger.info("Current transactions SQL: " + SQL);
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                        + "FROM sparkpay.transactions a " + whereQuery;
            } else {
                logger.info("Querying historical transactions.");
                SQL = "SELECT a.*, b.station_name FROM sparkpay.transaction_hist_s a "
                        + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                        + whereQuery
                        + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                logger.info("Historical transactions SQL: " + SQL);
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                        + "FROM sparkpay.transaction_hist_s a " + whereQuery;
            }

            LocalTime currentTime = LocalTime.now();
            int hour = currentTime.getHour();
            logger.info("Current hour: " + hour);
            if (isCurrent && transactions.size() < 1 && hour >= 12) {
                logger.info("No current transactions found and hour >= 12; switching to historical transactions.");
                SQL = "SELECT a.*, b.station_name FROM sparkpay.transaction_hist_s a "
                        + "LEFT JOIN sparkpay.station_pcis b ON a.destination_acquiring_institution_id = b.acquiring_institution_id "
                        + whereQuery
                        + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
                logger.info("Historical transactions SQL (fallback): " + SQL);
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsMapper());

                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                        + "FROM sparkpay.transaction_hist_s a " + whereQuery;
            }

            logger.info("Executing aggregation query: " + SQL);
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            Double totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;

            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords
                    + ", \"page\": " + page + ", \"limit\": " + limit + "}";
            networkResponse.setMeta(meta);
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched transactions");
            networkResponse.setData((ArrayList) transactions);

            logger.info("SearchTransactions completed successfully with " + transactions.size() + " records found.");
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException in SearchTransactions: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    private boolean CheckDisputeExist(String terminalid, String rrn, String stan) {
        logger.info(String.format(
                "🟢 EXIST 1: CheckDisputeExist() | terminalid=%s, rrn=%s, stan=%s",
                terminalid, rrn, stan
        ));
        try {
            String SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_disputes " +
                    "WHERE terminal_id = ? AND retrieval_ref_number = ? AND system_trace_number = ?";
            logger.info(String.format("🟢 EXIST 2: SQL → %s", SQL));
            Integer totalRows = jdbcTemplate.queryForObject(
                    SQL, new Object[]{ terminalid, rrn, stan }, Integer.class
            );
            boolean found = (totalRows != null && totalRows > 0);
            logger.info(String.format("🟢 EXIST 3: found=%s (count=%d)", found, (totalRows == null ? 0 : totalRows)));
            return found;
        } catch (DataAccessException ex) {
            logger.info(String.format("🔴 EXIST ERR: %s", ex.getMessage()));
            return false;
        }
    }

    private boolean isTxnAlreadyReversed(String terminalid, String rrn, String stan) {
        logger.info(String.format(
                "🟢 REV 1: isTxnAlreadyReversed() called | terminalid=%s, rrn=%s, stan=%s",
                terminalid, rrn, stan
        ));
        try {
            String SQL = "SELECT COUNT(*) " +
                    "FROM sparkpay.transaction_hist_s " +
                    "WHERE terminal_id = ? " +
                    "  AND retrieval_ref_number = ? " +
                    "  AND system_trace_number = ? " +
                    "  AND COALESCE(isTxnReversed,'') = '00'";
            logger.info(String.format("🟢 REV 2: SQL → %s", SQL));
            Integer totalRows = jdbcTemplate.queryForObject(
                    SQL, new Object[]{ terminalid, rrn, stan }, Integer.class
            );
            boolean reversed = (totalRows != null && totalRows > 0);
            logger.info(String.format(
                    "🟢 REV 3: Result → reversed=%s (count=%d)",
                    reversed, (totalRows == null ? 0 : totalRows)
            ));
            return reversed;
        } catch (DataAccessException ex) {
            logger.info(String.format("🔴 REV ERR: %s", ex.getMessage()));
            return false; // fail-open (treat as not reversed) or flip to true if you prefer fail-closed
        }
    }

    @Override
    public ResponseEntity LogDisputesBulk(String sessiontoken, String records, String username) {
        long t0 = System.currentTimeMillis();
        logger.info(String.format("🟢 Step 1: LogDisputesBulk() called | username=%s, sessiontokenPresent=%s",
                username, (sessiontoken != null)));

        try {
            // 🟢 Step 2: Parse input JSON array
            JSONArray jsonRecords = new JSONArray(records);
            logger.info(String.format("🟢 Step 2: Parsed %d record(s) from input JSON", jsonRecords.length()));

            int found = 0;                 // valid for logging
            int recorded = 0;              // actually inserted
            int skippedExists = 0;         // skipped because dispute already exists
            int skippedReversed = 0;       // skipped because txn already reversed (isTxnReversed='00')
            int skippedOldOrResp = 0;      // skipped due to age/respCode gate

            int userrole = GetUserRole(sessiontoken);
            logger.info(String.format("🟢 Step 3: User role resolved → user=%s, role=%d", username, userrole));

            // 🟢 Step 4: Iterate rows
            for (int i = 0; i < jsonRecords.length(); i++) {
                JSONObject rec = jsonRecords.getJSONObject(i);
                // tolerate whitespace and BOM if any
                String terminalid = rec.getString("terminalid").trim().replace("\uFEFF", "");
                String rrn        = rec.getString("rrn").trim();
                String stan       = rec.getString("stan").trim();

                logger.info(String.format("🟢 Step 4.%d: Processing | terminalid=%s, rrn=%s, stan=%s",
                        (i + 1), terminalid, rrn, stan));

                // 4.A — Already logged?
                boolean exists = CheckDisputeExist(terminalid, rrn, stan);
                logger.info(String.format("🟢 Step 4.%d.A: CheckDisputeExist → %s", (i + 1), exists));
                if (exists) {
                    skippedExists++;
                    logger.info(String.format("🟡 Step 4.%d.A1: Skipping — dispute already exists", (i + 1)));
                    continue;
                }

                // 4.B — Already reversed?
                boolean reversed = isTxnAlreadyReversed(terminalid, rrn, stan);
                logger.info(String.format("🟢 Step 4.%d.B: isTxnAlreadyReversed → %s", (i + 1), reversed));
                if (reversed) {
                    skippedReversed++;
                    logger.info(String.format("🟡 Step 4.%d.B1: Skipping — transaction reversed (isTxnReversed='00')", (i + 1)));
                    continue;
                }

                // 4.C — Fetch original txn
                List<CardsTransactionModel> txns = GetTransaction(terminalid, rrn, stan, false);
                logger.info(String.format("🟢 Step 4.%d.C: GetTransaction returned %d record(s)",
                        (i + 1), txns.size()));

                if (txns.isEmpty()) {
                    logger.info(String.format("🟡 Step 4.%d.C1: No matching transaction found — skipping", (i + 1)));
                    continue;
                }

                CardsTransactionModel t = txns.get(0);

                String tnxDate = t.getNcs_date_time();
                int daysAgo = dateUtil.daysAgo(tnxDate);
                String respCode = t.getResponse_code();

                logger.info(String.format("🟢 Step 4.%d.D: Txn snapshot → ncs_date_time=%s, daysAgo=%d, respCode=%s",
                        (i + 1), tnxDate, daysAgo, respCode));

                // You previously allowed logging if respCode == "00" AND (days<=120 OR role==8)
                if (respCode.equals("00") && (daysAgo <= 120 || userrole == 8)) {
                    found++;

                    int additionalDays = dateUtil.getDisputeTimeLineDate();
                    logger.info(String.format("🟢 Step 4.%d.E: Valid for logging → additionalDays=%d", (i + 1), additionalDays));

                    String unique_log_code = terminalid + stan + rrn;
                    String nuban = ""; // kept as in your original
                    String disputeType = !respCode.equals("00") ? "habari" : "institution";
                    logger.info(String.format("🟢 Step 4.%d.F: unique_log_code=%s, disputeType=%s", (i + 1), unique_log_code, disputeType));

                    String SQL = "INSERT INTO sparkpayweb_db.tbl_disputes(" +
                            "id, unique_log_code, terminal_id, merchant_id, system_trace_number, retrieval_ref_number, " +
                            "logged_by, owner_institution, type, status, date_created, timeline_date, cardholder_acct_nuban, " +
                            "message_type, pan, amount, destination_acquiring_institution_id, acquirer_institution_id, bin, " +
                            "ncs_date_time, response_code, cardholder_acct_number) " +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', NOW(), ADDDATE(NOW(), ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    logger.info(String.format("🟢 Step 4.%d.G: INSERT SQL → %s", (i + 1), SQL));
                    logger.info(String.format("🟢 Step 4.%d.G1: INSERT params → id=%s, unique_log_code=%s, terminalid=%s, merchant_id=%s, stan=%s, rrn=%s, username=%s, owner_institution=%s, type=%s, addDays=%d, nuban=%s, message_type=%s, pan=%s, amount=%s, dest_acq_id=%s, acq_id=%s, bin=%s, ncs_date_time=%s, response_code=%s, cardholder_acct_number=%s",
                            (i + 1),
                            t.getId(), unique_log_code, terminalid, t.getMerchant_id(),
                            stan, rrn, username, t.getAcquirer_institution_id(), disputeType, additionalDays, nuban,
                            t.getMessage_type(), t.getPan(), t.getRawAmount(), t.getDestination_acquiring_institution_id(),
                            t.getAcquirer_institution_id(), t.getBin(), t.getNcs_date_time(), t.getResponse_code(),
                            t.getCardholder_acct_number()
                    ));

                    int retval = jdbcTemplate.update(SQL, new Object[]{
                            t.getId(), unique_log_code, terminalid, t.getMerchant_id(),
                            stan, rrn, username, t.getAcquirer_institution_id(), disputeType,
                            additionalDays, nuban, t.getMessage_type(), t.getPan(),
                            t.getRawAmount(), t.getDestination_acquiring_institution_id(),
                            t.getAcquirer_institution_id(), t.getBin(),
                            t.getNcs_date_time(), t.getResponse_code(), t.getCardholder_acct_number()
                    });

                    logger.info(String.format("🟢 Step 4.%d.H: INSERT retval=%d", (i + 1), retval));

                    // auto-resolve for role==8 (kept from your original logic)
                    if (userrole == 8) {
                        String autoSql = "UPDATE sparkpayweb_db.tbl_disputes " +
                                "SET resolved_by = ?, status = '0', resolved = '0', date_modified = NOW() " +
                                "WHERE terminal_id = ? AND retrieval_ref_number = ? AND system_trace_number = ?";
                        logger.info(String.format("🟢 Step 4.%d.I: Role==8, auto-resolve UPDATE → %s", (i + 1), autoSql));
                        logger.info(String.format("🟢 Step 4.%d.I1: params → username=%s, terminalid=%s, rrn=%s, stan=%s",
                                (i + 1), username, terminalid, rrn, stan));
                        jdbcTemplate.update(autoSql, new Object[]{ username, terminalid, rrn, stan });
                    }

                    if (retval > 0) {
                        recorded++;
                        logger.info(String.format("🟢 Step 4.%d.J: recorded++ → %d", (i + 1), recorded));
                    }
                } else {
                    skippedOldOrResp++;
                    logger.info(String.format("🟡 Step 4.%d.Z: Skipping — respCode=%s, daysAgo=%d, role=%d (rule: respCode=='00' && (days<=120 || role==8))",
                            (i + 1), respCode, daysAgo, userrole));
                }
            }

            // 🟢 Step 5: Build response summary
            String summary = String.format(
                    "Total Records: %d%nValid Records: %d%nRecorded: %d%nSkipped (exists): %d%nSkipped (reversed): %d%nSkipped (age/resp): %d",
                    jsonRecords.length(), found, recorded, skippedExists, skippedReversed, skippedOldOrResp
            );
            logger.info(String.format("🟢 Step 5: Summary → %s", summary));

            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(summary);

            logger.info(String.format("🟢 Step 6: Returning OK | elapsed=%d ms", (System.currentTimeMillis() - t0)));
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info(String.format("🔴 Step ERR: DataAccessException → %s", ex.getMessage()));
            return responseManager.ResponseInternalServerError();
        } catch (JSONException ex) {
            logger.info(String.format("🔴 Step ERR: JSONException → %s", ex.getMessage()));
            Logger.getLogger(CardsTransactionsService.class.getName()).log(Level.SEVERE, null, ex);
            return responseManager.ResponseBadRequest(); // better than returning null
        }
    }

    @Override
    public ResponseEntity LogDisputesBulkExternal(String sessiontoken, String records, String username) {
        logger.info(String.format("🟢 Step 1: LogDisputesBulkExternal called | sessiontokenPresent=%s, username=%s, rawRecordsLength=%d",
                (sessiontoken != null), username, records != null ? records.length() : 0));
        try {
            JSONArray jsonRecords = new JSONArray(records);
            int found = 0;
            int recorded = 0;
            int skippedExists = 0;
            int skippedReversed = 0;

            int userrole = GetUserRoleExternal(sessiontoken);
            logger.info(String.format("🟢 Step 2: Parsed %d records; userrole=%d", jsonRecords.length(), userrole));
            if (userrole == -100) {
                logger.info(String.format("🔴 Step 2.1: Unauthorized userrole=%d → returning 401", userrole));
                return responseManager.ResponseUnathorized();
            }

            for (int i = 0; i < jsonRecords.length(); i++) {
                JSONObject obj = jsonRecords.getJSONObject(i);
                String terminalid = obj.getString("terminalid").trim().replace("\uFEFF", "");
                String merchantid = obj.getString("merchantid").trim();
                String rrn        = obj.getString("rrn").trim();
                String stan       = obj.getString("stan").trim();

                logger.info(String.format("🟢 Step 3.%d: Processing | terminalid=%s, merchantid=%s, rrn=%s, stan=%s",
                        i + 1, terminalid, merchantid, rrn, stan));

                boolean exists = CheckDisputeExist(terminalid, rrn, stan);
                logger.info(String.format("🟢 Step 3.%d.A: CheckDisputeExist → %s", i + 1, exists));
                if (exists) {
                    skippedExists++;
                    logger.info(String.format("🟡 Step 3.%d.A1: Skipping — dispute already exists", i + 1));
                    continue;
                }

                boolean reversed = isTxnAlreadyReversed(terminalid, rrn, stan);
                logger.info(String.format("🟢 Step 3.%d.B: isTxnAlreadyReversed → %s", i + 1, reversed));
                if (reversed) {
                    skippedReversed++;
                    logger.info(String.format("🟡 Step 3.%d.B1: Skipping — transaction already reversed (isTxnReversed='00')", i + 1));
                    continue;
                }

                List<CardsTransactionModel> txns = getTransactionExternal(terminalid, rrn, stan, merchantid, false);
                logger.info(String.format("🟢 Step 3.%d.C: getTransactionExternal returned %d record(s)", i + 1, txns.size()));
                if (txns.isEmpty()) {
                    logger.info(String.format("🟡 Step 3.%d.C1: No matching transaction found — skipping", i + 1));
                    continue;
                }

                CardsTransactionModel txn = txns.get(0);
                String tnxDate = txn.getNcs_date_time();
                int daysAgo = dateUtil.daysAgo(tnxDate);
                logger.info(String.format("🟢 Step 3.%d.D: Txn snapshot → ncs_date_time=%s, daysAgo=%d, response_code=%s",
                        i + 1, tnxDate, daysAgo, txn.getResponse_code()));

                if ("00".equals(txn.getResponse_code()) && (daysAgo <= 120 || userrole == 8)) {
                    found++;
                    int additionalDays = dateUtil.getDisputeTimeLineDate();
                    String uniqueLogCode = terminalid + stan + rrn;
                    String disputeType = "institution";
                    logger.info(String.format("🟢 Step 3.%d.E: Valid for logging → uniqueLogCode=%s, disputeType=%s, additionalDays=%d",
                            i + 1, uniqueLogCode, disputeType, additionalDays));

                    String insertSql =
                            "INSERT INTO sparkpayweb_db.tbl_disputes(" +
                                    "id, unique_log_code, terminal_id, merchant_id, system_trace_number, retrieval_ref_number, " +
                                    "logged_by, owner_institution, type, status, date_created, timeline_date, cardholder_acct_nuban, " +
                                    "message_type, pan, amount, destination_acquiring_institution_id, acquirer_institution_id, bin, " +
                                    "ncs_date_time, response_code, cardholder_acct_number) " +
                                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', NOW(), ADDDATE(NOW(), ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    Object[] insertParams = {
                            txn.getId(), uniqueLogCode, terminalid, txn.getMerchant_id(), stan, rrn,
                            username, txn.getAcquirer_institution_id(), disputeType, additionalDays,
                            "", txn.getMessage_type(), txn.getPan(), txn.getRawAmount(),
                            txn.getDestination_acquiring_institution_id(), txn.getAcquirer_institution_id(),
                            txn.getBin(), txn.getNcs_date_time(), txn.getResponse_code(), txn.getCardholder_acct_number()
                    };

                    logger.info(String.format("🟢 Step 3.%d.F: INSERT SQL → %s", i + 1, insertSql));
                    logger.info(String.format("🟢 Step 3.%d.F1: INSERT params → %s", i + 1, java.util.Arrays.toString(insertParams)));

                    int retval = jdbcTemplate.update(insertSql, insertParams);
                    logger.info(String.format("🟢 Step 3.%d.G: INSERT returned %d", i + 1, retval));

                    if (userrole == 8) {
                        String updateSql =
                                "UPDATE sparkpayweb_db.tbl_disputes " +
                                        "SET resolved_by=?, status='0', resolved='0', date_modified=NOW() " +
                                        "WHERE terminal_id=? AND retrieval_ref_number=? AND system_trace_number=?";
                        Object[] updateParams = { username, terminalid, rrn, stan };
                        logger.info(String.format("🟢 Step 3.%d.H: Auto-resolve UPDATE → %s", i + 1, updateSql));
                        logger.info(String.format("🟢 Step 3.%d.H1: UPDATE params → %s", i + 1, java.util.Arrays.toString(updateParams)));
                        jdbcTemplate.update(updateSql, updateParams);
                    }

                    if (retval > 0) {
                        recorded++;
                    }
                }
            }

            logger.info(String.format("🟢 Step 4: Loop complete | total=%d, valid=%d, recorded=%d, skippedExists=%d, skippedReversed=%d",
                    jsonRecords.length(), found, recorded, skippedExists, skippedReversed));

            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(String.format(
                    "Total Records: %d%nValid Records: %d%nRecorded: %d%nSkipped (exists): %d%nSkipped (reversed): %d",
                    jsonRecords.length(), found, recorded, skippedExists, skippedReversed
            ));
            logger.info(String.format("🟢 Step 5: Returning success response → %s", networkResponse.getMessage()));
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info(String.format("🔴 Step ERR: DataAccessException → %s", ex.getMessage()));
            ex.printStackTrace();
            return responseManager.ResponseInternalServerError();
        } catch (JSONException ex) {
            logger.info(String.format("🔴 Step ERR: JSONException → %s", ex.toString()));
            ex.printStackTrace();
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity UpdateCardsDisputesNUBAN() {
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
                    networkResponse.setMessage("Row: " + _id + " updated! Select Number: " + cardholder_acct_number + ", NUBAN: " + nuban);
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
    public ResponseEntity UpdateDisputesData() {
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
            } else {
                return responseManager.ResponseDeleted();
            }

        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity LogDispute(String sessiontoken, String terminalid, String rrn, String stan,
                                     String proof_of_debit_uri, String username, boolean isExternal) {

        logger.info(String.format(
                "🟢 Step 1: Entering LogDispute | sessiontokenPresent=%s, terminalid=%s, rrn=%s, stan=%s, username=%s, isExternal=%b",
                (sessiontoken != null), terminalid, rrn, stan, username, isExternal
        ));

        try {
            // 1) Duplicate check
            boolean exists = CheckDisputeExist(terminalid, rrn, stan);
            String unique_log_code = terminalid + stan + rrn;
            logger.info(String.format(
                    "🟢 Step 2: CheckDisputeExist → %b | unique_log_code=%s",
                    exists, unique_log_code
            ));
            if (exists) {
                logger.info(String.format("🟡 Step 2.1: Dispute already exists → aborting | %s", unique_log_code));
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Cannot log dispute with same details twice\nID: " + unique_log_code);
                return responseManager.ResponseOk(networkResponse);
            }

            // 2) Reversal check — if reversed ('00'), not eligible
            boolean reversed = isTxnAlreadyReversed(terminalid, rrn, stan);
            logger.info(String.format(
                    "🟢 Step 3: isTxnAlreadyReversed → %s | terminalid=%s, rrn=%s, stan=%s",
                    reversed, terminalid, rrn, stan
            ));
            if (reversed) {
                logger.info("🟡 Step 3.1: Transaction already reversed (isTxnReversed='00') → not eligible for dispute");
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Transaction has already been reversed and cannot be logged for dispute");
                return responseManager.ResponseOk(networkResponse);
            }

            // 3) Fetch original transaction
            List<CardsTransactionModel> txns = GetTransaction(terminalid, rrn, stan, false);
            logger.info(String.format(
                    "🟢 Step 4: GetTransaction returned %d record(s) | terminalid=%s, rrn=%s, stan=%s",
                    txns.size(), terminalid, rrn, stan
            ));
            if (txns.isEmpty()) {
                logger.info("🟡 Step 4.1: No transaction found → returning 404-style payload");
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(404);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Transaction not found");
                return responseManager.ResponseOk(networkResponse);
            }

            CardsTransactionModel t = txns.get(0);

            // 4) Role + eligibility checks
            int userrole = GetUserRole(sessiontoken);
            logger.info(String.format("🟢 Step 5: User role resolved → %d", userrole));

            if (!"00".equals(t.getResponse_code())) {
                logger.info(String.format(
                        "🟡 Step 5.1: response_code=%s != '00' → not eligible",
                        t.getResponse_code()
                ));
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Only completely processed or approved transactions can be logged for dispute");
                return responseManager.ResponseOk(networkResponse);
            }

            int additionalDays = dateUtil.getDisputeTimeLineDate();
            String tnxDate = t.getNcs_date_time();
            int daysAgo = dateUtil.daysAgo(tnxDate);
            logger.info(String.format(
                    "🟢 Step 6: Txn age check → ncs_date_time=%s, daysAgo=%d, windowAllowed=%d",
                    tnxDate, daysAgo, additionalDays
            ));

            if (daysAgo > 120 && userrole != 8) {
                logger.info(String.format(
                        "🟡 Step 6.1: Too old (>%d days) and userrole=%d != 8 → reject",
                        120, userrole
                ));
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Transaction occured more than 120 days ago and cannot be logged");
                return responseManager.ResponseOk(networkResponse);
            }

            // 5) Prepare insert
            String disputeType = "institution"; // resp_code is '00' here
            logger.info(String.format("🟢 Step 7: disputeType=%s | unique_log_code=%s", disputeType, unique_log_code));

            String SQL = "INSERT INTO sparkpayweb_db.tbl_disputes(" +
                    "id, unique_log_code, terminal_id, merchant_id, system_trace_number, retrieval_ref_number, " +
                    "logged_by, owner_institution, type, status, date_created, timeline_date, proof_of_debit_uri, " +
                    "cardholder_acct_nuban, message_type, pan, amount, destination_acquiring_institution_id, " +
                    "acquirer_institution_id, bin, ncs_date_time, response_code, cardholder_acct_number" +
                    ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', now(), ADDDATE(now(), ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            logger.info(String.format("🟢 Step 7.1: INSERT SQL → %s", SQL));

            Object[] params = {
                    t.getId(),
                    unique_log_code,
                    terminalid,
                    t.getMerchant_id(),
                    stan,
                    rrn,
                    username,
                    t.getAcquirer_institution_id(),
                    disputeType,
                    additionalDays,
                    proof_of_debit_uri,
                    "", // nuban placeholder
                    t.getMessage_type(),
                    t.getPan(),
                    t.getRawAmount(),
                    t.getDestination_acquiring_institution_id(),
                    t.getAcquirer_institution_id(),
                    t.getBin(),
                    t.getNcs_date_time(),
                    t.getResponse_code(),
                    t.getCardholder_acct_number()
            };
            logger.info(String.format("🟢 Step 7.2: INSERT params → %s", java.util.Arrays.toString(params)));

            int retval = jdbcTemplate.update(SQL, params);
            logger.info(String.format("🟢 Step 8: INSERT affected rows → %d", retval));

            // 6) Auto-resolve for role==8 (or external)
            if (userrole == 8 || isExternal) {
                String updateSQL = "UPDATE sparkpayweb_db.tbl_disputes " +
                        "SET resolved_by = ?, status = '0', resolved = '0', date_modified = now() " +
                        "WHERE terminal_id = ? AND retrieval_ref_number = ? AND system_trace_number = ?";
                logger.info(String.format("🟢 Step 9: Auto-resolve UPDATE → %s", updateSQL));
                jdbcTemplate.update(updateSQL, new Object[]{ username, terminalid, rrn, stan });
                logger.info("🟢 Step 9.1: Dispute marked resolved (PTSP/external or role==8)");
            }

            if (retval > 0) {
                // 7) Notify PTSP users (kept from your code)
                String ptspSQL = "SELECT ptsp_id FROM sparkpayweb_db.tbl_map_merchants_ptsps WHERE merchant_id = ?";
                String ptspid = jdbcTemplate.queryForObject(ptspSQL, new Object[]{ t.getMerchant_id() }, String.class);
                logger.info(String.format("🟢 Step 10: ptsp_id lookup → ptsp_id=%s, merchant_id=%s", ptspid, t.getMerchant_id()));

                String userSQL = "SELECT user_email FROM tbl_map_card_users_institution WHERE institution_id = ? LIMIT 3";
                java.util.List<java.util.Map<String, Object>> ptspUsers =
                        jdbcTemplate.queryForList(userSQL, new Object[]{ ptspid });
                logger.info(String.format("🟢 Step 10.1: PTSP user emails fetched → %d", ptspUsers.size()));

                for (java.util.Map<String, Object> row : ptspUsers) {
                    String toEmail = (String) row.get("user_email");
                    String message = "<html><body>Dear Team, <br/><br/>Please be informed that a new dispute has been logged against your institution. " +
                            "Please login to sparkpay and find the dispute under the unique log code " + unique_log_code +
                            "<br/><br/>Sparkpay,<br/>Cheers</body></html>";
                    logger.info(String.format("🟢 Step 10.2: Sending email → %s", toEmail));
                    try {
                        mailers.SendMailWithHabariOkHttpClient(
                                "New Dispute Log",
                                "no-reply@habaripay.com",
                                toEmail,
                                message
                        );
                        logger.info(String.format("🟢 Step 10.3: Mail sent → %s", toEmail));
                    } catch (Exception e) {
                        logger.info(String.format("🔴 Step 10.3: Mailer error for %s → %s", toEmail, e.toString()));
                    }
                }

                logger.info("🟢 Step 11: Returning 202 Accepted");
                return responseManager.ResponseAccepted();
            } else {
                logger.info("🔴 Step 11: INSERT returned 0 rows → internal error");
                return responseManager.ResponseInternalServerError();
            }

        } catch (DataAccessException ex) {
            logger.info(String.format("🔴 Step ERR: DataAccessException in LogDispute → %s", ex.getMessage()));
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
            if (inString.toString().equals("(")) {
                inString = inString.deleteCharAt(inString.length() - 1);
            }
            if (inString.toString().equals("")) {
                inString = inString.append("(-1");
            }
            inString = inString.append(")");
            SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                    + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                    + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                    + "FROM sparkpayweb_db.tbl_disputes a "
                    //                            + "LEFT JOIN sparkpay.transaction_hist_s b "
                    //                            + "ON a.id = b.id "
                    + "WHERE ((a.status = 1 AND a.resolved = 1) || a.resolved = 0) AND a.merchant_id IN " + inString.toString() + " AND a.response_code = '00' ORDER BY a.date_created DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsDisputesMapper());

            SQL = "SELECT "
                    + "SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
                    + "FROM sparkpayweb_db.tbl_disputes a "
                    //                    + "LEFT JOIN sparkpay.transaction_hist_s b "
                    //                    + "ON a.id = b.id "
                    + "WHERE ((a.status = 1 AND a.resolved = 1) || a.resolved = 0) AND a.merchant_id IN " + inString.toString() + " AND a.response_code = '00'";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);

            Map<String, Object> row = agg.get(0);
            Double tValue = (Double) row.get("totalValue");
            totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";

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
            switch (institutioncode) {
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
            String meta = "{\"totalValue\": " + totalValue + "}";

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
        logger.info(String.format("Entering GetDisputes with uniqueIds: %s", uniqueIds));
        NetworkResponse networkResponse = new NetworkResponse();
        List<String> idArray = new ArrayList<>(Arrays.asList(uniqueIds.split(",")));
        logger.info(String.format("Parsed %d IDs", idArray.size()));
        if (idArray.size() > 10) {
            logger.info("Request size too large, returning 400");
            networkResponse.setCode(400);
            networkResponse.setMessage("Request size must be lesser or equal to 10");
            networkResponse.setStatus("bad request");
            return responseManager.ResponseOk(networkResponse);
        }
        try {
            String placeholders = idArray.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));
            String SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, "
                    + "a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, "
                    + "a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, a.message_type, "
                    + "a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, "
                    + "a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                    + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, "
                    + "a.cardholder_acct_number "
                    + "FROM sparkpayweb_db.tbl_disputes a "
                    + "WHERE a.unique_log_code IN (" + placeholders + ")";
            logger.info(String.format("Executing SQL: %s", SQL));
            List<CardsDisputeModel> transactions = jdbcTemplate.query(SQL, idArray.toArray(), new CardsTransactionsDisputesMapper());
            logger.info(String.format("Retrieved %d dispute records", transactions.size()));

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Found total of: " + transactions.size() + " disputes");
            networkResponse.setData((ArrayList) transactions);
            logger.info("GetDisputes completed successfully");
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info(String.format("DataAccessException in GetDisputes: %s", ex.getMessage()));
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
            switch (institutioncode) {
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
            totalValue = tValue != null ? tValue / 100 : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";

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
        logger.info(String.format(
                "SearchDisputes called with terminal_id=%s, system_trace_number=%s, retrieval_ref_number=%s, response_code=%s, dispute_status=%s, dispute_type=%s, date_logged=%s, date_resolved=%s, timeline_date=%s, merchants=%s, pan=%s, uniqueLogId=%s, page=%d, limit=%d",
                terminal_id, system_trace_number, retrieval_ref_number, transaction_response_code,
                dispute_status, dispute_type, date_logged, date_resolved, timeline_date,
                merchantsasIds, pan, uniquelogid, page, limit
        ));

        try {
            List<String> clauses = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            int offset = page > 1 ? (page - 1) * limit : 0;

            if (StringUtils.hasText(terminal_id)) {
                clauses.add("a.terminal_id = ?");
                params.add(terminal_id);
            }
            if (StringUtils.hasText(system_trace_number)) {
                clauses.add("a.system_trace_number = ?");
                params.add(system_trace_number);
            }
            if (StringUtils.hasText(retrieval_ref_number)) {
                clauses.add("a.retrieval_ref_number = ?");
                params.add(retrieval_ref_number);
            }
            if (StringUtils.hasText(transaction_response_code)) {
                clauses.add("a.response_code = ?");
                params.add(transaction_response_code);
            }
            if (StringUtils.hasText(pan)) {
                clauses.add("a.pan = ?");
                params.add(pan);
            }
            if (StringUtils.hasText(uniquelogid)) {
                clauses.add("a.unique_log_code = ?");
                params.add(uniquelogid);
            }
            if (StringUtils.hasText(merchantsasIds)) {
                List<String> mids = Arrays.stream(merchantsasIds.split(","))
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList());
                if (!mids.isEmpty()) {
                    String placeholders = mids.stream().map(m -> "?").collect(Collectors.joining(","));
                    clauses.add("a.merchant_id IN (" + placeholders + ")");
                    params.addAll(mids);
                }
            }
            switch (dispute_status) {
                case "-1":
                    clauses.add("a.status = -1 AND a.resolved = 0");
                    break;
                case "0":
                    clauses.add("a.status = 0  AND a.resolved = 0");
                    break;
                case "1":
                    clauses.add("a.status = 1  AND a.resolved = 1");
                    break;
                case "-2":
                    clauses.add("a.status = -2  AND a.resolved = 1");
                    break;
            }
            if ("charge-back".equals(dispute_type)) {
                clauses.add("a.logged_by != a.resolved_by");
            } else if ("air".equals(dispute_type)) {
                clauses.add("a.logged_by = a.resolved_by");
            }

            BiConsumer<String, String> bindRange = (col, range) -> {
                String[] parts = range.split("E", 2);
                if (parts.length == 2) {
                    // parse each side as a full timestamp
                    LocalDateTime start = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    LocalDateTime end = LocalDateTime.parse(parts[1], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    clauses.add("a." + col + " BETWEEN ? AND ?");
                    params.add(start);
                    params.add(end);
                }
            };
            if (StringUtils.hasText(date_logged)) {
                bindRange.accept("date_created", date_logged);
            }
            if (StringUtils.hasText(date_resolved)) {
                bindRange.accept("date_modified", date_resolved);
            }
            if (StringUtils.hasText(timeline_date)) {
                bindRange.accept("timeline_date", timeline_date);
            }

            String where = clauses.isEmpty()
                    ? ""
                    : " WHERE " + String.join(" AND ", clauses);
            logger.info(String.format("Where query: %s", where));

            // main query
            String sql = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved,"
                    + " a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri,"
                    + " a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated,"
                    + " a.cardholder_acct_nuban, a.message_type, a.pan, a.amount,"
                    + " a.system_trace_number, a.retrieval_ref_number,"
                    + " a.destination_acquiring_institution_id, a.acquirer_institution_id,"
                    + " a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code,"
                    + " a.cardholder_acct_number"
                    + " FROM sparkpayweb_db.tbl_disputes a"
                    + where
                    + " ORDER BY a.date_created DESC LIMIT ? OFFSET ?";
            params.add(limit);
            params.add(offset);

            List<CardsDisputeModel> transactions
                    = jdbcTemplate.query(sql, params.toArray(), new CardsTransactionsDisputesMapper());
            logger.info(String.format("SearchDisputes returned %d rows", transactions.size()));

            // aggregate query
            String aggSql = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords"
                    + " FROM sparkpayweb_db.tbl_disputes a" + where;
            Object[] aggParams = params.subList(0, params.size() - 2).toArray();
            Map<String, Object> row = jdbcTemplate.queryForMap(aggSql, aggParams);
            double totalValue = row.get("totalValue") != null ? ((Number) row.get("totalValue")).doubleValue() / 100 : 0;
            int totalRecords = ((Number) row.get("totalRecords")).intValue();

            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched Disputes Results");
            networkResponse.setData(new ArrayList<>(transactions));
            networkResponse.setMeta(String.format(
                    "{\"totalValue\": %.2f, \"totalRecords\": %d, \"page\": %d, \"limit\": %d}",
                    totalValue, totalRecords, page, limit
            ));

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info(String.format("SearchDisputes returned %s rows", ex));
            return responseManager.ResponseInternalServerError();
        }
    }

//    @Override
//    public ResponseEntity SearchDisputes(
//            String terminal_id,
//            String system_trace_number,
//            String retrieval_ref_number,
//            String transaction_response_code,
//            String dispute_status,
//            String dispute_type,
//            String date_logged,
//            String date_resolved,
//            String timeline_date,
//            String merchantsasIds,
//            String pan,
//            String uniquelogid,
//            int page,
//            int limit
//    ) {
//        NetworkResponse networkResponse = new NetworkResponse();
//        try {
//            String start_date_logged = !date_logged.equals("") ? date_logged.substring(0, 10) : "";
//            String end_date_logged = !date_logged.equals("") ? date_logged.substring(11, date_logged.length()) : "";
//            String start_date_resolved = !date_resolved.equals("") ? date_resolved.substring(0, 10) : "";
//            String end_date_resolved = !date_resolved.equals("") ? date_resolved.substring(11, date_resolved.length()) : "";
//            String start_timeline_date = !timeline_date.equals("") ? timeline_date.substring(0, 10) : "";
//            String end_timeline_date = !timeline_date.equals("") ? timeline_date.substring(11, timeline_date.length()) : "";
//            String SQL;
//            Double totalValue;
//            List<Map<String, Object>> agg;
//            List<CardsDisputeModel> transactions;
//            int offset = page > 1 ? (page - 1) * limit : 0;
//            String whereQuery = !terminal_id.equals("")
//                    || !system_trace_number.equals("")
//                    || !retrieval_ref_number.equals("")
//                    || !transaction_response_code.equals("")
//                    || !dispute_status.equals("")
//                    || !dispute_type.equals("")
//                    || !start_date_logged.equals("")
//                    || !end_date_logged.equals("")
//                    || !start_date_resolved.equals("")
//                    || !end_date_resolved.equals("")
//                    || !start_timeline_date.equals("")
//                    || !end_timeline_date.equals("")
//                    || !merchantsasIds.equals("")
//                    || !pan.equals("")
//                    || !uniquelogid.equals("")
//                    ? "WHERE" : "";
//
//            if (!merchantsasIds.equals("")) {
//                List<String> merchantIds = new ArrayList<>(Arrays.asList(merchantsasIds.split(",")));
//                StringBuilder inString = new StringBuilder("(");
//                for (int i = 0; i < merchantIds.size(); i++) {
//                    inString.append("'").append(merchantIds.get(i)).append("'");
//                    inString.append(",");
//                }
//                inString = inString.deleteCharAt(inString.length() - 1);
//                if (inString.toString().equals("(")) {
//                    inString = inString.deleteCharAt(inString.length() - 1);
//                }
//                if (inString.toString().equals("")) {
//                    inString = inString.append("(-1");
//                }
//                inString = inString.append(")");
//                whereQuery += " a.merchant_id IN " + inString;
//            }
//            if (!terminal_id.equals("")) {
//                whereQuery += " a.terminal_id = '" + terminal_id + "'";
//            }
//            if (!uniquelogid.equals("")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                whereQuery += " a.unique_log_code = '" + uniquelogid + "'";
//            }
//            if (!pan.equals("")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                whereQuery += " a.pan = '" + pan + "'";
//            }
//            if (!system_trace_number.equals("")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                whereQuery += " a.system_trace_number = '" + system_trace_number + "'";
//            }
//            if (!retrieval_ref_number.equals("")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                whereQuery += " a.retrieval_ref_number = '" + retrieval_ref_number + "'";
//            }
//            if (!transaction_response_code.equals("")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                whereQuery += " a.response_code = '" + transaction_response_code + "'";
//            }
//            switch (dispute_status) {
//                case "-1":
//                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                    whereQuery += " a.status = -1 AND a.resolved = 0";
//                    break;
//                case "0":
//                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                    whereQuery += " a.status = 0 AND a.resolved = 0";
//                    break;
//                case "1":
//                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                    whereQuery += " a.status = 1 AND a.resolved = 1";
//                    break;
//                case "-2":
//                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                    whereQuery += " a.status = -2 AND a.resolved = 1";
//                    break;
//                default:
//                    break;
//            }
//            switch (dispute_type) {
//                case "charge-back":
//                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                    whereQuery += " a.logged_by != a.resolved_by";
//                    break;
//                case "air":
//                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                    whereQuery += " a.logged_by = a.resolved_by";
//                    break;
//                default:
//                    break;
//            }
//            if (!start_date_logged.equals("") && !end_date_logged.equals("")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                whereQuery += " a.date_created BETWEEN '" + start_date_logged + "' AND '" + end_date_logged + "'";
//            }
//            if (!start_date_resolved.equals("") && !end_date_resolved.equals("")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                whereQuery += " a.date_modified BETWEEN '" + start_date_resolved + "' AND '" + end_date_resolved + "'";
//            }
//            if (!start_timeline_date.equals("") && !end_timeline_date.equals("")) {
//                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
//                whereQuery += " a.timeline_date BETWEEN '" + start_timeline_date + "' AND '" + end_timeline_date + "'";
//            }
//            SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
//                    + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
//                    + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
//                    + "FROM sparkpayweb_db.tbl_disputes a "
//                    //                    + "LEFT JOIN sparkpay.transaction_hist_s b "
//                    //                    + "ON a.id = b.id " 
//                    + whereQuery
//                    + " ORDER BY a.date_created DESC LIMIT ? OFFSET ?";
//            transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new CardsTransactionsDisputesMapper());
//
//            SQL = "SELECT "
//                    + "SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
//                    + "FROM sparkpayweb_db.tbl_disputes a "
//                    //                    + "LEFT JOIN sparkpay.transaction_hist_s b "
//                    //                    + "ON a.id = b.id " 
//                    + whereQuery;
//            agg = jdbcTemplate.queryForList(SQL);
//            Map<String, Object> row = agg.get(0);
//            Double tValue = (Double) row.get("totalValue");
//            totalValue = tValue != null ? tValue / 100 : 0;
//            Long tRecords = (Long) row.get("totalRecords");
//            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
//            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + 1 + ", \"limit\": " + null + "}";
//
//            networkResponse.setCode(200);
//            networkResponse.setStatus("success");
//            networkResponse.setMessage("Searched Disputes Results");
//            networkResponse.setData((ArrayList) transactions);
//            networkResponse.setMeta(meta);
//
//            return responseManager.ResponseOk(networkResponse);
//        } catch (DataAccessException ex) {
//            System.out.println("error>>>>" + ex.getMessage());
//            return responseManager.ResponseInternalServerError();
//        }
//    }
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
                for (String _id : idS) {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";
                    int _retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, _id});
                    retVal = retVal + _retVal;
                }
            } else {
                if (status == -2) {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET arbitrated_by = ?, status = ?, date_arbitrated = now() WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{username, status, id});
                } else if (status < -2) {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET arbitration_closed_by = ?, arbitrated_proof_uri = ?, status = ?, arbitration_closed_date = now() WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{username, proof_of_reject_uri, status, id});
                } else {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, id});
                }
            }
            String action = status == 0 ? "accepted" : status == -1 ? "arbitrated" : "rejected";
            if (retVal > 0) {
                if (type.equals("bulk")) {
                    return responseManager.ResponseAccepted("Total of " + retVal + " dispute has been " + action);
                } else {
                    return responseManager.ResponseAccepted();
                }
            } else {
                return responseManager.ResponseBadRequest();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity respondToBulkDisputes(
            int unusedId,                    // no longer used
            int status,
            String proof_of_reject_uri,
            String selectedDisputes,         // now holds unique_log_code(s)
            String type,
            String username
    ) {
        long t0 = System.currentTimeMillis();
        logger.info(String.format(
                "🟢 Step 1: respondToBulkDisputes() called | params → unusedId=%d, status=%d, proof_of_reject_uri=%s, selectedDisputes=%s, type=%s, username=%s",
                unusedId, status, proof_of_reject_uri, selectedDisputes, type, username
        ));

        try {
            String SQL;
            int retVal = 0;
            int resolved = (status == 0) ? 0 : 1;
            logger.info(String.format("🟢 Step 2: Computed resolved flag → resolved=%d (status=%d)", resolved, status));

            if ("bulk".equals(type)) {
                logger.info("🟢 Step 3: Entering BULK path");
                String[] codes = (selectedDisputes == null ? new String[0] : selectedDisputes.split(","));
                logger.info(String.format("🟢 Step 3.1: Total unique_log_code items parsed → count=%d", codes.length));

                int idx = 1;
                for (String raw : codes) {
                    String code = (raw == null ? "" : raw.trim());
                    if (code.isEmpty()) {
                        logger.info(String.format("⚠️ Step 3.2.%d: Skipping empty unique_log_code entry", idx));
                        idx++;
                        continue;
                    }

                    SQL = "UPDATE sparkpayweb_db.tbl_disputes " +
                            "SET resolved_by = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? " +
                            "WHERE unique_log_code = ? AND status = ? AND resolved = ?";

                    logger.info(String.format(
                            "🟢 Step 3.3.%d: Executing UPDATE (bulk) | unique_log_code=%s | params=[resolved_by=%s, status=%d, resolved=%d, proof_of_reject_uri=%s]",
                            idx, code, username, status, resolved, proof_of_reject_uri
                    ));

                    int rows = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, code, "-1", "0"});
                    retVal += rows;

                    logger.info(String.format(
                            "🟢 Step 3.4.%d: Update result → rowsAffected=%d | cumulativeUpdated=%d | code=%s",
                            idx, rows, retVal, code
                    ));
                    idx++;
                }

                logger.info(String.format("🟢 Step 3.5: BULK path completed | totalRowsUpdated=%d", retVal));
            } else {
                logger.info("🟢 Step 3: Entering SINGLE-RECORD path");

                if (status == -2) {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes " +
                            "SET arbitrated_by = ?, status = ?, date_arbitrated = now() " +
                            "WHERE unique_log_code = ?";

                    logger.info(String.format(
                            "🟢 Step 3A: Arbitrated branch | unique_log_code=%s | params=[arbitrated_by=%s, status=%d]",
                            selectedDisputes, username, status
                    ));

                    retVal = jdbcTemplate.update(SQL, new Object[]{username, status, selectedDisputes});
                    logger.info(String.format("🟢 Step 3A.1: Update result → rowsAffected=%d", retVal));
                } else if (status < -2) {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes " +
                            "SET arbitration_closed_by = ?, arbitrated_proof_uri = ?, status = ?, arbitration_closed_date = now() " +
                            "WHERE unique_log_code = ?";

                    logger.info(String.format(
                            "🟢 Step 3B: Arbitration CLOSED branch | unique_log_code=%s | params=[arbitration_closed_by=%s, arbitrated_proof_uri=%s, status=%d]",
                            selectedDisputes, username, proof_of_reject_uri, status
                    ));

                    retVal = jdbcTemplate.update(SQL, new Object[]{username, proof_of_reject_uri, status, selectedDisputes});
                    logger.info(String.format("🟢 Step 3B.1: Update result → rowsAffected=%d", retVal));
                } else {
                    SQL = "UPDATE sparkpayweb_db.tbl_disputes " +
                            "SET resolved_by = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? " +
                            "WHERE unique_log_code = ?";

                    logger.info(String.format(
                            "🟢 Step 3C: Default resolve/reject branch | unique_log_code=%s | params=[resolved_by=%s, status=%d, resolved=%d, proof_of_reject_uri=%s]",
                            selectedDisputes, username, status, resolved, proof_of_reject_uri
                    ));

                    retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, selectedDisputes});
                    logger.info(String.format("🟢 Step 3C.1: Update result → rowsAffected=%d", retVal));
                }
                logger.info(String.format("🟢 Step 3Z: SINGLE-RECORD path completed | totalRowsUpdated=%d", retVal));
            }

            String action = (status == 0) ? "accepted"
                    : (status == -1) ? "arbitrated"
                    : "rejected";
            logger.info(String.format("🟢 Step 4: Derived action label → action=%s (status=%d)", action, status));

            if (retVal > 0) {
                if ("bulk".equals(type)) {
                    String msg = String.format("Total of %d dispute(s) have been %s", retVal, action);
                    logger.info(String.format("🟢 Step 5: Success (BULK) → %s", msg));
                    ResponseEntity res = responseManager.ResponseAccepted(msg);
                    logger.info(String.format("🟢 Step 6: Returning ResponseEntity (BULK) → %s", res));
                    logger.info(String.format("🟢 Step 7: Elapsed=%d ms", (System.currentTimeMillis() - t0)));
                    return res;
                } else {
                    logger.info("🟢 Step 5: Success (SINGLE) → ResponseAccepted()");
                    ResponseEntity res = responseManager.ResponseAccepted();
                    logger.info(String.format("🟢 Step 6: Returning ResponseEntity (SINGLE) → %s", res));
                    logger.info(String.format("🟢 Step 7: Elapsed=%d ms", (System.currentTimeMillis() - t0)));
                    return res;
                }
            } else {
                logger.info("🟡 Step 5: No rows updated → ResponseBadRequest()");
                ResponseEntity res = responseManager.ResponseBadRequest();
                logger.info(String.format("🟢 Step 6: Returning ResponseEntity (BAD REQUEST) → %s", res));
                logger.info(String.format("🟢 Step 7: Elapsed=%d ms", (System.currentTimeMillis() - t0)));
                return res;
            }

        } catch (org.springframework.dao.DataAccessException ex) {
            logger.info(String.format("🔴 Step ERR: DataAccessException → message=%s", ex.getMessage()));
            ResponseEntity res = responseManager.ResponseInternalServerError();
            logger.info(String.format("🟢 Step END: Returning ResponseEntity (INTERNAL ERROR) → %s | Elapsed=%d ms",
                    res, (System.currentTimeMillis() - t0)));
            return res;
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
            tnx.setCardholder_acct_number(rs.getString("cardholder_acct_number"));
//            tnx.setCardholder_acct_number(rs.getString("cardholder_acct_number") != null && rs.getString("cardholder_acct_number").length() > 17 ? formatter.FormatCardHolderAcctNum(rs.getString("cardholder_acct_number")) : "");
            tnx.setStatus_code_message(rs.getString("response_code") != null && rs.getString("response_code").toLowerCase() != "null" && rs.getString("response_code") != "" ? transactionsCodeInterpreter.GetResponse(rs.getString("response_code")) : "");
            if (hasColumn(rs, "arbitration_closed_date")) {
                tnx.setArbitration_closed_by(rs.getString("arbitration_closed_date"));
            }
            if (hasColumn(rs, "arbitrated_proof_uri")) {
                tnx.setArbitrated_proof_uri(rs.getString("arbitrated_proof_uri"));
            }
            if (hasColumn(rs, "arbitration_closed_by")) {
                tnx.setArbitration_closed_by(rs.getString("arbitration_closed_by"));
            }
            if (rs.getInt("status") == -1) {
                tnx.setResult("PENDING");
            } else if (rs.getInt("status") == 1) {
                tnx.setResult("REJECTED");
            } else if (rs.getInt("status") == 0 && rs.getInt("resolved") == 0) {
                tnx.setResult("ACCEPTED");
            } else if (rs.getInt("status") == 0 && rs.getInt("resolved") == 1) {
                tnx.setResult("RESOLVED");
            }
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
