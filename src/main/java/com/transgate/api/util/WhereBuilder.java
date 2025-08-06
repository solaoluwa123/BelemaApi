/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.transgate.api.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author USER
 */
public class WhereBuilder {
    private final StringBuilder where = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private boolean hasCondition = false;
    private String startDate = null, endDate = null;
    
    public void addDateRange(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        if (startDate != null && !startDate.isBlank())
            add("a.transaction_date_time >= ?", startDate);
        if (endDate != null && !endDate.isBlank())
            add("a.transaction_date_time < ?", endDate);
    }

    public WhereBuilder cloneWithDateRange(String newStartDate, String newEndDate) {
        WhereBuilder clone = new WhereBuilder();
        clone.where.append(this.where); // Only non-date filters
        clone.params.addAll(this.params);
        clone.hasCondition = this.hasCondition;
        clone.addDateRange(newStartDate, newEndDate);
        return clone;
    }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }

    public void add(String condition, Object value) {
        if (value == null) return;
        if (value instanceof String && ((String)value).isBlank()) return;
        if (!hasCondition) {
            where.append(" WHERE ");
            hasCondition = true;
        } else {
            where.append(" AND ");
        }
        where.append(condition);
        params.add(value);
    }

    public void addRaw(String rawCondition) {
        if (!hasCondition) {
            where.append(" WHERE ");
            hasCondition = true;
        } else {
            where.append(" AND ");
        }
        where.append(rawCondition);
    }

    public String build() { return hasCondition ? where.toString() : ""; }
    public List<Object> params() { return params; }
}

