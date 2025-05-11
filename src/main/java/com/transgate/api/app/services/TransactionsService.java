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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.transaction.annotation.Transactional;

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
    private Logger logger = Logger.getLogger(TransactionsService.class.getName());
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

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
        List<FullTransactionModel> transactions;
        if (!source.equals("-1")) {
            String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id = ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?)";

            transactions = jdbcTemplate.query(SQL, new Object[]{sessionId, source, source}, new FullTransactionMapper());
        } else {
            String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id = ?";

            transactions = jdbcTemplate.query(SQL, new Object[]{sessionId}, new FullTransactionMapper());
        }
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

//    @Override
//    public ResponseEntity Get(String institutioncode, String startDate, String endDate, int page, int limit, boolean isCurrent) {
//        NetworkResponse networkResponse = new NetworkResponse();
//        try {
//            String SQL;
//            int offset = page > 1 ? (page - 1) * limit : 0;
//            List<FullTransactionModel> transactions;
//            List<Map<String, Object>> agg;
//            if (isCurrent) {
//                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
//                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName, "
//                        + "a.destination_node "
//                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
//                        + "ON a.source_institution_code = b.code "
//                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
//                        + "ON a.destination_institution_code = c.code "
//                        //                    + "LEFT JOIN ajiswitch_db.tbl_transactions_routes n "
//                        //                    + "ON a.destination_node = n.port_number "
//                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
//                        + " ORDER BY id DESC LIMIT ? OFFSET ?";
//                logger.info("sql query: " + SQL);
//                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
//
//                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
//                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
//                        + "ORDER BY a.id DESC";
//                logger.info("sql query: " + SQL);
//                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
//            } else {
//                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
//                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName, "
//                        + "a.destination_node "
//                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
//                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
//                        + "ON a.source_institution_code = b.code "
//                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
//                        + "ON a.destination_institution_code = c.code "
//                        //                + "LEFT JOIN ajiswitch_db.tbl_transactions_routes n "
//                        //                + "ON a.destination_node = n.port_number "
//                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
//                        + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
//                logger.info("sql query: " + SQL);
//                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
//
//                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
//                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
//                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
//                        + "ORDER BY a.id DESC";
//                logger.info("sql query: " + SQL);
//                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
//            }
//
//            Map<String, Object> row = agg.get(0);
//            BigDecimal tValue = (BigDecimal) row.get("totalValue");
//            Double totalValue = tValue != null ? tValue.doubleValue() : 0;
//            Long tRecords = (Long) row.get("totalRecords");
//            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
////            SQL = "SELECT MIN(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
////            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
////            SQL = "SELECT MAX(transaction_date_time) from ajiswitch_db.tbl_creditfundtransfers";
////            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
//            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
//            networkResponse.setMeta(meta);
//
//            networkResponse.setCode(200);
//            networkResponse.setStatus("success");
//            networkResponse.setMessage("All Transactions");
//            networkResponse.setData((ArrayList) transactions);
//
//            return responseManager.ResponseOk(networkResponse);
//        } catch (DataAccessException ex) {
//            System.out.println("error>>>>" + ex.getMessage());
//            return responseManager.ResponseInternalServerError();
//        }
//    }
    @Override
    public ResponseEntity Get(String institutioncode, String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            // Log the entry parameters.
            logger.info("Entering Get transactions method for institution with parameters: startDate=" + startDate
                    + ", endDate=" + endDate + ", page=" + page
                    + ", limit=" + limit + ", isCurrent=" + isCurrent + ", institutioncode=" + institutioncode);

            // Calculate pagination offset and log it.
            int offset = page > 1 ? (page - 1) * limit : 0;
            logger.info("Computed offset: " + offset);

            List<FullTransactionModel> transactions;
            List<Map<String, Object>> agg;
            String SQL;

            if (isCurrent) {
                logger.info("Executing query for current transactions for institution from 'tbl_creditfundtransfers'.");
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName, "
                        + "a.destination_node "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                        + "ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                        + "ON a.destination_institution_code = c.code "
                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
                        + " ORDER BY transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("sql query to fetch current day transactions for institution: " + SQL);
                logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                logger.info("Current transactions query returned " + transactions.size() + " rows.");

                SQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?);";
                logger.info("sql query for summary for institution: " + SQL);
                logger.info("Executing current transactions aggregation query with parameters: [startDate, endDate].");
                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                logger.info("Aggregation query executed for current transactions.");
            } else {
                logger.info("Executing query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName, "
                        + "a.destination_node "
                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                        + "ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                        + "ON a.destination_institution_code = c.code "
                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
                        + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("sql query  to fetch older days transactions for institution: " + SQL);
                logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                logger.info("Historical transactions query returned for institution " + transactions.size() + " rows.");

                SQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?);";
                logger.info("sql query  to fetch hitorical days summary for institution: " + SQL);
                logger.info("Executing historical transactions aggregation query with parameters: [startDate, endDate].");
                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                logger.info("Aggregation query executed for historical transactions.");
            }

            // Process aggregation results.
            if (agg.isEmpty()) {
                logger.info("Aggregation query returned no results. Setting default aggregation values for institution.");
                networkResponse.setMeta("{\"totalValue\": 0, \"totalRecords\": 0, \"page\": " + page + ", \"limit\": " + limit + ", \"successRate\": 0");
            } else {

                Map<String, Object> row = agg.get(0);

                double totalValue = Optional.ofNullable((Number) row.get("totalValue"))
                        .map(Number::doubleValue)
                        .orElse(0.0);
                int totalRecords = Optional.ofNullable((Number) row.get("totalRecords"))
                        .map(Number::intValue)
                        .orElse(0);
                double successRate = Optional.ofNullable((Number) row.get("successRate"))
                        .map(Number::doubleValue)
                        .orElse(0.0);

                String meta = String.format(
                        "{\"totalValue\": %.2f, \"totalRecords\": %d, \"successRate\": %.2f, \"page\": %d, \"limit\": %d}",
                        totalValue, totalRecords, successRate, page, limit);
                networkResponse.setMeta(meta);
                logger.info("Aggregation results processed: " + meta);
            }

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions");
            networkResponse.setData((ArrayList) transactions);
            logger.info("Transaction response composed successfully. Returning response.");

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException occurred while retrieving transactions: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity getInstitutionTransactionsByDateOnly(String institutioncode, String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        ZonedDateTime idKey = ZonedDateTime.now();
        String marker = idKey.format(fmt);
        try {
            // Log the entry parameters.
            logger.info("Entering getInstitutionTransactionsByDateOnly transactions method for institution with parameters: startDate=" + startDate
                    + ", endDate=" + endDate + ", page=" + page
                    + ", limit=" + limit + ", isCurrent=" + isCurrent + ", institutioncode=" + institutioncode);

            // Calculate pagination offset and log it.
            int offset = page > 1 ? (page - 1) * limit : 0;
            logger.info("Computed offset: " + offset);

            List<FullTransactionModel> transactions;
            List<Map<String, Object>> agg;
            String SQL;

            if (isCurrent) {
                logger.info("Executing query for current transactions for institution from 'tbl_creditfundtransfers'.");
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName, "
                        + "a.destination_node "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                        + "ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                        + "ON a.destination_institution_code = c.code "
                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time < ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
                        + " ORDER BY transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("sql query to fetch current day transactions for institution: " + SQL);
                logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): tbl_creditfundtransfers request duration: ---> " + durationMs + " ms");
                logger.info("Current transactions query returned " + transactions.size() + " rows.");

                SQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?);";
                logger.info("sql query for summary for institution: " + SQL);
                logger.info("Executing current transactions aggregation query with parameters: [startDate, endDate].");
                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                logger.info("Aggregation query executed for current transactions.");
            } else {
                logger.info("Executing query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName, "
                        + "a.destination_node "
                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                        + "ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                        + "ON a.destination_institution_code = c.code "
                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time < ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
                        + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("sql query  to fetch older days transactions for institution: " + SQL);
                logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): tbl_creditfundtransfer_hist_s request duration: ---> " + durationMs + " ms");
                logger.info("Historical transactions query returned for institution " + transactions.size() + " rows.");

                SQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?);";

                logger.info("sql query  to fetch hitorical days summary for institution: " + SQL);
                logger.info("Executing historical transactions aggregation query with parameters: [startDate, endDate].");
                ZonedDateTime startTimeAgg = ZonedDateTime.now();
                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                ZonedDateTime endTimeAgg = ZonedDateTime.now();
                long durationMsAgg = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): agg request duration: ---> " + durationMsAgg + " ms");
                logger.info("Aggregation query executed for historical transactions.");
            }

            // Process aggregation results.
            if (agg.isEmpty()) {
                logger.info("Aggregation query returned no results. Setting default aggregation values for institution.");
                networkResponse.setMeta("{\"totalValue\": 0, \"totalRecords\": 0, \"page\": " + page + ", \"limit\": " + limit + ", \"successRate\": 0");
            } else {

                Map<String, Object> row = agg.get(0);

                double totalValue = Optional.ofNullable((Number) row.get("totalValue"))
                        .map(Number::doubleValue)
                        .orElse(0.0);
                int totalRecords = Optional.ofNullable((Number) row.get("totalRecords"))
                        .map(Number::intValue)
                        .orElse(0);
                double successRate = Optional.ofNullable((Number) row.get("successRate"))
                        .map(Number::doubleValue)
                        .orElse(0.0);

                String meta = String.format(
                        "{\"totalValue\": %.2f, \"totalRecords\": %d, \"successRate\": %.2f, \"page\": %d, \"limit\": %d}",
                        totalValue, totalRecords, successRate, page, limit);
                networkResponse.setMeta(meta);
                logger.info("Aggregation results processed: " + meta);
            }

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions");
            networkResponse.setData((ArrayList) transactions);
            logger.info("Transaction response composed successfully. Returning response.");

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException occurred while retrieving transactions: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity Get(String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        ZonedDateTime idKey = ZonedDateTime.now();
        String marker = idKey.format(fmt);
        try {
            // Log the entry parameters.
            logger.info("Entering Get transactions method with parameters: startDate=" + startDate
                    + ", endDate=" + endDate + ", page=" + page
                    + ", limit=" + limit + ", isCurrent=" + isCurrent);

            // Calculate pagination offset and log it.
            int offset = page > 1 ? (page - 1) * limit : 0;
            logger.info("Computed offset: " + offset);

            List<FullTransactionModel> transactions;
            List<Map<String, Object>> agg;
            String SQL;

            if (isCurrent) {
                logger.info("Executing query for current transactions from 'tbl_creditfundtransfers'.");
//                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, "
//                        + "a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, "
//                        + "a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
//                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, "
//                        + "a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, "
//                        + "b.name as srcInstitutionName, c.name as destInstitutionName, "
//                        + "a.destination_node "
//                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
//                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
//                        + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? "
//                        + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.destination_institution_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name AS srcInstitutionName, c.name AS destInstitutionName, a.destination_node FROM (SELECT id FROM ajiswitch_db.tbl_creditfundtransfers WHERE transaction_date_time >= ? AND transaction_date_time <= ? ORDER BY transaction_date_time DESC LIMIT ? OFFSET ?) AS sq JOIN ajiswitch_db.tbl_creditfundtransfers a ON a.id = sq.id LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code ORDER BY a.transaction_date_time DESC;";
                logger.info("sql query to fetch current day transactions: " + SQL);
                logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: Get(): tbl_creditfundtransfers request duration: ---> " + durationMs + " ms");
                logger.info("Current transactions query returned " + transactions.size() + " rows.");

                SQL = "SELECT SUM(amount) AS totalValue, COUNT(*) AS totalRecords, AVG(response_code = '00') * 100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfers WHERE transaction_date_time BETWEEN ? AND ?;";
                logger.info("sql query for summary: " + SQL);
                logger.info("Executing current transactions aggregation query with parameters: [startDate, endDate].");
                ZonedDateTime startTimeAgg = ZonedDateTime.now();
                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
                ZonedDateTime endTimeAgg = ZonedDateTime.now();
                long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
                logger.info("\nINFO: " + marker + " :: Get(): agg total duration: ---> " + durationMsAgg + " ms");
                logger.info("Aggregation query executed for current transactions.");
            } else {
                logger.info("Executing query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, "
                        + "a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, "
                        + "a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, "
                        + "a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, "
                        + "b.name as srcInstitutionName, c.name as destInstitutionName, "
                        + "a.destination_node "
                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? "
                        + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("sql query  to fetch older days transactions: " + SQL);
                logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTimeHist = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTimeHist = ZonedDateTime.now();
                long durationMsHist = Duration.between(startTimeHist, endTimeHist).toMillis();
                logger.info("\nINFO: " + marker + " :: Get(): tbl_creditfundtransfer_hist_s request duration: ---> " + durationMsHist + " ms");
                
                logger.info("Historical transactions query returned " + transactions.size() + " rows.");

                SQL = "SELECT SUM(amount) AS totalValue, COUNT(*) AS totalRecords, AVG(response_code = '00') * 100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfer_hist_s WHERE transaction_date_time BETWEEN ? AND ?;";
                logger.info("sql query  to fetch hitorical days summary: " + SQL);
                logger.info("Executing historical transactions aggregation query with parameters: [startDate, endDate].");
                ZonedDateTime startTimeAgg = ZonedDateTime.now();
                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
                ZonedDateTime endTimeAgg = ZonedDateTime.now();
                long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
                logger.info("\nINFO: " + marker + " :: Get(): agg total duration: ---> " + durationMsAgg + " ms");
                logger.info("Aggregation query executed for historical transactions.");
            }

            // Process aggregation results.
            if (agg.isEmpty()) {
                logger.info("Aggregation query returned no results. Setting default aggregation values for institution.");
                networkResponse.setMeta("{\"totalValue\": 0, \"totalRecords\": 0, \"page\": " + page + ", \"limit\": " + limit + ", \"successRate\": 0");
            } else {

                Map<String, Object> row = agg.get(0);

                double totalValue = Optional.ofNullable((Number) row.get("totalValue"))
                        .map(Number::doubleValue)
                        .orElse(0.0);
                int totalRecords = Optional.ofNullable((Number) row.get("totalRecords"))
                        .map(Number::intValue)
                        .orElse(0);
                double successRate = Optional.ofNullable((Number) row.get("successRate"))
                        .map(Number::doubleValue)
                        .orElse(0.0);

                String meta = String.format(
                        "{\"totalValue\": %.2f, \"totalRecords\": %d, \"successRate\": %.2f, \"page\": %d, \"limit\": %d}",
                        totalValue, totalRecords, successRate, page, limit);
                networkResponse.setMeta(meta);
                logger.info("Aggregation results processed: " + meta);
            }

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions");
            networkResponse.setData((ArrayList) transactions);
            logger.info("Transaction response composed successfully. Returning response.");
            ZonedDateTime endTimeTotalExe = ZonedDateTime.now();
                long durationMsTotalExe = Duration.between(idKey, endTimeTotalExe).toMillis();
                logger.info("\nINFO: " + marker + " :: Get(): total method execution duration: ---> " + durationMsTotalExe + " ms");

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException occurred while retrieving transactions: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity getTransactionsByDateOnly(String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            // Log the entry parameters.
            logger.info("Entering getTransactionsByDateOnly transactions method with parameters: startDate=" + startDate
                    + ", endDate=" + endDate + ", page=" + page
                    + ", limit=" + limit + ", isCurrent=" + isCurrent);

            // Calculate pagination offset and log it.
            int offset = page > 1 ? (page - 1) * limit : 0;
            logger.info("Computed offset: " + offset);

            List<FullTransactionModel> transactions;
            List<Map<String, Object>> agg;
            String SQL;

            if (isCurrent) {
                logger.info("Executing query for current transactions from 'tbl_creditfundtransfers'.");
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, "
                        + "a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, "
                        + "a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, "
                        + "a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, "
                        + "b.name as srcInstitutionName, c.name as destInstitutionName, "
                        + "a.destination_node "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? "
                        + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("sql query to fetch current day transactions: " + SQL);
                logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                logger.info("Current transactions query returned " + transactions.size() + " rows.");

                SQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ?;";
                logger.info("sql query for summary: " + SQL);
                logger.info("Executing current transactions aggregation query with parameters: [startDate, endDate].");
                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
                logger.info("Aggregation query executed for current transactions.");
            } else {
                logger.info("Executing query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, "
                        + "a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, "
                        + "a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, "
                        + "a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, "
                        + "b.name as srcInstitutionName, c.name as destInstitutionName, "
                        + "a.destination_node "
                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? "
                        + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("sql query  to fetch older days transactions: " + SQL);
                logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                logger.info("Historical transactions query returned " + transactions.size() + " rows.");

                SQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ?;";
                logger.info("sql query  to fetch hitorical days summary: " + SQL);
                logger.info("Executing historical transactions aggregation query with parameters: [startDate, endDate].");
                agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
                logger.info("Aggregation query executed for historical transactions.");
            }

            // Process aggregation results.
            if (agg.isEmpty()) {
                logger.info("Aggregation query returned no results. Setting default aggregation values for institution.");
                networkResponse.setMeta("{\"totalValue\": 0, \"totalRecords\": 0, \"page\": " + page + ", \"limit\": " + limit + ", \"successRate\": 0");
            } else {

                Map<String, Object> row = agg.get(0);

                double totalValue = Optional.ofNullable((Number) row.get("totalValue"))
                        .map(Number::doubleValue)
                        .orElse(0.0);
                int totalRecords = Optional.ofNullable((Number) row.get("totalRecords"))
                        .map(Number::intValue)
                        .orElse(0);
                double successRate = Optional.ofNullable((Number) row.get("successRate"))
                        .map(Number::doubleValue)
                        .orElse(0.0);

                String meta = String.format(
                        "{\"totalValue\": %.2f, \"totalRecords\": %d, \"successRate\": %.2f, \"page\": %d, \"limit\": %d}",
                        totalValue, totalRecords, successRate, page, limit);
                networkResponse.setMeta(meta);
                logger.info("Aggregation results processed: " + meta);
            }

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions");
            networkResponse.setData((ArrayList) transactions);
            logger.info("Transaction response composed successfully. Returning response.");

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException occurred while retrieving transactions: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity SearchTransactions(
            String session_id,
            String channel_code,
            String response_code,
            String source_institution_code,
            String destination_institution_code,
            String minAmount,
            String maxAmount,
            String originator_account_number,
            String beneficiary_account_number,
            String startDate, // expected format: yyyy-MM-dd
            String endDate, // expected format: yyyy-MM-dd
            int page,
            int limit,
            boolean isCurrent, // legacy parameter (will be overridden by date logic below)
            String userInstitutionCode
    ) {
        NetworkResponse networkResponse = new NetworkResponse();
        ZonedDateTime idKey = ZonedDateTime.now();
        String marker = idKey.format(fmt);
        try {
            logger.info("SearchTransactions called with: session_id=" + session_id
                    + ", channel_code=" + channel_code
                    + ", response_code=" + response_code
                    + ", source_institution_code=" + source_institution_code
                    + ", destination_institution_code=" + destination_institution_code
                    + ", minAmount=" + minAmount
                    + ", maxAmount=" + maxAmount
                    + ", originator_account_number=" + originator_account_number
                    + ", beneficiary_account_number=" + beneficiary_account_number
                    + ", startDate=" + startDate
                    + ", endDate=" + endDate
                    + ", page=" + page
                    + ", limit=" + limit
                    + ", isCurrent=" + isCurrent
                    + ", userInstitutionCode=" + userInstitutionCode);

            // Determine which table(s) to query based on date range.
            // Expecting dates in "yyyy-MM-dd" format.
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime start = LocalDateTime.parse(startDate, formatter);
            LocalDateTime end = LocalDateTime.parse(endDate, formatter);
            LocalDate today = LocalDate.now();

// Use only the date portions
            LocalDate startDay = start.toLocalDate();
            LocalDate endDay = end.toLocalDate();

            boolean includeCurrent = false;
            boolean includeHistory = false;
            if (start != null && end != null) {
                if (!startDay.isBefore(today)) {
                    // startDay is today or in the future
                    includeCurrent = true;
                } else if (endDay.isBefore(today)) {
                    // both start and end are before today (entire range is history)
                    includeHistory = true;
                } else {
                    // range spans a day(s) before today and today or after
                    includeCurrent = true;
                    includeHistory = true;
                }
            } else {
                // Fallback: use isCurrent parameter if dates are not provided
                includeCurrent = isCurrent;
            }
            logger.info("Date range determination: includeCurrent = " + includeCurrent + ", includeHistory = " + includeHistory);

            // Build the dynamic WHERE clause
            final java.util.concurrent.atomic.AtomicBoolean hasCondition = new java.util.concurrent.atomic.AtomicBoolean(false);
            StringBuilder where = new StringBuilder("WHERE ");

            // If userInstitutionCode is not '-1' and both institution codes are empty, set condition accordingly.
            if (!userInstitutionCode.equals("-1")
                    && source_institution_code.isEmpty()
                    && destination_institution_code.isEmpty()) {
                where.append("(a.source_institution_code = '").append(userInstitutionCode)
                        .append("' OR a.destination_institution_code = '").append(userInstitutionCode).append("')");
                hasCondition.set(true);
            }
            appendCondition(where, hasCondition, () -> {
                if (!session_id.isEmpty()) {
                    return "a.session_id = '" + session_id + "'";
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!channel_code.isEmpty()) {
                    return "a.channel_code = '" + channel_code + "'";
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!response_code.isEmpty()) {
                    if (response_code.equals("111")) {
                        return "a.response_code != '00'";
                    } else {
                        return "a.response_code = '" + response_code + "'";
                    }
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!source_institution_code.isEmpty()) {
                    return "a.source_institution_code = '" + source_institution_code + "'";
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!destination_institution_code.isEmpty()) {
                    return "a.destination_institution_code = '" + destination_institution_code + "'";
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!originator_account_number.isEmpty()) {
                    return "a.originator_account_number = '" + originator_account_number + "'";
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!beneficiary_account_number.isEmpty()) {
                    return "a.beneficiary_account_number = '" + beneficiary_account_number + "'";
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!minAmount.isEmpty() && Double.parseDouble(minAmount) > 0) {
                    return "a.amount >= " + minAmount;
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!maxAmount.isEmpty() && Double.parseDouble(maxAmount) > 0) {
                    return "a.amount <= " + maxAmount;
                }
                return null;
            });
            // For the dates, assume the WHERE clause already uses the desired literals.
            appendCondition(where, hasCondition, () -> {
                if (!startDate.isEmpty()) {
                    return "a.transaction_date_time >= '" + startDate + "'";
                }
                return null;
            });
            appendCondition(where, hasCondition, () -> {
                if (!endDate.isEmpty()) {
                    return "a.transaction_date_time < '" + endDate + "'";
                }
                return null;
            });

            String whereQuery = hasCondition.get() ? where.toString() : "";
            logger.info("Generated WHERE clause: " + whereQuery);

            int offset = page > 1 ? (page - 1) * limit : 0;
            String SQL = "";
            List<FullTransactionModel> transactions = null;
            String commonSelect = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, "
                    + "a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, "
                    + "a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, "
                    + "a.beneficiary_bvn, a.destination_institution_code, a.narration, a.transaction_date_time, "
                    + "a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, "
                    + "c.name as destInstitutionName, a.destination_node ";

            // Build queries based on date conditions.
            if (includeCurrent && !includeHistory) {
                // Query only current table.
                logger.info("Querying only current transactions.");
                SQL = commonSelect
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + whereQuery
                        + " ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("Final SQL: " + SQL);
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: SearchTransactions() only primary table : tbl_creditfundtransfers duration: ---> " + durationMs + " ms");
            
            } else if (includeHistory && !includeCurrent) {
                // Query only historical table.
                logger.info("Querying only historical transactions.");
                SQL = commonSelect
                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + whereQuery
                        + " ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("Final SQL: " + SQL);
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: SearchTransactions() only history table : tbl_creditfundtransfer_hist_s duration: ---> " + durationMs + " ms");
            
            } else if (includeCurrent && includeHistory) {
                // Query both tables using UNION ALL.
                logger.info("Querying both current and historical transactions via UNION ALL.");
                String currentSQL = commonSelect
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + whereQuery;
                String historySQL = commonSelect
                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + whereQuery;
                // Wrap in a subquery for ordering, limiting, and pagination.
                SQL = "SELECT * FROM (" + currentSQL + " UNION ALL " + historySQL + ") as combined "
                        + "ORDER BY transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("Final UNION SQL: " + SQL);
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: SearchTransactions(): tbl_creditfundtransfers and tbl_creditfundtransfer_hist_s request duration: ---> " + durationMs + " ms");
            } else {
                // Fallback if no dates are provided: default to current table.
                logger.info("No date range provided; defaulting to current transactions.");
                SQL = commonSelect
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + whereQuery
                        + " ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("Final SQL: " + SQL);
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: SearchTransactions(): tbl_creditfundtransfers request duration: ---> " + durationMs + " ms");
            }

            // Aggregation:
            // For aggregation, if both tables are included, we'll run two separate aggregation queries and sum their results.
            Double totalValue = 0.0;
            int totalRecords = 0;
            Double successRate = 0.0;
            if (includeCurrent && includeHistory) {
                String aggCurrentSQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, AVG(response_code = '00') * 100 AS successRate "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a " + whereQuery;
                String aggHistorySQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, AVG(response_code = '00') * 100 AS successRate "
                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a " + whereQuery;
                logger.info("Executing aggregation on current transactions: " + aggCurrentSQL);
                ZonedDateTime startTimeAgg = ZonedDateTime.now();
                List<Map<String, Object>> aggCurrent = jdbcTemplate.queryForList(aggCurrentSQL);
                ZonedDateTime endTimeAgg = ZonedDateTime.now();
                long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
                logger.info("\nINFO: " + marker + " :: SearchTransactions(): agg total duration: ---> " + durationMsAgg + " ms");
                logger.info("Executing aggregation on historical transactions: " + aggHistorySQL);
                ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                List<Map<String, Object>> aggHistory = jdbcTemplate.queryForList(aggHistorySQL);
                ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                logger.info("\nINFO: " + marker + " :: SearchTransactions(): agg total duration: ---> " + durationMsAggHist + " ms");
                // Sum the aggregates.
                totalValue = sumTotalValue(aggCurrent) + sumTotalValue(aggHistory);
                totalRecords = sumTotalRecords(aggCurrent) + sumTotalRecords(aggHistory);
            } else {
                // Single aggregation query.
                String aggSQL;
                if (includeCurrent) {
                    aggSQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords,"
                            + " (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate  "
                            + "FROM ajiswitch_db.tbl_creditfundtransfers a " + whereQuery;
                } else { // includeHistory must be true
                    aggSQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, "
                            + "(CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate "
                            + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a " + whereQuery;
                }
                logger.info("Executing aggregation query: " + aggSQL);
                ZonedDateTime startTimeAgg = ZonedDateTime.now();
                List<Map<String, Object>> agg = jdbcTemplate.queryForList(aggSQL);
                ZonedDateTime endTimeAgg = ZonedDateTime.now();
                long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
                logger.info("\nINFO: " + marker + " :: SearchTransactions(): agg total duration: ---> " + durationMsAgg + " ms");

                if (agg.isEmpty()) {
                    logger.info("Aggregation query returned no results. Setting default aggregation values for institution.");
                    networkResponse.setMeta("{\"totalValue\": 0, \"totalRecords\": 0, \"page\": " + page + ", \"limit\": " + limit + ", \"successRate\": 0");
                } else {

                    Map<String, Object> row = agg.get(0);

                    totalValue = Optional.ofNullable((Number) row.get("totalValue"))
                            .map(Number::doubleValue)
                            .orElse(0.0);
                    totalRecords = Optional.ofNullable((Number) row.get("totalRecords"))
                            .map(Number::intValue)
                            .orElse(0);
                    successRate = Optional.ofNullable((Number) row.get("successRate"))
                            .map(Number::doubleValue)
                            .orElse(0.0);
                }

            }

            String meta = String.format(
                    "{\"totalValue\": %.2f, \"totalRecords\": %d, \"successRate\": %.2f, \"page\": %d, \"limit\": %d}",
                    totalValue, totalRecords, successRate, page, limit);
            networkResponse.setMeta(meta);
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched transactions");
            networkResponse.setData((ArrayList) transactions);

            logger.info("SearchTransactions completed successfully with " + transactions.size() + " records found");
            ZonedDateTime endTimeTotalExe = ZonedDateTime.now();
                long durationMsTotalExe = Duration.between(idKey, endTimeTotalExe).toMillis();
                logger.info("\nINFO: " + marker + " :: SearchTransactions(): total method execution duration: ---> " + durationMsTotalExe + " ms");
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException occurred in SearchTransactions: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

// Helper method to append conditions.
    private void appendCondition(StringBuilder sb, java.util.concurrent.atomic.AtomicBoolean hasCond, ConditionSupplier conditionSupplier) {
        String condition = conditionSupplier.get();
        if (condition != null && !condition.isEmpty()) {
            if (hasCond.get()) {
                sb.append(" AND ");
            }
            sb.append(condition);
            hasCond.set(true);
        }
    }

    @FunctionalInterface
    interface ConditionSupplier {

        String get();
    }

// Helper methods to sum aggregation results.
    private Double sumTotalValue(List<Map<String, Object>> list) {
        Double sum = 0.0;
        for (Map<String, Object> row : list) {
            BigDecimal value = (BigDecimal) row.get("totalValue");
            if (value != null) {
                sum += value.doubleValue();
            }
        }
        return sum;
    }

    private int sumTotalRecords(List<Map<String, Object>> list) {
        int sum = 0;
        for (Map<String, Object> row : list) {
            Long count = (Long) row.get("totalRecords");
            if (count != null) {
                sum += count.intValue();
            }
        }
        return sum;
    }

    @Override
    public ResponseEntity GetTimeoutRetries(String startDate, String endDate, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> transactions;
            SQL = "SELECT a.*, "
                    + "b.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_timeout_retry a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code "
                    + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? "
                    + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, limit, offset});

            SQL = "SELECT COUNT(a.id) as totalRecords "
                    + "FROM ajiswitch_db.tbl_timeout_retry a "
                    + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? ";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Transactions Timeout Retries");
            networkResponse.setData((ArrayList) transactions);

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity SearchTimeoutRetries(String session_id,
            String response_at_reprocess,
            String destination_institution_code,
            String startDate,
            String endDate,
            int page,
            int limit,
            String isProcessed
    ) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery = "WHERE";

            if (!session_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.session_id = '" + session_id + "'";
            }
            if (!response_at_reprocess.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                if (response_at_reprocess.equals("111")) {
                    whereQuery += " a.response_code != 00";
                } else {
                    whereQuery += " a.response_at_reprocess = " + response_at_reprocess;
                }
            }
            if (!destination_institution_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.destination_institution_code = " + destination_institution_code;
            }
            if (!destination_institution_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.destination_institution_code = " + destination_institution_code;
            }
            if (!isProcessed.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.isProcessed = " + isProcessed;
            }
            if (!startDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.transaction_date_time >= '" + startDate + "'";
            }
            if (!endDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.transaction_date_time < '" + endDate + "'";
            }
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> transactions;
            SQL = "SELECT a.*, "
                    + "b.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_timeout_retry a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.destination_institution_code = b.code " + whereQuery
                    + " ORDER BY a.id DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.queryForList(SQL, new Object[]{limit, offset});

            SQL = "SELECT COUNT(a.id) as totalRecords "
                    + "FROM ajiswitch_db.tbl_timeout_retry a " + whereQuery;
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched timeout retries");
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
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.response_code = '00'";

            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});

            String table_ = isCurrent ? "ajiswitch_db.tbl_name_enquiries" : "ajiswitch_db.tbl_name_enquiries_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
                    + "FROM " + table_ + " a "
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
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.response_code = '00' AND a.source_institution_code = ? ";

            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});

            String table_ = isCurrent ? "ajiswitch_db.tbl_name_enquiries" : "ajiswitch_db.tbl_name_enquiries_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
                    + "FROM " + table_ + " a "
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
                    + "FROM " + table + " a "
                    + "WHERE a.response_code != '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.response_code "
                    + "ORDER BY volume DESC "
                    + "LIMIT 5";

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
                    + "FROM " + table + " a "
                    + "WHERE a.response_code != '00' AND a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ? "
                    + "GROUP BY a.response_code "
                    + "ORDER BY volume DESC "
                    + "LIMIT 5";

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
    public ResponseEntity GetFailedTnxCountByInstitutions(String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, b.shortName as label, a.source_institution_code, b.color "
                    + "FROM " + table + " a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.response_code != '00' AND a.transaction_date_time BETWEEN ? AND ?"
                    + "GROUP BY a.source_institution_code "
                    + "LIMIT 20";

            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});

            networkResponse.setCode(200);
            networkResponse.setMessage("Top failing institutions");
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
    public ResponseEntity GetFailedTnxCountByInstitutions(String institution, String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, b.shortName as label, a.source_institution_code, b.color "
                    + "FROM " + table + " a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "WHERE a.response_code != '00' AND a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ?"
                    + "GROUP BY a.source_institution_code "
                    + "LIMIT 20";

            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institution});

            networkResponse.setCode(200);
            networkResponse.setMessage("Top failing institutions");
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
    public ResponseEntity GetAllResponseCodesTNXInstitution(String institutioncode, String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ? "
                    + "GROUP BY a.response_code";

            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});

            networkResponse.setCode(200);
            networkResponse.setMessage("All Response Codes Summary");
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
    public ResponseEntity GetAllResponseCodesTNXInstitution(String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.response_code";

            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});

            networkResponse.setCode(200);
            networkResponse.setMessage("All Response Codes Summary");
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
                    + "FROM " + table + " a "
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
                    + "FROM " + table + " a "
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
    public ResponseEntity GetTransactionsRates(String startDate, String endDate, boolean inward, String institution) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            if (startDate.contains("T")) {
                startDate = startDate.replace("T", " ");
            }
            if (endDate.contains("T")) {
                endDate = endDate.replace("T", " ");
            }
            String SQL, SQL_;
            String where = "WHERE response_code = '00' AND transaction_date_time BETWEEN ? AND ? ";
            String whereTwo = "WHERE transaction_date_time BETWEEN ? AND ? ";
            if (inward) {
                if (!institution.equals("")) {
                    where += " AND destination_institution_code = ? ";
                }
                if (!institution.equals("")) {
                    whereTwo += " AND destination_institution_code = ? ";
                }
                SQL = "SELECT b.name, b.shortName, b.color, b.code, COALESCE(a.volume, 0) AS volume "
                        + "FROM transgateweb_db.tbl_financial_institutions b "
                        + "LEFT JOIN "
                        + "(SELECT destination_institution_code, COUNT(id) AS volume "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers "
                        + where
                        + "GROUP BY destination_institution_code) a "
                        + "ON b.code = a.destination_institution_code "
                        + "ORDER BY b.name ASC";

//                SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
//                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
//                    + "ON a.destination_institution_code = b.code "
//                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
//                    + "GROUP BY a.destination_institution_code "
//                    + "ORDER BY a.destination_institution_code";
                SQL_ = "SELECT b.name, b.shortName, b.color, b.code, COALESCE(a.volume, 0) AS volume "
                        + "FROM transgateweb_db.tbl_financial_institutions b "
                        + "LEFT JOIN "
                        + "(SELECT destination_institution_code, COUNT(id) AS volume "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers "
                        + whereTwo
                        + "GROUP BY destination_institution_code) a "
                        + "ON b.code = a.destination_institution_code "
                        + "ORDER BY b.name ASC";

//                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
//                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
//                    + "ON a.destination_institution_code = b.code "
//                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
//                    + "GROUP BY a.destination_institution_code "
//                    + "ORDER BY a.destination_institution_code";
            } else {
                if (!institution.equals("")) {
                    where += " AND source_institution_code = ? ";
                }
                if (!institution.equals("")) {
                    whereTwo += " AND source_institution_code = ? ";
                }
                SQL = "SELECT b.name, b.shortName, b.color, b.code, COALESCE(a.volume, 0) AS volume "
                        + "FROM transgateweb_db.tbl_financial_institutions b "
                        + "LEFT JOIN "
                        + "(SELECT source_institution_code, COUNT(id) AS volume "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers "
                        + where
                        + "GROUP BY source_institution_code) a "
                        + "ON b.code = a.source_institution_code "
                        + "ORDER BY b.name ASC";

//                SQL = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
//                    + "FROM transgateweb_db.tbl_financial_institutions b "
//                    + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
//                    + "ON a.source_institution_code = b.code "
//                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
//                    + "GROUP BY a.source_institution_code "
//                    + "ORDER BY a.source_institution_code";
                SQL_ = "SELECT b.name, b.shortName, b.color, b.code, COALESCE(a.volume, 0) AS volume "
                        + "FROM transgateweb_db.tbl_financial_institutions b "
                        + "LEFT JOIN "
                        + "(SELECT source_institution_code, COUNT(id) AS volume "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers "
                        + whereTwo
                        + "GROUP BY source_institution_code) a "
                        + "ON b.code = a.source_institution_code "
                        + "ORDER BY b.name ASC";

//                SQL_ = "SELECT COUNT(a.id) as volume, b.name, b.shortName, b.color, b.code "
//                    + "FROM transgateweb_db.tbl_financial_institutions b "
//                    + "LEFT JOIN ajiswitch_db.tbl_creditfundtransfers a "
//                    + "ON a.source_institution_code = b.code "
//                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
//                    + "GROUP BY a.source_institution_code "
//                    + "ORDER BY a.source_institution_code";
            }
