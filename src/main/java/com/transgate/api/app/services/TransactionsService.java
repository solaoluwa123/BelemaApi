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
import com.transgate.api.models.LiveMonitoringInstitutionModel;
import com.transgate.api.models.LiveMonitoringTimePointModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.TNXModel;
import com.transgate.api.models.TransactionHalfModel;
import com.transgate.api.models.TransactionModel;
import com.transgate.api.models.TransactionSummaryModel;
import com.transgate.api.util.DateUtil;
import com.transgate.api.util.ResponseCodeInterpreter;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.WhereBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("jdbcTemplate")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("secondJdbcTemplate")
    private JdbcTemplate secondJdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    DateUtil dateUtil = new DateUtil();
    private Logger logger = Logger.getLogger(TransactionsService.class.getName());
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");


    private final AppEnvironmentConfig appConfig;

    public TransactionsService(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }

    private static final String TNX_LIVE_TABLE = "ajiswitch_db.tbl_creditfundtransfers";

    private String archiveTable() {
        return appConfig.getTransactionsArchiveTable();
    }

    /**
     * False when archived transactions are read from the live table, in which
     * case a range spanning today must not be queried twice and merged.
     */
    private boolean hasSeparateArchive() {
        return !TNX_LIVE_TABLE.equalsIgnoreCase(archiveTable());
    }

    /**
     * Normalizes common date/time inputs to {@code yyyy-MM-dd'T'HH:mm:ss}.
     * Callers mix date-only ({@code yyyy-MM-dd}), JDBC space form, and ISO.
     */
    private static String toIsoDateTime(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10 && trimmed.charAt(4) == '-' && trimmed.charAt(7) == '-') {
            return trimmed + "T00:00:00";
        }
        if (trimmed.length() >= 19 && trimmed.charAt(10) == ' ') {
            return trimmed.substring(0, 10) + 'T' + trimmed.substring(11, Math.min(19, trimmed.length()));
        }
        if (trimmed.length() == 16 && trimmed.charAt(10) == 'T') {
            return trimmed + ":00";
        }
        return trimmed;
    }

    private int GetUserRole(String username, String session_token) {
        try {
            int role;

            String SQL = "SELECT role FROM tbl_user_details WHERE (email_address = ? OR username = ?) AND deleted = 0 AND session_token = ?";
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
                + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                + "ON a.source_institution_code = b.institution_code "
                + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                + "ON a.destination_institution_code = c.institution_code "
                + "WHERE a.session_id = ? AND a.source_institution_code = ?";

        List<FullTransactionModel> transactions = jdbcTemplate.query(SQL, new Object[]{sessionId, source}, new FullTransactionMapper());
        return transactions;
    }

    public List<FullTransactionModel> GetTransactionFromHistory(String sessionId, String source) {
        logger.info("This uses secondJdbc");
        List<FullTransactionModel> transactions;
        if (!source.equals("-1")) {
            String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.session_id = ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?)";

            transactions = secondJdbcTemplate.query(SQL, new Object[]{sessionId, source, source}, new FullTransactionMapper());
            if (transactions.isEmpty()) {
                // fallback to primary DB
                transactions = jdbcTemplate.query(SQL, new Object[]{sessionId, source, source}, new FullTransactionMapper());
            }
        } else {
            String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.session_id = ?";

            transactions = secondJdbcTemplate.query(SQL, new Object[]{sessionId}, new FullTransactionMapper());
            if (transactions.isEmpty()) {
                // fallback to primary DB
                transactions = jdbcTemplate.query(SQL, new Object[]{sessionId}, new FullTransactionMapper());
            }
        }
        return transactions;
    }

    public List<FullTransactionModel> GetTransactionFromPrimary(String sessionId, String source) {
        List<FullTransactionModel> transactions;
        if (!source.equals("-1")) {
            String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.session_id = ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?)";

            transactions = jdbcTemplate.query(SQL, new Object[]{sessionId, source, source}, new FullTransactionMapper());
        } else {
            String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.session_id = ?";

            transactions = jdbcTemplate.query(SQL, new Object[]{sessionId}, new FullTransactionMapper());
        }
        return transactions;
    }

    public List<FullTransactionModel> GetTransaction(String sessionId, String amount, String source, String responsecode) {
        String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                + "ON a.source_institution_code = b.institution_code "
                + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                + "ON a.destination_institution_code = c.institution_code "
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
//                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
//                        + "a.destination_node "
//                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
//                        + "ON a.source_institution_code = b.institution_code "
//                        + "LEFT JOIN ajiswitch_db.tbl_nodes c "
//                        + "ON a.destination_institution_code = c.institution_code "
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
//                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
//                        + "a.destination_node "
//                        + "FROM " + archiveTable() + " a "
//                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
//                        + "ON a.source_institution_code = b.institution_code "
//                        + "LEFT JOIN ajiswitch_db.tbl_nodes c "
//                        + "ON a.destination_institution_code = c.institution_code "
//                        //                + "LEFT JOIN ajiswitch_db.tbl_transactions_routes n "
//                        //                + "ON a.destination_node = n.port_number "
//                        + "WHERE (a.transaction_date_time >= ? AND a.transaction_date_time <= ?) AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
//                        + "ORDER BY a.id DESC LIMIT ? OFFSET ?";
//                logger.info("sql query: " + SQL);
//                transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
//
//                SQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords "
//                        + "FROM " + archiveTable() + " a "
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

        // Build condition for destination_node when institution_code is 000004
//        String destinationNodeCondition = institutioncode.equals("000004") ? " AND a.destination_node != '9082'" : "";

        if (isCurrent) {
            logger.info("Executing query for current transactions for institution from 'tbl_creditfundtransfers'.");
            SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
                    + "a.destination_node "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.transaction_date_time >= ? AND transaction_date_time <= ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?)" //+ destinationNodeCondition
                    + " ORDER BY transaction_date_time DESC LIMIT ? OFFSET ?";
            logger.info("sql query to fetch current day transactions for institution: " + SQL);
            logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
            transactions = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
            logger.info("Current transactions query returned " + transactions.size() + " rows.");

            SQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?)";// + destinationNodeCondition + ";";
            logger.info("sql query for summary for institution: " + SQL);
            logger.info("Executing current transactions aggregation query with parameters: [startDate, endDate].");
            agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
            logger.info("Aggregation query executed for current transactions.");
        } else {
            logger.info("Executing query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");
            SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
                    + "a.destination_node "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.transaction_date_time >= ? AND transaction_date_time <= ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?)" //+ destinationNodeCondition
                    + " ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
            logger.info("sql query to fetch older days transactions for institution: " + SQL);
            logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
            logger.info("This uses secondJdbc");
            transactions = secondJdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
            logger.info("Historical transactions query returned for institution " + transactions.size() + " rows.");

            SQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM " + archiveTable() + " a WHERE a.transaction_date_time >= ? AND a.transaction_date_time <= ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?)" ;//+ destinationNodeCondition + ";";
            logger.info("sql query to fetch historical days summary for institution: " + SQL);
            logger.info("Executing historical transactions aggregation query with parameters: [startDate, endDate].");
            agg = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
            logger.info("Aggregation query executed for historical transactions.");
        }

        // Process aggregation results.
        if (agg.isEmpty()) {
            logger.info("Aggregation query returned no results. Setting default aggregation values for institution.");
            networkResponse.setMeta("{\"totalValue\": 0, \"totalRecords\": 0, \"page\": " + page + ", \"limit\": " + limit + ", \"successRate\": 0}");
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
            String currentDayQuery = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
                    + "a.destination_node "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.transaction_date_time >= ? AND transaction_date_time < ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
                    + " ORDER BY transaction_date_time DESC LIMIT ? OFFSET ?";

            String olderDaysQuery = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
                    + "a.destination_node "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.transaction_date_time >= ? AND transaction_date_time < ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?) "
                    + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime start = Optional.ofNullable(startDate)
                    .filter(s -> !s.isBlank())
                    .map(s -> LocalDateTime.parse(toIsoDateTime(s), formatter))
                    .orElse(LocalDateTime.now());
            LocalDateTime end = Optional.ofNullable(endDate)
                    .filter(s -> !s.isBlank())
                    .map(s -> LocalDateTime.parse(toIsoDateTime(s), formatter))
                    .orElse(LocalDateTime.now());
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
            if (includeHistory && !hasSeparateArchive()) {
                // Archive reads resolve to the live table, so query it once.
                includeCurrent = true;
                includeHistory = false;
            }
            logger.info("getInstitutionTransactionsByDateOnly() :: Date range determination: includeCurrent = " + includeCurrent + ", includeHistory = " + includeHistory);

            LocalDateTime threshold = LocalDateTime.parse(appConfig.getTippingPoint(), DTF);
            logger.info("Tipping point or threshold date: " + threshold.format(DTF));

            if (includeCurrent && !includeHistory) {
                // Query only current table.
                logger.info("Querying only current transactions.");
                logger.info("Executing query for current transactions from 'tbl_creditfundtransfers'.");

                logger.info("sql query to fetch current day transactions for institution: " + currentDayQuery);
                logger.info("Executing current transactions query with parameters: [startDate, endDate, institutioncode, institutioncode, limit, offset].");
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(currentDayQuery, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): tbl_creditfundtransfers request duration: ---> " + durationMs + " ms");
                logger.info("Current transactions query returned " + transactions.size() + " rows.");
            } else if (includeHistory && !includeCurrent) {

                //To be removed later after we have the script
                if (end.isBefore(threshold)) {
                    // Query only historical table on 10.83.1.14.
                    logger.info("Querying only historical transactions on 10.83.1.14.");
                    logger.info("Executing query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");

                    logger.info("sql query  to fetch older days transactions: " + olderDaysQuery);
                    logger.info("Executing historical transactions query with parameters: [startDate, endDate, institutioncode, institutioncode, limit, offset].");
                    ZonedDateTime startTime = ZonedDateTime.now();
                    transactions = secondJdbcTemplate.query(olderDaysQuery, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                    ZonedDateTime endTime = ZonedDateTime.now();
                    long durationMs = Duration.between(startTime, endTime).toMillis();
                    logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): tbl_creditfundtransfer_hist_s request duration: ---> " + durationMs + " ms");
                    logger.info("Historical transactions query returned from 10.83.1.14 for institution " + transactions.size() + " rows.");

                } else if (!end.isBefore(threshold)) {
                    if (!start.isBefore(threshold)) {
                        // Query only historical table on 10.83.1.13.
                        logger.info("Querying only historical transactions on 10.83.1.13.");
                        logger.info("Executing query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");

                        logger.info("sql query  to fetch older days transactions: " + olderDaysQuery);
                        logger.info("Executing historical transactions query with parameters: [startDate, endDate, institutioncode, institutioncode, limit, offset].");
                        ZonedDateTime startTime = ZonedDateTime.now();
                        transactions = jdbcTemplate.query(olderDaysQuery, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                        ZonedDateTime endTime = ZonedDateTime.now();
                        long durationMs = Duration.between(startTime, endTime).toMillis();
                        logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): tbl_creditfundtransfer_hist_s request duration: ---> " + durationMs + " ms");
                        logger.info("Historical transactions query returned from 10.83.1.13 for institution " + transactions.size() + " rows.");
                    } else {
                        // CASE 3: start < threshold ≤ end  → span both
                        logger.info("Querying only historical transactions on 10.83.1.13 and 10.83.1.14.");
                        logger.info("Executing query for historical transactions from 10.83.1.13 'tbl_creditfundtransfer_hist_s'.");

                        logger.info("sql query  to fetch older days transactions: " + olderDaysQuery);
                        logger.info("Executing historical transactions query with parameters: [startDate, endDate, institutioncode, institutioncode, limit, offset].");

                        logger.info("staying in 10.84.1.13 ...");
                        ZonedDateTime start13 = ZonedDateTime.now();
                        List<FullTransactionModel> list13 = jdbcTemplate.query(olderDaysQuery, new Object[]{threshold.format(DTF), endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                        long durCurr = Duration.between(start13, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + list13.size()
                                + " rows from 10.83.1.13 in " + durCurr + " ms");

                        logger.info("travelling to 10.84.1.14 ...");
                        ZonedDateTime start14 = ZonedDateTime.now();
                        List<FullTransactionModel> list14 = secondJdbcTemplate.query(olderDaysQuery, new Object[]{startDate, threshold.format(DTF), institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                        long durHist = Duration.between(start14, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + list14.size()
                                + " rows from 10.83.1.14 in " + durHist + " ms");

                        // 3) Merge & sort by transaction_date_time descending
                        List<FullTransactionModel> combined = new ArrayList<>(list13);
                        combined.addAll(list14);
                        combined.sort((a, b)
                                -> b.getTransactiondate().compareTo(a.getTransactiondate())
                        );

                        // 4) Apply global pagination
                        int from = Math.min(offset, combined.size());
                        int to = Math.min(offset + limit, combined.size());
                        transactions = new ArrayList<>(combined.subList(from, to));

                        logger.info("After merge/sort/page 10.83.1.13 and 10.83.1.14, returning "
                                + transactions.size() + " rows");

                    }
                } else {
                    transactions = new ArrayList<>();
                    logger.info("No valid date");
                }

            } else if (includeCurrent && includeHistory) {
                logger.info("Querying both current and historical transactions.");

                logger.info("sql query to fetch from primary table : " + currentDayQuery);

                logger.info("sql query to fetch from historical table : " + olderDaysQuery);
                // 2) Fetch from each server
                ZonedDateTime startCurr = ZonedDateTime.now();
                List<FullTransactionModel> currList = jdbcTemplate.query(currentDayQuery, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                long durCurr = Duration.between(startCurr, ZonedDateTime.now()).toMillis();
                logger.info("Fetched " + currList.size()
                        + " current rows in " + durCurr + "ms");
                List<FullTransactionModel> histList = new ArrayList<>();

                if (end.isBefore(threshold)) {
                    // CASE 1: end < threshold  → all secondary
                    ZonedDateTime startHist = ZonedDateTime.now();
                    histList = secondJdbcTemplate.query(olderDaysQuery, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                    long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
                    logger.info("Fetched " + histList.size()
                            + " rows from 10.83.1.14 in " + durHist + "ms");

                } else if (!end.isBefore(threshold)) {
                    // end >= threshold
                    if (!start.isBefore(threshold)) {
                        // CASE 2: start >= threshold  → all primary
                        ZonedDateTime startHist = ZonedDateTime.now();
                        histList = jdbcTemplate.query(olderDaysQuery, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                        long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + histList.size()
                                + " rows from 10.83.1.13 in " + durHist + "ms");

                    } else {
                        // CASE 3: start < threshold ≤ end  → span both
                        logger.info("travelling to 10.83.1.14 ...");
                        ZonedDateTime startHist = ZonedDateTime.now();
                        List<FullTransactionModel> bothList14 = secondJdbcTemplate.query(olderDaysQuery, new Object[]{startDate, threshold.format(DTF), institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                        long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + bothList14.size()
                                + " rows from 10.83.1.14 in " + durHist + " ms");

                        logger.info("coming back to 10.83.1.13 ...");
                        ZonedDateTime startHist2 = ZonedDateTime.now();
                        List<FullTransactionModel> bothList13 = jdbcTemplate.query(olderDaysQuery, new Object[]{threshold.format(DTF), endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                        long durHist2 = Duration.between(startHist2, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + bothList13.size()
                                + " rows from 10.83.1.13 in " + durHist2 + " ms");

                        List<FullTransactionModel> combined13_and_14 = new ArrayList<>(bothList14);
                        combined13_and_14.addAll(bothList13);
                        combined13_and_14.sort((a, b)
                                -> b.getTransactiondate().compareTo(a.getTransactiondate())
                        );

                        // 4) Apply global pagination
                        int from1 = Math.min(offset, combined13_and_14.size());
                        int to1 = Math.min(offset + limit, combined13_and_14.size());
                        histList = new ArrayList<>(combined13_and_14.subList(from1, to1));

                        logger.info("After merge/sort/page of both 10.83.1.13 and 10.83.1.14, returning "
                                + histList.size() + " rows");

                    }
                }

                // 3) Merge & sort by transaction_date_time descending
                List<FullTransactionModel> combined = new ArrayList<>(currList);
                combined.addAll(histList);
                combined.sort((a, b)
                        -> b.getTransactiondate().compareTo(a.getTransactiondate())
                );

                // 4) Apply global pagination
                int from = Math.min(offset, combined.size());
                int to = Math.min(offset + limit, combined.size());
                transactions = new ArrayList<>(combined.subList(from, to));

                logger.info("After merge/sort/page, returning "
                        + transactions.size() + " rows");

            } else {
                // Fallback if no dates are provided: default to current table.
                logger.info("No date range provided; defaulting to current transactions.");
                logger.info("Executing query for current transactions from 'tbl_creditfundtransfers'.");

                logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(currentDayQuery, new Object[]{startDate, endDate, institutioncode, institutioncode, limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly() only primary table : tbl_creditfundtransfers duration: ---> " + durationMs + " ms");
                logger.info("Current transactions query returned " + transactions.size() + " rows.");
            }

            // Aggregation:
//             For aggregation, if both tables are included, we'll run two separate aggregation queries and sum their results.
            Double totalValue = 0.0;
            int totalRecords = 0;
            Double successRate = 0.0;
            List<Map<String, Object>> aggHistory = new ArrayList<>();
            if (includeCurrent && includeHistory) {
                String aggCurrentSQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?);";
                String aggHistorySQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM " + archiveTable() + " a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?);";
                logger.info("Executing aggregation on current transactions: " + aggCurrentSQL);
                ZonedDateTime startTimeAgg = ZonedDateTime.now();
                List<Map<String, Object>> aggCurrent = jdbcTemplate.queryForList(aggCurrentSQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                ZonedDateTime endTimeAgg = ZonedDateTime.now();
                long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
                logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): agg total duration: ---> " + durationMsAgg + " ms");

                if (end.isBefore(threshold)) {
                    // CASE 1: end < threshold  → all secondary
                    logger.info("Travelling to 10.83.1.14 to execute the aggregation on historical transactions: " + aggHistorySQL);
                    ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                    aggHistory = secondJdbcTemplate.queryForList(aggHistorySQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                    ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                    long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                    logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): agg total duration: ---> " + durationMsAggHist + " ms");
                } else if (!end.isBefore(threshold)) {
                    // end >= threshold
                    if (!start.isBefore(threshold)) {
                        // CASE 2: start >= threshold  → all primary
                        logger.info("Staying in 10.83.1.13 to execute the aggregation on historical transactions: " + aggHistorySQL);
                        ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                        aggHistory = jdbcTemplate.queryForList(aggHistorySQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                        ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                        long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                        logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): agg total duration: ---> " + durationMsAggHist + " ms");

                    } else {
                        // CASE 3: start < threshold ≤ end → span both databases
                        logger.info("Spanning both DBs for aggregation:");

                        // Query historicals from secondary DB (startDate up to threshold)
                        logger.info("Travelling to 10.83.1.14 for startDate to threshold: " + aggHistorySQL);
                        List<Map<String, Object>> aggHistorySecondary = secondJdbcTemplate.queryForList(aggHistorySQL, new Object[]{startDate, threshold.format(DTF), institutioncode, institutioncode});

                        // Query historicals from primary DB (threshold up to endDate)
                        logger.info("Staying in 10.83.1.13 for threshold to endDate: " + aggHistorySQL);
                        List<Map<String, Object>> aggHistoryPrimary = jdbcTemplate.queryForList(aggHistorySQL, new Object[]{threshold.format(DTF), endDate, institutioncode, institutioncode});

                        // Extract results (should be at most one row per list)
                        Map<String, Object> sec = aggHistorySecondary.isEmpty() ? new HashMap<>() : aggHistorySecondary.get(0);
                        Map<String, Object> pri = aggHistoryPrimary.isEmpty() ? new HashMap<>() : aggHistoryPrimary.get(0);

                        // Safely extract and sum numeric values
                        double totalSummedValue
                                = (sec.get("totalValue") == null ? 0.0 : ((Number) sec.get("totalValue")).doubleValue())
                                + (pri.get("totalValue") == null ? 0.0 : ((Number) pri.get("totalValue")).doubleValue());
                        int totalSummedRecords
                                = (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue())
                                + (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue());
                        double rateSec = sec.get("successRate") == null ? 0.0 : ((Number) sec.get("successRate")).doubleValue();
                        double ratePri = pri.get("successRate") == null ? 0.0 : ((Number) pri.get("successRate")).doubleValue();
                        int succCntSec = (int) Math.round(rateSec / 100.0 * (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue()));
                        int succCntPri = (int) Math.round(ratePri / 100.0 * (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue()));
                        double successSummedRate = totalSummedRecords == 0 ? 0.0 : ((double) (succCntSec + succCntPri)) * 100.0 / totalSummedRecords;

                        // Prepare the single combined map
                        Map<String, Object> combined = new HashMap<>();
                        combined.put("totalValue", totalSummedValue);
                        combined.put("totalRecords", totalSummedRecords);
                        combined.put("successRate", successSummedRate);

                        // Assign to aggHistory as a single-element list
                        aggHistory = new ArrayList<>();
                        aggHistory.add(combined);

                        logger.info(String.format(
                                "\nINFO: %s :: getInstitutionTransactionsByDateOnly(): Combined aggHistory [totalValue=%.2f, totalRecords=%d, successRate=%.2f%%]",
                                marker, totalSummedValue, totalSummedRecords, successSummedRate
                        ));
                    }

                }
                // Sum the aggregates.
                double curValue = sumTotalValue(aggCurrent);
                double histValue = sumTotalValue(aggHistory);
                int curRecords = sumTotalRecords(aggCurrent);
                int histRecords = sumTotalRecords(aggHistory);

                totalValue = curValue + histValue;
                totalRecords = curRecords + histRecords;

                // extract each successRate (defaults to 0 if missing)
                double curRate = Optional.ofNullable((Number) aggCurrent.get(0).get("successRate"))
                        .map(Number::doubleValue)
                        .orElse(0.0);
                double histRate = Optional.ofNullable((Number) aggHistory.get(0).get("successRate"))
                        .map(Number::doubleValue)
                        .orElse(0.0);

                // compute weighted average
                if (totalRecords > 0) {
                    successRate = (curRate * curRecords + histRate * histRecords) / totalRecords;
                } else {
                    successRate = 0.0;
                }

                logger.info(String.format("totalValue:   %.2f", totalValue));
                logger.info(String.format("totalRecords: %d", totalRecords));
                logger.info(String.format("successRate:  %.2f", successRate));
            } else {
                // Single aggregation query.
                String aggSQL;
                List<Map<String, Object>> agg;
                if (includeCurrent) {
                    aggSQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?);";
                    agg = jdbcTemplate.queryForList(aggSQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                } else { // includeHistory must be true
                    aggSQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM " + archiveTable() + " a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND (a.source_institution_code = ? OR a.destination_institution_code = ?);";
//                    agg = secondJdbcTemplate.queryForList(aggSQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                    if (end.isBefore(threshold)) {
                        // CASE 1: end < threshold  → all secondary
                        logger.info("Travelling to 10.83.1.14 to execute the aggregation on historical transactions: " + aggSQL);
                        ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                        agg = secondJdbcTemplate.queryForList(aggSQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                        ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                        long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                        logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): agg total duration: ---> " + durationMsAggHist + " ms");
                    } else if (!end.isBefore(threshold)) {
                        // end >= threshold
                        if (!start.isBefore(threshold)) {
                            // CASE 2: start >= threshold  → all primary
                            logger.info("Staying in 10.83.1.13 to execute the aggregation on historical transactions: " + aggSQL);
                            ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                            agg = jdbcTemplate.queryForList(aggSQL, new Object[]{startDate, endDate, institutioncode, institutioncode});
                            ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                            long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                            logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): agg total duration: ---> " + durationMsAggHist + " ms");

                        } else {
                            ZonedDateTime startTimeAgg = ZonedDateTime.now();
                            // CASE 3: start < threshold ≤ end → span both databases
                            logger.info("Spanning both DBs for aggregation:");

                            // Query historicals from secondary DB (startDate up to threshold)
                            logger.info("Travelling to 10.83.1.14 for startDate to threshold: " + aggSQL);
                            List<Map<String, Object>> aggHistorySecondary = secondJdbcTemplate.queryForList(
                                    aggSQL, new Object[]{startDate, threshold.format(DTF), institutioncode, institutioncode}
                            );

                            // Query historicals from primary DB (threshold up to endDate)
                            logger.info("Staying in 10.83.1.13 for threshold to endDate: " + aggSQL);
                            List<Map<String, Object>> aggHistoryPrimary = jdbcTemplate.queryForList(
                                    aggSQL, new Object[]{threshold.format(DTF), endDate, institutioncode, institutioncode}
                            );

                            // Extract results (should be at most one row per list)
                            Map<String, Object> sec = aggHistorySecondary.isEmpty() ? new HashMap<>() : aggHistorySecondary.get(0);
                            Map<String, Object> pri = aggHistoryPrimary.isEmpty() ? new HashMap<>() : aggHistoryPrimary.get(0);

                            // Safely extract and sum numeric values
                            double totalSummedValue
                                    = (sec.get("totalValue") == null ? 0.0 : ((Number) sec.get("totalValue")).doubleValue())
                                    + (pri.get("totalValue") == null ? 0.0 : ((Number) pri.get("totalValue")).doubleValue());
                            int totalSummedRecords
                                    = (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue())
                                    + (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue());
                            double rateSec = sec.get("successRate") == null ? 0.0 : ((Number) sec.get("successRate")).doubleValue();
                            double ratePri = pri.get("successRate") == null ? 0.0 : ((Number) pri.get("successRate")).doubleValue();
                            int succCntSec = (int) Math.round(rateSec / 100.0 * (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue()));
                            int succCntPri = (int) Math.round(ratePri / 100.0 * (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue()));
                            double successSummedRate = totalSummedRecords == 0 ? 0.0 : ((double) (succCntSec + succCntPri)) * 100.0 / totalSummedRecords;

                            // Prepare the single combined map
                            Map<String, Object> combined = new HashMap<>();
                            combined.put("totalValue", totalSummedValue);
                            combined.put("totalRecords", totalSummedRecords);
                            combined.put("successRate", successSummedRate);

                            // Assign to aggHistory as a single-element list
                            agg = new ArrayList<>();
                            agg.add(combined);

                            logger.info(String.format(
                                    "\nINFO: %s :: getInstitutionTransactionsByDateOnly(): Combined aggHistory [totalValue=%.2f, totalRecords=%d, successRate=%.2f%%]",
                                    marker, totalSummedValue, totalSummedRecords, successSummedRate
                            ));
                            ZonedDateTime endTimeAgg = ZonedDateTime.now();
                            long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
                            logger.info("\nINFO: " + marker + " :: getInstitutionTransactionsByDateOnly(): Combined agg total duration: ---> " + durationMsAgg + " ms");
                        }

                    } else {
                        agg = new ArrayList<>();
                        logger.info("Something almost impossible happened");
                    }
                }

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
            networkResponse.setMessage("All Transactions");
            networkResponse.setData((ArrayList) transactions);
            logger.info("Transaction response composed successfully. Returning response.");

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException occurred while retrieving transactions: " + ex.getMessage());
            ex.printStackTrace();
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
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.destination_institution_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name AS srcInstitutionName, c.institution_name AS destInstitutionName, a.destination_node FROM (SELECT id FROM ajiswitch_db.tbl_creditfundtransfers ORDER BY transaction_date_time DESC LIMIT ? OFFSET ?) AS sq JOIN ajiswitch_db.tbl_creditfundtransfers a ON a.id = sq.id LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code ORDER BY a.transaction_date_time DESC;";
                logger.info("sql query to fetch current day transactions: " + SQL);
                logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());
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
                        + "b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
                        + "a.destination_node "
                        + "FROM " + archiveTable() + " a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                        + " "
                        + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
                logger.info("sql query  to fetch older days transactions: " + SQL);
                logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTimeHist = ZonedDateTime.now();
                transactions = secondJdbcTemplate.query(SQL, new Object[]{limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTimeHist = ZonedDateTime.now();
                long durationMsHist = Duration.between(startTimeHist, endTimeHist).toMillis();
                logger.info("\nINFO: " + marker + " :: Get(): tbl_creditfundtransfer_hist_s request duration: ---> " + durationMsHist + " ms");

                logger.info("Historical transactions query returned " + transactions.size() + " rows.");

                SQL = "SELECT SUM(amount) AS totalValue, COUNT(*) AS totalRecords, AVG(response_code = '00') * 100 AS successRate FROM " + archiveTable() + " WHERE transaction_date_time BETWEEN ? AND ?;";
                logger.info("sql query  to fetch hitorical days summary: " + SQL);
                logger.info("Executing historical transactions aggregation query with parameters: [startDate, endDate].");
                ZonedDateTime startTimeAgg = ZonedDateTime.now();
                agg = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
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
        ZonedDateTime idKey = ZonedDateTime.now();
        String marker = idKey.format(fmt);
        try {
            // Log the entry parameters.
            logger.info("Entering getTransactionsByDateOnly transactions method with parameters: startDate=" + startDate
                    + ", endDate=" + endDate + ", page=" + page
                    + ", limit=" + limit + ", isCurrent=" + isCurrent);

            // Calculate pagination offset and log it.
            int offset = page > 1 ? (page - 1) * limit : 0;
            logger.info("Computed offset: " + offset);

            List<FullTransactionModel> transactions;
            String currentDayQuery = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, "
                    + "a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, "
                    + "a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, "
                    + "a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, "
                    + "b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
                    + "a.destination_node "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? "
                    + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
            String olderDaysQuery = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, "
                    + "a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, "
                    + "a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, "
                    + "a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, "
                    + "b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
                    + "a.destination_node "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? "
                    + "ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime start = Optional.ofNullable(startDate)
                    .filter(s -> !s.isBlank())
                    .map(s -> LocalDateTime.parse(toIsoDateTime(s), formatter))
                    .orElse(LocalDateTime.now());
            LocalDateTime end = Optional.ofNullable(endDate)
                    .filter(s -> !s.isBlank())
                    .map(s -> LocalDateTime.parse(toIsoDateTime(s), formatter))
                    .orElse(LocalDateTime.now());
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
            if (includeHistory && !hasSeparateArchive()) {
                // Archive reads resolve to the live table, so query it once.
                includeCurrent = true;
                includeHistory = false;
            }
            logger.info("getTransactionsByDateOnly() :: Date range determination: includeCurrent = " + includeCurrent + ", includeHistory = " + includeHistory);

//            LocalDateTime start = LocalDateTime.parse(startDate, DTF);
//            LocalDateTime end = LocalDateTime.parse(endDate, DTF);
            LocalDateTime threshold = LocalDateTime.parse(appConfig.getTippingPoint(), DTF);
            logger.info("Tipping point or threshold date: " + threshold.format(DTF));

            if (includeCurrent && !includeHistory) {
                // Query only current table.
                logger.info("Querying only current transactions.");
                logger.info("Executing query for current transactions from 'tbl_creditfundtransfers'.");

                logger.info("sql query to fetch current day transactions: " + currentDayQuery);
                logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(currentDayQuery, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly() only primary table : tbl_creditfundtransfers duration: ---> " + durationMs + " ms");
                logger.info("Current transactions query returned " + transactions.size() + " rows.");
            } else if (includeHistory && !includeCurrent) {

                //To be removed later after we have the script
                if (end.isBefore(threshold)) {
                    // Query only historical table on 10.83.1.14.
                    logger.info("Querying only historical transactions on 10.83.1.14.");
                    logger.info("Travelling to 10.83.1.14 to execute query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");

                    logger.info("sql query  to fetch older days transactions: " + olderDaysQuery);
                    logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
                    ZonedDateTime startTime = ZonedDateTime.now();
                    transactions = secondJdbcTemplate.query(olderDaysQuery, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                    ZonedDateTime endTime = ZonedDateTime.now();
                    long durationMs = Duration.between(startTime, endTime).toMillis();
                    logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly() only historical table : tbl_creditfundtransfer_hist_s duration: ---> " + durationMs + " ms");
                    logger.info("Historical transactions query from 10.83.1.14 returned " + transactions.size() + " rows.");
                } else if (!end.isBefore(threshold)) {
                    if (!start.isBefore(threshold)) {
                        // Query only historical table on 10.83.1.13.
                        logger.info("Querying only historical transactions on 10.83.1.13.");
                        logger.info("Staying 10.83.1.13 to execute query for historical transactions from 'tbl_creditfundtransfer_hist_s'.");

                        logger.info("sql query  to fetch older days transactions: " + olderDaysQuery);
                        logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
                        ZonedDateTime startTime = ZonedDateTime.now();
                        transactions = jdbcTemplate.query(olderDaysQuery, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                        ZonedDateTime endTime = ZonedDateTime.now();
                        long durationMs = Duration.between(startTime, endTime).toMillis();
                        logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly() only historical table : tbl_creditfundtransfer_hist_s duration: ---> " + durationMs + " ms");
                        logger.info("Historical transactions query returned " + transactions.size() + " rows.");
                    } else {
                        // CASE 3: start < threshold ≤ end  → span both
                        logger.info("Querying only historical transactions on 10.83.1.13 and 10.83.1.14.");
                        logger.info("Executing query for historical transactions from 10.83.1.13 'tbl_creditfundtransfer_hist_s'.");

                        logger.info("sql query  to fetch older days transactions: " + olderDaysQuery);
                        logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");

                        logger.info("staying in 10.84.1.13 ...");
                        ZonedDateTime start13 = ZonedDateTime.now();
                        List<FullTransactionModel> list13 = jdbcTemplate.query(
                                olderDaysQuery, new Object[]{threshold.format(DTF), endDate, limit, offset}, new FullTransactionMapper());
                        long durCurr = Duration.between(start13, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + list13.size()
                                + " rows from 10.83.1.13 in " + durCurr + " ms");

                        logger.info("travelling to 10.84.1.14 ...");
                        ZonedDateTime start14 = ZonedDateTime.now();
                        List<FullTransactionModel> list14 = secondJdbcTemplate.query(
                                olderDaysQuery, new Object[]{startDate, threshold.format(DTF), limit, offset}, new FullTransactionMapper());
                        long durHist = Duration.between(start14, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + list14.size()
                                + " rows from 10.83.1.14 in " + durHist + " ms");

                        // 3) Merge & sort by transaction_date_time descending
                        List<FullTransactionModel> combined = new ArrayList<>(list13);
                        combined.addAll(list14);
                        combined.sort((a, b)
                                -> b.getTransactiondate().compareTo(a.getTransactiondate())
                        );

                        // 4) Apply global pagination
                        int from = Math.min(offset, combined.size());
                        int to = Math.min(offset + limit, combined.size());
                        transactions = new ArrayList<>(combined.subList(from, to));

                        logger.info("After merge/sort/page 10.83.1.13 and 10.83.1.14, returning "
                                + transactions.size() + " rows");

                    }
                } else {
                    transactions = new ArrayList<>();
                    logger.info("No valid date");
                }

            } else if (includeCurrent && includeHistory) {
                logger.info("Querying both current and historical transactions.");

                logger.info("sql query to fetch from primary table : " + currentDayQuery);

                logger.info("sql query to fetch from historical table : " + olderDaysQuery);
                // 2) Fetch from each server
                ZonedDateTime startCurr = ZonedDateTime.now();
                List<FullTransactionModel> currList = jdbcTemplate.query(
                        currentDayQuery, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                long durCurr = Duration.between(startCurr, ZonedDateTime.now()).toMillis();
                logger.info("Fetched " + currList.size()
                        + " current rows in " + durCurr + "ms");
                List<FullTransactionModel> histList = new ArrayList<>();

                if (end.isBefore(threshold)) {
                    // CASE 1: end < threshold  → all history
                    ZonedDateTime startHist = ZonedDateTime.now();
                    histList = secondJdbcTemplate.query(
                            olderDaysQuery, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                    long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
                    logger.info("Fetched " + histList.size()
                            + " rows from 10.83.1.14 in " + durHist + "ms");

                } else if (!end.isBefore(threshold)) {
                    // end >= threshold
                    if (!start.isBefore(threshold)) {
                        // CASE 2: start >= threshold  → all current
                        ZonedDateTime startHist = ZonedDateTime.now();
                        histList = jdbcTemplate.query(
                                olderDaysQuery, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                        long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + histList.size()
                                + " rows from 10.83.1.13 in " + durHist + "ms");

                    } else {
                        // CASE 3: start < threshold ≤ end  → span both
                        logger.info("travelling to 10.83.1.14 ...");
                        ZonedDateTime startHist = ZonedDateTime.now();
                        List<FullTransactionModel> bothList14 = secondJdbcTemplate.query(
                                olderDaysQuery, new Object[]{startDate, threshold.format(DTF), limit, offset}, new FullTransactionMapper());
                        long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + bothList14.size()
                                + " rows from 10.83.1.14 in " + durHist + " ms");

                        logger.info("coming back to 10.83.1.13 ...");
                        ZonedDateTime startHist2 = ZonedDateTime.now();
                        List<FullTransactionModel> bothList13 = jdbcTemplate.query(
                                olderDaysQuery, new Object[]{threshold.format(DTF), endDate, limit, offset}, new FullTransactionMapper());
                        long durHist2 = Duration.between(startHist2, ZonedDateTime.now()).toMillis();
                        logger.info("Fetched " + bothList13.size()
                                + " rows from 10.83.1.13 in " + durHist2 + " ms");

                        List<FullTransactionModel> combined13_and_14 = new ArrayList<>(bothList14);
                        combined13_and_14.addAll(bothList13);
                        combined13_and_14.sort((a, b)
                                -> b.getTransactiondate().compareTo(a.getTransactiondate())
                        );

                        // 4) Apply global pagination
                        int from1 = Math.min(offset, combined13_and_14.size());
                        int to1 = Math.min(offset + limit, combined13_and_14.size());
                        histList = new ArrayList<>(combined13_and_14.subList(from1, to1));

                        logger.info("After merge/sort/page of both 10.83.1.13 and 10.83.1.14, returning "
                                + histList.size() + " rows");

                    }
                }

                // 3) Merge & sort by transaction_date_time descending
                List<FullTransactionModel> combined = new ArrayList<>(currList);
                combined.addAll(histList);
                combined.sort((a, b)
                        -> b.getTransactiondate().compareTo(a.getTransactiondate())
                );

                // 4) Apply global pagination
                int from = Math.min(offset, combined.size());
                int to = Math.min(offset + limit, combined.size());
                transactions = new ArrayList<>(combined.subList(from, to));

                logger.info("After merge/sort/page, returning "
                        + transactions.size() + " rows");

            } else {
                // Fallback if no dates are provided: default to current table.
                logger.info("No date range provided; defaulting to current transactions.");
                logger.info("Executing query for current transactions from 'tbl_creditfundtransfers'.");

                logger.info("sql query to fetch current day transactions: " + currentDayQuery);
                logger.info("Executing current transactions query with parameters: [startDate, endDate, limit, offset].");
                ZonedDateTime startTime = ZonedDateTime.now();
                transactions = jdbcTemplate.query(currentDayQuery, new Object[]{startDate, endDate, limit, offset}, new FullTransactionMapper());
                ZonedDateTime endTime = ZonedDateTime.now();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly() only primary table : tbl_creditfundtransfers duration: ---> " + durationMs + " ms");
                logger.info("Current transactions query returned " + transactions.size() + " rows.");
            }

            // Process aggregation results.
            // Aggregation:
//             For aggregation, if both tables are included, we'll run two separate aggregation queries and sum their results.
            Double totalValue = 0.0;
            int totalRecords = 0;
            Double successRate = 0.0;
            List<Map<String, Object>> aggHistory = new ArrayList<>();
            if (includeCurrent && includeHistory) {
                String aggCurrentSQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, AVG(response_code = '00') * 100 AS successRate "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? ";

                String aggHistorySQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, AVG(response_code = '00') * 100 AS successRate "
                        + "FROM " + archiveTable() + " a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? ";

                logger.info("Executing aggregation on current transactions: " + aggCurrentSQL);
                ZonedDateTime startTimeAgg = ZonedDateTime.now();
                List<Map<String, Object>> aggCurrent = jdbcTemplate.queryForList(aggCurrentSQL, new Object[]{startDate, endDate});
                ZonedDateTime endTimeAgg = ZonedDateTime.now();
                long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
                logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAgg + " ms");

                if (end.isBefore(threshold)) {
                    // CASE 1: end < threshold  → all secondary
                    logger.info("Travelling to 10.83.1.14 to execute the aggregation on historical transactions: " + aggHistorySQL);
                    ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                    aggHistory = secondJdbcTemplate.queryForList(aggHistorySQL, new Object[]{startDate, endDate});
                    ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                    long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                    logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAggHist + " ms");
                } else if (!end.isBefore(threshold)) {
                    // end >= threshold
                    if (!start.isBefore(threshold)) {
                        // CASE 2: start >= threshold  → all primary
                        logger.info("Staying in 10.83.1.13 to execute the aggregation on historical transactions: " + aggHistorySQL);
                        ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                        aggHistory = jdbcTemplate.queryForList(aggHistorySQL, new Object[]{startDate, endDate});
                        ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                        long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                        logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAggHist + " ms");

                    } else {
                        // CASE 3: start < threshold ≤ end → span both databases
                        logger.info("Spanning both DBs for aggregation:");

                        // Query historicals from secondary DB (startDate up to threshold)
                        logger.info("Travelling to 10.83.1.14 for startDate to threshold: " + aggHistorySQL);
                        List<Map<String, Object>> aggHistorySecondary = secondJdbcTemplate.queryForList(
                                aggHistorySQL, new Object[]{startDate, threshold.format(DTF)}
                        );

                        // Query historicals from primary DB (threshold up to endDate)
                        logger.info("Staying in 10.83.1.13 for threshold to endDate: " + aggHistorySQL);
                        List<Map<String, Object>> aggHistoryPrimary = jdbcTemplate.queryForList(
                                aggHistorySQL, new Object[]{threshold.format(DTF), endDate}
                        );

                        // Extract results (should be at most one row per list)
                        Map<String, Object> sec = aggHistorySecondary.isEmpty() ? new HashMap<>() : aggHistorySecondary.get(0);
                        Map<String, Object> pri = aggHistoryPrimary.isEmpty() ? new HashMap<>() : aggHistoryPrimary.get(0);

                        // Safely extract and sum numeric values
                        double totalSummedValue
                                = (sec.get("totalValue") == null ? 0.0 : ((Number) sec.get("totalValue")).doubleValue())
                                + (pri.get("totalValue") == null ? 0.0 : ((Number) pri.get("totalValue")).doubleValue());
                        int totalSummedRecords
                                = (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue())
                                + (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue());
                        double rateSec = sec.get("successRate") == null ? 0.0 : ((Number) sec.get("successRate")).doubleValue();
                        double ratePri = pri.get("successRate") == null ? 0.0 : ((Number) pri.get("successRate")).doubleValue();
                        int succCntSec = (int) Math.round(rateSec / 100.0 * (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue()));
                        int succCntPri = (int) Math.round(ratePri / 100.0 * (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue()));
                        double successSummedRate = totalSummedRecords == 0 ? 0.0 : ((double) (succCntSec + succCntPri)) * 100.0 / totalSummedRecords;

                        // Prepare the single combined map
                        Map<String, Object> combined = new HashMap<>();
                        combined.put("totalValue", totalSummedValue);
                        combined.put("totalRecords", totalSummedRecords);
                        combined.put("successRate", successSummedRate);

                        // Assign to aggHistory as a single-element list
                        aggHistory = new ArrayList<>();
                        aggHistory.add(combined);

                        logger.info(String.format(
                                "\nINFO: %s :: getTransactionsByDateOnly: Combined aggHistory include current and include history [totalValue=%.2f, totalRecords=%d, successRate=%.2f%%]",
                                marker, totalSummedValue, totalSummedRecords, successSummedRate
                        ));
                    }

                }
                // 1) Sum the aggregates
                double curValue = sumTotalValue(aggCurrent);
                logger.info(String.format("curValue (sum of aggCurrent.totalValue)      = %.2f", curValue));

                double histValue = sumTotalValue(aggHistory);
                logger.info(String.format("histValue (sum of aggHistory.totalValue)    = %.2f", histValue));

                // 2) Count the records
                int curRecords = sumTotalRecords(aggCurrent);
                logger.info(String.format("curRecords (count of aggCurrent.totalRecords)= %d", curRecords));

                int histRecords = sumTotalRecords(aggHistory);
                logger.info(String.format("histRecords (count of aggHistory.totalRecords)= %d", histRecords));

                // 3) Combine totals
                totalValue = curValue + histValue;
                logger.info(String.format("totalValue = curValue + histValue             = %.2f", totalValue));

                totalRecords = curRecords + histRecords;
                logger.info(String.format("totalRecords = curRecords + histRecords       = %d", totalRecords));

                // 4) Extract each successRate (defaults to 0.0 if missing)
                double curRate = Optional.ofNullable((Number) aggCurrent.get(0).get("successRate"))
                        .map(Number::doubleValue)
                        .orElse(0.0);
                logger.info(String.format("curRate   (successRate from aggCurrent)        = %.2f%%", curRate));

                double histRate = Optional.ofNullable((Number) aggHistory.get(0).get("successRate"))
                        .map(Number::doubleValue)
                        .orElse(0.0);
                logger.info(String.format("histRate  (successRate from aggHistory)        = %.2f%%", histRate));

                // 5) Compute weighted average successRate
                if (totalRecords > 0) {
                    successRate = (curRate * curRecords + histRate * histRecords) / totalRecords;
                    logger.info(String.format(
                            "successRate ((curRate*curRecords + histRate*histRecords) / totalRecords) = %.2f%%",
                            successRate
                    ));
                } else {
                    successRate = 0.0;
                    logger.info("successRate (no records)                      = 0.00%");
                }

                logger.info(String.format("totalValue:   %.2f", totalValue));
                logger.info(String.format("totalRecords: %d", totalRecords));
                logger.info(String.format("successRate:  %.2f", successRate));
            } else {
                // Single aggregation query.
                String aggSQL;
                List<Map<String, Object>> agg;
                if (includeCurrent) {
                    aggSQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM ajiswitch_db.tbl_creditfundtransfers a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ?;";
                    agg = jdbcTemplate.queryForList(aggSQL, new Object[]{startDate, endDate});
                } else { // includeHistory must be true
                    aggSQL = "SELECT SUM(a.amount) AS totalValue, COUNT(a.id) AS totalRecords, (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate FROM " + archiveTable() + " a WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ?;";
//                    agg = secondJdbcTemplate.queryForList(aggSQL, new Object[]{startDate, endDate});
                    if (end.isBefore(threshold)) {
                        // CASE 1: end < threshold  → all secondary
                        logger.info("Travelling to 10.83.1.14 to execute the aggregation on historical transactions: " + aggSQL);
                        ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                        agg = secondJdbcTemplate.queryForList(aggSQL, new Object[]{startDate, endDate});
                        ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                        long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                        logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAggHist + " ms");
                    } else if (!end.isBefore(threshold)) {
                        // end >= threshold
                        if (!start.isBefore(threshold)) {
                            // CASE 2: start >= threshold  → all primary
                            logger.info("Staying in 10.83.1.13 to execute the aggregation on historical transactions: " + aggSQL);
                            ZonedDateTime startTimeAggHist = ZonedDateTime.now();
                            agg = jdbcTemplate.queryForList(aggSQL, new Object[]{startDate, endDate});
                            ZonedDateTime endTimeAggHist = ZonedDateTime.now();
                            long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
                            logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAggHist + " ms");

                        } else {
                            ZonedDateTime startTimeAgg = ZonedDateTime.now();
                            // CASE 3: start < threshold ≤ end → span both databases
                            logger.info("Spanning both DBs for aggregation:");

                            // Query historicals from secondary DB (startDate up to threshold)
                            logger.info("Travelling to 10.83.1.14 for startDate to threshold: " + aggSQL);
                            List<Map<String, Object>> aggHistorySecondary = secondJdbcTemplate.queryForList(
                                    aggSQL, new Object[]{startDate, threshold.format(DTF)}
                            );

                            // Query historicals from primary DB (threshold up to endDate)
                            logger.info("Staying in 10.83.1.13 for threshold to endDate: " + aggSQL);
                            List<Map<String, Object>> aggHistoryPrimary = jdbcTemplate.queryForList(
                                    aggSQL, new Object[]{threshold.format(DTF), endDate}
                            );

                            // Extract results (should be at most one row per list)
                            Map<String, Object> sec = aggHistorySecondary.isEmpty() ? new HashMap<>() : aggHistorySecondary.get(0);
                            Map<String, Object> pri = aggHistoryPrimary.isEmpty() ? new HashMap<>() : aggHistoryPrimary.get(0);

                            // Safely extract and sum numeric values
                            double totalSummedValue
                                    = (sec.get("totalValue") == null ? 0.0 : ((Number) sec.get("totalValue")).doubleValue())
                                    + (pri.get("totalValue") == null ? 0.0 : ((Number) pri.get("totalValue")).doubleValue());
                            int totalSummedRecords
                                    = (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue())
                                    + (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue());
                            double rateSec = sec.get("successRate") == null ? 0.0 : ((Number) sec.get("successRate")).doubleValue();
                            double ratePri = pri.get("successRate") == null ? 0.0 : ((Number) pri.get("successRate")).doubleValue();
                            int succCntSec = (int) Math.round(rateSec / 100.0 * (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue()));
                            int succCntPri = (int) Math.round(ratePri / 100.0 * (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue()));
                            double successSummedRate = totalSummedRecords == 0 ? 0.0 : ((double) (succCntSec + succCntPri)) * 100.0 / totalSummedRecords;

                            // Prepare the single combined map
                            Map<String, Object> combined = new HashMap<>();
                            combined.put("totalValue", totalSummedValue);
                            combined.put("totalRecords", totalSummedRecords);
                            combined.put("successRate", successSummedRate);

                            // Assign to aggHistory as a single-element list
                            agg = new ArrayList<>();
                            agg.add(combined);

                            logger.info(String.format(
                                    "\nINFO: %s :: getTransactionsByDateOnly: Combined aggHistory [totalValue=%.2f, totalRecords=%d, successRate=%.2f%%]",
                                    marker, totalSummedValue, totalSummedRecords, successSummedRate
                            ));
                            ZonedDateTime endTimeAgg = ZonedDateTime.now();
                            long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
                            logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: Combined agg total duration: ---> " + durationMsAgg + " ms");
                        }

                    } else {
                        agg = new ArrayList<>();
                        logger.info("Something almost impossible happened");
                    }
                }

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
            networkResponse.setMessage("All Transactions");
            networkResponse.setData((ArrayList) transactions);
            logger.info("Transaction response composed successfully. Returning response.");

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException occurred while retrieving transactions: " + ex.getMessage());
            ex.printStackTrace();
            return responseManager.ResponseInternalServerError();
        }
    }

//    @Override
//    public ResponseEntity SearchTransactions(
//            String session_id,
//            String channel_code,
//            String response_code,
//            String source_institution_code,
//            String destination_institution_code,
//            String minAmount,
//            String maxAmount,
//            String originator_account_number,
//            String beneficiary_account_number,
//            String startDate, // expected format: yyyy-MM-dd
//            String endDate, // expected format: yyyy-MM-dd
//            int page,
//            int limit,
//            boolean isCurrent, // legacy parameter (will be overridden by date logic below)
//            String userInstitutionCode
//    ) {
//        NetworkResponse networkResponse = new NetworkResponse();
//        ZonedDateTime idKey = ZonedDateTime.now();
//        String marker = idKey.format(fmt);
//        try {
//            logger.info("SearchTransactions called with: session_id=" + session_id
//                    + ", channel_code=" + channel_code
//                    + ", response_code=" + response_code
//                    + ", source_institution_code=" + source_institution_code
//                    + ", destination_institution_code=" + destination_institution_code
//                    + ", minAmount=" + minAmount
//                    + ", maxAmount=" + maxAmount
//                    + ", originator_account_number=" + originator_account_number
//                    + ", beneficiary_account_number=" + beneficiary_account_number
//                    + ", startDate=" + startDate
//                    + ", endDate=" + endDate
//                    + ", page=" + page
//                    + ", limit=" + limit
//                    + ", isCurrent=" + isCurrent
//                    + ", userInstitutionCode=" + userInstitutionCode);
//
//            // Determine which table(s) to query based on date range.
//            // Expecting dates in "yyyy-MM-dd" format.
//            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//
//            Optional<LocalDateTime> startOpt = Optional.ofNullable(startDate)
//                    .filter(s -> !s.isBlank())
//                    .flatMap(s -> {
//                        try {
//                            return Optional.of(LocalDateTime.parse(s, fmt));
//                        } catch (DateTimeParseException e) {
//                            return Optional.empty();
//                        }
//                    });
//
//            Optional<LocalDateTime> endOpt = Optional.ofNullable(endDate)
//                    .filter(s -> !s.isBlank())
//                    .flatMap(s -> {
//                        try {
//                            return Optional.of(LocalDateTime.parse(s, fmt));
//                        } catch (DateTimeParseException e) {
//                            return Optional.empty();
//                        }
//                    });
//
//            LocalDateTime now = LocalDateTime.now();
//
//            boolean includeCurrent = false;
//            boolean includeHistory = false;
//            LocalDateTime start = null;
//            LocalDateTime end = null;
//
//            if (startOpt.isPresent() && endOpt.isPresent()) {
//                start = startOpt.get();
//                end = endOpt.get();
//
//                if (!start.isBefore(now)) {
//                    // Start is now or in the future (all current)
//                    includeCurrent = true;
//                } else if (end.isBefore(now)) {
//                    // Both start and end are before now (all history)
//                    includeHistory = true;
//                } else {
//                    // Range spans past and present/future (both)
//                    includeCurrent = true;
//                    includeHistory = true;
//                }
//            } else {
//                // Fallback: no valid dates provided → include both
//                LocalDateTime todayStart = LocalDate.now().atStartOfDay(); // today at 00:00:00
//                start = todayStart.minusMonths(12); // 12 months ago, 00:00:00
//                end = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(0); // today at 23:59:59
//
//                includeCurrent = true;
//                includeHistory = true;
//            }
//
//            logger.info("Date range determination: includeCurrent = " + includeCurrent + ", includeHistory = " + includeHistory);
//
//            LocalDateTime threshold = LocalDateTime.parse(appConfig.getTippingPoint(), DTF);
//            logger.info("Threshhold or tipping point: " + threshold.format(DTF));
//            // Build the dynamic WHERE clause
//            final java.util.concurrent.atomic.AtomicBoolean hasCondition = new java.util.concurrent.atomic.AtomicBoolean(false);
//            StringBuilder where = new StringBuilder("WHERE ");
//
//            // If userInstitutionCode is not '-1' and both institution codes are empty, set condition accordingly.
//            if (!userInstitutionCode.equals("-1")
//                    && source_institution_code.isEmpty()
//                    && destination_institution_code.isEmpty()) {
//                where.append("(a.source_institution_code = '").append(userInstitutionCode)
//                        .append("' OR a.destination_institution_code = '").append(userInstitutionCode).append("')");
//                hasCondition.set(true);
//            }
//            appendCondition(where, hasCondition, () -> {
//                if (!session_id.isEmpty()) {
//                    return "a.session_id = '" + session_id + "'";
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!channel_code.isEmpty()) {
//                    return "a.channel_code = '" + channel_code + "'";
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!response_code.isEmpty()) {
//                    if (response_code.equals("111")) {
//                        return "a.response_code != '00'";
//                    } else {
//                        return "a.response_code = '" + response_code + "'";
//                    }
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!source_institution_code.isEmpty()) {
//                    return "a.source_institution_code = '" + source_institution_code + "'";
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!destination_institution_code.isEmpty()) {
//                    return "a.destination_institution_code = '" + destination_institution_code + "'";
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!originator_account_number.isEmpty()) {
//                    return "a.originator_account_number = '" + originator_account_number + "'";
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!beneficiary_account_number.isEmpty()) {
//                    return "a.beneficiary_account_number = '" + beneficiary_account_number + "'";
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!minAmount.isEmpty() && Double.parseDouble(minAmount) > 0) {
//                    return "a.amount >= " + minAmount;
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!maxAmount.isEmpty() && Double.parseDouble(maxAmount) > 0) {
//                    return "a.amount <= " + maxAmount;
//                }
//                return null;
//            });
//            // For the dates, assume the WHERE clause already uses the desired literals.
//            appendCondition(where, hasCondition, () -> {
//                if (!startDate.isEmpty()) {
//                    return "a.transaction_date_time >= '" + startDate + "'";
//                }
//                return null;
//            });
//            appendCondition(where, hasCondition, () -> {
//                if (!endDate.isEmpty()) {
//                    return "a.transaction_date_time < '" + endDate + "'";
//                }
//                return null;
//            });
//
//            String whereQuery = hasCondition.get() ? where.toString() : "";
//            logger.info("Generated WHERE clause: " + whereQuery);
//
//            int offset = page > 1 ? (page - 1) * limit : 0;
////            String SQL = "";
//
//            List<FullTransactionModel> transactions = null;
//            List<FullTransactionModel> histList = null;
//            String commonSelect = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, "
//                    + "a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, "
//                    + "a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, "
//                    + "a.beneficiary_bvn, a.destination_institution_code, a.narration, a.transaction_date_time, "
//                    + "a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, "
//                    + "c.institution_name as destInstitutionName, a.destination_node ";
//            String currentDayQuery = commonSelect
//                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                    + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
//                    + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
//                    + whereQuery
//                    + " ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
//            String olderDaysQuery = commonSelect
//                    + "FROM " + archiveTable() + " a "
//                    + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
//                    + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
//                    + whereQuery
//                    + " ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
//
//            // Build queries based on date conditions.
//            if (includeCurrent && !includeHistory) {
//                // Query only current table.
//                logger.info("Querying only current transactions.");
//
//                logger.info("Final SQL: " + currentDayQuery);
//                ZonedDateTime startTime = ZonedDateTime.now();
//                transactions = jdbcTemplate.query(currentDayQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                ZonedDateTime endTime = ZonedDateTime.now();
//                long durationMs = Duration.between(startTime, endTime).toMillis();
//                logger.info("\nINFO: " + marker + " :: SearchTransactions() only primary table : tbl_creditfundtransfers duration: ---> " + durationMs + " ms");
//
//            } else if (includeHistory && !includeCurrent) {
//
//                if (start != null && end != null) {
//                    if (end.isBefore(threshold)) {
//                        // Query only historical table on 10.83.1.14.
//                        logger.info("Travelling to 10.83.1.14 to execute query for historical transactions.");
//
//                        logger.info("Final SQL: " + olderDaysQuery);
//                        ZonedDateTime startTime = ZonedDateTime.now();
//                        transactions = secondJdbcTemplate.query(olderDaysQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                        ZonedDateTime endTime = ZonedDateTime.now();
//                        long durationMs = Duration.between(startTime, endTime).toMillis();
//                        logger.info("\nINFO: " + marker + " :: SearchTransactions() only history table on 10.83.1.14 : tbl_creditfundtransfer_hist_s duration: ---> " + durationMs + " ms");
//
//                    } else if (!end.isBefore(threshold)) {
//                        if (!start.isBefore(threshold)) {
//                            // Query only historical table on 10.83.1.13.
//                            logger.info("Staying in 10.83.1.13 to execute query for historical transactions.");
//
//                            logger.info("Final SQL: " + olderDaysQuery);
//                            ZonedDateTime startTime = ZonedDateTime.now();
//                            transactions = jdbcTemplate.query(olderDaysQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                            ZonedDateTime endTime = ZonedDateTime.now();
//                            long durationMs = Duration.between(startTime, endTime).toMillis();
//                            logger.info("\nINFO: " + marker + " :: SearchTransactions() only history table on 10.83.1.13 : tbl_creditfundtransfer_hist_s duration: ---> " + durationMs + " ms");
//
//                        } else {
//                            // CASE 3: start < threshold ≤ end  → span both
//                            logger.info("Querying only historical transactions on 10.83.1.13 and 10.83.1.14.");
//                            logger.info("Executing query for historical transactions from 10.83.1.13 'tbl_creditfundtransfer_hist_s'.");
//
//                            logger.info("sql query  to fetch older days transactions: " + olderDaysQuery);
//                            logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
//
//                            logger.info("staying in 10.84.1.13 ...");
//                            ZonedDateTime start13 = ZonedDateTime.now();
//                            List<FullTransactionModel> list13 = jdbcTemplate.query(olderDaysQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                            long durCurr = Duration.between(start13, ZonedDateTime.now()).toMillis();
//                            logger.info("Fetched " + list13.size()
//                                    + " rows from 10.83.1.13 in " + durCurr + " ms");
//
//                            logger.info("travelling to 10.84.1.14 ...");
//                            ZonedDateTime start14 = ZonedDateTime.now();
//                            List<FullTransactionModel> list14 = secondJdbcTemplate.query(olderDaysQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                            long durHist = Duration.between(start14, ZonedDateTime.now()).toMillis();
//                            logger.info("Fetched " + list14.size()
//                                    + " rows from 10.83.1.14 in " + durHist + " ms");
//
//                            // 3) Merge & sort by transaction_date_time descending
//                            List<FullTransactionModel> combined = new ArrayList<>(list13);
//                            combined.addAll(list14);
//                            combined.sort((a, b)
//                                    -> b.getTransactiondate().compareTo(a.getTransactiondate())
//                            );
//
//                            // 4) Apply global pagination
//                            int from = Math.min(offset, combined.size());
//                            int to = Math.min(offset + limit, combined.size());
//                            transactions = new ArrayList<>(combined.subList(from, to));
//
//                            logger.info("After merge/sort/page 10.83.1.13 and 10.83.1.14, returning "
//                                    + transactions.size() + " rows");
//
//                        }
//                    } else {
//                        transactions = new ArrayList<>();
//                        logger.info("No valid date");
//                    }
//                } else {
//                    // Fallback handling if start or end is null
//                    logger.info(String.format("Invalid or missing start/end datetime: start=%s, end=%s", start, end));
//                    // For example: decide to include both, throw, or use defaults
//                    // includeCurrent = true; includeHistory = true;
//                }
//
//                // Query only historical table.
//            } else if (includeCurrent && includeHistory) {
//                logger.info("Querying both current and historical transactions.");
//
//                // 1) Build the two SQLs (no UNION)
//                logger.info("sql query to fetch from primary table : " + currentDayQuery);
//                logger.info("sql query to fetch from history table : " + olderDaysQuery);
//
//                // 2) Fetch from each server
//                ZonedDateTime startCurr = ZonedDateTime.now();
//                List<FullTransactionModel> currList = jdbcTemplate.query(
//                        currentDayQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                long durCurr = Duration.between(startCurr, ZonedDateTime.now()).toMillis();
//                logger.info("Fetched " + currList.size()
//                        + " current rows in " + durCurr + "ms");
//
//                if (start != null && end != null) {
//                    if (end.isBefore(threshold)) {
//                        // Query only historical table on 10.83.1.14.
//                        logger.info("Travelling to 10.83.1.14 to execute query for historical transactions.");
//                        logger.info("Final SQL: " + olderDaysQuery);
//                        ZonedDateTime startHist = ZonedDateTime.now();
//                        histList = secondJdbcTemplate.query(
//                                olderDaysQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                        long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
//                        logger.info("Fetched " + histList.size()
//                                + " history rows in " + durHist + "ms");
//                        logger.info("\nINFO: " + marker + " :: SearchTransactions() only history table on 10.83.1.14 : tbl_creditfundtransfer_hist_s duration: ---> " + durHist + " ms");
//
//                    } else if (!end.isBefore(threshold)) {
//                        if (!start.isBefore(threshold)) {
//                            // Query only historical table on 10.83.1.13.
//                            logger.info("Staying in 10.83.1.13 to execute query for historical transactions.");
//                            ZonedDateTime startHist = ZonedDateTime.now();
//                            histList = jdbcTemplate.query(
//                                    olderDaysQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                            long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
//                            logger.info("Fetched " + histList.size()
//                                    + " history rows in " + durHist + "ms");
//                            logger.info("Final SQL: " + olderDaysQuery);
//                            logger.info("\nINFO: " + marker + " :: SearchTransactions() only history table on 10.83.1.13 : tbl_creditfundtransfer_hist_s duration: ---> " + durHist + " ms");
//
//                        } else {
//                            // CASE 3: start < threshold ≤ end  → span both
//                            logger.info("Querying only historical transactions on 10.83.1.13 and 10.83.1.14.");
//                            logger.info("Executing query for historical transactions from 10.83.1.13 'tbl_creditfundtransfer_hist_s'.");
//
//                            logger.info("sql query  to fetch older days transactions: " + olderDaysQuery);
//                            logger.info("Executing historical transactions query with parameters: [startDate, endDate, limit, offset].");
//
//                            logger.info("staying in 10.84.1.13 ...");
//                            ZonedDateTime startHist = ZonedDateTime.now();
//                            List<FullTransactionModel> list13 = jdbcTemplate.query(
//                                    olderDaysQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                            long durHist = Duration.between(startHist, ZonedDateTime.now()).toMillis();
//                            logger.info("Fetched " + list13.size()
//                                    + " history rows in " + durHist + "ms");
//
//                            logger.info("travelling to 10.84.1.14 ...");
//                            ZonedDateTime startHist14 = ZonedDateTime.now();
//                            List<FullTransactionModel> list14 = secondJdbcTemplate.query(
//                                    olderDaysQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                            long durHist14 = Duration.between(startHist14, ZonedDateTime.now()).toMillis();
//                            logger.info("Fetched " + list14.size()
//                                    + " history rows in " + durHist14 + "ms");
//
//                            // 3) Merge & sort by transaction_date_time descending
//                            List<FullTransactionModel> combined = new ArrayList<>(list13);
//                            combined.addAll(list14);
//                            combined.sort((a, b)
//                                    -> b.getTransactiondate().compareTo(a.getTransactiondate())
//                            );
//
//                            // 4) Apply global pagination
//                            int from = Math.min(offset, combined.size());
//                            int to = Math.min(offset + limit, combined.size());
//                            histList = new ArrayList<>(combined.subList(from, to));
//
//                            logger.info("After merge/sort/page 10.83.1.13 and 10.83.1.14, returning "
//                                    + histList.size() + " rows");
//
//                        }
//                    } else {
//                        histList = new ArrayList<>();
//                        logger.info("An almost impossible situation happened. No valid date");
//                    }
//                } else {
//                    logger.info(String.format("Invalid or missing start/end datetime: start=%s, end=%s", start, end));
//                }
//
//                //before you combine curr and history together
//                // 3) Merge & sort by transaction_date_time descending
//                List<FullTransactionModel> combined = new ArrayList<>(currList);
//                combined.addAll(histList);
//                combined.sort((a, b)
//                        -> b.getTransactiondate().compareTo(a.getTransactiondate())
//                );
//                logger.info("Before pagination, combined size: " + combined.size());
//
//                // 4) Apply global pagination
//                int from = Math.min(offset, combined.size());
//                int to = Math.min(offset + limit, combined.size());
//                logger.info(String.format("Pagination: from=%d, to=%d, limit=%d", from, to, limit));
//
//                transactions = new ArrayList<>(combined.subList(from, to));
//
//                logger.info("After merge/sort/page, returning "
//                        + transactions.size() + " rows");
//            } else {
//                // Fallback if no dates are provided: default to current table.
//                logger.info("No date range provided; defaulting to current transactions.");
//
//                logger.info("Final SQL: " + currentDayQuery);
//                ZonedDateTime startTime = ZonedDateTime.now();
//                transactions = jdbcTemplate.query(currentDayQuery, new Object[]{limit, offset}, new FullTransactionMapper());
//                ZonedDateTime endTime = ZonedDateTime.now();
//                long durationMs = Duration.between(startTime, endTime).toMillis();
//                logger.info("\nINFO: " + marker + " :: SearchTransactions(): tbl_creditfundtransfers request duration: ---> " + durationMs + " ms");
//            }
//
//            // Aggregation:
//            // For aggregation, if both tables are included, we'll run two separate aggregation queries and sum their results.
//            Double totalValue = 0.0;
//            int totalRecords = 0;
//            Double successRate = 0.0;
//            if (includeCurrent && includeHistory) {
//                String aggCurrentSQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, AVG(response_code = '00') * 100 AS successRate "
//                        + "FROM ajiswitch_db.tbl_creditfundtransfers a " + whereQuery;
//
//                String aggHistorySQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, AVG(response_code = '00') * 100 AS successRate "
//                        + "FROM " + archiveTable() + " a " + whereQuery;
//
//                List<Map<String, Object>> aggHistory = null;
//
//                logger.info("Executing aggregation on current transactions: " + aggCurrentSQL);
//                ZonedDateTime startTimeAgg = ZonedDateTime.now();
//                List<Map<String, Object>> aggCurrent = jdbcTemplate.queryForList(aggCurrentSQL);
//                ZonedDateTime endTimeAgg = ZonedDateTime.now();
//                long durationMsAgg = Duration.between(startTimeAgg, endTimeAgg).toMillis();
//                logger.info("\nINFO: " + marker + " :: SearchTransactions(): agg total duration: ---> " + durationMsAgg + " ms");
//
//                if (start != null && end != null) {
//                    if (end.isBefore(threshold)) {
//                        logger.info("Travelling to 10.83.1.14 to execute the aggregation on historical transactions: " + aggHistorySQL);
//                        ZonedDateTime startTimeAggHist = ZonedDateTime.now();
//                        aggHistory = secondJdbcTemplate.queryForList(aggHistorySQL);
//                        ZonedDateTime endTimeAggHist = ZonedDateTime.now();
//                        long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
//                        logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAggHist + " ms");
//                    } else if (!end.isBefore(threshold)) {
//                        // end >= threshold
//                        if (!start.isBefore(threshold)) {
//                            // CASE 2: start >= threshold  → all primary
//                            logger.info("Staying in 10.83.1.13 to execute the aggregation on historical transactions: " + aggHistorySQL);
//                            ZonedDateTime startTimeAggHist = ZonedDateTime.now();
//                            aggHistory = jdbcTemplate.queryForList(aggHistorySQL);
//                            ZonedDateTime endTimeAggHist = ZonedDateTime.now();
//                            long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
//                            logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAggHist + " ms");
//
//                        } else {
//                            // CASE 3: start < threshold ≤ end → span both databases
//                            logger.info("Spanning both DBs for aggregation:");
//
//                            // Query historicals from secondary DB (startDate up to threshold)
//                            logger.info("Travelling to 10.83.1.14 for startDate to threshold: " + aggHistorySQL);
//                            List<Map<String, Object>> aggHistorySecondary = secondJdbcTemplate.queryForList(aggHistorySQL);
//
//                            // Query historicals from primary DB (threshold up to endDate)
//                            logger.info("Staying in 10.83.1.13 for threshold to endDate: " + aggHistorySQL);
//                            List<Map<String, Object>> aggHistoryPrimary = jdbcTemplate.queryForList(aggHistorySQL);
//
//                            // Extract results (should be at most one row per list)
//                            Map<String, Object> sec = aggHistorySecondary.isEmpty() ? new HashMap<>() : aggHistorySecondary.get(0);
//                            Map<String, Object> pri = aggHistoryPrimary.isEmpty() ? new HashMap<>() : aggHistoryPrimary.get(0);
//
//                            // Safely extract and sum numeric values
//                            double totalSummedValue
//                                    = (sec.get("totalValue") == null ? 0.0 : ((Number) sec.get("totalValue")).doubleValue())
//                                    + (pri.get("totalValue") == null ? 0.0 : ((Number) pri.get("totalValue")).doubleValue());
//                            int totalSummedRecords
//                                    = (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue())
//                                    + (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue());
//                            double rateSec = sec.get("successRate") == null ? 0.0 : ((Number) sec.get("successRate")).doubleValue();
//                            double ratePri = pri.get("successRate") == null ? 0.0 : ((Number) pri.get("successRate")).doubleValue();
//                            int succCntSec = (int) Math.round(rateSec / 100.0 * (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue()));
//                            int succCntPri = (int) Math.round(ratePri / 100.0 * (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue()));
//                            double successSummedRate = totalSummedRecords == 0 ? 0.0 : ((double) (succCntSec + succCntPri)) * 100.0 / totalSummedRecords;
//
//                            // Prepare the single combined map
//                            Map<String, Object> combined = new HashMap<>();
//                            combined.put("totalValue", totalSummedValue);
//                            combined.put("totalRecords", totalSummedRecords);
//                            combined.put("successRate", successSummedRate);
//
//                            // Assign to aggHistory as a single-element list
//                            aggHistory = new ArrayList<>();
//                            aggHistory.add(combined);
//
//                            logger.info(String.format(
//                                    "\nINFO: %s :: SearchTransactions() : Combined aggHistory [totalValue=%.2f, totalRecords=%d, successRate=%.2f%%]",
//                                    marker, totalSummedValue, totalSummedRecords, successSummedRate
//                            ));
//                        }
//                    }
//                } else {
//                    // Fallback handling if start or end is null
//                    logger.info(String.format("Invalid or missing start/end datetime: start=%s, end=%s", start, end));
//
//                }
//
//                // Sum the aggregates.
//                // 1) Sum values and log
//                double curValue = sumTotalValue(aggCurrent);
//                logger.info(String.format("curValue (sum of aggCurrent.totalValue)       = %.2f", curValue));
//
//                double histValue = sumTotalValue(aggHistory);
//                logger.info(String.format("histValue (sum of aggHistory.totalValue)     = %.2f", histValue));
//
//                // 2) Sum records and log
//                int curRecords = sumTotalRecords(aggCurrent);
//                logger.info(String.format("curRecords (sum of aggCurrent.totalRecords)  = %d", curRecords));
//
//                int histRecords = sumTotalRecords(aggHistory);
//                logger.info(String.format("histRecords (sum of aggHistory.totalRecords)= %d", histRecords));
//
//                // 3) Combine totals and log
//                totalValue = curValue + histValue;
//                logger.info(String.format("totalValue (curValue + histValue)            = %.2f", totalValue));
//
//                totalRecords = curRecords + histRecords;
//                logger.info(String.format("totalRecords (curRecords + histRecords)      = %d", totalRecords));
//
//                // 4) Extract and log each successRate
//                double curRate = Optional.ofNullable((Number) aggCurrent.get(0).get("successRate"))
//                        .map(Number::doubleValue)
//                        .orElse(0.0);
//                logger.info(String.format("curRate   (successRate from aggCurrent)       = %.2f%%", curRate));
//
//                double histRate = Optional.ofNullable((Number) aggHistory.get(0).get("successRate"))
//                        .map(Number::doubleValue)
//                        .orElse(0.0);
//                logger.info(String.format("histRate  (successRate from aggHistory)       = %.2f%%", histRate));
//
//                // 5) Compute weighted average and log
//                if (totalRecords > 0) {
//                    successRate = (curRate * curRecords + histRate * histRecords) / totalRecords;
//                    logger.info(String.format(
//                            "successRate ((curRate*curRecords + histRate*histRecords) / totalRecords) = %.2f%%",
//                            successRate
//                    ));
//                } else {
//                    successRate = 0.0;
//                    logger.info("successRate (no records, defaulted to)        = 0.00%");
//                }
//
//                logger.info(String.format("totalValue:   %.2f", totalValue));
//                logger.info(String.format("totalRecords: %d", totalRecords));
//                logger.info(String.format("successRate:  %.2f", successRate));
//            } else {
//                // Single aggregation query.
//                String aggSQL;
//                List<Map<String, Object>> agg = null;
//                if (includeCurrent) {
//                    aggSQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords,"
//                            + " (CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate  "
//                            + "FROM ajiswitch_db.tbl_creditfundtransfers a " + whereQuery;
//                    agg = jdbcTemplate.queryForList(aggSQL);
//                } else { // includeHistory must be true
//                    aggSQL = "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, "
//                            + "(CAST(SUM(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) AS DECIMAL(10,2))/COUNT(a.id))*100 AS successRate "
//                            + "FROM " + archiveTable() + " a " + whereQuery;
//
//                    if (start != null && end != null) {
//                        if (end.isBefore(threshold)) {
//                            logger.info("Travelling to 10.83.1.14 to execute the aggregation on historical transactions: " + aggSQL);
//                            ZonedDateTime startTimeAggHist = ZonedDateTime.now();
//                            agg = secondJdbcTemplate.queryForList(aggSQL);
//                            ZonedDateTime endTimeAggHist = ZonedDateTime.now();
//                            long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
//                            logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAggHist + " ms");
//                        } else if (!end.isBefore(threshold)) {
//                            // end >= threshold
//                            if (!start.isBefore(threshold)) {
//                                // CASE 2: start >= threshold  → all primary
//                                logger.info("Staying in 10.83.1.13 to execute the aggregation on historical transactions: " + aggSQL);
//                                ZonedDateTime startTimeAggHist = ZonedDateTime.now();
//                                agg = jdbcTemplate.queryForList(aggSQL);
//                                ZonedDateTime endTimeAggHist = ZonedDateTime.now();
//                                long durationMsAggHist = Duration.between(startTimeAggHist, endTimeAggHist).toMillis();
//                                logger.info("\nINFO: " + marker + " :: getTransactionsByDateOnly: agg total duration: ---> " + durationMsAggHist + " ms");
//
//                            } else {
//                                // CASE 3: start < threshold ≤ end → span both databases
//                                logger.info("Spanning both DBs for aggregation:");
//
//                                // Query historicals from secondary DB (startDate up to threshold)
//                                logger.info("Travelling to 10.83.1.14 for startDate to threshold: " + aggSQL);
//                                List<Map<String, Object>> aggHistorySecondary = secondJdbcTemplate.queryForList(aggSQL);
//
//                                // Query historicals from primary DB (threshold up to endDate)
//                                logger.info("Staying in 10.83.1.13 for threshold to endDate: " + aggSQL);
//                                List<Map<String, Object>> aggHistoryPrimary = jdbcTemplate.queryForList(aggSQL);
//
//                                // Extract results (should be at most one row per list)
//                                Map<String, Object> sec = aggHistorySecondary.isEmpty() ? new HashMap<>() : aggHistorySecondary.get(0);
//                                Map<String, Object> pri = aggHistoryPrimary.isEmpty() ? new HashMap<>() : aggHistoryPrimary.get(0);
//
//                                // Safely extract and sum numeric values
//                                double totalSummedValue
//                                        = (sec.get("totalValue") == null ? 0.0 : ((Number) sec.get("totalValue")).doubleValue())
//                                        + (pri.get("totalValue") == null ? 0.0 : ((Number) pri.get("totalValue")).doubleValue());
//                                int totalSummedRecords
//                                        = (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue())
//                                        + (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue());
//                                double rateSec = sec.get("successRate") == null ? 0.0 : ((Number) sec.get("successRate")).doubleValue();
//                                double ratePri = pri.get("successRate") == null ? 0.0 : ((Number) pri.get("successRate")).doubleValue();
//                                int succCntSec = (int) Math.round(rateSec / 100.0 * (sec.get("totalRecords") == null ? 0 : ((Number) sec.get("totalRecords")).intValue()));
//                                int succCntPri = (int) Math.round(ratePri / 100.0 * (pri.get("totalRecords") == null ? 0 : ((Number) pri.get("totalRecords")).intValue()));
//                                double successSummedRate = totalSummedRecords == 0 ? 0.0 : ((double) (succCntSec + succCntPri)) * 100.0 / totalSummedRecords;
//
//                                // Prepare the single combined map
//                                Map<String, Object> combined = new HashMap<>();
//                                combined.put("totalValue", totalSummedValue);
//                                combined.put("totalRecords", totalSummedRecords);
//                                combined.put("successRate", successSummedRate);
//
//                                // Assign to aggHistory as a single-element list
//                                agg = new ArrayList<>();
//                                agg.add(combined);
//
//                                logger.info(String.format(
//                                        "\nINFO: %s :: searchTransactions() : Combined aggHistory from AdS [totalValue=%.2f, totalRecords=%d, successRate=%.2f%%]",
//                                        marker, totalSummedValue, totalSummedRecords, successSummedRate
//                                ));
//                            }
//                        }
//                    } else {
//                        // Fallback handling if start or end is null
//                        logger.info(String.format("Invalid or missing start/end datetime: start=%s, end=%s", start, end));
//
//                    }
////                    agg = secondJdbcTemplate.queryForList(aggSQL);
//                }
//
//                if (agg.isEmpty()) {
//                    logger.info("Aggregation query returned no results. Setting default aggregation values for institution.");
//                    networkResponse.setMeta("{\"totalValue\": 0, \"totalRecords\": 0, \"page\": " + page + ", \"limit\": " + limit + ", \"successRate\": 0");
//                } else {
//
//                    Map<String, Object> row = agg.get(0);
//
//                    totalValue = Optional.ofNullable((Number) row.get("totalValue"))
//                            .map(Number::doubleValue)
//                            .orElse(0.0);
//                    totalRecords = Optional.ofNullable((Number) row.get("totalRecords"))
//                            .map(Number::intValue)
//                            .orElse(0);
//                    successRate = Optional.ofNullable((Number) row.get("successRate"))
//                            .map(Number::doubleValue)
//                            .orElse(0.0);
//                }
//
//            }
//
//            String meta = String.format(
//                    "{\"totalValue\": %.2f, \"totalRecords\": %d, \"successRate\": %.2f, \"page\": %d, \"limit\": %d}",
//                    totalValue, totalRecords, successRate, page, limit);
//            networkResponse.setMeta(meta);
//            networkResponse.setCode(200);
//            networkResponse.setStatus("success");
//            networkResponse.setMessage("Searched transactions");
//            networkResponse.setData((ArrayList) transactions);
//
//            logger.info("SearchTransactions completed successfully with " + transactions.size() + " records found");
//            ZonedDateTime endTimeTotalExe = ZonedDateTime.now();
//            long durationMsTotalExe = Duration.between(idKey, endTimeTotalExe).toMillis();
//            logger.info("\nINFO: " + marker + " :: SearchTransactions(): total method execution duration: ---> " + durationMsTotalExe + " ms");
//            return responseManager.ResponseOk(networkResponse);
//        } catch (DataAccessException ex) {
//            logger.info("DataAccessException occurred in SearchTransactions: " + ex.getMessage());
//            ex.printStackTrace();
//            return responseManager.ResponseInternalServerError();
//        }
//    }
//
//// Helper method to append conditions.
//    private void appendCondition(StringBuilder sb, java.util.concurrent.atomic.AtomicBoolean hasCond, ConditionSupplier conditionSupplier) {
//        String condition = conditionSupplier.get();
//        if (condition != null && !condition.isEmpty()) {
//            if (hasCond.get()) {
//                sb.append(" AND ");
//            }
//            sb.append(condition);
//            hasCond.set(true);
//        }
//    }
//
//    @FunctionalInterface
//    interface ConditionSupplier {
//
//        String get();
//    }
//
// Helper methods to sum aggregation results.
    private Double sumTotalValue(List<Map<String, Object>> list) {
        Double sum = 0.0;
        for (Map<String, Object> row : list) {
            Object valueObj = row.get("totalValue");
            if (valueObj instanceof BigDecimal) {
                sum += ((BigDecimal) valueObj).doubleValue();
            } else if (valueObj instanceof Double) {
                sum += (Double) valueObj;
            } else if (valueObj instanceof Number) {
                sum += ((Number) valueObj).doubleValue();
            } else if (valueObj != null) {
                try {
                    sum += Double.parseDouble(valueObj.toString());
                } catch (NumberFormatException e) {
                    // You might want to log/handle this
                }
            }
            // If null, just skip (add nothing)
        }
        return sum;
    }

    private int sumTotalRecords(List<Map<String, Object>> list) {
        int sum = 0;
        for (Map<String, Object> row : list) {
            Object countObj = row.get("totalRecords");
            if (countObj instanceof Number) {
                sum += ((Number) countObj).intValue();
            } else if (countObj != null) {
                try {
                    sum += Integer.parseInt(countObj.toString());
                } catch (NumberFormatException e) {
                    // Optionally log or handle unexpected value
                }
            }
            // If null, skip
        }
        return sum;
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
            String startDate, // expected format: yyyy-MM-dd'T'HH:mm:ss
            String endDate, // expected format: yyyy-MM-dd'T'HH:mm:ss
            int page,
            int limit,
            boolean isCurrent, // legacy parameter (ignored)
            String userInstitutionCode
    ) {
        NetworkResponse networkResponse = new NetworkResponse();
        ZonedDateTime methodStart = ZonedDateTime.now();
        String marker = methodStart.format(fmt);

        try {
            logger.info(String.format(
                    "[%s] SearchTransactions called with: session_id=%s, channel_code=%s, response_code=%s, "
                    + "source_institution_code=%s, destination_institution_code=%s, minAmount=%s, maxAmount=%s, "
                    + "originator_account_number=%s, beneficiary_account_number=%s, startDate=%s, endDate=%s, page=%d, limit=%d, userInstitutionCode=%s",
                    marker, session_id, channel_code, response_code, source_institution_code, destination_institution_code,
                    minAmount, maxAmount, originator_account_number, beneficiary_account_number, startDate, endDate, page, limit, userInstitutionCode
            ));

            // --- Date Parsing ---
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            Optional<LocalDateTime> startOpt = Optional.ofNullable(startDate)
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try {
                            return Optional.of(LocalDateTime.parse(s, fmt));
                        } catch (DateTimeParseException e) {
                            logger.info(String.format("[%s] Failed to parse startDate '%s': %s", marker, s, e.getMessage()));
                            return Optional.empty();
                        }
                    });
            Optional<LocalDateTime> endOpt = Optional.ofNullable(endDate)
                    .filter(s -> !s.isBlank())
                    .flatMap(s -> {
                        try {
                            return Optional.of(LocalDateTime.parse(s, fmt));
                        } catch (DateTimeParseException e) {
                            logger.info(String.format("[%s] Failed to parse endDate '%s': %s", marker, s, e.getMessage()));
                            return Optional.empty();
                        }
                    });

            boolean includeCurrent = false, includeHistory = false;
            LocalDateTime start = null, end = null;
            LocalDate today = LocalDate.now();

            if (startOpt.isPresent() && endOpt.isPresent()) {
                start = startOpt.get();
                end = endOpt.get();
                logger.info(String.format("[%s] Parsed startDate: %s, endDate: %s", marker, start, end));
                if (!start.toLocalDate().isBefore(today)) {
                    includeCurrent = true;
                } else if (end.toLocalDate().isBefore(today)) {
                    includeHistory = true;
                } else {
                    includeCurrent = true;
                    includeHistory = true;
                }
                logger.info(String.format("[%s] Date logic: includeCurrent=%s, includeHistory=%s (today=%s)", marker, includeCurrent, includeHistory, today));
            } else {
                start = LocalDate.now().atStartOfDay().minusMonths(12);
                end = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(0);
                includeCurrent = true;
                includeHistory = true;
                logger.info(String.format("[%s] No/invalid date input, using default: start=%s, end=%s, includeCurrent=true, includeHistory=true", marker, start, end));
            }

            if (includeHistory && !hasSeparateArchive()) {
                // Archive reads resolve to the live table, so query it once.
                includeCurrent = true;
                includeHistory = false;
                logger.info(String.format("[%s] Single transactions table: querying live only", marker));
            }

            LocalDateTime threshold = LocalDateTime.parse(appConfig.getTippingPoint(), DTF);
            logger.info(String.format("[%s] Threshold (tipping point): %s", marker, threshold));

            // --- WHERE BUILDER base filters ---
            WhereBuilder wbBase = buildWhereBuilder(
                    session_id, channel_code, response_code, source_institution_code, destination_institution_code,
                    minAmount, maxAmount, originator_account_number, beneficiary_account_number, userInstitutionCode
            );
            logger.info(String.format("[%s] WHERE clause (excluding date): %s", marker, wbBase.build()));

            int offset = Math.max(0, (page - 1) * limit);
            List<FullTransactionModel> transactions = null;

            String commonSelect = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, "
                    + "a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, "
                    + "a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, "
                    + "a.beneficiary_bvn, a.destination_institution_code, a.narration, a.transaction_date_time, "
                    + "a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, "
                    + "c.institution_name as destInstitutionName, a.destination_node ";

            // --- Transaction Query Logic ---
            if (includeCurrent && !includeHistory) {
                logger.info(String.format("[%s] Query: ONLY current-day transactions (tbl_creditfundtransfers)", marker));
                logger.info(String.format("Common: %s\nWHERE: %s", commonSelect, buildFullFromCurrent(wbBase, startDate, endDate), wbBase.params()));

                transactions = queryTrans(
                        jdbcTemplate,
                        commonSelect, buildFullFromCurrent(wbBase, startDate, endDate), wbBase.params(), limit, offset
                );
                logger.info(String.format("[%s] Returned %d transactions from current table.", marker, transactions.size()));
            } else if (includeHistory && !includeCurrent) {
                // FULL SPLIT-LOGIC BLOCK for ARCHIVE
                logger.info(String.format("[%s] Query: ONLY historical/archival table", marker));
                List<FullTransactionModel> histList = new ArrayList<>();
                if (start != null && end != null) {
                    if (end.isBefore(threshold)) {
                        // Only secondary DB
                        logger.info(String.format("[%s] Querying ONLY secondary archive DB [start=%s, end=%s) < threshold=%s", marker, start, end, threshold));
                        WhereBuilder wbSec = wbBase.cloneWithDateRange(start.format(fmt), end.format(fmt));
                        histList = queryTrans(
                                secondJdbcTemplate,
                                commonSelect, buildFullFromHist(wbSec, wbSec.getStartDate(), wbSec.getEndDate()), wbSec.params(), limit, offset
                        );
                        logger.info(String.format("[%s] Fetched %d rows from secondary DB (10.83.1.14)", marker, histList.size()));
                    } else if (!end.isBefore(threshold)) {
                        if (!start.isBefore(threshold)) {
                            // Only primary DB
                            logger.info(String.format("[%s] Querying ONLY primary archive DB [start=%s, end=%s) >= threshold=%s", marker, start, end, threshold));
//                            logger.info(String.format("SQL: %s", commonSelect, buildFullFromCurrent(wbBase, startDate, endDate), wbBase.params()));

                            WhereBuilder wbPri = wbBase.cloneWithDateRange(start.format(fmt), end.format(fmt));
                            histList = queryTrans(
                                    jdbcTemplate,
                                    commonSelect, buildFullFromHist(wbPri, wbPri.getStartDate(), wbPri.getEndDate()), wbPri.params(), limit, offset
                            );
                            logger.info(String.format("[%s] Fetched %d rows from primary DB (10.83.1.13)", marker, histList.size()));
                        } else {
                            // Spanning threshold: need both!
                            logger.info(String.format("[%s] Querying BOTH archive DBs: Secondary [start=%s, threshold=%s), Primary [threshold=%s, end=%s)", marker, start, threshold, threshold, end));
                            // 1. Secondary DB: [start, threshold)
                            WhereBuilder wbSec = wbBase.cloneWithDateRange(start.format(fmt), threshold.format(fmt));
                            logger.info(String.format("Common: %s\nWHERE: %s", commonSelect, buildFullFromCurrent(wbBase, startDate, endDate), wbBase.params()));

                            List<FullTransactionModel> secList = queryTrans(
                                    secondJdbcTemplate,
                                    commonSelect, buildFullFromHist(wbSec, wbSec.getStartDate(), wbSec.getEndDate()), wbSec.params(), limit, offset
                            );
                            logger.info(String.format("[%s] Fetched %d rows from secondary DB (10.83.1.14)", marker, secList.size()));

                            // 2. Primary DB: [threshold, end)
                            WhereBuilder wbPri = wbBase.cloneWithDateRange(threshold.format(fmt), end.format(fmt));
                            List<FullTransactionModel> priList = queryTrans(
                                    jdbcTemplate,
                                    commonSelect, buildFullFromHist(wbPri, wbPri.getStartDate(), wbPri.getEndDate()), wbPri.params(), limit, offset
                            );
                            logger.info(String.format("[%s] Fetched %d rows from primary DB (10.83.1.13)", marker, priList.size()));

                            histList.addAll(secList);
                            histList.addAll(priList);
                            logger.info(String.format("[%s] Total rows after merging both DBs: %d", marker, histList.size()));
                        }
                    }
                } else {
                    // Fallback handling if start or end is null
                    logger.info(String.format("[%s] Invalid or missing start/end datetime: start=%s, end=%s. Using only primary archive DB as fallback.", marker, start, end));
                    WhereBuilder wbPri = wbBase.cloneWithDateRange(startDate, endDate);
                    histList = queryTrans(
                            jdbcTemplate,
                            commonSelect, buildFullFromHist(wbPri, wbPri.getStartDate(), wbPri.getEndDate()), wbPri.params(), limit, offset
                    );
                    logger.info(String.format("[%s] Fetched %d rows from primary DB (fallback)", marker, histList.size()));
                }
                transactions = histList;
            } else if (includeCurrent && includeHistory) {
                logger.info(String.format("[%s] Query: BOTH current and historical tables", marker));
                // Fetch current (today)
                List<FullTransactionModel> currList = queryTrans(
                        jdbcTemplate,
                        commonSelect, buildFullFromCurrent(wbBase, startDate, endDate), wbBase.params(), limit, offset
                );
                logger.info(String.format("[%s] Current table returned %d transactions.", marker, currList.size()));

                List<FullTransactionModel> histList = new ArrayList<>();
                if (start != null && end != null && start.isBefore(threshold) && !end.isBefore(threshold)) {
                    // [start < threshold <= end]: archive split on threshold
                    logger.info(String.format("[%s] Archive window SPLIT: Secondary DB [start=%s, threshold=%s), Primary DB [threshold=%s, end=%s)", marker,
                            start, threshold, threshold, end));
                    // 1. Secondary DB: [start, threshold)
                    WhereBuilder wbSec = wbBase.cloneWithDateRange(start.format(fmt), threshold.format(fmt));
                    logger.info(String.format("His | Common: %s\nWHERE: %s", commonSelect, buildFullFromHist(wbSec, wbSec.getStartDate(), wbSec.getEndDate()), wbSec.params()));

                    List<FullTransactionModel> secList = queryTrans(
                            secondJdbcTemplate,
                            commonSelect, buildFullFromHist(wbSec, wbSec.getStartDate(), wbSec.getEndDate()), wbSec.params(), limit, offset
                    );
                    logger.info(String.format("[%s] Secondary archive DB returned %d rows.", marker, secList.size()));

                    // 2. Primary DB: [threshold, end)
                    WhereBuilder wbPri = wbBase.cloneWithDateRange(threshold.format(fmt), end.format(fmt));
                    logger.info(String.format("Pri | Common: %s\nWHERE: %s", commonSelect, buildFullFromHist(wbPri, wbPri.getStartDate(), wbPri.getEndDate()), wbPri.params()));
                    List<FullTransactionModel> priList = queryTrans(
                            jdbcTemplate,
                            commonSelect, buildFullFromHist(wbPri, wbPri.getStartDate(), wbPri.getEndDate()), wbPri.params(), limit, offset
                    );
                    logger.info(String.format("[%s] Primary archive DB returned %d rows.", marker, priList.size()));

                    histList.addAll(secList);
                    histList.addAll(priList);
                } else {
                    // Regular: All history in one DB
                    logger.info(String.format("[%s] Querying single archive table (tbl_creditfundtransfer_hist_s)", marker));
                    histList = queryTrans(
                            (start != null && end != null && end.isBefore(threshold)) ? secondJdbcTemplate : jdbcTemplate,
                            commonSelect, buildFullFromHist(wbBase, startDate, endDate), wbBase.params(), limit, offset
                    );
                    logger.info(String.format("[%s] Archive table returned %d rows.", marker, histList.size()));
                }
                // Merge, sort, and paginate globally
                List<FullTransactionModel> combined = new ArrayList<>(currList);
                combined.addAll(histList);
                combined.sort((a, b) -> b.getTransactiondate().compareTo(a.getTransactiondate()));
                int from = Math.min(offset, combined.size());
                int to = Math.min(offset + limit, combined.size());
                transactions = new ArrayList<>(combined.subList(from, to));
                logger.info(String.format("[%s] Total combined after merge/sort/paginate: %d rows.", marker, transactions.size()));
            } else {
                logger.info(String.format("[%s] Fallback: Only current transactions.", marker));
                transactions = queryTrans(
                        jdbcTemplate,
                        commonSelect, buildFullFromCurrent(wbBase, startDate, endDate), wbBase.params(), limit, offset
                );
                logger.info(String.format("[%s] Returned %d transactions from current table (fallback).", marker, transactions.size()));
            }

            // --- Aggregation Logic ---
            Map<String, Object> aggResult = doAggregationsWithTippingPoint(
                    includeCurrent, includeHistory, wbBase,
                    start, end, threshold, startDate, endDate
            );
            Double totalValue = safeDouble(aggResult, "totalValue");
            int totalRecords = safeInt(aggResult, "totalRecords");
            Double successRate = safeDouble(aggResult, "successRate");

            logger.info(String.format("[%s] Aggregation results: totalValue=%.2f, totalRecords=%d, successRate=%.2f",
                    marker, totalValue, totalRecords, successRate));

            String meta = String.format(
                    "{\"totalValue\": %.2f, \"totalRecords\": %d, \"successRate\": %.2f, \"page\": %d, \"limit\": %d}",
                    totalValue, totalRecords, successRate, page, limit);
            networkResponse.setMeta(meta);
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched transactions");
            networkResponse.setData((ArrayList) transactions);

            logger.info(String.format("[%s] SearchTransactions completed successfully. Total records in final result: %d", marker, transactions.size()));
            long durationMsTotalExe = Duration.between(methodStart, ZonedDateTime.now()).toMillis();
            logger.info(String.format("[%s] SearchTransactions(): total method execution duration: %d ms", marker, durationMsTotalExe));
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info(String.format("[%s] DataAccessException in SearchTransactions: %s", marker, ex.getMessage()));
            ex.printStackTrace();
            return responseManager.ResponseInternalServerError();
        }
    }

    private double safeDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                // Log it, if you wish
                return 0.0;
            }
        }
        return 0.0;
    }

    private int safeInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                // Log it, if you wish
                return 0;
            }
        }
        return 0;
    }

    // Returns the right template for a date range (archive)
    private JdbcTemplate getHistTemplate(LocalDateTime start, LocalDateTime end, LocalDateTime threshold) {
        if (start != null && end != null && end.isBefore(threshold)) {
            logger.info("Travelling to 10.83.1.14 ...");
            return secondJdbcTemplate;
        }
        logger.info("Staying in 10.83.1.13 ...");
        return jdbcTemplate;
    }

