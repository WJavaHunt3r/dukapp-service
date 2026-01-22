package com.ktk.workhuservice.service.microsoft;

import com.ktk.workhuservice.data.activity.Activity;
import com.ktk.workhuservice.data.activityitems.ActivityItem;
import com.ktk.workhuservice.enums.TransactionType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MicrosoftUtils {

    public static String changeSpecChars(String text) {
        for (String entry : getEngChar().keySet()) {
            text = text.replaceAll(entry, getEngChar().get(entry));
        }
        return text;
    }

    static private Map<String, String> getEngChar() {
        Map<String, String> map = new HashMap<>();
        map.put("ö", "o");
        map.put("ü", "u");
        map.put("ó", "o");
        map.put("ő", "o");
        map.put("ú", "u");
        map.put("ű", "u");
        map.put("é", "e");
        map.put("á", "a");
        map.put("í", "i");
        map.put("-", "_");
        map.put(" ", "_");
        return map;
    }

    public static String formatDate(LocalDate date) {
        return formatDate(date, "yyyy.MM.dd");
    }

    public static String formatDate(LocalDate date, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }

    public static String createXlsxFromActivity(Activity activity, List<ActivityItem> registrations, double fullHours, InputStream file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet sheet = workbook.getSheetAt(0);

        CellStyle style = workbook.createCellStyle();
        CreationHelper ch = workbook.getCreationHelper();
        style.setDataFormat(ch.createDataFormat().getFormat("#,##0 Ft"));

        var row1 = sheet.getRow(1);
        row1.getCell(2).setCellValue(activity.getEmployer().getFullName());
        row1.getCell(5).setCellValue(formatDate(activity.getActivityDateTime().toLocalDate()));

        var row2 = sheet.getRow(2);
        row2.getCell(2).setCellValue(activity.getDescription());
        row2.getCell(5).setCellValue(fullHours);

        var row3 = sheet.getRow(3);
        row3.getCell(2).setCellValue(activity.getResponsible().getFullName());
        row3.getCell(5).setCellValue(
                activity.getTransactionType().equals(TransactionType.HOURS) ? fullHours * 3000 :
                        activity.getTransactionType().equals(TransactionType.DUKA_MUNKA_2000) ? fullHours * 2000 :
                                activity.getTransactionType().equals(TransactionType.DUKA_MUNKA) ? fullHours * 1000 : 0
        );
        row3.getCell(5).setCellStyle(style);

        for (int i = 0; i < registrations.size(); i++) {
            var reg = registrations.get(i);
            var currRow = sheet.getRow(5 + i);
            currRow.getCell(0).setCellValue(i + 1);
            currRow.getCell(1).setCellValue(reg.getUser().getMyShareID());
            currRow.createCell(2).setCellValue(reg.getUser().getFullName());
            currRow.getCell(3).setCellValue(reg.getUser().getAge());
            currRow.getCell(4).setCellValue(reg.getHours());
            currRow.getCell(5).setCellValue(
                    activity.getTransactionType().equals(TransactionType.HOURS) ? reg.getHours() * 3000 :
                    activity.getTransactionType().equals(TransactionType.DUKA_MUNKA_2000) ? reg.getHours() * 2000 :
                            activity.getTransactionType().equals(TransactionType.DUKA_MUNKA) ? reg.getHours() * 1000 : 0
            );
            currRow.getCell(5).setCellStyle(style);
        }

        File tempFile = File.createTempFile("temp", ".xlsx");

        FileOutputStream outputStream = new FileOutputStream(tempFile.getPath());
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();

        return tempFile.getPath();
    }

}
