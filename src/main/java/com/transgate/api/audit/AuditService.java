package com.transgate.api.audit;

import com.transgate.api.models.AuditLogModel;
import com.transgate.api.models.NetworkResponse;
import com.transgate.api.util.PlatformRole;
import com.transgate.api.util.ResponseManager;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final String CONTEXT_PATH = "/sparkpayapi";

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private final ResponseManager responseManager = new ResponseManager();

    public void log(
            String actorUsername,
            String actorEmail,
            Integer actorRole,
            String action,
            String resource,
            String httpMethod,
            String requestPath,
            String ipAddress,
            String userAgent,
            String outcome,
            Integer httpStatus,
            String details) {
        try {
            String SQL = "INSERT INTO tbl_audit_log("
                    + "actor_username, actor_email, actor_role, action, resource, http_method, "
                    + "request_path, ip_address, user_agent, outcome, http_status, details) "
                    + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(SQL, new Object[]{
                truncate(actorUsername, 50),
                truncate(actorEmail, 100),
                actorRole,
                truncate(action, 40),
                truncate(resource, 80),
                truncate(httpMethod, 10),
                truncate(requestPath, 512),
                truncate(ipAddress, 64),
                truncate(userAgent, 255),
                truncate(outcome, 20),
                httpStatus,
                AuditRedactor.truncate(details)
            });
        } catch (Exception ex) {
            logger.warn("Failed to write audit log action={}: {}", action, ex.getMessage());
        }
    }

    public void logAuthEvent(String actorEmail, String actorUsername, Integer actorRole,
            String action, String outcome, Integer httpStatus, String details) {
        HttpServletRequest request = currentRequest();
        String path = request != null ? buildPath(request) : "/sparkpayapi/users/login";
        String method = request != null ? request.getMethod() : "POST";
        log(
                actorUsername,
                actorEmail,
                actorRole,
                action,
                resolveResource(path),
                method,
                path,
                resolveClientIp(request),
                resolveUserAgent(request),
                outcome,
                httpStatus,
                details
        );
    }

    public void logMutation(HttpServletRequest request, Integer httpStatus, Exception ex) {
        if (request == null) {
            return;
        }
        String method = request.getMethod();
        if (!isMutation(method)) {
            return;
        }
        String path = buildPath(request);
        if (shouldSkipPath(path)) {
            return;
        }

        String actorEmail = stringAttr(request, AuditConstants.ATTR_ACTOR_EMAIL);
        String actorUsername = stringAttr(request, AuditConstants.ATTR_ACTOR_USERNAME);
        Integer actorRole = intAttr(request, AuditConstants.ATTR_ACTOR_ROLE);

        boolean success = ex == null && httpStatus != null && httpStatus < 400;
        String outcome = success ? AuditConstants.OUTCOME_SUCCESS : AuditConstants.OUTCOME_FAILURE;
        String action = resolveAction(method, path);
        String details = null;
        if (ex != null) {
            details = AuditRedactor.truncate("exception=" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        log(
                actorUsername,
                actorEmail,
                actorRole,
                action,
                resolveResource(path),
                method,
                path,
                resolveClientIp(request),
                resolveUserAgent(request),
                outcome,
                httpStatus,
                details
        );
    }

    public ResponseEntity list(
            String sessionToken,
            String startDate,
            String endDate,
            int page,
            int limit,
            String email,
            String action,
            String outcome) {
        try {
            if (!isAdmin(sessionToken)) {
                return responseManager.ResponseForbidden("Admin role required");
            }

            int safePage = page < 1 ? 1 : page;
            int safeLimit = limit < 1 ? 50 : Math.min(limit, 200);
            int offset = (safePage - 1) * safeLimit;

            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new ArrayList<>();
            if (startDate != null && !startDate.isBlank()) {
                where.append(" AND event_time >= ? ");
                args.add(startDate);
            }
            if (endDate != null && !endDate.isBlank()) {
                where.append(" AND event_time <= ? ");
                args.add(endDate);
            }
            if (email != null && !email.isBlank()) {
                where.append(" AND (actor_email = ? OR actor_username = ?) ");
                args.add(email);
                args.add(email);
            }
            if (action != null && !action.isBlank()) {
                where.append(" AND action = ? ");
                args.add(action);
            }
            if (outcome != null && !outcome.isBlank()) {
                where.append(" AND outcome = ? ");
                args.add(outcome);
            }

            String countSql = "SELECT COUNT(*) FROM tbl_audit_log" + where;
            int totalRecords = jdbcTemplate.queryForObject(countSql, args.toArray(), Integer.class);

            String dataSql = "SELECT id, event_time, actor_username, actor_email, actor_role, action, resource, "
                    + "http_method, request_path, ip_address, user_agent, outcome, http_status, details "
                    + "FROM tbl_audit_log" + where
                    + " ORDER BY event_time DESC, id DESC LIMIT ? OFFSET ?";
            List<Object> dataArgs = new ArrayList<>(args);
            dataArgs.add(safeLimit);
            dataArgs.add(offset);
            List<AuditLogModel> rows = jdbcTemplate.query(dataSql, dataArgs.toArray(), new AuditLogMapper());

            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Audit logs");
            networkResponse.setData((ArrayList) rows);
            networkResponse.setMeta("{\"totalRecords\":" + totalRecords
                    + ",\"page\":" + safePage
                    + ",\"limit\":" + safeLimit + "}");
            return responseManager.ResponseOk(networkResponse);
        } catch (DataAccessException ex) {
            logger.error("Failed to list audit logs: {}", ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    public ResponseEntity getById(String sessionToken, long id) {
        try {
            if (!isAdmin(sessionToken)) {
                return responseManager.ResponseForbidden("Admin role required");
            }
            String SQL = "SELECT id, event_time, actor_username, actor_email, actor_role, action, resource, "
                    + "http_method, request_path, ip_address, user_agent, outcome, http_status, details "
                    + "FROM tbl_audit_log WHERE id = ?";
            AuditLogModel row = jdbcTemplate.queryForObject(SQL, new Object[]{id}, new AuditLogMapper());
            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(200);
            networkResponse.setStatus("success");
            networkResponse.setMessage("Audit log");
            ArrayList data = new ArrayList();
            data.add(row);
            networkResponse.setData(data);
            return responseManager.ResponseOk(networkResponse);
        } catch (EmptyResultDataAccessException ex) {
            NetworkResponse networkResponse = new NetworkResponse();
            networkResponse.setCode(404);
            networkResponse.setStatus("failed");
            networkResponse.setMessage("Audit log not found");
            return responseManager.ResponseNotFound(networkResponse);
        } catch (DataAccessException ex) {
            logger.error("Failed to get audit log {}: {}", id, ex.getMessage());
            return responseManager.ResponseInternalServerError();
        }
    }

    private boolean isAdmin(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return false;
        }
        try {
            Integer role = jdbcTemplate.queryForObject(
                    "SELECT role FROM tbl_user_details WHERE session_token = ? AND deleted = 0",
                    new Object[]{sessionToken},
                    Integer.class);
            return role != null && role == PlatformRole.ADMIN;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    public static boolean isMutation(String method) {
        if (method == null) {
            return false;
        }
        switch (method.toUpperCase()) {
            case "POST":
            case "PUT":
            case "DELETE":
            case "PATCH":
                return true;
            default:
                return false;
        }
    }

    public static boolean shouldSkipPath(String path) {
        if (path == null) {
            return true;
        }
        String p = path.toLowerCase();
        if (p.contains("/crons/")) {
            return true;
        }
        if (p.startsWith("/sparkpayapi/swagger-ui") || p.startsWith("/sparkpayapi/v3/api-docs")) {
            return true;
        }
        if (p.startsWith("/sparkpayapi/audit-logs")) {
            return true;
        }
        // Auth events are logged explicitly from UsersService
        if (p.equals("/sparkpayapi/users/login")
                || p.equals("/sparkpayapi/users/login-2fa")
                || p.equals("/sparkpayapi/users/logout")
                || p.equals("/sparkpayapi/users/recoverpassword")
                || p.equals("/sparkpayapi/users/resetpassword")
                || p.equals("/sparkpayapi/users/activateaccount")
                || p.equals("/sparkpayapi/user/generate-token")) {
            return true;
        }
        return false;
    }

    public static String resolveAction(String method, String path) {
        String lower = path == null ? "" : path.toLowerCase();
        if (lower.contains("/approval") || lower.contains("/approve")) {
            return AuditConstants.ACTION_APPROVE;
        }
        if (lower.contains("/reject")) {
            return AuditConstants.ACTION_REJECT;
        }
        if (method == null) {
            return AuditConstants.ACTION_MUTATION;
        }
        switch (method.toUpperCase()) {
            case "PUT":
                if (lower.contains("/create") || lower.contains("/log")) {
                    return AuditConstants.ACTION_CREATE;
                }
                return AuditConstants.ACTION_UPDATE;
            case "POST":
                if (lower.contains("/edit") || lower.contains("/update")) {
                    return AuditConstants.ACTION_UPDATE;
                }
                return AuditConstants.ACTION_UPDATE;
            case "DELETE":
                return AuditConstants.ACTION_DELETE;
            case "PATCH":
                return AuditConstants.ACTION_UPDATE;
            default:
                return AuditConstants.ACTION_MUTATION;
        }
    }

    public static String resolveResource(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String stripped = path;
        if (stripped.startsWith(CONTEXT_PATH)) {
            stripped = stripped.substring(CONTEXT_PATH.length());
        }
        if (stripped.startsWith("/")) {
            stripped = stripped.substring(1);
        }
        int slash = stripped.indexOf('/');
        String first = slash < 0 ? stripped : stripped.substring(0, slash);
        int query = first.indexOf('?');
        if (query >= 0) {
            first = first.substring(0, query);
        }
        return first.isBlank() ? null : first;
    }

    public static String buildPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return uri;
        }
        return uri + "?" + query;
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    public static String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader("User-Agent");
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String stringAttr(HttpServletRequest request, String key) {
        Object value = request.getAttribute(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Integer intAttr(HttpServletRequest request, String key) {
        Object value = request.getAttribute(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static class AuditLogMapper implements RowMapper<AuditLogModel> {
        @Override
        public AuditLogModel mapRow(ResultSet rs, int rowNum) throws SQLException {
            AuditLogModel model = new AuditLogModel();
            model.setId(rs.getLong("id"));
            Object eventTime = rs.getObject("event_time");
            model.setEvent_time(eventTime == null ? null : String.valueOf(eventTime));
            model.setActor_username(rs.getString("actor_username"));
            model.setActor_email(rs.getString("actor_email"));
            int role = rs.getInt("actor_role");
            model.setActor_role(rs.wasNull() ? null : role);
            model.setAction(rs.getString("action"));
            model.setResource(rs.getString("resource"));
            model.setHttp_method(rs.getString("http_method"));
            model.setRequest_path(rs.getString("request_path"));
            model.setIp_address(rs.getString("ip_address"));
            model.setUser_agent(rs.getString("user_agent"));
            model.setOutcome(rs.getString("outcome"));
            int status = rs.getInt("http_status");
            model.setHttp_status(rs.wasNull() ? null : status);
            model.setDetails(rs.getString("details"));
            return model;
        }
    }
}