// WHERE builder for all fields except date (so date can be changed for splits)
private WhereBuilder buildWhereBuilder(String session_id, String channel_code, String response_code,
                                       String source_institution_code, String destination_institution_code,
                                       String minAmount, String maxAmount, String originator_account_number, String beneficiary_account_number,
                                       String userInstitutionCode
) {
    WhereBuilder wb = new WhereBuilder();

    // Add condition for destination_node when userInstitutionCode is 000004
//    if ("000004".equals(userInstitutionCode)) {
//        wb.add("a.destination_node != ?", "9082");
//    }

    // A blank code means "all institutions" (global operator), same as -1.
    boolean scopedToInstitution = userInstitutionCode != null
            && !userInstitutionCode.isBlank()
            && !"-1".equals(userInstitutionCode);

    if (scopedToInstitution
            && (source_institution_code == null || source_institution_code.isEmpty())
            && (destination_institution_code == null || destination_institution_code.isEmpty())) {
        wb.addRaw("(a.source_institution_code = ? OR a.destination_institution_code = ?)");
        wb.params().add(userInstitutionCode);
        wb.params().add(userInstitutionCode);
    }

    wb.add("a.session_id = ?", session_id);
    wb.add("a.channel_code = ?", channel_code);
    if (response_code != null && !response_code.isBlank()) {
        if ("111".equals(response_code)) {
            wb.addRaw("a.response_code != ?");
            wb.params().add("00");
        } else {
            wb.add("a.response_code = ?", response_code);
        }
    }
    wb.add("a.source_institution_code = ?", source_institution_code);
    wb.add("a.destination_institution_code = ?", destination_institution_code);
    wb.add("a.originator_account_number = ?", originator_account_number);
    wb.add("a.beneficiary_account_number = ?", beneficiary_account_number);
    if (minAmount != null && !minAmount.isBlank() && Double.parseDouble(minAmount) > 0) {
        wb.add("a.amount >= ?", Double.parseDouble(minAmount));
    }
    if (maxAmount != null && !maxAmount.isBlank() && Double.parseDouble(maxAmount) > 0) {
        wb.add("a.amount <= ?", Double.parseDouble(maxAmount));
    }

    return wb;
}

    // Helper for full from clause
    private String buildFullFromCurrent(WhereBuilder wb, String startDate, String endDate) {
        // current table: ajiswitch_db.tbl_creditfundtransfers
        wb.addDateRange(startDate, endDate);
        return "FROM ajiswitch_db.tbl_creditfundtransfers a "
                + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                + wb.build() + " ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
    }

    private String buildFullFromHist(WhereBuilder wb, String startDate, String endDate) {
        wb.addDateRange(startDate, endDate);
        return "FROM " + archiveTable() + " a "
                + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                + wb.build() + " ORDER BY a.transaction_date_time DESC LIMIT ? OFFSET ?";
    }

    private List<FullTransactionModel> queryTrans(JdbcTemplate template, String select, String from, List<Object> params, int limit, int offset) {
        List<Object> allParams = new ArrayList<>(params);
        allParams.add(limit);
        allParams.add(offset);
        return template.query(select + from, allParams.toArray(), new FullTransactionMapper());
    }

    private Map<String, Object> doAggregationsWithTippingPoint(
            boolean includeCurrent, boolean includeHistory, WhereBuilder wbBase,
            LocalDateTime start, LocalDateTime end, LocalDateTime threshold, String startDate, String endDate
    ) {
        if (includeCurrent && includeHistory) {
            // Aggregate both current and history (with tipping split if needed)
            Map<String, Object> aggCurrent = runAggregate(
                    buildAggSQL("ajiswitch_db.tbl_creditfundtransfers", wbBase, startDate, endDate),
                    wbBase.params(), jdbcTemplate);

            Map<String, Object> aggHist;
            if (start != null && end != null && start.isBefore(threshold) && !end.isBefore(threshold)) {
                // Need split agg
                // Secondary: [start, threshold)
                WhereBuilder wbSec = wbBase.cloneWithDateRange(startDate, threshold.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                Map<String, Object> aggSec = runAggregate(
                        buildAggSQL(archiveTable(), wbSec, wbSec.getStartDate(), wbSec.getEndDate()),
                        wbSec.params(), secondJdbcTemplate
                );
                // Primary: [threshold, end)
                WhereBuilder wbPri = wbBase.cloneWithDateRange(threshold.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), endDate);
                Map<String, Object> aggPri = runAggregate(
                        buildAggSQL(archiveTable(), wbPri, wbPri.getStartDate(), wbPri.getEndDate()),
                        wbPri.params(), jdbcTemplate
                );
                // Combine
                aggHist = combineAggs(aggSec, aggPri);
            } else {
                // Only one DB
                aggHist = runAggregate(
                        buildAggSQL(archiveTable(), wbBase, startDate, endDate),
                        wbBase.params(),
                        getHistTemplate(start, end, threshold)
                );
            }
            return combineAggs(aggCurrent, aggHist);
        } else if (includeCurrent) {
            // Only current
            return runAggregate(
                    buildAggSQL("ajiswitch_db.tbl_creditfundtransfers", wbBase, startDate, endDate),
                    wbBase.params(), jdbcTemplate);
        } else if (includeHistory) {
            if (start != null && end != null && start.isBefore(threshold) && !end.isBefore(threshold)) {
                // Archive split
                WhereBuilder wbSec = wbBase.cloneWithDateRange(startDate, threshold.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                Map<String, Object> aggSec = runAggregate(
                        buildAggSQL(archiveTable(), wbSec, wbSec.getStartDate(), wbSec.getEndDate()),
                        wbSec.params(), secondJdbcTemplate
                );
                WhereBuilder wbPri = wbBase.cloneWithDateRange(threshold.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), endDate);
                Map<String, Object> aggPri = runAggregate(
                        buildAggSQL(archiveTable(), wbPri, wbPri.getStartDate(), wbPri.getEndDate()),
                        wbPri.params(), jdbcTemplate
                );
                return combineAggs(aggSec, aggPri);
            } else {
                // Only one DB
                return runAggregate(
                        buildAggSQL(archiveTable(), wbBase, startDate, endDate),
                        wbBase.params(),
                        getHistTemplate(start, end, threshold)
                );
            }
        }
        return new HashMap<>();
    }

