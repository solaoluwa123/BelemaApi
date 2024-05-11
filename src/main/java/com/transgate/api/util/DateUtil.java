/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
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
    
    public Boolean canSendDisputeReminder() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek >= 1 && dayOfWeek < 7 && !isDayHoliday(-1))
            return true;
        else 
            return false;
    }
    
    public int daysAgo(String dateString) {
        // Parse the input date string to LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime givenDate = LocalDateTime.parse(dateString, formatter);

        // Get the current date and time
        LocalDateTime currentDate = LocalDateTime.now();

        // Calculate the difference in days
        long daysDifference = ChronoUnit.DAYS.between(givenDate.toLocalDate(), currentDate.toLocalDate());
        
        return (int) daysDifference;
    }
}
