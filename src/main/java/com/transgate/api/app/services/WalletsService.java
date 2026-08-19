/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.app.TransgateApi;
import com.transgate.api.interfaces.FinancialInstitutionsInterface;
import com.transgate.api.models.FinancialInstitutionModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.WalletModel;
import com.transgate.api.util.PlatformRole;
import com.transgate.api.util.Randomizer;
import com.transgate.api.util.ResponseManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import com.transgate.api.interfaces.WalletsInterface;
import java.math.BigDecimal;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Makintola
 */
@Service
public class WalletsService implements WalletsInterface {

    @Autowired
    @Qualifier("jdbcTemplate")
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    
    @Autowired
    private FinancialInstitutionsInterface financialInstitutionsInterface;
    private static Logger logger = Logger.getLogger(WalletsService.class.getName());

    private BigDecimal parseDecimal(String value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(trimmed);
    }

    private BigDecimal parseDecimal(ResultSet rs, String column) throws SQLException {
        return parseDecimal(rs.getString(column));
    }

    /**
     * Belema stores wallettype as a name (e.g. PSSP); other DBs store a numeric id.
     * {@code ResultSet.getInt} throws when the column is a non-numeric string.
     */
    private int parseWalletTypeId(String raw) {
        if (raw == null) {
            return 0;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            switch (value.toLowerCase()) {
                case "merchant":
                    return 1;
                case "pssp":
                    return 2;
                case "ptsp":
                    return 3;
                case "super agent":
                case "superagent":
                    return 4;
                case "switch":
                    return 5;
                default:
                    return 0;
            }
        }
    }

    private String walletTypeName(int typeId, String raw) {
        switch (typeId) {
            case 1:
                return "Merchant";
            case 2:
                return "PSSP";
            case 3:
                return "PTSP";
            case 4:
                return "Super Agent";
            case 5:
                return "Switch";
            default:
                if (raw != null && !raw.trim().isEmpty() && !raw.trim().matches("\\d+")) {
                    return raw.trim();
                }
                return "Others";
        }
    }

    private void applyWalletType(WalletModel response, ResultSet rs) throws SQLException {
        String raw = rs.getString("wallettype");
        int typeId = parseWalletTypeId(raw);
        response.setWallettype(typeId);
        response.setWalletTypeName(walletTypeName(typeId, raw));
    }

    private static final String WALLET_INSTITUTION_NAME_SELECT =
            "COALESCE(NULLIF(b.name, ''), n.institution_name, '') as financialInstitutionname ";

    private static final String WALLET_INSTITUTION_JOINS =
            "LEFT JOIN tbl_financial_institutions b ON a.financialinstitutioncode = b.code "
            + "LEFT JOIN ajiswitch_db.tbl_nodes n ON a.financialinstitutioncode = n.institution_code ";

    private static final String WALLET_OPS_INSTITUTION_NAME_SELECT =
            "COALESCE(NULLIF(b.name, ''), n.institution_name, '') as financialInstitutionName ";

    private static final String WALLET_OPS_INSTITUTION_JOINS =
            "LEFT JOIN tbl_financial_institutions b ON a.financialInstitutionCode = b.code "
            + "LEFT JOIN ajiswitch_db.tbl_nodes n ON a.financialInstitutionCode = n.institution_code ";
    
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
    
