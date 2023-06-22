/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.MerchantsInterface;
import com.transgate.api.models.CardsMerchantModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.util.ResponseManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
public class MerchantsService implements MerchantsInterface {
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
    
    private boolean CheckItemPendingEdit(String merchant_id) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpay.merchants WHERE merchant_id = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{merchant_id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    public int CheckMerchantExit(String id, String table, String column) {
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
    
    public ResponseEntity Get(boolean pending) {
         NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsMerchantModel> merchants;
            if (!pending) {
                SQL = "SELECT * FROM sparkpay.merchants "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpay.merchants "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            merchants = jdbcTemplate.query(SQL, new MerchantsMapper());
            SQL = "SELECT MIN(date_created) from sparkpay.merchants";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_created) from sparkpay.merchants";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Merchants");
            networkResponse.setData((ArrayList) merchants);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Get(String merchant_id) {
         NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsMerchantModel> merchants;
            SQL = "SELECT * FROM sparkpay.merchants "
                + "WHERE merchant_id = ?";
            merchants = jdbcTemplate.query(SQL, new Object[]{merchant_id}, new MerchantsMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Get Merchant " + merchant_id);
            networkResponse.setData((ArrayList) merchants);
            
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
            List<CardsMerchantModel> merchants;
            SQL = "SELECT a.id, a.merchant_id, a.merchant_name, a.merchant_state, a.merchant_country, a.merchant_category_code, a.merchant_service_charge, a.msc_cap, a.date_created, a.delete_flag, a.edit_flag, a.create_flag "
                    + "FROM sparkpay.merchants a "
                    + "LEFT JOIN sparkpayweb_db.tbl_map_merchants_ptsps b "
                    + "ON a.merchant_id = b.merchant_id "
                + "WHERE b.ptsp_id = ?";
            merchants = jdbcTemplate.query(SQL, new Object[]{ptsp}, new MerchantsMapper());
            
            SQL = "SELECT MIN(date_created) from sparkpay.merchants";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_created) from sparkpay.merchants";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Get Merchant by ptsp: " + ptsp);
            networkResponse.setData((ArrayList) merchants);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByTerminalOwner(String owner) {
         NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsMerchantModel> merchants;
            SQL = "SELECT a.id, a.merchant_id, a.merchant_name, a.merchant_state, a.merchant_country, a.merchant_category_code, a.merchant_service_charge, a.msc_cap, a.date_created, a.delete_flag, a.edit_flag, a.create_flag "
                    + "FROM sparkpay.merchants a "
                    + "LEFT JOIN sparkpay.terminals b "
                    + "ON a.merchant_id = b.merchant_id "
                + "WHERE b.owner_id = ?";
            merchants = jdbcTemplate.query(SQL, new Object[]{owner}, new MerchantsMapper());
            
            SQL = "SELECT MIN(date_created) from sparkpay.merchants";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_created) from sparkpay.merchants";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Get Merchant by Terminal Owner: " + owner);
            networkResponse.setData((ArrayList) merchants);
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
            List<CardsMerchantModel> merchants;
            SQL = "SELECT a.id, a.merchant_id, a.merchant_name, a.merchant_state, a.merchant_country, a.merchant_category_code, a.merchant_service_charge, a.msc_cap, a.date_created, a.delete_flag, a.edit_flag, a.create_flag "
                    + "FROM sparkpay.merchants a "
                    + "LEFT JOIN sparkpay.terminals b "
                    + "ON a.merchant_id = b.merchant_id "
                    + "LEFT JOIN sparkpayweb_db.tbl_financial_institutions c "
                    + "ON b.cbn_bank_code = c.bank_code "
                + "WHERE c.acquirer_id = ?";
            merchants = jdbcTemplate.query(SQL, new Object[]{institution}, new MerchantsMapper());
            
            SQL = "SELECT MIN(date_created) from sparkpay.merchants";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_created) from sparkpay.merchants";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Get Merchant by institution: " + institution);
            networkResponse.setData((ArrayList) merchants);
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
            List<CardsMerchantModel> merchants;
            SQL = "SELECT * FROM sparkpay.merchants "
                + "WHERE id = ?";
            merchants = jdbcTemplate.query(SQL, new Object[]{id}, new MerchantsMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Get Merchant by id " + id);
            networkResponse.setData((ArrayList) merchants);
            
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
    public ResponseEntity Create(String merchant_id, String merchant_name, String merchant_state, String merchant_country, String merchant_category_code, double merchant_service_charge, double msc_cap, ArrayList ptsps, String sessiontoken) {
            NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            int retval;
            int terminalIDExit = CheckMerchantExit(merchant_id, "sparkpay.merchants", "merchant_id");
            if (terminalIDExit == 0) {
                int userrole = GetUserRole(sessiontoken);
                int create_flag = 0;
                if (userrole != 1 && userrole != 2) {
                    return responseManager.ResponseUnathorized();
                }
                if (userrole == 1) 
                    create_flag = 1;

                for (int i = 0; i < ptsps.size(); i++) {
                    SQL = "INSERT INTO sparkpayweb_db.tbl_map_merchants_ptsps "
                            + "(merchant_id, ptsp_id, date_created) "
                            + "VALUES(?, ?, now())";
                    jdbcTemplate.update(SQL, new Object[]{merchant_id, ptsps.get(i)});
                }
                SQL = "INSERT into sparkpay.merchants"
                        + "(merchant_id, merchant_name, merchant_state, merchant_country,"
                        + "merchant_category_code, create_flag, date_created, merchant_service_charge, msc_cap) "
                        + "VALUES(?, ?, ?, ?, ?, ?,now(), ?, ?)";
                retval = jdbcTemplate.update(SQL, new Object[]{merchant_id, merchant_name, merchant_state, merchant_country, merchant_category_code, create_flag, merchant_service_charge, msc_cap});
                
                SQL = "INSERT into postxnprocessor.tbl_merchants"
                        + "(merchant_id, merchant_name, merchant_state, country_code,"
                        + "merchant_category_code, create_flag, datecreated) "
                        + "VALUES(?, ?, ?, ?, ?, ?,now())";
                jdbcTemplate.update(SQL, new Object[]{merchant_id, merchant_name, merchant_state, merchant_country, merchant_category_code, create_flag});
                if (retval > 0) 
                    return responseManager.ResponseAccepted();
                else 
                    return responseManager.ResponseInternalServerError();
            }
            else {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Merchant with ID "+merchant_id+" already exist");
                return responseManager.ResponseOk(networkResponse);
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Edit(String merchant_id, String merchant_name, String merchant_state, String merchant_country, String merchant_category_code, double merchant_service_charge, double msc_cap, ArrayList ptsps, String sessiontoken) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "DELETE FROM sparkpayweb_db.tbl_map_merchants_ptsps WHERE merchant_id = ?";
                    jdbcTemplate.update(SQL, new Object[]{merchant_id});
                    for (int i = 0; i < ptsps.size(); i++) {
                        SQL = "INSERT INTO sparkpayweb_db.tbl_map_merchants_ptsps "
                                + "(merchant_id, ptsp_id, date_created) "
                                + "VALUES(?, ?, now())";
                        jdbcTemplate.update(SQL, new Object[]{merchant_id, ptsps.get(i)});
                    }
                    SQL = "UPDATE postxnprocessor.tbl_merchants SET merchant_name = ?, merchant_state = ?, country_code = ?, merchant_category_code = ? WHERE merchant_id = ?";
                    jdbcTemplate.update(SQL, new Object[]{merchant_name, merchant_state, merchant_country, merchant_category_code, merchant_id});
                    SQL = "UPDATE sparkpay.merchants SET merchant_name = ?, merchant_state = ?, merchant_country = ?, merchant_category_code = ?, merchant_service_charge = ?, msc_cap = ? WHERE merchant_id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{merchant_name, merchant_state, merchant_country, merchant_category_code, merchant_service_charge, msc_cap, merchant_id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckItemPendingEdit(merchant_id);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Merchant already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = Get(merchant_id);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    CardsMerchantModel merchant = networkResponse != null ? (CardsMerchantModel) networkResponse.getData().get(0) : new CardsMerchantModel();
                    for (int i = 0; i < ptsps.size(); i++) {
                        SQL = "INSERT INTO sparkpayweb_db.tbl_map_merchants_ptsps_bkp "
                                + "(merchant_id, ptsp_id, date_created) "
                                + "VALUES(?, ?, now())";
                        jdbcTemplate.update(SQL, new Object[]{merchant.getId(), ptsps.get(i)});
                    }
                    SQL = "INSERT into sparkpayweb_db.merchants_bkp"
                        + "(id, merchant_id, merchant_name, merchant_state, merchant_country,"
                        + "merchant_category_code, merchant_service_charge, msc_cap, date_created) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, now())";
                    jdbcTemplate.update(SQL, new Object[]{merchant.getId(), merchant_id, merchant_name, merchant_state, merchant_country, merchant_category_code, merchant_service_charge, msc_cap});
                    SQL = "UPDATE sparkpay.merchants SET edit_flag = 1 WHERE merchant_id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{merchant_id});
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
    public ResponseEntity SearchMerchants(
            String start_date,
            String end_date,
            String merchant_name,
            String merchant_id,
            String merchant_category_code){

        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery =  !start_date.equals("")
                    || !end_date.equals("")
                    || !merchant_name.equals("")
                    || !merchant_id.equals("")
                    || !merchant_category_code.equals("")
                    ? "WHERE" : "";


            if (!merchant_name.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" merchant_name LIKE '%" + merchant_name+"%'";
            }
            if (!merchant_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" merchant_id LIKE '%" + merchant_id+"%'";
            }
            if (!merchant_category_code.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" merchant_category_code LIKE '%" + merchant_category_code+"%'";
            }

            if (!start_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" date_created >= '" + start_date + "'";
            }
            if (!end_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" date_created < '" + end_date + "'";
            }
            String SQL;
            String allowedRows = " AND delete_flag = ? AND edit_flag = ? AND create_flag = ? ";
            List<CardsMerchantModel> merchants;
            SQL = "SELECT * FROM sparkpay.merchants "
                +whereQuery + allowedRows 
                + " ORDER BY date_created DESC";
            merchants = jdbcTemplate.query(SQL, new Object[]{"0", "0", "1"}, new MerchantsMapper());

            SQL = "SELECT MIN(date_created) from sparkpay.merchants";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(date_created) from sparkpay.merchants";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{ \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched merchants");
            networkResponse.setData((ArrayList) merchants);

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class MerchantsMapper implements RowMapper<CardsMerchantModel> {

        @Override
        public CardsMerchantModel mapRow(ResultSet rs, int arg1) throws SQLException {
            CardsMerchantModel merchant = new CardsMerchantModel();
            merchant.setId(rs.getInt("id"));
            merchant.setMerchant_id(rs.getString("merchant_id"));
            merchant.setMerchant_name(rs.getString("merchant_name"));
            merchant.setMerchant_category_code(rs.getString("merchant_category_code"));
            merchant.setDate_created(rs.getString("date_created"));
            merchant.setMerchant_state(rs.getString("merchant_state"));
            merchant.setMerchant_country(rs.getString("merchant_country"));
            merchant.setDelete_flag(rs.getInt("delete_flag"));
            merchant.setEdit_flag(rs.getInt("edit_flag"));
            merchant.setCreate_flag(rs.getInt("create_flag"));
            merchant.setMerchant_service_charge(rs.getDouble("merchant_service_charge"));
            merchant.setMsc_cap(rs.getDouble("msc_cap"));
            return merchant;
        }
    }
}
