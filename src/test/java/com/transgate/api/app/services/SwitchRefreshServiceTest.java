package com.transgate.api.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SwitchRefreshServiceTest {

    @Test
    void parseUrlsSplitsAndTrimsConfiguredList() {
        List<String> urls = SwitchRefreshService.parseUrls(
                " http://a:1057/ajipayaccounts/refresh ,http://b:1057/ajipayaccounts/refresh, ");
        assertEquals(List.of(
                "http://a:1057/ajipayaccounts/refresh",
                "http://b:1057/ajipayaccounts/refresh"), urls);
    }

    @Test
    void parseUrlsTreatsBlankAsEmpty() {
        assertTrue(SwitchRefreshService.parseUrls(null).isEmpty());
        assertTrue(SwitchRefreshService.parseUrls("").isEmpty());
        assertTrue(SwitchRefreshService.parseUrls(" , , ").isEmpty());
    }

    @Test
    void successfulRefreshRequires2xxAndSuccessMarker() {
        assertTrue(SwitchRefreshService.isSuccessfulRefresh(
                200, "All parameters successfully refreshed: institutions=28, charges=12, soapHandlersRefreshed=0, took=45ms"));
        assertFalse(SwitchRefreshService.isSuccessfulRefresh(500, "All parameters successfully refreshed"));
        assertFalse(SwitchRefreshService.isSuccessfulRefresh(200, "refresh failed"));
        assertFalse(SwitchRefreshService.isSuccessfulRefresh(200, null));
        assertFalse(SwitchRefreshService.isSuccessfulRefresh(401, "unauthorized"));
    }
}