    private String GetNewWalletNumber() {
        int totalRows = 1;
        String walletnumber = "";
        try {
            while (totalRows > 0) {
                String SQL;
                walletnumber = Randomizer.GenerateWalletNumber();
                SQL = "SELECT COUNT(*) FROM ajiswitch_db.tbl_wallets WHERE walletnumber = ?";
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
                    + WALLET_OPS_INSTITUTION_NAME_SELECT
                    + "FROM tbl_wallets_operations a "
                    + WALLET_OPS_INSTITUTION_JOINS
                    + "WHERE a.id = ? AND a.actionType = ? "
                    + "ORDER BY a.id DESC";
            List<WalletModel> wallets = jdbcTemplate.query(SQL, new Object[]{id, actionType}, new WalletMapper2());
            return wallets;
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return null;
        }
    }
    
    @Override
    public ResponseEntity Create(String sessiontoken, String walletname, String institutionCode, String creator, int type) {
        try {
            String SQL;
            int retval = 0;
            String walletnumber = GetNewWalletNumber();
            int userrole = GetUserRole(creator, sessiontoken);
            if (userrole == 1) {
                SQL = "INSERT INTO ajiswitch_db.tbl_wallets(walletnumber, walletname, creator, financialinstitutioncode, creationdate, balance, lien, wallettype, is_active) VALUES(?, ?, ?, ?, now(), 0.00, 0.00, ?, 1)";
                retval = jdbcTemplate.update(SQL, new Object[]{walletnumber, walletname, creator, institutionCode, type});
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
    public ResponseEntity GetWallets(String institutioncode){
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.creationdate, a.financialinstitutioncode, a.balance, a.lien, a.wallettype, a.is_active, "
                    + WALLET_INSTITUTION_NAME_SELECT
                    + "FROM ajiswitch_db.tbl_wallets a "
                    + WALLET_INSTITUTION_JOINS
                    + "WHERE a.financialinstitutioncode = ? "
                    + "ORDER BY a.id DESC";
            List<WalletModel> wallets = jdbcTemplate.query(SQL, new Object[]{institutioncode}, new WalletMapper());
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
    public ResponseEntity GetWallets() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.creationdate, a.financialinstitutioncode, a.balance, a.lien, a.wallettype, a.is_active, "
                    + WALLET_INSTITUTION_NAME_SELECT
                    + "FROM ajiswitch_db.tbl_wallets a "
                    + WALLET_INSTITUTION_JOINS
                    + "ORDER BY a.id DESC";
            List<WalletModel> wallets = jdbcTemplate.query(SQL, new WalletMapperAdmin());
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
            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.creationdate, a.financialinstitutioncode, a.balance, a.lien, a.wallettype, a.is_active, "
                    + WALLET_INSTITUTION_NAME_SELECT
                    + "FROM ajiswitch_db.tbl_wallets a "
                    + WALLET_INSTITUTION_JOINS
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
                    + WALLET_OPS_INSTITUTION_NAME_SELECT
                    + "FROM tbl_wallets_operations a "
                    + WALLET_OPS_INSTITUTION_JOINS
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
    public ResponseEntity InitiateDebitCreditWallet(String sessiontoken, String walletnumber, String actionType, BigDecimal amount, String fundby) {
        try {
            String SQL;
            if (actionType.equals("dr")) {
                SQL = "SELECT balance from ajiswitch_db.tbl_wallets WHERE walletnumber = ?";
                float balance = jdbcTemplate.queryForObject(SQL, new Object[]{walletnumber}, float.class);
                if (balance < 100) {
                    return responseManager.ResponseUnathorized();
                }
            }
            SQL = "SELECT is_active FROM ajiswitch_db.tbl_wallets WHERE walletnumber = ?";
            int isActive = jdbcTemplate.queryForObject(SQL, new Object[]{walletnumber}, int.class);
            if (isActive == 0) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setMessage("Wallet is Inactive");
                networkResponse.setStatus("error");
                return responseManager.ResponseOk(networkResponse);
            }
            int userrole = GetUserRole(fundby, sessiontoken);
            SQL = "INSERT into ajiswitch_db.tbl_wallet_activities(walletnumber, amount, credit_or_debit, actor, activity_date_time) VALUES(?, ?, ?, ?, now())";
            int retval = jdbcTemplate.update(SQL, new Object[]{walletnumber, amount, actionType, fundby});
            if (retval > 0) {
                switch (userrole) {
                    case 1:
                        if (actionType.equals("cr")){
                            SQL = "UPDATE ajiswitch_db.tbl_wallets SET balance = balance + ? WHERE walletnumber = ?";
                            jdbcTemplate.update(SQL, new Object[]{amount, walletnumber});
                        }
                        else {
                            SQL = "UPDATE ajiswitch_db.tbl_wallets SET balance = balance - lien - ? WHERE walletnumber = ? AND balance - lien - ? > -1 AND is_active = 1";                            
                            jdbcTemplate.update(SQL, new Object[]{amount, walletnumber, amount});
                        }
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
            double balance = 0;
            if (wallets.size() > 0) {
                balance = wallets.get(0).getBalance().doubleValue();
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

    public ResponseEntity EditWallet(String sessiontoken, String walletnumber, String walletname, String institutionCode, String baseAmount, String editor) {
        try {
            String SQL;
            logger.info(String.format("EditWallet called with sessiontoken=%s, walletnumber=%s, walletname=%s, institutionCode=%s, baseAmount=%s, editor=%s",
                    sessiontoken, walletnumber, walletname, institutionCode, baseAmount, editor));

            int userrole = GetUserRole(editor, sessiontoken);
            logger.info(String.format("EditWallet - resolved userrole=%d for editor=%s", userrole, editor));

            int retVal;
            switch (userrole) {
                case 1:
                    // Belema tbl_wallets has no baseAmount column
                    SQL = "UPDATE ajiswitch_db.tbl_wallets SET walletname = ?, financialInstitutioncode = ? WHERE walletnumber = ?";
                    logger.info(String.format("EditWallet (case 1) - about to execute SQL: %s | params: walletname=%s, institutionCode=%s, walletnumber=%s",
                            SQL, walletname, institutionCode, walletnumber));

                    retVal = jdbcTemplate.update(SQL, new Object[]{walletname, institutionCode, walletnumber});
                    logger.info(String.format("EditWallet (case 1) - update returned retVal=%d", retVal));

                    if (retVal > 0) {
                        SQL = "INSERT into ajiswitch_db.tbl_wallet_activities(walletnumber, amount, credit_or_debit, actor, activity_date_time) VALUES(?, ?, ?, ?, now())";
                        logger.info(String.format("EditWallet (case 1) - about to execute SQL: %s | params: walletnumber=%s, amount=%d, credit_or_debit=%s, actor=%s",
                                SQL, walletnumber, 0, "ne", editor));

                        int retval = jdbcTemplate.update(SQL, new Object[]{walletnumber, 0, "ne", editor});
                        logger.info(String.format("EditWallet (case 1) - insert activity returned retval=%d", retval));

                        return responseManager.ResponseAccepted();
                    }
                    else
                        return responseManager.ResponseInternalServerError();
                case 2:
                    boolean checkPendingAction = CheckWalletActionPending(walletnumber, "edit");
                    logger.info(String.format("EditWallet (case 2) - CheckWalletActionPending returned %b for walletnumber=%s", checkPendingAction, walletnumber));

                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Wallet already pending edit");
                        logger.info(String.format("EditWallet (case 2) - wallet already pending edit for walletnumber=%s, returning failed response", walletnumber));
                        return responseManager.ResponseOk(networkResponse);
                    }

                    ResponseEntity responseEntity = GetWalletByNumber(walletnumber);
                    logger.info(String.format("EditWallet (case 2) - GetWalletByNumber called for walletnumber=%s, responseEntity=%s", walletnumber, responseEntity));

                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    WalletModel wallet = networkResponse != null ? (WalletModel) networkResponse.getData().get(0) : new WalletModel();
                    logger.info(String.format("EditWallet (case 2) - loaded wallet: walletnumber=%s, walletname=%s, financialInstitutionCode=%s, balance=%s",
                            wallet.getWalletnumber(), wallet.getWalletname(), wallet.getFinancialInstitutionCode(), wallet.getBalance()));

                    // Belema tbl_wallets_operations has no baseAmount column
                    SQL = "INSERT INTO tbl_wallets_operations(walletnumber, walletname, creator, financialInstitutionCode, balance, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, 'edit', ?, now())";
                    String nameChange = wallet.getWalletname().equals(walletname) ? wallet.getWalletname() : wallet.getWalletname() + "|" + walletname;
                    logger.info(String.format("EditWallet (case 2) - computed nameChange=%s", nameChange));

                    responseEntity = financialInstitutionsInterface.GetFinancialInstitutionByCode(sessiontoken, institutionCode);
                    logger.info(String.format("EditWallet (case 2) - GetFinancialInstitutionByCode called for code=%s, responseEntity=%s", institutionCode, responseEntity));

                    networkResponse = (NetworkResponse) responseEntity.getBody();
                    FinancialInstitutionModel financialInstitutionModel = networkResponse != null ? (FinancialInstitutionModel) networkResponse.getData().get(0) : new FinancialInstitutionModel();
                    logger.info(String.format("EditWallet (case 2) - loaded financialInstitutionModel: code=%s, name=%s",
                            financialInstitutionModel.getCode(), financialInstitutionModel.getName()));

                    String institutionChange = wallet.getFinancialInstitutionCode().equals(institutionCode) ? wallet.getFinancialInstitutionCode() : institutionCode;
                    logger.info(String.format("EditWallet (case 2) - computed institutionChange=%s", institutionChange));

                    String note = wallet.getWalletname().equals(walletname) ? "" : "Change wallet name from " + wallet.getWalletname() + " to " + walletname;
                    note = wallet.getFinancialInstitutionCode().equals(institutionCode) ? note : !note.equals("") ? note + ", change wallet institution from " + wallet.getFinancialInstitutionName() + " to " + financialInstitutionModel.getName() : "Change wallet institution from " + wallet.getFinancialInstitutionName() + " to " + financialInstitutionModel.getName();
                    logger.info(String.format("EditWallet (case 2) - final note=%s", note));

                    logger.info(String.format("EditWallet (case 2) - about to execute SQL: %s | params: walletnumber=%s, walletname=%s, creator=%s, financialInstitutionCode=%s, balance=%s, note=%s",
                            SQL, wallet.getWalletnumber(), nameChange, editor, institutionChange, wallet.getBalance(), note));

                    retVal = jdbcTemplate.update(SQL, new Object[]{wallet.getWalletnumber(), nameChange, editor, institutionChange, wallet.getBalance(), note});
                    logger.info(String.format("EditWallet (case 2) - insert operation returned retVal=%d", retVal));

                    if (retVal > 0) {
                        return responseManager.ResponseAccepted();
                    }
                    else {
                        return responseManager.ResponseInternalServerError();
                    }
                default:
                    logger.info(String.format("EditWallet - unauthorized access attempt by editor=%s with userrole=%d", editor, userrole));
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            logger.info(String.format("EditWallet - DataAccessException caught: %s", ex.getMessage()));
            return responseManager.ResponseInternalServerError();
        }
    }




    @Override
    public ResponseEntity GetWalletActivity(String walletnumber, String startDate, String endDate, int page, int limit, boolean isCurrent) {
        NetworkResponse networkResponse = new NetworkResponse();
        try{
            int offset = page > 1 ? (page - 1) * limit : 0;
//            String meta;
//            int offset = page > 1 ? (page - 1) * limit : 0;
            String table = isCurrent ? "ajiswitch_db.tbl_wallet_activities " : "ajiswitch_db.tbl_wallet_activities ";
            String debitActors = "('eseoghene.onoriode@habaripay@com','omoyosola.afolayan@habaripay.com','valerie.ejegi@habaripay.com')";
            String SQL = "SELECT * FROM "+table
                    + "WHERE walletnumber = ? "
                    + "AND activity_date_time >= ? AND activity_date_time < ? "
                    + "AND credit_or_debit = 'cr' AND actor != 'SYSTEM' "
                    + "ORDER BY id DESC LIMIT ? OFFSET ?";
//            String SQL = "SELECT * FROM ajiswitch_db.tbl_wallet_activities "
//                    + "WHERE walletnumber = ? "
//                    + "AND activity_date_time >= ? AND activity_date_time < ?"
//                    + "ORDER BY id DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, new Object[]{walletnumber, startDate, endDate, limit, offset});
           
            SQL = "SELECT COUNT(id) as totalRecords "
                    + "FROM "+table
                    + "WHERE walletnumber = ? "
                    + "AND activity_date_time >= ? AND activity_date_time < ? "
                    + "AND credit_or_debit = 'cr' AND actor != 'SYSTEM' ";
//            SQL = "SELECT COUNT(id) as totalRecords "
//                    + "FROM ajiswitch_db.tbl_wallet_activities "
//                    + "WHERE walletnumber = ? "
//                    + "AND activity_date_time >= ? AND activity_date_time < ?";
            List<Map<String, Object>> agg = jdbcTemplate.queryForList(SQL, new Object[]{walletnumber, startDate, endDate});
            Map<String, Object> row = agg.get(0);
            Long tRecords = (Long) row.get("totalRecords");
            int totalRecords = tRecords != null ? tRecords.intValue() : 0;
//            meta = "{\"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            String meta = "{\"totalRecords\": " + totalRecords +", \"page\": " + page +", \"limit\": " + limit +"}";
            networkResponse.setCode(200);
            networkResponse.setMeta(meta);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Wallet Activity");
            networkResponse.setData((ArrayList) rows);
            return responseManager.ResponseOk(networkResponse);
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
                            SQL = "INSERT INTO ajiswitch_db.tbl_wallets(walletnumber, walletname, creator, financialinstitutioncode, creationdate) VALUES(?, ?, ?, ?, ?)";
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

    @Override
    @Transactional
    public ResponseEntity ApproveInstitutionFunding(String sessiontoken, int id, String approver) {
        try {
            String SQL;
            int userrole = GetUserRole(approver, sessiontoken);
            if (userrole != PlatformRole.ADMIN && userrole != PlatformRole.APPROVER) {
                return responseManager.ResponseUnathorized();
            }

            SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.financialInstitutionCode, a.wallettype, a.balance, a.lien, a.assignee, a.actionType, a.note, a.date_created, "
                    + "b.name as financialInstitutionName "
                    + "FROM transgateweb_db.tbl_wallets_operations a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.financialInstitutionCode = b.code "
                    + "WHERE a.id = ? AND a.actionType = 'credit' "
                    + "ORDER BY a.id DESC";

            List<WalletModel> pendings = jdbcTemplate.query(SQL, new Object[]{id}, new WalletMapper2());
            if (pendings == null || pendings.isEmpty()) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Funding request not found");
                return responseManager.ResponseNotFound(networkResponse);
            }

            WalletModel pending = pendings.get(0);
            SQL = "DELETE FROM transgateweb_db.tbl_wallets_operations WHERE id = ? AND actionType = 'credit'";
            int removed = jdbcTemplate.update(SQL, new Object[]{id});

            SQL = "UPDATE ajiswitch_db.tbl_wallets SET balance = COALESCE(balance, 0.00) + ? WHERE walletnumber = ?";
            int updated = jdbcTemplate.update(SQL, new Object[]{pending.getBalance(), pending.getWalletnumber()});

            SQL = "INSERT INTO transgateweb_db.tbl_wallet_funding_approvals(operation_id, walletnumber, approved_amount, initiated_by, approved_by, approved_at, action_type, approval_status, rejection_reason) "
                    + "VALUES(?, ?, ?, ?, ?, now(), ?, 'approved', NULL)";
            int logged = jdbcTemplate.update(SQL, new Object[]{id, pending.getWalletnumber(), pending.getBalance(), pending.getCreator(), approver, pending.getActionType()});

            if (removed > 0 && updated > 0 && logged > 0) {
                return responseManager.ResponseAccepted();
            }
            return responseManager.ResponseInternalServerError();
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    @Transactional
    public ResponseEntity RejectInstitutionFunding(String sessiontoken, int id, String approver, String reason) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Rejection reason is required");
                return responseManager.ResponseOk(networkResponse);
            }

            int userrole = GetUserRole(approver, sessiontoken);
            if (userrole != PlatformRole.ADMIN && userrole != PlatformRole.APPROVER) {
                return responseManager.ResponseUnathorized();
            }

            String SQL = "SELECT a.id, a.walletnumber, a.walletname, a.creator, a.financialInstitutionCode, a.wallettype, a.balance, a.lien, a.assignee, a.actionType, a.note, a.date_created, "
                    + "b.name as financialInstitutionName "
                    + "FROM transgateweb_db.tbl_wallets_operations a "
                    + "LEFT JOIN transgateweb_db.tbl_financial_institutions b "
                    + "ON a.financialInstitutionCode = b.code "
                    + "WHERE a.id = ? AND a.actionType = 'credit' "
                    + "ORDER BY a.id DESC";

            List<WalletModel> pendings = jdbcTemplate.query(SQL, new Object[]{id}, new WalletMapper2());
            if (pendings == null || pendings.isEmpty()) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Funding request not found");
                return responseManager.ResponseNotFound(networkResponse);
            }

            WalletModel pending = pendings.get(0);
            SQL = "DELETE FROM transgateweb_db.tbl_wallets_operations WHERE id = ? AND actionType = 'credit'";
            int removed = jdbcTemplate.update(SQL, new Object[]{id});

            SQL = "INSERT INTO transgateweb_db.tbl_wallet_funding_approvals(operation_id, walletnumber, approved_amount, initiated_by, approved_by, approved_at, action_type, approval_status, rejection_reason) "
                    + "VALUES(?, ?, ?, ?, ?, now(), ?, 'rejected', ?)";
            int logged = jdbcTemplate.update(SQL, new Object[]{id, pending.getWalletnumber(), pending.getBalance(), pending.getCreator(), approver, pending.getActionType(), reason.trim()});

            if (removed > 0 && logged > 0) {
                return responseManager.ResponseAccepted();
            }
            return responseManager.ResponseInternalServerError();
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
            response.setFinancialInstitutionCode(rs.getString("financialinstitutioncode") != null ? rs.getString("financialinstitutioncode") : "");
            response.setFinancialInstitutionName(rs.getString("financialInstitutionname") != null ? rs.getString("financialInstitutionname") : "");
            applyWalletType(response, rs);
            BigDecimal _balance = parseDecimal(rs, "balance");
            BigDecimal _lien = parseDecimal(rs, "lien");
            BigDecimal balance = _balance.subtract(_lien);
            response.setBalance(balance);
            response.setLien(_lien);
            response.setDate_created(rs.getString("creationdate"));
            response.setDate_updated(null);
            switch (rs.getInt("is_active")) {
                case 0:
                    response.setStatus("Inactive");
                    break;
                case 1:
                    response.setStatus("Active");
                    break;
                default:
                    response.setStatus("Unknown");
                    break;
            }
            return response;
        }
    }


    class WalletMapperAdmin implements RowMapper<WalletModel> {
        @Override
        public WalletModel mapRow(ResultSet rs, int arg1) throws SQLException {
            WalletModel response = new WalletModel();
            
            response.setId(rs.getInt("id"));
            response.setWalletnumber(rs.getString("walletnumber"));
            response.setWalletname(rs.getString("walletname"));
            response.setCreator(rs.getString("creator"));
            response.setFinancialInstitutionCode(rs.getString("financialinstitutioncode") != null ? rs.getString("financialinstitutioncode") : "");
            response.setFinancialInstitutionName(rs.getString("financialInstitutionname") != null ? rs.getString("financialInstitutionname") : "");
            applyWalletType(response, rs);
            // Belema tbl_wallets has no baseAmount
            response.setBaseAmount(null);
            BigDecimal _balance = parseDecimal(rs, "balance");
            BigDecimal _lien = parseDecimal(rs, "lien");
            BigDecimal balance = _balance.subtract(_lien);
            response.setBalance(balance);
            response.setLien(_lien);
            response.setDate_created(rs.getString("creationdate"));
            response.setDate_updated(null);
            switch (rs.getInt("is_active")) {
                case 0:
                    response.setStatus("Inactive");
                    break;
                case 1:
                    response.setStatus("Active");
                    break;
                default:
                    response.setStatus("Unknown");
                    break;
            }
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
            applyWalletType(response, rs);
            BigDecimal _balance = parseDecimal(rs, "balance");
            BigDecimal _lien = parseDecimal(rs, "lien");
            BigDecimal balance = _balance.subtract(_lien);
            response.setBalance(balance);
            response.setLien(_lien);
            response.setDate_created(rs.getString("date_created"));
            response.setAssignnee(rs.getString("assignee"));
            response.setActionType(rs.getString("actionType"));
            response.setNote(rs.getString("note"));
            response.setDate_updated(null);
            return response;
        }
    }

}
