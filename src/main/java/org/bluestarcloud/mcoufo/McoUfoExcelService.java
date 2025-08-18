package org.bluestarcloud.mcoufo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bluestarcloud.common.CellStyles;
import org.bluestarcloud.common.ExcelService;
import org.bluestarcloud.common.ExcelSheetData;
import org.bluestarcloud.common.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class McoUfoExcelService extends ExcelService {

    private static final Logger logger = LogManager.getLogger(McoUfoExcelService.class);

    @Autowired
    ExcelSheetData salesOrderData;

    public List<SalesOrders> getSalesOrdersData(MultipartFile file) throws Exception {
        List<SalesOrders> salesOrdersList = new ArrayList<>();

        salesOrderData = getExcelData(file, 0);

        List<Map<String, String>> salesOrderRows = salesOrderData.getRows();

        salesOrderRows.forEach(salesOrder -> {
            SalesOrders salesOrders = new SalesOrders(salesOrder.get("Sales Order"), salesOrder.get("SO Item Date"), salesOrder.get("Material NO"), salesOrder.get("Part Desc."),
                    parseDoubleSafe(salesOrder.get("UFO Qty.")), salesOrder.get("UOM"), salesOrder.get("Division"),
                    salesOrder.get("Plant"), salesOrder.get("Plant Desc"), parseDoubleSafe(salesOrder.get("UFO Val.")),
                    salesOrder.get("Sold to Party Name"), salesOrder.get("Sales Employee Name"), salesOrder.get("Sales Office"), salesOrder.get("Sales Office Desc"),
                    salesOrder.get("Sales Order Type"));
            salesOrdersList.add(salesOrders);
        });
        return salesOrdersList;
    }

    public List<PartAvailability> getPartAvailabilityData(MultipartFile file) throws Exception {
        List<PartAvailability> partAvailabilityList = new ArrayList<>();

        ExcelSheetData partAvailabilityData = getExcelData(file, 1);
        List<Map<String, String>> partAvailabilityRows = partAvailabilityData.getRows();

        partAvailabilityRows.forEach(part -> {
            PartAvailability partAvailability1 = new PartAvailability(part.get("Plant"), part.get("Storage location"), part.get("Material"),
                    part.get("Material description"), parseDoubleSafe(part.get("Unrestricted")));
            partAvailabilityList.add(partAvailability1);
        });
        return partAvailabilityList;
    }

    public List<GitTransit> getGitTransitData(MultipartFile file) throws Exception {
        List<GitTransit> gitTransitList = new ArrayList<>();

        ExcelSheetData gitTransitData = getExcelData(file, 2);
        List<Map<String, String>> gitTransitRows = gitTransitData.getRows();

        gitTransitRows.forEach(git -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String dateStr = git.get("PGI Date");
            if (dateStr != null && !dateStr.isBlank()) {
                try {
                    LocalDate pgiDate = LocalDate.parse(dateStr, formatter);
                    LocalDate dateBeforeThirtyDays = LocalDate.now().minusDays(30);
                    if (pgiDate.isAfter(dateBeforeThirtyDays)) {
                        GitTransit gitTransit = new GitTransit(git.get("Plnt"), git.get("Material"), git.get("SPlt"),
                                parseDoubleSafe(git.get("GIT Qty")));
                        gitTransitList.add(gitTransit);
                    }
                } catch (DateTimeParseException e) {
                    logger.error("Invalid date format: {}", dateStr);
                    throw new IllegalStateException("Failed to process file", e);
                }
            }
        });
        return gitTransitList;
    }

    protected Workbook writeProcessedSalesOrdersToExcel(List<SalesOrders> processedSalesOrders) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("UFO Data");
        Map<Long, Object[]> data = new TreeMap<>();
        List<String> headerList = new ArrayList<>();
        CellStyle headerCellStyle = getHeaderCellStyle(workbook);
        CellStyle cellStyle = getCellStyle(workbook);

        Map<String, CellStyle> highlightCellStyle = new HashMap<>();
        highlightCellStyle.put(CellStyles.NORMAL, cellStyle);

        Map<Integer, String> salesOrderHeaderMap = salesOrderData.getHeaderMap();

        for (int i = 0; i < salesOrderHeaderMap.size(); i++) {
            headerList.add(salesOrderHeaderMap.get(i));
        }
        headerList.add("MCO Remarks");

        Object[] headerArray = headerList.toArray();
        data.put(1L, headerArray);

        List<Long> headers = new ArrayList<>();
        headers.add(1L);

        long rowCount = 2L;
        for (SalesOrders salesOrders : processedSalesOrders) {
            String ufoQty = Utils.getCorrectDecimalForDouble(salesOrders.getUfoQty(), 3);
            String ufoValue = Utils.getCorrectDecimalForDouble(salesOrders.getUfoValue(), 2);
            Object[] row = new Object[]{salesOrders.getSalesOrder(), salesOrders.getSoItemDate(), salesOrders.getMaterialNo(),
                    salesOrders.getPartDesc(), ufoQty, salesOrders.getUom(), salesOrders.getDivision(),
                    salesOrders.getPlant(), salesOrders.getPlantDesc(), ufoValue,
                    salesOrders.getSoldToPartyName(), salesOrders.getSalesEmployeeName(), salesOrders.getSalesOffice(),
                    salesOrders.getSalesOfficeDesc(), salesOrders.getSalesOrderType(), salesOrders.getMcoRemarks()};
            data.put(rowCount++, row);
        }
        iterateOverDataAndCreateSheet(sheet, data, headerCellStyle, highlightCellStyle, headers, new ArrayList<>());
//        for (int i = 0; i < headerList.size(); i++)
//            sheet.autoSizeColumn(i);

        logger.info("Processed workbook created");
        return workbook;
    }
}
