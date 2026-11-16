package com.transgate.api.app.services;

import com.transgate.api.models.NetworkResponse;
import com.transgate.api.models.SwitchRefreshInstanceResult;
import com.transgate.api.util.PlatformRole;
import com.transgate.api.util.ResponseManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Fans out institution/charge memory refresh to every configured AjiPay switch instance.
 * The portal only gets success when every instance returns a successful refresh.
 */
@Service
public class SwitchRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(SwitchRefreshService.class);
    private static final String SUCCESS_MARKER = "All parameters successfully refreshed";

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private final AppEnvironmentConfig appConfig;
    private final ResponseManager responseManager = new ResponseManager();

    public SwitchRefreshService(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }

    public ResponseEntity refreshAll(String sessionToken) {
        if (!isSuperAdmin(sessionToken)) {
            return responseManager.ResponseForbidden("Super Admin role required");
        }

        List<String> urls = parseUrls(appConfig.getSwitchRefreshUrls());
        if (urls.isEmpty()) {
            NetworkResponse response = new NetworkResponse();
            response.setCode(200);
            response.setStatus("failed");
            response.setMessage("No switch refresh URLs configured (app.switch.refresh.urls)");
            response.setData(new ArrayList());
            return responseManager.ResponseOk(response);
        }

        String refreshKey = nz(appConfig.getSwitchRefreshKey()).trim();
        int timeoutMs = appConfig.getSwitchRefreshTimeoutMs();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(urls.size(), 8));
        try {
            List<CompletableFuture<SwitchRefreshInstanceResult>> futures = urls.stream()
                    .map(url -> CompletableFuture.supplyAsync(
                            () -> callOne(client, url, refreshKey, timeoutMs), pool))
                    .collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            List<SwitchRefreshInstanceResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            boolean allOk = results.stream().allMatch(SwitchRefreshInstanceResult::isOk);

            NetworkResponse response = new NetworkResponse();
            response.setCode(200);
            response.setStatus(allOk ? "success" : "failed");
            response.setMessage(allOk
                    ? "All switch instances refreshed"
                    : "One or more switch instances failed to refresh");
            response.setData(new ArrayList(results));
            logger.info("Switch refresh campaign finished allOk={} instances={}", allOk, results.size());
            return responseManager.ResponseOk(response);
        } finally {
            pool.shutdownNow();
        }
    }

    SwitchRefreshInstanceResult callOne(HttpClient client, String url, String refreshKey, int timeoutMs) {
        SwitchRefreshInstanceResult result = new SwitchRefreshInstanceResult();
        result.setUrl(url);
        long started = System.currentTimeMillis();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET();
            if (refreshKey != null && !refreshKey.isEmpty()) {
                builder.header("X-Refresh-Key", refreshKey);
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            result.setHttpStatus(response.statusCode());
            result.setBody(trimBody(response.body()));
            result.setOk(isSuccessfulRefresh(response.statusCode(), response.body()));
            if (!result.isOk() && result.getError() == null) {
                result.setError("Switch refresh was not successful");
            }
        } catch (Exception ex) {
            result.setOk(false);
            result.setError(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            logger.warn("Switch refresh failed for {}: {}", url, result.getError());
        } finally {
            result.setTookMs(System.currentTimeMillis() - started);
        }
        return result;
    }

    static boolean isSuccessfulRefresh(int httpStatus, String body) {
        if (httpStatus < 200 || httpStatus >= 300) {
            return false;
        }
        return body != null && body.contains(SUCCESS_MARKER);
    }

    static List<String> parseUrls(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isSuperAdmin(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return false;
        }
        try {
            Integer role = jdbcTemplate.queryForObject(
                    "SELECT role FROM tbl_user_details WHERE session_token = ? AND deleted = 0 LIMIT 1",
                    new Object[]{sessionToken},
                    Integer.class);
            return role != null && role == PlatformRole.ADMIN;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private static String trimBody(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
