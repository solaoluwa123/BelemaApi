/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.util.ResponseManager;
import com.transgate.api.interfaces.UsersInterface;
import com.transgate.api.models.LoginResponse;
import com.transgate.api.models.MenuModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.RoleModel;
import com.transgate.api.models.UserModel;
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
import org.springframework.beans.factory.annotation.Autowired;
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
    JdbcTemplate jdbcTemplate;

    ResponseManager responseManager = new ResponseManager();
    Randomizer randomizer = new Randomizer();
    
    private final Validators validators;

    // Constructor injection for RestCall
    public UsersService(Validators validators) {
        this.validators = validators;
    }
    
    private int GetUserRole(String username, String session_token) {
        try {
            int role;

            String SQL = "SELECT role FROM tbl_user_details WHERE email_address = ? OR username = ? AND deleted = 0 AND session_token = ?";
            role = jdbcTemplate.queryForObject(SQL, new Object[]{username, username, session_token}, int.class);
            return role;
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return -100;
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
            SQL = "SELECT a.id, a.username, a.password, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.actionType, a.note, a.date_created, b.role_name "
                    + "from tbl_user_details_operations a "
                    + "LEFT JOIN tbl_role b "
                    + "ON a.role = b.id "
                    + "WHERE a.id = ? AND a.actionType = ?"
                    + "ORDER BY a.id DESC";
            List<UserModel> users = jdbcTemplate.query(SQL, new Object[]{id, actionType}, new UserMapper2());
            return users;
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
            return null;
        }
    }
    
    @Override
    public ResponseEntity ClearUserSession(String sessiontoken, String username) {
        try {
            String SQL = "UPDATE tbl_user_details SET session_token = '' WHERE session_token = ? AND username = ?";
            jdbcTemplate.update(SQL, new Object[]{sessiontoken, username});
            return responseManager.ResponseAccepted();
        } catch (DataAccessException ex) {
            System.out.println(ex);
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
                response.setTwofasecretkey(secret);
                String sessiontoken = randomizer.GenerateToken(100);
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
                }
                else {
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
                if (userRoleid < 5){
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = 0 AND a.access = 1 OR a.parent_id IS NULL AND a.role_id = ? AND a.access = 1 "
                            + "ORDER BY a.id ASC";
                    menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());

                    if (menu.size() > 0) {
                        response.setTransgateMenu(menu);
                    }
                    else {
                        List<MenuModel> eM = new ArrayList<>();
                        response.setTransgateMenu(eM);
                    }
                }
                else if (userRoleid == 8) {
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = ? AND a.access = 1 "
                            + "ORDER BY a.id ASC";
                    menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());

                    if (menu.size() > 0) {
                        response.setTransgateMenu(menu);
                    }
                    else {
                        List<MenuModel> eM = new ArrayList<>();
                        response.setTransgateMenu(eM);
                    }
                }
                else
                    response.setTransgateMenu(new ArrayList<>());
                if (userRoleid < 4) 
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = 0 AND a.access = 2 OR a.parent_id IS NULL AND a.role_id = ? AND a.access = 2 "
                            + "ORDER BY a.id ASC";
                else
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = ? AND a.access = 2 "
                            + "ORDER BY a.id ASC";
                menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());
                
                if (menu.size() > 0) {
                    response.setSparkpayMenu(menu);
                }
                else {
                    List<MenuModel> eM = new ArrayList<>();
                    response.setSparkpayMenu(eM);
                }
                return responseManager.ResponseOk(response);
            }
            else {
                SQL = "UPDATE tbl_user_details SET attempts_left = attempts_left - 1 WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{username});
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Invalid 2FA");
                return responseManager.ResponseOk(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid username or password");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
            return responseManager.ResponseUnathorized();
        }
    }
    
    @Override
    public boolean LoginExternal(String username, String password) {
        LoginResponse response = new LoginResponse();
        try {
            String SQL;
            SQL = "SELECT attempts_left FROM tbl_user_details WHERE email_address = ?";
            int attemptsLeft = jdbcTemplate.queryForObject(SQL, new Object[]{username}, int.class);
            if (attemptsLeft == 0) {
                SQL = "UPDATE tbl_user_details SET unlock_account_in = 15 WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{username});
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Account locked for invalid multiple attempts, try again in 15 minutes");
                return false;
            }
            SQL = "SELECT a.password, a.two_fa_enabled, a.two_fa_secret from sparkpayweb_db.tbl_users a "
                    + "LEFT JOIN tbl_user_details b "
                    + "ON a.username = b.email_address "
                    + "WHERE a.username = ? AND a.enabled = 1 AND b.deleted = 0";
            String security = "";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(SQL, new Object[]{username});
            if (users.size() == 1) {
                security = (String) users.get(0).get("password");
            } else {
                return false;
            }
            boolean comparePassword = BCrypt.checkpw(password, security);
            return comparePassword;
        } catch (DataAccessException ex) {
            return false;
        }
    }
    
    @Override
    public ResponseEntity Login(String username, String password) {
        LoginResponse response = new LoginResponse();
        try {
            String SQL;
            SQL = "SELECT attempts_left FROM tbl_user_details WHERE email_address = ?";
            int attemptsLeft = jdbcTemplate.queryForObject(SQL, new Object[]{username}, int.class);
            if (attemptsLeft == 0) {
                SQL = "UPDATE tbl_user_details SET unlock_account_in = 15 WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{username});
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Account locked for invalid multiple attempts, try again in 15 minutes");
                return responseManager.ResponseOk(response);
            }
            SQL = "SELECT a.password, a.two_fa_enabled, a.two_fa_secret from sparkpayweb_db.tbl_users a "
                    + "LEFT JOIN tbl_user_details b "
                    + "ON a.username = b.email_address "
                    + "WHERE a.username = ? AND a.enabled = 1 AND b.deleted = 0";
//            String security = jdbcTemplate.queryForObject(SQL, new Object[]{username}, String.class);
            String security = "", two_fa_secret = "";
            boolean comparePassword = false;
            int two_fa_enabled = 0;
            List<Map<String, Object>> users = jdbcTemplate.queryForList(SQL, new Object[]{username});
            if (users.size() > 0) {
                security = (String) users.get(0).get("password");
                two_fa_secret = (String) users.get(0).get("two_fa_secret");
                two_fa_enabled = (int) users.get(0).get("two_fa_enabled");
                response.setTwofaenabled(two_fa_enabled);
//                response.setTwofasecretkey(two_fa_secret);
            }
            if (password.length() > 0)
                comparePassword = BCrypt.checkpw(password, security);
            Date date = new Date();
            long time = date.getTime();
            Date expirationDate = new Date(time + (1000 * 60 * 120)); //120mins
            String sessiontoken = validators.GenerateJSONWebToken(username, expirationDate);
            response.setSession_token(sessiontoken);
            response.setUsername(username);
//                String sessiontoken = randomizer.GenerateToken(100);
            if (comparePassword == true) {
                SQL = "UPDATE tbl_user_details SET attempts_left = 3, last_login = now(), session_token = ? WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{sessiontoken, username});
                if (two_fa_enabled == 1) {
                    response.setCode(200);
                    response.setStatus("success");
                    response.setMessage("Login successful");
                    return responseManager.ResponseOk(response);
                }
//                String token = randomizer.GenerateToken();
//                SQL = "INSERT into tbl_user_token(username, token, date_created) VALUES(?, ?, now())";
//                jdbcTemplate.update(SQL, new Object[]{username, token});
//                response.setToken(token);
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
                    response.setDate_updated(details.get(0).getDate_updated());
                }
                else {
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
                if (userRoleid < 5){
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = 0 AND a.access = 1 OR a.parent_id IS NULL AND a.role_id = ? AND a.access = 1 "
                            + "ORDER BY a.id ASC";
                    menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());

                    if (menu.size() > 0) {
                        response.setTransgateMenu(menu);
                    }
                    else {
                        List<MenuModel> eM = new ArrayList<>();
                        response.setTransgateMenu(eM);
                    }
                }
                else if (userRoleid == 8 || userRoleid == 5) {
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = ? AND a.access = 1 "
                            + "ORDER BY a.id ASC";
                    menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());

                    if (menu.size() > 0) {
                        response.setTransgateMenu(menu);
                    }
                    else {
                        List<MenuModel> eM = new ArrayList<>();
                        response.setTransgateMenu(eM);
                    }
                }
                else
                    response.setTransgateMenu(new ArrayList<>());
                if (userRoleid < 4) 
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = 0 AND a.access = 2 OR a.parent_id IS NULL AND a.role_id = ? AND a.access = 2 "
                            + "ORDER BY a.id ASC";
                else
                    SQL = "SELECT a.id, a.role_id, a.label, a.icon, a.path, b.id as child_id, b.label as child_label, b.path as child_path, b.parent_id "
                            + "FROM `tbl_menus` a "
                            + "LEFT JOIN `tbl_menus` b "
                            + "ON a.id = b.parent_id "
                            + "WHERE a.parent_id IS NULL AND a.role_id = ? AND a.access = 2 "
                            + "ORDER BY a.id ASC";
                menu = jdbcTemplate.query(SQL, new Object[]{userRoleid}, new MenuMapper());
                
                if (menu.size() > 0) {
                    response.setSparkpayMenu(menu);
                }
                else {
                    List<MenuModel> eM = new ArrayList<>();
                    response.setSparkpayMenu(eM);
                }
                return responseManager.ResponseOk(response);
            }
            else {
                SQL = "UPDATE tbl_user_details SET attempts_left = attempts_left - 1 WHERE email_address = ?";
                jdbcTemplate.update(SQL, new Object[]{username});
                response.setCode(404);
                response.setStatus("failed");
                response.setMessage("Invalid username or password");
                return responseManager.ResponseOk(response);
            }
        } catch (DataAccessException ex) {
            System.out.println(ex);
            if ("Incorrect result size: expected 1, actual 0".equals(ex.getMessage())) {
                response.setCode(400);
                response.setStatus("failed");
                response.setMessage("Invalid username or password");
                return responseManager.ResponseOk(response);
            }
            System.out.println("error>>>>" + ex.getMessage());
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
                    String meta = "{\"qrCodeUri\": " + "\"" + qrCodeUri + "\"" +"}";
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
            }
            else {
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
            }
            else {
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
            }
            else {
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
            if (systemUsers)
                SQL = "SELECT a.id, a.username, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.date_created, a.date_updated, a.last_login, b.role_name, c.financial_institution_code, d.name as institution_name  "
                        + "from tbl_user_details a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "LEFT JOIN tbl_financial_institution_contacts c "
                        + "ON a.email_address = c.email_address "
                        + "LEFT JOIN tbl_financial_institutions d "
                        + "ON c.financial_institution_code = d.code "
                        + "WHERE a.deleted = 0 AND role < 4 "
                        + "ORDER BY a.id DESC";
            else
                SQL = "SELECT a.id, a.username, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.date_created, a.date_updated, a.last_login, "
                        + "b.role_name, "
                        + "c.institution_id as financial_institution_code, c.institution_name as institution_name  "
                        + "from tbl_user_details a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "LEFT JOIN tbl_map_card_users_institution c "
                        + "ON a.email_address = c.user_email "
                        + "WHERE a.deleted = 0 AND role > 4 "
                        + "ORDER BY a.id DESC";
            List<UserModel> users = jdbcTemplate.query(SQL, new UserMapper());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(systemUsers ? "System USers" : "Other Users");
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
    public ResponseEntity Create(String sessiontoken, String creator, String username, String firstname, String surname, String phone_number, String email_address, int roleid, String password){
        try {
            boolean userExist = CheckExistingUser(email_address);
            if (userExist) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("success");
                networkResponse.setMessage("Email address already exit");
                return responseManager.ResponseOk(networkResponse);
            }
            String reference = randomizer.GenerateReference();
            String SQL;
            int userrole = GetUserRole(creator, sessiontoken);
            String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            
//            String code = roleid == 4 ? hashPassword : randomizer.GenerateReference(45, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890");
            String code = hashPassword;
            String ref = randomizer.GenerateReference(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            int enabled = 1;
            switch (userrole) {
                case 1:
//                    SQL = "UPDATE sparkpayweb_db.tbl_users SET reference = ?, password = ? WHERE username = ?";
//                    jdbcTemplate.update(SQL, new Object[]{ref, code, email});
                    SQL = "INSERT into sparkpayweb_db.tbl_users(username, password, date_created, enabled, reference) VALUES(?, ?, now(), ?, ?)";
                    int retval = jdbcTemplate.update(SQL, new Object[]{email_address, code, enabled, ref});
                    if (retval > 0) {
                        SQL = "INSERT into tbl_user_details(username, firstname, surname, phone_number, email_address, role, date_created) VALUES(?, ?, ?, ?, ?, ?, now())";
                        retval = jdbcTemplate.update(SQL, new Object[]{username, firstname, surname, phone_number, email_address, roleid});
                        if (retval > 0){
                            LoginResponse response = new LoginResponse();
                            response.setCode(201);
                            response.setStatus("success");
                            response.setMessage("Account created");
                            response.setFirstname(ref);
                            response.setSurname(code);
                            return responseManager.ResponseOk(response);
                        }
//                            return responseManager.ResponseAccepted();
                        else
                            return responseManager.ResponseInternalServerError();
                    }
                    else 
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean userPending = CheckUserPending(username, email_address, "create");
                    if (userPending) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Account with username - " + username + " or email - " + email_address + " is already pending for creation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "INSERT INTO tbl_user_details_operations(username, password, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, ?, 'create', 'Create user account', now())";
                    retval = jdbcTemplate.update(SQL, new Object[]{username, hashPassword, firstname, surname, phone_number, email_address, roleid});
                    if (retval > 0) 
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
    
    public void MapUserToMerchants(String username, List<String> merchants) {
        for (int i = 0; i < merchants.size(); i++) {
            String SQL = "INSERT INTO sparkpayweb_db.tbl_map_user_to_merchants "
                    + "(username, merchant_id) "
                    + "VALUES(?, ?)";
            jdbcTemplate.update(SQL, new Object[]{username, merchants.get(i)});
        }
    }
    
    @Override
    public ResponseEntity CreateOther(String sessiontoken, String creator, String username, String firstname, String surname, String phone_number, String email_address, int roleid, String password, String institutionid, String institutionname){
        try {
            boolean userExist = CheckExistingUser(email_address);
            if (userExist) {
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("success");
                networkResponse.setMessage("Email address already exit");
                return responseManager.ResponseOk(networkResponse);
            }
            String reference = randomizer.GenerateReference();
            String SQL;
            int userrole = GetUserRole(creator, sessiontoken);
            String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            switch (userrole) {
                case 1:
                    String code = randomizer.GenerateReference(45, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890");
                    String ref = randomizer.GenerateReference(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
                    SQL = "INSERT into sparkpayweb_db.tbl_users(username, password, date_created, enabled, reference) VALUES(?, ?, now(), 1, ?)";
                    int retval = jdbcTemplate.update(SQL, new Object[]{email_address, hashPassword, ref});
                    if (retval > 0) {
                        SQL = "INSERT into tbl_user_details(username, firstname, surname, phone_number, email_address, role, date_created) VALUES(?, ?, ?, ?, ?, ?, now())";
                        retval = jdbcTemplate.update(SQL, new Object[]{username, firstname, surname, phone_number, email_address, roleid});
                        if (retval > 0){
                            if (roleid == 6) {
                                List<String> merchantIds = new ArrayList<>(Arrays.asList(institutionid.split(",")));
                                MapUserToMerchants(email_address, merchantIds);    
                            }
                            String institutiontype = roleid == 5 ? "financial_institution_user" : roleid == 6 ? "merchant_user" : roleid == 7 ? "terminal_owner_user" : roleid == 8 ? "ptsp_user" : "";
                            SQL = "INSERT into tbl_map_card_users_institution(user_email, institution_id, institution_name, institution_type, date_created) VALUES(?, ?, ?, ?, now())";
                            jdbcTemplate.update(SQL, new Object[]{email_address, institutionid, institutionname, institutiontype});
                            LoginResponse response = new LoginResponse();
                            response.setCode(201);
                            response.setStatus("success");
                            response.setMessage("Account created");
                            response.setFirstname(ref);
                            response.setSurname(code);
                            return responseManager.ResponseOk(response);
//                            return responseManager.ResponseAccepted();
                        }
                        else
                            return responseManager.ResponseInternalServerError();
                    }
                    else 
                        return responseManager.ResponseBadRequest();
                case 2:
                    boolean userPending = CheckUserPending(username, email_address, "create");
                    if (userPending) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Account with username - " + username + " or email - " + email_address + " is already pending for creation");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    SQL = "INSERT INTO tbl_user_details_operations(username, password, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, ?, 'create', 'Create user account', now())";
                    retval = jdbcTemplate.update(SQL, new Object[]{username, hashPassword, firstname, surname, phone_number, email_address, roleid});
                    if (retval > 0) {
                        if (roleid == 6) {
                            List<String> merchantIds = new ArrayList<>(Arrays.asList(institutionid.split(",")));
                            MapUserToMerchants(email_address, merchantIds);    
                        }
                        String institutiontype = roleid == 5 ? "financial_institution_user" : roleid == 6 ? "merchant_user" : roleid == 7 ? "terminal_owner_user" : roleid == 8 ? "ptsp_user" : "";
                        SQL = "INSERT into tbl_map_card_users_institution(user_email, institution_id, institution_name, institution_type, date_created) VALUES(?, ?, ?, ?, now())";
                        jdbcTemplate.update(SQL, new Object[]{email_address, institutionid, institutionname, institutiontype});
                        return responseManager.ResponseAccepted();
                    }
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
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE tbl_user_details SET deleted = 1 WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{userid});
                    if (retVal > 0)
                        return responseManager.ResponseDeleted();
                    else
                        return responseManager.ResponseInternalServerError();
                case 2:
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
    public ResponseEntity UpdateNames(String sessiontoken, String firstname, String surname, String phone_number, String username) {
        try {
            String SQL;
            int retVal;
            SQL = "UPDATE tbl_user_details SET firstname = ?, surname = ?, phone_number = ? WHERE session_token = ?";
            retVal = jdbcTemplate.update(SQL, new Object[]{firstname, surname, phone_number, sessiontoken});
            if (retVal > 0){
                NetworkResponse networkResponse = new NetworkResponse();
                networkResponse.setCode(200);
                networkResponse.setStatus("success");
                networkResponse.setMessage("Profile Updated");
                return responseManager.ResponseOk(networkResponse);
            }
            else
                return responseManager.ResponseInternalServerError();
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
            }
            else {
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
    public ResponseEntity Edit(String sessiontoken, int userid, String firstname, String surname, String phone_number, int roleid, String username) {
        try {
            String SQL;
            int userrole = GetUserRole(username, sessiontoken);
            int retVal;
            switch (userrole) {
                case 1:
                    SQL = "UPDATE tbl_user_details SET firstname = ?, surname = ?, phone_number = ?, role = ? WHERE id = ?";
                    retVal = jdbcTemplate.update(SQL, new Object[]{firstname, surname, phone_number, roleid, userid});
                    if (retVal > 0)
                        return responseManager.ResponseAccepted();
                    else
                        return responseManager.ResponseInternalServerError();
                case 2:
                    ResponseEntity responseEntity = GetUserById(sessiontoken, userid);
                    LoginResponse loginResponse = responseEntity != null ? (LoginResponse) responseEntity.getBody() : new LoginResponse();
                    if (loginResponse.getId() == 0) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("User not found");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    boolean userPending = CheckUserPending(loginResponse.getUsername(), loginResponse.getEmail_address(), "edit");
                    if (userPending) {
                        NetworkResponse networkResponse = new NetworkResponse();
                        networkResponse.setCode(200);
                        networkResponse.setStatus("failed");
                        networkResponse.setMessage("Account already pending for edit");
                        return responseManager.ResponseOk(networkResponse);
                    }
                    String note = loginResponse.getFirstname().equals(firstname) || firstname == null ? "" : "Change firstname from " + loginResponse.getFirstname() + " to " + firstname;
                    note = loginResponse.getSurname().equals(surname) || surname == null ? note : !note.equals("") ? note + ", change surname from " + loginResponse.getSurname() + " to " + surname : "Change surname from " + loginResponse.getSurname() + " to " + surname;
                    note = loginResponse.getPhone_number().equals(phone_number) || phone_number == null ? note : !note.equals("") ? note + ", change phone number from " + loginResponse.getPhone_number() + " to " + phone_number : "Change phone number from " + loginResponse.getPhone_number() + " to " + phone_number;
                    SQL = "INSERT INTO tbl_user_details_operations(username, firstname, surname, phone_number, email_address, role, actionType, note, date_created) VALUES(?, ?, ?, ?, ?, ?, 'edit', ?, now())";
                    retVal = jdbcTemplate.update(SQL, new Object[]{loginResponse.getUsername(), loginResponse.getFirstname().equals(firstname) || firstname == null ? loginResponse.getFirstname() : firstname, loginResponse.getSurname().equals(surname) || surname == null ? loginResponse.getSurname() : surname, loginResponse.getPhone_number().equals(phone_number) || phone_number == null ? loginResponse.getPhone_number() : phone_number, loginResponse.getEmail_address(), roleid, note});
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
    
    @Override
    public ResponseEntity UserApprovals(String sessiontoken, int id, String actionType, String username, boolean isContact) {
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
                            if (retVal > 0 && retVal2 > 0)
                                return responseManager.ResponseDeleted();
                            else
                                return responseManager.ResponseInternalServerError();
                        case "edit":
                            SQL = "DELETE FROM tbl_user_details_operations WHERE id = ? AND actionType = 'edit'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            SQL = "UPDATE tbl_user_details SET firstname = ?, surname = ?, phone_number = ?, role = ? WHERE username = ? AND email_address = ?";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{users.get(0).getFirstname(), users.get(0).getSurname(), users.get(0).getPhone_number(), users.get(0).getRoleid(), users.get(0).getUsername(), users.get(0).getEmail_address()});
                            if (isContact) {
                                SQL = "DELETE FROM tbl_financial_institution_contacts_operations WHERE email_address = ? AND actionType = 'edit'";
                                jdbcTemplate.update(SQL, new Object[]{users.get(0).getEmail_address()});
                                SQL = "UPDATE tbl_financial_institution_contacts SET firstname = ?, surname = ?, phone_number = ? WHERE email_address = ?";
                                jdbcTemplate.update(SQL, new Object[]{users.get(0).getFirstname(), users.get(0).getSurname(), users.get(0).getPhone_number(), users.get(0).getEmail_address()});
                            }
//                            if (retVal > 0 && retVal2 > 0)
                            return responseManager.ResponseAccepted();
//                            else
//                                return responseManager.ResponseInternalServerError();
                        case "create":
                            SQL = "DELETE FROM tbl_user_details_operations WHERE id = ? AND actionType = 'create'";
                            retVal = jdbcTemplate.update(SQL, new Object[]{id});
                            String reference = randomizer.GenerateReference();
                            String code = randomizer.GenerateReference(45, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890");
                            String ref = randomizer.GenerateReference(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
                            SQL = "INSERT into sparkpayweb_db.tbl_users(username, password, date_created, enabled, reference) VALUES(?, ?, now(), 1, ?)";
                            jdbcTemplate.update(SQL, new Object[]{users.get(0).getEmail_address(), users.get(0).getSecurity(), ref});
                            SQL = "INSERT into tbl_user_details(username, firstname, surname, phone_number, email_address, role, date_created) VALUES(?, ?, ?, ?, ?, ?, now())";
                            retVal2 = jdbcTemplate.update(SQL, new Object[]{users.get(0).getUsername(), users.get(0).getFirstname(), users.get(0).getSurname(), users.get(0).getPhone_number(), users.get(0).getEmail_address(), users.get(0).getRoleid()});
                            if (retVal > 0 && retVal2 > 0){
                                LoginResponse response = new LoginResponse();
                                response.setCode(201);
                                response.setStatus("success");
                                response.setMessage("Account approved");
                                response.setFirstname(ref);
                                response.setSurname(code);
                                response.setUsername(users.get(0).getEmail_address());
                                return responseManager.ResponseOk(response);
                            }
//                                return responseManager.ResponseAccepted();
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
    public ResponseEntity GetUsersForActions(boolean systemUsers) {
        try {
            NetworkResponse networkResponse = new NetworkResponse();
            String SQL;
            if (systemUsers)
                SQL = "SELECT a.id, a.username, a.password, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.actionType, a.note, a.date_created, b.role_name "
                        + "from tbl_user_details_operations a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "WHERE a.role < 4 "
                        + "ORDER BY a.id DESC";
            else 
                SQL = "SELECT a.id, a.username, a.password, a.firstname, a.surname, a.phone_number, a.email_address, a.role, a.actionType, a.note, a.date_created, "
                        + "b.role_name, "
                        + "c.institution_id as financial_institution_code, c.institution_name as institution_name  "
                        + "from tbl_user_details_operations a "
                        + "LEFT JOIN tbl_role b "
                        + "ON a.role = b.id "
                        + "LEFT JOIN tbl_map_card_users_institution c "
                        + "ON a.email_address = c.user_email "
                        + "WHERE a.role > 4 "
                        + "ORDER BY a.id DESC";
            List<UserModel> users = jdbcTemplate.query(SQL, new UserMapper2());
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage(systemUsers ? "Pending System Users" : "Pending Other Users");
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
                    + "c.financial_institution_code, d.name as institution_name  "
                    + "from tbl_user_details_operations a "
                    + "LEFT JOIN tbl_role b "
                    + "ON a.role = b.id "
                    + "LEFT JOIN tbl_financial_institution_contacts c "
                    + "ON a.email_address = c.email_address "
                    + "LEFT JOIN tbl_financial_institutions d "
                    + "ON c.financial_institution_code = d.code "
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
            response.setInstitution(hasColumn(rs, "financial_institution_code") ? rs.getString("financial_institution_code") : "");
            response.setInstitutionName(hasColumn(rs, "institution_name") ? rs.getString("institution_name") : "");
            return response;
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
