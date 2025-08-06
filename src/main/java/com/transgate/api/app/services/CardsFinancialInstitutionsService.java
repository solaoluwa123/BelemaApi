/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.CardsFinancialInstitutionsInterface;
import com.transgate.api.models.CardsFinancialInstitutionModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.util.ResponseManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
public class CardsFinancialInstitutionsService implements CardsFinancialInstitutionsInterface {
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
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_financial_institutions WHERE id = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckItemPendingEdit(String id, String column) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_financial_institutions WHERE "+column+" = ? AND edit_flag = 1";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{id}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckBankCodeExist(String code) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM sparkpayweb_db.tbl_financial_institutions WHERE bank_code = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{code}, int.class);

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
            List<CardsFinancialInstitutionModel> cardFI;
            if (!pending) {
                SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                    + "WHERE create_flag = 1 AND delete_flag = 0 AND edit_flag = 0 "
                    + "ORDER BY id DESC";
            }
            else {
                SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                    + "WHERE create_flag = 0 OR delete_flag = 1 OR edit_flag = 1 "
                    + "ORDER BY id DESC";
            }
            cardFI = jdbcTemplate.query(SQL, new CardFIMapper());
            SQL = "SELECT MIN(created) from sparkpayweb_db.tbl_financial_institutions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(created) from sparkpayweb_db.tbl_financial_institutions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{\"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institutions");
            networkResponse.setData((ArrayList) cardFI);
            networkResponse.setMeta(meta);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetByMerchantUser(String merchant) {
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
            SQL = "SELECT cbn_bank_code FROM sparkpay.terminals WHERE merchant_id IN "+inString.toString();
            rows = jdbcTemplate.queryForList(SQL);
            inString = new StringBuilder("(");
            for (final Map<String, Object> row : rows) {
                inString.append(row.get("cbn_bank_code"));
                inString.append(",");
            }
            inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals("(")) inString = inString.deleteCharAt(inString.length() - 1);
            if (inString.toString().equals(""))
                inString = inString.append("(-1");
            inString = inString.append(")");
            List<CardsFinancialInstitutionModel> cardFI;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                + "WHERE bank_code IN " + inString.toString();
            cardFI = jdbcTemplate.query(SQL, new CardFIMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Financial Institutions by merchant: " + merchant);
            networkResponse.setData((ArrayList) cardFI);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    public ResponseEntity Get(String id, String column) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsFinancialInstitutionModel> cardFI;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                + "WHERE "+column+" = ?";
            cardFI = jdbcTemplate.query(SQL, new Object[]{id}, new CardFIMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Financial Institutions by "+column+": " + id);
            networkResponse.setData((ArrayList) cardFI);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    public ResponseEntity Get(int id) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<CardsFinancialInstitutionModel> cardFI;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                + "WHERE id = ?";
            cardFI = jdbcTemplate.query(SQL, new Object[]{id}, new CardFIMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Financial Institutions by ID: " + id);
            networkResponse.setData((ArrayList) cardFI);
            
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
    public ResponseEntity Create(String acquirer_id, String institution_name, String issuer_id, String bank_code, String sessiontoken) {
        try {
            String SQL;
            int retval;
            if (CheckBankCodeExist(bank_code)) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Bank code already in use");
                return responseManager.ResponseOk(networkResponse);
            }
            int userrole = GetUserRole(sessiontoken);
            int create_flag = 0;
            if (userrole != 1 && userrole != 2) {
                return responseManager.ResponseUnathorized();
            }
            if (userrole == 1) 
                create_flag = 1;
            
            SQL = "INSERT into sparkpayweb_db.tbl_financial_institutions"
                    + "(acquirer_id, institution_name, issuer_id, bank_code, created, create_flag) VALUES(?, ?, ?, ?, now(), ?)";
            retval = jdbcTemplate.update(SQL, new Object[]{acquirer_id, institution_name, issuer_id, bank_code, create_flag});
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
    public ResponseEntity Edit(int id, String acquirer_id, String institution_name, String issuer_id, String bank_code, String sessiontoken) {
        try {
            String SQL;
            int userrole = GetUserRole(sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE sparkpayweb_db.tbl_financial_institutions "
                            + "SET acquirer_id = ?, institution_name = ?, issuer_id = ?, bank_code = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{acquirer_id, institution_name, issuer_id, bank_code, id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckItemPendingEdit(id);
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Financial Institution already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "INSERT into sparkpayweb_db.tbl_financial_institutions_bkp"
                        + "(id, acquirer_id, institution_name, issuer_id, bank_code, created) "
                        + "VALUES(?, ?, ?, ?, ?,now())";
                    jdbcTemplate.update(SQL, new Object[]{id, acquirer_id, institution_name, issuer_id, bank_code});
                    SQL = "UPDATE sparkpayweb_db.tbl_financial_institutions SET edit_flag = 1 WHERE id = ?";
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
    public ResponseEntity SearchCardsFinancialInstitutions(
           String start_date,
           String end_date,
           String acquirer_id,
           String institution_name,
           String issuer_id){

        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String whereQuery =  !start_date.equals("")
                    || !end_date.equals("")
                    || !acquirer_id.equals("")
                    || !institution_name.equals("")
                    || !issuer_id.equals("")
                    ? "WHERE" : "";


            if (!acquirer_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" acquirer_id LIKE '%" + acquirer_id+"%'";
            }
            if (!institution_name.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" institution_name LIKE '%" + institution_name+"%'";
            }
            if (!issuer_id.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" issuer_id LIKE '%" + issuer_id+"%'";
            }

            if (!start_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" created >= '" + start_date + "'";
            }
            if (!end_date.equals("")) {
                whereQuery = !whereQuery.equals("WHERE") ? whereQuery+" AND " : whereQuery+"";
                whereQuery+=" created < '" + end_date + "'";
            }
            String SQL;
            String allowedRows = " AND delete_flag = ? AND edit_flag = ? AND create_flag = ? ";
            List<CardsFinancialInstitutionModel> FIs;
            SQL = "SELECT * FROM sparkpayweb_db.tbl_financial_institutions "
                +whereQuery + allowedRows 
                + " ORDER BY created DESC";
            FIs = jdbcTemplate.query(SQL, new Object[]{"0", "0", "1"}, new CardFIMapper());

            SQL = "SELECT MIN(created) from sparkpayweb_db.tbl_financial_institutions";
            String minDate = jdbcTemplate.queryForObject(SQL, String.class);
            SQL = "SELECT MAX(created) from sparkpayweb_db.tbl_financial_institutions";
            String maxDate = jdbcTemplate.queryForObject(SQL, String.class);
            String meta = "{ \"minDate\": \"" + minDate + "\", \"maxDate\": \"" + maxDate + "\"}";
            networkResponse.setMeta(meta);

            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Searched Financial Institutions");
            networkResponse.setData((ArrayList) FIs);

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
     }
    
    class CardFIMapper implements RowMapper<CardsFinancialInstitutionModel> {

        @Override
        public CardsFinancialInstitutionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            CardsFinancialInstitutionModel fi = new CardsFinancialInstitutionModel();
            fi.setId(rs.getInt("id"));
            fi.setAcquirer_id(rs.getString("acquirer_id"));
            fi.setInstitution_name(rs.getString("institution_name"));
            fi.setIssuer_id(rs.getString("issuer_id"));
            fi.setBank_code(rs.getString("bank_code"));
            fi.setCreated(rs.getString("created"));
            fi.setDelete_flag(rs.getInt("delete_flag"));
            fi.setEdit_flag(rs.getInt("edit_flag"));
            fi.setCreate_flag(rs.getInt("create_flag"));
            return fi;
        }
    }
}
