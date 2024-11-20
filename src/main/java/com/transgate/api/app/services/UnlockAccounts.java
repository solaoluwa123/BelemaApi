/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.services;

import com.transgate.api.interfaces.UnlockAccountsInterface;
import com.transgate.api.util.CSVHelper;
import static com.transgate.api.util.Constants.FRONTENDURL;
import com.transgate.api.util.Formatter;
import com.transgate.api.util.Mailers;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    JdbcTemplate jdbcTemplate;
    
    CSVHelper cSVHelper = new CSVHelper();
    Mailers mailers = new Mailers();
    
    Formatter formatter = new Formatter();
    
    private final AppEnvironmentConfig appConfig;
    public UnlockAccounts(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }
    
    @Override
    public void AutoPassDisputesForSettlement() {
        try {
            List<Map<String, Object>> disputes;
            String SQL = "SELECT * FROM sparkpayweb_db.tbl_disputes WHERE timeline_date <= now() AND status = -1 AND resolved = 0 AND type != 'habari'";
            disputes = jdbcTemplate.queryForList(SQL);
            if (disputes.size() > 0) {
                for (int i = 0; i < disputes.size(); i++) {
                    SQL = "SELECT * FROM sparkpay.merchants WHERE merchant_id = ?";
                    String merchant_id = (String) disputes.get(i).get("merchant_id");
                    int dispute_id = (int) disputes.get(i).get("id");
                    List<Map<String, Object>> merchants = jdbcTemplate.queryForList(SQL, new Object[]{merchant_id});
                    if (merchants.size() > 0) {
                        SQL = "UPDATE sparkpayweb_db.tbl_disputes SET resolved_by = 'Auto Resolved', status = 0, resolved = 0, date_modified = now() WHERE id = ?";
                        jdbcTemplate.update(SQL, new Object[]{dispute_id});
                    }
                }
            }
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
        return appConfig.getHabariIncomeAccount();
    }
    
    public String GetBankIncomeAccountNumber() {
        return appConfig.getBankIncomeAccount();
    }
    
    public boolean isWeekend(String dateString) {
        try {
            // Parse the input date string using the specified format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);

            // Check if the day of the week is Saturday or Sunday
            DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        } catch (Exception e) {
            // Handle parsing errors, e.g., invalid date string
            return false;
        }
    }
    
    public boolean isMonday(String dateString) {
        try {
            // Parse the input date string using the specified format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);

            // Check if the day of the week is Saturday or Sunday
            DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
            return dayOfWeek == DayOfWeek.MONDAY;
        } catch (Exception e) {
            // Handle parsing errors, e.g., invalid date string
            return false;
        }
    }
    
    @Override
    public void SendAllAcceptedDisputes() {
        try {
            String SQL;
            List<Map<String, Object>> merchants = GetMerchants();
            String habaripayAccountNumber = GetHabaripayAccountNumber();
            String bankIncomeAccountNumber = GetBankIncomeAccountNumber();
            
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
            String paddedMonth = currentMonth < 10 ? "0" + currentMonth : ""+currentMonth;
            int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            String paddedCurrentDay = currentDay < 10 ? "0"+currentDay : "" + currentDay;
            int yesterDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1;
            String paddedYesterDay = yesterDay < 10 ? "0"+yesterDay : "" + yesterDay;
            String startDate = currentYear+"-"+paddedMonth+"-"+paddedYesterDay+"T00:00:00";
            String endDate = currentYear+"-"+paddedMonth+"-"+paddedCurrentDay+"T00:00:00";
            if (!isWeekend(endDate)) {
                if (isMonday(endDate)) {
                    int threeDaysBefore = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 3;
                    String paddedThreeDaysBefore = threeDaysBefore < 10 ? "0"+threeDaysBefore : "" + threeDaysBefore;
                    startDate = currentYear+"-"+paddedMonth+"-"+paddedThreeDaysBefore+"T00:00:00";
                }
                for (final Map<String, Object> merchant : merchants) {
                    Double totalValue;
                    List<Map<String, Object>> agg;
                    final List<Map<String, Object>> transactions;
                    String merchantId = (String) merchant.get("merchant_id");
                    SQL = "SELECT a.id, a.logged_by, a.resolved_by, a.status, a.resolved, a.date_modified, a.date_created, a.timeline_date, a.proof_of_debit_uri, a.proof_of_reject_uri, a.arbitrated_by, a.date_arbitrated, a.cardholder_acct_nuban, "
                        + "a.message_type, a.pan, a.amount, a.system_trace_number, a.retrieval_ref_number, a.destination_acquiring_institution_id, a.acquirer_institution_id, "
                        + "a.terminal_id, a.merchant_id, a.bin, a.ncs_date_time, a.response_code, a.cardholder_acct_number "
                        + "FROM sparkpayweb_db.tbl_disputes a "
                        + "WHERE a.merchant_id = ? AND a.status = 0 AND a.resolved = 0 AND a.date_modified >= ? AND a.date_modified < ? AND a.type = 'institution'"
                        + " ORDER BY a.date_created DESC";

                    transactions = jdbcTemplate.queryForList(SQL, new Object[]{merchantId, startDate, endDate});

                    SQL = "SELECT "
                            + "SUM(a.amount) as totalValue "
                            + "FROM sparkpayweb_db.tbl_disputes a "
                            + "WHERE a.merchant_id = ? AND a.status = 0 AND a.resolved = 0 AND a.date_modified >= ? AND a.date_modified < ? AND a.type = 'institution'";

                    agg = jdbcTemplate.queryForList(SQL, new Object[]{merchantId, startDate, endDate});
                    Map<String, Object> row = agg.get(0);
                    Double tValue = (Double) row.get("totalValue");
                    totalValue = tValue != null ? tValue/100 : 0;
                    String[] headers = {"", "Acct Number", "", "", "", "", "Amount", "Debit/Credit", "Remark"};
                    List<String[]> data = new ArrayList<>();
                    String merchantAccount = (String) merchant.get("account_account_number");
                    String merchantName = (String) merchant.get("merchant_name");
                    merchantName = formatter.RemoveSpecialCharacters(merchantName.split(" ")[0]);
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
                                formatter.FormatCardHolderAcctNum(accountNumber).replace('/', ','),
                                tAmount.toString(),
                                "2",
                                remark
                            });
                        }

                        Double tnxTotalMSCHalf = (tnxTotalMSC / 2);
                        data.add(0, new String[]{"Merchant Acct", merchantAccount.replace('/', ','), merchantDr.toString(), "1", "Refund for accepted Chargeback _"+startDate+" refunds"});
                        data.add(1, new String[]{"Bank Income Acct", bankIncomeAccountNumber.replace('/', ','), tnxTotalMSCHalf.toString(), "1", "Reversal of fee earned for accepted Chargeback -"+merchantName.trim()+" terminals"});
                        data.add(2, new String[]{"Habaripay Income Account", habaripayAccountNumber.replace('/', ','), tnxTotalMSCHalf.toString(), "1", "Reversal of fee earned for accepted Chargeback -"+merchantName.trim()+" terminals"});
                        String filePath = cSVHelper.WriteFile(fileName,
                                headers,
                                data
                        );

                        String url = FRONTENDURL+"/accepteddisputereports/"+formatter.RemoveSpecialCharacters(fileName)+".csv";;
                        String message = "Dear Team,<br/><br/>Please find attached Habaripay entries to be regularized in respective accounts/ledgers.<br/><br/>Thank you for your continuous support";
//                        mailers.SendMailWithHabariOkHttpClient(
//                                "Dispute Report for "+merchantName.trim(), 
//                                "no-reply@habaripay.com", 
//                                "amicheal@supersoft.com.ng", 
//                                message,
//                                fileName,
//                                filePath
//                        );
                        mailers.SendMailWithHabariOkHttpClient(
                                "Dispute Report for "+merchantName.trim(), 
                                "no-reply@habaripay.com", 
                                "chioma.enechi@habaripay.com", 
                                message,
                                fileName,
                                filePath
                        );
                        mailers.SendMailWithHabariOkHttpClient(
                                "Dispute Report for "+merchantName.trim(), 
                                "no-reply@habaripay.com", 
                                "kenneth.ekunwe@gtbank.com", 
                                message,
                                fileName,
                                filePath
                        );
                        mailers.SendMailWithHabariOkHttpClient(
                                "Dispute Report for "+merchantName.trim(), 
                                "no-reply@habaripay.com", 
                                "epaymentsupportlist@gtbank.com", 
                                message,
                                fileName,
                                filePath
                        );
                        mailers.SendMailWithHabariOkHttpClient(
                                "Dispute Report for "+merchantName.trim(), 
                                "no-reply@habaripay.com", 
                                "settlement@habaripay.com", 
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
            }
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
        } catch (ParseException ex) {
            Logger.getLogger(UnlockAccounts.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void SendDisputesReminders() {
        try {
            String SQL;
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
            String paddedMonth = currentMonth < 10 ? "0" + currentMonth : ""+currentMonth;
            int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            String paddedCurrentDay = currentDay < 10 ? "0"+currentDay : "" + currentDay;
            String todayDate = currentYear+"-"+paddedMonth+"-"+paddedCurrentDay+"T00:00:00";
            if (!isWeekend(todayDate)) {
                final List<Map<String, Object>> disputes;
                SQL = "SELECT COUNT(id) as count, merchant_id FROM sparkpayweb_db.tbl_disputes WHERE status = -1 AND resolved = 0 AND type = 'institution' GROUP BY merchant_id";
                disputes = jdbcTemplate.queryForList(SQL);
                if (disputes.size() > 0) {
                   for (final Map<String, Object> dispute : disputes) {
                        SQL = "SELECT ptsp_id FROM sparkpayweb_db.tbl_map_merchants_ptsps WHERE merchant_id = ?";
                        String ptspid = jdbcTemplate.queryForObject(SQL, new Object[]{dispute.get("merchant_id")}, String.class);
                        SQL = "SELECT user_email FROM tbl_map_card_users_institution WHERE institution_id = ? LIMIT 3";

                        List<Map<String, Object>> ptspUsers = jdbcTemplate.queryForList(SQL, new Object[]{ptspid});
                        if (ptspUsers.size() > 0) {
                            try {
                                ptspUsers.forEach(row -> {
                                    long totalDisputes = (long)dispute.get("count");
                                    String partMessage = totalDisputes == 1 ? " is a pending dispute for your team to treat on Sparkpay " : " are a total of " + totalDisputes + " disputes left for your team to treat on Sparkpay ";
                                    String message = "<html><body>Dear Team, <br/><br/>Please be informed that there "+partMessage+". Please login to sparkpay to take action before they are auto-accepted "
                                            + "<br/><br/>Sparkpay,"
                                            + "<br/>Cheers</body><html>";
                                    mailers.SendMailWithHabariOkHttpClient(
                                            "Dispute Action Reminder",
                                            "no-reply@habaripay.com",
                                            (String) row.get("user_email"),
                                            message
                                    );
                                });
                            } catch(Exception e) {
                                System.out.println("mailer error: " + e.toString());
                            }
                        }
                   } 
                }
                
            }
            
        } catch (DataAccessException ex) {
            System.out.println("error>>>>" + ex.getMessage());
        }
    }
}
