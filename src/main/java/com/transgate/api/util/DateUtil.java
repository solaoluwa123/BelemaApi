/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Locale;

/**
 *
 * @author Makintola
 */
public class DateUtil {
    
    public Boolean isDayHoliday(int num) {
        String[] publicHolidays = {
            "New Year's Day",
            "Good Friday",
            "Easter Monday",
            "Labour Day",
            "Democracy Day",
            "Id el-Fitr",
            "Id el-Kabir",
            "Christmas Day",
            "Boxing Day"
        };
        LocalDate now = LocalDate.now();
        // Get the date for tomorrow
        LocalDate future = now.plusDays(num);
        // Check if tomorrow's date matches any of the public holidays
        for (String holiday : publicHolidays) {
            if (future.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).equals(holiday)) {
                return true;
            }
        }
        return false;
    }
    
    public int getDisputeTimeLineDate() {
        int timeLineDate = 1;
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch(dayOfWeek) {
            case 6:
                timeLineDate = 3;
                break;
            case 7:
            case 1:
                timeLineDate = 2;
                break;
            default:
                timeLineDate = 1;
                break;
        }
        if (isDayHoliday(timeLineDate)) 
            timeLineDate++;
        
        return timeLineDate;
    }
}
