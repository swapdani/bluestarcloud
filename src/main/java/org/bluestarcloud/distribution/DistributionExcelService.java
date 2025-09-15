package org.bluestarcloud.distribution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bluestarcloud.common.CellStyles;
import org.bluestarcloud.common.ExcelService;
import org.bluestarcloud.common.ExcelSheetData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DistributionExcelService extends ExcelService {
    private static final Logger logger = LogManager.getLogger(DistributionExcelService.class);

    public List<DistributionStock> getDistributionStock(MultipartFile file) throws Exception {
        ExcelSheetData excelSheetData = getExcelData(file, 0);
        List<Map<String, String>> distributionStockRows = excelSheetData.getRows();
        List<DistributionStock> distributionStockList = new ArrayList<>();

        distributionStockRows.forEach(row -> {
            DistributionStock stock = new DistributionStock(row.get("Plant"), row.get("Material"), row.get("Material Description"), parseDoubleSafe(row.get("Unrestricted")));
            distributionStockList.add(stock);
        });
        return distributionStockList;
    }

    public List<DistributionStock> getConsumption(MultipartFile file) throws Exception {
        ExcelSheetData excelSheetData = getExcelData(file, 1);
        List<DistributionStock> distributionStockList = new ArrayList<>();
        List<Map<String, String>> distributionStockRows = excelSheetData.getRows();
        distributionStockRows.forEach(row -> {
            DistributionStock stock = new DistributionStock(row.get("Plant"), row.get("Storage location"), row.get("Material"), row.get("Material Description"), parseDoubleSafe(row.get("Quantity")), row.get("Posting Date"), row.get("Material Group"));
            distributionStockList.add(stock);
        });
        return distributionStockList;
    }

    public List<GitTransit> getGitTransitListFromExcel(MultipartFile file) throws Exception {
        ExcelSheetData excelSheetData = getExcelData(file, 2);
        List<GitTransit> gitTransitList = new ArrayList<>();
        List<Map<String, String>> gitTransitRows = excelSheetData.getRows();
        gitTransitRows.forEach(row -> {
            String obdDate = row.get("OBD Creation Date");
            if (obdDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                LocalDate pgiDate = LocalDate.parse(obdDate, formatter);
                LocalDate dateBeforeFifteenDays = LocalDate.now().minusDays(15);
                String storageLocation = row.get("Storage location");

                if (storageLocation.startsWith("S") && pgiDate.isAfter(dateBeforeFifteenDays)) {
                    GitTransit git = new GitTransit(row.get("Plant"), row.get("Material"), parseDoubleSafe(row.get("GIT Qty")));
                    gitTransitList.add(git);
                }
            }
        });
        return gitTransitList;
    }

    public List<DistributionStock> getMaterialLeadTime(MultipartFile file) throws Exception {
        ExcelSheetData excelSheetData = getExcelData(file, 3);
        List<DistributionStock> distributionStockList = new ArrayList<>();
        List<Map<String, String>> materialLeadTimeRows = excelSheetData.getRows();
        materialLeadTimeRows.forEach(row -> {
            DistributionStock stock = new DistributionStock(row.get("Material"), row.get("Material Description"), row.get("Supplier"), Integer.parseInt(row.get("Lead Time in Months")));
            distributionStockList.add(stock);
        });
        return distributionStockList;
    }

    public List<PendingPO> getPendingPO(MultipartFile file) throws Exception {
        ExcelSheetData excelSheetData = getExcelData(file, 4);
        List<PendingPO> pendingPOList = new ArrayList<>();
        List<Map<String, String>> pendingPORows = excelSheetData.getRows();
        pendingPORows.forEach(row -> {
            PendingPO pendingPO = new PendingPO(row.get("Material"), parseDoubleSafe(row.get("Still to be delivered (qty)")));
            pendingPOList.add(pendingPO);
        });
        return pendingPOList;
    }

    public List<PendingPR> getPendingPR(MultipartFile file) throws Exception {
        ExcelSheetData excelSheetData = getExcelData(file, 5);
        List<PendingPR> pendingPRList = new ArrayList<>();
        List<Map<String, String>> pendingPRRows = excelSheetData.getRows();
        pendingPRRows.forEach(row -> {
            PendingPR pendingPR = new PendingPR(row.get("Material"), parseDoubleSafe(row.get("Quantity requested")), parseDoubleSafe(row.get("Quantity ordered")));
            pendingPRList.add(pendingPR);
        });
        return pendingPRList;
    }

    public List<PlantMapping> getPlantMappingFromExcel(MultipartFile file) throws Exception {
        ExcelSheetData excelSheetData = getExcelData(file, 6);
        List<PlantMapping> plantMappingList = new ArrayList<>();
        List<Map<String, String>> plantMappingRows = excelSheetData.getRows();
        plantMappingRows.forEach(row -> {
            PlantMapping plantMapping = new PlantMapping(row.get("Original plant code"), row.get("Inventory move to plant"), row.get("% of consumption to move"));
            plantMappingList.add(plantMapping);
        });
        return plantMappingList;
    }

    Workbook writeProcessedDistributionsToExcel(List<Distribution> distributionList, List<DistributionPRDetails> distributionPRDetailsList,
                                                Map<Integer, String> distributionHeaderMap, List<DistributionPlanning> distributionPlanningList,
                                                Map<Integer, String> distributionPlanningHeaders, List<DistributionSparesConsumption> distributionSparesConsumptionList, List<PlantMapping> plantMappingList) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet distributionSheet = workbook.createSheet("Distribution");
        Sheet distributionPRDetailsSheet = workbook.createSheet("DistributionPRDetails");
        Sheet distributionPlanningSheet = workbook.createSheet("DistributionPlanning");
        Sheet distributionSparesConsumptionSheet = workbook.createSheet("DistributionSparesConsumption");

        CellStyle headerCellStyle = getHeaderCellStyle(workbook);
        CellStyle cellStyle = getCellStyle(workbook);
        CellStyle yellowCellStyle = getYellowCellStyle(workbook);
        CellStyle lGreenCellStyle = getLGreenCellStyle(workbook, false);
        CellStyle lGreenHeaderCellStyle = getLGreenCellStyle(workbook, true);
        CellStyle lPurpleCellStyle = getLPurpleCellStyle(workbook, false);
        CellStyle lPurpleHeaderCellStyle = getLPurpleCellStyle(workbook, true);
        CellStyle lTurquoiseCellStyle = getLTurquoiseCellStyle(workbook, false);
        CellStyle lTurquoiseHeaderCellStyle = getLTurquoiseCellStyle(workbook, true);

        Map<String, CellStyle> highlightCellStyle = new HashMap<>();
        highlightCellStyle.put(CellStyles.NORMAL, cellStyle);
        highlightCellStyle.put(CellStyles.YELLOW, yellowCellStyle);
        highlightCellStyle.put(CellStyles.L_GREEN, lGreenCellStyle);
        highlightCellStyle.put(CellStyles.L_GREEN_HEADER, lGreenHeaderCellStyle);
        highlightCellStyle.put(CellStyles.L_PURPLE, lPurpleCellStyle);
        highlightCellStyle.put(CellStyles.L_PURPLE_HEADER, lPurpleHeaderCellStyle);
        highlightCellStyle.put(CellStyles.L_TURQUOISE, lTurquoiseCellStyle);
        highlightCellStyle.put(CellStyles.L_TURQUOISE_HEADER, lTurquoiseHeaderCellStyle);

        prepareDistributionSheet(distributionList, distributionHeaderMap, distributionSheet, headerCellStyle, highlightCellStyle);
        prepareDistributionPRDetailsSheet(distributionPRDetailsList, distributionPRDetailsSheet, highlightCellStyle);
        prepareDistributionPlanningSheet(distributionPlanningList, distributionPlanningHeaders, distributionPlanningSheet, headerCellStyle, highlightCellStyle);
        prepareDistributionSparesConsumptionSheet(distributionSparesConsumptionList, plantMappingList, distributionSparesConsumptionSheet, headerCellStyle, highlightCellStyle);
        return workbook;
    }

    private void prepareDistributionSparesConsumptionSheet(List<DistributionSparesConsumption> distributionSparesConsumptionList, List<PlantMapping> plantMappingList, Sheet distributionSparesConsumptionSheet,
                                                           CellStyle headerCellStyle, Map<String, CellStyle> highlightCellStyle) {
        List<Map<String, String>> topHeaderList = new ArrayList<>();
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.W11C));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.S16C));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.S06C));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.S36C));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.N66C));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.S54C));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.N33C));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));
        topHeaderList.add(getHighlightMap(CellStyles.NORMAL, ""));

        Map<Long, Object[]> data = new TreeMap<>();
        Object[] topHeaderArray = topHeaderList.toArray();
        data.put(1L, topHeaderArray);

        List<Map<String, String>> headerMapList = new ArrayList<>();
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.MATERIAL));
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.MATERIAL_DESC));
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.NPC));

        String percentage = plantMappingList.stream().filter(p -> Plants.W11C.name().equalsIgnoreCase(p.getPlant())).map(PlantMapping::getPercentage).findFirst().orElse("0");
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.CONSUMPTION));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.FORECAST.replace("<percentage>", percentage)));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.CURRENT_STOCK));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.STOCK_AVAILABILITY));

        percentage = plantMappingList.stream().filter(p -> Plants.S16C.name().equalsIgnoreCase(p.getPlant())).map(PlantMapping::getPercentage).findFirst().orElse("0");
        headerMapList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.CONSUMPTION));
        headerMapList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.FORECAST.replace("<percentage>", percentage)));
        headerMapList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.CURRENT_STOCK));
        headerMapList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.STOCK_AVAILABILITY));

        percentage = plantMappingList.stream().filter(p -> Plants.S06C.name().equalsIgnoreCase(p.getPlant())).map(PlantMapping::getPercentage).findFirst().orElse("0");
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.CONSUMPTION));
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.FORECAST.replace("<percentage>", percentage)));
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.CURRENT_STOCK));
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.STOCK_AVAILABILITY));

        percentage = plantMappingList.stream().filter(p -> Plants.S36C.name().equalsIgnoreCase(p.getPlant())).map(PlantMapping::getPercentage).findFirst().orElse("0");
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.CONSUMPTION));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.FORECAST.replace("<percentage>", percentage)));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.CURRENT_STOCK));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.STOCK_AVAILABILITY));

        percentage = plantMappingList.stream().filter(p -> Plants.N66C.name().equalsIgnoreCase(p.getPlant())).map(PlantMapping::getPercentage).findFirst().orElse("0");
        headerMapList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.CONSUMPTION));
        headerMapList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.FORECAST.replace("<percentage>", percentage)));
        headerMapList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.CURRENT_STOCK));
        headerMapList.add(getHighlightMap(CellStyles.L_PURPLE_HEADER, DistributionSparesConsumptionHeaders.STOCK_AVAILABILITY));

        percentage = plantMappingList.stream().filter(p -> Plants.S54C.name().equalsIgnoreCase(p.getPlant())).map(PlantMapping::getPercentage).findFirst().orElse("0");
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.CONSUMPTION));
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.FORECAST.replace("<percentage>", percentage)));
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.CURRENT_STOCK));
        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.STOCK_AVAILABILITY));

        percentage = plantMappingList.stream().filter(p -> Plants.N33C.name().equalsIgnoreCase(p.getPlant())).map(PlantMapping::getPercentage).findFirst().orElse("0");
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.CONSUMPTION));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.FORECAST.replace("<percentage>", percentage)));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.CURRENT_STOCK));
        headerMapList.add(getHighlightMap(CellStyles.L_GREEN_HEADER, DistributionSparesConsumptionHeaders.STOCK_AVAILABILITY));

        headerMapList.add(getHighlightMap(CellStyles.L_TURQUOISE_HEADER, DistributionSparesConsumptionHeaders.GRAND_TOTAL));

        Object[] headerArray = headerMapList.toArray();
        data.put(2L, headerArray);

        long rowCount = 3L;

        for (DistributionSparesConsumption distributionSparesConsumption : distributionSparesConsumptionList) {
            Object[] row = getDistributionSparesConsumptionRow(distributionSparesConsumption);
            data.put(rowCount++, row);
        }
        List<Long> headers = new ArrayList<>();
        headers.add(1L);
        headers.add(2L);

        iterateOverDataAndCreateSheet(distributionSparesConsumptionSheet, data, headerCellStyle, highlightCellStyle, headers, new ArrayList<>());
        distributionSparesConsumptionSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        distributionSparesConsumptionSheet.addMergedRegion(new CellRangeAddress(0, 0, 3, 6));
        distributionSparesConsumptionSheet.addMergedRegion(new CellRangeAddress(0, 0, 7, 10));
        distributionSparesConsumptionSheet.addMergedRegion(new CellRangeAddress(0, 0, 11, 14));
        distributionSparesConsumptionSheet.addMergedRegion(new CellRangeAddress(0, 0, 15, 18));
        distributionSparesConsumptionSheet.addMergedRegion(new CellRangeAddress(0, 0, 19, 22));
        distributionSparesConsumptionSheet.addMergedRegion(new CellRangeAddress(0, 0, 23, 26));
        distributionSparesConsumptionSheet.addMergedRegion(new CellRangeAddress(0, 0, 27, 30));

        for (int i = 0; i < headers.size(); i++) {
            ((SXSSFSheet) distributionSparesConsumptionSheet).trackColumnForAutoSizing(i);
            distributionSparesConsumptionSheet.autoSizeColumn(i);
        }
    }

    private Object[] getDistributionSparesConsumptionRow(DistributionSparesConsumption distributionSparesConsumption) {
        Map<String, Consumption> consumptionMap = distributionSparesConsumption.getPlantWiseConsumption();
        Consumption w11cConsumption = consumptionMap.get(Plants.W11C.name());
        Consumption s16cConsumption = consumptionMap.get(Plants.S16C.name());
        Consumption s06cConsumption = consumptionMap.get(Plants.S06C.name());
        Consumption s36cConsumption = consumptionMap.get(Plants.S36C.name());
        Consumption n66cConsumption = consumptionMap.get(Plants.N66C.name());
        Consumption s54cConsumption = consumptionMap.get(Plants.S54C.name());
        Consumption n33cConsumption = consumptionMap.get(Plants.N33C.name());

        return new Object[]{
                distributionSparesConsumption.getMaterial(), distributionSparesConsumption.getMaterialDescription(), distributionSparesConsumption.getNpc()
                , getHighlightMap(CellStyles.L_GREEN, w11cConsumption.getConsumption()), getHighlightMap(CellStyles.L_GREEN, w11cConsumption.getForecastNewRpc()), getHighlightMap(CellStyles.L_GREEN, w11cConsumption.getCurrentStockParentRpc()), getHighlightMap(CellStyles.L_GREEN, w11cConsumption.getStockAvailabilityStatus())
                , getHighlightMap(CellStyles.L_PURPLE, s16cConsumption.getConsumption()), getHighlightMap(CellStyles.L_PURPLE, s16cConsumption.getForecastNewRpc()), getHighlightMap(CellStyles.L_PURPLE, s16cConsumption.getCurrentStockParentRpc()), getHighlightMap(CellStyles.L_PURPLE, s16cConsumption.getStockAvailabilityStatus())
                , getHighlightMap(CellStyles.L_TURQUOISE, s06cConsumption.getConsumption()), getHighlightMap(CellStyles.L_TURQUOISE, s06cConsumption.getForecastNewRpc()), getHighlightMap(CellStyles.L_TURQUOISE, s06cConsumption.getCurrentStockParentRpc()), getHighlightMap(CellStyles.L_TURQUOISE, s06cConsumption.getStockAvailabilityStatus())
                , getHighlightMap(CellStyles.L_GREEN, s36cConsumption.getConsumption()), getHighlightMap(CellStyles.L_GREEN, s36cConsumption.getForecastNewRpc()), getHighlightMap(CellStyles.L_GREEN, s36cConsumption.getCurrentStockParentRpc()), getHighlightMap(CellStyles.L_GREEN, s36cConsumption.getStockAvailabilityStatus())
                , getHighlightMap(CellStyles.L_PURPLE, n66cConsumption.getConsumption()), getHighlightMap(CellStyles.L_PURPLE, n66cConsumption.getForecastNewRpc()), getHighlightMap(CellStyles.L_PURPLE, n66cConsumption.getCurrentStockParentRpc()), getHighlightMap(CellStyles.L_PURPLE, n66cConsumption.getStockAvailabilityStatus())
                , getHighlightMap(CellStyles.L_TURQUOISE, s54cConsumption.getConsumption()), getHighlightMap(CellStyles.L_TURQUOISE, s54cConsumption.getForecastNewRpc()), getHighlightMap(CellStyles.L_TURQUOISE, s54cConsumption.getCurrentStockParentRpc()), getHighlightMap(CellStyles.L_TURQUOISE, s54cConsumption.getStockAvailabilityStatus())
                , getHighlightMap(CellStyles.L_GREEN, n33cConsumption.getConsumption()), getHighlightMap(CellStyles.L_GREEN, n33cConsumption.getForecastNewRpc()), getHighlightMap(CellStyles.L_GREEN, n33cConsumption.getCurrentStockParentRpc()), getHighlightMap(CellStyles.L_GREEN, n33cConsumption.getStockAvailabilityStatus())
                , distributionSparesConsumption.getGrandTotal()
        };
    }

    private void prepareDistributionPlanningSheet(List<DistributionPlanning> distributionPlanningList, Map<Integer, String> distributionPlanningHeaders, Sheet distributionPlanningSheet, CellStyle headerCellStyle, Map<String, CellStyle> highlightCellStyle) {
        List<String> headerList = new ArrayList<>();
        for (int i = 0; i < distributionPlanningHeaders.size(); i++) {
            headerList.add(distributionPlanningHeaders.get(i));
        }
        Map<Long, Object[]> data = new TreeMap<>();
        Object[] headerArray = headerList.toArray();
        data.put(1L, headerArray);

        List<Long> headers = new ArrayList<>();
        headers.add(1L);

        long rowCount = 2L;
        for (DistributionPlanning distributionPlanning : distributionPlanningList) {
            Object[] row = getDistributionPlanningRow(distributionPlanning);
            data.put(rowCount++, row);
        }
        iterateOverDataAndCreateSheet(distributionPlanningSheet, data, headerCellStyle, highlightCellStyle, headers, new ArrayList<>());
        for (int i = 0; i < headers.size(); i++) {
            ((SXSSFSheet) distributionPlanningSheet).trackColumnForAutoSizing(i);
            distributionPlanningSheet.autoSizeColumn(i);
        }
    }

    private Object[] getDistributionPlanningRow(DistributionPlanning distributionPlanning) {
        return new Object[]{distributionPlanning.getMaterial()
                , distributionPlanning.getDescription()
                , distributionPlanning.getSupplier()
                , distributionPlanning.getStock()
                , distributionPlanning.getLeadTime()
                , distributionPlanning.getQtyToArrange()};
    }

    private void prepareDistributionSheet(List<Distribution> distributionList, Map<Integer, String> distributionHeaderMap, Sheet distributionSheet, CellStyle headerCellStyle, Map<String, CellStyle> highlightCellStyle) {
        List<String> headerList = new ArrayList<>();
        for (int i = 0; i < distributionHeaderMap.size(); i++) {
            headerList.add(distributionHeaderMap.get(i));
        }
        Map<Long, Object[]> data = new TreeMap<>();
        Object[] headerArray = headerList.toArray();
        data.put(1L, headerArray);

        List<Long> headers = new ArrayList<>();
        headers.add(1L);

        long rowCount = 2L;
        for (Distribution distribution : distributionList) {
            Object[] row = getDistributionRow(distribution);
            data.put(rowCount++, row);
        }
        iterateOverDataAndCreateSheet(distributionSheet, data, headerCellStyle, highlightCellStyle, headers, new ArrayList<>());
        for (int i = 0; i < headers.size(); i++) {
            ((SXSSFSheet) distributionSheet).trackColumnForAutoSizing(i);
            distributionSheet.autoSizeColumn(i);
        }
    }

    private void prepareDistributionPRDetailsSheet(List<DistributionPRDetails> distributionPRDetailsList, Sheet distributionPRDetailsSheet,
                                                   Map<String, CellStyle> highlightCellStyle) {
        Map<Long, Object[]> data = new TreeMap<>();
        boolean isHighlight = true;
        long rowCount = 1L;
        for (DistributionPRDetails distributionPRDetails : distributionPRDetailsList) {
            Object[] row = getDistributionPRDetailsRow(distributionPRDetails, isHighlight);
            if (isHighlight) {
                if (rowCount % 10 == 0) {
                    isHighlight = false;
                }
            } else {
                if (rowCount % 10 == 0) {
                    isHighlight = true;
                }
            }
            data.put(rowCount++, row);
        }
        iterateOverDataAndCreateSheet(distributionPRDetailsSheet, data, null, highlightCellStyle, new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < 8; i++) {
            ((SXSSFSheet) distributionPRDetailsSheet).trackColumnForAutoSizing(i);
            distributionPRDetailsSheet.autoSizeColumn(i);
        }
    }

    private Object[] getDistributionPRDetailsRow(DistributionPRDetails distributionPRDetails, boolean isHighlight) {
        if (isHighlight) {
            return new Object[]{getHighlightMap(distributionPRDetails.getU())
                    , getHighlightMap(distributionPRDetails.getMaterial())
                    , getHighlightMap(" ")
                    , getHighlightMap(distributionPRDetails.getStockToTransfer())
                    , getHighlightMap(distributionPRDetails.getFromPlant())
                    , getHighlightMap(distributionPRDetails.getPlant())
                    , getHighlightMap(distributionPRDetails.getS51())
                    , getHighlightMap(distributionPRDetails.getMANJUSHREE())
                    , getHighlightMap(distributionPRDetails.getMSL())};
        } else {
            return new Object[]{distributionPRDetails.getU()
                    , distributionPRDetails.getMaterial()
                    , " "
                    , distributionPRDetails.getStockToTransfer()
                    , distributionPRDetails.getFromPlant()
                    , distributionPRDetails.getPlant()
                    , distributionPRDetails.getS51()
                    , distributionPRDetails.getMANJUSHREE()
                    , distributionPRDetails.getMSL()};
        }
    }

    private Map<String, String> getHighlightMap(String stockToTransfer) {
        Map<String, String> stockToTransferMap = new HashMap<>();
        stockToTransferMap.put(CellStyles.YELLOW, stockToTransfer);
        return stockToTransferMap;
    }

    private Map<String, String> getHighlightMap(String color, String value) {
        Map<String, String> highlightMap = new HashMap<>();
        highlightMap.put(color, value);
        return highlightMap;
    }

    private Object[] getDistributionRow(Distribution distribution) {
        return new Object[]{distribution.getMaterial(), distribution.getMaterialDescription()
                , distribution.getW15cStock()
                , distribution.getW15cEligible()
                , distribution.getTotalStockToTransfer()
                , distribution.getTotalStock()
                , distribution.getTotalConsumption()
                , distribution.getCurrentStockAvailability()

                , distribution.getE06cStock(), distribution.getE06cInTransit(), distribution.getE06cConsumption(), getHighlightMap(distribution.getE06cStockToTransfer())
                , distribution.getE16cStock(), distribution.getE16cInTransit(), distribution.getE16cConsumption(), getHighlightMap(distribution.getE16cStockToTransfer())
                , distribution.getE26cStock(), distribution.getE26cInTransit(), distribution.getE26cConsumption(), getHighlightMap(distribution.getE26cStockToTransfer())
                , distribution.getE33cStock(), distribution.getE33cInTransit(), distribution.getE33cConsumption(), getHighlightMap(distribution.getE33cStockToTransfer())
                , distribution.getE41cStock(), distribution.getE41cInTransit(), distribution.getE41cConsumption(), getHighlightMap(distribution.getE41cStockToTransfer())
                , distribution.getN06cStock(), distribution.getN06cInTransit(), distribution.getN06cConsumption(), getHighlightMap(distribution.getN06cStockToTransfer())
                , distribution.getN13cStock(), distribution.getN13cInTransit(), distribution.getN13cConsumption(), getHighlightMap(distribution.getN13cStockToTransfer())
                , distribution.getN33cStock(), distribution.getN33cInTransit(), distribution.getN33cConsumption(), getHighlightMap(distribution.getN33cStockToTransfer())
                , distribution.getN34cStock(), distribution.getN34cInTransit(), distribution.getN34cConsumption(), getHighlightMap(distribution.getN34cStockToTransferW15C()), getHighlightMap(distribution.getN34cStockToTransferN33C())
                , distribution.getN48cStock(), distribution.getN48cInTransit(), distribution.getN48cConsumption(), getHighlightMap(distribution.getN48cStockToTransfer())
                , distribution.getN57cStock(), distribution.getN57cInTransit(), distribution.getN57cConsumption(), getHighlightMap(distribution.getN57cStockToTransfer())
                , distribution.getN66cStock(), distribution.getN66cInTransit(), distribution.getN66cConsumption(), getHighlightMap(distribution.getN66cStockToTransfer())
                , distribution.getN64cStock(), distribution.getN64cInTransit(), distribution.getN64cConsumption(), getHighlightMap(distribution.getN64cStockToTransferW15C()), getHighlightMap(distribution.getN64cStockToTransferN66C())
                , distribution.getN76cStock(), distribution.getN76cInTransit(), distribution.getN76cConsumption(), getHighlightMap(distribution.getN76cStockToTransfer())
                , distribution.getS06cStock(), distribution.getS06cInTransit(), distribution.getS06cConsumption(), getHighlightMap(distribution.getS06cStockToTransfer())
                , distribution.getS66cStock(), distribution.getS66cInTransit(), distribution.getS66cConsumption(), getHighlightMap(distribution.getS66cStockToTransferW15C()), getHighlightMap(distribution.getS66cStockToTransferS06C())
                , distribution.getS16cStock(), distribution.getS16cInTransit(), distribution.getS16cConsumption(), getHighlightMap(distribution.getS16cStockToTransfer())
                , distribution.getS49cStock(), distribution.getS49cInTransit(), distribution.getS49cConsumption(), getHighlightMap(distribution.getS49cStockToTransferW15C()), getHighlightMap(distribution.getS49cStockToTransferS16C())
                , distribution.getS26cStock(), distribution.getS26cInTransit(), distribution.getS26cConsumption(), getHighlightMap(distribution.getS26cStockToTransfer())
                , distribution.getS36cStock(), distribution.getS36cInTransit(), distribution.getS36cConsumption(), getHighlightMap(distribution.getS36cStockToTransfer())
                , distribution.getS33cStock(), distribution.getS33cInTransit(), distribution.getS33cConsumption(), getHighlightMap(distribution.getS33cStockToTransferW15C()), getHighlightMap(distribution.getS33cStockToTransferS36C())
                , distribution.getS54cStock(), distribution.getS54cInTransit(), distribution.getS54cConsumption(), getHighlightMap(distribution.getS54cStockToTransfer())
                , distribution.getS50cStock(), distribution.getS50cInTransit(), distribution.getS50cConsumption(), getHighlightMap(distribution.getS50cStockToTransferW15C()), getHighlightMap(distribution.getS50cStockToTransferS54C())
                , distribution.getW06cStock(), distribution.getW06cInTransit(), distribution.getW06cConsumption(), getHighlightMap(distribution.getW06cStockToTransfer())
                , distribution.getW11cStock(), distribution.getW11cInTransit(), distribution.getW11cConsumption(), getHighlightMap(distribution.getW11cStockToTransfer())
                , distribution.getW14cStock(), distribution.getW14cInTransit(), distribution.getW14cConsumption(), getHighlightMap(distribution.getW14cStockToTransferW15C()), getHighlightMap(distribution.getW14cStockToTransferW11C())
                , distribution.getW17cStock(), distribution.getW17cInTransit(), distribution.getW17cConsumption(), getHighlightMap(distribution.getW17cStockToTransfer())
                , distribution.getW31cStock(), distribution.getW31cInTransit(), distribution.getW31cConsumption(), getHighlightMap(distribution.getW31cStockToTransfer())
                , distribution.getW41cStock(), distribution.getW41cInTransit(), distribution.getW41cConsumption(), getHighlightMap(distribution.getW41cStockToTransfer())
                , distribution.getW51cStock(), distribution.getW51cInTransit(), distribution.getW51cConsumption(), getHighlightMap(distribution.getW51cStockToTransfer())
                , distribution.getW66cStock(), distribution.getW66cInTransit(), distribution.getW66cConsumption(), getHighlightMap(distribution.getW66cStockToTransfer())
                , distribution.getW76cStock(), distribution.getW76cInTransit(), distribution.getW76cConsumption(), getHighlightMap(distribution.getW76cStockToTransfer())
        };
    }
}
