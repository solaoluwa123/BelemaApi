/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

import java.math.BigInteger;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Makintola
 */
public interface TransactionsInterface {
    public ResponseEntity Get();
    
    public ResponseEntity Get(String startDate, String endDate, int page, int limit, boolean isCurrent);
    
    public ResponseEntity getTransactionsByDateOnly(String startDate, String endDate, int page, int limit, boolean isCurrent);
    
    public ResponseEntity GetBySessionId(String sessionid);
    
    public ResponseEntity GetBySessionId(String sessionid, boolean isCurrent);
    
    public ResponseEntity GetTransactionsVolume(String startDate, String endDate);
    
    public ResponseEntity GetFTTimeAverage(String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetFTTimeAverage(String institutioncode, String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetSuccessTNXVolume(String startDate, String endDate);
    
    public ResponseEntity GetSuccessTNXVolume(String institutioncode, String startDate, String endDate);
    
    public ResponseEntity GetTop6ResponseCodesTNX(String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetFailedTnxCountByInstitutions(String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetFailedTnxCountByInstitutions(String institution, String startDate, String endDate, boolean isCurrent);
    
    /** Success rate % and volumes per destination bank (transfers sent to). Success = response_code 00 only. */
    public ResponseEntity GetDestinationSuccessRates(String startDate, String endDate, boolean isCurrent);

    /** Same as above, scoped to a source institution (destinations that institution sent to). */
    public ResponseEntity GetDestinationSuccessRates(String sourceInstitution, String startDate, String endDate, boolean isCurrent);

    public ResponseEntity GetTop6ResponseCodesTNX(String institutioncode, String startDate, String endDate, boolean isCurrent);

    /** All response codes (including 00) with volumes for dashboard distribution chart. */
    public ResponseEntity GetResponseCodeVolumes(String startDate, String endDate, boolean isCurrent);

    public ResponseEntity GetResponseCodeVolumes(String institutioncode, String startDate, String endDate, boolean isCurrent);

    /** Time-bucketed transaction throughput (TPS) for dashboard line chart. */
    public ResponseEntity GetTransactionsTps(String startDate, String endDate, boolean isCurrent, int bucketSeconds);

    public ResponseEntity GetTransactionsTps(String institutioncode, String startDate, String endDate, boolean isCurrent, int bucketSeconds);
    
    public ResponseEntity GetAllResponseCodesTNXInstitution(String institutioncode, String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetAllResponseCodesTNXInstitution(String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetTransactionsVolumeByChannels(String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetTransactionsVolumeByChannels(String institutioncode, String startDate, String endDate, boolean isCurrent);
    
    public ResponseEntity GetTransactionsVolume(String institutioncode, String startDate, String endDate);
    
    public ResponseEntity GetTransactionsRates(String startDate, String endDate, boolean inward, String institution);
    
    public ResponseEntity GetTransactionsRates(String institutioncode, String startDate, String endDate, boolean inward);
    
    public ResponseEntity GetInsitutionTnxTrend(String institutioncode, String type, String startDate, String endDate);
    
    public ResponseEntity GetLiveMonitoring(String startDate, String endDate, String institution, int bucketMinutes, int limit);
    
    public ResponseEntity GetLiveTransactionFeed(String since, int limit, String institution);
    
    public java.util.List<com.transgate.api.models.FullTransactionModel> PollLiveTransactions(String since, int limit, String institution);
    
    public ResponseEntity GetStatusSummary(String startDate, String endDate, boolean isCurrent, String institution);
    
    public ResponseEntity GetDashboardCompare(String startDate, String endDate, boolean isCurrent, String institution);
    
    public ResponseEntity Get(int id);
    
    public ResponseEntity Get(String institutioncode);
    
    public ResponseEntity Get(String institutioncode, String startDate, String endDate, int page, int limit, boolean isCurrent);
    
    public ResponseEntity getInstitutionTransactionsByDateOnly(String institutioncode, String startDate, String endDate, int page, int limit, boolean isCurrent);
    
    public ResponseEntity LogDispute(String sessiontoken, String sessionId, String amount, String wallet, String sourceInstitution, String type, String username);
    
    public ResponseEntity LogDisputesBulk(String sessiontoken, String records, String sourceInstitution, String username);
    
    public ResponseEntity GetDisputes(String institutioncode, int page, int limit);
    
    public ResponseEntity GetDisputes(int id);
    
    public ResponseEntity GetDisputes(int id, int status, String institutioncode, int page, int limit);

    /** FT disputes with status = -1 and resolved = 0 (arbitrated, awaiting settlement). */
    public ResponseEntity GetArbitratedDisputes(String institutioncode);
    
    public ResponseEntity SearchDisputes(
            String sessionid,
            String response_code,
            String source_bank,
            String beneficiary_bank,
            String dispute_status,
            String date_logged,
            String date_resolved,
            String timeline_date,
            int page,
            int limit
        );
    
    public ResponseEntity GetSettlements(int id);
    
    public ResponseEntity GetSettlements(String institution);
    
    public ResponseEntity GetDisputeTypes();
    
    public ResponseEntity ApproveSettlement(String sessiontoken, BigInteger id, String username, int status, String proof_of_reject_uri, String selectedDisputes, String type);
    
    public ResponseEntity SearchTransactionsForSessionIds(String sessionids);
    
    public ResponseEntity SearchTransactionsForSessionIds(String sessionids, String startDate, String endDate);
    
    public ResponseEntity SearchTransactions(String srcSessionid,
            String channelCode,
            String responseCode,
            String srcInstitutioncode,
            String destInstitutioncode,
            String minAmount,
            String maxAmount,
            String srcAccountNumber,
            String destAccountNumber,
            String startDate,
            String endDate, 
            int page, 
            int limit,
            boolean isCurrent,
            String userInstitutionCode);
    
    public ResponseEntity GetCommissions(String institutionCode, String startDate, String endDate);

    /** Count successful txns, apply tbl_charges, upsert into tbl_commission_paid. */
    public ResponseEntity GenerateCommissions(String institutionCode, String startDate, String endDate, boolean isCurrent);

    /** Weekly cron: Sun–Fri Africa/Lagos window, one GenerateCommissions call per tbl_charges institution. */
    public ResponseEntity GenerateWeeklyCommissionsCron();
    
    public ResponseEntity GetTimeoutRetries(String startDate, String endDate, int page, int limit);
    
    public ResponseEntity SearchTimeoutRetries(String session_id,
            String response_at_reprocess,
            String destination_institution_code,
            String startDate,
            String endDate, 
            int page, 
            int limit,
            String isProcessed);

    public ResponseEntity GetTsqRetries(String sessionId, String destinationInstitutionCode, int page, int limit);

    public ResponseEntity ResetTsqRetryCounter(String sessiontoken, String username, String sessionId);
    
    public ResponseEntity RequestTransactionStatusChange(String sessionid, String sessiontoken, String username, String status);
    public ResponseEntity UpdateTransactionStatusChange(String id, String sessiontoken, String username, String status);
}
