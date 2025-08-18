package org.bluestarcloud.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class ExcelService {
    private static final Logger logger = LogManager.getLogger(ExcelService.class);

    @Autowired
    ExcelStreamingReader excelStreamingReader;

    public ExcelSheetData getExcelData(MultipartFile file, int sheetIndex) throws Exception {
        logger.info("Starting to get data from excel");
        try (InputStream inputStream = file.getInputStream()) {
            return excelStreamingReader.readExcel(inputStream, sheetIndex);
        }
    }

    protected double parseDoubleSafe(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            logger.error("Invalid number format: {}", value);
            throw new IllegalStateException("Failed to process file", e);
        }
    }

    protected void iterateOverDataAndCreateSheet(Sheet sheet, Map<Long, Object[]> data, CellStyle headerCellStyle,
                                                 Map<String, CellStyle> highlightCellStyle, List<Long> headerList, List<Long> highlightList) {
        //Iterate over data and write to sheet
        Set<Long> keySet = data.keySet();
        int rowNum = 0;
        for (Long key : keySet) {
            Row row = sheet.createRow(rowNum++);
            Object[] objArr = data.get(key);
            int cellNum = 0;
            for (Object obj : objArr) {
                Cell cell = row.createCell(cellNum++);
                if (headerList.contains(key))
                    cell.setCellStyle(headerCellStyle);
                else if (highlightList.contains(key)) {
                    CellStyle yellowCellStyle = highlightCellStyle.get(CellStyles.YELLOW);
                    cell.setCellStyle(yellowCellStyle);
                } else {
                    CellStyle cellStyle = highlightCellStyle.get(CellStyles.NORMAL);
                    cell.setCellStyle(cellStyle);
                }
                if (obj instanceof String)
                    cell.setCellValue((String) obj);
                else if (obj instanceof Integer)
                    cell.setCellValue((Integer) obj);
                else if (obj instanceof Long)
                    cell.setCellValue((Long) obj);
                else if (obj instanceof Map) {
                    Optional<?> keyObj = ((Map<?, ?>) obj).keySet().stream().findFirst();
                    if (keyObj.isPresent()) {
                        Object k = keyObj.get();
                        if (k instanceof String) {
                            CellStyle cellStyle = highlightCellStyle.get(k);
                            if (cellStyle == null) {
                                cellStyle = highlightCellStyle.get(CellStyles.NORMAL);
                            }
                            String v = (String) ((Map<?, ?>) obj).get(k);
                            cell.setCellValue(v);
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
            }
        }
    }

    protected CellStyle getCellStyle(SXSSFWorkbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setFont(font);
        return cellStyle;
    }

    protected CellStyle getHeaderCellStyle(SXSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();

        headerFont.setFontHeight((short) (9 * 20));
        headerFont.setBold(true);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    protected CellStyle getYellowCellStyle(XSSFWorkbook workbook) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setFontHeight((short) (9 * 20));
        headerFont.setBold(true);
        highlightCellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(headerFont);
        return highlightCellStyle;
    }

    protected CellStyle getRedCellStyle(XSSFWorkbook workbook) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        font.setColor(IndexedColors.DARK_RED.getIndex());
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(255, 200, 200), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getAmberCellStyle(XSSFWorkbook workbook) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        font.setColor(IndexedColors.BROWN.getIndex());
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(255, 231, 155), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getGreenCellStyle(XSSFWorkbook workbook) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(200, 240, 200), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }


    protected CellStyle getPinkCellStyle(XSSFWorkbook workbook) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
//        font.setColor(IndexedColors.MAROON.getIndex());
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(255, 153, 204), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getBlueCellStyle(XSSFWorkbook workbook) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
//        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(153, 204, 255), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getLYellowCellStyle(XSSFWorkbook workbook) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
//        font.setColor(IndexedColors.MAROON.getIndex());
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(255, 255, 204), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getLBlueCellStyle(XSSFWorkbook workbook, boolean isHeader) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        font.setBold(isHeader);
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(192, 230, 245), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getLGreenCellStyle(XSSFWorkbook workbook, boolean isHeader) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        font.setBold(isHeader);
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(218, 242, 208), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getLPinkCellStyle(XSSFWorkbook workbook, boolean isHeader) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        font.setBold(isHeader);
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(242, 206, 239), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getLOrangeCellStyle(XSSFWorkbook workbook, boolean isHeader) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        font.setBold(isHeader);
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(251, 226, 213), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }

    protected CellStyle getBGreenCellStyle(XSSFWorkbook workbook, boolean isHeader) {
        CellStyle highlightCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeight((short) (9 * 20));
        font.setBold(isHeader);
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor bgColor = new XSSFColor(new java.awt.Color(181, 230, 162), colorMap);
        highlightCellStyle.setFillForegroundColor(bgColor);
        highlightCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightCellStyle.setBorderBottom(BorderStyle.THIN);
        highlightCellStyle.setBorderLeft(BorderStyle.THIN);
        highlightCellStyle.setBorderRight(BorderStyle.THIN);
        highlightCellStyle.setBorderTop(BorderStyle.THIN);
        highlightCellStyle.setAlignment(HorizontalAlignment.CENTER);
        highlightCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        highlightCellStyle.setFont(font);
        return highlightCellStyle;
    }
}
