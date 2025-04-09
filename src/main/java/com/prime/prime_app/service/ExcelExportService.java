package com.prime.prime_app.service;

import com.prime.prime_app.entities.Client;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private static final String[] CLIENT_HEADERS = {
        "Client Name", "National ID", "Contact Number", "Insurance Type", "Location",
        "Agent Name", "Interaction Date", "Status"
    };

    public ByteArrayInputStream exportClientsToExcel(List<Client> clients) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Clients");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            for (int i = 0; i < CLIENT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(CLIENT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Create data rows
            int rowNum = 1;
            CellStyle dateStyle = createDateStyle(workbook);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (Client client : clients) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(client.getName());
                row.createCell(1).setCellValue(client.getNationalId());
                row.createCell(2).setCellValue(client.getPhoneNumber());
                row.createCell(3).setCellValue(client.getInsuranceType().toString());
                row.createCell(4).setCellValue(client.getLocation());
                row.createCell(5).setCellValue(client.getAgent().getName());
                
                Cell dateCell = row.createCell(6);
                dateCell.setCellValue(client.getTimeOfInteraction().format(dateFormatter));
                dateCell.setCellStyle(dateStyle);
                
                row.createCell(7).setCellValue(client.getPolicyStatus().toString());
            }

            // Auto-size columns
            for (int i = 0; i < CLIENT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export clients to Excel", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        return style;
    }
}