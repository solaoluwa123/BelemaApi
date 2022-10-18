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
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.TransactionsCodeInterpreter;
import java.sql.ResultSet;
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
    
    public List<CardsTransactionModel> GetTransaction(String terminalid, String rrn, String stan) {
        String SQL;
        List<CardsTransactionModel> transactions;
        SQL = "SELECT * FROM sparkpay.transactions WHERE terminal_id = ? AND retrieval_ref_number = ? AND system_trace_number = ? ORDER BY id DESC";
        transactions = jdbcTemplate.query(SQL, new Object[]{terminalid, rrn, stan}, new CardsTransactionsMapper());
        return transactions;
    }
    
    @Override
    public ResponseEntity GetByTerminalOwner(String owner) {
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
            SQL = "SELECT * FROM sparkpay.transactions WHERE terminal_id IN "+inString.toString()+" ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM sparkpay.transactions a WHERE a.terminal_id IN "+inString.toString();
            Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
            totalValue = totalValue != null ? totalValue / 100 : 0;
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ncs_date_time) from sparkpay.transactions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Terminal Owner: " + owner);
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByPTSP(String ptsp) {
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
            SQL = "SELECT * FROM sparkpay.transactions WHERE merchant_id IN "+inString.toString()+" ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM sparkpay.transactions a WHERE a.merchant_id IN "+inString.toString();
            Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
            totalValue = totalValue != null ? totalValue / 100 : 0;
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ncs_date_time) from sparkpay.transactions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by PTSP: " + ptsp);
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByTerminal(String terminalid) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsTransactionModel> transactions;
            SQL = "SELECT * FROM sparkpay.transactions WHERE terminal_id = ? ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new Object[]{terminalid}, new CardsTransactionsMapper());
            
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
    public ResponseEntity GetByMerchant(String merchantid) {
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
            SQL = "SELECT * FROM sparkpay.transactions WHERE merchant_id IN "+inString.toString()+" ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM sparkpay.transactions a WHERE a.merchant_id IN "+inString.toString();
            Double totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
            totalValue = totalValue != null ? totalValue / 100 : 0;
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions WHERE merchant_id IN "+inString.toString();
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ncs_date_time) from sparkpay.transactions WHERE merchant_id IN "+inString.toString();
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Merchant: " + merchantid);
            networkResponse.setData((ArrayList) transactions);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByFI(String institution) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsTransactionModel> transactions;
            SQL = "SELECT * FROM sparkpay.transactions WHERE acquirer_institution_id = ? OR destination_acquiring_institution_id = ? ORDER BY id DESC";
            transactions = jdbcTemplate.query(SQL, new Object[]{institution, institution}, new CardsTransactionsMapper());
            
            SQL = "SELECT SUM(a.amount) as totalValue "
                + "FROM sparkpay.transactions a WHERE a.acquirer_institution_id = ? OR destination_acquiring_institution_id = ?";
            Double totalValue = jdbcTemplate.queryForObject(SQL, new Object[]{institution, institution}, Double.class);
            totalValue = totalValue != null ? totalValue / 100 : 0;
            SQL = "SELECT MIN(ncs_date_time) from sparkpay.transactions WHERE acquirer_institution_id = ? OR destination_acquiring_institution_id = ?";
            String minDate = jdbcTemplate.queryForObject(SQL, new Object[]{institution, institution}, String.class);
            SQL = "SELECT MAX(ncs_date_time) from sparkpay.transactions WHERE acquirer_institution_id = ? OR destination_acquiring_institution_id = ?";
            String maxDate = jdbcTemplate.queryForObject(SQL, new Object[]{institution, institution}, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Transactions by Institution: " + institution);
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
                whereQuery+=" ncs_date_time >= '" + start_date + "'";
            }
            if (!end_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" ncs_date_time < '" + end_date + "'";
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
            totalValue = totalValue != null ? totalValue / 100 : 0;
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
    public ResponseEntity LogDispute(String sessiontoken, String terminalid, String rrn, String stan, String username){
        try {
            
            boolean sessionIdExist = CheckDisputeExist(terminalid, rrn, stan);
            if (sessionIdExist) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Cannot log dispute with same details twice");
                return responseManager.ResponseOk(networkResponse);
            }
            
            List<CardsTransactionModel> getTransaction = GetTransaction(terminalid, rrn, stan);
            if (getTransaction.size() > 0) {
                String SQL;
                SQL = "INSERT into sparkpayweb_db.tbl_disputes(id, terminal_id, system_trace_number, retrieval_ref_number, logged_by, owner_institution, status, date_created) VALUES(?, ?, ?, ?, ?, ?, '-1', now())";
                int retval = jdbcTemplate.update(SQL, new Object[]{getTransaction.get(0).getId(), terminalid, stan, rrn, username, getTransaction.get(0).getAcquirer_institution_id()});
                if (retval > 0) 
                    return responseManager.ResponseAccepted();
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
    public ResponseEntity GetDisputes(String institutioncode) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            Double totalValue;
            List<CardsDisputeModel> transactions;
            switch(institutioncode) {
                case "":
                case "-1":
                    SQL = "SELECT a.id, a.logged_by, a.status, a.resolved, a.date_modified, a.date_created, "
                            + "b.id as transactionid, b.message_type, b.pan, b.amount, b.system_trace_number, b.retrieval_ref_number, b.destination_acquiring_institution_id, "
                            + "b.terminal_id, b.bin, b.ncs_date_time, b.response_code "
                            + "FROM sparkpayweb_db.tbl_disputes a "
                            + "LEFT JOIN sparkpay.transactions b "
                            + "ON a.id = b.id "
                            + "WHERE a.resolved = 0 ORDER BY a.id DESC";
                    transactions = jdbcTemplate.query(SQL, new CardsTransactionsDisputesMapper());

                    SQL = "SELECT "
                            + "SUM(b.amount) as totalValue "
                            + "FROM sparkpayweb_db.tbl_disputes a "
                            + "LEFT JOIN sparkpay.transactions b "
                            + "ON a.id = b.id "
                            + "WHERE a.resolved = 0";
                    totalValue = jdbcTemplate.queryForObject(SQL, Double.class);
                    break;
                default:
                    SQL = "SELECT a.id, a.logged_by, a.status, a.resolved, a.date_modified, a.date_created, "
                            + "b.id as transactionid, b.message_type, b.pan, b.amount, b.system_trace_number, b.retrieval_ref_number, b.destination_acquiring_institution_id, "
                            + "b.terminal_id, b.bin, b.ncs_date_time, b.response_code "
                            + "FROM sparkpayweb_db.tbl_disputes a "
                            + "LEFT JOIN sparkpay.transactions b "
                            + "ON a.id = b.id "
                            + "WHERE a.resolved = 0 AND (b.acquirer_institution_id = ? OR b.destination_acquiring_institution_id = ?) ORDER BY a.id DESC";
                    transactions = jdbcTemplate.query(SQL, new Object[]{institutioncode, institutioncode}, new CardsTransactionsDisputesMapper());

                    SQL = "SELECT "
                            + "SUM(b.amount) as totalValue "
                            + "FROM sparkpayweb_db.tbl_disputes a "
                            + "LEFT JOIN sparkpay.transactions b "
                            + "ON a.id = b.id "
                            + "WHERE a.resolved = 0 AND (b.acquirer_institution_id = ? OR b.destination_acquiring_institution_id = ?)";
                    totalValue = jdbcTemplate.queryForObject(SQL, new Object[]{institutioncode, institutioncode}, Double.class);
                    break;
            }
            
            totalValue = totalValue != null ? totalValue / 100 : 0;
            SQL = "SELECT "
                    + "MIN(b.ncs_date_time) "
                    + "FROM sparkpayweb_db.tbl_disputes a "
                    + "LEFT JOIN sparkpay.transactions b "
                    + "ON a.id = b.id "
                    + "WHERE a.owner_institution = ?";
            String minDate = jdbcTemplate.queryForObject(SQL, new Object[]{institutioncode}, String.class);
            SQL = "SELECT MAX(b.ncs_date_time) "
                    + "FROM sparkpayweb_db.tbl_disputes a "
                    + "LEFT JOIN sparkpay.transactions b "
                    + "ON a.id = b.id "
                    + "WHERE a.owner_institution = ?";
            String maxDate = jdbcTemplate.queryForObject(SQL, new Object[]{institutioncode}, String.class);
            String meta = "{\"totalValue\": " +totalValue+ ", \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
           
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
    public ResponseEntity ApproveSettlement(String sessiontoken, int id, int status) {
        try {
            String SQL;
            int retVal;
            int resolved = status == 0 ? 0 : 1;
            SQL = "UPDATE sparkpayweb_db.tbl_disputes SET status = ?, resolved = ?, date_modified = now() WHERE id = ?";
            retVal = jdbcTemplate.update(SQL, new Object[]{status, resolved, id});
            if (retVal > 0)
                return responseManager.ResponseAccepted();
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
            tnx.setTransaction_id(rs.getInt("transactionid"));
            tnx.setStatus(rs.getInt("status"));
            tnx.setResolved(rs.getInt("resolved"));
            tnx.setDate_modified(rs.getString("date_modified"));
            tnx.setDate_created(rs.getString("date_created"));
            tnx.setMessage_type(rs.getString("message_type"));
            tnx.setPan(rs.getString("pan"));
            Double amount = rs.getString("amount") != null && rs.getString("amount") != "" ? Double.parseDouble(rs.getString("amount")) / 100 : 0.00;
            tnx.setAmount(amount.toString());
            tnx.setSystem_trace_number(rs.getString("system_trace_number"));
            tnx.setRetrieval_ref_number(rs.getString("retrieval_ref_number"));
            tnx.setDestination_acquiring_institution_id(rs.getString("destination_acquiring_institution_id"));
            tnx.setTerminal_id(rs.getString("terminal_id"));
            tnx.setBin(rs.getString("bin"));
            tnx.setNcs_date_time(rs.getString("ncs_date_time"));
            tnx.setStatus_code_message(transactionsCodeInterpreter.GetResponse(rs.getString("response_code")));
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
            tnx.setStatus_code_message(transactionsCodeInterpreter.GetResponse(rs.getString("response_code")));
            return tnx;
        }
    }
}
