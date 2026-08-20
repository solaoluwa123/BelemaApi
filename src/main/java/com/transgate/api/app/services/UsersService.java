/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.util.ResponseManager;
import com.transgate.api.interfaces.UsersInterface;
import com.transgate.api.audit.AuditConstants;
import com.transgate.api.audit.AuditService;
import com.transgate.api.models.LoginResponse;
import com.transgate.api.models.MenuModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.RoleModel;
import com.transgate.api.models.UserModel;
import com.transgate.api.util.PlatformRole;
import com.transgate.api.util.Randomizer;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import static java.lang.Integer.parseInt;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 *
 * @author Makintola
 */
@Service
public class UsersService implements UsersInterface {

    @Autowired
    @Qualifier("jdbcTemplate")
    JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditService auditService;

    ResponseManager responseManager = new ResponseManager();
    Randomizer randomizer = new Randomizer();
    private static final Logger logger = LoggerFactory.getLogger(UsersService.class);

    private final Validators validators;

    // Constructor injection for RestCall
    public UsersService(Validators validators) {
        this.validators = validators;
    }

    private int GetUserRole(String username, String session_token) {
        try {
            int role;

            String SQL = "SELECT role FROM tbl_user_details WHERE (email_address = ? OR username = ?) AND deleted = 0 AND session_token = ?";
            role = jdbcTemplate.queryForObject(SQL, new Object[]{username, username, session_token}, int.class);
            return role;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return -100;
        }
    }

    /**
     * Resolve the calling user's role from the session token alone.
     * Prefer this for admin mutations where the request body/path carries the
     * *target* user's username (e.g. /users/edit, DELETE /users/{id}/{username}),
     * not the actor's identity.
     */
    private int GetActorRole(String session_token) {
        if (session_token == null || session_token.isBlank()) {
            return -100;
        }
        try {
            Integer role = jdbcTemplate.queryForObject(
                    "SELECT role FROM tbl_user_details WHERE session_token = ? AND deleted = 0 LIMIT 1",
                    new Object[]{session_token},
                    Integer.class);
            return role != null ? role : -100;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return -100;
        }
    }

    private void auditAuth(String email, String username, Integer role, String action, String outcome, int httpStatus, String details) {
        try {
            if (auditService != null) {
                auditService.logAuthEvent(email, username, role, action, outcome, httpStatus, details);
            }
        } catch (Exception ex) {
            logger.warn("Audit auth event failed: {}", ex.getMessage());
        }
    }