//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.response_code = '00'";
//            int totalSuccessFul = jdbcTemplate.queryForObject(SQL, int.class);
            Object params[] = new Object[]{startDate, endDate};
            if (!institution.equals("")) {
                params = new Object[]{startDate, endDate, institution};
            }
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, params, new TransactionSummaryMapper());

//            SQL = "SELECT COUNT(a.id) as total FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.response_code != '00'";
//            int totalFailures = jdbcTemplate.queryForObject(SQL, int.class);
            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL_, params, new TransactionSummaryMapper());

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
            } else {
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
    public ResponseEntity SearchTransactionsForSessionIds(String sessionids) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL = "SELECT a.session_id, a.originator_account_name, a.originator_account_number, a.originator_kyc, a.beneficiary_account_name, a.beneficiary_account_number, a.beneficiary_kyc, a.name_enquiry_ref, a.txn_duration, a.response_date_time, a.response_code, a.transaction_date_time, a.amount, a.destination_node, "
                    + "b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id IN (" + sessionids + ")";
            List<TransactionHalfModel> transactions = jdbcTemplate.query(SQL, new TransactionHalfMapper());
            SQL = "SELECT a.session_id, a.originator_account_name, a.originator_account_number, a.originator_kyc, a.beneficiary_account_name, a.beneficiary_account_number, a.beneficiary_kyc, a.name_enquiry_ref, a.txn_duration, a.response_date_time, a.response_code, a.transaction_date_time, a.amount, a.destination_node, "
                    + "b.name as srcInstitutionName, c.name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    + "WHERE a.session_id IN (" + sessionids + ")";
            List<TransactionHalfModel> transactions_s = jdbcTemplate.query(SQL, new TransactionHalfMapper());
            logger.info("SearchTransactionsForSessionIds() :: Total transactions from bulk search fetched: " + transactions_s.size());
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions For Uploaded Session IDs");
            transactions.addAll(transactions_s);
            networkResponse.setData((ArrayList) transactions);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("SearchTransactionsForSessionIds() :: Error occured while doing bulk search --> " + ex.getMessage());
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity SearchTransactionsForSessionIds(String sessionids, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        logger.info("Entering SearchTransactionsForSessionIds(sessionids=" + sessionids
                + ", startDate=" + startDate + ", endDate=" + endDate + ")");
        try {
            // Build and log first SQL
//            String SQL = "SELECT a.session_id, a.originator_account_name, a.originator_account_number, "
//                    + "a.originator_kyc, a.beneficiary_account_name, a.beneficiary_account_number, "
//                    + "a.beneficiary_kyc, a.name_enquiry_ref, a.txn_duration, a.response_date_time, "
//                    + "a.response_code, a.transaction_date_time, a.amount, a.destination_node, "
//                    + "b.name as srcInstitutionName, c.name as destInstitutionName "
//                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
//                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
//                    + "WHERE a.session_id IN (" + sessionids.trim().replaceAll("\\s+", "") + ") "
//                    + "AND a.transaction_date_time BETWEEN '" + startDate + "' AND '" + endDate + "'";
            String SQL = "SELECT t.session_id, t.originator_account_name, t.originator_account_number, t.originator_kyc, t.beneficiary_account_name, t.beneficiary_account_number, t.beneficiary_kyc, t.name_enquiry_ref, t.txn_duration, t.response_date_time, t.response_code, t.transaction_date_time, t.amount, t.destination_node, b.name AS srcInstitutionName, c.name AS destInstitutionName FROM (SELECT session_id, originator_account_name, originator_account_number, originator_kyc, beneficiary_account_name, beneficiary_account_number, beneficiary_kyc, name_enquiry_ref, txn_duration, response_date_time, response_code, transaction_date_time, amount, destination_node, source_institution_code, destination_institution_code FROM ajiswitch_db.tbl_creditfundtransfers WHERE session_id IN (" + sessionids.trim().replaceAll("\\s+", "") + ")  AND transaction_date_time >= ? AND transaction_date_time <= ?) AS t LEFT JOIN transgateweb_db.tbl_financial_institutions b ON t.source_institution_code=b.code LEFT JOIN transgateweb_db.tbl_financial_institutions c ON t.destination_institution_code=c.code;";
            logger.info("Executing live transactions query: " + SQL);
            List<TransactionHalfModel> transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionHalfMapper());
            logger.info("Live query returned " + transactions.size() + " rows");

            // If none, query history table
//            if (transactions.isEmpty()) {
                logger.info("No live transactions found, querying history table");
//                SQL = "SELECT a.session_id, a.originator_account_name, a.originator_account_number, "
//                        + "a.originator_kyc, a.beneficiary_account_name, a.beneficiary_account_number, " 
//                        + "a.beneficiary_kyc, a.name_enquiry_ref, a.txn_duration, a.response_date_time, "
//                        + "a.response_code, a.transaction_date_time, a.amount, a.destination_node, "
//                        + "b.name as srcInstitutionName, c.name as destInstitutionName "
//                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
//                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
//                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
//                        + "WHERE a.session_id IN (" + sessionids.trim().replaceAll("\\s+", "") + ") "
//                        + "AND a.transaction_date_time BETWEEN '" + startDate + "' AND '" + endDate + "'";
                SQL = "SELECT t.session_id, t.originator_account_name, t.originator_account_number, t.originator_kyc, t.beneficiary_account_name, t.beneficiary_account_number, t.beneficiary_kyc, t.name_enquiry_ref, t.txn_duration, t.response_date_time, t.response_code, t.transaction_date_time, t.amount, t.destination_node, b.name AS srcInstitutionName, c.name AS destInstitutionName FROM (SELECT session_id, originator_account_name, originator_account_number, originator_kyc, beneficiary_account_name, beneficiary_account_number, beneficiary_kyc, name_enquiry_ref, txn_duration, response_date_time, response_code, transaction_date_time, amount, destination_node, source_institution_code, destination_institution_code FROM ajiswitch_db.tbl_creditfundtransfer_hist_s WHERE session_id  IN (" + sessionids.trim().replaceAll("\\s+", "") + ")) AS t LEFT JOIN transgateweb_db.tbl_financial_institutions b ON t.source_institution_code=b.code LEFT JOIN transgateweb_db.tbl_financial_institutions c ON t.destination_institution_code=c.code;";
                logger.info("Executing history transactions query: " + SQL);
                List<TransactionHalfModel> history = jdbcTemplate.query(SQL, new TransactionHalfMapper());
                logger.info("History query returned " + history.size() + " rows");
                transactions.addAll(history);
                logger.info("Total combined transactions: " + transactions.size());
//            }

            // Build response
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions For Uploaded Session IDs");
            networkResponse.setData((ArrayList) transactions);
            logger.info("SearchTransactionsForSessionIds completed successfully, returning "
                    + transactions.size() + " records");
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info("DataAccessException in SearchTransactionsForSessionIds: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetInsitutionTnxTrend(String institutioncode, String type, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL = "";
            List<Map<String, Object>> trend;
            switch (type) {
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
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.name as srcInstitutionName, c.name as destInstitutionName, "
                    + "a.destination_node "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.source_institution_code = b.code "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions c "
                    + "ON a.destination_institution_code = c.code "
                    //                + "LEFT JOIN ajiswitch_db.tbl_transactions_routes n "
                    //                + "ON a.destination_node = n.port_number "
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
            String meta = "{\"totalValue\": " + totalValue + "}";

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
            } else {
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
                String meta = "{\"totalValue\": " + totalValue + "}";
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
                    + "FROM " + table
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
            } else {
                switch (code) {
                    case "":
                    case "-1":
                        if (status == 0) {
                            SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                                    + "FROM tbl_disputes dispute "
                                    + "LEFT JOIN tbl_financial_institution_contacts a "
                                    + "ON dispute.loggedBy = a.email_address "
                                    + "WHERE dispute.resolved = 0 || (dispute.status = 1 AND dispute.resolved = 1) "
                                    + "ORDER BY dispute.id DESC LIMIT ? OFFSET ?";

                            SQL2 = "SELECT SUM(dispute.amount) as totalValue, COUNT(dispute.id) as totalRecords "
                                    + "FROM tbl_disputes dispute "
                                    + "WHERE dispute.resolved = 0 || (dispute.status = 1 AND dispute.resolved = 1)";
                        } else {
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
                        if (status == 0) {
                            SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                                    + "FROM tbl_disputes dispute "
                                    + "LEFT JOIN tbl_financial_institution_contacts a "
                                    + "ON dispute.loggedBy = a.email_address "
                                    + "WHERE ((dispute.status = 1 AND dispute.resolved = 1) || dispute.resolved = 0) AND (dispute.ownerInstitution = " + code + " OR dispute.destInstitution = " + code + ") "
                                    + "ORDER BY dispute.id DESC LIMIT ? OFFSET ?";

                            SQL2 = "SELECT SUM(dispute.amount) as totalValue, COUNT(dispute.id) as totalRecords "
                                    + "FROM tbl_disputes dispute "
                                    + "WHERE ((dispute.status = 1 AND dispute.resolved = 1) || dispute.resolved = 0) AND (dispute.ownerInstitution = " + code + " OR dispute.destInstitution = " + code + ")";
                        } else {
                            SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                                    + "FROM tbl_disputes dispute "
                                    + "LEFT JOIN tbl_financial_institution_contacts a "
                                    + "ON dispute.loggedBy = a.email_address "
                                    + "WHERE dispute.ownerInstitution = " + code + " OR dispute.destInstitution = " + code + ""
                                    + " ORDER BY dispute.id DESC LIMIT ? OFFSET ?";

                            SQL2 = "SELECT SUM(dispute.amount) as totalValue, COUNT(dispute.id) as totalRecords "
                                    + "FROM tbl_disputes dispute "
                                    + "WHERE dispute.ownerInstitution = " + code + " OR dispute.destInstitution = " + code + "";
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
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + page + ", \"limit\": " + limit + "}";

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            if (status == 0) {
                networkResponse.setMessage(id > 0 ? "Dispute" : "All disputes");
            } else if (status == 1) {
                networkResponse.setMessage(id > 0 ? "Settlement" : "All settlements");
            }
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
                whereQuery += " a.transactionSessionid = '" + sessionid + "'";
            }
            if (!source_bank.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.ownerInstitution = '" + source_bank + "'";
            }
            if (!beneficiary_bank.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.destInstitution = '" + beneficiary_bank + "'";
            }
            switch (dispute_status) {
                case "-1":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                    whereQuery += " a.status = -1 AND a.resolved = 0";
                    break;
                case "0":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                    whereQuery += " a.status = 0 AND a.resolved = 0";
                    break;
                case "1":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                    whereQuery += " a.status = 1 AND a.resolved = 1";
                    break;
                case "2":
                    whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                    whereQuery += " a.status = 0 AND a.resolved = 1";
                    break;
//                case "":
//                    whereQuery = whereQuery.equals("") ? "WHERE" : !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
//                    whereQuery+=" a.status != -1 AND a.resolved != 1";
//                    break;
                default:
                    break;
            }
            if (!start_date_logged.equals("") && !end_date_logged.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.date_created BETWEEN '" + start_date_logged + "' AND '" + end_date_logged + "'";
            }
            if (!start_date_resolved.equals("") && !end_date_resolved.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.date_modified BETWEEN '" + start_date_resolved + "' AND '" + end_date_resolved + "'";
            }
            if (!start_timeline_date.equals("") && !end_timeline_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery + " AND " : whereQuery + "";
                whereQuery += " a.timeline_date BETWEEN '" + start_timeline_date + "' AND '" + end_timeline_date + "'";
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
            String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + ", \"page\": " + 1 + ", \"limit\": " + null + "}";

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
    public ResponseEntity LogDisputesBulk(String sessiontoken, String records, String sourceInstitution, String username) {
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
                            if (retval > 0) {
                                recorded++;
                                if (getTransaction.get(0).getDestInstitutioncode().equals(sourceInstitution)) {
                                    SQL = "UPDATE tbl_disputes SET resolvedBy = ?, status = '0', resolved = '0', date_modified = now() WHERE transactionSessionid = ?";
                                    jdbcTemplate.update(SQL, new Object[]{username, sessionId});
                                }
                            }
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
    public ResponseEntity LogDispute(String sessiontoken, String sessionId, String amount, String wallet, String sourceInstitution, String type, String username) {
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
                if (retval > 0) {
                    if (getTransaction.get(0).getDestInstitutioncode().equals(sourceInstitution)) {
                        SQL = "UPDATE tbl_disputes SET resolvedBy = ?, status = '0', resolved = '0', date_modified = now() WHERE transactionSessionid = ?";
                        jdbcTemplate.update(SQL, new Object[]{username, sessionId});
                    }
                    return responseManager.ResponseAccepted();
                } else {
                    return responseManager.ResponseInternalServerError();
                }
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
//            response.setDestNodeInstitutionName(rs.getString("dest_node_institution_name"));
            if (ColumnExistinRS(rs, "destination_node")) {
                switch (rs.getString("destination_node")) {
                    case "9082":
                        response.setDestNodeInstitutionName("NIP");
                        break;
                    default:
                        response.setDestNodeInstitutionName("HabariPay");
                        break;
                }
            }
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
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, String username, int status, String proof_of_reject_uri, String selectedDisputes, String type) {
        try {
            String SQL;
//            int userrole = GetUserRole(username, sessiontoken);
            int retVal = 0;
//            switch (userrole) {
//                case 1:
//                case 3:
            int resolved = status == 0 ? 0 : 1;
            if (type.equals("bulk")) {
                String[] idS = selectedDisputes.split(",");
                for (String _id : idS) {
                    SQL = "UPDATE tbl_disputes SET resolvedBy = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";
                    int _retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, _id});
                    retVal = retVal + _retVal;
                }
                if (retVal > 0) {
                    return responseManager.ResponseAccepted("Total of " + retVal + " dispute has been accepted");
                } else {
                    return responseManager.ResponseBadRequest();
                }
            } else {
                SQL = "UPDATE tbl_disputes SET resolvedBy = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";
                retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, id});
                if (retVal > 0) {
                    return responseManager.ResponseAccepted();
                } else {
                    return responseManager.ResponseBadRequest();
                }
            }
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
            if (ColumnExistinRS(rs, "destination_node")) {
                switch (rs.getString("destination_node")) {
                    case "9082":
                        response.setDestNodeInstitutionName("NIP");
                        break;
                    default:
                        response.setDestNodeInstitutionName("HabariPay");
                        break;
                }
            }
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
                String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + "}";
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
                String meta = "{\"totalValue\": " + totalValue + ", \"totalRecords\": " + totalRecords + "}";
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

    @Override
    @Transactional
    public ResponseEntity RequestTransactionStatusChange(
            String sessionid,
            String sessiontoken,
            String username,
            String status) {

        NetworkResponse networkResponse = new NetworkResponse();
        logger.info("Entering RequestTransactionStatusChange(sessionid=" + sessionid
                + ", username=" + username + ", status=" + status + ")");

        try {
            int userrole = GetUserRole(username, sessiontoken);
            logger.info("Retrieved user role: " + userrole);

            List<String> sessionIds = Arrays.stream(sessionid.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            logger.info("Session IDs to process: " + sessionIds);

            for (String sid : sessionIds) {
                logger.info("Processing sessionId: " + sid);
                boolean isCurrent = false;

                logger.info(String.format("%s :: Initial isCurrent = %s", sid, isCurrent));

                String SQL = "SELECT a.*, b.name as source_institution_name, c.name as destination_institution_name "
                        + "FROM ajiswitch_db.tbl_creditfundtransfer_hist_s a "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                        + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                        + "WHERE a.session_id = ? AND a.response_code = '09'";
                logger.info(String.format("%s :: SQL Query: %s", sid, SQL));
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, new Object[]{sid});
                logger.info(String.format("%s :: History table rows found:: %s", sid, rows.size()));

                if (rows.isEmpty()) {
                    logger.info(String.format("%s :: No history rows, querying live table instead.", sid));
                    SQL = "SELECT a.*, b.name as source_institution_name, c.name as destination_institution_name "
                            + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                            + "LEFT JOIN transgateweb_db.tbl_financial_institutions b ON a.source_institution_code = b.code "
                            + "LEFT JOIN transgateweb_db.tbl_financial_institutions c ON a.destination_institution_code = c.code "
                            + "WHERE a.session_id = ? AND a.response_code = '09'";
                    logger.info(String.format("%s :: Finding transaction SQL Query: %s", sid, SQL));
                    rows = jdbcTemplate.queryForList(SQL, new Object[]{sid});
                    isCurrent = true;
                    logger.info(String.format("%s :: Live table rows found:  %s, isCurrent = %s", sid, rows.size(), isCurrent));
                }

                if (!rows.isEmpty()) {
                    Map<String, Object> txn = rows.get(0);
                    logger.info(String.format("%s :: Found txn record: %s", sid, txn));

                    // only if no existing status row
                    String checkStatusSql = "SELECT 1 FROM ajiswitch_db.tbl_transactions_status WHERE session_id = ?";
                    List<Map<String, Object>> rows2 = jdbcTemplate.queryForList(checkStatusSql, sid);
                    if (rows2.isEmpty()) {
                        logger.info(String.format("%s :: No existing status row", sid));
                        logger.info(String.format("%s :: Status --> %s", sid, status));
                        int retVal = 0;
                        switch (userrole) {
                            case 1:
                                if (!status.equals("00")) {
                                    logger.info(String.format("%s :: Status is not success, so proceeding to reverse transaction amount into wallet", sid));
                                    // --- NEW: 1) lookup walletnumber for this institution ---
                                    String nodeSql = "SELECT walletnumber, institution_name "
                                            + "FROM ajiswitch_db.tbl_nodes "
                                            + "WHERE institution_code = ? AND is_active = 1";
                                    String sourceInst = txn.get("source_institution_code").toString();
                                    List<Map<String, Object>> nodeRows = jdbcTemplate.queryForList(nodeSql, sourceInst);
                                    if (!nodeRows.isEmpty()) {
                                        int walletUpd = 0;
                                        BigDecimal amount = BigDecimal.ZERO.setScale(2);
                                        String walletNumber = nodeRows.get(0).get("walletnumber").toString();
                                        logger.info(String.format("%s :: Found walletNumber= %s for institution= %s", sid, walletNumber, sourceInst));
                                        if (walletNumber != null && walletNumber.matches("\\d{10}")) {
                                            // --- 2) credit the wallet ---
                                            amount = new BigDecimal(txn.get("amount").toString());
                                            logger.info(String.format("%s :: Transaction amount to be reversed to wallet: %s", sid, amount));
                                            String walletUpdateSql = "UPDATE ajiswitch_db.tbl_wallets "
                                                    + "SET balance = balance + ? "
                                                    + "WHERE walletnumber = ?";
                                            logger.info(String.format("%s :: Reversal Query: %s", sid, walletUpdateSql));
                                            walletUpd = jdbcTemplate.update(walletUpdateSql, amount, walletNumber);
                                            logger.info(String.format("%s :: Wallet update rowsAffected= %s", sid, walletUpd));
                                        }

                                        // --- 3) log wallet activity ---
                                        if (walletUpd > 0) {
                                            String activitySql = "INSERT INTO ajiswitch_db.tbl_wallet_activities "
                                                    + "(walletnumber, amount, credit_or_debit, actor, activity_date_time, session_id) "
                                                    + "VALUES (?, ?, 'CR', 'SYSTEM', now(), ?)";
                                            logger.info(String.format("%s :: Reversal record Query --> %s", sid, activitySql));
                                            int actIns = jdbcTemplate.update(activitySql, walletNumber, amount, sid);
                                            logger.info(String.format("%s :: Wallet activity insert rowsAffected= %s", sid, actIns));
                                        }

                                    } else {
                                        logger.info(String.format("%s :: No active node found for institution= %s; skipping wallet steps", sid, sourceInst));
                                    }
                                } else {
                                    logger.info("Status is success");
                                    logger.info(String.format("%s :: Status is success", sid));
                                }
                                // --- THEN do your existing insert into tbl_transactions_status ---

                                logger.info(String.format("%s :: User role = Admin, inserting with approved_by = requested_by.", sid));
                                SQL = "INSERT INTO ajiswitch_db.tbl_transactions_status "
                                        + "(session_id, requested_by, approved_by, current_status, new_status, approved_at, amount, transaction_date_time, originator_account_name, beneficiary_account_name, source_institution_code, destination_institution_code, source_institution_name, destination_institution_name, status) "
                                        + "VALUES(?, ?, ?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, 1)";
                                logger.info(String.format("%s :: Insert transaction status Query: %s", sid, SQL));
                                retVal = jdbcTemplate.update(SQL, new Object[]{
                                    sid, username, username,
                                    rows.get(0).get("response_code"), status,
                                    rows.get(0).get("amount"), rows.get(0).get("transaction_date_time"),
                                    rows.get(0).get("originator_account_name"), rows.get(0).get("beneficiary_account_name"),
                                    rows.get(0).get("source_institution_code"), rows.get(0).get("destination_institution_code"),
                                    rows.get(0).get("source_institution_name"), rows.get(0).get("destination_institution_name")
                                });
                                logger.info(String.format("%s :: Admin insert returned: %s", sid, retVal));
                                if (retVal > 0) {
                                    logger.info(String.format("%s :: retVal for Insert greater than 0:  %s", sid, retVal));
                                    retVal = 0;
                                    logger.info(String.format("%s :: retVal set back to 0: %s", sid, retVal));
                                    String tnxTable = isCurrent
                                            ? "ajiswitch_db.tbl_creditfundtransfers"
                                            : "ajiswitch_db.tbl_creditfundtransfer_hist_s";
                                    logger.info(String.format("%s :: Updating response_code in table: %s. Status transaction is to be updated to --> %s", sid, tnxTable,status));
                                    SQL = "UPDATE " + tnxTable + " SET response_code = ? WHERE session_id = ?";
                                    logger.info(String.format("%s :: SQL Query to update response code: %s", sid, SQL));
                                    int upd = jdbcTemplate.update(SQL, new Object[]{status, sid});
                                    
                                    logger.info(String.format("%s :: Update on  %s returned: %s", sid, tnxTable, upd));
                                    if (upd > 0) {
                                        String delRetrySql = "DELETE FROM ajiswitch_db.tbl_tsq_retry WHERE session_id = ?";
                                        logger.info(String.format("%s :: Deleting record from TSQ retry table :: Query --> %s", sid, delRetrySql));
                                        int del = jdbcTemplate.update(delRetrySql, sid);
                                        logger.info(String.format("%s :: Deleted from tsq_retry rowsAffected= %s", sid, del));
                                    }

                                }
                                break;
                            default:
                                logger.info("User role = " + userrole + ", inserting without approved_by.");
                                SQL = "INSERT INTO ajiswitch_db.tbl_transactions_status "
                                        + "(session_id, requested_by, current_status, new_status, amount, transaction_date_time, originator_account_name, beneficiary_account_name, source_institution_code, destination_institution_code, source_institution_name, destination_institution_name) "
                                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                                logger.info(String.format("%s :: Insert transaction status Query: %s", sid, SQL));
                                retVal = jdbcTemplate.update(SQL, new Object[]{
                                    sid, username,
                                    rows.get(0).get("response_code"), status,
                                    rows.get(0).get("amount"), rows.get(0).get("transaction_date_time"),
                                    rows.get(0).get("originator_account_name"), rows.get(0).get("beneficiary_account_name"),
                                    rows.get(0).get("source_institution_code"), rows.get(0).get("destination_institution_code"),
                                    rows.get(0).get("source_institution_name"), rows.get(0).get("destination_institution_name")
                                });
                                logger.info(String.format("%s :: Default insert returned: %s", sid, retVal));
                                break;
                        }

                    } else {
                        logger.info("Status row already exists for sessionId=" + sid + "; skipping insert/update.");
                    }
                } else {
                    logger.info("No transaction record found for sessionId=" + sid + "; skipping entirely.");
                }
            }

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(userrole == 1
                    ? "Transaction(s) status updated"
                    : "Transaction(s) status update submitted");
            logger.info("RequestTransactionStatusChange completed successfully: " + networkResponse.getMessage());
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info("DataAccessException in RequestTransactionStatusChange: " + ex.getMessage());
            // Because @Transactional, this will roll back everything we did above
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity UpdateTransactionStatusChange(String sessionid, String sessiontoken, String username, String status) {
        NetworkResponse networkResponse = new NetworkResponse();
        logger.info("Entering UpdateTransactionStatusChange(sessionid=" + sessionid
                + ", username=" + username + ", status=" + status + ")");
        try {
            List<String> sessionIds = new ArrayList<>(Arrays.asList(sessionid.split(",")));
            logger.info("Session IDs to update: " + sessionIds);

            for (String sid : sessionIds) {
                logger.info("Processing sessionId: " + sid);
                int retVal = 0;

                if ("approve".equals(status)) {
                    logger.info("Status = approve; checking existing status record.");
                    String SQL = "SELECT * FROM ajiswitch_db.tbl_transactions_status WHERE session_id = ?";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, new Object[]{sid});
                    logger.info("Status records found: " + rows.size());

                    if (!rows.isEmpty()) {
                        logger.info("Updating response_code in history table.");
                        SQL = "UPDATE ajiswitch_db.tbl_creditfundtransfer_hist_s SET response_code = ? WHERE session_id = ?";
                        retVal = jdbcTemplate.update(SQL, new Object[]{rows.get(0).get("new_status"), sid});
                        logger.info("History table update returned: " + retVal);

                        if (retVal < 1) {
                            logger.info("No rows updated in history; updating live table.");
                            SQL = "UPDATE ajiswitch_db.tbl_creditfundtransfers SET response_code = ? WHERE session_id = ?";
                            retVal = jdbcTemplate.update(SQL, new Object[]{rows.get(0).get("new_status"), sid});
                            logger.info("Live table update returned: " + retVal);
                        }
                    } else {
                        logger.info("No status record found for sessionId " + sid);
                    }
                }

                if (retVal > 0 || "reject".equals(status)) {
                    int _status = "approve".equals(status) ? 1 : 0;
                    logger.info("Recording final approval/rejection in tbl_transactions_status (status=" + _status + ").");
                    String SQL = "UPDATE ajiswitch_db.tbl_transactions_status SET approved_by = ?, approved_at = now(), status = ? WHERE session_id = ?";
                    int upd = jdbcTemplate.update(SQL, new Object[]{username, _status, sid});
                    logger.info("Final status update returned: " + upd);
                } else {
                    logger.info("No update performed for sessionId " + sid + " (retVal=" + retVal + ").");
                }
            }

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Transaction(s) status updated");
            logger.info("UpdateTransactionStatusChange completed: code=200, message=" + networkResponse.getMessage());
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info("DataAccessException in UpdateTransactionStatusChange: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
}
