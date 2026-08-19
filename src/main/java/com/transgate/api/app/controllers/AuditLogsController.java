package com.transgate.api.app.controllers;

import com.transgate.api.audit.AuditService;
import com.transgate.api.app.services.Validators;
import com.transgate.api.util.ResponseManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogsController {

    private final AuditService auditService;
    private final Validators validators;
    private final ResponseManager responseManager = new ResponseManager();

    public AuditLogsController(AuditService auditService, Validators validators) {
        this.auditService = auditService;
        this.validators = validators;
    }

    @RequestMapping(value = "/audit-logs", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity list(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessionToken,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "outcome", required = false) String outcome) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        String actorFilter = (email != null && !email.isBlank()) ? email : username;
        return auditService.list(sessionToken, startDate, endDate, page, limit, actorFilter, action, outcome);
    }

    @RequestMapping(value = "/audit-logs/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity getOne(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessionToken,
            @PathVariable("id") long id) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return auditService.getById(sessionToken, id);
    }
}