// Build agg SQL
    private String buildAggSQL(String table, WhereBuilder wb, String startDate, String endDate) {
        wb.addDateRange(startDate, endDate);
        return "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, "
                + "AVG(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) * 100 AS successRate "
                + "FROM " + table + " a " + wb.build();
    }

    private Map<String, Object> runAggregate(String sql, List<Object> params, JdbcTemplate template) {
        List<Map<String, Object>> results = template.queryForList(sql, params.toArray());
        if (results == null || results.isEmpty()) {
            return new HashMap<>();
        }
        return results.get(0);
    }

    private Map<String, Object> combineAggs(Map<String, Object> aggA, Map<String, Object> aggB) {
        double aVal = safeDouble(aggA, "totalValue");
        double bVal = safeDouble(aggB, "totalValue");
        int aRec = safeInt(aggA, "totalRecords");
        int bRec = safeInt(aggB, "totalRecords");
        double aRate = safeDouble(aggA, "successRate");
        double bRate = safeDouble(aggB, "successRate");

        int totalRecords = aRec + bRec;
        double successRate = (totalRecords > 0) ? ((aRate * aRec) + (bRate * bRec)) / totalRecords : 0.0;
        double totalValue = aVal + bVal;

        Map<String, Object> result = new HashMap<>();
        result.put("totalValue", totalValue);
        result.put("totalRecords", totalRecords);
        result.put("successRate", successRate);
        return result;
    }

    @Override
    public ResponseEntity GetTimeoutRetries(String startDate, String endDate, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> transactions;
            SQL = "SELECT a.*, "
                    + "b.institution_name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_timeout_retry a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.destination_institution_code = b.institution_code "
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
                    + "b.institution_name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_timeout_retry a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.destination_institution_code = b.institution_code " + whereQuery
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
            List<Map<String, Object>> summary;
            List<Map<String, Object>> summary_;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.response_code = '00'";

            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }

