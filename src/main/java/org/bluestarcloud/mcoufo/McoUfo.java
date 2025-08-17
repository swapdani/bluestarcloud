package org.bluestarcloud.mcoufo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.bluestarcloud.common.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class McoUfo {
    private static final Logger logger = LogManager.getLogger(McoUfo.class);

    @Autowired
    McoUfoExcelService excelService;

    public Workbook processUfo(MultipartFile multipartFile) {
        try {
            List<PartAvailability> partAvailabilityList = excelService.getPartAvailabilityData(multipartFile);
            logger.info("Stock availability sheet parsed.");
            List<SalesOrders> salesOrdersList = excelService.getSalesOrdersData(multipartFile);
            logger.info("Sales orders sheet parsed.");
            List<GitTransit> gitTransitList = excelService.getGitTransitData(multipartFile);
            logger.info("Git transit sheet parsed.");

            Map<String, Double> stockAvailabilityByPlantMaterial = partAvailabilityList.stream().collect(
                    Collectors.groupingBy(p -> Utils.getPlantMaterialKey(getPlantGroup(p.getPlant()), p.getMaterialNo()),
                            Collectors.summingDouble(PartAvailability::getAvailableQty)));

            logger.info("Stock availability grouped by plant and material number.");

            Map<String, Map<String, List<SalesOrders>>> salesOrdersByDateMaterialPlant = salesOrdersList.stream().collect(
                    Collectors.groupingBy(s -> Utils.getPlantMaterialKey(getPlantGroup(s.getPlant()), s.getMaterialNo()),
                            Collectors.groupingBy(s1 -> Utils.getYYYYMMDD(s1.getSoItemDate(), "."))));

            logger.info("Sales orders grouped by plant, material number and SO date.");

            Map<String, Double> gitTransitByPlantMaterial = gitTransitList.stream().collect(
                    Collectors.groupingBy(g -> Utils.getPlantMaterialKey(getPlantGroup(g.getPlant()), g.getMaterialNo()),
                            Collectors.summingDouble(GitTransit::getGitQty)));

            Map<String, Set<String>> gitSourcePlantByPlantMaterial = gitTransitList.stream().collect(
                    Collectors.groupingBy(g -> Utils.getPlantMaterialKey(getPlantGroup(g.getPlant()), g.getMaterialNo()),
                            Collectors.mapping(GitTransit::getSupplyingPlant, Collectors.toSet())));

            logger.info("GIT grouped by plant and material number.");

            List<SalesOrders> processedSalesOrders = new ArrayList<>();
            Map<String, Map<Double, Map<String, List<SalesOrders>>>> salesOrdersWithNoStockByDateQtyPlantMaterial = new HashMap<>();
            Map<String, Double> remainingQtyByPlantMaterial = new HashMap<>();
            for (String plantMaterialKey : salesOrdersByDateMaterialPlant.keySet()) {
                Map<String, List<SalesOrders>> salesOrdersByDate = salesOrdersByDateMaterialPlant.get(plantMaterialKey);
                if (salesOrdersByDate != null && !salesOrdersByDate.isEmpty()) {
                    Set<String> soDateKeySet = salesOrdersByDate.keySet();
                    List<String> sortedSODates = new ArrayList<>(soDateKeySet);
                    Collections.sort(sortedSODates);
                    Double availableStock = stockAvailabilityByPlantMaterial.get(plantMaterialKey);
                    availableStock = availableStock == null ? 0.0 : availableStock;
                    for (String sortedSODate : sortedSODates) {
                        List<SalesOrders> salesOrders = salesOrdersByDate.get(sortedSODate);
                        if (salesOrders.isEmpty()) {
                            continue;
                        }
                        if (salesOrders.size() > 1) {
                            Map<Double, List<SalesOrders>> salesOrderSortedByQty = new HashMap<>();
                            for (SalesOrders order : salesOrders) {
                                List<SalesOrders> ordersList = salesOrderSortedByQty.computeIfAbsent(order.getUfoQty(), k -> new ArrayList<>());
                                ordersList.add(order);
                            }
                            List<Double> sortedQty = new ArrayList<>(salesOrderSortedByQty.keySet());
                            for (Double qty : sortedQty) {
                                List<SalesOrders> ordersList = salesOrderSortedByQty.get(qty);
                                for (SalesOrders order : ordersList) {
                                    Double requiredQty = order.getUfoQty();
                                    if (availableStock >= requiredQty) {
                                        order.setMcoRemarks("Stock available at RPC. Arrange OBD from commercial.");
                                        availableStock = availableStock - requiredQty;
                                        remainingQtyByPlantMaterial.put(plantMaterialKey, availableStock);
                                    } else {
                                        remainingQtyByPlantMaterial.put(plantMaterialKey, availableStock);
                                        Map<Double, Map<String, List<SalesOrders>>> salesOrdersWithNoStockByQtyDate = salesOrdersWithNoStockByDateQtyPlantMaterial.computeIfAbsent(plantMaterialKey, k -> new HashMap<>());
                                        Map<String, List<SalesOrders>> salesOrdersWithNoStockByDate = salesOrdersWithNoStockByQtyDate.computeIfAbsent(order.getUfoQty(), k -> new HashMap<>());
                                        List<SalesOrders> salesOrdersWithNoStock = salesOrdersWithNoStockByDate.computeIfAbsent(Utils.getYYYYMMDD(order.getSoItemDate(), "."), k -> new ArrayList<>());
                                        salesOrdersWithNoStock.add(order);
                                    }
                                    processedSalesOrders.add(order);
                                }
                            }
                        } else {
                            SalesOrders order = salesOrders.get(0);
                            Double requiredQty = order.getUfoQty();
                            if (availableStock >= requiredQty) {
                                order.setMcoRemarks("Stock available at RPC. Arrange OBD from commercial.");
                                availableStock = availableStock - requiredQty;
                                remainingQtyByPlantMaterial.put(plantMaterialKey, availableStock);
                            } else {
                                remainingQtyByPlantMaterial.put(plantMaterialKey, availableStock);
                                Map<Double, Map<String, List<SalesOrders>>> salesOrdersWithNoStockByQtyDate = salesOrdersWithNoStockByDateQtyPlantMaterial.computeIfAbsent(plantMaterialKey, k -> new HashMap<>());
                                Map<String, List<SalesOrders>> salesOrdersWithNoStockByDate = salesOrdersWithNoStockByQtyDate.computeIfAbsent(order.getUfoQty(), k -> new HashMap<>());
                                List<SalesOrders> salesOrdersWithNoStock = salesOrdersWithNoStockByDate.computeIfAbsent(Utils.getYYYYMMDD(order.getSoItemDate(), "."), k -> new ArrayList<>());
                                salesOrdersWithNoStock.add(order);
                            }
                            processedSalesOrders.add(order);
                        }
                    }
                }
            }

            logger.info("Stock availabilities processed.");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String etaDate = LocalDate.now().plusDays(7).format(formatter);

            salesOrdersWithNoStockByDateQtyPlantMaterial.keySet().parallelStream().forEach(plantMaterialKey -> {
                Double remainingStock = remainingQtyByPlantMaterial.get(plantMaterialKey);
                remainingStock = remainingStock == null ? 0.0 : remainingStock;
                Double quantityInGit = gitTransitByPlantMaterial.get(plantMaterialKey);
                quantityInGit = quantityInGit == null ? 0.0 : quantityInGit;

                String srcPlant = gitSourcePlantByPlantMaterial.containsKey(plantMaterialKey) ? gitSourcePlantByPlantMaterial.get(plantMaterialKey).toString()
                        .replace("[", "")
                        .replace("]", "")
                        .replace(",", " and")
                        : null;

                Map<Double, Map<String, List<SalesOrders>>> salesOrdersByDateQty = salesOrdersWithNoStockByDateQtyPlantMaterial.get(plantMaterialKey);
                if (salesOrdersByDateQty != null && !salesOrdersByDateQty.isEmpty()) {
                    List<Double> qtyKeyList = new ArrayList<>(salesOrdersByDateQty.keySet());
                    Collections.sort(qtyKeyList);
                    for (Double qtyKey : qtyKeyList) {
                        Map<String, List<SalesOrders>> salesOrdersByDate = salesOrdersByDateQty.get(qtyKey);
                        if (salesOrdersByDate != null && !salesOrdersByDate.isEmpty()) {
                            List<String> dateKeySet = new ArrayList<>(salesOrdersByDate.keySet());
                            Collections.sort(dateKeySet);
                            for (String dateKey : dateKeySet) {
                                List<SalesOrders> salesOrdersWithNoStock = salesOrdersByDate.get(dateKey);
                                if (salesOrdersWithNoStock != null && !salesOrdersWithNoStock.isEmpty()) {
                                    for (SalesOrders order : salesOrdersWithNoStock) {
                                        Double requiredQty = order.getUfoQty();
                                        Double qtyToArrange = requiredQty - remainingStock;
                                        Double qtyInGitForOrder = quantityInGit > qtyToArrange ? qtyToArrange : quantityInGit;
                                        double qtyToArrangeForOrder = qtyToArrange - qtyInGitForOrder;
                                        String mcoRemark = null;
                                        if (remainingStock > 0.0 && qtyInGitForOrder > 0.0 && qtyToArrangeForOrder > 0.0) {
                                            mcoRemark = Utils.getCorrectDecimalForDouble(remainingStock, 3) + " quantity available at RPC, " + Utils.getCorrectDecimalForDouble(qtyInGitForOrder, 3) + " quantity in GIT from " + srcPlant + " expected at RPC on " + etaDate + ",  " + Utils.getCorrectDecimalForDouble(qtyToArrangeForOrder, 3) + " quantity needs to be arranged";
                                        } else if (remainingStock > 0.0 && qtyInGitForOrder > 0.0 && qtyToArrangeForOrder <= 0.0) {
                                            mcoRemark = Utils.getCorrectDecimalForDouble(remainingStock, 3) + " quantity available at RPC, " + Utils.getCorrectDecimalForDouble(qtyInGitForOrder, 3) + " quantity in GIT from " + srcPlant + " expected at RPC on " + etaDate;
                                        } else if (remainingStock > 0.0 && qtyInGitForOrder <= 0.0 && qtyToArrangeForOrder > 0.0) {
                                            mcoRemark = Utils.getCorrectDecimalForDouble(remainingStock, 3) + " quantity available at RPC, " + Utils.getCorrectDecimalForDouble(qtyToArrangeForOrder, 3) + " quantity needs to be arranged";
                                        } else if (remainingStock > 0.0 && qtyInGitForOrder <= 0.0 && qtyToArrangeForOrder <= 0.0) {
                                            mcoRemark = Utils.getCorrectDecimalForDouble(remainingStock, 3) + " quantity available at RPC, " + Utils.getCorrectDecimalForDouble(qtyToArrangeForOrder, 3) + " quantity needs to be arranged";
                                        } else if (remainingStock <= 0.0 && qtyInGitForOrder > 0.0 && qtyToArrangeForOrder > 0.0) {
                                            mcoRemark = Utils.getCorrectDecimalForDouble(qtyInGitForOrder, 3) + " quantity in GIT from " + srcPlant + " expected at RPC on " + etaDate + ", " + Utils.getCorrectDecimalForDouble(qtyToArrangeForOrder, 3) + " quantity needs to be arranged";
                                        } else if (remainingStock <= 0.0 && qtyInGitForOrder > 0.0 && qtyToArrangeForOrder <= 0.0) {
                                            mcoRemark = Utils.getCorrectDecimalForDouble(qtyInGitForOrder, 3) + " quantity in GIT from " + srcPlant + " expected at RPC on " + etaDate;
                                        } else if (remainingStock <= 0.0 && qtyInGitForOrder <= 0.0 && qtyToArrangeForOrder > 0.0) {
                                            mcoRemark = Utils.getCorrectDecimalForDouble(qtyToArrangeForOrder, 3) + " quantity needs to be arranged";
                                        }

                                        order.setMcoRemarks(mcoRemark);
                                        remainingStock = Math.max(remainingStock - qtyToArrange, 0.0);
                                        quantityInGit = Math.max(quantityInGit - qtyInGitForOrder, 0.0);
                                    }
                                }
                            }
                        }
                    }
                }
            });

            logger.info("Stock non availabilities processed.");

            return excelService.writeProcessedSalesOrdersToExcel(processedSalesOrders);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process file", e);
        }
    }

    private String getPlantGroup(String plant) {
        if (plant == null || plant.length() < 3)
            return plant == null ? "" : plant;
        return plant.substring(0, 3);
    }
}