    private Integer lookupRoleByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT role FROM tbl_user_details WHERE (email_address = ? OR username = ?) AND deleted = 0 LIMIT 1",
                    new Object[]{email, email},
                    Integer.class);
        } catch (DataAccessException ex) {
            return null;
        }
    }

    private String lookupUsernameByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT username FROM tbl_user_details WHERE (email_address = ? OR username = ?) AND deleted = 0 LIMIT 1",
                    new Object[]{email, email},
                    String.class);
        } catch (DataAccessException ex) {
            return email;
        }
    }

    private String GetUserSecret(String username, String session_token) {
        try {
            String secret;

            String SQL = "SELECT a.two_fa_secret "
                    + "FROM sparkpayweb_db.tbl_users a "
                    + "LEFT JOIN tbl_user_details b "
                    + "ON b.email_address = a.username "
                    + "WHERE a.username = ? AND b.deleted = 0 AND b.session_token = ?";
            secret = jdbcTemplate.queryForObject(SQL, new Object[]{username, session_token}, String.class);
            return secret;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return "";
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

    private int insertPendingUserOperation(String username, String hashPassword, String firstname, String surname,
            String phone_number, String email_address, int roleid, String createdBy, String actionType, String note) {
        String SQL = "INSERT INTO tbl_user_details_operations(username, password, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
        logger.info("Executing SQL (tbl_user_details_operations): " + SQL);
        return jdbcTemplate.update(SQL, new Object[]{username, hashPassword, firstname, surname, phone_number, email_address, roleid, actionType, note});
    }

    private boolean CheckUserPending(String username, String email_address, String actionType) {
        boolean found;
        try {
            String SQL;
            SQL = "SELECT COUNT(*) FROM tbl_user_details_operations WHERE username = ? AND actionType = ? OR email_address = ? AND actionType = ?";
            int totalRows = jdbcTemplate.queryForObject(SQL, new Object[]{username, actionType, email_address, actionType}, int.class);

            found = totalRows > 0;

            return found;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return false;
        }
    }

    private List<UserModel> GetUserFromPendings(int id, String actionType) {
        try {
            String SQL;
            SQL = "SELECT a.id, a.username, a.password, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.actionType, a.note, a.date_created, "
                    + "b.role_name, c.financial_institution_code "
                    + "from tbl_user_details_operations a "
                    + "LEFT JOIN tbl_role b "
                    + "ON a.role = b.id "
                    + "LEFT JOIN tbl_financial_institution_contacts_operations c "
                    + "ON a.email_address = c.email_address AND c.actionType = a.actionType "
                    + "WHERE a.id = ? AND a.actionType = ? "
                    + "ORDER BY a.id DESC";
            List<UserModel> users = jdbcTemplate.query(SQL, new Object[]{id, actionType}, new UserMapper2());
            return users;

        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return null;
        }
    }

    /**
     * On user create approval for role 4 (institution contact), promote pending contact
     * into tbl_financial_institution_contacts (not the _operations table — that is staging only).
     */
    private void ApprovePendingFinancialInstitutionContact(UserModel user, String financialInstitutionCode) {
        String email = user.getEmail_address();
        try {
            int existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tbl_financial_institution_contacts WHERE email_address = ?",
                    new Object[]{email},
                    int.class);
            if (existing > 0) {
                jdbcTemplate.update(
                        "DELETE FROM tbl_financial_institution_contacts_operations WHERE email_address = ? AND actionType = 'create'",
                        email);
                logger.info("Contact already exists for {}; cleared pending contact operations.", email);
                return;
            }

            String institutionCode = null;
            String firstname = user.getFirstname();
            String surname = user.getSurname();
            String phone = user.getPhone_number();

            String SQL = "SELECT financial_institution_code, firstname, surname, phone_number "
                    + "FROM tbl_financial_institution_contacts_operations "
                    + "WHERE email_address = ? AND actionType = 'create' "
                    + "ORDER BY id DESC LIMIT 1";
            List<Map<String, Object>> pending = jdbcTemplate.queryForList(SQL, email);
            if (!pending.isEmpty()) {
                Map<String, Object> row = pending.get(0);
                if (row.get("financial_institution_code") != null) {
                    institutionCode = String.valueOf(row.get("financial_institution_code"));
                }
                if (row.get("firstname") != null) {
                    firstname = String.valueOf(row.get("firstname"));
                }
                if (row.get("surname") != null) {
                    surname = String.valueOf(row.get("surname"));
                }
                if (row.get("phone_number") != null) {
                    phone = String.valueOf(row.get("phone_number"));
                }
            }

            if (institutionCode == null || institutionCode.isEmpty()) {
                if (financialInstitutionCode != null && !financialInstitutionCode.isEmpty()) {
                    institutionCode = financialInstitutionCode;
                } else if (user.getInstitution() != null && !user.getInstitution().isEmpty()) {
                    institutionCode = user.getInstitution();
                } else if (user.getNote() != null && !user.getNote().isEmpty()
                        && !"Create user account".equals(user.getNote())
                        && !"Create account".equals(user.getNote())
                        && !"Create contact account".equals(user.getNote())
                        && !"Create contact".equals(user.getNote())
                        && !"Edit account".equals(user.getNote())
                        && !"Edit contact account".equals(user.getNote())
                        && !"Delete account".equals(user.getNote())
                        && !"Delete contact account".equals(user.getNote())) {
                    institutionCode = user.getNote();
                }
            }

            if (institutionCode == null || institutionCode.isEmpty()) {
                logger.error("Cannot approve role 4 contact {}: no financial_institution_code in pending operations, note, or approval request.", email);
                throw new IllegalStateException("Missing financial institution code for contact " + email);
            }

            SQL = "INSERT INTO tbl_financial_institution_contacts("
                    + "financial_institution_code, firstname, surname, phone_number, email_address, date_created) "
                    + "VALUES(?, ?, ?, ?, ?, now())";
            int inserted = jdbcTemplate.update(SQL,
                    new Object[]{institutionCode, firstname, surname, phone, email});
            if (inserted > 0) {
                jdbcTemplate.update(
                        "DELETE FROM tbl_financial_institution_contacts_operations WHERE email_address = ? AND actionType = 'create'",
                        email);
                logger.info("Approved contact saved to tbl_financial_institution_contacts for {} (institution={})", email, institutionCode);
            } else {
                throw new IllegalStateException("Insert into tbl_financial_institution_contacts failed for " + email);
            }
        } catch (DataAccessException ex) {
            logger.error("Failed to promote pending contact for {}: {}", email, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public ResponseEntity ClearUserSession(String sessiontoken, String username) {
        try {
            Integer role = lookupRoleByEmail(username);
            String displayName = lookupUsernameByEmail(username);
            String SQL = "UPDATE tbl_user_details SET session_token = '' WHERE session_token = ? AND username = ?";
            jdbcTemplate.update(SQL, new Object[]{sessiontoken, username});
            auditAuth(username, displayName, role, AuditConstants.ACTION_LOGOUT,
                    AuditConstants.OUTCOME_SUCCESS, 202, "Logout successful");
            return responseManager.ResponseAccepted();
        } catch (DataAccessException ex) {
            System.out.println(ex);
            auditAuth(username, username, null, AuditConstants.ACTION_LOGOUT,
                    AuditConstants.OUTCOME_FAILURE, 401, "Logout failed");
            return responseManager.ResponseUnathorized();
        }
    }

    @Override
    public ResponseEntity Login2FA(String _sessiontoken, String username, String password) {
        LoginResponse response = new LoginResponse();
        try {
            String SQL;
            String secret = GetUserSecret(username, _sessiontoken);
//            String code = randomizer.GetTOTPCode(secret);

            if (randomizer.AuthorizeGoogleAuthenticatorCode(secret, Integer.parseInt(password))) {
                response.setTwofaenabled(1);
                Date now = new Date();
                Date expirationDate = new Date(now.getTime() + (1000 * 60 * 120));
                String sessiontoken = validators.GenerateJSONWebToken(username, expirationDate);
                SQL = "UPDATE tbl_user_details SET attempts_left = 3, last_login = now(), session_token = ? WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{sessiontoken, username});
                SQL = "SELECT a.id, a.username, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.date_created, a.date_updated, a.session_token, a.last_login, b.role_name, c.financial_institution_code, d.name as institution_name "
                        + "from tbl_user_details a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "LEFT JOIN tbl_financial_institution_contacts c "
                        + "ON a.email_address = c.email_address "
                        + "LEFT JOIN tbl_financial_institutions d "
                        + "ON c.financial_institution_code = d.code "
                        + "WHERE a.email_address = ? || a.username = ? AND a.deleted = 0";

                List<UserModel> details = jdbcTemplate.query(SQL, new Object[]{username, username}, new UserMapper());
                response.setCode(200);
                response.setStatus("success");
                response.setMessage("Login successful");
                int userRoleid;
                if (details.size() > 0) {
                    userRoleid = details.get(0).getRoleid();
                    response.setId(details.get(0).getId());
                    response.setUsername(details.get(0).getUsername());
                    response.setEmail_address(details.get(0).getEmail_address());
                    response.setFirstname(details.get(0).getFirstname());
                    response.setSurname(details.get(0).getSurname());
                    response.setPhone_number(details.get(0).getPhone_number());
                    response.setRoleid(userRoleid);
                    response.setRole(details.get(0).getRole());
                    response.setDate_created(details.get(0).getDate_created());
                    response.setFinancial_institution_code(details.get(0).getInstitution());
                    response.setFinancial_institution_name(details.get(0).getInstitutionName());
                    response.setLast_login(details.get(0).getLast_login());
                    response.setSession_token(sessiontoken);
                    response.setDate_updated(details.get(0).getDate_updated());
                } else {
                    userRoleid = 4;
                    response.setId(parseInt(randomizer.GenerateReference(5, "1234567890")));
                    response.setUsername(username);
                    response.setEmail_address(username);
                    response.setFirstname("");
                    response.setSurname("");
                    response.setPhone_number("");
                    response.setRoleid(4);
                    response.setRole("Third Party Vendor");
                    response.setDate_created((new Date()).toString());
                    response.setDate_updated(null);
                    response.setSession_token(sessiontoken);
                }

                if (userRoleid == 5) {
                    SQL = "SELECT a.id, a.institution_id, b.acquirer_id, b.institution_name "
                            + "FROM tbl_map_card_users_institution a "
                            + "LEFT JOIN sparkpayweb_db.tbl_financial_institutions b "
                            + "ON a.institution_id = b.bank_code "
                            + "WHERE a.user_email = ?";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, new Object[]{username});
                    if (rows.size() > 0) {
                        response.setFinancial_institution_code((String) rows.get(0).get("acquirer_id"));
                        response.setFinancial_institution_name((String) rows.get(0).get("institution_name"));
                    }
                }

                if (userRoleid == 6) {
                    SQL = "SELECT a.id, a.institution_id, a.institution_name "
                            + "FROM tbl_map_card_users_institution a "
                            + "WHERE a.user_email = ?";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, new Object[]{username});
                    if (rows.size() > 0) {
                        response.setFinancial_institution_code((String) rows.get(0).get("institution_id"));
                        response.setFinancial_institution_name((String) rows.get(0).get("institution_name"));
                    }
                }

                if (userRoleid == 7) {
                    SQL = "SELECT a.id, a.institution_id, a.institution_name "
                            + "FROM tbl_map_card_users_institution a "
                            + "WHERE a.user_email = ?";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, new Object[]{username});
                    if (rows.size() > 0) {
                        response.setFinancial_institution_code((String) rows.get(0).get("institution_id"));
                        response.setFinancial_institution_name((String) rows.get(0).get("institution_name"));
                    }
                }

                if (userRoleid == 8) {
                    SQL = "SELECT a.id, a.institution_id, a.institution_name "
                            + "FROM tbl_map_card_users_institution a "
                            + "WHERE a.user_email = ?";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, new Object[]{username});
                    if (rows.size() > 0) {
                        response.setFinancial_institution_code((String) rows.get(0).get("institution_id"));
                        response.setFinancial_institution_name((String) rows.get(0).get("institution_name"));
                    }
                }

                List<MenuModel> menu;
                if (PlatformRole.hasTransgateMenu(userRoleid)) {
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = 0 AND a.access = 1 OR a.parent_id IS NULL AND a.role_id = ? AND a.access = 1 "
                            + "ORDER BY a.id ASC";
                    menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());

                    if (menu.size() > 0) {
                        response.setTransgateMenu(menu);
                    } else {
                        List<MenuModel> eM = new ArrayList<>();
                        response.setTransgateMenu(eM);
                    }
                } else if (userRoleid == 8) {
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = ? AND a.access = 1 "
                            + "ORDER BY a.id ASC";
                    menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());

                    if (menu.size() > 0) {
                        response.setTransgateMenu(menu);
                    } else {
                        List<MenuModel> eM = new ArrayList<>();
                        response.setTransgateMenu(eM);
                    }
                } else {
                    response.setTransgateMenu(new ArrayList<>());
                }
                if (PlatformRole.hasSparkpayMenu(userRoleid)) {
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = 0 AND a.access = 2 OR a.parent_id IS NULL AND a.role_id = ? AND a.access = 2 "
                            + "ORDER BY a.id ASC";
                } else {
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = ? AND a.access = 2 "
                            + "ORDER BY a.id ASC";
                }
                menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());

                if (menu.size() > 0) {
                    response.setSparkpayMenu(menu);
                } else {
                    List<MenuModel> eM = new ArrayList<>();
                    response.setSparkpayMenu(eM);
                }
                auditAuth(username, response.getUsername(), userRoleid,
                        AuditConstants.ACTION_LOGIN_2FA, AuditConstants.OUTCOME_SUCCESS, 200, "2FA login successful");
                return responseManager.ResponseOk(response);
            } else {
                SQL = "UPDATE tbl_user_details SET attempts_left = attempts_left - 1 WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{username});
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Invalid 2FA");
                auditAuth(username, lookupUsernameByEmail(username), lookupRoleByEmail(username),
                        AuditConstants.ACTION_LOGIN_2FA_FAILED, AuditConstants.OUTCOME_FAILURE, 200, "Invalid 2FA code");
                return responseManager.ResponseOk(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid username or password");
                auditAuth(username, username, null,
                        AuditConstants.ACTION_LOGIN_2FA_FAILED, AuditConstants.OUTCOME_FAILURE, 200, "User not found");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            auditAuth(username, username, null,
                    AuditConstants.ACTION_LOGIN_2FA_FAILED, AuditConstants.OUTCOME_FAILURE, 401, "2FA login error");
            return responseManager.ResponseUnathorized();
        } catch (Exception ex) {
            System.out.println("error>>>>" + ex.getMessage());
            auditAuth(username, username, null,
                    AuditConstants.ACTION_LOGIN_2FA_FAILED, AuditConstants.OUTCOME_FAILURE, 401, "Unexpected 2FA error");
            return responseManager.ResponseUnathorized();
        }
    }

    @Override
    public boolean LoginExternal(String username, String password) {
        logger.info(String.format("LoginExternal called with username='%s'", username));
        try {
            String SQL = "SELECT attempts_left FROM tbl_user_details WHERE email_address = ?";
            int attemptsLeft = jdbcTemplate.queryForObject(SQL, new Object[]{username}, int.class);
            logger.info(String.format("Attempts left for user '%s': %d", username, attemptsLeft));

            if (attemptsLeft == 0) {
                logger.info(String.format("No attempts left; locking account for 15 minutes for user '%s'", username));
                SQL = "UPDATE tbl_user_details SET unlock_account_in = 15 WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{username});
                logger.info("Returning false due to account lock");
                return false;
            }

            SQL = "SELECT a.password, a.two_fa_enabled, a.two_fa_secret "
                    + "FROM sparkpayweb_db.tbl_users a "
                    + "LEFT JOIN tbl_user_details b ON a.username = b.email_address "
                    + "WHERE a.username = ? AND a.enabled = 1 AND b.deleted = 0";
            logger.info(String.format("Querying credentials for user '%s'", username));
            List<Map<String, Object>> users = jdbcTemplate.queryForList(SQL, new Object[]{username});
            logger.info(String.format("Query returned %d record(s) for username='%s'", users.size(), username));

            if (users.size() != 1) {
                logger.info(String.format("Invalid user count (%d) for '%s', returning false", users.size(), username));
                return false;
            }

            String security = (String) users.get(0).get("password");
            logger.info("Password hash retrieved, verifying password");
            boolean comparePassword = BCrypt.checkpw(password, security);
            logger.info(String.format("Password match for user '%s': %b", username, comparePassword));
            return comparePassword;

        } catch (DataAccessException ex) {
            logger.info(String.format("DataAccessException during LoginExternal for '%s': %s", username, ex.getMessage()));
            return false;
        }
    }
    
    @Override
    public boolean LoginExternal2(String username, String sessionToken) {
        int isSuccessful = 0;
        logger.info(String.format("LoginExternal called with username='%s'", username));
        try {
            String SQL = "SELECT attempts_left FROM tbl_user_details WHERE email_address = ?";
            int attemptsLeft = jdbcTemplate.queryForObject(SQL, new Object[]{username}, int.class);
            logger.info(String.format("Attempts left for user '%s': %d", username, attemptsLeft));

            if (attemptsLeft == 0) {
                logger.info(String.format("No attempts left; locking account for 15 minutes for user '%s'", username));
                SQL = "UPDATE tbl_user_details SET unlock_account_in = 15 WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{username});
                logger.info("Returning false due to account lock");
                return false;
            }
            
                SQL = "UPDATE tbl_user_details SET attempts_left = 3, last_login = now(), session_token = ? WHERE email_address = ?";
                logger.info("{} : sql query to reset password{}.", username, SQL);
                isSuccessful = jdbcTemplate.update(SQL, new Object[]{sessionToken, username});
                logger.info("User {} authenticated successfully.", username);

                return isSuccessful == 1;

           

        } catch (DataAccessException ex) {
            logger.info(String.format("DataAccessException during LoginExternal for '%s': %s", username, ex.getMessage()));
            return false;
        }
    }


    @Override
    public ResponseEntity Login(String username, String password) {
        logger.info("Login attempt initiated for user: {}", username);
        LoginResponse response = new LoginResponse();
        try {
            // Query to fetch the number of login attempts left
            String sql = "SELECT attempts_left FROM tbl_user_details WHERE email_address = ?";
            logger.info("{} : sql query to get attempts left {} -->", username, sql);
            int attemptsLeft = jdbcTemplate.queryForObject(sql, new Object[]{username}, int.class);
            logger.info("User {} has {} login attempts remaining.", username, attemptsLeft);

            if (attemptsLeft == 0) {
                logger.warn("Account locked for user {} due to multiple failed login attempts.", username);
                sql = "UPDATE tbl_user_details SET unlock_account_in = 15 WHERE email_address = ?";
                logger.info("{} : sql query to update unlock time --> {}.", username, sql);
                jdbcTemplate.update(sql, new Object[]{username});
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Account locked for invalid multiple attempts, try again in 15 minutes");
                auditAuth(username, lookupUsernameByEmail(username), lookupRoleByEmail(username),
                        AuditConstants.ACTION_LOGIN_FAILED, AuditConstants.OUTCOME_FAILURE, 200, "Account locked");
                return responseManager.ResponseOk(response);
            }

            // Query to obtain user security details including password and two-factor configuration
            sql = "SELECT a.password, a.two_fa_enabled, a.two_fa_secret from sparkpayweb_db.tbl_users a "
                    + "LEFT JOIN tbl_user_details b ON a.username = b.email_address "
                    + "WHERE a.username = ? AND a.enabled = 1 AND b.deleted = 0";
            logger.info("Fetching security details for user --> {}", username);
            logger.info("{} : sql query to obtain user security details including password and two-factor configuration {}.", username, sql);
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, new Object[]{username});
            String security = "";
            String twoFaSecret = "";
            boolean comparePassword = false;
            int twoFaEnabled = 0;

            if (!users.isEmpty()) {
                security = (String) users.get(0).get("password");
                twoFaSecret = (String) users.get(0).get("two_fa_secret");
                twoFaEnabled = (int) users.get(0).get("two_fa_enabled");
                response.setTwofaenabled(twoFaEnabled);
                logger.info("User {} found. two_fa_enabled: {}.", username, twoFaEnabled);
            } else {
                logger.info("No matching user record found for username: {}", username);
            }
            logger.info("Password before check for user {}: {}", username, password);
            if (password != null && password.length() > 0) {
                logger.info("Password after check for user {}: {}", username, password);
                comparePassword = BCrypt.checkpw(password, security);
                logger.info("Password comparison result for user {}: {}", username, comparePassword);
            }

            // Generate JWT session token valid for 120 minutes
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + (1000 * 60 * 120)); // 120 minutes
            String sessionToken = validators.GenerateJSONWebToken(username, expirationDate);
            response.setSession_token(sessionToken);
            response.setUsername(username);
            logger.info("Session token generated for user {}: {}", username, sessionToken);

            if (comparePassword) {
                sql = "UPDATE tbl_user_details SET attempts_left = 3, last_login = now(), session_token = ? WHERE email_address = ?";
                logger.info("{} : sql query to reset password{}.", username, sql);
                jdbcTemplate.update(sql, new Object[]{sessionToken, username});
                logger.info("User {} authenticated successfully.", username);

                // If two-factor authentication is enabled, return challenge response (JWT pending OTP).
                if (twoFaEnabled == 1) {
                    response.setCode(200);
                    response.setStatus("success");
                    response.setMessage("2FA required");
                    response.setEmail_address(username);
                    response.setUsername(username);
                    response.setTwofaenabled(1);
                    auditAuth(username, lookupUsernameByEmail(username), lookupRoleByEmail(username),
                            AuditConstants.ACTION_LOGIN, AuditConstants.OUTCOME_SUCCESS, 200, "Password ok; 2FA required");
                    return responseManager.ResponseOk(response);
                }

                // Query to fetch complete user details
                sql = "SELECT a.id, a.username, a.firstname, a.surname, a.phone_number, a.email_address, a.role, "
                        + "a.date_created, a.date_updated, a.session_token, a.last_login, b.role_name, c.financial_institution_code, "
                        + "d.name as institution_name "
                        + "FROM tbl_user_details a "
                        + "LEFT JOIN tbl_role b ON a.role = b.id "
                        + "LEFT JOIN tbl_financial_institution_contacts c ON a.email_address = c.email_address "
                        + "LEFT JOIN tbl_financial_institutions d ON c.financial_institution_code = d.code "
                        + "WHERE a.email_address = ? || a.username = ? AND a.deleted = 0";
                logger.info("Fetching full user details for {}", username);
                logger.info("{} : sql query to fetch complete user detail{}.", username, sql);
                List<UserModel> details = jdbcTemplate.query(sql, new Object[]{username, username}, new UserMapper());
                response.setCode(200);
                response.setStatus("success");
                response.setMessage("Login successful");
                int userRoleid;

                if (!details.isEmpty()) {
                    userRoleid = details.get(0).getRoleid();
                    response.setId(details.get(0).getId());
                    response.setUsername(details.get(0).getUsername());
                    response.setEmail_address(details.get(0).getEmail_address());
                    response.setFirstname(details.get(0).getFirstname());
                    response.setSurname(details.get(0).getSurname());
                    response.setPhone_number(details.get(0).getPhone_number());
                    response.setRoleid(userRoleid);
                    response.setRole(details.get(0).getRole());
                    response.setDate_created(details.get(0).getDate_created());
                    response.setFinancial_institution_code(details.get(0).getInstitution());
                    response.setFinancial_institution_name(details.get(0).getInstitutionName());
                    response.setLast_login(details.get(0).getLast_login());
                    response.setDate_updated(details.get(0).getDate_updated());
                    logger.info("User details populated for {}", username);
                } else {
                    userRoleid = 4;
                    response.setId(parseInt(randomizer.GenerateReference(5, "1234567890")));
                    response.setUsername(username);
                    response.setEmail_address(username);
                    response.setFirstname("");
                    response.setSurname("");
                    response.setPhone_number("");
                    response.setRoleid(4);
                    response.setRole("Third Party Vendor");
                    response.setDate_created(now.toString());
                    response.setDate_updated(null);
                    logger.info("No detailed user record found for {}. Defaulting role to 'Third Party Vendor'.", username);
                }

                // Retrieve financial institution details based on user role
                if (userRoleid == 5 || userRoleid == 6 || userRoleid == 7 || userRoleid == 8) {
                    sql = "SELECT a.id, a.institution_id, a.institution_name FROM tbl_map_card_users_institution a WHERE a.user_email = ?";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[]{username});
                    if (!rows.isEmpty()) {
                        // Set the details based on the role (using the same fields for simplicity)
                        response.setFinancial_institution_code((String) rows.get(0).get("institution_id"));
                        response.setFinancial_institution_name((String) rows.get(0).get("institution_name"));
                        logger.info("Financial institution info for user {}: {} - {}", username,
                                response.getFinancial_institution_code(), response.getFinancial_institution_name());
                    }
                }

                // Handling menu based on user role
                List<MenuModel> menu;
                if (PlatformRole.hasTransgateMenu(userRoleid)) {
                    sql = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, "
                            + "b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM tbl_menus a LEFT JOIN tbl_menus b ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND (a.role_id = 0 OR a.role_id = ?) AND a.access = 1 "
                            + "ORDER BY a.id ASC";
                    logger.info("{} : sql query {}.", username, sql);
                    menu = jdbcTemplate.query(sql, new Object[]{userRoleid}, new MenuMapper());
                    response.setTransgateMenu(!menu.isEmpty() ? menu : new ArrayList<>());
                } else if (userRoleid == 8 || userRoleid == 5) {
                    sql = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, "
                            + "b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM tbl_menus a LEFT JOIN tbl_menus b ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = ? AND a.access = 1 "
                            + "ORDER BY a.id ASC";
                    logger.info("{} : sql query {}.", username, sql);
                    menu = jdbcTemplate.query(sql, new Object[]{userRoleid}, new MenuMapper());
                    response.setTransgateMenu(!menu.isEmpty() ? menu : new ArrayList<>());
                } else {
                    response.setTransgateMenu(new ArrayList<>());
                }

                // Retrieve secondary menu information based on role
                if (PlatformRole.hasSparkpayMenu(userRoleid)) {
                    sql = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, "
                            + "b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM tbl_menus a LEFT JOIN tbl_menus b ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND (a.role_id = 0 OR a.role_id = ?) AND a.access = 2 "
                            + "ORDER BY a.id ASC";
                    logger.info("{} : sql query {}.", username, sql);

                } else {
                    sql = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, "
                            + "b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM tbl_menus a LEFT JOIN tbl_menus b ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = ? AND a.access = 2 "
                            + "ORDER BY a.id ASC";
                    logger.info("{} : sql query {}.", username, sql);
                }
                menu = jdbcTemplate.query(sql, new Object[]{userRoleid}, new MenuMapper());
                response.setSparkpayMenu(!menu.isEmpty() ? menu : new ArrayList<>());
                logger.info("Login process completed successfully for user: {}", username);
                auditAuth(username, response.getUsername(), userRoleid,
                        AuditConstants.ACTION_LOGIN, AuditConstants.OUTCOME_SUCCESS, 200, "Login successful");
                return responseManager.ResponseOk(response);
            } else {
                logger.info("Invalid password provided for user: {}", username);
                sql = "UPDATE tbl_user_details SET attempts_left = attempts_left - 1 WHERE email_address = ?";
                logger.info("{} : sql query {}.", username, sql);
                jdbcTemplate.update(sql, new Object[]{username});
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Invalid username or password");
                auditAuth(username, lookupUsernameByEmail(username), lookupRoleByEmail(username),
                        AuditConstants.ACTION_LOGIN_FAILED, AuditConstants.OUTCOME_FAILURE, 200, "Invalid password");
                return responseManager.ResponseOk(response);
            }
        } catch (DataAccessException ex) {
            logger.info("DataAccessException while processing login for user {}: {}", username, ex.getMessage(), ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid username or password");
                auditAuth(username, username, null,
                        AuditConstants.ACTION_LOGIN_FAILED, AuditConstants.OUTCOME_FAILURE, 200, "User not found");
                return responseManager.ResponseOk(response);
            }
            auditAuth(username, username, null,
                    AuditConstants.ACTION_LOGIN_FAILED, AuditConstants.OUTCOME_FAILURE, 401, "Login error");
            return responseManager.ResponseUnathorized();
        } catch (Exception ex) {
            logger.info("Unexpected error while processing login for user {}: {}", username, ex.getMessage(), ex);
            auditAuth(username, username, null,
                    AuditConstants.ACTION_LOGIN_FAILED, AuditConstants.OUTCOME_FAILURE, 401, "Unexpected login error");
            return responseManager.ResponseUnathorized();
        }
    }

    @Override
    public ResponseEntity SetUp2FA(String sessiontoken, String username, int enable) {
        NetworkResponse response = new NetworkResponse();
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            if (userrole > 0) {
                String secret = "";
                GoogleAuthenticatorKey key = randomizer.GenerateGoogleAuthenticatorSecretKey();
                String qrCodeUri = "";
                response.setMessage("2FA disbaled successfully");
                if (enable == 1) {
//                    secret = randomizer.GenerateSecretKey();
                    secret = key.getKey();
//                    qrCodeUri = randomizer.GetGoogleAuthenticatorBarCode(secret, username, "Sparkpay");
                    qrCodeUri = randomizer.GetGoogleAuthenticatorQRCode("Sparkpay", username, key);
                    response.setMessage("2FA enabled successfully");
                }
                SQL = "UPDATE sparkpayweb_db.tbl_users SET two_fa_enabled = ?, two_fa_secret = ? "
                        + "WHERE username = ?";
                int ret = jdbcTemplate.update(SQL, new Object[]{enable, secret, username});
                if (ret > 0) {
                    response.setStatus("success");
                    // Escape for JSON string value — otpauth URIs can contain quotes/backslashes after encoding edge cases.
                    String escapedUri = qrCodeUri
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"");
                    String meta = "{\"qrCodeUri\":\"" + escapedUri + "\"}";
                    response.setMeta(meta);
                } else {
                    response.setStatus("failed");
                    response.setMessage("2FA setup failed");
                }
            }
            response.setCode(200);
            return responseManager.ResponseOk(response);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseUnathorized();
        }
    }

    @Override
    public ResponseEntity ResetPassword(String code, String password, String token) {
        LoginResponse response = new LoginResponse();
        try {
            String SQL;
            SQL = "SELECT a.password from sparkpayweb_db.tbl_users a "
                    + "WHERE a.reference = ? AND a.enabled = 1";
            String security = jdbcTemplate.queryForObject(SQL, new Object[]{code}, String.class);
//            boolean comparePassword = BCrypt.checkpw(token, security);
            if (security.equals(token)) {
                String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                String reference = randomizer.GenerateReference();
                SQL = "UPDATE sparkpayweb_db.tbl_users SET password = ?, reference = ? WHERE reference = ?";
                jdbcTemplate.update(SQL, new Object[]{hashPassword, reference, code});
                response.setCode(200);
                response.setMessage("Password reset complete");
                return responseManager.ResponseOk(response);
            } else {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Invalid reset code");
                return responseManager.ResponseOk(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Unable to handle password reset");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseUnathorized();
        }
    }

    @Override
    public ResponseEntity ActivateAccount(String code, String password, String token) {
        LoginResponse response = new LoginResponse();
        try {
            String SQL;
            SQL = "SELECT a.password from sparkpayweb_db.tbl_users a "
                    + "WHERE a.reference = ? AND a.enabled = 0";
            String security = jdbcTemplate.queryForObject(SQL, new Object[]{code}, String.class);
//            boolean comparePassword = BCrypt.checkpw(token, security);
            if (security.equals(token)) {
                String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                String reference = randomizer.GenerateReference();
                SQL = "UPDATE sparkpayweb_db.tbl_users SET password = ?, reference = ?, enabled = 1 WHERE reference = ?";
                jdbcTemplate.update(SQL, new Object[]{hashPassword, reference, code});
                response.setCode(200);
                response.setMessage("Account activated");
                return responseManager.ResponseOk(response);
            } else {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Invalid activation link");
                return responseManager.ResponseOk(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Unable to handle account activation");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseUnathorized();
        }
    }

    @Override
    public ResponseEntity SendPasswordRecoveryCode(String email) {
        LoginResponse response = new LoginResponse();
        try {
            String SQL;
            SQL = "SELECT attempts_left FROM tbl_user_details WHERE email_address = ?";
            int attemptsLeft = jdbcTemplate.queryForObject(SQL, new Object[]{email}, int.class);
            if (attemptsLeft == 0) {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Account locked for invalid multiple attempts, try again in 15 minutes");
                return responseManager.ResponseOk(response);
            }
            SQL = "SELECT a.id from sparkpayweb_db.tbl_users a "
                    + "LEFT JOIN tbl_user_details b "
                    + "ON a.username = b.email_address "
                    + "WHERE a.username = ? AND a.enabled = 1 AND b.deleted = 0";
            int found = jdbcTemplate.queryForObject(SQL, new Object[]{email}, int.class);

            if (found > 0) {
                String code = randomizer.GenerateReference(45, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890");
                String ref = randomizer.GenerateReference(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
                SQL = "UPDATE sparkpayweb_db.tbl_users SET reference = ?, password = ? WHERE username = ?";
                jdbcTemplate.update(SQL, new Object[]{ref, code, email});
                response.setCode(200);
                response.setMessage("Password recovery done");
                response.setFirstname(ref);
                response.setSurname(code);
                return responseManager.ResponseOk(response);
            }
            return responseManager.ResponseUnathorized();
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Email address not found");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetUserById(String sessiontoken, int userid) {
        LoginResponse response = new LoginResponse();
        try {
            String SQL;
            SQL = "SELECT a.id, a.username, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.date_created, a.date_updated, a.last_login, b.role_name, c.financial_institution_code, d.name as institution_name  "
                    + "from tbl_user_details a "
                    + "LEFT JOIN tbl_role b "
                    + "ON a.role = b.id "
                    + "LEFT JOIN tbl_financial_institution_contacts c "
                    + "ON a.email_address = c.email_address "
                    + "LEFT JOIN tbl_financial_institutions d "
                    + "ON c.financial_institution_code = d.code "
                    + "WHERE a.id = ? AND a.deleted = 0";
            List<UserModel> details = jdbcTemplate.query(SQL, new Object[]{userid}, new UserMapper());
            int userRoleid;
            if (details.size() > 0) {
                response.setCode(200);
                response.setStatus("success");
                response.setMessage("User detail");
                userRoleid = details.get(0).getRoleid();
                response.setId(details.get(0).getId());
                response.setUsername(details.get(0).getUsername());
                response.setEmail_address(details.get(0).getEmail_address());
                response.setFirstname(details.get(0).getFirstname());
                response.setSurname(details.get(0).getSurname());
                response.setPhone_number(details.get(0).getPhone_number());
                response.setRoleid(userRoleid);
                response.setRole(details.get(0).getRole());
                response.setDate_created(details.get(0).getDate_created());
                response.setDate_updated(details.get(0).getDate_updated());
                return responseManager.ResponseOk(response);
            } else {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("User not found");
                return responseManager.ResponseNotFound(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid username");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetUsers(boolean systemUsers) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            if (systemUsers) {
                SQL = "SELECT a.id, a.username, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.date_created, a.date_updated, a.last_login, b.role_name, c.financial_institution_code, d.name as institution_name  "
                        + "from tbl_user_details a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "LEFT JOIN tbl_financial_institution_contacts c "
                        + "ON a.email_address = c.email_address "
                        + "LEFT JOIN tbl_financial_institutions d "
                        + "ON c.financial_institution_code = d.code "
                        + "WHERE a.deleted = 0 AND a.role BETWEEN " + PlatformRole.ADMIN + " AND " + PlatformRole.APPROVER + " "
                        + "ORDER BY a.id DESC";
            } else {
                SQL = "SELECT a.id, a.username, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.date_created, a.date_updated, a.last_login, "
                        + "b.role_name, "
                        + "c.institution_id as financial_institution_code, c.institution_name as institution_name  "
                        + "from tbl_user_details a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "LEFT JOIN tbl_map_card_users_institution c "
                        + "ON a.email_address = c.user_email "
                        + "WHERE a.deleted = 0 AND a.role BETWEEN " + PlatformRole.OTHER_MIN + " AND " + PlatformRole.OTHER_MAX + " "
                        + "ORDER BY a.id DESC";
            }
            List<UserModel> users = jdbcTemplate.query(SQL, new UserMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(systemUsers ? "System Users" : "Other Users");
            networkResponse.setData((ArrayList) users);

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetRoles() {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            String SQL;
            SQL = "SELECT a.id, a.role_name, a.date_created "
                    + "from tbl_role a "
                    + "ORDER BY a.id ASC";
            List<RoleModel> roles = jdbcTemplate.query(SQL, new RoleMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("All roles");
            networkResponse.setData((ArrayList) roles);

            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity Create(String sessiontoken, String creator, String username, String firstname,
            String surname, String phone_number, String email_address, int roleid, String password) {
        logger.info("Create invoked with sessiontoken: " + sessiontoken
                + ", creator: " + creator
                + ", username: " + username
                + ", firstname: " + firstname
                + ", surname: " + surname
                + ", phone_number: " + phone_number
                + ", email_address: " + email_address
                + ", roleid: " + roleid);

        try {
            // Check if the user with the given email address already exists.
            boolean userExist = CheckExistingUser(email_address);
            if (userExist) {
                logger.info("User already exists with email: " + email_address);
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("success");
                networkResponse.setMessage("Email address already exists");
                return responseManager.ResponseOk(networkResponse);
            }

            if (!PlatformRole.isSystemUserRole(roleid)) {
                logger.info("Create rejected: roleid {} is not a system user role (1-3)", roleid);
                return responseManager.ResponseBadRequest("System users must have role 1-3");
            }

            // Generate a reference and retrieve the user role.
            String reference = randomizer.GenerateReference();
            int userrole = GetUserRole(creator, sessiontoken);
            logger.info("User role for creator " + creator + ": " + userrole);

            // Create the hash password.
            String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            // Use the hashed password as the code for now.
            String code = hashPassword;
            // Generate a random reference for the new user.
            String ref = randomizer.GenerateReference(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            int enabled = 1;
            String SQL;
            int retval = 0;

            switch (userrole) {
                case 1:
                    // Insert the new user into tbl_users.
                    SQL = "INSERT into sparkpayweb_db.tbl_users(username, password, date_created, enabled, reference) VALUES(?, ?, now(), ?, ?)";
                    logger.info("Executing SQL (tbl_users): " + SQL);
                    retval = jdbcTemplate.update(SQL, new Object[]{email_address, code, enabled, ref});
                    logger.info("tbl_users insert returned: " + retval);

                    if (retval > 0) {
                        // Insert user details into tbl_user_details.
                        SQL = "INSERT into tbl_user_details(username, firstname, surname, phone_number, email_address, role, date_created) VALUES(?, ?, ?, ?, ?, ?, now())";
                        logger.info("Executing SQL (tbl_user_details): " + SQL);
                        retval = jdbcTemplate.update(SQL, new Object[]{username, firstname, surname, phone_number, email_address, roleid});
                        logger.info("tbl_user_details insert returned: " + retval);

                        if (retval > 0) {
                            // Build and return a LoginResponse.
                            LoginResponse response = new LoginResponse();
                            response.setCode(201);
                            response.setStatus("success");
                            response.setMessage("Account created");
                            response.setFirstname(ref); // Example: Using generated ref
                            response.setSurname(code);   // Example: Using hashed password as a placeholder
                            logger.info("Account created successfully for user: " + email_address);
                            return responseManager.ResponseOk(response);
                        } else {
                            logger.info("Insert into tbl_user_details failed for user: " + email_address);
                            return responseManager.ResponseInternalServerError();
                        }
                    } else {
                        logger.info("Insert into tbl_users failed for user: " + email_address);
                        return responseManager.ResponseBadRequest();
                    }
                case 2:
                    // Check if user creation is already pending.
                    boolean userPending = CheckUserPending(username, email_address, "create");
                    if (userPending) {
                        logger.info("User pending creation already exists for username: " + username + " or email: " + email_address);
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Account with username - " + username + " or email - " + email_address + " is already pending for creation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    // Insert a record into tbl_user_details_operations for pending creation.
                    retval = insertPendingUserOperation(username, hashPassword, firstname, surname, phone_number, email_address, roleid, creator, "create", "Create user account");
                    if (retval > 0) {
                        logger.info("Pending account creation recorded for username: " + username);
                        return responseManager.ResponseAccepted();
                    } else {
                        logger.info("Insert into tbl_user_details_operations failed for username: " + username);
                        return responseManager.ResponseInternalServerError();
                    }
                case 3:
                    userPending = CheckUserPending(username, email_address, "create");
                    if (userPending) {
                        logger.info("User pending creation already exists for username: " + username + " or email: " + email_address);
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Account with username - " + username + " or email - " + email_address + " is already pending for creation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    retval = insertPendingUserOperation(username, hashPassword, firstname, surname, phone_number, email_address, roleid, creator, "create", "Create user account");
                    if (retval > 0) {
                        logger.info("Pending account creation recorded for username: " + username);
                        return responseManager.ResponseAccepted();
                    } else {
                        logger.info("Insert into tbl_user_details_operations failed for username: " + username);
                        return responseManager.ResponseInternalServerError();
                    }
                default:
                    logger.info("Unauthorized user role (" + userrole + ") for creator: " + creator);
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            logger.info("DataAccessException in Create method: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    public void MapUserToMerchants(String username, List<String> merchants) {
        for (int i = 0; i < merchants.size(); i++) {
            String SQL = "INSERT INTO sparkpayweb_db.tbl_map_user_to_merchants "
                    + "(username, merchant_id) "
                    + "VALUES(?, ?)";
            jdbcTemplate.update(SQL, new Object[]{username, merchants.get(i)});
        }
    }

    /** Role 4 is an institution contact — login lives on the contact row, not the card-user map. */
    private void mapOtherUserToInstitution(String email, String institutionid, String institutionname, int roleid) {
        if (roleid == PlatformRole.OTHER_MIN) {
            return;
        }
        if (roleid == 6 && institutionid != null && !institutionid.isEmpty()) {
            List<String> merchantIds = new ArrayList<>(Arrays.asList(institutionid.split(",")));
            logger.info("Mapping user to merchants: " + merchantIds);
            MapUserToMerchants(email, merchantIds);
        }
        String institutiontype = roleid == 5 ? "financial_institution_user"
                : roleid == 6 ? "merchant_user"
                        : roleid == 7 ? "terminal_owner_user"
                                : roleid == 8 ? "ptsp_user"
                                        : "";
        String SQL = "INSERT into tbl_map_card_users_institution(user_email, institution_id, institution_name, institution_type, date_created) VALUES(?, ?, ?, ?, now())";
        jdbcTemplate.update(SQL, new Object[]{email, institutionid, institutionname, institutiontype});
    }

    @Override
    public ResponseEntity CreateOther(String sessiontoken, String creator, String username,
            String firstname, String surname, String phone_number, String email_address,
            int roleid, String password, String institutionid, String institutionname) {
        try {
            logger.info("CreateOther invoked with: sessiontoken=" + sessiontoken
                    + ", creator=" + creator
                    + ", username=" + username
                    + ", firstname=" + firstname
                    + ", surname=" + surname
                    + ", phone_number=" + phone_number
                    + ", email_address=" + email_address
                    + ", roleid=" + roleid
                    + ", institutionid=" + institutionid
                    + ", institutionname=" + institutionname);

            // Check if the user already exists.
            boolean userExist = CheckExistingUser(email_address);
            if (userExist) {
                logger.info("User already exists with email: " + email_address);
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("success");
                networkResponse.setMessage("Email address already exists");
                return responseManager.ResponseOk(networkResponse);
            }

            if (!PlatformRole.isOtherUserRole(roleid)) {
                logger.info("CreateOther rejected: roleid {} is not an other-user role (4-8)", roleid);
                return responseManager.ResponseBadRequest("Other users must have role 4-8");
            }

            // Generate a reference (if needed) and retrieve the user role.
            String reference = randomizer.GenerateReference();
            int userrole = GetUserRole(creator, sessiontoken);
            logger.info("Retrieved user role (" + userrole + ") for creator: " + creator);

            // Create hashed password.
            String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            logger.info("Password hashed successfully.");

            String SQL;
            int retval = 0;
            switch (userrole) {
                case 1:
                    // In case 1, also generate additional random references.
                    String code = randomizer.GenerateReference(45, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890");
                    String ref = randomizer.GenerateReference(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
                    SQL = "INSERT into sparkpayweb_db.tbl_users(username, password, date_created, enabled, reference) VALUES(?, ?, now(), 1, ?)";
                    logger.info("Executing SQL (tbl_users): " + SQL);
                    retval = jdbcTemplate.update(SQL, new Object[]{email_address, hashPassword, ref});
                    logger.info("tbl_users insert result: " + retval);

                    if (retval > 0) {
                        SQL = "INSERT into tbl_user_details(username, firstname, surname, phone_number, email_address, role, date_created) VALUES(?, ?, ?, ?, ?, ?, now())";
                        logger.info("Executing SQL (tbl_user_details): " + SQL);
                        retval = jdbcTemplate.update(SQL, new Object[]{username, firstname, surname, phone_number, email_address, roleid});
                        logger.info("tbl_user_details insert result: " + retval);

                        if (retval > 0) {
                            mapOtherUserToInstitution(email_address, institutionid, institutionname, roleid);

                            LoginResponse response = new LoginResponse();
                            response.setCode(201);
                            response.setStatus("success");
                            response.setMessage("Account created");
                            response.setFirstname(ref);
                            response.setSurname(code);
                            logger.info("Account creation successful. Returning LoginResponse.");
                            return responseManager.ResponseOk(response);
                        } else {
                            logger.info("tbl_user_details insert failed for email: " + email_address);
                            return responseManager.ResponseInternalServerError();
                        }
                    } else {
                        logger.info("tbl_users insert failed for email: " + email_address);
                        return responseManager.ResponseBadRequest();
                    }
                case 2:
                    boolean userPending = CheckUserPending(username, email_address, "create");
                    if (userPending) {
                        logger.info("User pending creation already exists for username: " + username + " or email: " + email_address);
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Account with username - " + username + " or email - " + email_address + " is already pending for creation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    retval = insertPendingUserOperation(username, hashPassword, firstname, surname, phone_number, email_address, roleid, creator, "create", "Create user account");
                    logger.info("tbl_user_details_operations insert result: " + retval);

                    if (retval > 0) {
                        mapOtherUserToInstitution(email_address, institutionid, institutionname, roleid);
                        return responseManager.ResponseAccepted();
                    } else {
                        logger.info("Insert into tbl_user_details_operations failed for username: " + username);
                        return responseManager.ResponseInternalServerError();
                    }
                case 3: {
                    boolean approverPending = CheckUserPending(username, email_address, "create");
                    if (approverPending) {
                        logger.info("User pending creation already exists for username: " + username + " or email: " + email_address);
                        NetworkResponse pendingResp = new NetworkResponse();
                        pendingResp.setCode(200);
                        pendingResp.setStatus("failed");
                        pendingResp.setMessage("Account with username - " + username + " or email - " + email_address + " is already pending for creation");
                        return responseManager.ResponseOk(pendingResp);
                    }
                    retval = insertPendingUserOperation(username, hashPassword, firstname, surname, phone_number, email_address, roleid, creator, "create", "Create user account");
                    if (retval > 0) {
                        mapOtherUserToInstitution(email_address, institutionid, institutionname, roleid);
                        return responseManager.ResponseAccepted();
                    }
                    logger.info("Insert into tbl_user_details_operations failed for username: " + username);
                    return responseManager.ResponseInternalServerError();
                }
                default:
                    logger.info("Unauthorized user role: " + userrole + " for creator " + creator);
                    return responseManager.ResponseUnathorized();
            }
        } catch (DataAccessException ex) {
            logger.info("DataAccessException in CreateOther: " + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity Delete(String sessiontoken, int userid, String username) {
        try {
            NetworkResponse response = new NetworkResponse();
            String SQL;
            ResponseEntity responseEntity = GetUserById(sessiontoken, userid);
            LoginResponse loginResponse = responseEntity != null ? (LoginResponse) responseEntity.getBody() : new LoginResponse();
            if (loginResponse.getRoleid() == 1) {
                response.setCode(200);
                response.setStatus("failed");
                response.setMessage("Can not delete an administrator account");
                return responseManager.ResponseOk(response);
            }
            // Authorize the caller from session — path {username} is the target being deleted.
            int userrole = GetActorRole(sessiontoken);
            logger.info("Delete user id={} target={} actorRole={}", userid, username, userrole);
            int retVal;
            switch (userrole) {
                case PlatformRole.ADMIN:
                    SQL = "UPDATE tbl_user_details SET deleted = 1 WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{userid});
                    if (retVal > 0) {
                        return responseManager.ResponseDeleted();
                    } else {
                        return responseManager.ResponseInternalServerError();
                    }
                case PlatformRole.OPERATOR:
                    boolean userPending = CheckUserPending(loginResponse.getUsername(), loginResponse.getEmail_address(), "delete");
                    if (userPending) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Account already pending for delete");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "INSERT INTO tbl_user_details_operations(id, username, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, ?, 'delete', 'Delete user account', now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{loginResponse.getId(), loginResponse.getUsername(), loginResponse.getFirstname(), loginResponse.getSurname(), loginResponse.getPhone_number(), loginResponse.getEmail_address(), loginResponse.getRoleid()});
                    if (retVal > 0) {
                        return responseManager.ResponseDeleted();
                    } else {
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
    public ResponseEntity UpdateNames(String sessiontoken, String firstname, String surname, String phone_number, String username) {
        try {
            String SQL;
            int retVal;
            SQL = "UPDATE tbl_user_details SET firstname = ?, surname = ?, phone_number = ? WHERE session_token = ?";
            retVal = jdbcTemplate.update(SQL, new Object[]{firstname, surname, phone_number, sessiontoken});
            if (retVal > 0) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("success");
                networkResponse.setMessage("Profile Updated");
                return responseManager.ResponseOk(networkResponse);
            } else {
                return responseManager.ResponseInternalServerError();
            }
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity UpdatePassword(String sessiontoken, String password, String session_token, String username) {
        LoginResponse response = new LoginResponse();
        try {
            String SQL;
            SQL = "SELECT a.password from sparkpayweb_db.tbl_users a "
                    + "WHERE a.username = ? AND a.enabled = 1";
            String security = jdbcTemplate.queryForObject(SQL, new Object[]{username}, String.class);
            boolean comparePassword = BCrypt.checkpw(session_token, security);
            if (comparePassword) {
                String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                SQL = "UPDATE sparkpayweb_db.tbl_users SET password = ? WHERE username = ?";
                jdbcTemplate.update(SQL, new Object[]{hashPassword, username});
                response.setCode(200);
                response.setStatus("success");
                response.setMessage("Password updated successfully");
                SQL = "UPDATE tbl_user_details SET date_updated = now() WHERE username = ? AND session_token = ?";
                jdbcTemplate.update(SQL, new Object[]{username, sessiontoken});
                return responseManager.ResponseOk(response);
            } else {
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Current password is incorrect");
                return responseManager.ResponseOk(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Unable to handle password reset");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseUnathorized();
        }
    }

    @Override
    public ResponseEntity Edit(String sessiontoken, int userid, String firstname, String surname, String phone_number, int roleid, String username, String email_address) {
        try {
            String SQL;
            // Authorize the caller from session — body.username is the target being edited.
            int userrole = GetActorRole(sessiontoken);
            logger.info("Edit user id={} targetUsername={} email={} actorRole={}", userid, username, email_address, userrole);

            ResponseEntity existingEntity = GetUserById(sessiontoken, userid);
            LoginResponse existing = existingEntity != null && existingEntity.getBody() instanceof LoginResponse
                    ? (LoginResponse) existingEntity.getBody()
                    : new LoginResponse();
            if (existing.getId() == 0) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("User not found");
                return responseManager.ResponseOk(networkResponse);
            }

            String newUsername = (username != null && !username.isBlank()) ? username.trim() : existing.getUsername();
            String newFirstname = (firstname != null && !firstname.isBlank()) ? firstname.trim() : existing.getFirstname();
            String newSurname = (surname != null && !surname.isBlank()) ? surname.trim() : existing.getSurname();
            String newPhone = phone_number != null ? phone_number.trim() : existing.getPhone_number();
            String newEmail = (email_address != null && !email_address.isBlank())
                    ? email_address.trim().toLowerCase()
                    : existing.getEmail_address();
            String oldEmail = existing.getEmail_address() != null ? existing.getEmail_address().trim().toLowerCase() : "";

            if (!newEmail.equalsIgnoreCase(oldEmail) && CheckExistingUser(newEmail)) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("failed");
                networkResponse.setMessage("Email already in use");
                return responseManager.ResponseOk(networkResponse);
            }

            int retVal;
            switch (userrole) {
                case PlatformRole.ADMIN:
                    // Administrator: apply changes immediately (including email used for login).
                    SQL = "UPDATE tbl_user_details SET username = ?, firstname = ?, surname = ?, phone_number = ?, email_address = ?, role = ?, date_updated = now() WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{newUsername, newFirstname, newSurname, newPhone, newEmail, roleid, userid});
                    if (retVal > 0) {
                        if (!newEmail.equalsIgnoreCase(oldEmail) && !oldEmail.isEmpty()) {
                            // Login joins sparkpayweb_db.tbl_users.username to tbl_user_details.email_address.
                            jdbcTemplate.update(
                                    "UPDATE sparkpayweb_db.tbl_users SET username = ? WHERE username = ?",
                                    new Object[]{newEmail, oldEmail});
                        }
                        return responseManager.ResponseAccepted();
                    } else {
                        return responseManager.ResponseInternalServerError();
                    }
                case PlatformRole.OPERATOR:
                    // Operator: queue edit for Approver/Admin approval.
                    boolean userPending = CheckUserPending(existing.getUsername(), existing.getEmail_address(), "edit");
                    if (userPending) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Account already pending for edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    String note = "";
                    if (!safeEq(existing.getUsername(), newUsername)) {
                        note = appendNote(note, "Change username from " + existing.getUsername() + " to " + newUsername);
                    }
                    if (!safeEq(existing.getFirstname(), newFirstname)) {
                        note = appendNote(note, "Change firstname from " + existing.getFirstname() + " to " + newFirstname);
                    }
                    if (!safeEq(existing.getSurname(), newSurname)) {
                        note = appendNote(note, "Change surname from " + existing.getSurname() + " to " + newSurname);
                    }
                    if (!safeEq(existing.getPhone_number(), newPhone)) {
                        note = appendNote(note, "Change phone number from " + existing.getPhone_number() + " to " + newPhone);
                    }
                    if (!newEmail.equalsIgnoreCase(oldEmail)) {
                        note = appendNote(note, "Change email from " + oldEmail + " to " + newEmail);
                    }
                    if (existing.getRoleid() != roleid) {
                        note = appendNote(note, "Change role from " + existing.getRoleid() + " to " + roleid);
                    }
                    // Store target user id so approval can update by id even when email changes.
                    SQL = "INSERT INTO tbl_user_details_operations(id, username, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, ?, 'edit', ?, now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{userid, newUsername, newFirstname, newSurname, newPhone, newEmail, roleid, note});
                    if (retVal > 0) {
                        return responseManager.ResponseAccepted();
                    } else {
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

    private static boolean safeEq(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static String appendNote(String note, String part) {
        if (part == null || part.isBlank()) return note == null ? "" : note;
        if (note == null || note.isBlank()) return part;
        return note + ", " + part;
    }

    @Override
    public ResponseEntity UserApprovals(String sessiontoken, int id, String actionType, String username, boolean isContact, String financialInstitutionCode) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            int retVal2;
            if (userrole == 1 || userrole == 3) {
                List<UserModel> users = GetUserFromPendings(id, actionType);
                if (users.size() == 1) {
                    switch (actionType) {
                        case "delete":
                            SQL = "DELETE FROM tbl_user_details_operations WHERE id = ? AND actionType = 'delete'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "UPDATE tbl_user_details SET deleted = 1 WHERE id = ?";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{users.get(0).getId()});
                            if (isContact) {
                                SQL = "DELETE FROM tbl_financial_institution_contacts_operations WHERE email_address = ? AND actionType = 'delete'";
                                jdbcTemplate.update(SQL, new Object[]{users.get(0).getEmail_address()});
                            }
                            if (retVal > 0 && retVal2 > 0) {
                                return responseManager.ResponseDeleted();
                            } else {
                                return responseManager.ResponseInternalServerError();
                            }
                        case "edit":
                            SQL = "DELETE FROM tbl_user_details_operations WHERE id = ? AND actionType = 'edit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            UserModel pendingEdit = users.get(0);
                            String previousEmail = null;
                            try {
                                previousEmail = jdbcTemplate.queryForObject(
                                        "SELECT email_address FROM tbl_user_details WHERE id = ? AND deleted = 0",
                                        new Object[]{pendingEdit.getId()},
                                        String.class);
                            } catch (DataAccessException ignore) {
                                previousEmail = null;
                            }
                            SQL = "UPDATE tbl_user_details SET username = ?, firstname = ?, surname = ?, phone_number = ?, email_address = ?, role = ?, date_updated = now() WHERE id = ?";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{
                                pendingEdit.getUsername(),
                                pendingEdit.getFirstname(),
                                pendingEdit.getSurname(),
                                pendingEdit.getPhone_number(),
                                pendingEdit.getEmail_address(),
                                pendingEdit.getRoleid(),
                                pendingEdit.getId()
                            });
                            if (retVal2 > 0
                                    && previousEmail != null
                                    && pendingEdit.getEmail_address() != null
                                    && !previousEmail.equalsIgnoreCase(pendingEdit.getEmail_address())) {
                                jdbcTemplate.update(
                                        "UPDATE sparkpayweb_db.tbl_users SET username = ? WHERE username = ?",
                                        new Object[]{pendingEdit.getEmail_address().trim().toLowerCase(), previousEmail.trim().toLowerCase()});
                            }
                            if (isContact) {
                                SQL = "DELETE FROM tbl_financial_institution_contacts_operations WHERE email_address = ? AND actionType = 'edit'";
                                jdbcTemplate.update(SQL, new Object[]{pendingEdit.getEmail_address()});
                                SQL = "UPDATE tbl_financial_institution_contacts SET firstname = ?, surname = ?, phone_number = ?"
                                        + (previousEmail != null ? ", email_address = ?" : "")
                                        + " WHERE email_address = ?";
                                if (previousEmail != null) {
                                    jdbcTemplate.update(SQL, new Object[]{
                                        pendingEdit.getFirstname(),
                                        pendingEdit.getSurname(),
                                        pendingEdit.getPhone_number(),
                                        pendingEdit.getEmail_address(),
                                        previousEmail
                                    });
                                } else {
                                    jdbcTemplate.update(
                                            "UPDATE tbl_financial_institution_contacts SET firstname = ?, surname = ?, phone_number = ? WHERE email_address = ?",
                                            new Object[]{
                                                pendingEdit.getFirstname(),
                                                pendingEdit.getSurname(),
                                                pendingEdit.getPhone_number(),
                                                pendingEdit.getEmail_address()
                                            });
                                }
                            }
                            return responseManager.ResponseAccepted();
                        case "create":
                            UserModel pendingUser = users.get(0);
                            if (isContact || pendingUser.getRoleid() == 4) {
                                String institutionForContact = financialInstitutionCode;
                                if (institutionForContact == null || institutionForContact.isEmpty()) {
                                    institutionForContact = pendingUser.getInstitution();
                                }
                                ApprovePendingFinancialInstitutionContact(pendingUser, institutionForContact);
                            }
                            SQL = "DELETE FROM tbl_user_details_operations WHERE id = ? AND actionType = 'create'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            String reference = randomizer.GenerateReference();
                            String code = randomizer.GenerateReference(45, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890");
                            String ref = randomizer.GenerateReference(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
                            SQL = "INSERT into sparkpayweb_db.tbl_users(username, password, date_created, enabled, reference) VALUES(?, ?, now(), 1, ?)";
                            jdbcTemplate.update(SQL, new Object[]{pendingUser.getEmail_address(), pendingUser.getSecurity(), ref});
                            SQL = "INSERT into tbl_user_details(username, firstname, surname, phone_number, email_address, role, date_created) VALUES(?, ?, ?, ?, ?, ?, now())";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{pendingUser.getUsername(), pendingUser.getFirstname(), pendingUser.getSurname(), pendingUser.getPhone_number(), pendingUser.getEmail_address(), pendingUser.getRoleid()});
                            if (retVal > 0 && retVal2 > 0) {
                                LoginResponse response = new LoginResponse();
                                response.setCode(201);
                                response.setStatus("success");
                                response.setMessage("Account approved");
                                response.setFirstname(ref);
                                response.setSurname(code);
                                response.setUsername(pendingUser.getEmail_address());
                                return responseManager.ResponseOk(response);
                            } //                                return responseManager.ResponseAccepted();
                            else {
                                return responseManager.ResponseInternalServerError();
                            }
                        default:
                            return responseManager.ResponseBadRequest();
                    }
                } else {
                    NetworkResponse networkResponse = new NetworkResponse();
                    networkResponse.setCode(200);
                    networkResponse.setStatus("failed");
                    networkResponse.setMessage("Not found");
                    return responseManager.ResponseNotFound(networkResponse);
                }
            } else {
                return responseManager.ResponseUnathorized();
            }
        } catch (IllegalStateException ex) {
            logger.error("User approval failed: {}", ex.getMessage());
            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(500);
            networkResponse.setStatus("failed");
            networkResponse.setMessage(ex.getMessage());
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetUsersForActions(boolean systemUsers) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            if (systemUsers) {
                SQL = "SELECT a.id, a.username, a.password, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.actionType, a.note, a.date_created, "
                        + "b.role_name, "
                        + "COALESCE(c.financial_institution_code, cop.financial_institution_code, d.institution_id) as financial_institution_code, "
                        + "COALESCE(fi.name, d.institution_name) as institution_name "
                        + "from tbl_user_details_operations a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "LEFT JOIN tbl_financial_institution_contacts c "
                        + "ON a.email_address = c.email_address "
                        + "LEFT JOIN tbl_financial_institution_contacts_operations cop "
                        + "ON cop.email_address = a.email_address AND cop.actionType = a.actionType "
                        + "LEFT JOIN tbl_map_card_users_institution d "
                        + "ON a.email_address = d.user_email "
                        + "LEFT JOIN tbl_financial_institutions fi "
                        + "ON fi.code = COALESCE(c.financial_institution_code, cop.financial_institution_code, d.institution_id) "
                        + "WHERE a.role BETWEEN " + PlatformRole.ADMIN + " AND " + PlatformRole.APPROVER + " "
                        + "ORDER BY a.id DESC";
            } else {
                SQL = "SELECT a.id, a.username, a.password, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.actionType, a.note, a.date_created, "
                        + "b.role_name, "
                        + "c.institution_id as financial_institution_code, c.institution_name as institution_name  "
                        + "from tbl_user_details_operations a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "LEFT JOIN tbl_map_card_users_institution c "
                        + "ON a.email_address = c.user_email "
                        + "WHERE a.role BETWEEN " + PlatformRole.OTHER_MIN + " AND " + PlatformRole.OTHER_MAX + " "
                        + "ORDER BY a.id DESC";
            }
            List<UserModel> users = jdbcTemplate.query(SQL, new UserMapper2());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(systemUsers ? "Pending Users" : "Pending Other Users");
            networkResponse.setData((ArrayList) users);
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    @Override
    public ResponseEntity GetContactsForActions() {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            SQL = "SELECT a.id, a.username, a.password, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.actionType, a.note, a.date_created, "
                    + "b.role_name, "
                    + "COALESCE(c.financial_institution_code, cop.financial_institution_code) as financial_institution_code, "
                    + "d.name as institution_name  "
                    + "from tbl_user_details_operations a "
                    + "LEFT JOIN tbl_role b "
                    + "ON a.role = b.id "
                    + "LEFT JOIN tbl_financial_institution_contacts c "
                    + "ON a.email_address = c.email_address "
                    + "LEFT JOIN tbl_financial_institution_contacts_operations cop "
                    + "ON cop.email_address = a.email_address AND cop.actionType = a.actionType "
                    + "LEFT JOIN tbl_financial_institutions d "
                    + "ON d.code = COALESCE(c.financial_institution_code, cop.financial_institution_code) "
                    + "WHERE a.role = 4 "
                    + "ORDER BY a.id DESC";
            List<UserModel> users = jdbcTemplate.query(SQL, new UserMapper2());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Pending Institution Contacts");
            networkResponse.setData((ArrayList) users);
            return responseManager.ResponseOk(networkResponse);

        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    class UserMapper implements RowMapper<UserModel> {

        @Override
        public UserModel mapRow(ResultSet rs, int arg1) throws SQLException {
            UserModel response = new UserModel();

            response.setId(rs.getInt("id"));
            response.setUsername(rs.getString("username"));
            response.setEmail_address(rs.getString("email_address"));
            response.setFirstname(rs.getString("firstname"));
            response.setSurname(rs.getString("surname"));
            response.setPhone_number(rs.getString("phone_number"));
            response.setRole(rs.getString("role_name"));
            response.setRoleid(rs.getInt("role"));
            response.setDate_created(rs.getString("date_created"));
//            response.setSession_token(rs.getString("session_token") != null ? rs.getString("session_token") : "");
            response.setLast_login(rs.getString("last_login"));
            response.setDate_updated(rs.getString("date_updated"));
            response.setInstitution(rs.getString("financial_institution_code") != null ? rs.getString("financial_institution_code") : "-1");
            response.setInstitutionName(rs.getString("institution_name") != null ? rs.getString("institution_name") : "");
            return response;
        }
    }

    class UserMapper2 implements RowMapper<UserModel> {

        @Override
        public UserModel mapRow(ResultSet rs, int arg1) throws SQLException {
            UserModel response = new UserModel();

            response.setId(rs.getInt("id"));
            response.setUsername(rs.getString("username"));
            response.setEmail_address(rs.getString("email_address"));
            response.setFirstname(rs.getString("firstname"));
            response.setSurname(rs.getString("surname"));
            response.setPhone_number(rs.getString("phone_number"));
            response.setRole(rs.getString("role_name"));
            response.setRoleid(rs.getInt("role"));
            response.setDate_created(rs.getString("date_created"));
            response.setDate_updated("");
            response.setNote(rs.getString("note"));
            response.setActionType(rs.getString("actionType"));
            response.setSecurity(rs.getString("password"));
            String institutionCode = hasColumn(rs, "financial_institution_code") ? rs.getString("financial_institution_code") : "";
            String institutionName = hasColumn(rs, "institution_name") ? rs.getString("institution_name") : "";
            if (institutionCode == null) {
                institutionCode = "";
            }
            if (institutionName == null) {
                institutionName = "";
            }
            response.setInstitution(institutionCode);
            response.setInstitutionName(institutionName);
            response.setFinancial_institution_code(institutionCode);
            response.setFinancial_institution_name(institutionName);
            mapOperationSubmitter(response, rs);
            return response;
        }
    }

    private static void mapOperationSubmitter(UserModel response, ResultSet rs) throws SQLException {
        if (!hasColumn(rs, "created_by")) {
            return;
        }
        String submitter = rs.getString("created_by");
        if (submitter != null && !submitter.isEmpty()) {
            response.setCreator(submitter);
            response.setCreated_by(submitter);
        }
    }

    class MenuMapper implements RowMapper<MenuModel> {

        @Override
        public MenuModel mapRow(ResultSet rs, int arg1) throws SQLException {
            MenuModel response = new MenuModel();

            response.setId(rs.getInt("id"));
            response.setIcon(rs.getString("icon"));
            response.setLabel(rs.getString("label"));
            response.setParent_id(rs.getInt("parent_id"));
            response.setPath(rs.getString("path"));
            response.setRole_id(rs.getInt("role_id"));
            response.setChild_id(rs.getInt("child_id"));
            response.setChild_label(rs.getString("child_label"));
            response.setChild_path(rs.getString("child_path"));
            return response;
        }
    }

    class RoleMapper implements RowMapper<RoleModel> {

        @Override
        public RoleModel mapRow(ResultSet rs, int arg1) throws SQLException {
            RoleModel response = new RoleModel();

            response.setId(rs.getInt("id"));
            response.setRole_name(rs.getString("role_name"));
            response.setDate_created(rs.getString("date_created"));
            return response;
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

}
