/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.FinancialInstitutionsInterface;
import com.transgate.api.interfaces.UsersInterface;
import com.transgate.api.models.FinancialInstitutionModel;
import com.transgate.api.models.InstitutionTypesModel;
import com.transgate.api.models.LoginResponse;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.UserModel;
import com.transgate.api.util.Randomizer;
import com.transgate.api.util.ResponseManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
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
public class FinancialInstitutionsService implements FinancialInstitutionsInterface {
    @Autowired
    @Qualifier("jdbcTemplate")
    JdbcTemplate jdbcTemplate;
    
    @Autowired
    private UsersInterface usersInterface;

    ResponseManager responseManager = new ResponseManager();
    
    private Logger logger = Logger.getLogger(FinancialInstitutionsService.class.getName());
    private final AppEnvironmentConfig appConfig;
    public FinancialInstitutionsService(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }
    
    private boolean CheckExistingContact(String email_address, String institution) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_financial_institution_contacts WHERE financial_institution_code = ? AND email_address = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{institution, email_address}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    private boolean CheckInstitutionActionPending(String code, String actionType) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_financial_institutions_pendings WHERE code = ? AND actionType = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{code, actionType}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }

    private int settlementFlag(FinancialInstitutionModel institution) {
        return institution != null && institution.getIssettlementbank() == 1 ? 1 : 0;
    }

    private int instWithWalletFlag(FinancialInstitutionModel institution) {
        return settlementFlag(institution) == 1 ? 0 : 1;
    }

    private String defaultServerIp(String value) {
        String ip = nz(value).trim();
        return ip.isEmpty() ? "localhost" : ip;
    }

    private int defaultTimeout(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private String generateWalletNumber() {
        int totalRows = 1;
        String walletnumber = "";
        try {
            while (totalRows > 0) {
                walletnumber = Randomizer.GenerateWalletNumber();
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM ajiswitch_db.tbl_wallets WHERE walletnumber = ?",
                        new Object[]{walletnumber},
                        Integer.class);
                totalRows = count == null ? 0 : count;
            }
            return walletnumber;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return "";
        }
    }

    private void insertInstitutionExt(FinancialInstitutionModel institution) {
        int withWallet = instWithWalletFlag(institution);
        int extActive = institutionExtActive(institution);
        String SQL = "INSERT into ajiswitch_db.tbl_institution_ext("
                + "is_active, institution_code, neEnvelope, url, neResponseStartTag, neResponseEndTag, "
                + "ftEnvelope, ftResponseStartTag, ftResponseEndTag, tsqEnvelope, urlTSQ, "
                + "tsqResponseStartTag, tsqResponseEndTag, instWithWallet) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(SQL, new Object[]{
            extActive,
            institution.getCode(),
            nz(institution.getNeEnvelope()),
            nz(institution.getUrl()),
            nz(institution.getNeResponseStartTag()),
            nz(institution.getNeResponseEndTag()),
            nz(institution.getFtEnvelope()),
            nz(institution.getFtResponseStartTag()),
            nz(institution.getFtResponseEndTag()),
            nz(institution.getTsqEnvelope()),
            nz(institution.getUrlTSQ()),
            nz(institution.getTsqResponseStartTag()),
            nz(institution.getTsqResponseEndTag()),
            withWallet
        });
    }

    private void upsertInstitutionExt(FinancialInstitutionModel institution) {
        if (institution == null || institution.getCode() == null || institution.getCode().trim().isEmpty()) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ajiswitch_db.tbl_institution_ext WHERE institution_code = ?",
                new Object[]{institution.getCode().trim()},
                Integer.class);
        if (count != null && count > 0) {
            int withWallet = instWithWalletFlag(institution);
            int extActive = institutionExtActive(institution);
            String SQL = "UPDATE ajiswitch_db.tbl_institution_ext SET "
                    + "is_active = ?, neEnvelope = ?, url = ?, neResponseStartTag = ?, neResponseEndTag = ?, "
                    + "ftEnvelope = ?, ftResponseStartTag = ?, ftResponseEndTag = ?, tsqEnvelope = ?, urlTSQ = ?, "
                    + "tsqResponseStartTag = ?, tsqResponseEndTag = ?, instWithWallet = ? "
                    + "WHERE institution_code = ?";
            jdbcTemplate.update(SQL, new Object[]{
                extActive,
                nz(institution.getNeEnvelope()),
                nz(institution.getUrl()),
                nz(institution.getNeResponseStartTag()),
                nz(institution.getNeResponseEndTag()),
                nz(institution.getFtEnvelope()),
                nz(institution.getFtResponseStartTag()),
                nz(institution.getFtResponseEndTag()),
                nz(institution.getTsqEnvelope()),
                nz(institution.getUrlTSQ()),
                nz(institution.getTsqResponseStartTag()),
                nz(institution.getTsqResponseEndTag()),
                withWallet,
                institution.getCode().trim()
            });
        } else {
            insertInstitutionExt(institution);
        }
    }

    private String financialInstitutionSelectSql() {
        return "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, "
                + "n.is_active as status, n.date_created, n.cbn_bank_account, n.isProcessTSQ, n.serverIP, n.neTimeout, n.ftTimeout, "
                + "a.shortName, a.color, a.businessType, a.business_address, a.date_updated, b.name as businessTypeName, "
                + "c.charge_amount, c.vat, "
                + "e.url, e.urlTSQ, e.neEnvelope, e.neResponseStartTag, e.neResponseEndTag, "
                + "e.ftEnvelope, e.ftResponseStartTag, e.ftResponseEndTag, "
                + "e.tsqEnvelope, e.tsqResponseStartTag, e.tsqResponseEndTag, e.is_active as ext_active "
                + "FROM ajiswitch_db.tbl_nodes n "
                + "LEFT JOIN ajiswitch_db.tbl_charges c ON c.institution_code = n.institution_code "
                + "LEFT JOIN tbl_financial_institutions a ON n.institution_code = a.code "
                + "LEFT JOIN tbl_institution_types b ON a.businessType = b.id "
                + "LEFT JOIN ajiswitch_db.tbl_institution_ext e ON e.institution_code = n.institution_code ";
    }

    private void applyInstitutionEdit(FinancialInstitutionModel institution) {
        String code = institution.getCode();
        String name = institution.getName();
        String serverIp = defaultServerIp(institution.getServerIP());
        int neTimeout = defaultTimeout(institution.getNeTimeout(), 5);
        int ftTimeout = defaultTimeout(institution.getFtTimeout(), 10);

        String SQL = "UPDATE tbl_financial_institutions SET name = ?, shortName = ?, color = ?, businessType = ?, business_address = ? WHERE code = ?";
        int editRetVal = jdbcTemplate.update(SQL, new Object[]{
            name,
            institution.getShortName(),
            institution.getColor(),
            institution.getBusinessType(),
            institution.getBusiness_address(),
            code
        });
        if (editRetVal < 1) {
            SQL = "INSERT into tbl_financial_institutions(code, name, shortName, color, businessType, business_address) VALUES(?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(SQL, new Object[]{
                code,
                name,
                institution.getShortName(),
                institution.getColor(),
                institution.getBusinessType(),
                institution.getBusiness_address()
            });
        }

        SQL = "UPDATE ajiswitch_db.tbl_charges SET charge_amount = ?, vat = ? WHERE institution_code = ?";
        jdbcTemplate.update(SQL, new Object[]{
            institution.getCharge_amount(),
            institution.getVat(),
            code
        });

        SQL = "UPDATE ajiswitch_db.tbl_nodes SET institution_name = ?, port_number = ?, publickeylocation = ?, "
                + "cbn_bank_account = ?, isProcessTSQ = ?, serverIP = ?, neTimeout = ?, ftTimeout = ? "
                + "WHERE institution_code = ?";
        jdbcTemplate.update(SQL, new Object[]{
            name,
            institution.getPort_number(),
            institution.getPublickeylocation(),
            institution.getCbn_bank_account(),
            institution.getIsProcessTSQ(),
            serverIp,
            neTimeout,
            ftTimeout,
            code
        });

        upsertInstitutionExt(institution);
    }

    private int institutionExtActive(FinancialInstitutionModel institution) {
        if (institution.getEnableInward() == 1) {
            return 1;
        }
        if (!nz(institution.getUrl()).isEmpty() || !nz(institution.getNeEnvelope()).isEmpty()) {
            return 1;
        }
        return 0;
    }

    private String insertLiveWallet(String creator, String code, String walletname, int wallettype) {
        if (walletname == null || walletname.trim().isEmpty()) {
            return "";
        }
        String walletnumber = generateWalletNumber();
        if (walletnumber == null || walletnumber.isEmpty()) {
            return "";
        }
        jdbcTemplate.update(
                "INSERT INTO ajiswitch_db.tbl_wallets(walletnumber, walletname, creator, financialinstitutioncode, creationdate, balance, lien, wallettype, is_active) VALUES(?, ?, ?, ?, now(), 0.00, 0.00, ?, 1)",
                new Object[]{walletnumber, walletname.trim(), creator, code, wallettype});
        return walletnumber;
    }
    
    private List<FinancialInstitutionModel> GetInstitutionsFromPendings(int id, String actionType) {
        try {
            String SQL;
            SQL = "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, n.date_created, "
                    + "n.cbn_bank_account, n.hashkey, n.isProcessTSQ, n.issettlementbank, n.serverIP, n.neTimeout, n.ftTimeout, n.canFundWallet, "
                    + "a.shortName, a.color, a.businessType, a.actionType, a.note, a.created_by, a.business_address, "
                    + "a.charge_amount, a.vat, a.password, a.url, a.urlTSQ, a.neEnvelope, a.neResponseStartTag, a.neResponseEndTag, "
                    + "a.ftEnvelope, a.ftResponseStartTag, a.ftResponseEndTag, a.tsqEnvelope, a.tsqResponseStartTag, a.tsqResponseEndTag, "
                    + "a.instWithWallet, a.walletname, a.wallettype, b.name as businessTypeName "
                    + "FROM tbl_nodes_pendings n "
                    + "LEFT JOIN tbl_financial_institutions_pendings a "
                    + "ON n.institution_code = a.code "
                    + "LEFT JOIN tbl_institution_types b "
                    + "ON a.businessType = b.id "
                    + "WHERE n.id = ? AND a.actionType = ?";
            try {
                return jdbcTemplate.query(SQL, new Object[]{id, actionType}, new FinancialInstitutionMapper2());
            } catch (DataAccessException missingColumns) {
                SQL = "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, n.date_created, "
                        + "a.shortName, a.color, a.businessType, a.actionType, a.note, a.business_address, b.name as businessTypeName "
                        + "FROM tbl_nodes_pendings n "
                        + "LEFT JOIN tbl_financial_institutions_pendings a "
                        + "ON n.institution_code = a.code "
                        + "LEFT JOIN tbl_institution_types b "
                        + "ON a.businessType = b.id "
                        + "WHERE n.id = ? AND a.actionType = ?";
                return jdbcTemplate.query(SQL, new Object[]{id, actionType}, new FinancialInstitutionMapper2());
            }
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return null;
        }
    }
    
    private int CheckCodeAndName(String name, String code) {
        int totalRows = 0;
        try {
            String SQL = "SELECT COUNT(*) FROM tbl_financial_institutions WHERE name = ? OR code = ?";
            totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{name, code}, int.class);
            return totalRows;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return -1;
        }
    }

    /** True if another live institution already uses this code (trimmed, case-insensitive). */
    private boolean institutionCodeExists(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        String normalized = code.trim();
        try {
            Integer fiCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tbl_financial_institutions WHERE LOWER(TRIM(code)) = LOWER(?)",
                    new Object[]{normalized},
                    Integer.class);
            if (fiCount != null && fiCount > 0) {
                return true;
            }
            Integer nodeCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ajiswitch_db.tbl_nodes WHERE LOWER(TRIM(institution_code)) = LOWER(?)",
                    new Object[]{normalized},
                    Integer.class);
            return nodeCount != null && nodeCount > 0;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return true;
        }
    }

    private boolean institutionNameExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tbl_financial_institutions WHERE LOWER(TRIM(name)) = LOWER(?)",
                    new Object[]{name.trim()},
                    Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return true;
        }
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
    
    private boolean CheckContactPending(String email_address, String actionType) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_financial_institution_contacts_operations WHERE  email_address = ? AND actionType = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{email_address, actionType}, int.class);

            found = totalRows > 0;
            
            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutionsForActions() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, n.date_created, "
                    + "n.cbn_bank_account, n.isProcessTSQ, n.issettlementbank, n.serverIP, n.neTimeout, n.ftTimeout, "
                    + "a.shortName, a.color, a.businessType, a.actionType, a.note, a.created_by, a.business_address, "
                    + "a.charge_amount, a.vat, a.instWithWallet, b.name as businessTypeName "
                    + "FROM tbl_nodes_pendings n "
                    + "LEFT JOIN tbl_financial_institutions_pendings a "
                    + "ON n.institution_code = a.code "
                    + "LEFT JOIN tbl_institution_types b "
                    + "ON a.businessType = b.id "
                    + "ORDER BY n.id DESC";
            List<FinancialInstitutionModel> financialInstitutionModel;
            try {
                financialInstitutionModel = jdbcTemplate.query(SQL, new FinancialInstitutionMapper2());
            } catch (DataAccessException missingColumns) {
                SQL = "SELECT n.id, n.institution_name as name, n.institution_code as code, n.port_number, n.publickeylocation, n.date_created, "
                        + "a.shortName, a.color, a.businessType, a.actionType, a.note, a.business_address, b.name as businessTypeName "
                        + "FROM tbl_nodes_pendings n "
                        + "LEFT JOIN tbl_financial_institutions_pendings a "
                        + "ON n.institution_code = a.code "
                        + "LEFT JOIN tbl_institution_types b "
                        + "ON a.businessType = b.id "
                        + "ORDER BY n.id DESC";
                financialInstitutionModel = jdbcTemplate.query(SQL, new FinancialInstitutionMapper2());
            }
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institutions");
            networkResponse.setData((ArrayList) financialInstitutionModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutionByCode(String sessiontoken, String code) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = financialInstitutionSelectSql() + "WHERE n.institution_code = ?";
            
            List<FinancialInstitutionModel> financialInstitutionModel = jdbcTemplate.query(SQL, new Object[]{code}, new FinancialInstitutionMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Get financial institution " + code + " detail");
            networkResponse.setData((ArrayList) financialInstitutionModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutions() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = financialInstitutionSelectSql() + "ORDER BY n.id DESC";
            List<FinancialInstitutionModel> financialInstitutionModel = jdbcTemplate.query(SQL, new FinancialInstitutionMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institutions");
            networkResponse.setData((ArrayList) financialInstitutionModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Create(String sessiontoken, FinancialInstitutionModel institution) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            int retval;
            String code = institution.getCode();
            String name = institution.getName();
            String creator = institution.getCreated_by();
            if (code == null || code.trim().isEmpty()) {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Institution code is required");
                return responseManager.ResponseOk(networkResponse);
            }
            code = code.trim();
            institution.setCode(code);
            if (!code.matches("\\d{1,6}")) {
                return responseManager.ResponseBadRequest("Institution code must be 1 to 6 digits");
            }
            if (institutionCodeExists(code)) {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Institution code already exists");
                return responseManager.ResponseOk(networkResponse);
            }
            if (institutionNameExists(name)) {
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Financial Institution with name already exist");
                return responseManager.ResponseOk(networkResponse);
            }
            int isSettlement = settlementFlag(institution);
            int withWallet = instWithWalletFlag(institution);
            int canFund = withWallet;
            String cbnAccount = isSettlement == 1 ? nz(institution.getCbn_bank_account()) : "";
            String serverIp = defaultServerIp(institution.getServerIP());
            int neTimeout = defaultTimeout(institution.getNeTimeout(), 5);
            int ftTimeout = defaultTimeout(institution.getFtTimeout(), 10);
            int userrole = GetUserRole(creator, sessiontoken);
            switch (userrole) {
                    case 1:
                        SQL = "INSERT into ajiswitch_db.tbl_charges(institution_code, charge_amount, vat, date_created, is_active) VALUES(?, ?, ?, now(), '1')";
                        jdbcTemplate.update(SQL, new Object[]{code, institution.getCharge_amount(), institution.getVat()});
                        SQL = "INSERT into ajiswitch_db.tbl_token_users(institution_name, password) VALUES(?, TO_BASE64(AES_ENCRYPT(?, ?)))";
                        jdbcTemplate.update(SQL, new Object[]{code, institution.getPassword(), appConfig.getSqlEncodeString()});
                        SQL = "INSERT into tbl_financial_institutions(code, name, shortName, color, businessType, business_address) VALUES(?, ?, ?, ?, ?, ?)";
                        jdbcTemplate.update(SQL, new Object[]{code, name, institution.getShortName(), institution.getColor(), institution.getBusinessType(), institution.getBusiness_address()});
                        SQL = "INSERT into ajiswitch_db.tbl_nodes(port_number, is_active, publickeylocation, institution_code, institution_name, date_created, cbn_bank_account, hashkey, isProcessTSQ, issettlementbank, serverIP, neTimeout, ftTimeout, canFundWallet, walletnumber) VALUES(?, 1, ?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        retval = jdbcTemplate.update(SQL, new Object[]{institution.getPort_number(), institution.getPublickeylocation(), code, name, cbnAccount, institution.getHashKey(), institution.getIsProcessTSQ(), isSettlement, serverIp, neTimeout, ftTimeout, canFund, ""});
                        insertInstitutionExt(institution);
                        if (isSettlement == 0) {
                            String walletnumber = insertLiveWallet(creator, code, institution.getWalletname(), institution.getWallettype());
                            if (walletnumber != null && !walletnumber.isEmpty()) {
                                jdbcTemplate.update("UPDATE ajiswitch_db.tbl_nodes SET walletnumber = ? WHERE institution_code = ?", new Object[]{walletnumber, code});
                            }
                        }
                        if (retval > 0)
                            return responseManager.ResponseAccepted();
                        else
                            return responseManager.ResponseInternalServerError();
                    case 2:
                        boolean checkPendingAction = CheckInstitutionActionPending(code, "create");
                        if (checkPendingAction) {
                            networkResponse = new NetworkResponse();
                            networkResponse.setCode(200);
                            networkResponse.setStatus("failed");
                            networkResponse.setMessage("Institution already pending create");
                            return responseManager.ResponseOk(networkResponse);
                        }
                        SQL = "INSERT INTO tbl_financial_institutions_pendings(code, name, shortName, color, businessType, actionType, note, created_by, business_address, charge_amount, vat, password, url, urlTSQ, neEnvelope, neResponseStartTag, neResponseEndTag, ftEnvelope, ftResponseStartTag, ftResponseEndTag, tsqEnvelope, tsqResponseStartTag, tsqResponseEndTag, instWithWallet, walletname, wallettype) VALUES(?, ?, ?, ?, ?, 'create', 'Create financial institution', ?, ?, ?, ?, TO_BASE64(AES_ENCRYPT(?, ?)), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        jdbcTemplate.update(SQL, new Object[]{code, name, institution.getShortName(), institution.getColor(), institution.getBusinessType(), creator, institution.getBusiness_address(), institution.getCharge_amount(), institution.getVat(), institution.getPassword(), appConfig.getSqlEncodeString(), nz(institution.getUrl()), nz(institution.getUrlTSQ()), nz(institution.getNeEnvelope()), nz(institution.getNeResponseStartTag()), nz(institution.getNeResponseEndTag()), nz(institution.getFtEnvelope()), nz(institution.getFtResponseStartTag()), nz(institution.getFtResponseEndTag()), nz(institution.getTsqEnvelope()), nz(institution.getTsqResponseStartTag()), nz(institution.getTsqResponseEndTag()), withWallet, nz(institution.getWalletname()), institution.getWallettype()});
                        SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created, cbn_bank_account, hashkey, isProcessTSQ, issettlementbank, serverIP, neTimeout, ftTimeout, canFundWallet) VALUES(?, 1, ?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?)";
                        retval = jdbcTemplate.update(SQL, new Object[]{institution.getPort_number(), institution.getPublickeylocation(), code, name, cbnAccount, institution.getHashKey(), institution.getIsProcessTSQ(), isSettlement, serverIp, neTimeout, ftTimeout, canFund});
                        if (retval > 0) 
                            return responseManager.ResponseAccepted();
                        else 
                            return responseManager.ResponseInternalServerError();
                    default:
                        return responseManager.ResponseUnathorized();
                }
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("failed");
            networkResponse.setMessage("Institution code already exists");
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutionsTypes() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.name, a.date_created "
                    + "FROM tbl_institution_types a "
                    + "ORDER BY a.id DESC";
            List<InstitutionTypesModel> institutionTypesModel = jdbcTemplate.query(SQL, new InstitutionTypesMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All Financial Institution Types");
            networkResponse.setData((ArrayList) institutionTypesModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetFinancialInstitutionsTypeById(String sessiontoken, int id) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.name, a.date_created "
                    + "FROM tbl_institution_types a "
                    + "WHERE a.id = ?";
            List<InstitutionTypesModel> institutionTypesModel = jdbcTemplate.query(SQL, new Object[]{id}, new InstitutionTypesMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Financial Institution Type");
            networkResponse.setData((ArrayList) institutionTypesModel);
            return responseManager.ResponseOk(networkResponse);
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity Delete(String sessiontoken, String code, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE ajiswitch_db.tbl_nodes SET is_active = -1 WHERE institution_code = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{code});
                    if (retVal > 0){
                        ActivateDeactivateContacts("deactivate", code);
                        return responseManager.ResponseAccepted();
                    }
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckInstitutionActionPending(code, "deactivate");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Institution already pending deactivation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetFinancialInstitutionByCode(sessiontoken, code);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    FinancialInstitutionModel institution = networkResponse != null ? (FinancialInstitutionModel) networkResponse.getData().get(0) : new FinancialInstitutionModel();
                    SQL = "INSERT INTO tbl_financial_institutions_pendings(name, shortName, color, code, businessType, actionType, note, created_by, business_address) VALUES(?, ?, ?, ?, ?, 'deactivate', 'Deactivate financial institution', ?, ?)";
                    jdbcTemplate.update(SQL, new Object[]{institution.getName(), institution.getShortName(), institution.getColor(), institution.getCode(), institution.getBusinessType(), username, institution.getBusiness_address()});
                    SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created) VALUES(?, ?, ?, ?, ?, now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{institution.getPort_number(), institution.getStatus(), institution.getPublickeylocation(), code, institution.getName()});
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
    public ResponseEntity Activate(String sessiontoken, String code, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE ajiswitch_db.tbl_nodes SET is_active = 1 WHERE institution_code = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{code});
                    if (retVal > 0){
                        ActivateDeactivateContacts("activated", code);
                        return responseManager.ResponseAccepted();
                    }
                    else
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean checkPendingAction = CheckInstitutionActionPending(code, "activate");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Institution already pending activation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetFinancialInstitutionByCode(sessiontoken, code);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    FinancialInstitutionModel institution = networkResponse != null ? (FinancialInstitutionModel) networkResponse.getData().get(0) : new FinancialInstitutionModel();
                    SQL = "INSERT INTO tbl_financial_institutions_pendings(name, shortName, color, code, businessType, actionType, note, created_by, business_address) VALUES(?, ?, ?, ?, ?, 'activate', 'Activate financial institution', ?, ?)";
                    jdbcTemplate.update(SQL, new Object[]{institution.getName(), institution.getShortName(), institution.getColor(), institution.getCode(), institution.getBusinessType(), username, institution.getBusiness_address()});
                    SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created) VALUES(?, ?, ?, ?, ?, now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{institution.getPort_number(), institution.getStatus(), institution.getPublickeylocation(), code, institution.getName()});
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
    public ResponseEntity Edit(String sessiontoken, FinancialInstitutionModel institution) {
        try {
            if (institution == null || institution.getCode() == null || institution.getCode().trim().isEmpty()) {
                return responseManager.ResponseBadRequest();
            }
            String code = institution.getCode().trim();
            institution.setCode(code);
            String editor = institution.getCreated_by();
            String SQL;
            int userrole = GetUserRole(editor, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    applyInstitutionEdit(institution);
                    return responseManager.ResponseAccepted();
                case 2:
                    boolean checkPendingAction = CheckInstitutionActionPending(code, "edit");
                    if (checkPendingAction) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Institution already pending edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    ResponseEntity responseEntity = GetFinancialInstitutionByCode(sessiontoken, code);
                    NetworkResponse networkResponse = (NetworkResponse) responseEntity.getBody();
                    FinancialInstitutionModel existing = networkResponse != null && networkResponse.getData() != null && !networkResponse.getData().isEmpty()
                            ? (FinancialInstitutionModel) networkResponse.getData().get(0)
                            : new FinancialInstitutionModel();
                    responseEntity = GetFinancialInstitutionsTypeById(sessiontoken, institution.getBusinessType());
                    networkResponse = (NetworkResponse) responseEntity.getBody();
                    InstitutionTypesModel typeModel = networkResponse != null && networkResponse.getData() != null && !networkResponse.getData().isEmpty()
                            ? (InstitutionTypesModel) networkResponse.getData().get(0)
                            : new InstitutionTypesModel();
                    String name = institution.getName();
                    String shortName = institution.getShortName();
                    String color = institution.getColor();
                    String business_address = institution.getBusiness_address();
                    int businessType = institution.getBusinessType();
                    SQL = "INSERT INTO tbl_financial_institutions_pendings(name, shortName, color, code, businessType, actionType, note, created_by, business_address, charge_amount, vat, url, urlTSQ, neEnvelope, neResponseStartTag, neResponseEndTag, ftEnvelope, ftResponseStartTag, ftResponseEndTag, tsqEnvelope, tsqResponseStartTag, tsqResponseEndTag, instWithWallet) VALUES(?, ?, ?, ?, ?, 'edit', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    String note = existing.getName().equals(name) ? "" : "Change institution name from " + existing.getName() + " to " + name;
                    note = existing.getColor().equals(color) ? note : !note.equals("") ? note + ", change color from " + existing.getColor() + " to " + color : "Change color from " + existing.getColor() + " to " + color;
                    note = existing.getShortName().equals(shortName) ? note : !note.equals("") ? note + ", change short name from " + existing.getShortName() + " to " + shortName : "Change short name from " + existing.getShortName() + " to " + shortName;
                    note = existing.getBusiness_address().equals(business_address) ? note : !note.equals("") ? note + ", change address from " + existing.getBusiness_address() + " to " + business_address : "Change address from " + existing.getBusiness_address() + " to " + business_address;
                    note = existing.getBusinessType() == businessType ? note : !note.equals("") ? note + ", change type from " + existing.getBusinessTypeName() + " to " + typeModel.getName() : "Change type from " + existing.getBusinessTypeName() + " to " + typeModel.getName();
                    int withWallet = instWithWalletFlag(institution);
                    try {
                        jdbcTemplate.update(SQL, new Object[]{
                            name,
                            shortName,
                            color,
                            code,
                            businessType,
                            note,
                            editor,
                            business_address,
                            institution.getCharge_amount(),
                            institution.getVat(),
                            nz(institution.getUrl()),
                            nz(institution.getUrlTSQ()),
                            nz(institution.getNeEnvelope()),
                            nz(institution.getNeResponseStartTag()),
                            nz(institution.getNeResponseEndTag()),
                            nz(institution.getFtEnvelope()),
                            nz(institution.getFtResponseStartTag()),
                            nz(institution.getFtResponseEndTag()),
                            nz(institution.getTsqEnvelope()),
                            nz(institution.getTsqResponseStartTag()),
                            nz(institution.getTsqResponseEndTag()),
                            withWallet
                        });
                    } catch (DataAccessException missingColumns) {
                        SQL = "INSERT INTO tbl_financial_institutions_pendings(name, shortName, color, code, businessType, actionType, note, created_by, business_address, charge_amount, vat) VALUES(?, ?, ?, ?, ?, 'edit', ?, ?, ?, ?, ?)";
                        jdbcTemplate.update(SQL, new Object[]{
                            name,
                            shortName,
                            color,
                            code,
                            businessType,
                            note,
                            editor,
                            business_address,
                            institution.getCharge_amount(),
                            institution.getVat()
                        });
                    }
                    String serverIp = defaultServerIp(institution.getServerIP());
                    int neTimeout = defaultTimeout(institution.getNeTimeout(), 5);
                    int ftTimeout = defaultTimeout(institution.getFtTimeout(), 10);
                    SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created, cbn_bank_account, isProcessTSQ, serverIP, neTimeout, ftTimeout) VALUES(?, ?, ?, ?, ?, now(), ?, ?, ?, ?, ?)";
                    try {
                        retVal = jdbcTemplate.update(SQL, new Object[]{
                            institution.getPort_number(),
                            existing.getStatus() != null && existing.getStatus().equals("-1") ? -1 : 1,
                            institution.getPublickeylocation(),
                            code,
                            name,
                            institution.getCbn_bank_account(),
                            institution.getIsProcessTSQ(),
                            serverIp,
                            neTimeout,
                            ftTimeout
                        });
                    } catch (DataAccessException missingColumns) {
                        SQL = "INSERT into tbl_nodes_pendings(port_number, is_active, publickeylocation, institution_code, institution_name, date_created) VALUES(?, ?, ?, ?, ?, now())";
                        retVal = jdbcTemplate.update(SQL, new Object[]{
                            institution.getPort_number(),
                            existing.getStatus() != null && existing.getStatus().equals("-1") ? -1 : 1,
                            institution.getPublickeylocation(),
                            code,
                            name
                        });
                    }
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
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
    
    private void ActivateDeactivateContacts(String action, String institution) {
        int isEnabled = action.equals("deactivate") ? -1 : 1;
        String SQL = "UPDATE sparkpayweb_db.tbl_users a "
                + "LEFT JOIN tbl_financial_institution_contacts b "
                + "ON a.username = b.email_address "
                + "SET a.enabled = ? "
                + "WHERE b.financial_institution_code = ?";
        jdbcTemplate.update(SQL, new Object[]{isEnabled, institution});
    }
    
    @Override
    public ResponseEntity FinancialInstitutionApprovals(String sessiontoken, int id, String actionType, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            int retVal2;
            if (userrole == 1 || userrole == 3) {
                List<FinancialInstitutionModel> institutions = GetInstitutionsFromPendings(id, actionType);
                if (institutions.size() == 1) {
                    switch (actionType) {
                        case "deactivate":
                        case "activate":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = ?";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id, actionType});
                            SQL = "UPDATE ajiswitch_db.tbl_nodes SET is_active = ? WHERE institution_code = ?";
                            int acInt = actionType.equals("deactivate") ? -1 : 1;
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{acInt, institutions.get(0).getCode()});
                            if (retVal > 0 && retVal2 > 0){
                                ActivateDeactivateContacts(actionType, institutions.get(0).getCode());
                                return responseManager.ResponseAccepted();
                            }
                            else
                                return responseManager.ResponseInternalServerError();
                        case "edit":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = 'edit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            applyInstitutionEdit(institutions.get(0));
                            retVal2 = 1;
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "create":
                            FinancialInstitutionModel pendingCreate = institutions.get(0);
                            int isSettlement = settlementFlag(pendingCreate);
                            int canFund = instWithWalletFlag(pendingCreate);
                            String cbnAccount = isSettlement == 1 ? nz(pendingCreate.getCbn_bank_account()) : "";
                            String serverIp = defaultServerIp(pendingCreate.getServerIP());
                            int neTimeout = defaultTimeout(pendingCreate.getNeTimeout(), 5);
                            int ftTimeout = defaultTimeout(pendingCreate.getFtTimeout(), 10);
                            String walletnumber = "";
                            SQL = "INSERT into ajiswitch_db.tbl_charges(institution_code, charge_amount, vat, date_created, is_active) VALUES(?, ?, ?, now(), '1')";
                            jdbcTemplate.update(SQL, new Object[]{pendingCreate.getCode(), pendingCreate.getCharge_amount(), pendingCreate.getVat()});
                            try {
                                SQL = "INSERT into ajiswitch_db.tbl_token_users(institution_name, password) "
                                        + "SELECT b.code, b.password FROM tbl_nodes_pendings a "
                                        + "INNER JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code "
                                        + "WHERE a.id = ? AND b.actionType = 'create' AND b.password IS NOT NULL";
                                jdbcTemplate.update(SQL, new Object[]{id});
                            } catch (DataAccessException tokenUsersEx) {
                                System.out.println("error>>>>" + tokenUsersEx.getMessage());
                            }
                            SQL = "INSERT into tbl_financial_institutions(name, shortName, color, code, businessType, business_address) VALUES(?, ?, ?, ?, ?, ?)";
                            jdbcTemplate.update(SQL, new Object[]{pendingCreate.getName(), pendingCreate.getShortName(), pendingCreate.getColor(), pendingCreate.getCode(), pendingCreate.getBusinessType(), pendingCreate.getBusiness_address()});
                            if (isSettlement == 0) {
                                walletnumber = insertLiveWallet(username, pendingCreate.getCode(), pendingCreate.getWalletname(), pendingCreate.getWallettype());
                            }
                            SQL = "INSERT into ajiswitch_db.tbl_nodes(port_number, is_active, publickeylocation, institution_code, institution_name, date_created, cbn_bank_account, hashkey, isProcessTSQ, issettlementbank, serverIP, neTimeout, ftTimeout, canFundWallet, walletnumber) VALUES(?, 1, ?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{pendingCreate.getPort_number(), pendingCreate.getPublickeylocation(), pendingCreate.getCode(), pendingCreate.getName(), cbnAccount, pendingCreate.getHashKey(), pendingCreate.getIsProcessTSQ(), isSettlement, serverIp, neTimeout, ftTimeout, canFund, walletnumber});
                            insertInstitutionExt(pendingCreate);
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = 'create'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
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
    public ResponseEntity FinancialInstitutionReject(String sessiontoken, int id, String actionType, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            int retVal2;
            if (userrole == 1 || userrole == 3) {
                List<FinancialInstitutionModel> institutions = GetInstitutionsFromPendings(id, actionType);
                if (institutions.size() == 1) {
                    switch (actionType) {
                        case "deactivate":
                        case "activate":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = ?";
//                            SQL = "DELETE FROM tbl_financial_institutions_pendings WHERE id = ? AND actionType = ?";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id, actionType});
//                            SQL = "UPDATE tbl_financial_institutions SET status = ? WHERE code = ?";
//                            int acInt = actionType.equals("deactivate") ? -1 : 1;
//                            retVal2 = jdbcTemplate.update(SQL, new Object[]{acInt, institutions.get(0).getCode()});
                            if (retVal > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "edit":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = 'edit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
//                            SQL = "UPDATE tbl_financial_institutions SET name = ?, businessType = ?, business_address = ? WHERE code = ?";
//                            retVal2 = jdbcTemplate.update(SQL, new Object[]{institutions.get(0).getName(), institutions.get(0).getBusinessType(), institutions.get(0).getBusiness_address(), institutions.get(0).getCode()});
                            if (retVal > 0)
                                return responseManager.ResponseAccepted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "create":
                            SQL = "DELETE a, b FROM tbl_nodes_pendings a LEFT JOIN tbl_financial_institutions_pendings b ON a.institution_code = b.code WHERE a.id = ? AND b.actionType = 'create'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
//                            SQL = "INSERT into tbl_financial_institutions(name, code, businessType, date_created, business_address) VALUES(?, ?, ?, now(), ?)";
//                            retVal2 = jdbcTemplate.update(SQL, new Object[]{institutions.get(0).getName(), institutions.get(0).getCode(), institutions.get(0).getBusinessType(), institutions.get(0).getBusiness_address()});
                            if (retVal > 0)
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
    public ResponseEntity GetAllContacts(String sessiontoken) {
        return GetAllContacts(sessiontoken, "");
    }
    
    @Override
    public ResponseEntity GetAllContacts(String sessiontoken, String code) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<UserModel> contacts;
            if (!code.equals("")) {
                SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "WHERE a.financial_institution_code = ?";
                contacts = jdbcTemplate.query(SQL, new Object[]{code}, new ContactMapper());
            }
            else {
                SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "ORDER BY a.id DESC";
                contacts = jdbcTemplate.query(SQL, new ContactMapper());
            }
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(!code.equals("") ? "Institution contacts" : "All institution contacts");
            networkResponse.setData((ArrayList) contacts);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetAllContactsForActions(String sessiontoken) {
        return GetAllContactsForActions(sessiontoken, "");
    }
    
    @Override
    public ResponseEntity GetAllContactsForActions(String sessiontoken, String code) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            List<UserModel> contacts;
            if (!code.equals("")) {
                SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.actionType, a.note, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts_operations a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "WHERE a.financial_institution_code = ?";
                contacts = jdbcTemplate.query(SQL, new Object[]{code}, new ContactMapper2());
            }
            else {
                SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.actionType, a.note, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts_operations a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "ORDER BY a.id DESC";
                contacts = jdbcTemplate.query(SQL, new ContactMapper2());
            }
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(!code.equals("") ? "Institution contacts" : "All institution contacts");
            networkResponse.setData((ArrayList) contacts);
            
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    public ResponseEntity GetContactByEmail(String sessiontoken, String email) {
        UserModel response = new UserModel();
        try {
            String SQL;
            SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "WHERE a.email_address = ?";
            List<UserModel> details = jdbcTemplate.query(SQL, new Object[]{email}, new ContactMapper());
            if (details.size() > 0) {
                response.setCode(200);
                response.setStatus("success");
                response.setMessage("Contact detail");
                response.setId(details.get(0).getId());
                response.setInstitution(details.get(0).getInstitution());
                response.setFirstname(details.get(0).getFirstname());
                response.setSurname(details.get(0).getSurname());
                response.setPhone_number(details.get(0).getPhone_number());
                response.setEmail_address(details.get(0).getEmail_address());
                response.setDate_created(details.get(0).getDate_created());
                response.setInstitutionName(details.get(0).getInstitutionName());
                return responseManager.ResponseOk(response);
            }
            else {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Contact not found");
                return responseManager.ResponseNotFound(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid id");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity GetContactById(String sessiontoken, int id) {
        UserModel response = new UserModel();
        try {
            String SQL;
            SQL = "SELECT a.id, a.financial_institution_code, a.firstname, a.surname, a.phone_number, a.email_address, a.date_created, "
                    + "b.name as institutionname "
                    + "FROM tbl_financial_institution_contacts a "
                    + "LEFT JOIN tbl_financial_institutions b "
                    + "ON a.financial_institution_code = b.code "
                    + "WHERE a.id = ?";
            List<UserModel> details = jdbcTemplate.query(SQL, new Object[]{id}, new ContactMapper());
            if (details.size() > 0) {
                response.setCode(200);
                response.setStatus("success");
                response.setMessage("Contact detail");
                response.setId(details.get(0).getId());
                response.setInstitution(details.get(0).getInstitution());
                response.setFirstname(details.get(0).getFirstname());
                response.setSurname(details.get(0).getSurname());
                response.setPhone_number(details.get(0).getPhone_number());
                response.setEmail_address(details.get(0).getEmail_address());
                response.setDate_created(details.get(0).getDate_created());
                response.setInstitutionName(details.get(0).getInstitutionName());
                return responseManager.ResponseOk(response);
            }
            else {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Contact not found");
                return responseManager.ResponseNotFound(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid id");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    private String lookupInstitutionName(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        try {
            String name = jdbcTemplate.queryForObject(
                    "SELECT institution_name FROM tbl_nodes WHERE institution_code = ? LIMIT 1",
                    String.class,
                    code);
            return name != null && !name.isEmpty() ? name : code;
        } catch (DataAccessException ex) {
            logger.info("lookupInstitutionName: " + ex.getMessage());
            return code;
        }
    }

    @Override
public ResponseEntity CreateContact(String sessiontoken, String creator, String institution, 
        String firstname, String surname, String phone_number, String email_address, String security) {
    logger.info("CreateContact invoked with: sessiontoken=" + sessiontoken
            + ", creator=" + creator
            + ", institution=" + institution
            + ", firstname=" + firstname
            + ", surname=" + surname
            + ", phone_number=" + phone_number
            + ", email_address=" + email_address);
    try {
        // Check if the contact already exists for the institution.
        boolean contactExist = CheckExistingContact(email_address, institution);
        if (contactExist) {
            logger.info("Contact already exists with email: " + email_address + " for institution: " + institution);
            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("failed");
            networkResponse.setMessage("Email address already exist");
            return responseManager.ResponseOk(networkResponse);
        }

        if (institution == null || institution.isEmpty()) {
            NetworkResponse missingFi = new NetworkResponse();
            missingFi.setCode(200);
            missingFi.setStatus("failed");
            missingFi.setMessage("Financial institution code is required");
            return responseManager.ResponseOk(missingFi);
        }

        int userrole = GetUserRole(creator, sessiontoken);
        logger.info("Retrieved userrole: " + userrole + " for creator: " + creator);

        // Blank security → CreateOther.resolveAssignedPassword generates a temporary password
        // and UserCreatedEvent triggers the welcome email.
        String passwordOrBlank = security == null ? "" : security;
        String institutionName = lookupInstitutionName(institution);
        ResponseEntity createLoginResponse = usersInterface.CreateOther(
                sessiontoken, creator, email_address, firstname, surname, phone_number, email_address, 4, passwordOrBlank,
                institution, institutionName);
        logger.info("Received response from usersInterface.CreateOther");
        Object createBody = createLoginResponse.getBody();
        boolean userCreateOk = false;
        if (createBody instanceof LoginResponse) {
            userCreateOk = "success".equals(((LoginResponse) createBody).getStatus());
        } else if (createBody instanceof NetworkResponse) {
            userCreateOk = "success".equals(((NetworkResponse) createBody).getStatus());
        }
        // Operator pending create returns HTTP 202 Accepted (no LoginResponse body).
        if (!userCreateOk && createLoginResponse.getStatusCode() != null
                && createLoginResponse.getStatusCode().value() == 202) {
            userCreateOk = true;
        }
        if (userCreateOk) {
            jdbcTemplate.update(
                    "UPDATE tbl_user_details_operations SET note = 'Create contact account' WHERE email_address = ? AND actionType = 'create'",
                    email_address);
            logger.info("Stored create-account note on pending contact for " + email_address);
            logger.info("User create accepted. Processing contact creation for userrole: " + userrole);
            String SQL;
            int retval;
            switch (userrole) {
                case 1:
                    SQL = "INSERT into tbl_financial_institution_contacts(" +
                          "financial_institution_code, firstname, surname, phone_number, email_address, date_created) " +
                          "VALUES(?, ?, ?, ?, ?, now())";
                    logger.info("Executing SQL for userrole 1: " + SQL);
                    retval = jdbcTemplate.update(SQL, new Object[]{institution, firstname, surname, phone_number, email_address});
                    logger.info("Insert into tbl_financial_institution_contacts returned: " + retval);
                    if (retval > 0) {
                        logger.info("Successfully inserted contact for email: " + email_address);
                        return responseManager.ResponseAccepted();
                    } else {
                        logger.info("Insert into tbl_financial_institution_contacts failed for email: " + email_address);
                        return responseManager.ResponseBadRequest();
                    }
                case 2:
                case 3:
                    if (CheckContactPending(email_address, "create")) {
                        logger.info("Contact pending creation already exists for email: " + email_address);
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Contact with email - " + email_address + " is already pending for creation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "INSERT INTO tbl_financial_institution_contacts_operations(" +
                          "financial_institution_code, firstname, surname, phone_number, email_address, actionType, note, date_created) " +
                          "VALUES(?, ?, ?, ?, ?, 'create', 'Create contact', now())";
                    logger.info("Executing SQL for pending contact (userrole " + userrole + "): " + SQL);
                    retval = jdbcTemplate.update(SQL, new Object[]{institution, firstname, surname, phone_number, email_address});
                    logger.info("Insert into tbl_financial_institution_contacts_operations returned: " + retval);
                    if (retval > 0) {
                        logger.info("Pending contact recorded for email: " + email_address);
                        return responseManager.ResponseAccepted();
                    } else {
                        logger.info("Insert into tbl_financial_institution_contacts_operations failed for email: " + email_address);
                        return responseManager.ResponseInternalServerError();
                    }
                default:
                    logger.info("Unauthorized user role: " + userrole + " for creator: " + creator);
                    return responseManager.ResponseUnathorized();
            }
        } else {
            logger.info("User create failed; returning nested response. Status: "
                    + (createLoginResponse.getStatusCode() != null ? createLoginResponse.getStatusCode() : "unknown"));
            return createLoginResponse;
        }
    } catch (DataAccessException ex) {
        logger.info("DataAccessException in CreateContact: " + ex.getMessage());
        return responseManager.ResponseInternalServerError();
    } catch (Exception ex) {
        logger.info("Exception in CreateContact: " + ex.getMessage());
        return responseManager.ResponseInternalServerError();
    }
}

    
    
    @Override
    public ResponseEntity DeleteContact(String sessiontoken, String email, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "DELETE FROM sparkpayweb_db.tbl_users WHERE username = ?";
                    jdbcTemplate.update(SQL, new Object[]{email});
                    SQL = "DELETE FROM tbl_user_details WHERE username = ? || email_address = ?";
                    jdbcTemplate.update(SQL, new Object[]{email, email});
                    SQL = "DELETE FROM tbl_financial_institution_contacts WHERE email_address = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{email});
                    if (retVal > 0)
                        return responseManager.ResponseDeleted();
                    else
                        return responseManager.ResponseInternalServerError();
//                case 2:
//                    ResponseEntity responseEntity = GetContactByEmail(sessiontoken, email);
//                    UserModel contact = responseEntity != null ? (UserModel) responseEntity.getBody() : new UserModel();
//                    if (contact.getCode() == 404) {
//                        NetworkResponse networkResponse = new NetworkResponse();
//                        networkResponse.setCode(contact.getCode());
//                        networkResponse.setStatus(contact.getStatus());
//                        networkResponse.setMessage(contact.getMessage());
//                        return responseManager.ResponseNotFound(networkResponse);
//                    }
//                    boolean userPending = CheckContactPending(contact.getEmail_address(), "delete");
//                    if (userPending) {
//                        NetworkResponse networkResponse = new NetworkResponse();
//                        networkResponse.setCode(200);
//                        networkResponse.setStatus("failed");
//                        networkResponse.setMessage("Contact already pending for delete");
//                        return responseManager.ResponseOk(networkResponse);
//                    }
//                    SQL = "INSERT INTO tbl_financial_institution_contacts_operations(financial_institution_code, firstname, surname, phone_number, email_address, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, 'delete', 'Delete contact', now())";
//                    retVal = jdbcTemplate.update(SQL, new Object[]{contact.getInstitution(), contact.getFirstname(), contact.getSurname(), contact.getPhone_number(), contact.getEmail_address()});
//                    if (retVal > 0) 
//                        return responseManager.ResponseDeleted();
//                    else
//                        return responseManager.ResponseInternalServerError();
                default:
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }
    
    @Override
    public ResponseEntity EditContact(String sessiontoken, int id, String firstname, String surname, String phone_number, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE tbl_financial_institution_contacts SET firstname = ?, surname = ?, phone_number = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{firstname, surname, phone_number, id});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseInternalServerError();
                case 2:
                    ResponseEntity responseEntity = GetContactById(sessiontoken, id);
                    UserModel contact = responseEntity != null ? (UserModel) responseEntity.getBody() : new UserModel();
                    if (contact.getCode() == 404) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(contact.getCode());
                        networkResponse.setStatus(contact.getStatus());
                        networkResponse.setMessage(contact.getMessage());
                        return responseManager.ResponseNotFound(networkResponse);
                    }
                    boolean userPending = CheckContactPending(contact.getEmail_address(), "edit");
                    if (userPending) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Contact already pending for edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    String note = contact.getFirstname().equals(firstname) || firstname == null ? "" : "Change firstname from " + contact.getFirstname() + " to " + firstname;
                    note = contact.getSurname().equals(surname) || surname == null ? note : !note.equals("") ? note + ", change surname from " + contact.getSurname() + " to " + surname : "Change surname from " + contact.getSurname() + " to " + surname;
                    note = contact.getPhone_number().equals(phone_number) || phone_number == null ? note : !note.equals("") ? note + ", change phone number from " + contact.getPhone_number() + " to " + phone_number : "Change phone number from " + contact.getPhone_number() + " to " + phone_number;
                    SQL = "INSERT INTO tbl_financial_institution_contacts_operations(financial_institution_code, firstname, surname, phone_number, email_address, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, 'edit', ?, now())";
                    jdbcTemplate.update(SQL, new Object[]{contact.getInstitution(), contact.getFirstname(), contact.getSurname(), contact.getPhone_number(), contact.getEmail_address(), note});
                    SQL = "INSERT INTO tbl_user_details_operations(username, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, 'edit', 'Edit contact account', now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{contact.getEmail_address(), firstname, surname, phone_number, contact.getEmail_address(), 4});
                    if (retVal > 0) 
                        return responseManager.ResponseAccepted();
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
    
    class ContactMapper implements RowMapper<UserModel> {
        @Override
        public UserModel mapRow(ResultSet rs, int arg1) throws SQLException {
            UserModel response = new UserModel();
            response.setId(rs.getInt("id"));
            response.setInstitution(rs.getString("financial_institution_code"));
            response.setFirstname(rs.getString("firstname"));
            response.setSurname(rs.getString("surname"));
            response.setPhone_number(rs.getString("phone_number"));
            response.setEmail_address(rs.getString("email_address"));
            response.setDate_created(rs.getString("date_created"));
            response.setInstitutionName(rs.getString("institutionname"));
            return response;
        }
    }
    
    class ContactMapper2 implements RowMapper<UserModel> {
        @Override
        public UserModel mapRow(ResultSet rs, int arg1) throws SQLException {
            UserModel response = new UserModel();
            response.setId(rs.getInt("id"));
            response.setInstitution(rs.getString("financial_institution_code"));
            response.setFirstname(rs.getString("firstname"));
            response.setSurname(rs.getString("surname"));
            response.setPhone_number(rs.getString("phone_number"));
            response.setEmail_address(rs.getString("email_address"));
            response.setDate_created(rs.getString("date_created"));
            response.setInstitutionName(rs.getString("institutionname"));
            response.setActionType(rs.getString("actionType"));
            response.setNote(rs.getString("note"));
            mapContactOperationSubmitter(response, rs);
            return response;
        }
    }

    private void mapContactOperationSubmitter(UserModel response, ResultSet rs) throws SQLException {
        if (!UsersService.hasColumn(rs, "created_by")) {
            return;
        }
        String submitter = rs.getString("created_by");
        if (submitter != null && !submitter.isEmpty()) {
            response.setCreator(submitter);
            response.setCreated_by(submitter);
        }
    }
    
    class FinancialInstitutionMapper implements RowMapper<FinancialInstitutionModel> {
        @Override
        public FinancialInstitutionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            FinancialInstitutionModel response = new FinancialInstitutionModel();
            
            response.setId(rs.getInt("id"));
            response.setName(rs.getString("name"));
            response.setPort_number(rs.getInt("port_number"));
            response.setPublickeylocation(rs.getString("publickeylocation"));
            response.setShortName(rs.getString("shortName"));
            response.setCode(rs.getString("code"));
            response.setColor(rs.getString("color"));
            response.setBusinessType(rs.getInt("businessType"));
            response.setBusiness_address(rs.getString("business_address"));
            response.setDate_created(rs.getString("date_created"));
            response.setBusinessTypeName(rs.getString("businessTypeName"));
            response.setStatus(rs.getString("status"));
            response.setDate_updated(rs.getString("date_updated"));
            response.setCharge_amount(rs.getFloat("charge_amount"));
            response.setVat(rs.getFloat("vat"));
            response.setCbn_bank_account(rs.getString("cbn_bank_account"));
            response.setIsProcessTSQ(rs.getInt("isProcessTSQ"));
            mapOptionalInstitutionColumns(rs, response);
            return response;
        }
    }

    private void mapOptionalInstitutionColumns(ResultSet rs, FinancialInstitutionModel response) throws SQLException {
        if (UsersService.hasColumn(rs, "serverIP")) {
            response.setServerIP(rs.getString("serverIP"));
        }
        if (UsersService.hasColumn(rs, "neTimeout")) {
            response.setNeTimeout(rs.getInt("neTimeout"));
        }
        if (UsersService.hasColumn(rs, "ftTimeout")) {
            response.setFtTimeout(rs.getInt("ftTimeout"));
        }
        if (UsersService.hasColumn(rs, "url")) {
            response.setUrl(rs.getString("url"));
        }
        if (UsersService.hasColumn(rs, "urlTSQ")) {
            response.setUrlTSQ(rs.getString("urlTSQ"));
        }
        if (UsersService.hasColumn(rs, "neEnvelope")) {
            response.setNeEnvelope(rs.getString("neEnvelope"));
        }
        if (UsersService.hasColumn(rs, "neResponseStartTag")) {
            response.setNeResponseStartTag(rs.getString("neResponseStartTag"));
        }
        if (UsersService.hasColumn(rs, "neResponseEndTag")) {
            response.setNeResponseEndTag(rs.getString("neResponseEndTag"));
        }
        if (UsersService.hasColumn(rs, "ftEnvelope")) {
            response.setFtEnvelope(rs.getString("ftEnvelope"));
        }
        if (UsersService.hasColumn(rs, "ftResponseStartTag")) {
            response.setFtResponseStartTag(rs.getString("ftResponseStartTag"));
        }
        if (UsersService.hasColumn(rs, "ftResponseEndTag")) {
            response.setFtResponseEndTag(rs.getString("ftResponseEndTag"));
        }
        if (UsersService.hasColumn(rs, "tsqEnvelope")) {
            response.setTsqEnvelope(rs.getString("tsqEnvelope"));
        }
        if (UsersService.hasColumn(rs, "tsqResponseStartTag")) {
            response.setTsqResponseStartTag(rs.getString("tsqResponseStartTag"));
        }
        if (UsersService.hasColumn(rs, "tsqResponseEndTag")) {
            response.setTsqResponseEndTag(rs.getString("tsqResponseEndTag"));
        }
        if (UsersService.hasColumn(rs, "ext_active")) {
            response.setEnableInward(rs.getInt("ext_active"));
        }
    }
    
    class FinancialInstitutionMapper2 implements RowMapper<FinancialInstitutionModel> {
        @Override
        public FinancialInstitutionModel mapRow(ResultSet rs, int arg1) throws SQLException {
            FinancialInstitutionModel response = new FinancialInstitutionModel();
            
            response.setId(rs.getInt("id"));
            response.setName(rs.getString("name"));
            response.setPort_number(rs.getInt("port_number"));
            response.setPublickeylocation(rs.getString("publickeylocation"));
            response.setShortName(rs.getString("shortName"));
            response.setCode(rs.getString("code"));
            response.setColor(rs.getString("color"));
            response.setBusinessType(rs.getInt("businessType"));
            response.setBusiness_address(rs.getString("business_address"));
            response.setDate_created(rs.getString("date_created"));
            response.setBusinessTypeName(rs.getString("businessTypeName"));
            response.setActionType(rs.getString("actionType"));
            response.setNote(rs.getString("note"));
            if (UsersService.hasColumn(rs, "created_by")) {
                String submitter = rs.getString("created_by");
                if (submitter != null && !submitter.isEmpty()) {
                    response.setCreated_by(submitter);
                }
            }
            if (UsersService.hasColumn(rs, "charge_amount")) {
                response.setCharge_amount(rs.getFloat("charge_amount"));
            }
            if (UsersService.hasColumn(rs, "vat")) {
                response.setVat(rs.getFloat("vat"));
            }
            if (UsersService.hasColumn(rs, "cbn_bank_account")) {
                response.setCbn_bank_account(rs.getString("cbn_bank_account"));
            }
            if (UsersService.hasColumn(rs, "isProcessTSQ")) {
                response.setIsProcessTSQ(rs.getInt("isProcessTSQ"));
            }
            if (UsersService.hasColumn(rs, "password")) {
                byte[] encodedPassword = rs.getBytes("password");
                if (encodedPassword != null && encodedPassword.length > 0) {
                    response.setPassword(java.util.HexFormat.of().formatHex(encodedPassword));
                }
            }
            if (UsersService.hasColumn(rs, "hashkey")) {
                response.setHashKey(rs.getString("hashkey"));
            }
            if (UsersService.hasColumn(rs, "issettlementbank")) {
                response.setIssettlementbank(rs.getInt("issettlementbank"));
            }
            if (UsersService.hasColumn(rs, "serverIP")) {
                response.setServerIP(rs.getString("serverIP"));
            }
            if (UsersService.hasColumn(rs, "neTimeout")) {
                response.setNeTimeout(rs.getInt("neTimeout"));
            }
            if (UsersService.hasColumn(rs, "ftTimeout")) {
                response.setFtTimeout(rs.getInt("ftTimeout"));
            }
            if (UsersService.hasColumn(rs, "url")) {
                response.setUrl(rs.getString("url"));
            }
            if (UsersService.hasColumn(rs, "urlTSQ")) {
                response.setUrlTSQ(rs.getString("urlTSQ"));
            }
            if (UsersService.hasColumn(rs, "neEnvelope")) {
                response.setNeEnvelope(rs.getString("neEnvelope"));
            }
            if (UsersService.hasColumn(rs, "neResponseStartTag")) {
                response.setNeResponseStartTag(rs.getString("neResponseStartTag"));
            }
            if (UsersService.hasColumn(rs, "neResponseEndTag")) {
                response.setNeResponseEndTag(rs.getString("neResponseEndTag"));
            }
            if (UsersService.hasColumn(rs, "ftEnvelope")) {
                response.setFtEnvelope(rs.getString("ftEnvelope"));
            }
            if (UsersService.hasColumn(rs, "ftResponseStartTag")) {
                response.setFtResponseStartTag(rs.getString("ftResponseStartTag"));
            }
            if (UsersService.hasColumn(rs, "ftResponseEndTag")) {
                response.setFtResponseEndTag(rs.getString("ftResponseEndTag"));
            }
            if (UsersService.hasColumn(rs, "tsqEnvelope")) {
                response.setTsqEnvelope(rs.getString("tsqEnvelope"));
            }
            if (UsersService.hasColumn(rs, "tsqResponseStartTag")) {
                response.setTsqResponseStartTag(rs.getString("tsqResponseStartTag"));
            }
            if (UsersService.hasColumn(rs, "tsqResponseEndTag")) {
                response.setTsqResponseEndTag(rs.getString("tsqResponseEndTag"));
            }
            if (UsersService.hasColumn(rs, "instWithWallet")) {
                response.setInstWithWallet(rs.getInt("instWithWallet"));
            }
            if (UsersService.hasColumn(rs, "walletname")) {
                response.setWalletname(rs.getString("walletname"));
            }
            if (UsersService.hasColumn(rs, "wallettype")) {
                response.setWallettype(rs.getInt("wallettype"));
            }
            return response;
        }
    }
    
    class InstitutionTypesMapper implements RowMapper<InstitutionTypesModel> {
        @Override
        public InstitutionTypesModel mapRow(ResultSet rs, int arg1) throws SQLException {
            InstitutionTypesModel response = new InstitutionTypesModel();
            
            response.setId(rs.getInt("id"));
            response.setName(rs.getString("name"));
            response.setDate_created(rs.getString("date_created"));
            return response;
        }
    }
}
