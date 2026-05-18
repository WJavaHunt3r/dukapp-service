package com.ktk.dukappservice.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ServiceUtils {
    public static LocalDateTime getDateTo(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return LocalDateTime.now();
        }
        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
        return LocalDateTime.of(date.getYear(), date.getMonth(), YearMonth.of(date.getYear(), date.getMonth()).atEndOfMonth().getDayOfMonth(), 0, 0).plusDays(1);
    }

    public static LocalDateTime getDateFrom(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return LocalDateTime.of(2024, 1, 1, 0, 0);
        }

        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
        return LocalDateTime.of(date.getYear(), date.getMonth(), 1, 0, 0);
    }
}
