/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.CardChartInterface;
import com.transgate.api.models.*;
import com.transgate.api.util.DateUtil;
import com.transgate.api.util.ResponseCodeInterpreter;
import com.transgate.api.util.ResponseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 *
 * @author Makintola
 */
@Service
public class CardChartsService implements CardChartInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    @Qualifier("jdbcTemplate")
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    DateUtil dateUtil = new DateUtil();
    LocalDate now = LocalDate.now();
    @Override
    public ResponseEntity GetSuccessTNXVolume(String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {    
            String SQL;
            String today1, today2, today = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            today1 = today + " 00:00:00";
            today2 = today + " 23:59:59";
//
            SQL = "SELECT COUNT(a.id) as volume, a.ncs_date_time as label "
                    + "FROM sparkpay.transaction_hist_s a "
                    + "WHERE a.ncs_date_time between ? AND ? AND a.response_code = '00' "
                    + "GROUP BY CAST(a.ncs_date_time as DATE) "
                    + "ORDER BY a.ncs_date_time DESC";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(
                    SQL, new Object[]{startDate+" 00:00:00", endDate+" 23:59:59"});

            SQL = "SELECT COUNT(a.id) as volume, a.ncs_date_time as label "
                    + "FROM sparkpay.transactions a "
                    + "WHERE a.ncs_date_time between ? AND ? AND a.response_code = '00' "
                    + "GROUP BY CAST(a.ncs_date_time as DATE) "
                    + "ORDER BY a.ncs_date_time DESC";

            List<Map<String, Object>> summary1 = jdbcTemplate.queryForList(SQL, today1, today2);
//            if (summary1.size() > 0)
                summary1.addAll(summary);
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Successful Transactions Summary");
            TNXModel tnxModel = new TNXModel();
            tnxModel.setSummary((ArrayList) summary1);
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

            SQL = "SELECT COUNT(a.id) as volume, a.transaction_date_time as label "
                    + "FROM sparkpay.transactions a "
                    + "WHERE a.ncs_date_time >= ? AND a.ncs_date_time < ? AND a.response_code = '00' AND a.source_institution_code = ? "
                    + "GROUP BY CAST(a.ncs_date_time as DATE) "
                    + "ORDER BY a.ncs_date_time DESC";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, institutioncode});
            
//            summary.addAll(summary_);
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Successful Transactions Summary");
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
            String table = isCurrent ? "sparkpay.transactions" : "sparkpay.transaction_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM "+table+" a "
                    + "WHERE a.ncs_date_time BETWEEN ? AND ?"
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
            String table = isCurrent ? "sparkpay.transactions" : "sparkpay.transaction_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.response_code as label "
                    + "FROM "+table+" a "
                    + "WHERE a.ncs_date_time BETWEEN ? AND ? AND a.source_institution_code = ? "
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
            String table = isCurrent ? "sparkpay.transactions" : "sparkpay.transaction_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, m.merchant_name as label "
                    + "FROM "+table+" a "
                    +"join sparkpay.merchants m on a.merchant_id = m.merchant_id "
                    + "WHERE a.ncs_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.merchant_id "
                    + "LIMIT 6";
            
            List<Map<String, Object>> summary = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            
            networkResponse.setCode(200);
            networkResponse.setMessage("Transactions Volumes by Merchant");
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
            String table = isCurrent ? "sparkpay.transactions" : "sparkpay.transaction_hist_s";
            SQL = "SELECT COUNT(a.id) as volume, a.merchant_id as label "
                    + "FROM "+table+" a "
                    + "WHERE a.ncs_date_time BETWEEN ? AND ? "
                    + "GROUP BY a.merchant_id "
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