//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
//            String table_ = isCurrent ? "ajiswitch_db.tbl_name_enquiries" : "ajiswitch_db.tbl_name_enquiries_hist_s";
//            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
//                    + "FROM " + table_ + " a "
//                    + "WHERE a.transactiondate BETWEEN ? AND ? AND a.response_code = '00'";
//            
//            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
//                summary_ = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
//
//            } else if (table.equalsIgnoreCase(archiveTable())) {
//                summary_ = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
//            } else {
//                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
//                logger.info(msg);
//                throw new IllegalStateException(msg);
//            }
//            List<Map<String, Object>> summary_ = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
//            summary.addAll(summary_);
            networkResponse.setCode(200);
            networkResponse.setMessage("Transaction Duration Average");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetFTTimeAverage(String institutioncode, String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.response_code = '00' AND a.source_institution_code = ? ";
            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }

//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
//            String table_ = isCurrent ? "ajiswitch_db.tbl_name_enquiries" : "ajiswitch_db.tbl_name_enquiries_hist_s";
//            SQL = "SELECT COUNT(a.id) as volume, SUM(a.txn_duration) as totalduration "
//                    + "FROM " + table_ + " a "
//                    + "WHERE a.transactiondate BETWEEN ? AND ? AND a.response_code = '00' AND a.destination_institution_code = ? ";
//
//            List<Map<String, Object>> summary_ = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
//            summary.addAll(summary_);
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
                    + "FROM " + archiveTable() + " a "
                    + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND a.response_code = '00'"
                    + "GROUP BY CAST(a.transaction_date_time as DATE) "
                    + "ORDER BY a.transaction_date_time DESC";

            List<Map<String, Object>> summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});

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
                    + "FROM " + archiveTable() + " a "
                    + "WHERE a.transaction_date_time >= ? AND a.transaction_date_time < ? AND a.response_code = '00' AND a.source_institution_code = ? "
                    + "GROUP BY CAST(a.transaction_date_time as DATE) "
                    + "ORDER BY a.transaction_date_time DESC";

            List<Map<String, Object>> summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});

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
            List<Map<String, Object>> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM " + table + " a "
                    + "WHERE a.response_code != '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.response_code "
                    + "ORDER BY volume DESC "
                    + "LIMIT 5";

            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }

