/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.PTSPsInterface;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.PTSPModel;
import com.transgate.api.util.ResponseManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class PTSPsService implements PTSPsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    @Qualifier("jdbcTemplate")
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
    
    private boolean CheckItemPendingEdit(int id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.ptsp WHERE id = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckItemPendingEdit(String id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.ptsp WHERE ptsp_id = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    public ResponseEntity Get(boolean pending) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<PTSPModel> ptsps;
            if (!pending) {
                SQL = "SELECT * FROM sparkpayweb_db.ptsp "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpayweb_db.ptsp "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            ptsps = jdbcTemplate.query(SQL, new PTSPsMapper());
            SQL = "SELECT MIN(ptsp_date) from sparkpayweb_db.ptsp";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ptsp_date) from sparkpayweb_db.ptsp";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All PTSPs");
            networkResponse.setData((ArrayList) ptsps);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByInstitution(String institution) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<PTSPModel> ptsps;
            SQL = "SELECT a.id, a.ptsp_id, a.ptsp_name, a.ptsp_date, a.delete_flag, a.edit_flag, a.create_flag "
                    + "FROM sparkpayweb_db.ptsp a "
                    + "LEFT JOIN sparkpayweb_db.tbl_map_merchants_ptsps b "
                    + "ON a.ptsp_id = b.ptsp_id "
                    + "LEFT JOIN sparkpay.terminals c "
                    + "ON b.merchant_id = c.merchant_id "
                    + "LEFT JOIN sparkpayweb_db.tbl_financial_institutions d "
                    + "ON c.cbn_bank_code = d.bank_code "
                + "WHERE d.acquirer_id = ?";
            ptsps = jdbcTemplate.query(SQL, new Object[]{institution}, new PTSPsMapper());
            SQL = "SELECT MIN(ptsp_date) from sparkpayweb_db.ptsp";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ptsp_date) from sparkpayweb_db.ptsp";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("PTSP by institution: " + institution);
            networkResponse.setData((ArrayList) ptsps);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByMerchant(String merchant_id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            List<String> merchantIds = new ArrayList<>(Arrays.asList(merchant_id.split(",")));
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
            List<PTSPModel> ptsps;
            SQL = "SELECT a.id, a.ptsp_id, a.ptsp_name, a.ptsp_date, a.delete_flag, a.edit_flag, a.create_flag "
                    + "FROM sparkpayweb_db.ptsp a "
                    + "LEFT JOIN sparkpayweb_db.tbl_map_merchants_ptsps b "
                    + "ON a.ptsp_id = b.ptsp_id "
                + "WHERE b.merchant_id IN " + inString.toString();
            ptsps = jdbcTemplate.query(SQL, new PTSPsMapper());
            SQL = "SELECT MIN(ptsp_date) from sparkpayweb_db.ptsp";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ptsp_date) from sparkpayweb_db.ptsp";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("PTSP by merchant: " + merchant_id);
            networkResponse.setData((ArrayList) ptsps);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Get(String ptsp_id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<PTSPModel> ptsps;
            SQL = "SELECT * FROM sparkpayweb_db.ptsp "
                + "WHERE ptsp_id = ?";
            ptsps = jdbcTemplate.query(SQL, new Object[]{ptsp_id}, new PTSPsMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("PTSP " + ptsp_id);
            networkResponse.setData((ArrayList) ptsps);
            
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
            List<PTSPModel> ptsps;
            SQL = "SELECT * FROM sparkpayweb_db.ptsp "
                + "WHERE id = ?";
            ptsps = jdbcTemplate.query(SQL, new Object[]{id}, new PTSPsMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("PTSP by ID: " + id);
            networkResponse.setData((ArrayList) ptsps);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Get() {
        return Get(false);
    }
    
    @Override
    public ResponseEntity GetApprovals() {
        return Get(true);
    }
    
    @Override
    public ResponseEntity Create(String ptsp_id, String ptsp_name, String sessiontoken) {
        try {
            String SQL;
            int retval;
            int userrole = GetUserRole(sessiontoken);
            int create_flag = 0;
            if (userrole != 1 && userrole != 2) {
                return responseManager.ResponseUnathorized();
            }
            if (userrole == 1) 
                create_flag = 1;
            
            SQL = "INSERT into sparkpayweb_db.ptsp"
                    + "(ptsp_id, ptsp_name, ptsp_date, create_flag) VALUES(?, ?, now(), ?)";
            retval = jdbcTemplate.update(SQL, new Object[]{ptsp_id, ptsp_name, create_flag});
            if (retval > 0) 
                return responseManager.ResponseAccepted();
            else 
                return responseManager.ResponseInternalServerError();
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Edit(String ptsp_id, String ptsp_name, String sessiontoken) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE sparkpayweb_db.ptsp SET ptsp_name = ? WHERE ptsp_id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{ptsp_name, ptsp_id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckItemPendingEdit(ptsp_id);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("PTSP already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = Get(ptsp_id);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    PTSPModel model = networkResponse != null ? (PTSPModel) networkResponse.getData().get(0) : new PTSPModel();
                    SQL = "INSERT into sparkpayweb_db.ptsp_bkp"
                        + "(id, ptsp_id, ptsp_name, ptsp_date) "
                        + "VALUES(?, ?, ?, now())";
                    jdbcTemplate.update(SQL, new Object[]{model.getId(), ptsp_id, ptsp_name});
                    SQL = "UPDATE sparkpayweb_db.ptsp SET edit_flag = 1 WHERE ptsp_id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{ptsp_id});
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
    public ResponseEntity SearchPTSPs(
           String start_date,
           String end_date,
           String ptsp_id,
           String ptsp_name){

        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery =  !start_date.equals("")
                    || !end_date.equals("")
                    || !ptsp_id.equals("")
                    || !ptsp_name.equals("")
                    ? "WHERE" : "";


            if (!ptsp_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" ptsp_id LIKE '%" + ptsp_id+"%'";
            }

            if (!ptsp_name.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" ptsp_name LIKE '%" + ptsp_name+"%'";
            }

            if (!start_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" ptsp_date >= '" + start_date + "'";
            }
            if (!end_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery + "";
                whereQuery+=" ptsp_date < '" + end_date + "'";
            }
            String SQL;
            String allowedRows = " AND delete_flag = ? AND edit_flag = ? AND create_flag = ? ";
            List<PTSPModel> ptsps;
            SQL = "SELECT * FROM sparkpayweb_db.ptsp "
                +whereQuery + allowedRows 
                + " ORDER BY ptsp_date DESC";
            ptsps = jdbcTemplate.query(SQL, new Object[]{"0", "0", "1"}, new PTSPsMapper());

            SQL = "SELECT MIN(ptsp_date) from sparkpayweb_db.ptsp";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(ptsp_date) from sparkpayweb_db.ptsp";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{ \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched PTSPs");
            networkResponse.setData((ArrayList) ptsps);

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }

    }
    
    class PTSPsMapper implements RowMapper<PTSPModel> {

        @Override
        public PTSPModel mapRow(ResultSet rs, int arg1) throws SQLException {
            PTSPModel ptsp = new PTSPModel();
            ptsp.setId(rs.getInt("id"));
            ptsp.setPtsp_id(rs.getString("ptsp_id"));
            ptsp.setPtsp_name(rs.getString("ptsp_name"));
            ptsp.setPtsp_date(rs.getString("ptsp_date"));
            ptsp.setDelete_flag(rs.getInt("delete_flag"));
            ptsp.setEdit_flag(rs.getInt("edit_flag"));
            ptsp.setCreate_flag(rs.getInt("create_flag"));
            return ptsp;
        }
    }
}
