package com.prime.prime_app.service;

import com.prime.prime_app.entities.Client;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {

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

    public ByteArrayInputStream exportClientsToPdf(List<Client> clients) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Client Reports", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Create table
            PdfPTable table = new PdfPTable(CLIENT_HEADERS.length);
            table.setWidthPercentage(100);
            
            // Set headers
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            for (String header : CLIENT_HEADERS) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBackgroundColor(new Color(41, 128, 185)); // Blue header
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(5);
                table.addCell(headerCell);
            }
            
            // Add data rows
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            for (Client client : clients) {
                addCell(table, client.getName(), cellFont);
                addCell(table, client.getNationalId(), cellFont);
                addCell(table, client.getPhoneNumber(), cellFont);
                addCell(table, client.getInsuranceType().toString(), cellFont);
                addCell(table, client.getLocation(), cellFont);
                addCell(table, client.getAgent().getName(), cellFont);
                addCell(table, client.getTimeOfInteraction().format(dateFormatter), cellFont);
                addCell(table, client.getPolicyStatus().toString(), cellFont);
            }
            
            document.add(table);
            document.close();
            return new ByteArrayInputStream(out.toByteArray());
            
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to export clients to PDF", e);
        }
    }
    
    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
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