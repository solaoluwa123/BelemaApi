/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.GenericInterface;
import com.transgate.api.models.GenericModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.TerminalTypeModel;
import com.transgate.api.util.Constants;
import com.transgate.api.util.ResponseManager;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class GenericService implements GenericInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    private int GetUserRole(String session_token) {
        try {
            int role;

            String SQL = "SELECT role FROM tbl_user_details WHERE deleted = 0 AND session_token = ?";
            role = jdbcTemplate.queryForObject(SQL, new Object[]{session_token}, int.class);
            return role;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage() + "------------");
            return -100;
        }
    }
    
    private boolean CheckPendingDelete(int id, String table) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM "+table+" WHERE id = ? AND delete_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckPendingDelete(String id, String column, String table) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM "+table+" WHERE "+column+" = ? AND delete_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    public int CheckItemExit(String id, String table, String column) {
        int totalRows = 0;
        try {
            String SQL = "SELECT COUNT(*) FROM "+table+" WHERE "+column+" = ?";
            totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);
            return totalRows;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return -1;
        }
    }
    
    @Override
    public ResponseEntity GetBanks() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> banks;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_bank_code ORDER BY name ASC";
            banks = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Banks");
            networkResponse.setData((ArrayList) banks);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSKR() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> skr;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_send_key_request ORDER BY id ASC";
            skr = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Send Key Request");
            networkResponse.setData((ArrayList) skr);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTransactionDirection() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> skr;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_trasaction_direction ORDER BY id ASC";
            skr = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transaction Direction");
            networkResponse.setData((ArrayList) skr);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetStates() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> states;
            SQL = "SELECT * FROM sparkpayweb_db.states ORDER BY id ASC";
            states = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All States");
            networkResponse.setData((ArrayList) states);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetResponseCodes() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<GenericModel> codes;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_response_codes ORDER BY id ASC";
            codes = jdbcTemplate.query(SQL, new GenericMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Response codes");
            networkResponse.setData((ArrayList) codes);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSettlements(String institution, String startDate, String endDate, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> rows;
            String meta;
            if (institution.equals(Constants.SYESTEMFICODE)) {
                List<Map<String, Object>> _rows;
                SQL = "SELECT a.institution_code FROM ajiswitch_db.tbl_nodes a WHERE a.issettlementbank = 0";
                _rows = jdbcTemplate.queryForList(SQL);
                
                StringBuilder inString = new StringBuilder("(");
                inString.append("'").append(institution).append("',");
                for (final Map<String, Object> row : _rows) {
                    inString.append("'").append(row.get("institution_code")).append("'");
                    inString.append(",");
                }
                inString = inString.deleteCharAt(inString.length() - 1);
                inString = inString.append(")");
                
                SQL = "SELECT a.id, a.institution_code, a.institution_name, a.acqVol, a.acqVal, a.issVol, a.issVal, a.net_set_pos, a.settlement_date, a.report_location "
                        + "FROM ajiswitch_db.tbl_settlement_details a "
                        + "LEFT JOIN tbl_financial_institutions b "
                        + "ON a.institution_code = b.code "
                        + "WHERE a.institution_code IN "+inString.toString()+" AND a.settlement_date >= ? AND a.settlement_date < ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv') "
                        + "ORDER BY a.settlement_date DESC LIMIT ? OFFSET ?";
                rows = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, limit, offset});
                
                SQL = "SELECT COUNT(a.id) as totalRecords "
                    + "FROM ajiswitch_db.tbl_settlement_details a "
                        + "WHERE a.institution_code IN "+inString.toString()+" AND a.settlement_date >= ? AND a.settlement_date < ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv')";
            
                List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
                Map<String, Object> row = agg.get(0);
                Long tRecords = (Long) row.get("totalRecords");
                int totalRecords = tRecords != null ? tRecords.intValue() : 0;
                meta = "{\"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";

            } else {
                SQL = "SELECT a.id, a.institution_code, a.institution_name, a.acqVol, a.acqVal, a.issVol, a.issVal, a.net_set_pos, a.settlement_date, a.report_location "
                        + "FROM ajiswitch_db.tbl_settlement_details a "
                        + "LEFT JOIN tbl_financial_institutions b "
                        + "ON a.institution_code = b.code "
                        + "WHERE a.institution_code = ? AND a.settlement_date >= ? AND a.settlement_date < ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv') "
                        + "ORDER BY a.settlement_date DESC LIMIT ? OFFSET ?";
                rows = jdbcTemplate.queryForList(SQL, new Object[]{institution, startDate, endDate, limit, offset});
                
                SQL = "SELECT COUNT(a.id) as totalRecords "
                    + "FROM ajiswitch_db.tbl_settlement_details a "
                        + "WHERE a.institution_code = ? AND a.settlement_date >= ? AND a.settlement_date < ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv')";
            
                List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{institution, startDate, endDate});
                Map<String, Object> row = agg.get(0);
                Long tRecords = (Long) row.get("totalRecords");
                int totalRecords = tRecords != null ? tRecords.intValue() : 0;
                meta = "{\"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            }
            
            networkResponse.setMeta(meta);
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlements");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity SearchSettlementsByInstitution(String institution, String startDate, String endDate, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> rows;
            String meta;
            SQL = "SELECT a.id, a.institution_code, a.institution_name, a.acqVol, a.acqVal, a.issVol, a.issVal, a.net_set_pos, a.settlement_date, a.report_location "
                    + "FROM ajiswitch_db.tbl_settlement_details a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.institution_code = b.code "
                    + "WHERE a.institution_code = ? AND a.settlement_date >= ? AND a.settlement_date < ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv') "
                    + "ORDER BY a.settlement_date DESC LIMIT ? OFFSET ?";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{institution, startDate, endDate, limit, offset});

            SQL = "SELECT COUNT(a.id) as totalRecords "
                + "FROM ajiswitch_db.tbl_settlement_details a "
                    + "WHERE a.institution_code = ? AND a.settlement_date >= ? AND a.settlement_date < ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv')";

            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{institution, startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            meta = "{\"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            
            networkResponse.setMeta(meta);
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlements");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSettlements(String startDate, String endDate, int page, int limit) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> rows;
            SQL = "SELECT a.id, a.institution_code, a.institution_name, a.acqVol, a.acqVal, a.issVol, a.issVal, a.net_set_pos, a.settlement_date, a.report_location "
                    + "FROM ajiswitch_db.tbl_settlement_details a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.institution_code = b.code "
                    + "WHERE a.settlement_date >= ? AND a.settlement_date < ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv') "
                    + "ORDER BY a.settlement_date DESC LIMIT ? OFFSET ?";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate, limit, offset});
            
            SQL = "SELECT COUNT(a.id) as totalRecords "
                    + "FROM ajiswitch_db.tbl_settlement_details a "
                    + "WHERE a.settlement_date >= ? AND a.settlement_date < ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv')";
            
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            
            networkResponse.setMeta(meta);
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlements");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSmartDets(String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT a.id, a.settlement_date, a.report_location "
                    + "FROM ajiswitch_db.tbl_smartdet_details a "
                    + "ORDER BY a.settlement_date DESC";
            rows = jdbcTemplate.queryForList(SQL);
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Smartdet");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSettlementSummary(String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT a.id, a.settlement_date, a.report_location "
                    + "FROM ajiswitch_db.tbl_acct_summary_details a "
                    + "ORDER BY a.settlement_date DESC";
            rows = jdbcTemplate.queryForList(SQL);
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlement Summary");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetCardsSettlementSummary(String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT a.id, a.settlement_date, a.report_location "
                    + "FROM sparkpay.tbl_acct_summary_details a "
                    + "ORDER BY a.settlement_date DESC";
            rows = jdbcTemplate.queryForList(SQL);
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlement Summary");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetSettlementsByMerchant(String merchant, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            List<String> merchantIds = new ArrayList<>(Arrays.asList(merchant.split(",")));
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
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT a.id, a.institution_name, a.merchant_id, a.acqVol, a.acqVal, a.msc, a.net_set_pos, a.settlement_date, a.report_location, b.merchant_name "
                    + "FROM sparkpay.tbl_settlement_details_merchant a "
                    + "LEFT JOIN postxnprocessor.tbl_merchants b "
                    + "ON a.merchant_id = b.merchant_id "
                    + "WHERE a.merchant_id IN "+inString.toString()+" AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv') ORDER BY a.settlement_date DESC";
            rows = jdbcTemplate.queryForList(SQL);
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlements");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetCardsSettlementsByPTSP(String ptsp, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT a.merchant_id FROM sparkpayweb_db.tbl_map_merchants_ptsps a WHERE a.ptsp_id = ?";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{ptsp});
            StringBuilder inString = new StringBuilder("(");
            for (final Map<String, Object> row : rows) {
                inString.append("'").append(row.get("merchant_id")).append("'");
                inString.append(",");
            }
            inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");

            SQL = "SELECT a.id, a.institution_name, a.merchant_id, a.acqVol, a.acqVal, a.msc, a.net_set_pos, a.settlement_date, a.report_location, b.merchant_name "
                    + "FROM sparkpay.tbl_settlement_details_merchant a "
                    + "LEFT JOIN postxnprocessor.tbl_merchants b "
                    + "ON a.merchant_id = b.merchant_id "
                    + "WHERE a.merchant_id IN "+inString.toString()+" AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv') ORDER BY a.settlement_date DESC";
            rows = jdbcTemplate.queryForList(SQL);
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlements");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetCardsSettlements(String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
//            SQL = "SELECT a.id, a.institution_name, a.acqVol, a.acqVal, a.issVol, a.issVal, a.net_set_pos, a.settlement_date, a.report_location, a.acquirer_id, a.issuer_id "
//                    + "FROM sparkpay.tbl_settlement_details a "
//                    + "WHERE a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv' ORDER BY a.settlement_date DESC";
//            rows = jdbcTemplate.queryForList(SQL);
            
            SQL = "SELECT a.id, a.institution_name, a.merchant_id, a.acqVol, a.acqVal, a.msc, a.net_set_pos, a.settlement_date, a.report_location, b.merchant_name "
                    + "FROM sparkpay.tbl_settlement_details_merchant a "
                    + "LEFT JOIN postxnprocessor.tbl_merchants b "
                    + "ON a.merchant_id = b.merchant_id "
                    + "WHERE a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv' ORDER BY a.settlement_date DESC";
            rows = jdbcTemplate.queryForList(SQL);
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlements");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetCardsSettlementsByAcquirer(String acquirer, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT a.id, a.institution_name, a.acqVol, a.acqVal, a.issVol, a.issVal, a.net_set_pos, a.settlement_date, a.report_location, a.acquirer_id, a.issuer_id "
                    + "FROM sparkpay.tbl_settlement_details a "
                    + "WHERE a.acquirer_id = ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv') ORDER BY a.settlement_date DESC";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{acquirer});
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlements by ACQ: " + acquirer);
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetCardsSettlementsByIssuer(String issuer, String startDate, String endDate) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<Map<String, Object>> rows;
            SQL = "SELECT a.id, a.institution_name, a.acqVol, a.acqVal, a.issVol, a.issVal, a.net_set_pos, a.settlement_date, a.report_location, a.acquirer_id, a.issuer_id "
                    + "FROM sparkpay.tbl_settlement_details a "
                    + "WHERE a.issuer_id = ? AND (a.report_location LIKE '%.xlsx' || a.report_location LIKE '%.csv') ORDER BY a.settlement_date DESC";
            rows = jdbcTemplate.queryForList(SQL, new Object[]{issuer});
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Settlements by ISS: " + issuer);
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity DeleteHelper(String sessiontoken, int id, String table, String entity) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                case 3:
                    SQL = "DELETE FROM "+table+" WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseDeleted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckPendingDelete(id, table);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage(entity+" in pending for approval");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "UPDATE "+table+" SET delete_flag = 1 WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else 
                        return responseManager.ResponseBadRequest();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity DeleteHelper(String sessiontoken, String id, String column, String table, String entity) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                case 3:
                    SQL = "DELETE FROM "+table+" WHERE "+column+" = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseDeleted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckPendingDelete(id, column, table);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage(entity+" in pending for approval");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "UPDATE "+table+" SET delete_flag = 1 WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else 
                        return responseManager.ResponseBadRequest();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity ApprovalHelper(String sessiontoken, int id, String table, String entity, String approvalType) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                case 3:
                    if (approvalType.equals("edit")) {
                        final List<Map<String, Object>> rows;
                        switch(entity) {
                            case "Merchant":
                                SQL = "SELECT * FROM sparkpayweb_db.merchants_bkp WHERE id = ?";
                                rows = jdbcTemplate.queryForList(SQL, new Object[]{id});
                                String merchant_id = rows.size() > 0 ? (String) rows.get(0).get("merchant_id") : "";
                                for (final Map<String, Object> row : rows) {
                                    SQL = "UPDATE postxnprocessor.tbl_merchants SET merchant_name = ?, merchant_state = ?, merchant_country = ?, merchant_category_code = ? WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{row.get("merchant_name"), row.get("merchant_state"), row.get("merchant_country"), row.get("merchant_category_code"), id});
                                    SQL = "DELETE FROM sparkpayweb_db.merchants_bkp WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{id});
                                }
                                SQL = "DELETE FROM sparkpayweb_db.tbl_map_merchants_ptsps WHERE merchant_id = ?";
                                jdbcTemplate.update(SQL, new Object[]{merchant_id});
                                SQL = "SELECT * FROM sparkpayweb_db.tbl_map_merchants_ptsps_bkp WHERE merchant_id = ?";
                                final List<Map<String, Object>> rowsptsps = jdbcTemplate.queryForList(SQL, new Object[]{merchant_id});
                                for (final Map<String, Object> row : rowsptsps) {
                                    SQL = "INSERT INTO sparkpayweb_db.tbl_map_merchants_ptsps "
                                            + "(merchant_id, ptsp_id, date_created) "
                                            + "VALUES(?, ?, now())";
                                    jdbcTemplate.update(SQL, new Object[]{merchant_id, row.get("ptsp_id")});
                                }
                                SQL = "DELETE FROM sparkpayweb_db.tbl_map_merchants_ptsps_bkp WHERE merchant_id = ?";
                                jdbcTemplate.update(SQL, new Object[]{merchant_id});
                                break;
                            case "PTSP":
                                SQL = "SELECT * FROM sparkpayweb_db.ptsp_bkp WHERE id = ?";
                                rows = jdbcTemplate.queryForList(SQL, new Object[]{id});
                                for (final Map<String, Object> row : rows) {
                                    SQL = "UPDATE sparkpayweb_db.ptsp SET ptsp_name = ? WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{row.get("ptsp_name"), id});
                                    SQL = "DELETE FROM sparkpayweb_db.ptsp_bkp WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{id});
                                }
                                break;
                            case "Terminal Owner":
                                SQL = "SELECT * FROM sparkpayweb_db.tbl_terminal_owners_bkp WHERE id = ?";
                                rows = jdbcTemplate.queryForList(SQL, new Object[]{id});
                                for (final Map<String, Object> row : rows) {
                                    SQL = "UPDATE sparkpayweb_db.tbl_terminal_owners SET terminal_owner_name = ? WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{row.get("terminal_owner_name"), id});
                                    SQL = "DELETE FROM sparkpayweb_db.tbl_terminal_owners_bkp WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{id});
                                }
                                break;
                            case "Financial Institution":
                                SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions_bkp WHERE id = ?";
                                rows = jdbcTemplate.queryForList(SQL, new Object[]{id});
                                for (final Map<String, Object> row : rows) {
                                    SQL = "UPDATE sparkpayweb_db.tbl_financial_institutions SET acquirer_id = ?, institution_name = ?, issuer_id = ?, bank_code = ? WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{row.get("acquirer_id"), row.get("institution_name"), row.get("issuer_id"), row.get("bank_code"), id});
                                    SQL = "DELETE FROM sparkpayweb_db.tbl_financial_institutions_bkp WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{id});
                                }
                                break;
                            case "Route":
                                SQL = "SELECT * FROM sparkpayweb_db.transaction_route_bkp WHERE id = ?";
                                rows = jdbcTemplate.queryForList(SQL, new Object[]{id});
                                for (final Map<String, Object> row : rows) {
                                    SQL = "UPDATE sparkpay.transaction_route SET source_acq_id = ?, destination_bin = ?, card_bin = ? WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{row.get("source_acq_id"), row.get("destination_bin"), row.get("card_bin"), id});
                                    SQL = "DELETE FROM sparkpayweb_db.transaction_route_bkp WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{id});
                                }
                                break;
                            case "Node":
                                SQL = "SELECT * FROM sparkpayweb_db.station_pcis_bkp WHERE id = ?";
                                rows = jdbcTemplate.queryForList(SQL, new Object[]{id});
                                for (final Map<String, Object> row : rows) {
                                    SQL = "UPDATE sparkpay.station_pcis "
                                            + "SET station_name = ?, local_port = ?, acquiring_institution_id = ?, kek = ?, "
                                            + "send_key_request = ?, cbn_bank_code = ?, key_check_value = ?, transaction_direction = ?, "
                                            + "remoteIP = ?, remote_port = ? WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{row.get("station_name"), row.get("local_port"), row.get("acquiring_institution_id"), row.get("kek"),
                                        row.get("send_key_request"), row.get("cbn_bank_code"), row.get("key_check_value"), row.get("transaction_direction"),
                                        row.get("remoteIP"), row.get("remote_port"),
                                        id});
                                    SQL = "DELETE FROM sparkpayweb_db.station_pcis_bkp WHERE id = ?";
                                    jdbcTemplate.update(SQL, new Object[]{id});
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    SQL = approvalType.equals("delete") ?
                            "DELETE FROM "+table+" WHERE delete_flag = 1 AND id = ?"
                            : approvalType.equals("edit") || approvalType.equals("create") ? "UPDATE "+table+" SET delete_flag = 0, edit_flag = 0, create_flag = 1 WHERE id = ?"
                            : ""
                            ;
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else 
                        return responseManager.ResponseBadRequest();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity ApprovalHelper(String sessiontoken, String id, String column, String table, String entity, String approvalType) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                case 3:
                    if (approvalType.equals("edit")) {
                        final List<Map<String, Object>> rows;
                        switch(entity) {
                            case "Terminal":
                                SQL = "SELECT * FROM sparkpayweb_db.terminals_bkp WHERE "+column+" = ?";
                                rows = jdbcTemplate.queryForList(SQL, new Object[]{id});
                                for (final Map<String, Object> row : rows) {
                                    SQL = "UPDATE sparkpay.terminals SET merchant_id = ?, merchant_name = ?, route_mode = ?, acquiring_institution_id = ?,"
                                            + "acquiring_institution_name = ?, cbn_bank_code = ?, terminal_type = ? WHERE "+column+" = ?";
                                    jdbcTemplate.update(SQL, new Object[]{row.get("merchant_id"), row.get("merchant_name"), row.get("route_mode"), row.get("acquiring_institution_id"), 
                                        row.get("acquiring_institution_name"), row.get("cbn_bank_code"), row.get("terminal_type"), id});
                                    SQL = "DELETE FROM sparkpayweb_db.terminals_bkp WHERE "+column+" = ?";
                                    jdbcTemplate.update(SQL, new Object[]{id});
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    SQL = approvalType.equals("delete") ?
                            "DELETE FROM "+table+" WHERE delete_flag = 1 AND "+column+" = ?"
                            : approvalType.equals("edit") || approvalType.equals("create") ? "UPDATE "+table+" SET delete_flag = 0, edit_flag = 0, create_flag = 1 WHERE "+column+" = ?"
                            : ""
                            ;
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity RejectHelper(String sessiontoken, int id, String table, String entity, String approvalType) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                case 3:
                    if (approvalType.equals("edit")) {
                        switch(entity) {
                            case "Merchant":
                                SQL = "DELETE FROM sparkpayweb_db.merchants_bkp WHERE id = ?";
                                jdbcTemplate.update(SQL, new Object[]{id});
                                break;
                            case "PTSP":
                                SQL = "DELETE FROM sparkpayweb_db.ptsp_bkp WHERE id = ?";
                                jdbcTemplate.update(SQL, new Object[]{id});
                                break;
                            case "Terminal Owner":
                                SQL = "DELETE FROM sparkpayweb_db.tbl_terminal_owners_bkp WHERE id = ?";
                                jdbcTemplate.update(SQL, new Object[]{id});
                                break;
                            case "Financial Institution":
                                SQL = "DELETE FROM sparkpayweb_db.tbl_financial_institutions_bkp WHERE id = ?";
                                jdbcTemplate.update(SQL, new Object[]{id});
                                break;
                            case "Route":
                                SQL = "DELETE FROM sparkpayweb_db.transaction_route_bkp WHERE id = ?";
                                jdbcTemplate.update(SQL, new Object[]{id});
                                break;
                            case "Node":
                                SQL = "DELETE FROM sparkpayweb_db.station_pcis_bkp WHERE id = ?";
                                jdbcTemplate.update(SQL, new Object[]{id});
                                break;
                            default:
                                break;
                        }
                    }
                    SQL = approvalType.equals("delete") ?
                            "UPDATE "+table+" SET delete_flag = 0 WHERE id = ?"
                            : approvalType.equals("edit") ?
                            "UPDATE "+table+" SET edit_flag = 0 WHERE id = ?"
                            : approvalType.equals("create") ?
                            "DELETE FROM "+table+" WHERE create_flag = 0 AND id = ?" 
                            : "";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else 
                        return responseManager.ResponseBadRequest();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity RejectHelper(String sessiontoken, String id, String column, String table, String entity, String approvalType) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                case 3:
                    if (approvalType.equals("edit")) {
                        switch(entity) {
                            case "Terminal":
                                SQL = "DELETE FROM sparkpayweb_db.terminals_bkp WHERE "+column+" = ?";
                                jdbcTemplate.update(SQL, new Object[]{id});
                                break;
                            default:
                                break;
                        }
                    }
                    SQL = approvalType.equals("delete") ?
                            "UPDATE "+table+" SET delete_flag = 0 WHERE "+column+" = ?"
                            : approvalType.equals("edit") ?
                            "UPDATE "+table+" SET edit_flag = 0 WHERE "+column+" = ?"
                            : approvalType.equals("create") ?
                            "DELETE FROM "+table+" WHERE create_flag = 0 AND "+column+" = ?" 
                            : "";
                    retVal = jdbcTemplate.update(SQL, new Object[]{id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTerminalTypes() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<TerminalTypeModel> types;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_terminal_types ORDER BY terminal_type ASC";
            types = jdbcTemplate.query(SQL, new TerminalTypeMapper());
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Terminal Types");
            networkResponse.setData((ArrayList) types);
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class GenericMapper implements RowMapper<GenericModel> {

        @Override
        public GenericModel mapRow(ResultSet rs, int arg1) throws SQLException {
            GenericModel bank = new GenericModel();
            bank.setId(rs.getInt("id"));
            bank.setName(rs.getString("name") != null ? rs.getString("name") : "");
            bank.setCode(rs.getString("code") != null ? rs.getString("code") : "");
            bank.setDate_created(hasColumn(rs, "date_created") ? rs.getString("date_created") : null);
            return bank;
        }
    }
    
    class TerminalTypeMapper implements RowMapper<TerminalTypeModel> {

        @Override
        public TerminalTypeModel mapRow(ResultSet rs, int arg1) throws SQLException {
            TerminalTypeModel type = new TerminalTypeModel();
            type.setId(rs.getInt("id"));
            type.setTerminal_type(rs.getString("terminal_type"));
            return type;
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
    
    @Override
    public ResponseEntity GetCardPayments(String institutionCode, String startDate, String endDate, int page, int limit, String response_code, String transaction_id, String merchant_name) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> payments;
            String whereQuery = !startDate.equals("")
                    || !endDate.equals("")
                    || !response_code.equals("")
                    || !transaction_id.equals("")
                    || !merchant_name.equals("")
                    ? "WHERE" : "";
            
            if (!startDate.equals("") && !endDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.txndatetime BETWEEN '" + startDate + "' AND '" + endDate + "'";
            }
            if (!response_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                if (response_code.equals("111"))
                    whereQuery+=" a.responsecode != 00";
                else                    
                    whereQuery+=" a.responsecode = " + response_code+"";
            }
            if (!transaction_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.transactionid = '" + transaction_id + "'";
            }
            if (!merchant_name.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.merchantname LIKE '%" + merchant_name + "%'";
            }
//            if (institutionCode.equals("-1") || institutionCode.equals("000013")) {
            SQL = "SELECT a.amount, a.transactionid, a.merchantname, a.responsecode, a.responsemessage, a.txndatetime "
                + "FROM cardweb_db.tbl_cardpayments a "
                + whereQuery
                + " ORDER BY a.txndatetime DESC LIMIT ? OFFSET ?";
            payments = jdbcTemplate.queryForList(SQL, new Object[]{limit, offset});
            SQL = "SELECT COUNT(a.id) as totalRecords, SUM(a.amount) as totalValue "
                + "FROM cardweb_db.tbl_cardpayments a "
                + whereQuery;

            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            Double totalValue = tValue != null ? tValue.doubleValue() : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
//            }
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All cards payments");
            networkResponse.setData((ArrayList) payments);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetGapsPayments(String merchant_id, String startDate, String endDate, int page, int limit, String isSettled, String reference) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> payments;
            String whereQuery = !startDate.equals("")
                    || !endDate.equals("")
                    || !merchant_id.equals("")
                    || !reference.equals("")
                    || !isSettled.equals("")
                    ? "WHERE" : "";
            
            if (!startDate.equals("") && !endDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.settlement_date BETWEEN '" + startDate + "' AND '" + endDate + "'";
            }
            if (!merchant_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.merchant_id = '" + merchant_id + "'";
            }
            if (!isSettled.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.isSettled = '" + isSettled + "'";
            }
            if (!reference.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.gaps_reference = '" + reference + "'";
            }
//            if (institutionCode.equals("-1") || institutionCode.equals("000013")) {
            SQL = "SELECT a.* "
                + "FROM sparkpay.tbl_merchant_paid_list a "
                + whereQuery
                + " ORDER BY a.settlement_date DESC LIMIT ? OFFSET ?";
            payments = jdbcTemplate.queryForList(SQL, new Object[]{limit, offset});
            SQL = "SELECT COUNT(a.id) as totalRecords, SUM(a.amountpaid) as totalValue "
                + "FROM sparkpay.tbl_merchant_paid_list a "
                + whereQuery;

            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            Double totalValue = tValue != null ? tValue.doubleValue() : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
//            }
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All gaps payments");
            networkResponse.setData((ArrayList) payments);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetTNXStatusChange(String session_id, String startDate, String endDate, int page, int limit, String requested_by, String approved_by, String current_status, String new_status, String status) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int offset = page > 1 ? (page - 1) * limit : 0;
            List<Map<String, Object>> transactions;
            String whereQuery = !startDate.equals("")
                    || !endDate.equals("")
                    || !session_id.equals("")
                    || !requested_by.equals("")
                    || !approved_by.equals("")
                    || !current_status.equals("")
                    || !new_status.equals("")
                    || !status.equals("")
                    ? "WHERE" : "";
            
            if (!startDate.equals("") && !endDate.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.created_at BETWEEN '" + startDate + "' AND '" + endDate + "'";
            }
            if (!session_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.session_id = '" + session_id + "'";
            }
            if (!requested_by.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.requested_by = '" + requested_by + "'";
            }
            if (!approved_by.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.approved_by = '" + approved_by + "'";
            }
            if (!new_status.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.new_status = '" + new_status + "'";
            }
            if (!current_status.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.current_status = '" + current_status + "'";
            }
            if (!status.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" a.status = '" + status + "'";
            }
            
            SQL = "SELECT a.* "
                + "FROM ajiswitch_db.tbl_transactions_status a "
                + whereQuery
                + " ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
            transactions = jdbcTemplate.queryForList(SQL, new Object[]{limit, offset});
            SQL = "SELECT COUNT(a.id) as totalRecords, SUM(a.amount) as totalValue "
                + "FROM ajiswitch_db.tbl_transactions_status a "
                + whereQuery;

            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL);
            Map<String, Object> row = agg.get(0);
            BigDecimal tValue = (BigDecimal) row.get("totalValue");
            Double totalValue = tValue != null ? tValue.doubleValue() : 0;
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
            String meta = "{\"totalValue\": " + totalValue+ ", \"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setMeta(meta);
//            }
            
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All transactions");
            networkResponse.setData((ArrayList) transactions);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
}
