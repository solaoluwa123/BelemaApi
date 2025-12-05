/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    
//    public int getDisputeTimeLineDate() {
//        int timeLineDate = 1;
//        Calendar calendar = Calendar.getInstance();
//        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
//        switch(dayOfWeek) {
//            case 6:
//                timeLineDate = 3;
//                break;
//            case 7:
//            case 1:
//                timeLineDate = 2;
//                break;
//            default:
//                timeLineDate = 1;
//                break;
//        }
//        if (isDayHoliday(timeLineDate))
//            timeLineDate++;
//
//        return timeLineDate;
//    }


    // Returns the number of *calendar* days from today to the date that is `businessDays` ahead,
// counting only days that are not weekend and not holiday.
    public int getDisputeTimeLineDate(int businessDays) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate due = today;
        int added = 0;
        while (added < businessDays) {
            due = due.plusDays(1);                 // start counting from tomorrow
            if (!isWeekend(due) && !isHoliday(due)) {
                added++;                            // count only business days
            }
        }
        return (int) ChronoUnit.DAYS.between(today, due);
    }

    // Your original no-arg version: "next two business days"
    public int getDisputeTimeLineDate() {
        return getDisputeTimeLineDate(2);
    }

    private boolean isWeekend(LocalDate d) {
        DayOfWeek w = d.getDayOfWeek();
        return w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY;
    }

    // Plug this into your real holiday calendar
//    private boolean isHoliday(LocalDate d) {
//        return isDayHolidayChrono(d);
//    }

    // Top-level (cache per year to avoid recomputing each call)
    private static final Map<Integer, Set<LocalDate>> HOLIDAY_CACHE = new ConcurrentHashMap<>();

    private boolean isHoliday(LocalDate d) {
        int y = d.getYear();
        Set<LocalDate> holidays = HOLIDAY_CACHE.computeIfAbsent(y, this::buildHolidaysForYear);
        return holidays.contains(d);
    }

    private Set<LocalDate> buildHolidaysForYear(int year) {
        Set<LocalDate> h = new HashSet<>();

        // Fixed-date Nigerian public holidays (update if your policy differs)
        h.add(LocalDate.of(year, 1, 1));   // New Year's Day
        h.add(LocalDate.of(year, 5, 1));   // Labour Day
        h.add(LocalDate.of(year, 6, 12));  // Democracy Day
        h.add(LocalDate.of(year, 12, 25)); // Christmas Day
        h.add(LocalDate.of(year, 12, 26)); // Boxing Day

        // Easter-based (Western/Gregorian) — compute once and derive
        LocalDate easter = easterSunday(year);
        h.add(easter.minusDays(2)); // Good Friday
        h.add(easter.plusDays(1));  // Easter Monday

        // Eid dates (Id el-Fitr, Id el-Kabir) vary yearly — load from config/DB:
        h.addAll(loadEidDatesFromDbOrConfig(year));

        // Observed days when a fixed holiday falls on weekend (if your org observes)
        h.addAll(applyObservedRules(h, year));

        return h;
    }

    private LocalDate easterSunday(int year) { // Anonymous Gregorian algorithm
        int a = year % 19;
        int b = year / 100, c = year % 100;
        int d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19*a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4;
        int l = (32 + 2*e + 2*i - h - k) % 7;
        int m = (a + 11*h + 22*l) / 451;
        int month = (h + l - 7*m + 114) / 31;           // 3=March, 4=April
        int day   = ((h + l - 7*m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    private Set<LocalDate> loadEidDatesFromDbOrConfig(int year) {
        // TODO: pull from your DB or config (they shift yearly). For now, return empty.
        return Collections.emptySet();
    }

    private Set<LocalDate> applyObservedRules(Set<LocalDate> base, int year) {
        Set<LocalDate> observed = new HashSet<>();
        for (LocalDate d : base) {
            DayOfWeek w = d.getDayOfWeek();
            if (w == DayOfWeek.SATURDAY) observed.add(d.plusDays(2)); // observe Monday
            else if (w == DayOfWeek.SUNDAY) observed.add(d.plusDays(1));
        }
        return observed;
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
