package com.transgate.api.app.controllers;

import com.transgate.api.app.services.SwitchRefreshService;
import com.transgate.api.app.services.Validators;
import com.transgate.api.util.ResponseManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portal-facing switch operations. Super Admin only.
 */
@RestController
public class SwitchController {

    private final SwitchRefreshService switchRefreshService;
    private final Validators validators;
    private final ResponseManager responseManager = new ResponseManager();

    public SwitchController(SwitchRefreshService switchRefreshService, Validators validators) {
        this.switchRefreshService = switchRefreshService;
        this.validators = validators;
    }

    /**
     * Refresh in-memory institutions and charges on every configured AjiPay switch instance.
     * Returns success only when every instance reports a successful refresh.
     */
    @RequestMapping(value = "/switch/refresh-institutions", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity refreshInstitutions(
            @RequestHeader(value = "Authorization") String header,
            @RequestHeader(value = "auth-token") String sessionToken) {
        if (!validators.validHeader().equals(header)) {
            return responseManager.InvalidAuthorizationHeader();
        }
        return switchRefreshService.refreshAll(sessionToken);
    }
}
