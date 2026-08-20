package com.transgate.api.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Resolve the signed-in user's role and FI code from {@code auth-token}.
 * Used to force Third Party Vendor (role 4) onto institution-scoped transaction APIs.
 */
@Component
public class SessionActorResolver {

    public record Actor(int role, String email, String institutionCode) {
        public boolean isThirdPartyVendor() {
            return PlatformRole.isThirdPartyVendor(role);
        }

        public boolean hasInstitutionCode() {
            String code = institutionCode == null ? "" : institutionCode.trim();
            return !code.isEmpty() && !"-1".equals(code);
        }
    }

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public Optional<Actor> resolve(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        try {
            String SQL = "SELECT a.role, a.email_address, c.financial_institution_code "
                    + "FROM tbl_user_details a "
                    + "LEFT JOIN tbl_financial_institution_contacts c "
                    + "ON a.email_address = c.email_address "
                    + "WHERE a.session_token = ? AND a.deleted = 0 "
                    + "LIMIT 1";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, sessionToken);
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            Map<String, Object> row = rows.get(0);
            Object roleObj = row.get("role");
            int role = roleObj instanceof Number
                    ? ((Number) roleObj).intValue()
                    : Integer.parseInt(String.valueOf(roleObj));
            String email = row.get("email_address") == null ? "" : String.valueOf(row.get("email_address"));
            String code = row.get("financial_institution_code") == null
                    ? ""
                    : String.valueOf(row.get("financial_institution_code")).trim();
            return Optional.of(new Actor(role, email, code));
        } catch (DataAccessException | NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