//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
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
            List<Map<String, Object>> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM " + table + " a "
                    + "WHERE a.response_code != '00' AND a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ? "
                    + "GROUP BY a.response_code "
                    + "ORDER BY volume DESC "
                    + "LIMIT 5";
            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }

//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
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
            List<Map<String, Object>> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, b.shortName as label, a.destination_institution_code, b.color "
                    + "FROM " + table + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.destination_institution_code = b.institution_code "
                    + "WHERE a.response_code != '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.destination_institution_code "
                    + "LIMIT 20";
            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }

//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            networkResponse.setCode(200);
            networkResponse.setMessage("Top failing institutions");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary);
            networkResponse.setTnxModel(tnxModel);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("error>>>>" + ex.getMessage());
            ex.printStackTrace();
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetFailedTnxCountByInstitutions(String institution, String startDate, String endDate, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, b.shortName as label, a.source_institution_code, b.color "
                    + "FROM " + table + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "WHERE a.response_code != '00' AND a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ?"
                    + "GROUP BY a.source_institution_code "
                    + "LIMIT 20";
            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institution});

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institution});
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }

//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institution});
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
            List<Map<String, Object>> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ? "
                    + "GROUP BY a.response_code";

            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }
//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});

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
            List<Map<String, Object>> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.response_code";
            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }

//            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
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
            List<ChannelsTnxValueModel> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, a.channel_code as label "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ?"
                    + "GROUP BY a.channel_code "
                    + "LIMIT 6";
            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionChannelsSummaryMapper());

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionChannelsSummaryMapper());
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }

//            List<ChannelsTnxValueModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionChannelsSummaryMapper());
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
            List<ChannelsTnxValueModel> summary;
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers" : archiveTable();
            SQL = "SELECT COUNT(a.id) as volume, a.channel_code as label "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? AND a.source_institution_code = ? "
                    + "GROUP BY a.channel_code "
                    + "LIMIT 6";

            if (table.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
                summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode}, new TransactionChannelsSummaryMapper());

            } else if (table.equalsIgnoreCase(archiveTable())) {
                summary = secondJdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode}, new TransactionChannelsSummaryMapper());
            } else {
                String msg = String.format("Unknown transaction table '%s'—cannot update response_code", table);
                logger.info(msg);
                throw new IllegalStateException(msg);
            }
