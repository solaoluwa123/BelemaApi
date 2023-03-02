/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.UnlockAccountsInterface;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author Makintola
 */
@Service
public class UnlockAccounts implements UnlockAccountsInterface {
    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;
    
    @Override
    public void AutoPassDisputesForSettlement() {
        try {
            String SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = 'Auto Resolved', status = 0, resolved = 0, date_modified = now() WHERE timeline_date <= now() AND status = -1 AND resolved = 0";
            jdbcTemplate.update(SQL);
        } catch (DataAccessException e) {
            System.out.println("auto pass disputes for settlements exception " + e.getMessage());
        }
    }
    
    @Override
    public void ReduceLockTime() {
        try {
            String SQL = "UPDATE tbl_user_details SET unlock_account_in = unlock_account_in - 1 WHERE unlock_account_in > 0";
            jdbcTemplate.update(SQL);
        } catch (DataAccessException e) {
            System.out.println("reduce lock time exception " + e.getMessage());
        }
    }
    
    @Override
    public void Unlock() {
        try {
            String SQL = "UPDATE tbl_user_details SET attempts_left = 3 WHERE unlock_account_in = 0";
            jdbcTemplate.update(SQL);
        } catch (DataAccessException e) {
            System.out.println("unlock exception " + e.getMessage());
        }
    }
    
    
}
