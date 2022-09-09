/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.FinancialInstitutionsInterface;
import com.transgate.api.models.FinancialInstitutionModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.WalletModel;
import com.transgate.api.util.Randomizer;
import com.transgate.api.util.ResponseManager;
import com.transgate.api.util.Validators;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import com.transgate.api.interfaces.WalletsInterface;

/**
 *
 * @author Makintola
 */
@Service
public class WalletsService implements WalletsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    Randomizer randomizer = new Randomizer();
    Validators validators = new Validators();
    
    @Autowired
    private FinancialInstitutionsInterface financialInstitutionsInterface;
    
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
    
    private String GetNewWalletNumber() {
        int totalRows = 1;
        String walletnumber = "";
        try {
            while (totalRows > 0) {
                String SQL;
                walletnumber = Randomizer.GenerateWalletNumber();
                SQL = "SELECT COUNT(*) FROM tbl_wallets WHERE walletnumber = ?";
                totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{walletnumber}, int.class);
            }
            return walletnumber;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return "";
        }
    }
    
    private boolean CheckWalletToUser(String walletnumber, String username) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_map_user_wallets WHERE username = ? AND walletnumber = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{username, walletnumber}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckWalletActionPending(String walletnumber, String actionType) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_wallets_operations WHERE walletnumber = ? AND actionType = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{walletnumber, actionType}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private List<WalletModel> GetWalletsFromPendings(int id, String actionType) {
        try {
            String SQL;
            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.financialInstitutionCode, a.wallettype, a.balance, a.lien, a.assignee, a.actionType, a.note, a.date_created, "
                    + "b.name as financialInstitutionName "
                    + "FROM tbl_wallets_operations a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financialInstitutionCode = b.code "
                    + "WHERE a.id = ? AND a.actionType = ?"
                    + "ORDER BY a.id DESC";
            List<WalletModel> wallets = jdbcTemplate.query(SQL, new Object[]{id, actionType}, new WalletMapper2());
            return wallets;
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return null;
        }
    }
    
    @Override
    public ResponseEntity Create(String sessiontoken, String walletname, String institutionCode, String creator) {
        try {
            String SQL = null;
            int retval = 0;
            String walletnumber = GetNewWalletNumber();
            int userrole = GetUserRole(creator, sessiontoken);
            if (userrole == 1) {
                SQL = "INSERT INTO tbl_wallets(walletnumber, walletname, creator, financialInstitutionCode, date_created) VALUES(?, ?, ?, ?, now())";
                retval = jdbcTemplate.update(SQL, new Object[]{walletnumber, walletname, creator, institutionCode});
            }
            else if (userrole == 2) {
                SQL = "INSERT INTO tbl_wallets_operations(walletnumber, walletname, creator, financialInstitutionCode, actionType, date_created) VALUES(?, ?, ?, ?, 'create', now())";
                retval = jdbcTemplate.update(SQL, new Object[]{walletnumber, walletname, creator, institutionCode});
            }
            if (retval > 0) {
                return responseManager.ResponseAccepted();
            }
            else {
                return responseManager.ResponseBadRequest();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetWallets() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.financialInstitutionCode, a.wallettype, a.balance, a.lien, a.date_created, "
                    + "b.name as financialInstitutionName "
                    + "FROM tbl_wallets a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financialInstitutionCode = b.code "
                    + "ORDER BY a.id DESC";
            List<WalletModel> wallets = jdbcTemplate.query(SQL, new WalletMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All wallets");
            networkResponse.setData((ArrayList) wallets);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetWalletByNumber(String walletnumber) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.financialInstitutionCode, a.wallettype, a.balance, a.lien, a.date_created, "
                    + "b.name as financialInstitutionName "
                    + "FROM tbl_wallets a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financialInstitutionCode = b.code "
                    + "WHERE a.walletnumber = ?";
            List<WalletModel> wallets = jdbcTemplate.query(SQL, new Object[]{walletnumber}, new WalletMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Get wallet " + walletnumber + " detail");
            networkResponse.setData((ArrayList) wallets);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetWalletsForActions() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.financialInstitutionCode, a.wallettype, a.balance, a.lien, a.assignee, a.actionType, a.note, a.date_created, "
                    + "b.name as financialInstitutionName "
                    + "FROM tbl_wallets_operations a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financialInstitutionCode = b.code "
                    + "ORDER BY a.id DESC";
            List<WalletModel> wallets = jdbcTemplate.query(SQL, new WalletMapper2());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All wallets");
            networkResponse.setData((ArrayList) wallets);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity InitiateDebitCreditWallet(String sessiontoken, String walletnumber, String actionType, float amount, String fundby) {
        try {
            String SQL;
            if (actionType.equals("dr")) {
                SQL = "SELECT balance from tbl_wallets WHERE walletnumber = ?";
                float balance = jdbcTemplate.queryForObject(SQL, new Object[]{walletnumber}, float.class);
                if (balance < 100) {
                    return responseManager.ResponseUnathorized();
                }
            }
            int userrole = GetUserRole(fundby, sessiontoken);
            SQL = "INSERT into tbl_wallet_fundings(walletnumber, amount, fundby, debit_or_credit, action_date) VALUES(?, ?, ?, ?, now())";
            int retval = jdbcTemplate.update(SQL, new Object[]{walletnumber, amount, fundby, actionType});
            if (retval > 0) {
                switch (userrole) {
                    case 1:
                        if (actionType.equals("cr")){
                            SQL = "UPDATE tbl_wallets SET balance = (balance + " + amount + ") WHERE walletnumber = ?";
                        }
                        else {
                            SQL = "UPDATE tbl_wallets SET balance = (balance - " + amount + ") WHERE walletnumber = ?";
                        }
                        jdbcTemplate.update(SQL, new Object[]{walletnumber});
                        return responseManager.ResponseAccepted();
                    case 2:
                        ResponseEntity responseEntity = GetWalletByNumber(walletnumber);
                        NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                        WalletModel wallet = networkResponse != null ? (WalletModel) networkResponse.getData().get(0) : new WalletModel();
                        String note = actionType.equals("cr") ? "Credit Wallet with "+amount : "Debit " + amount + " from wallet";
                        SQL = "INSERT INTO tbl_wallets_operations(walletnumber, walletname, creator, financialInstitutionCode, balance, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, ?, now())";
                        retval = jdbcTemplate.update(SQL, new Object[]{wallet.getWalletnumber(), wallet.getWalletname(), fundby, wallet.getFinancialInstitutionCode(), amount, actionType.equals("cr") ? "credit" : "debit", note});
                        if (retval > 0) {
                            return responseManager.ResponseAccepted();
                        }
                        else {
                            return responseManager.ResponseBadRequest();
                        }
                    default:
                        return responseManager.ResponseUnathorized();
                }
            }
            else {
                return responseManager.ResponseBadRequest();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity MapWalletToUser(String sessiontoken, String walletnumber, String assignee, String username) {
        try {
            boolean hasWallet = CheckWalletToUser(walletnumber, assignee);
            if (hasWallet) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("success");
                networkResponse.setMessage("Wallet already assigned to user");
                return responseManager.ResponseOk(networkResponse);
            }
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retval;
            switch (userrole) {
                case 1:
                    SQL = "INSERT into tbl_map_user_wallets(username, walletnumber, date_created) VALUES(?, ?, now())";
                    retval = jdbcTemplate.update(SQL, new Object[]{assignee, walletnumber});
                    if (retval > 0) {
                        return responseManager.ResponseAccepted();
                    }
                    else {
                        return responseManager.ResponseBadRequest();
                    }
                case 2:
                    boolean checkPendingAction = CheckWalletActionPending(walletnumber, "assign");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Wallet already pending assignment");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetWalletByNumber(walletnumber);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    WalletModel wallet = networkResponse != null ? (WalletModel) networkResponse.getData().get(0) : new WalletModel();
                    String note = "Assign wallet to user - " + assignee;
                    SQL = "INSERT INTO tbl_wallets_operations(walletnumber, walletname, creator, financialInstitutionCode, balance, assignee, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, 'assign', ?, now())";
                    retval = jdbcTemplate.update(SQL, new Object[]{wallet.getWalletnumber(), wallet.getWalletname(), username, wallet.getFinancialInstitutionCode(), wallet.getBalance(), assignee, note});
                    if (retval > 0) {
                        return responseManager.ResponseAccepted();
                    }
                    else {
                        return responseManager.ResponseInternalServerError();
                    }
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity DeleteWallet(String sessiontoken, String walletnumber, String username) {
        try {
            NetworkResponse response = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.financialInstitutionCode, a.wallettype, a.balance, a.lien, a.date_created, "
                    + "b.name as financialInstitutionName "
                    + "FROM tbl_wallets a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financialInstitutionCode = b.code "
                    + "WHERE a.walletnumber = ?";
            List<WalletModel> wallets = jdbcTemplate.query(SQL, new Object[] {walletnumber}, new WalletMapper());
            float balance = 0;
            if (wallets.size() > 0) {
                balance = wallets.get(0).getBalance();
            }
            if (balance > 0 || balance < 0){
                response.setCode(200);
                response.setStatus("failed");
                response.setMessage("To delete a wallet, balance must be 0.00");
                return responseManager.ResponseOk(response);
            }
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "DELETE FROM tbl_Wallets WHERE walletnumber = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{walletnumber});
                    if (retVal > 0)
                        return responseManager.ResponseDeleted();
                    else
                        return responseManager.ResponseInternalServerError();
                case 2:
                    boolean checkPendingAction = CheckWalletActionPending(walletnumber, "delete");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Wallet already pending delete");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetWalletByNumber(walletnumber);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    WalletModel wallet = networkResponse != null ? (WalletModel) networkResponse.getData().get(0) : new WalletModel();
                    SQL = "INSERT INTO tbl_wallets_operations(walletnumber, walletname, creator, financialInstitutionCode, balance, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, 'delete', 'Delete wallet', now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{wallet.getWalletnumber(), wallet.getWalletname(), username, wallet.getFinancialInstitutionCode(), wallet.getBalance()});
                    if (retVal > 0) 
                        return responseManager.ResponseDeleted();
                    else 
                        return responseManager.ResponseInternalServerError();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity EditWallet(String sessiontoken, String walletnumber, String walletname, String institutionCode, String editor) {
        try {
            String SQL;
            int userrole = GetUserRole(editor, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE tbl_wallets SET walletname = ?, financialInstitutionCode = ? WHERE walletnumber = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{walletname, institutionCode, walletnumber});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseInternalServerError();
                case 2:
                    boolean checkPendingAction = CheckWalletActionPending(walletnumber, "edit");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Wallet already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetWalletByNumber(walletnumber);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    WalletModel wallet = networkResponse != null ? (WalletModel) networkResponse.getData().get(0) : new WalletModel();
                    SQL = "INSERT INTO tbl_wallets_operations(walletnumber, walletname, creator, financialInstitutionCode, balance, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, 'edit', ?, now())";
                    String nameChange = wallet.getWalletname().equals(walletname) ? wallet.getWalletname() : wallet.getWalletname() + "|" + walletname;
                    responseEntity = financialInstitutionsInterface.GetFinancialInstitutionByCode(sessiontoken, institutionCode);
                    networkResponse = (NetworkResponse) responseEntity.getBody();
                    FinancialInstitutionModel financialInstitutionModel = networkResponse != null ? (FinancialInstitutionModel) networkResponse.getData().get(0) : new FinancialInstitutionModel();
                    String institutionChange = wallet.getFinancialInstitutionCode().equals(institutionCode) ? wallet.getFinancialInstitutionCode() : institutionCode;
                    String note = wallet.getWalletname().equals(walletname) ? "" : "Change wallet name from " +  wallet.getWalletname()+ " to " + walletname;
                    note = wallet.getFinancialInstitutionCode().equals(institutionCode) ? note : !note.equals("") ? note + ", change wallet institution from " + wallet.getFinancialInstitutionName() + " to " + financialInstitutionModel.getName() : "Change wallet institution from " + wallet.getFinancialInstitutionName() + " to " + financialInstitutionModel.getName();
                    retVal = jdbcTemplate.update(SQL, new Object[]{wallet.getWalletnumber(), nameChange, editor, institutionChange, wallet.getBalance(), note});
                    if (retVal > 0) {
                        return responseManager.ResponseAccepted();
                    }
                    else {
                        return responseManager.ResponseInternalServerError();
                    }
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity WalletApprovals(String sessiontoken, int id, String actionType, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            int retVal2;
            if (userrole == 1 || userrole == 3) {
                List<WalletModel> wallets = GetWalletsFromPendings(id, actionType);
                if (wallets.size() == 1) {
                    switch (actionType) {
                        case "delete":
                            SQL = "DELETE FROM tbl_wallets_operations WHERE id = ? AND actionType = 'delete'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "DELETE FROM tbl_wallets WHERE walletnumber = ?";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{wallets.get(0).getWalletnumber()});
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseDeleted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "edit":
                            SQL = "DELETE FROM tbl_wallets_operations WHERE id = ? AND actionType = 'edit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "UPDATE tbl_wallets SET walletname = ?, financialInstitutionCode = ? WHERE walletnumber = ?";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{wallets.get(0).getWalletname(), wallets.get(0).getFinancialInstitutionCode(), wallets.get(0).getWalletnumber()});
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "assign":
                            SQL = "DELETE FROM tbl_wallets_operations WHERE id = ? AND actionType = 'assign'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "INSERT into tbl_map_user_wallets(username, walletnumber, date_created) VALUES(?, ?, now())";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{wallets.get(0).getAssignnee(), wallets.get(0).getWalletnumber()});
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "credit":
                            SQL = "DELETE FROM tbl_wallets_operations WHERE id = ? AND actionType = 'credit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "UPDATE tbl_wallets SET balance = (balance + " + wallets.get(0).getBalance() + ") WHERE walletnumber = ?";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{wallets.get(0).getWalletnumber()});
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "debit":
                            SQL = "DELETE FROM tbl_wallets_operations WHERE id = ? AND actionType = 'debit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "UPDATE tbl_wallets SET balance = (balance - " + wallets.get(0).getBalance() + ") WHERE walletnumber = ?";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{wallets.get(0).getWalletnumber()});
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "create":
                            SQL = "DELETE FROM tbl_wallets_operations WHERE id = ? AND actionType = 'create'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "INSERT INTO tbl_wallets(walletnumber, walletname, creator, financialInstitutionCode, date_created) VALUES(?, ?, ?, ?, ?)";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{wallets.get(0).getWalletnumber(), wallets.get(0).getWalletname(), wallets.get(0).getCreator(), wallets.get(0).getFinancialInstitutionCode(), wallets.get(0).getDate_created()});
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        default:
                            return responseManager.ResponseBadRequest();
                    }
                }
                else {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(200);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Not found");
                    return responseManager.ResponseNotFound(networkResponse);
                }
            }
            else
                return responseManager.ResponseUnathorized();
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    class WalletMapper implements RowMapper<WalletModel> {
        @Override
        public WalletModel mapRow(ResultSet rs, int arg1) throws SQLException {
            WalletModel response = new WalletModel();
            
            response.setId(rs.getInt("id"));
            response.setWalletnumber(rs.getString("walletnumber"));
            response.setWalletname(rs.getString("walletname"));
            response.setCreator(rs.getString("creator"));
            response.setFinancialInstitutionCode(rs.getString("financialInstitutionCode") != null ? rs.getString("financialInstitutionCode") : "");
            response.setFinancialInstitutionName(rs.getString("financialInstitutionName") != null ? rs.getString("financialInstitutionName") : "");
            response.setWallettype(rs.getInt("wallettype"));
            response.setBalance(rs.getFloat("balance"));
            response.setLien(rs.getFloat("lien"));
            response.setDate_created(rs.getString("date_created"));
            response.setDate_updated(null);
            return response;
        }
    }
    
    class WalletMapper2 implements RowMapper<WalletModel> {
        @Override
        public WalletModel mapRow(ResultSet rs, int arg1) throws SQLException {
            WalletModel response = new WalletModel();
            
            response.setId(rs.getInt("id"));
            response.setWalletnumber(rs.getString("walletnumber"));
            response.setWalletname(rs.getString("walletname"));
            response.setCreator(rs.getString("creator"));
            String institutionCodes = rs.getString("financialInstitutionCode") != null ? rs.getString("financialInstitutionCode") : "";
            String institutionName = rs.getString("financialInstitutionName") != null ? rs.getString("financialInstitutionName") : "";
            if (rs.getString("actionType").equals("edit")) {
                String[] arrOfinstitutionCodes = institutionCodes.split("@", -1);
                for (String a : arrOfinstitutionCodes)
			System.out.println(a);
                System.out.println(arrOfinstitutionCodes.length);
                if (arrOfinstitutionCodes.length == 4) {
                    response.setFinancialInstitutionCode(arrOfinstitutionCodes[0]);
                    response.setFinancialInstitutionName(arrOfinstitutionCodes[2] + " -> " + arrOfinstitutionCodes[3]);
                }
                else {
                    response.setFinancialInstitutionCode(arrOfinstitutionCodes[0]);
                    response.setFinancialInstitutionName(institutionName);
                }
            }
            else {
                response.setFinancialInstitutionCode(institutionCodes);
                response.setFinancialInstitutionName(institutionName);
            }
//            response.setFinancialInstitutionCode(rs.getString("financialInstitutionCode") != null ? rs.getString("financialInstitutionCode") : "");
//            response.setFinancialInstitutionName(rs.getString("financialInstitutionName") != null ? rs.getString("financialInstitutionName") : "");
            response.setWallettype(rs.getInt("wallettype"));
            response.setBalance(rs.getFloat("balance"));
            response.setLien(rs.getFloat("lien"));
            response.setDate_created(rs.getString("date_created"));
            response.setAssignnee(rs.getString("assignee"));
            response.setActionType(rs.getString("actionType"));
            response.setNote(rs.getString("note"));
            response.setDate_updated(null);
            return response;
        }
    }

}
