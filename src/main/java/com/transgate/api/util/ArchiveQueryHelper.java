    ///*
    // * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
    // * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
    // */
    //package com.transgate.api.util;
    //
    //import com.transgate.api.models.FullTransactionModel;
    //import java.time.LocalDateTime;
    //import java.time.format.DateTimeFormatter;
    //import java.util.ArrayList;
    //import java.util.HashMap;
    //import java.util.List;
    //import java.util.Map;
    //import java.util.logging.Logger;
    //import org.springframework.jdbc.core.JdbcTemplate;
    //
    ///**
    // *
    // * @author USER
    // */
    //public class ArchiveQueryHelper {
    //
    //    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    //
    //    /**
    //     * Fetches archive transactions, handling threshold splits if needed.
    //     */
    //    public static List<FullTransactionModel> fetchArchiveTransactions(
    //            LocalDateTime start,
    //            LocalDateTime end,
    //            LocalDateTime threshold,
    //            String startDate,
    //            String endDate,
    //            WhereBuilder wbBase,
    //            JdbcTemplate primaryJdbc,    // 10.83.1.13
    //            JdbcTemplate secondaryJdbc,  // 10.83.1.14
    //            String selectString,
    //            int limit,
    //            int offset,
    //            Logger logger,
    //            String marker
    //    ) {
    //        List<FullTransactionModel> histList = new ArrayList<>();
    //        DateTimeFormatter fmt = LOG_FMT;
    //
    //        if (start != null && end != null) {
    //            if (end.isBefore(threshold)) {
    //                // Only secondary DB
    //                logger.info(String.format("[%s] Query: ONLY secondary archive DB [start=%s, end=%s) < threshold=%s",
    //                        marker, start, end, threshold));
    //                histList = queryTrans(
    //                        secondaryJdbc,
    //                        selectString,
    //                        buildFullFromHist(wbBase, startDate, endDate),
    //                        wbBase.params(),
    //                        limit, offset
    //                );
    //            } else if (!end.isBefore(threshold)) {
    //                if (!start.isBefore(threshold)) {
    //                    // Only primary DB
    //                    logger.info(String.format("[%s] Query: ONLY primary archive DB [start=%s, end=%s), threshold=%s",
    //                            marker, start, end, threshold));
    //                    histList = queryTrans(
    //                            primaryJdbc,
    //                            selectString,
    //                            buildFullFromHist(wbBase, startDate, endDate),
    //                            wbBase.params(),
    //                            limit, offset
    //                    );
    //                } else {
    //                    // Spanning threshold, need both DBs
    //                    logger.info(String.format("[%s] Query: BOTH archive DBs: Secondary [start=%s, threshold=%s), Primary [threshold=%s, end=%s)",
    //                            marker, start, threshold, threshold, end));
    //
    //                    // Secondary DB: [start, threshold)
    //                    WhereBuilder wbSec = wbBase.cloneWithDateRange(startDate, threshold.format(fmt));
    //                    List<FullTransactionModel> secList = queryTrans(
    //                            secondaryJdbc,
    //                            selectString,
    //                            buildFullFromHist(wbSec, wbSec.getStartDate(), wbSec.getEndDate()),
    //                            wbSec.params(),
    //                            limit, offset
    //                    );
    //
    //                    // Primary DB: [threshold, end)
    //                    WhereBuilder wbPri = wbBase.cloneWithDateRange(threshold.format(fmt), endDate);
    //                    List<FullTransactionModel> priList = queryTrans(
    //                            primaryJdbc,
    //                            selectString,
    //                            buildFullFromHist(wbPri, wbPri.getStartDate(), wbPri.getEndDate()),
    //                            wbPri.params(),
    //                            limit, offset
    //                    );
    //
    //                    histList.addAll(secList);
    //                    histList.addAll(priList);
    //                }
    //            }
    //        } else {
    //            // Fallback: only primary
    //            logger.info(String.format("[%s] Fallback: Only primary archive DB as start or end is missing", marker));
    //            histList = queryTrans(
    //                    primaryJdbc,
    //                    selectString,
    //                    buildFullFromHist(wbBase, startDate, endDate),
    //                    wbBase.params(),
    //                    limit, offset
    //            );
    //        }
    //
    //        return histList;
    //    }
    //
    //    /**
    //     * Aggregates archive transactions, handling threshold splits if needed.
    //     * Returns a combined aggregation Map.
    //     */
    //    public static Map<String, Object> aggregateArchiveTransactions(
    //            LocalDateTime start,
    //            LocalDateTime end,
    //            LocalDateTime threshold,
    //            String startDate,
    //            String endDate,
    //            WhereBuilder wbBase,
    //            JdbcTemplate primaryJdbc,
    //            JdbcTemplate secondaryJdbc,
    //            String aggSQLBuilderTable,
    //            Logger logger,
    //            String marker
    //    ) {
    //        Map<String, Object> aggHist = new HashMap<>();
    //        DateTimeFormatter fmt = LOG_FMT;
    //
    //        if (start != null && end != null) {
    //            if (end.isBefore(threshold)) {
    //                // Only secondary
    //                logger.info(String.format("[%s] Aggregate: ONLY secondary archive DB [start=%s, end=%s) < threshold=%s",
    //                        marker, start, end, threshold));
    //                aggHist = runAggregate(
    //                        buildAggSQL(aggSQLBuilderTable, wbBase, startDate, endDate),
    //                        wbBase.params(),
    //                        secondaryJdbc
    //                );
    //            } else if (!end.isBefore(threshold)) {
    //                if (!start.isBefore(threshold)) {
    //                    // Only primary
    //                    logger.info(String.format("[%s] Aggregate: ONLY primary archive DB [start=%s, end=%s), threshold=%s",
    //                            marker, start, end, threshold));
    //                    aggHist = runAggregate(
    //                            buildAggSQL(aggSQLBuilderTable, wbBase, startDate, endDate),
    //                            wbBase.params(),
    //                            primaryJdbc
    //                    );
    //                } else {
    //                    // Split - both DBs
    //                    logger.info(String.format("[%s] Aggregate: BOTH archive DBs: Secondary [start=%s, threshold=%s), Primary [threshold=%s, end=%s)",
    //                            marker, start, threshold, threshold, end));
    //                    // Secondary
    //                    WhereBuilder wbSec = wbBase.cloneWithDateRange(startDate, threshold.format(fmt));
    //                    Map<String, Object> aggSec = runAggregate(
    //                            buildAggSQL(aggSQLBuilderTable, wbSec, wbSec.getStartDate(), wbSec.getEndDate()),
    //                            wbSec.params(),
    //                            secondaryJdbc
    //                    );
    //                    // Primary
    //                    WhereBuilder wbPri = wbBase.cloneWithDateRange(threshold.format(fmt), endDate);
    //                    Map<String, Object> aggPri = runAggregate(
    //                            buildAggSQL(aggSQLBuilderTable, wbPri, wbPri.getStartDate(), wbPri.getEndDate()),
    //                            wbPri.params(),
    //                            primaryJdbc
    //                    );
    //                    aggHist = combineAggs(aggSec, aggPri);
    //                }
    //            }
    //        } else {
    //            // Fallback: only primary
    //            logger.info(String.format("[%s] Aggregate fallback: Only primary archive DB as start or end is missing", marker));
    //            aggHist = runAggregate(
    //                    buildAggSQL(aggSQLBuilderTable, wbBase, startDate, endDate),
    //                    wbBase.params(),
    //                    primaryJdbc
    //            );
    //        }
    //        return aggHist;
    //    }
    //    
    //    // Builds the aggregation SQL
    //    public static String buildAggSQL(String table, WhereBuilder wb, String startDate, String endDate) {
    //        wb.addDateRange(startDate, endDate);
    //        return "SELECT SUM(a.amount) as totalValue, COUNT(a.id) as totalRecords, "
    //                + "AVG(CASE WHEN a.response_code = '00' THEN 1 ELSE 0 END) * 100 AS successRate "
    //                + "FROM " + table + " a " + wb.build();
    //    }
    //
    //    // Runs aggregation and returns a map
    //    public static Map<String, Object> runAggregate(String sql, List<Object> params, JdbcTemplate template) {
    //        List<Map<String, Object>> results = template.queryForList(sql, params.toArray());
    //        if (results == null || results.isEmpty()) return new HashMap<>();
    //        return results.get(0);
    //    }
    //
    //    // Combines two aggregation maps
    //    public static Map<String, Object> combineAggs(Map<String, Object> aggA, Map<String, Object> aggB) {
    //        double aVal = safeDouble(aggA, "totalValue");
    //        double bVal = safeDouble(aggB, "totalValue");
    //        int aRec = safeInt(aggA, "totalRecords");
    //        int bRec = safeInt(aggB, "totalRecords");
    //        double aRate = safeDouble(aggA, "successRate");
    //        double bRate = safeDouble(aggB, "successRate");
    //
    //        int totalRecords = aRec + bRec;
    //        double successRate = (totalRecords > 0) ? ((aRate * aRec) + (bRate * bRec)) / totalRecords : 0.0;
    //        double totalValue = aVal + bVal;
    //
    //        Map<String, Object> result = new HashMap<>();
    //        result.put("totalValue", totalValue);
    //        result.put("totalRecords", totalRecords);
    //        result.put("successRate", successRate);
    //        return result;
    //    }
    //
    //    // Safe extractors
    //    public static double safeDouble(Map<String, Object> map, String key) {
    //        if (map == null) return 0.0;
    //        Object value = map.get(key);
    //        if (value instanceof Number) {
    //            return ((Number) value).doubleValue();
    //        } else if (value != null) {
    //            try {
    //                return Double.parseDouble(value.toString());
    //            } catch (NumberFormatException e) {
    //            }
    //        }
    //        return 0.0;
    //    }
    //
    //    public static int safeInt(Map<String, Object> map, String key) {
    //        if (map == null) return 0;
    //        Object value = map.get(key);
    //        if (value instanceof Number) {
    //            return ((Number) value).intValue();
    //        } else if (value != null) {
    //            try {
    //                return Integer.parseInt(value.toString());
    //            } catch (NumberFormatException e) {
    //            }
    //        }
    //        return 0;
    //    }
    //}
    //
