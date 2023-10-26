/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.UnlockAccountsInterface;
import com.transgate.api.util.CSVHelper;
import static com.transgate.api.util.Constants.BANK_INCOME_ACCOUNT;
import static com.transgate.api.util.Constants.FRONTENDURL;
import static com.transgate.api.util.Constants.HABARIPAY_INCOME_ACCOUNT;
import com.transgate.api.util.Mailers;
import com.transgate.api.util.Validators;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    
    CSVHelper cSVHelper = new CSVHelper();
    Validators validators = new Validators();
    Mailers mailers = new Mailers();
    
    @Override
    public void AutoPassDisputesForSettlement() {
        try {
            String SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = 'Auto Resolved', status = 0, resolved = 0, date_modified = now() WHERE timeline_date <= now() AND status = -1 AND resolved = 0 AND type != 'habari'";
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
    
    public List GetMerchants() {
        final List<Map<String, Object>> merchants;
        String SQL = "SELECT id, merchant_id, merchant_name, merchant_service_charge, msc_cap, old_account_account_number as account_account_number "
                + "FROM sparkpay.merchants "
                + "WHERE isLockedForSettlement = 1";
        merchants = jdbcTemplate.queryForList(SQL);
        return merchants;
    }
    
    public String GetHabaripayAccountNumber() {
        return HABARIPAY_INCOME_ACCOUNT;
    }
    
    public String GetBankIncomeAccountNumber() {
        return BANK_INCOME_ACCOUNT;
    }
    
    @Override
    public void SendAllAcceptedDisputes() {
        try {
            String SQL;
            List<Map<String, Object>> merchants = GetMerchants();
            String habaripayAccountNumber = GetHabaripayAccountNumber();
            String bankIncomeAccountNumber = GetBankIncomeAccountNumber();
            for (final Map<String, Object> merchant : merchants) {
                Double totalValue;
                List<Map<String, Object>> agg;
                final List<Map<String, Object>> transactions;
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
                String paddedMonth = currentMonth < 10 ? "0" + currentMonth : ""+currentMonth;
                int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                String paddedCurrentDay = currentDay < 10 ? "0"+currentDay : "" + currentDay;
                int yesterDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1;
                String paddedYesterDay = yesterDay < 10 ? "0"+yesterDay : "" + yesterDay;
                String startDate = currentYear+"-"+paddedMonth+"-"+paddedYesterDay+"T00:00:00";
                String endDate = currentYear+"-"+paddedMonth+"-"+paddedCurrentDay+"T00:00:00";
                String merchantId = (String) merchant.get("merchant_id");
                SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                    + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                    + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                    + "FROM sparkpayweb_db.tbl_disputes a "
                    + "WHERE a.merchant_id = ? AND a.status = 0 AND a.resolved = 0 AND a.timeline_date >= ? AND a.timeline_date < ?"
                    + " ORDER BY a.date_created DESC";

                transactions = jdbcTemplate.queryForList(SQL, new Object[]{merchantId, startDate, endDate});

                SQL = "SELECT "
                        + "SUM(a.amount) as totalValue "
                        + "FROM sparkpayweb_db.tbl_disputes a "
                        + "WHERE a.merchant_id = ? AND a.status = 0 AND a.resolved = 0 AND a.timeline_date >= ? AND a.timeline_date < ?";

                agg = jdbcTemplate.queryForList(SQL, new Object[]{merchantId, startDate, endDate});
                Map<String, Object> row = agg.get(0);
                Double tValue = (Double) row.get("totalValue");
                totalValue = tValue != null ? tValue/100 : 0;
                String[] headers = {"", "Acct Number", "Amount", "Debit/Credit", "Remark"};
                List<String[]> data = new ArrayList<>();
                String merchantAccount = (String) merchant.get("account_account_number");
                String merchantName = (String) merchant.get("merchant_name");
                merchantName = validators.RemoveSpecialCharacters(merchantName.split(" ")[0]);
                BigDecimal _msc = (BigDecimal) merchant.get("merchant_service_charge");
                DecimalFormat df = new DecimalFormat("0.00");
                Double msc = _msc.doubleValue();
                BigDecimal _mscap = (BigDecimal) merchant.get("msc_cap");
                Double mscap = _mscap.doubleValue();
//                Double totalMSC = msc.doubleValue() * transactions.size();
//                Double merchantDr = totalValue - totalMSC;
                String fileName = merchantName.trim()+startDate+"-"+endDate;
                Double merchantDr = 0.00;
                Double totalMSC = 0.00;
                Double tnxTotalMSC = 0.00;
                if (transactions.size() > 0) {
                    for (int i = 0; i < transactions.size(); i++) {
                        final Map<String, Object> transaction = transactions.get(i);
                        String accountNumber = (String) transaction.get("cardholder_acct_number");
                        String amount_ = (String) transaction.get("amount");
                        BigDecimal amount = new BigDecimal(amount_);
                        Double tAmount = Double.parseDouble(df.format(amount.doubleValue() / 100));
                        totalMSC = Double.parseDouble(df.format((msc / 100) * tAmount));
                        if (totalMSC > mscap)
                            totalMSC = mscap;
                        merchantDr = merchantDr + (tAmount - totalMSC);
                        tnxTotalMSC = tnxTotalMSC + totalMSC;
                        String retrieval_ref_number = (String) transaction.get("retrieval_ref_number");
                        String system_trace_number = (String) transaction.get("system_trace_number");
                        LocalDateTime tnxDate = (LocalDateTime) transaction.get("ncs_date_time");
                        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = inputDateFormat.parse(tnxDate.toString().split(" ")[0]);
                        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                        String formatedDate = outputDateFormat.format(date);
                        String remark = "RVSL_POS TRANSFER"+formatedDate+"_"+system_trace_number+"_"+retrieval_ref_number;
                        data.add(new String[]{
                            i == 0 ? "Card holder's Accts" : "",
                            validators.FormatCardHolderAcctNum(accountNumber),
                            tAmount.toString(),
                            "2",
                            remark
                        });
                    }
                    
                    Double tnxTotalMSCHalf = (tnxTotalMSC / 2);
                    data.add(0, new String[]{"Merchant Acct", merchantAccount, merchantDr.toString(), "1", "Refund for accepted Chargeback _"+startDate+" refunds"});
                    data.add(1, new String[]{"Bank Income Acct", bankIncomeAccountNumber, tnxTotalMSCHalf.toString(), "1", "Reversal of fee earned for accepted Chargeback -"+merchantName.trim()+" terminals"});
                    data.add(2, new String[]{"Habaripay Income Account", habaripayAccountNumber, tnxTotalMSCHalf.toString(), "1", "Reversal of fee earned for accepted Chargeback -"+merchantName.trim()+" terminals"});
                    String filePath = cSVHelper.WriteFile(fileName,
                            headers,
                            data
                    );

                    String url = FRONTENDURL+"/accepteddisputereports/"+validators.RemoveSpecialCharacters(fileName)+".csv";;
                    String message = "Dear Team,<br/><br/>Please find attached Habaripay entries to be regularized in respective accounts/ledgers.<br/><br/>Thank you for your continuous support";
                    mailers.SendMailWithHabariOkHttpClient(
                            "Dispute Report for "+merchantName.trim(), "no-reply@habaripay.com", 
                            "amicheal@supersoft.com.ng", 
                            message,
                            fileName,
                            filePath
                    );
                    mailers.SendMailWithHabariOkHttpClient(
                            "Dispute Report for "+merchantName.trim(), "no-reply@habaripay.com", 
                            "chioma.enechi@habaripay.com", 
                            message,
                            fileName,
                            filePath
                    );
//                    mailers.sendMail(
//                            "michealakintola106.pog@gmail.com", 
//                            "Dispute Report for "+merchantName.trim(), 
//                            message,
//                            "no-reply@habaripay.com",
//                            "amicheal@supersoft.com.ng"
//                    );
                }
            }
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
        } catch (ParseException ex) {
            Logger.getLogger(UnlockAccounts.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