//            List<ChannelsTnxValueModel> summary = jdbcTemplate.query(SQL, new Object[]{startDate, endDate, institutioncode}, new TransactionChannelsSummaryMapper());

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
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.institution_name, b.shortName, b.color, b.institution_code "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.source_institution_code";

            List<TransactionSummaryModel> summary = secondJdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());

            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.institution_name, b.shortName, b.color, b.institution_code "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.destination_institution_code = b.institution_code "
                    + "WHERE a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.destination_institution_code";

            List<TransactionSummaryModel> summary_ = secondJdbcTemplate.query(SQL, new Object[]{startDate, endDate}, new TransactionSummaryMapper());
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
            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.institution_name, b.shortName, b.color, b.institution_code "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "WHERE a.source_institution_code = ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";

            List<TransactionSummaryModel> summary = secondJdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());

            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value "
                    + "FROM " + archiveTable() + " a "
                    + "WHERE a.source_institution_code != ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";

            List<TransactionSummaryModel> summaryOthers = secondJdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
            summary.add(summaryOthers.get(0));

            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value, b.institution_name, b.shortName, b.color, b.institution_code "
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.destination_institution_code = b.institution_code "
                    + "WHERE a.destination_institution_code = ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";

            List<TransactionSummaryModel> summary_ = secondJdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());

            SQL = "SELECT COUNT(a.id) as volume, SUM(a.amount) as value "
                    + "FROM " + archiveTable() + " a "
                    + "WHERE a.destination_institution_code != ? AND a.response_code = '00' AND a.transaction_date_time BETWEEN ? AND ?";
            List<TransactionSummaryModel> summary_Others = secondJdbcTemplate.query(SQL, new Object[]{institutioncode, startDate, endDate}, new TransactionSummaryMapper());
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
            // --- normalize date strings ---
            if (startDate.contains("T")) {
                startDate = startDate.replace("T", " ");
            }
            if (endDate.contains("T")) {
                endDate = endDate.replace("T", " ");
            }

            logger.info("GetTransactionsRates called with startDate=" + startDate
                    + ", endDate=" + endDate
                    + ", inward=" + inward
                    + ", institution='" + institution + "'");

            String SQL, SQL_;
            String where = "WHERE response_code = '00' AND transaction_date_time BETWEEN ? AND ? ";
            String whereTwo = "WHERE transaction_date_time BETWEEN ? AND ? ";

            // --- build SQL for inward vs outward ---
            if (inward) {
                logger.info("Building INWARD query");
                if (!institution.isEmpty()) {
                    where += " AND destination_institution_code = ? ";
                    whereTwo += " AND destination_institution_code = ? ";
                }
                SQL = "SELECT b.institution_name, b.shortName, b.color, b.institution_code, COALESCE(a.volume, 0) AS volume "
                        + "FROM ajiswitch_db.tbl_nodes b "
                        + "LEFT JOIN ( "
                        + "  SELECT destination_institution_code, COUNT(id) AS volume "
                        + "  FROM ajiswitch_db.tbl_creditfundtransfers "
                        + where
                        + "  GROUP BY destination_institution_code"
                        + ") a ON b.institution_code = a.destination_institution_code "
                        + "ORDER BY b.institution_name ASC";//SELECT b.institution_name, b.shortName, b.color, b.institution_code, COALESCE(a.volume, 0) AS volume FROM ajiswitch_db.tbl_nodes b LEFT JOIN (   SELECT destination_institution_code, COUNT(id) AS volume   FROM ajiswitch_db.tbl_creditfundtransfers WHERE response_code = '00' AND transaction_date_time BETWEEN '2025-06-01 00:00' AND '2025-06-01 23:00'   GROUP BY destination_institution_code) a ON b.institution_code = a.destination_institution_code ORDER BY b.institution_name ASC
                SQL_ = SQL.replace("destination_institution_code", "destination_institution_code") // same subquery but without response_code filter
                        .replace("response_code = '00' AND ", "");
            } else {
                logger.info("Building OUTWARD query");
                if (!institution.isEmpty()) {
                    where += " AND source_institution_code = ? ";
                    whereTwo += " AND source_institution_code = ? ";
                }
                SQL = "SELECT b.institution_name, b.shortName, b.color, b.institution_code, COALESCE(a.volume, 0) AS volume "
                        + "FROM ajiswitch_db.tbl_nodes b "
                        + "LEFT JOIN ( "
                        + "  SELECT source_institution_code, COUNT(id) AS volume "
                        + "  FROM ajiswitch_db.tbl_creditfundtransfers "
                        + where
                        + "  GROUP BY source_institution_code"
                        + ") a ON b.institution_code = a.source_institution_code "
                        + "ORDER BY b.institution_name ASC";
                SQL_ = SQL.replace("source_institution_code", "source_institution_code")
                        .replace("response_code = '00' AND ", "");
            }

            logger.info("Primary SQL: " + SQL);
            logger.info("Secondary SQL: " + SQL_);

            // --- prepare parameters ---
            Object[] params = institution.isEmpty()
                    ? new Object[]{startDate, endDate}
                    : new Object[]{startDate, endDate, institution};
            logger.info("Query parameters: " + Arrays.toString(params));

            // --- execute and log results ---
            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, params, new TransactionSummaryMapper());
            logger.info("Summary (successful) rows returned: " + summary.size());

            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL_, params, new TransactionSummaryMapper());
            logger.info("Summary (total)      rows returned: " + summary_.size());

            // --- build response ---
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSuccessVolumes((ArrayList) summary);
            tnxModel.setTotalVolumes((ArrayList) summary_);
            networkResponse.setTnxModel(tnxModel);

            logger.info("GetTransactionsRates completed successfully");
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info("GetTransactionsRates failed: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetTransactionsRates(String institutioncode, String startDate, String endDate, boolean inward) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            logger.info("GetTransactionsRates called with institutioncode=" + institutioncode
                    + ", startDate=" + startDate
                    + ", endDate=" + endDate
                    + ", inward=" + inward);

            String SQL, SQL_;
            if (inward) {
                logger.info("Building INWARD queries");
                SQL = "SELECT COUNT(a.id) as volume, b.institution_name, b.shortName, b.color, b.institution_code "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                        + "  ON a.destination_institution_code = b.institution_code "
                        + "WHERE a.response_code = '00' "
                        + "  AND a.destination_institution_code = ? "
                        + "  AND a.transaction_date_time BETWEEN ? AND ?";
                SQL_ = "SELECT COUNT(a.id) as volume, b.institution_name, b.shortName, b.color, b.institution_code "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                        + "  ON a.destination_institution_code = b.institution_code "
                        + "WHERE a.destination_institution_code = ? "
                        + "  AND a.transaction_date_time BETWEEN ? AND ?";
            } else {
                logger.info("Building OUTWARD queries");
                SQL = "SELECT COUNT(a.id) as volume, b.institution_name, b.shortName, b.color, b.institution_code "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                        + "  ON a.source_institution_code = b.institution_code "
                        + "WHERE a.response_code = '00' "
                        + "  AND a.source_institution_code = ? "
                        + "  AND a.transaction_date_time BETWEEN ? AND ?";
                SQL_ = "SELECT COUNT(a.id) as volume, b.institution_name, b.shortName, b.color, b.institution_code "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                        + "  ON a.source_institution_code = b.institution_code "
                        + "WHERE a.source_institution_code = ? "
                        + "  AND a.transaction_date_time BETWEEN ? AND ?";
            }

            logger.info("SQL (successful only): " + SQL);
            logger.info("SQL (all transactions): " + SQL_);

            Object[] params = new Object[]{institutioncode, startDate, endDate};
            logger.info("Query parameters: " + Arrays.toString(params));

            List<TransactionSummaryModel> summary = jdbcTemplate.query(SQL, params, new TransactionSummaryMapper());
            logger.info("Successful transactions returned: " + summary.size());

            List<TransactionSummaryModel> summary_ = jdbcTemplate.query(SQL_, params, new TransactionSummaryMapper());
            logger.info("Total transactions returned:      " + summary_.size());

            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSuccessVolumes((ArrayList) summary);
            tnxModel.setTotalVolumes((ArrayList) summary_);
            networkResponse.setTnxModel(tnxModel);

            logger.info("GetTransactionsRates completed successfully");
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info("GetTransactionsRates failed: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    private static String normalizeMonitoringDate(String value) {
        if (value == null) {
            return null;
        }
        if (value.contains("T")) {
            return value.replace("T", " ");
        }
        return value;
    }

    private static int pct(long success, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(success * 100.0 / total);
    }

    private static double pctDouble(long success, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(success * 1000.0 / total) / 10.0;
    }

    private static String formatInstitutionDisplayName(String institutionName, String shortName) {
        if (institutionName == null || institutionName.isEmpty()) {
            return shortName != null ? shortName : "";
        }
        if (shortName == null || shortName.isEmpty() || institutionName.equalsIgnoreCase(shortName)) {
            return institutionName;
        }
        return institutionName + " (" + shortName + ")";
    }

    private static LocalDateTime toLocalDateTime(Object bucketTime) {
        if (bucketTime == null) {
            return null;
        }
        if (bucketTime instanceof LocalDateTime) {
            return (LocalDateTime) bucketTime;
        }
        if (bucketTime instanceof Timestamp) {
            return ((Timestamp) bucketTime).toLocalDateTime();
        }
        if (bucketTime instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) bucketTime).getTime()).toLocalDateTime();
        }
        try {
            return LocalDateTime.parse(String.valueOf(bucketTime).replace(" ", "T"));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static class LiveMonitoringBucket {
        long inflowTotal;
        long inflowSuccess;
        long outflowTotal;
        long outflowSuccess;
    }

    private static class LiveMonitoringAccumulator {
        String institutionCode;
        String institutionName;
        String shortName;
        long inflowTotal;
        long inflowSuccess;
        long outflowTotal;
        long outflowSuccess;
        TreeMap<LocalDateTime, LiveMonitoringBucket> buckets = new TreeMap<>();
    }

    @Override
    public ResponseEntity GetLiveMonitoring(String startDate, String endDate, String institution, int bucketMinutes, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            startDate = normalizeMonitoringDate(startDate);
            endDate = normalizeMonitoringDate(endDate);
            if (institution == null) {
                institution = "";
            }
            final String institutionFilter = institution;
            int safeBucketMinutes = bucketMinutes > 0 ? bucketMinutes : 10;
            int safeLimit = limit > 0 ? limit : 8;
            int bucketSeconds = safeBucketMinutes * 60;

            String institutionFilterIn = institutionFilter.isEmpty() ? "" : " AND a.destination_institution_code = ? ";
            String institutionFilterOut = institutionFilter.isEmpty() ? "" : " AND a.source_institution_code = ? ";

            String inflowSql = "SELECT b.institution_code, b.institution_name, b.shortName, "
                    + "FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(a.transaction_date_time) / ?) * ?) AS bucket_time, "
                    + "COUNT(a.id) AS total_count, "
                    + "SUM(CASE WHEN a.response_code IN ('00','10','11','16') THEN 1 ELSE 0 END) AS success_count "
                    + "FROM " + TNX_LIVE_TABLE + " a "
                    + "JOIN ajiswitch_db.tbl_nodes b ON a.destination_institution_code = b.institution_code "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
                    + institutionFilterIn
                    + "GROUP BY b.institution_code, b.institution_name, b.shortName, bucket_time";

            String outflowSql = "SELECT b.institution_code, b.institution_name, b.shortName, "
                    + "FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(a.transaction_date_time) / ?) * ?) AS bucket_time, "
                    + "COUNT(a.id) AS total_count, "
                    + "SUM(CASE WHEN a.response_code IN ('00','10','11','16') THEN 1 ELSE 0 END) AS success_count "
                    + "FROM " + TNX_LIVE_TABLE + " a "
                    + "JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
                    + institutionFilterOut
                    + "GROUP BY b.institution_code, b.institution_name, b.shortName, bucket_time";

            Object[] inflowParams = institutionFilter.isEmpty()
                    ? new Object[]{bucketSeconds, bucketSeconds, startDate, endDate}
                    : new Object[]{bucketSeconds, bucketSeconds, startDate, endDate, institutionFilter};
            Object[] outflowParams = institutionFilter.isEmpty()
                    ? new Object[]{bucketSeconds, bucketSeconds, startDate, endDate}
                    : new Object[]{bucketSeconds, bucketSeconds, startDate, endDate, institutionFilter};

            List<Map<String, Object>> inflowRows = jdbcTemplate.queryForList(inflowSql, inflowParams);
            List<Map<String, Object>> outflowRows = jdbcTemplate.queryForList(outflowSql, outflowParams);

            Map<String, LiveMonitoringAccumulator> byCode = new LinkedHashMap<>();

            for (Map<String, Object> row : inflowRows) {
                String code = String.valueOf(row.get("institution_code"));
                LiveMonitoringAccumulator acc = byCode.computeIfAbsent(code, k -> new LiveMonitoringAccumulator());
                acc.institutionCode = code;
                acc.institutionName = row.get("institution_name") != null ? String.valueOf(row.get("institution_name")) : code;
                acc.shortName = row.get("shortName") != null ? String.valueOf(row.get("shortName")) : "";
                long total = row.get("total_count") != null ? ((Number) row.get("total_count")).longValue() : 0L;
                long success = row.get("success_count") != null ? ((Number) row.get("success_count")).longValue() : 0L;
                acc.inflowTotal += total;
                acc.inflowSuccess += success;
                LocalDateTime bucket = toLocalDateTime(row.get("bucket_time"));
                if (bucket != null) {
                    LiveMonitoringBucket bucketData = acc.buckets.computeIfAbsent(bucket, k -> new LiveMonitoringBucket());
                    bucketData.inflowTotal += total;
                    bucketData.inflowSuccess += success;
                }
            }

            for (Map<String, Object> row : outflowRows) {
                String code = String.valueOf(row.get("institution_code"));
                LiveMonitoringAccumulator acc = byCode.computeIfAbsent(code, k -> new LiveMonitoringAccumulator());
                acc.institutionCode = code;
                if (acc.institutionName == null || acc.institutionName.isEmpty()) {
                    acc.institutionName = row.get("institution_name") != null ? String.valueOf(row.get("institution_name")) : code;
                }
                if (acc.shortName == null || acc.shortName.isEmpty()) {
                    acc.shortName = row.get("shortName") != null ? String.valueOf(row.get("shortName")) : "";
                }
                long total = row.get("total_count") != null ? ((Number) row.get("total_count")).longValue() : 0L;
                long success = row.get("success_count") != null ? ((Number) row.get("success_count")).longValue() : 0L;
                acc.outflowTotal += total;
                acc.outflowSuccess += success;
                LocalDateTime bucket = toLocalDateTime(row.get("bucket_time"));
                if (bucket != null) {
                    LiveMonitoringBucket bucketData = acc.buckets.computeIfAbsent(bucket, k -> new LiveMonitoringBucket());
                    bucketData.outflowTotal += total;
                    bucketData.outflowSuccess += success;
                }
            }

            List<LiveMonitoringAccumulator> ranked = new ArrayList<>(byCode.values());
            ranked.sort((a, b) -> Long.compare(
                    (b.inflowTotal + b.outflowTotal),
                    (a.inflowTotal + a.outflowTotal)));

            if (!institutionFilter.isEmpty()) {
                ranked = ranked.stream()
                        .filter(acc -> institutionFilter.equals(acc.institutionCode))
                        .collect(Collectors.toList());
            } else if (ranked.size() > safeLimit) {
                ranked = ranked.subList(0, safeLimit);
            }

            DateTimeFormatter timeLabelFmt = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);
            ArrayList data = new ArrayList();

            for (LiveMonitoringAccumulator acc : ranked) {
                LiveMonitoringInstitutionModel model = new LiveMonitoringInstitutionModel();
                model.setInstitutionCode(acc.institutionCode);
                model.setShortName(acc.shortName);
                model.setName(formatInstitutionDisplayName(acc.institutionName, acc.shortName));

                int inflowSuccessPct = pct(acc.inflowSuccess, acc.inflowTotal);
                int outflowSuccessPct = pct(acc.outflowSuccess, acc.outflowTotal);
                model.setInflowSuccess(inflowSuccessPct);
                model.setInflowFailure(100 - inflowSuccessPct);
                model.setOutflowSuccess(outflowSuccessPct);
                model.setOutflowFailure(100 - outflowSuccessPct);

                List<LiveMonitoringTimePointModel> series = new ArrayList<>();
                for (Map.Entry<LocalDateTime, LiveMonitoringBucket> entry : acc.buckets.entrySet()) {
                    LiveMonitoringBucket bucket = entry.getValue();
                    LiveMonitoringTimePointModel point = new LiveMonitoringTimePointModel();
                    point.setTime(entry.getKey().format(timeLabelFmt).toLowerCase(Locale.ENGLISH));
                    point.setInflow(pctDouble(bucket.inflowSuccess, bucket.inflowTotal));
                    point.setOutflow(pctDouble(bucket.outflowSuccess, bucket.outflowTotal));
                    series.add(point);
                }
                model.setTimeSeries(series);
                data.add(model);
            }

            networkResponse.setCode(200);
            networkResponse.setMessage("Live monitoring");
            networkResponse.setData(data);
            networkResponse.setMeta(String.format(
                    "{\"windowMinutes\":90,\"bucketMinutes\":%d,\"generatedAt\":\"%s\"}",
                    safeBucketMinutes,
                    LocalDateTime.now().format(DTF)));
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("GetLiveMonitoring failed: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetLiveTransactionFeed(String since, int limit, String institution) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            if (institution == null) {
                institution = "";
            }
            int safeLimit = limit > 0 ? Math.min(limit, 100) : 50;
            String sinceValue = since != null ? since.trim() : "";
            boolean hasSince = !sinceValue.isEmpty();

            String institutionFilter = institution.isEmpty()
                    ? ""
                    : " AND (a.source_institution_code = ? OR a.destination_institution_code = ?) ";
            String sinceFilter = hasSince ? " AND a.transaction_date_time > ? " : "";

            String SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.response_code, a.beneficiary_account_number, a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.destination_institution_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name AS srcInstitutionName, c.institution_name AS destInstitutionName, a.destination_node "
                    + "FROM " + TNX_LIVE_TABLE + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                    + "WHERE 1=1 "
                    + sinceFilter
                    + institutionFilter
                    + " ORDER BY a.transaction_date_time DESC LIMIT ?";

            List<Object> params = new ArrayList<>();
            if (hasSince) {
                params.add(normalizeMonitoringDate(sinceValue));
            }
            if (!institution.isEmpty()) {
                params.add(institution);
                params.add(institution);
            }
            params.add(safeLimit);

            List<FullTransactionModel> transactions = jdbcTemplate.query(SQL, params.toArray(), new FullTransactionMapper());

            String newestTimestamp = "";
            if (!transactions.isEmpty() && transactions.get(0).getTransactiondate() != null) {
                newestTimestamp = transactions.get(0).getTransactiondate();
            }

            LocalDateTime serverNow = LocalDateTime.now();
            String serverTime = serverNow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String escapedNewest = newestTimestamp.replace("\\", "\\\\").replace("\"", "\\\"");
            String metaJson = String.format(
                    "{\"serverTime\":\"%s\",\"newestTimestamp\":\"%s\",\"count\":%d}",
                    serverTime,
                    escapedNewest,
                    transactions.size());

            networkResponse.setCode(200);
            networkResponse.setMessage("Live transaction feed");
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(metaJson);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("GetLiveTransactionFeed failed: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetStatusSummary(String startDate, String endDate, boolean isCurrent, String institution) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            startDate = normalizeMonitoringDate(startDate);
            endDate = normalizeMonitoringDate(endDate);
            if (institution == null) {
                institution = "";
            }

            String table = isCurrent ? TNX_LIVE_TABLE : archiveTable();
            String institutionFilter = institution.isEmpty()
                    ? ""
                    : " AND (a.source_institution_code = ? OR a.destination_institution_code = ?) ";

            String SQL = "SELECT "
                    + "CASE "
                    + "  WHEN a.response_code IN ('00','10','11','16') THEN 'Successful' "
                    + "  WHEN a.response_code = '09' THEN 'Pending' "
                    + "  ELSE 'Failed' "
                    + "END AS label, "
                    + "COUNT(a.id) AS volume "
                    + "FROM " + table + " a "
                    + "WHERE a.transaction_date_time BETWEEN ? AND ? "
                    + institutionFilter
                    + "GROUP BY label";

            Object[] params = institution.isEmpty()
                    ? new Object[]{startDate, endDate}
                    : new Object[]{startDate, endDate, institution, institution};

            List<Map<String, Object>> rows;
            if (table.equalsIgnoreCase(TNX_LIVE_TABLE)) {
                rows = jdbcTemplate.queryForList(SQL, params);
            } else if (table.equalsIgnoreCase(archiveTable())) {
                rows = secondJdbcTemplate.queryForList(SQL, params);
            } else {
                throw new IllegalStateException("Unknown transaction table '" + table + "'");
            }

            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("Successful", 0L);
            counts.put("Pending", 0L);
            counts.put("Failed", 0L);
            long totalTransactions = 0L;

            for (Map<String, Object> row : rows) {
                String label = row.get("label") != null ? String.valueOf(row.get("label")) : "Failed";
                long volume = row.get("volume") != null ? ((Number) row.get("volume")).longValue() : 0L;
                counts.put(label, counts.getOrDefault(label, 0L) + volume);
                totalTransactions += volume;
            }

            ArrayList summary = new ArrayList();
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("label", entry.getKey());
                item.put("volume", entry.getValue());
                summary.add(item);
            }

            networkResponse.setCode(200);
            networkResponse.setMessage("Transaction status summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary(summary);
            networkResponse.setTnxModel(tnxModel);
            networkResponse.setMeta(String.format("{\"totalTransactions\":%d}", totalTransactions));
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("GetStatusSummary failed: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

//    @Override
//    public ResponseEntity SearchTransactionsForSessionIds(String sessionids) {
//        logger.info("Session ids: " + sessionids);
//        NetworkResponse networkResponse = new NetworkResponse();
//        try {
//            String SQL = "SELECT a.session_id, a.originator_account_name, a.originator_account_number, a.originator_kyc, a.beneficiary_account_name, a.beneficiary_account_number, a.beneficiary_kyc, a.name_enquiry_ref, a.txn_duration, a.response_date_time, a.response_code, a.transaction_date_time, a.amount, a.destination_node, "
//                    + "b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
//                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
//                    + "ON a.source_institution_code = b.institution_code "
//                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
//                    + "ON a.destination_institution_code = c.institution_code "
//                    + "WHERE a.session_id IN (" + sessionids + ")";
//            List<TransactionHalfModel> transactions = jdbcTemplate.query(SQL, new TransactionHalfMapper());
//            SQL = "SELECT a.session_id, a.originator_account_name, a.originator_account_number, a.originator_kyc, a.beneficiary_account_name, a.beneficiary_account_number, a.beneficiary_kyc, a.name_enquiry_ref, a.txn_duration, a.response_date_time, a.response_code, a.transaction_date_time, a.amount, a.destination_node, "
//                    + "b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
//                    + "FROM " + archiveTable() + " a "
//                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
//                    + "ON a.source_institution_code = b.institution_code "
//                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
//                    + "ON a.destination_institution_code = c.institution_code "
//                    + "WHERE a.session_id IN (" + sessionids + ")";
//            logger.info("SearchTransactionsForSessionIds() :: SQL query " + SQL);
//            List<TransactionHalfModel> transactions_s = secondJdbcTemplate.query(SQL, new TransactionHalfMapper());
//
//            logger.info("SearchTransactionsForSessionIds() :: Total transactions from bulk search fetched: " + transactions_s.size());
//            networkResponse.setCode(200);
//            networkResponse.setMessage("Transactions For Uploaded Session IDs");
//            transactions.addAll(transactions_s);
//            networkResponse.setData((ArrayList) transactions);
//            return responseManager.ResponseOk(networkResponse);
//        } catch (DataAccessException ex) {
//            logger.info("SearchTransactionsForSessionIds() :: Error occured while doing bulk search --> " + ex.getMessage());
//            System.out.println("error>>>>" + ex.getMessage());
//            return responseManager.ResponseInternalServerError();
//        }
//    }
    @Override
    public ResponseEntity SearchTransactionsForSessionIds(String sessionids) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            if (sessionids == null || sessionids.isBlank()) {
                networkResponse.setCode(400);
                networkResponse.setMessage("No valid session IDs provided.");
                return responseManager.ResponseBadRequest();
            }
            // Parse sessionids safely
            List<String> sessionIdList = Arrays.stream(sessionids.split(","))
                    .map(s -> s.replaceAll("'", "").trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (sessionIdList.isEmpty()) {
                networkResponse.setCode(400);
                networkResponse.setMessage("No valid session IDs provided.");
                return responseManager.ResponseBadRequest();
            }
            String inClause = sessionIdList.stream().map(s -> "?").collect(Collectors.joining(","));

            String selectFields = "a.session_id, a.originator_account_name, a.originator_account_number, a.originator_kyc, "
                    + "a.beneficiary_account_name, a.beneficiary_account_number, a.beneficiary_kyc, a.name_enquiry_ref, "
                    + "a.txn_duration, a.response_date_time, a.response_code, a.transaction_date_time, a.amount, a.destination_node, "
                    + "b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName ";

            String currentSQL = "SELECT " + selectFields
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.session_id IN (" + inClause + ")";

            String historyPrimarySQL = "SELECT " + selectFields
                    + "FROM " + archiveTable() + " a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                    + "WHERE a.session_id IN (" + inClause + ")";

            Object[] params = sessionIdList.toArray();
            List<TransactionHalfModel> result = new ArrayList<>();

            // Query each source
            result.addAll(jdbcTemplate.query(currentSQL, params, new TransactionHalfMapper()));
            result.addAll(jdbcTemplate.query(historyPrimarySQL, params, new TransactionHalfMapper()));
            result.addAll(secondJdbcTemplate.query(historyPrimarySQL, params, new TransactionHalfMapper()));

            // Deduplicate on session_id (optional)
            Map<String, TransactionHalfModel> deduped = new LinkedHashMap<>();
            for (TransactionHalfModel txn : result) {
                deduped.putIfAbsent(txn.getSessionid(), txn);
            }
            List<TransactionHalfModel> finalResults = new ArrayList<>(deduped.values());

            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions For Uploaded Session IDs");
            networkResponse.setData(new ArrayList<>(finalResults));
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info("SearchTransactionsForSessionIds() :: Error occurred while doing bulk search --> " + ex.getMessage());
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
            if (sessionids == null || sessionids.isBlank()) {
                networkResponse.setCode(400);
                networkResponse.setMessage("No session IDs provided.");
                return responseManager.ResponseBadRequest();
            }
            // 1. Parse/prepare session ID list for parameterized SQL
            List<String> sessionIdList = Arrays.stream(sessionids.split(","))
                    .map(s -> s.replaceAll("['\"]", "").trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            logger.info("sessionIdList after cleaning: " + sessionIdList);

            if (sessionIdList.isEmpty()) {
                networkResponse.setCode(400);
                networkResponse.setMessage("No session IDs provided.");
                return responseManager.ResponseBadRequest();
            }

            // For parameterization: (?, ?, ? ...)
            String inClause = sessionIdList.stream().map(s -> "?").collect(Collectors.joining(","));

            String commonSelect = "SELECT t.session_id, t.originator_account_name, t.originator_account_number, t.originator_kyc, "
                    + "t.beneficiary_account_name, t.beneficiary_account_number, t.beneficiary_kyc, t.name_enquiry_ref, "
                    + "t.txn_duration, t.response_date_time, t.response_code, t.transaction_date_time, t.amount, t.destination_node, "
                    + "b.institution_name AS srcInstitutionName, c.institution_name AS destInstitutionName ";

            String fromTpl = "FROM (SELECT session_id, originator_account_name, originator_account_number, originator_kyc, "
                    + "beneficiary_account_name, beneficiary_account_number, beneficiary_kyc, name_enquiry_ref, txn_duration, "
                    + "response_date_time, response_code, transaction_date_time, amount, destination_node, source_institution_code, "
                    + "destination_institution_code FROM %s WHERE session_id IN (" + inClause + ")) AS t "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b ON t.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c ON t.destination_institution_code = c.institution_code";

            // 2. Query all sources
            List<TransactionHalfModel> allResults = new ArrayList<>();

            // a) Live table - always in primary DB
            String sqlLive = commonSelect + String.format(fromTpl, "ajiswitch_db.tbl_creditfundtransfers");
            logger.info("Executing live transactions query: " + sqlLive);
            List<TransactionHalfModel> liveResults = jdbcTemplate.query(sqlLive, sessionIdList.toArray(), new TransactionHalfMapper());
            logger.info("Live query returned " + liveResults.size() + " rows");
            allResults.addAll(liveResults);

            // b) Archive table (primary DB)
            String sqlHistPrimary = commonSelect + String.format(fromTpl, archiveTable());
            logger.info("Executing history (primary) transactions query: " + sqlHistPrimary);
            List<TransactionHalfModel> histPrimary = jdbcTemplate.query(sqlHistPrimary, sessionIdList.toArray(), new TransactionHalfMapper());
            logger.info("History (primary) query returned " + histPrimary.size() + " rows");
            allResults.addAll(histPrimary);

            // c) Archive table (secondary DB)
            logger.info("Executing history (secondary) transactions query: " + sqlHistPrimary + " [on 10.83.1.14]");
            List<TransactionHalfModel> histSecondary = secondJdbcTemplate.query(sqlHistPrimary, sessionIdList.toArray(), new TransactionHalfMapper());
            logger.info("History (secondary) query returned " + histSecondary.size() + " rows");
            allResults.addAll(histSecondary);

            // 3. Deduplicate by session_id + transaction_date_time (if needed)
            Map<String, TransactionHalfModel> sessionIdMap = new LinkedHashMap<>();
            for (TransactionHalfModel txn : allResults) {
                String uniqueKey = txn.getSessionid() + "_" + txn.getTransactiondate(); // Use more fields if needed
                sessionIdMap.putIfAbsent(uniqueKey, txn);
            }
            List<TransactionHalfModel> combined = new ArrayList<>(sessionIdMap.values());

            // 4. Build response
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions For Uploaded Session IDs");
            networkResponse.setData((ArrayList) combined);
            logger.info("SearchTransactionsForSessionIds completed successfully, returning "
                    + combined.size() + " unique records");
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info("DataAccessException in SearchTransactionsForSessionIds: " + ex.getMessage());
            ex.printStackTrace();
            return responseManager.ResponseInternalServerError();
        }
    }

//    @Override
//    public ResponseEntity SearchTransactionsForSessionIds(String sessionids, String startDate, String endDate) {
//        NetworkResponse networkResponse = new NetworkResponse();
//        logger.info("Entering SearchTransactionsForSessionIds(sessionids=" + sessionids
//                + ", startDate=" + startDate + ", endDate=" + endDate + ")");
//        try {
//            String SQL = "SELECT t.session_id, t.originator_account_name, t.originator_account_number, t.originator_kyc, t.beneficiary_account_name, t.beneficiary_account_number, t.beneficiary_kyc, t.name_enquiry_ref, t.txn_duration, t.response_date_time, t.response_code, t.transaction_date_time, t.amount, t.destination_node, b.institution_name AS srcInstitutionName, c.institution_name AS destInstitutionName FROM (SELECT session_id, originator_account_name, originator_account_number, originator_kyc, beneficiary_account_name, beneficiary_account_number, beneficiary_kyc, name_enquiry_ref, txn_duration, response_date_time, response_code, transaction_date_time, amount, destination_node, source_institution_code, destination_institution_code FROM ajiswitch_db.tbl_creditfundtransfers WHERE session_id IN (" + sessionids.trim().replaceAll("\\s+", "") + ") ) AS t LEFT JOIN ajiswitch_db.tbl_nodes b ON t.source_institution_code=b.institution_code LEFT JOIN ajiswitch_db.tbl_nodes c ON t.destination_institution_code=c.institution_code;";
//            logger.info("Executing live transactions query: " + SQL);
//            List<TransactionHalfModel> transactions = jdbcTemplate.query(SQL, new TransactionHalfMapper());
//            logger.info("Live query returned " + transactions.size() + " rows");
//
//            SQL = "SELECT t.session_id, t.originator_account_name, t.originator_account_number, t.originator_kyc, t.beneficiary_account_name, t.beneficiary_account_number, t.beneficiary_kyc, t.name_enquiry_ref, t.txn_duration, t.response_date_time, t.response_code, t.transaction_date_time, t.amount, t.destination_node, b.institution_name AS srcInstitutionName, c.institution_name AS destInstitutionName FROM (SELECT session_id, originator_account_name, originator_account_number, originator_kyc, beneficiary_account_name, beneficiary_account_number, beneficiary_kyc, name_enquiry_ref, txn_duration, response_date_time, response_code, transaction_date_time, amount, destination_node, source_institution_code, destination_institution_code FROM " + archiveTable() + " WHERE session_id  IN (" + sessionids.trim().replaceAll("\\s+", "") + ")) AS t LEFT JOIN ajiswitch_db.tbl_nodes b ON t.source_institution_code=b.institution_code LEFT JOIN ajiswitch_db.tbl_nodes c ON t.destination_institution_code=c.institution_code;";
//            logger.info("Executing history transactions query: " + SQL);
//            List<TransactionHalfModel> history = jdbcTemplate.query(SQL, new TransactionHalfMapper());
//            logger.info("History query returned " + history.size() + " rows");
//            transactions.addAll(history);
//            logger.info("Total combined transactions: " + transactions.size());
////            }
//
//            // Build response
//            networkResponse.setCode(200);
//            networkResponse.setMessage("Transactions For Uploaded Session IDs");
//            networkResponse.setData((ArrayList) transactions);
//            logger.info("SearchTransactionsForSessionIds completed successfully, returning "
//                    + transactions.size() + " records");
//            return responseManager.ResponseOk(networkResponse);
//
//        } catch (DataAccessException ex) {
//            logger.info("DataAccessException in SearchTransactionsForSessionIds: " + ex.getMessage());
//            ex.printStackTrace();
//            return responseManager.ResponseInternalServerError();
//        }
//    }
    @Override
    public ResponseEntity GetInsitutionTnxTrend(String institutioncode, String type, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL = "";
            List<Map<String, Object>> trend;
            switch (type) {
                case "month":
                    SQL = "SELECT a.transaction_date_time as label, COUNT(a.id) as volume, SUM(a.amount) as value, b.institution_name, b.shortName, b.color, b.institution_code "
                            + "FROM " + archiveTable() + " a "
                            + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                            + "ON a.source_institution_code = b.institution_code "
                            + "WHERE a.response_code = '00' AND a.source_institution_code = ? "
                            + "AND a.transaction_date_time BETWEEN ? AND ?"
                            + "GROUP BY MONTH(a.transaction_date_time)";
                    break;
                case "day":
                default:
                    SQL = "SELECT a.transaction_date_time as label, COUNT(a.id) as volume, SUM(a.amount) as value, b.institution_name, b.shortName, b.color, b.institution_code "
                            + "FROM " + archiveTable() + " a "
                            + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                            + "ON a.source_institution_code = b.institution_code "
                            + "WHERE a.response_code = '00' AND a.source_institution_code = ? "
                            + "AND a.transaction_date_time BETWEEN ? AND ? "
                            + "GROUP BY CAST(a.transaction_date_time as DATE)";
                    break;
            }

            trend = secondJdbcTemplate.queryForList(SQL, new Object[]{institutioncode, startDate, endDate});

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
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName, "
                    + "a.destination_node "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
                    //                + "LEFT JOIN ajiswitch_db.tbl_transactions_routes n "
                    //                + "ON a.destination_node = n.port_number "
                    + "WHERE a.source_institution_code = ? OR a.destination_institution_code = ? ORDER BY a.id DESC";
            transactions = jdbcTemplate.query(SQL, new Object[]{institutioncode, institutioncode}, new FullTransactionMapper());

            SQL = "SELECT SUM(a.amount) as totalValue "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
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
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                        + "ON a.source_institution_code = b.institution_code "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                        + "ON a.destination_institution_code = c.institution_code "
                        + "WHERE a.id = ? ";
                transactions = jdbcTemplate.query(SQL, new Object[]{id}, new FullTransactionMapper());
            } else {
                SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                        + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                        + "ON a.source_institution_code = b.institution_code "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                        + "ON a.destination_institution_code = c.institution_code "
                        + "ORDER BY a.id DESC";
                transactions = jdbcTemplate.query(SQL, new FullTransactionMapper());

                SQL = "SELECT SUM(a.amount) as totalValue "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                        + "ON a.source_institution_code = b.institution_code "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                        + "ON a.destination_institution_code = c.institution_code ";
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
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                    + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
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
            String table = isCurrent ? "ajiswitch_db.tbl_creditfundtransfers a " : archiveTable() + " a ";
            SQL = "SELECT a.id, a.session_id, a.payment_reference, a.channel_code, a.originator_account_number, a.originator_account_name, a.originator_kyc, a.originator_bvn, a.amount, a.source_institution_code, a.session_id, a.response_code, a.beneficiary_account_number, "
                    + "a.beneficiary_account_name, a.beneficiary_kyc, a.beneficiary_bvn, a.amount, a.destination_institution_code, a.response_code, a.narration, a.transaction_date_time, a.name_enquiry_ref, a.txn_duration, a.response_date_time, b.institution_name as srcInstitutionName, c.institution_name as destInstitutionName "
                    + "FROM " + table
                    + "LEFT JOIN ajiswitch_db.tbl_nodes b "
                    + "ON a.source_institution_code = b.institution_code "
                    + "LEFT JOIN ajiswitch_db.tbl_nodes c "
                    + "ON a.destination_institution_code = c.institution_code "
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
        // correlation id to track this request in logs
        final String corrId = java.util.UUID.randomUUID().toString();
        final long tStart = System.currentTimeMillis();
        try {
            String code = institutioncode != null ? institutioncode : "";
            String SQL = null, SQL2 = null;
            List<DisputeModel> transactions;
            Double totalValue;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> agg = null;

            logger.info(String.format("🟢 [corr=%s] Entering GetDisputes | id=%d | status=%d | institutioncode='%s' | page=%d | limit=%d | offset=%d",
                    corrId, id, status, code, page, limit, offset));

            if (id > 0) {
                SQL = "SELECT dispute.id, dispute.transactionSessionid as session_id, dispute.transactionid, dispute.amount, dispute.originator_account_name, dispute.beneficiary_account_name, dispute.transaction_date_time, dispute.ownerInstitutionName as srcInstitutionName, dispute.destInstitutionName, dispute.loggedBy, dispute.resolvedBy, dispute.ownerInstitution, dispute.destInstitution, dispute.type, dispute.status, dispute.resolved, dispute.date_modified, dispute.date_created, dispute.timeline_date, dispute.proof_of_reject_uri, a.financial_institution_code "
                        + "FROM tbl_disputes dispute "
                        + "LEFT JOIN tbl_financial_institution_contacts a "
                        + "ON dispute.loggedBy = a.email_address "
                        + "WHERE dispute.id = ?";
                logger.info(String.format("🧩 [corr=%s] Built SQL (id path).", corrId));
                logger.info(String.format("🧾 [corr=%s] SQL: %s", corrId, SQL));
                Object[] params = new Object[]{id};
                logger.info(String.format("🔢 [corr=%s] JDBC params: %s", corrId, java.util.Arrays.toString(params)));
                transactions = jdbcTemplate.query(SQL, params, new DisputeTransactionMapper());
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
                } // end switch

                logger.info(String.format("🧩 [corr=%s] Built SQL (non-id path).", corrId));
                logger.info(String.format("🧾 [corr=%s] SQL: %s", corrId, SQL));
                if (SQL2 != null) logger.info(String.format("🧾 [corr=%s] SQL2: %s", corrId, SQL2));

                Object[] params = new Object[]{limit, offset};
                logger.info(String.format("🔢 [corr=%s] JDBC params for list query: %s", corrId, java.util.Arrays.toString(params)));

                transactions = jdbcTemplate.query(SQL, params, new DisputeTransactionMapper());
                if (SQL2 != null) {
                    agg = jdbcTemplate.queryForList(SQL2);
                } else {
                    agg = new java.util.ArrayList<>();
                    Map<String, Object> emptyAgg = new java.util.HashMap<>();
                    emptyAgg.put("totalValue", null);
                    emptyAgg.put("totalRecords", 0L);
                    agg.add(emptyAgg);
                }
            } // end id/non-id

            long tAfterQuery = System.currentTimeMillis();
            logger.info(String.format("⏱ [corr=%s] Query executed in %dms. Retrieved rows: %d", corrId, (tAfterQuery - tStart), (transactions != null ? transactions.size() : 0)));

            // defensive: if agg is null or empty, provide defaults
            if (agg == null || agg.isEmpty()) {
                logger.info(String.format("⚠️ [corr=%s] Aggregation result empty or null - setting totalValue=0, totalRecords=0", corrId));
                agg = new java.util.ArrayList<>();
                Map<String, Object> emptyAgg = new java.util.HashMap<>();
                emptyAgg.put("totalValue", null);
                emptyAgg.put("totalRecords", 0L);
                agg.add(emptyAgg);
            }

            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            totalValue = tValue != null ? tValue.doubleValue() : 0;
            Object totalRecordsObj = row.get("totalRecords");
            int totalRecords;
            if (totalRecordsObj instanceof Number) {
                totalRecords = ((Number) totalRecordsObj).intValue();
            } else {
                try {
                    totalRecords = Integer.parseInt(String.valueOf(totalRecordsObj));
                } catch (Exception e) {
                    totalRecords = 0;
                }
            }

            logger.info(String.format("📊 [corr=%s] Aggregation: totalValue=%s totalRecords=%d", corrId, totalValue, totalRecords));

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

            long tEnd = System.currentTimeMillis();
            logger.info(String.format("✅ [corr=%s] Completed GetDisputes successfully in %dms | returnedRows=%d", corrId, (tEnd - tStart), (transactions != null ? transactions.size() : 0)));
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            // log exception details and stacktrace (but still use logger.info)
            logger.info(String.format("❌ [corr=%s] DataAccessException in GetDisputes: %s", corrId, ex.toString()));
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            logger.info(String.format("❌ [corr=%s] Stacktrace: %s", corrId, sw.toString()));
            return responseManager.ResponseInternalServerError();
        } catch (Exception ex) {
            // catch-all to ensure stacktrace is logged
            logger.info(String.format("❌ [corr=%s] Unexpected exception in GetDisputes: %s", corrId, ex.toString()));
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            logger.info(String.format("❌ [corr=%s] Stacktrace: %s", corrId, sw.toString()));
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
        logger.info("LogDisputesBulk called with sessiontoken=" + ""
                + ", records=" + records
                + ", sourceInstitution=" + sourceInstitution
                + ", username=" + username);
        try {
            JSONArray jsonRecords = new JSONArray(records);
            int found = 0;
            int recorded = 0;

            logger.info("Parsed " + jsonRecords.length() + " records for bulk dispute");

            for (int i = 0; i < jsonRecords.length(); i++) {
                String sessionId = jsonRecords.getJSONObject(i).getString("sessionid");
                logger.info("Processing record index=" + i + " with sessionId=" + sessionId);

                boolean sessionIdExist = CheckSessionId(sessionId);
                logger.info("sessionIdExist=" + sessionIdExist + " for sessionId=" + sessionId);

                if (!sessionIdExist) {
                    // 1. Try history
                    List<FullTransactionModel> getTransaction = GetTransactionFromHistory(sessionId, sourceInstitution);
                    logger.info("GetTransactionFromHistory returned " + getTransaction.size() + " records for sessionId=" + sessionId);

                    // 2. Fallback to primary if none found in history
                    if (getTransaction.isEmpty()) {
                        logger.info("No history record; querying primary table for sessionId=" + sessionId + ", institution=" + sourceInstitution);
                        getTransaction = GetTransactionFromPrimary(sessionId, sourceInstitution);
                        logger.info("GetTransactionFromPrimary returned " + getTransaction.size() + " records for sessionId=" + sessionId);
                    }

                    if (!getTransaction.isEmpty()) {
                        found++;
                        FullTransactionModel txn = getTransaction.get(0);
                        String respCode = txn.getSrcResponsecode();
                        logger.info("Txn responseCode=" + respCode + " for sessionId=" + sessionId);

                        if ("00".equals(respCode)) {
                            int additionalDays = dateUtil.getDisputeTimeLineDate();
                            String insertSql = ""
                                    + "INSERT INTO tbl_disputes("
                                    + "transactionSessionid, transactionid, amount, originator_account_name, "
                                    + "beneficiary_account_name, transaction_date_time, loggedBy, ownerInstitution, "
                                    + "destInstitution, ownerInstitutionName, destInstitutionName, status, date_created, timeline_date"
                                    + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', NOW(), ADDDATE(NOW(), ?))";
                            logger.info("Inserting dispute for sessionId=" + sessionId + ", transactionId=" + txn.getId());
                            int retval = jdbcTemplate.update(insertSql,
                                    sessionId,
                                    txn.getId(),
                                    txn.getSrcAmount(),
                                    txn.getSrcAccountName(),
                                    txn.getDestAccountName(),
                                    txn.getTransactiondate(),
                                    username,
                                    txn.getSrcInstitutioncode(),
                                    txn.getDestInstitutioncode(),
                                    txn.getSrcInstitutionName(),
                                    txn.getDestInstitutionName(),
                                    additionalDays
                            );
                            logger.info("Insert returned " + retval + " for sessionId=" + sessionId + "\n");

                            if (retval > 0) {
                                recorded++;
                                if (txn.getDestInstitutioncode().equals(sourceInstitution)) {
                                    String updateSql = ""
                                            + "UPDATE tbl_disputes "
                                            + "SET resolvedBy = ?, status = '0', resolved = '0', date_modified = NOW() "
                                            + "WHERE transactionSessionid = ?";
                                    logger.info("Auto-resolving dispute for own institution, sessionId=" + sessionId);
                                    jdbcTemplate.update(updateSql, username, sessionId);
                                }
                            }
                        } else {
                            logger.info("Skipping dispute insert for sessionId=" + sessionId + " due to non-00 response code" + "\n");
                        }
                    } else {
                        logger.info("No transaction found for sessionId=" + sessionId + " in history or primary" + "\n");
                    }
                } else {
                    logger.info("Skipping sessionId=" + sessionId + " because dispute already exists" + "\n");
                }
            }

            logger.info("Bulk processing complete: totalRecords=" + jsonRecords.length()
                    + ", validRecords=" + found
                    + ", recorded=" + recorded);

            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(
                    "Total Records: " + jsonRecords.length()
                    + "\nValid Records: " + found
                    + "\nRecorded: " + recorded
            );
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            logger.info("DataAccessException in LogDisputesBulk: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        } catch (JSONException ex) {
            logger.info("JSONException in LogDisputesBulk: " + ex.getMessage());
            return responseManager.ResponseBadRequest();
        }
    }

    @Override
    public ResponseEntity LogDispute(String sessiontoken, String sessionId, String amount, String wallet, String sourceInstitution, String type, String username) {
        logger.info("LogDispute called with sessionId=" + sessionId + ", amount=" + amount + ", wallet=" + wallet + ", sourceInstitution=" + sourceInstitution + ", type=" + type + ", username=" + username);
        try {
            // 1. History lookup
            logger.info("Looking up history for sessionId=" + sessionId + ", institution=" + sourceInstitution);
            List<FullTransactionModel> getTransaction = GetTransactionFromHistory(sessionId, sourceInstitution);
            if (getTransaction.isEmpty()) {
                // 2. Fallback to primary
                logger.info("No history record; querying primary table for sessionId=" + sessionId + ", institution=" + sourceInstitution);
                getTransaction = GetTransactionFromPrimary(sessionId, sourceInstitution);
                logger.info("Primary table returned " + getTransaction.size() + " records");
            }

            // 3. Process result
            if (!getTransaction.isEmpty()) {
                FullTransactionModel txn = getTransaction.get(0);
                logger.info("First txn responseCode=" + txn.getSrcResponsecode());
                if (!"00".equals(txn.getSrcResponsecode())) {
                    logger.info("Declined txn; cannot dispute, responseCode=" + txn.getSrcResponsecode());
                    NetworkResponse nr = new NetworkResponse();
                    nr.setCode(404);
                    nr.setStatus("failed");
                    nr.setMessage("Declined Transaction cannot be logged for dispute");
                    return responseManager.ResponseOk(nr);
                }

                int additionalDays = dateUtil.getDisputeTimeLineDate();
                String insertSql = "INSERT INTO tbl_disputes("
                        + "transactionSessionid, transactionid, amount, originator_account_name, "
                        + "beneficiary_account_name, transaction_date_time, loggedBy, ownerInstitution, "
                        + "destInstitution, ownerInstitutionName, destInstitutionName, status, date_created, timeline_date"
                        + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '-1', NOW(), ADDDATE(NOW(), ?))";
                logger.info("Inserting dispute for sessionId=" + sessionId + ", transactionId=" + txn.getId());
                int retval = jdbcTemplate.update(insertSql,
                        sessionId,
                        txn.getId(),
                        txn.getSrcAmount(),
                        txn.getSrcAccountName(),
                        txn.getDestAccountName(),
                        txn.getTransactiondate(),
                        username,
                        txn.getSrcInstitutioncode(),
                        txn.getDestInstitutioncode(),
                        txn.getSrcInstitutionName(),
                        txn.getDestInstitutionName(),
                        additionalDays
                );
                logger.info("Insert returned " + retval);
                if (retval > 0) {
                    if (txn.getDestInstitutioncode().equals(sourceInstitution)) {
                        String updateSql = "UPDATE tbl_disputes SET resolvedBy = ?, status = '0', resolved = '0', date_modified = NOW() WHERE transactionSessionid = ?";
                        logger.info("Auto-resolving own-institution dispute for sessionId=" + sessionId);
                        jdbcTemplate.update(updateSql, username, sessionId);
                    }
                    logger.info("Dispute logged successfully for sessionId=" + sessionId);
                    return responseManager.ResponseAccepted();
                } else {
                    logger.info("Failed to insert dispute record for sessionId=" + sessionId);
                    return responseManager.ResponseInternalServerError();
                }
            }

            // 4. Nothing found anywhere
            logger.info("Transaction not found in history or primary for sessionId=" + sessionId);
            NetworkResponse nr = new NetworkResponse();
            nr.setCode(404);
            nr.setStatus("failed");
            nr.setMessage("Transaction not found");
            return responseManager.ResponseOk(nr);

        } catch (DataAccessException ex) {
            // catch and log only with info
            logger.info("DataAccessException in LogDispute for sessionId=" + sessionId + ": " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    class DisputeTransactionMapper implements RowMapper<DisputeModel> {

        @Override
        public DisputeModel mapRow(ResultSet rs, int arg1) throws SQLException {
            ResponseCodeInterpreter responseCodeInterpreter = new ResponseCodeInterpreter();
            DisputeModel response = new DisputeModel();
            response.setId(new BigInteger(rs.getString("id")));
            response.setTransactionId(new BigInteger(rs.getString("transactionid")));//(rs.getInt("transactionid"));
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
            response.setCode(ColumnExistinRS(rs, "institution_code") ? rs.getString("institution_code") : "-1");
            response.setName(ColumnExistinRS(rs, "institution_name") ? rs.getString("institution_name") : "Other Banks");
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
            response.setId(new BigInteger(rs.getString("id")));
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
    public ResponseEntity ApproveSettlement(String sessiontoken, BigInteger id, String username, int status, String proof_of_reject_uri, String selectedDisputes, String type) {
        logger.info("ApproveSettlement called with sessiontoken=" + ""
                + ", id=" + id
                + ", username=" + username
                + ", status=" + status
                + ", proof_of_reject_uri=" + proof_of_reject_uri
                + ", selectedDisputes=" + selectedDisputes
                + ", type=" + type);
        try {
            int retVal = 0;
            int resolved = status == 0 ? 0 : 1;
            String SQL = "UPDATE tbl_disputes SET resolvedBy = ?, status = ?, resolved = ?, date_modified = now(), proof_of_reject_uri = ? WHERE id = ?";

            if ("bulk".equals(type)) {
                logger.info("Processing bulk approval for disputes: " + selectedDisputes);
                String[] idS = selectedDisputes.split(",");
                for (String _id : idS) {
                    logger.info("Updating dispute id=" + _id);
                    int _retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, _id});
                    logger.info("Update returned " + _retVal + " for id=" + _id);
                    retVal += _retVal;
                }
                logger.info("Bulk update total affected records: " + retVal);
                if (retVal > 0) {
                    logger.info("Bulk approve succeeded, total " + retVal + " disputes");
                    return responseManager.ResponseAccepted("Total of " + retVal + " dispute has been accepted");
                } else {
                    logger.info("Bulk approve found no records to update");
                    return responseManager.ResponseBadRequest();
                }
            } else {
                logger.info("Processing single approval for dispute id=" + id);
                retVal = jdbcTemplate.update(SQL, new Object[]{username, status, resolved, proof_of_reject_uri, id});
                logger.info("Update returned " + retVal + " for id=" + id);
                if (retVal > 0) {
                    logger.info("Single approve succeeded for id=" + id);
                    return responseManager.ResponseAccepted();
                } else {
                    logger.info("Single approve found no record to update for id=" + id);
                    return responseManager.ResponseBadRequest();
                }
            }
        } catch (DataAccessException ex) {
            logger.info("DataAccessException in ApproveSettlement for id=" + id + ": " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    class TransactionMapper implements RowMapper<TransactionModel> {

        @Override
        public TransactionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            TransactionModel response = new TransactionModel();
            ResponseCodeInterpreter responseCodeInterpreter = new ResponseCodeInterpreter();
            response.setId(new BigInteger(rs.getString("id")));
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

//    @Override
//    @Transactional
//    public ResponseEntity RequestTransactionStatusChange(
//            String sessionid,
//            String sessiontoken,
//            String username,
//            String status) {
//
//        NetworkResponse networkResponse = new NetworkResponse();
//        logger.info("Entering RequestTransactionStatusChange(sessionid=" + sessionid
//                + ", username=" + username + ", status=" + status + ")");
//
//        try {
//            int userrole = GetUserRole(username, sessiontoken);
//            logger.info("Retrieved user role: " + userrole);
//
//            List<String> sessionIds = Arrays.stream(sessionid.split(","))
//                    .map(String::trim)
//                    .collect(Collectors.toList());
//            logger.info("Session IDs to process: " + sessionIds);
//
//            for (String sid : sessionIds) {
//                logger.info("Processing sessionId: " + sid);
//                boolean isCurrent = false;
//
//                logger.info(String.format("%s :: Initial isCurrent = %s", sid, isCurrent));
//
//                String SQL = "SELECT a.*, b.institution_name as source_institution_name, c.institution_name as destination_institution_name "
//                        + "FROM " + archiveTable() + " a "
//                        + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
//                        + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
//                        + "WHERE a.session_id = ? AND a.response_code = '09'";
//                logger.info(String.format("%s :: SQL Query: %s", sid, SQL));
//                List<Map<String, Object>> rows = secondJdbcTemplate.queryForList(SQL, new Object[]{sid});
//                logger.info(String.format("%s :: History table rows found:: %s", sid, rows.size()));
//
//                if (rows.isEmpty()) {
//                    logger.info(String.format("%s :: No history rows, querying live table instead.", sid));
//                    SQL = "SELECT a.*, b.institution_name as source_institution_name, c.institution_name as destination_institution_name "
//                            + "FROM ajiswitch_db.tbl_creditfundtransfers a "
//                            + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
//                            + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
//                            + "WHERE a.session_id = ? AND a.response_code = '09'";
//                    logger.info(String.format("%s :: Finding transaction SQL Query: %s", sid, SQL));
//                    rows = jdbcTemplate.queryForList(SQL, new Object[]{sid});
//                    isCurrent = true;
//                    logger.info(String.format("%s :: Live table rows found:  %s, isCurrent = %s", sid, rows.size(), isCurrent));
//                }
//
//                if (!rows.isEmpty()) {
//                    Map<String, Object> txn = rows.get(0);
//                    logger.info(String.format("%s :: Found txn record: %s", sid, txn));
//
//                    // only if no existing status row
//                    String checkStatusSql = "SELECT 1 FROM ajiswitch_db.tbl_transactions_status WHERE session_id = ?";
//                    List<Map<String, Object>> rows2 = jdbcTemplate.queryForList(checkStatusSql, sid);
//                    if (rows2.isEmpty()) {
//                        logger.info(String.format("%s :: No existing status row", sid));
//                        logger.info(String.format("%s :: Status --> %s", sid, status));
//                        int retVal = 0;
//                        switch (userrole) {
//                            case 1:
//                                if (!status.equals("00")) {
//                                    logger.info(String.format("%s :: Status is not success, so proceeding to reverse transaction amount into wallet", sid));
//                                    // --- NEW: 1) lookup walletnumber for this institution ---
//                                    String nodeSql = "SELECT walletnumber, institution_name "
//                                            + "FROM ajiswitch_db.tbl_nodes "
//                                            + "WHERE institution_code = ? AND is_active = 1";
//                                    String sourceInst = txn.get("source_institution_code").toString();
//                                    List<Map<String, Object>> nodeRows = jdbcTemplate.queryForList(nodeSql, sourceInst);
//                                    if (!nodeRows.isEmpty()) {
//                                        int walletUpd = 0;
//                                        BigDecimal amount = BigDecimal.ZERO.setScale(2);
//                                        String walletNumber = nodeRows.get(0).get("walletnumber").toString();
//                                        logger.info(String.format("%s :: Found walletNumber= %s for institution= %s", sid, walletNumber, sourceInst));
//                                        if (walletNumber != null && walletNumber.matches("\\d{10}")) {
//                                            // --- 2) credit the wallet ---
//                                            amount = new BigDecimal(txn.get("amount").toString());
//                                            logger.info(String.format("%s :: Transaction amount to be reversed to wallet: %s", sid, amount));
//                                            String walletUpdateSql = "UPDATE ajiswitch_db.tbl_wallets "
//                                                    + "SET balance = balance + ? "
//                                                    + "WHERE walletnumber = ?";
//                                            logger.info(String.format("%s :: Reversal Query: %s", sid, walletUpdateSql));
//                                            walletUpd = jdbcTemplate.update(walletUpdateSql, amount, walletNumber);
//                                            logger.info(String.format("%s :: Wallet update rowsAffected= %s", sid, walletUpd));
//                                        }
//
//                                        // --- 3) log wallet activity ---
//                                        if (walletUpd > 0) {
//                                            String activitySql = "INSERT INTO ajiswitch_db.tbl_wallet_activities "
//                                                    + "(walletnumber, amount, credit_or_debit, actor, activity_date_time, session_id) "
//                                                    + "VALUES (?, ?, 'CR', 'SYSTEM', now(), ?)";
//                                            logger.info(String.format("%s :: Reversal record Query --> %s", sid, activitySql));
//                                            int actIns = jdbcTemplate.update(activitySql, walletNumber, amount, sid);
//                                            logger.info(String.format("%s :: Wallet activity insert rowsAffected= %s", sid, actIns));
//                                        }
//
//                                    } else {
//                                        logger.info(String.format("%s :: No active node found for institution= %s; skipping wallet steps", sid, sourceInst));
//                                    }
//                                } else {
//                                    logger.info("Status is success");
//                                    logger.info(String.format("%s :: Status is success", sid));
//                                }
//                                // --- THEN do your existing insert into tbl_transactions_status ---
//
//                                logger.info(String.format("%s :: User role = Admin, inserting with approved_by = requested_by.", sid));
//                                SQL = "INSERT INTO ajiswitch_db.tbl_transactions_status "
//                                        + "(session_id, requested_by, approved_by, current_status, new_status, approved_at, amount, transaction_date_time, originator_account_name, beneficiary_account_name, source_institution_code, destination_institution_code, source_institution_name, destination_institution_name, status) "
//                                        + "VALUES(?, ?, ?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, 1)";
//                                logger.info(String.format("%s :: Insert transaction status Query: %s", sid, SQL));
//                                retVal = jdbcTemplate.update(SQL, new Object[]{
//                                    sid, username, username,
//                                    rows.get(0).get("response_code"), status,
//                                    rows.get(0).get("amount"), rows.get(0).get("transaction_date_time"),
//                                    rows.get(0).get("originator_account_name"), rows.get(0).get("beneficiary_account_name"),
//                                    rows.get(0).get("source_institution_code"), rows.get(0).get("destination_institution_code"),
//                                    rows.get(0).get("source_institution_name"), rows.get(0).get("destination_institution_name")
//                                });
//                                logger.info(String.format("%s :: Admin insert returned: %s", sid, retVal));
//                                if (retVal > 0) {
//                                    logger.info(String.format("%s :: retVal for Insert greater than 0:  %s", sid, retVal));
//                                    retVal = 0;
//                                    logger.info(String.format("%s :: retVal set back to 0: %s", sid, retVal));
//                                    String tnxTable = isCurrent
//                                            ? "ajiswitch_db.tbl_creditfundtransfers"
//                                            : archiveTable();
//                                    logger.info(String.format("%s :: Updating response_code in table: %s. Status transaction is to be updated to --> %s", sid, tnxTable, status));
//                                    SQL = "UPDATE " + tnxTable + " SET response_code = ? WHERE session_id = ?";
//                                    logger.info(String.format("%s :: SQL Query to update response code: %s", sid, SQL));
//                                    int upd = 0;
//                                    if (tnxTable.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers")) {
//                                        upd = jdbcTemplate.update(SQL, new Object[]{status, sid});
//
//                                    } else if (tnxTable.equalsIgnoreCase(archiveTable())) {
//                                        upd = secondJdbcTemplate.update(SQL, new Object[]{status, sid});
//                                    } else {
//                                        String msg = String.format("Unknown transaction table '%s'—cannot update response_code", tnxTable);
//                                        logger.info(msg);
//                                        throw new IllegalStateException(msg);
//                                    }
//
//                                    logger.info(String.format("%s :: Update on  %s returned: %s", sid, tnxTable, upd));
//                                    if (upd > 0) {
//                                        String delRetrySql = "DELETE FROM ajiswitch_db.tbl_tsq_retry WHERE session_id = ?";
//                                        logger.info(String.format("%s :: Deleting record from TSQ retry table :: Query --> %s", sid, delRetrySql));
//                                        int del = jdbcTemplate.update(delRetrySql, sid);
//                                        logger.info(String.format("%s :: Deleted from tsq_retry rowsAffected= %s", sid, del));
//                                    }
//
//                                }
//                                break;
//                            default:
//                                logger.info("User role = " + userrole + ", inserting without approved_by.");
//                                SQL = "INSERT INTO ajiswitch_db.tbl_transactions_status "
//                                        + "(session_id, requested_by, current_status, new_status, amount, transaction_date_time, originator_account_name, beneficiary_account_name, source_institution_code, destination_institution_code, source_institution_name, destination_institution_name) "
//                                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//                                logger.info(String.format("%s :: Insert transaction status Query: %s", sid, SQL));
//                                retVal = jdbcTemplate.update(SQL, new Object[]{
//                                    sid, username,
//                                    rows.get(0).get("response_code"), status,
//                                    rows.get(0).get("amount"), rows.get(0).get("transaction_date_time"),
//                                    rows.get(0).get("originator_account_name"), rows.get(0).get("beneficiary_account_name"),
//                                    rows.get(0).get("source_institution_code"), rows.get(0).get("destination_institution_code"),
//                                    rows.get(0).get("source_institution_name"), rows.get(0).get("destination_institution_name")
//                                });
//                                logger.info(String.format("%s :: Default insert returned: %s", sid, retVal));
//                                break;
//                        }
//
//                    } else {
//                        logger.info("Status row already exists for sessionId=" + sid + "; skipping insert/update.");
//                    }
//                } else {
//                    logger.info("No transaction record found for sessionId=" + sid + "; skipping entirely.");
//                }
//            }
//
//            networkResponse.setCode(200);
//            networkResponse.setStatus("success");
//            networkResponse.setMessage(userrole == 1
//                    ? "Transaction(s) status updated"
//                    : "Transaction(s) status update submitted");
//            logger.info("RequestTransactionStatusChange completed successfully: " + networkResponse.getMessage());
//            return responseManager.ResponseOk(networkResponse);
//
//        } catch (DataAccessException ex) {
//            logger.info("DataAccessException in RequestTransactionStatusChange: " + ex.getMessage());
//            // Because @Transactional, this will roll back everything we did above
//            return responseManager.ResponseInternalServerError();
//        }
//    }
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

            // Admin-only, immediate apply — no approval queue on Belema.
            if (userrole != 1) {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Only an administrator can change transaction status");
                return responseManager.ResponseOk(networkResponse);
            }

            String newCode = status == null ? "" : status.trim();
            // Accept UI labels as well as ISO response codes.
            if ("Successful".equalsIgnoreCase(newCode) || "Success".equalsIgnoreCase(newCode)) {
                newCode = "00";
            } else if ("Failed".equalsIgnoreCase(newCode) || "Failure".equalsIgnoreCase(newCode)) {
                newCode = "91";
            } else if ("Pending".equalsIgnoreCase(newCode)) {
                newCode = "09";
            }
            if (newCode.isEmpty() || !newCode.matches("\\d{2}")) {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Invalid target status. Use Successful/Failed or a 2-digit response code.");
                return responseManager.ResponseOk(networkResponse);
            }
            if (sessionid == null || sessionid.trim().isEmpty()) {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Session id is required");
                return responseManager.ResponseOk(networkResponse);
            }

            Set<String> sessionIds = Arrays.stream(sessionid.split(","))
                    .map(s -> s.replaceAll("['\"]", "").trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            int updatedCount = 0;
            for (String sid : sessionIds) {
                Map<String, Object> txn = null;
                String foundTable = null;
                JdbcTemplate sourceJdbc = null;

                String SQL = "SELECT a.*, b.institution_name as source_institution_name, c.institution_name as destination_institution_name "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                        + "WHERE a.session_id = ? AND a.response_code = '09'";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, sid);
                if (!rows.isEmpty()) {
                    txn = rows.get(0);
                    foundTable = "ajiswitch_db.tbl_creditfundtransfers";
                    sourceJdbc = jdbcTemplate;
                }

                if (txn == null) {
                    SQL = "SELECT a.*, b.institution_name as source_institution_name, c.institution_name as destination_institution_name "
                            + "FROM " + archiveTable() + " a "
                            + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                            + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                            + "WHERE a.session_id = ? AND a.response_code = '09'";
                    rows = jdbcTemplate.queryForList(SQL, sid);
                    if (!rows.isEmpty()) {
                        txn = rows.get(0);
                        foundTable = archiveTable();
                        sourceJdbc = jdbcTemplate;
                    }
                }

                if (txn == null && hasSeparateArchive()) {
                    rows = secondJdbcTemplate.queryForList(SQL, sid);
                    if (!rows.isEmpty()) {
                        txn = rows.get(0);
                        foundTable = archiveTable();
                        sourceJdbc = secondJdbcTemplate;
                    }
                }

                if (txn == null) {
                    logger.info(String.format("%s :: No pending (09) transaction found; skipping.", sid));
                    continue;
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(txn.get("amount").toString());
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        logger.info(String.format("%s :: Invalid amount %s; skipping.", sid, amount));
                        continue;
                    }
                } catch (Exception e) {
                    logger.info(String.format("%s :: Could not parse amount; skipping.", sid));
                    continue;
                }

                // When marking failed (not success), credit source institution wallet (legacy admin behaviour).
                if (!"00".equals(newCode)) {
                    Object sourceInstObj = txn.get("source_institution_code");
                    if (sourceInstObj != null) {
                        String sourceInst = sourceInstObj.toString();
                        String nodeSql = "SELECT walletnumber, institution_name "
                                + "FROM ajiswitch_db.tbl_nodes "
                                + "WHERE institution_code = ? AND is_active = 1";
                        List<Map<String, Object>> nodeRows = jdbcTemplate.queryForList(nodeSql, sourceInst);
                        if (!nodeRows.isEmpty() && nodeRows.get(0).get("walletnumber") != null) {
                            String walletNumber = nodeRows.get(0).get("walletnumber").toString();
                            if (walletNumber.matches("\\d{10}")) {
                                int walletUpd = jdbcTemplate.update(
                                        "UPDATE ajiswitch_db.tbl_wallets SET balance = balance + ? WHERE walletnumber = ?",
                                        amount, walletNumber);
                                if (walletUpd > 0) {
                                    try {
                                        jdbcTemplate.update(
                                                "INSERT INTO ajiswitch_db.tbl_wallet_activities "
                                                        + "(walletnumber, amount, credit_or_debit, actor, activity_date_time, session_id) "
                                                        + "VALUES (?, ?, 'CR', ?, now(), ?)",
                                                walletNumber, amount, username, sid);
                                    } catch (DataAccessException actEx) {
                                        logger.info(String.format("%s :: Wallet activity insert skipped: %s", sid, actEx.getMessage()));
                                    }
                                }
                            }
                        }
                    }
                }

                String updateSql = "UPDATE " + foundTable + " SET response_code = ? WHERE session_id = ? AND response_code = '09'";
                int upd = (sourceJdbc != null ? sourceJdbc : jdbcTemplate).update(updateSql, newCode, sid);
                logger.info(String.format("%s :: response_code update rowsAffected=%d -> %s", sid, upd, newCode));
                if (upd > 0) {
                    updatedCount++;
                    try {
                        jdbcTemplate.update("DELETE FROM ajiswitch_db.tbl_tsq_retry WHERE session_id = ?", sid);
                    } catch (DataAccessException delEx) {
                        logger.info(String.format("%s :: tsq_retry delete skipped: %s", sid, delEx.getMessage()));
                    }
                }
            }

            networkResponse.setCode(200);
            if (updatedCount > 0) {
                networkResponse.setStatus("success");
                networkResponse.setMessage("Transaction status updated (" + updatedCount + ")");
            } else {
                networkResponse.setStatus("failed");
                networkResponse.setMessage("No pending transaction was updated. Confirm the session id has response code 09.");
            }
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.info("DataAccessException in RequestTransactionStatusChange: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        } catch (Exception ex) {
            logger.info("Exception in RequestTransactionStatusChange: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @SuppressWarnings("unused")
    private ResponseEntity RequestTransactionStatusChangeLegacy(
            String sessionid,
            String sessiontoken,
            String username,
            String status) {

        NetworkResponse networkResponse = new NetworkResponse();
        try {
            int userrole = GetUserRole(username, sessiontoken);
            logger.info("Retrieved user role: " + userrole);

            // Deduplicate and clean session IDs
            Set<String> sessionIds = Arrays.stream(sessionid.split(","))
                    .map(s -> s.replaceAll("['\"]", "").trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            logger.info("Session IDs to process: " + sessionIds);

            for (String sid : sessionIds) {
                logger.info("Processing sessionId: " + sid);

                Map<String, Object> txn = null;
                String foundTable = null;
                JdbcTemplate sourceJdbc = null;

                // 1. Try live table (primary DB)
                String SQL = "SELECT a.*, b.institution_name as source_institution_name, c.institution_name as destination_institution_name "
                        + "FROM ajiswitch_db.tbl_creditfundtransfers a "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                        + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                        + "WHERE a.session_id = ? AND a.response_code = '09'";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, sid);
                if (!rows.isEmpty()) {
                    txn = rows.get(0);
                    foundTable = "ajiswitch_db.tbl_creditfundtransfers";
                    sourceJdbc = jdbcTemplate;
                    logger.info(String.format("%s :: Found in current table.", sid));
                }

                // 2. Try archive (primary DB) if not found
                if (txn == null) {
                    SQL = "SELECT a.*, b.institution_name as source_institution_name, c.institution_name as destination_institution_name "
                            + "FROM " + archiveTable() + " a "
                            + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                            + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                            + "WHERE a.session_id = ? AND a.response_code = '09'";
                    rows = jdbcTemplate.queryForList(SQL, sid);
                    if (!rows.isEmpty()) {
                        txn = rows.get(0);
                        foundTable = archiveTable();
                        sourceJdbc = jdbcTemplate;
                        logger.info(String.format("%s :: Found in archive (primary DB).", sid));
                    }
                }

                // 3. Try archive (secondary DB) if still not found
                if (txn == null) {
                    SQL = "SELECT a.*, b.institution_name as source_institution_name, c.institution_name as destination_institution_name "
                            + "FROM " + archiveTable() + " a "
                            + "LEFT JOIN ajiswitch_db.tbl_nodes b ON a.source_institution_code = b.institution_code "
                            + "LEFT JOIN ajiswitch_db.tbl_nodes c ON a.destination_institution_code = c.institution_code "
                            + "WHERE a.session_id = ? AND a.response_code = '09'";
                    rows = secondJdbcTemplate.queryForList(SQL, sid);
                    if (!rows.isEmpty()) {
                        txn = rows.get(0);
                        foundTable = archiveTable();
                        sourceJdbc = secondJdbcTemplate;
                        logger.info(String.format("%s :: Found in archive (secondary DB).", sid));
                    }
                }

                // If still not found, log and skip
                if (txn == null) {
                    logger.info(String.format("%s :: No transaction record found in any table; skipping.", sid));
                    continue;
                }

                // Validate amount
                BigDecimal amount = null;
                try {
                    amount = new BigDecimal(txn.get("amount").toString());
                    logger.info(String.format("%s :: Transaction amount: %s; ", sid, amount));
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        logger.info(String.format("%s :: Transaction amount invalid: %s; skipping.", sid, amount));
                        continue;
                    }
                } catch (Exception e) {
                    logger.info(String.format("%s :: Could not parse amount: %s; skipping.", sid, txn.get("amount")));
                    continue;
                }

                // Check if status row already exists (prevents double-reversal)
                String checkStatusSql = "SELECT 1 FROM ajiswitch_db.tbl_transactions_status WHERE session_id = ?";
                List<Map<String, Object>> rows2 = jdbcTemplate.queryForList(checkStatusSql, sid);
                if (!rows2.isEmpty()) {
                    logger.info("Status row already exists for sessionId=" + sid + "; skipping insert/update.");
                    continue;
                }

                // Admin/privileged user logic (wallet reversal)
                if (userrole == 1 && !status.equals("00")) {
                    logger.info(String.format("%s :: Will reverse amount into wallet (status is not success).", sid));

                    // Lookup wallet for source institution
                    String sourceInst = txn.get("source_institution_code").toString();
                    String nodeSql = "SELECT walletnumber, institution_name "
                            + "FROM ajiswitch_db.tbl_nodes "
                            + "WHERE institution_code = ? AND is_active = 1";
                    List<Map<String, Object>> nodeRows = jdbcTemplate.queryForList(nodeSql, sourceInst);

                    if (!nodeRows.isEmpty()) {
                        String walletNumber = nodeRows.get(0).get("walletnumber").toString();
                        logger.info(String.format("%s :: Found walletNumber=%s for institution=%s", sid, walletNumber, sourceInst));

                        // Validate wallet number
                        if (walletNumber != null && walletNumber.matches("\\d{10}")) {
                            // Credit the wallet
                            String walletUpdateSql = "UPDATE ajiswitch_db.tbl_wallets SET balance = balance + ? WHERE walletnumber = ?";
                            int walletUpd = jdbcTemplate.update(walletUpdateSql, amount, walletNumber);
                            logger.info(String.format("%s :: Wallet credited (rowsAffected=%d)", sid, walletUpd));

                            // Log wallet activity if credited
                            if (walletUpd > 0) {
                                String activitySql = "INSERT INTO ajiswitch_db.tbl_wallet_activities "
                                        + "(walletnumber, amount, credit_or_debit, actor, activity_date_time, session_id) "
                                        + "VALUES (?, ?, 'CR', 'SYSTEM', now(), ?)";
                                int actIns = jdbcTemplate.update(activitySql, walletNumber, amount, sid);
                                logger.info(String.format("%s :: Wallet activity insert rowsAffected=%d", sid, actIns));
                            } else {
                                logger.info(String.format("%s :: Wallet not updated (maybe does not exist or is frozen)", sid));
                            }
                        } else {
                            logger.info(String.format("%s :: Invalid wallet number for institution %s: %s", sid, sourceInst, walletNumber));
                        }
                    } else {
                        logger.info(String.format("%s :: No active node found for institution=%s; skipping wallet reversal", sid, sourceInst));
                    }
                }

                // Insert into tbl_transactions_status (protect with UNIQUE constraint on session_id at DB level)
                int retVal;
                if (userrole == 1) {
                    SQL = "INSERT INTO ajiswitch_db.tbl_transactions_status "
                            + "(session_id, requested_by, approved_by, current_status, new_status, approved_at, amount, transaction_date_time, originator_account_name, beneficiary_account_name, source_institution_code, destination_institution_code, source_institution_name, destination_institution_name, status) "
                            + "VALUES(?, ?, ?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, 1)";
                    retVal = jdbcTemplate.update(SQL, new Object[]{
                        sid, username, username,
                        txn.get("response_code"), status,
                        amount, txn.get("transaction_date_time"),
                        txn.get("originator_account_name"), txn.get("beneficiary_account_name"),
                        txn.get("source_institution_code"), txn.get("destination_institution_code"),
                        txn.get("source_institution_name"), txn.get("destination_institution_name")
                    });
                    logger.info(String.format("%s :: Admin status insert returned: %s", sid, retVal));
                } else {
                    SQL = "INSERT INTO ajiswitch_db.tbl_transactions_status "
                            + "(session_id, requested_by, current_status, new_status, amount, transaction_date_time, originator_account_name, beneficiary_account_name, source_institution_code, destination_institution_code, source_institution_name, destination_institution_name) "
                            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    retVal = jdbcTemplate.update(SQL, new Object[]{
                        sid, username,
                        txn.get("response_code"), status,
                        amount, txn.get("transaction_date_time"),
                        txn.get("originator_account_name"), txn.get("beneficiary_account_name"),
                        txn.get("source_institution_code"), txn.get("destination_institution_code"),
                        txn.get("source_institution_name"), txn.get("destination_institution_name")
                    });
                    logger.info(String.format("%s :: User status insert returned: %s", sid, retVal));
                }

                // If status insert successful, update original transaction table's response_code
                if (retVal > 0) {
                    logger.info(String.format("%s :: Updating response_code in table: %s --> %s", sid, foundTable, status));
                    String updateSql = "UPDATE " + foundTable + " SET response_code = ? WHERE session_id = ?";
                    int upd;
                    if (foundTable.equalsIgnoreCase("ajiswitch_db.tbl_creditfundtransfers") || foundTable.equalsIgnoreCase(archiveTable())) {
                        // Use correct JdbcTemplate based on foundTable
                        upd = (sourceJdbc != null ? sourceJdbc : jdbcTemplate).update(updateSql, status, sid);
                    } else {
                        String msg = String.format("Unknown transaction table '%s'—cannot update response_code", foundTable);
                        logger.info(msg);
                        throw new IllegalStateException(msg);
                    }
                    logger.info(String.format("%s :: response_code update rowsAffected=%d", sid, upd));

                    // Delete retry record if exists (idempotent)
                    String delRetrySql = "DELETE FROM ajiswitch_db.tbl_tsq_retry WHERE session_id = ?";
                    int del = jdbcTemplate.update(delRetrySql, sid);
                    logger.info(String.format("%s :: Deleted from tsq_retry rowsAffected=%d", sid, del));
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
            ex.printStackTrace();
            return responseManager.ResponseInternalServerError();
        } catch (Exception ex) {
            logger.info("Exception in RequestTransactionStatusChange: " + ex.getMessage());
            ex.printStackTrace();
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity UpdateTransactionStatusChange(String sessionid, String sessiontoken, String username, String status) {
        NetworkResponse networkResponse = new NetworkResponse();
        logger.info("Entering UpdateTransactionStatusChange(sessionid=" + sessionid
                + ", username=" + username + ", status=" + status + ")");
        networkResponse.setCode(200);
        networkResponse.setStatus("failed");
        networkResponse.setMessage("Transaction status change is not available on this Belema schema (tbl_transactions_status is missing)");
        return responseManager.ResponseOk(networkResponse);
    }
}
