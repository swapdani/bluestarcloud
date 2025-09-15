package org.bluestarcloud.distribution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.bluestarcloud.common.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.bluestarcloud.distribution.Plants.*;

@Service
public class DistributionProcessor {
    private static final Logger logger = LogManager.getLogger(DistributionProcessor.class);

    @Autowired
    DistributionExcelService distributionExcelService;

    private Set<String> newPlantsSet = new HashSet<>();
    private Set<String> oldPlantsSet = new HashSet<>();
    Map<String, List<String>> newPlantListByOldPlant = new HashMap<>();

    public Workbook processDistribution(MultipartFile file, boolean reWork) {
        try {
            List<DistributionStock> distributionStockList = distributionExcelService.getDistributionStock(file);
            logger.info("Stockfile parsed.");

            List<DistributionStock> consumptionList = distributionExcelService.getConsumption(file);
            logger.info("Consumption parsed.");

            List<GitTransit> gitTransitList = distributionExcelService.getGitTransitListFromExcel(file);
            logger.info("Git transit parsed.");

            List<DistributionStock> materialLeadTimeList = distributionExcelService.getMaterialLeadTime(file);
            logger.info("Material lead time parsed");

            List<PendingPO> pendingPOList = distributionExcelService.getPendingPO(file);
            logger.info("Pending PO parsed");

            List<PendingPR> pendingPRList = distributionExcelService.getPendingPR(file);
            logger.info("Pending PR parsed");

            List<PlantMapping> plantMappingList = distributionExcelService.getPlantMappingFromExcel(file);
            logger.info("Plant mapping parsed");

            Map<String, Map<String, List<DistributionStock>>> distributionStockMapByPlantMaterial = getDistributionStockMapByPlantMaterial(distributionStockList);

            replaceMaterialNameEndingWith00R(consumptionList);

            setN33nS06GroupPlants(consumptionList);

            Map<String, Map<String, Double>> consumptionByPlantMaterial = getConsumptionByPlantMaterial(consumptionList);

            setupQuantityForMaterialGroup(consumptionList, consumptionByPlantMaterial);

            newPlantListByOldPlant = getNewPlantListByOldPlant(plantMappingList);
            getConsumptionByPlantMaterialForNewPlants(plantMappingList, consumptionByPlantMaterial);

            Map<String, Map<String, Double>> gitTransitMaterialPlant = getGitTransitMaterialPlant(gitTransitList);

            Map<String, String> masterMaterialMap = createMasterMaterialMap(distributionStockList);

            List<Distribution> distributionList = new ArrayList<>();
            List<DistributionPRDetails> distributionPRDetailsList = new ArrayList<>();

            processDistributionCalculation(distributionStockMapByPlantMaterial, consumptionByPlantMaterial, gitTransitMaterialPlant, masterMaterialMap, distributionList, distributionPRDetailsList);
            logger.info("Distribution calculation completed.");

            if (reWork) {
                logger.info("Distribution recalculation for over calculated stock started ");
                List<Distribution> reWorkDistributionList = new ArrayList<>();
                List<DistributionPRDetails> reWordDistributionPRDetailsList = new ArrayList<>();

                reWorkDistributionCalculation(consumptionList, distributionStockMapByPlantMaterial, gitTransitMaterialPlant,
                        masterMaterialMap, distributionList, distributionPRDetailsList, reWorkDistributionList,
                        reWordDistributionPRDetailsList, reWork);

                distributionList.addAll(reWorkDistributionList);
                distributionPRDetailsList.addAll(reWordDistributionPRDetailsList);
                logger.info("Distribution recalculation completed");
            }

            setMonthForMaterialConsumption(consumptionList);

            Map<String, Double> distributionStockByQty = getDistributionStockByQty(distributionStockList);

            Map<String, Map<String, Double>> consumptionByMonthMaterial = getConsumptionByMonthMaterial(consumptionList);

            Map<String, Double> pendingPOByMaterial = getPendingPOByMaterial(pendingPOList);

            Map<String, Double> pendingPRByMaterial = getPendingPRByMaterial(pendingPRList);

            List<DistributionPlanning> distributionPlanningList = calculateDistributionPlanning(distributionStockByQty, consumptionByMonthMaterial, materialLeadTimeList, pendingPOByMaterial, pendingPRByMaterial);

            logger.info("Distribution planning calculation completed.");

            List<DistributionSparesConsumption> distributionSparesConsumptionList = getDistributionSparesConsumption(distributionList, plantMappingList);

            logger.info("Distribution spares consumption calculation completed.");


            logger.info("Distribution processing completed.");
            return distributionExcelService.writeProcessedDistributionsToExcel(distributionList, distributionPRDetailsList, DistributionHeaders.getDistributionHeaders(), distributionPlanningList, DistributionPlanningHeaders.getDistributionPlanningHeaders()
                    , distributionSparesConsumptionList, plantMappingList);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to process file", ex);
        }
    }

    private void reWorkDistributionCalculation(List<DistributionStock> consumptionList, Map<String, Map<String, List<DistributionStock>>> distributionStockMapByPlantMaterial,
                                               Map<String, Map<String, Double>> gitTransitMaterialPlant, Map<String, String> masterMaterialMap,
                                               List<Distribution> distributionList, List<DistributionPRDetails> distributionPRDetailsList,
                                               List<Distribution> reWorkDistributionList, List<DistributionPRDetails> reWordDistributionPRDetailsList, boolean reWork) {
        List<String> overCalculatedMaterialList = distributionList.stream().filter(dist -> Long.parseLong(dist.getTotalStockToTransfer()) > Long.parseLong(dist.getW15cEligible())).map(Distribution::getMaterial).toList();
        if (overCalculatedMaterialList != null && !overCalculatedMaterialList.isEmpty()) {
            distributionPRDetailsList.removeIf(pr -> overCalculatedMaterialList.contains(pr.getMaterial()));
            distributionList.removeIf(dist -> overCalculatedMaterialList.contains(dist.getMaterial()));

            Map<String, Map<String, Double>> originalConsumptionByPlantMaterial = getConsumptionByPlantMaterial(consumptionList);
            Map<String, Map<String, Double>> reWorkConsumptionByPlantMaterial = originalConsumptionByPlantMaterial.entrySet().stream().filter(key ->
                    overCalculatedMaterialList.contains(key.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<String, Map<String, List<DistributionStock>>> reWorkDistributionStockMapByPlantMaterial = distributionStockMapByPlantMaterial.entrySet().stream().filter(key ->
                    overCalculatedMaterialList.contains(key.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<String, String> reWorkMasterMaterialMap = masterMaterialMap.entrySet().stream().filter(key ->
                    overCalculatedMaterialList.contains(key.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            processDistributionCalculation(reWorkDistributionStockMapByPlantMaterial, reWorkConsumptionByPlantMaterial, gitTransitMaterialPlant, reWorkMasterMaterialMap, reWorkDistributionList, reWordDistributionPRDetailsList, reWork);

        } else {
            logger.info("No over calculated stock, not recalculating.");
        }
    }

    private List<DistributionSparesConsumption> getDistributionSparesConsumption(List<Distribution> distributionList, List<PlantMapping> plantMappingList) {
        List<DistributionSparesConsumption> distributionSparesConsumptionList = new ArrayList<>();
        distributionList.forEach(distribution -> {
            DistributionSparesConsumption distributionSparesConsumption = new DistributionSparesConsumption(distribution.getMaterial(), distribution.getMaterialDescription(), distribution.getW15cStock());
            BigDecimal grandTotal = BigDecimal.ZERO;
            Map<String, Consumption> consumptionMap = new HashMap<>();
            for (String oldPlant : oldPlantsSet) {
                switch (Plants.valueOf(oldPlant)) {
                    case N33C -> grandTotal = calculateConsumptionParameters(N33C.name(), distribution.getN33cConsumption(), plantMappingList, distribution.getN33cStock(), consumptionMap, grandTotal);
                    case N66C -> grandTotal = calculateConsumptionParameters(N66C.name(), distribution.getN66cConsumption(), plantMappingList, distribution.getN66cStock(), consumptionMap, grandTotal);
                    case S06C -> grandTotal = calculateConsumptionParameters(S06C.name(), distribution.getS06cConsumption(), plantMappingList, distribution.getS06cStock(), consumptionMap, grandTotal);
                    case S16C -> grandTotal = calculateConsumptionParameters(S16C.name(), distribution.getS16cConsumption(), plantMappingList, distribution.getS16cStock(), consumptionMap, grandTotal);
                    case S36C -> grandTotal = calculateConsumptionParameters(S36C.name(), distribution.getS36cConsumption(), plantMappingList, distribution.getS36cStock(), consumptionMap, grandTotal);
                    case S54C -> grandTotal = calculateConsumptionParameters(S54C.name(), distribution.getS54cConsumption(), plantMappingList, distribution.getS54cStock(), consumptionMap, grandTotal);
                    case W11C -> grandTotal = calculateConsumptionParameters(W11C.name(), distribution.getW11cConsumption(), plantMappingList, distribution.getW11cStock(), consumptionMap, grandTotal);
                }
            }
            distributionSparesConsumption.setPlantWiseConsumption(consumptionMap);
            distributionSparesConsumption.setGrandTotal(grandTotal.setScale(0, RoundingMode.UP).longValue());
            distributionSparesConsumptionList.add(distributionSparesConsumption);
        });
        return distributionSparesConsumptionList.stream().sorted(Comparator.comparingLong(DistributionSparesConsumption::getGrandTotal).reversed()).collect(Collectors.toList());
    }

    private BigDecimal calculateConsumptionParameters(String plant, String strConsumption, List<PlantMapping> plantMappingList, String strParentStock, Map<String, Consumption> consumptionMap, BigDecimal grandTotal) {
        BigDecimal consumptionQty = BigDecimal.valueOf(Double.parseDouble(strConsumption)).setScale(0, RoundingMode.UP);
        BigDecimal percentage = BigDecimal.valueOf(Double.parseDouble(plantMappingList.stream().filter(p -> plant.equalsIgnoreCase(p.getPlant())).map(PlantMapping::getPercentage).findFirst().orElse("0")));
        BigDecimal currentStockParentPlant = BigDecimal.valueOf(Double.parseDouble(strParentStock)).setScale(0, RoundingMode.UP);
        BigDecimal foreCastNewPlant = consumptionQty.multiply(percentage).divide(BigDecimal.valueOf(100), 0, RoundingMode.UP);
        BigDecimal stockAvailability = consumptionQty.subtract(currentStockParentPlant).setScale(0, RoundingMode.UP);
        Consumption consumption = new Consumption(String.valueOf(consumptionQty), String.valueOf(foreCastNewPlant), String.valueOf(currentStockParentPlant), String.valueOf(stockAvailability));
        consumptionMap.put(plant, consumption);
        grandTotal = grandTotal.add(consumptionQty);
        return grandTotal;
    }

    private void setupQuantityForMaterialGroup(List<DistributionStock> consumptionList, Map<String, Map<String, Double>> consumptionByPlantMaterial) {
        Set<String> materialGroupStocks = new HashSet<>(consumptionList.stream()
                .filter(material -> material.getMaterialGroup() != null && !material.getMaterialGroup().isEmpty())
                .toList().stream().map(DistributionStock::getMaterial).toList());

        materialGroupStocks.forEach(material -> {
            Map<String, Double> consumptionByPlant = consumptionByPlantMaterial.get(material);
            Set<String> plantSet = consumptionByPlant.keySet();
            for (Plants plant : Plants.values()) {
                if (!plantSet.contains(plant.name())) {
                    consumptionByPlant.put(plant.name(), 1D);
                }
            }
        });
    }

    private Map<String, List<String>> getNewPlantListByOldPlant(List<PlantMapping> plantMappingList) {
        return plantMappingList.stream().collect(
                Collectors.groupingBy(PlantMapping::getPlant,
                        Collectors.mapping(PlantMapping::getNewPlant, Collectors.toList()))
        );
    }

    private void getConsumptionByPlantMaterialForNewPlants(List<PlantMapping> plantMappingList, Map<String, Map<String, Double>> consumptionByPlantMaterial) {
        consumptionByPlantMaterial.keySet().forEach(material -> plantMappingList.forEach(plantMap -> {
            String plant = plantMap.getPlant();
            if (consumptionByPlantMaterial.get(material).containsKey(plant)) {
                BigDecimal consumptionOfPlant = BigDecimal.valueOf(consumptionByPlantMaterial.get(material).get(plant));
                BigDecimal consumptionPercentage = BigDecimal.valueOf(Long.parseLong(plantMap.getPercentage()));
                BigDecimal consumptionOfNewPlant = consumptionOfPlant.multiply(consumptionPercentage).divide(BigDecimal.valueOf(100), RoundingMode.UP).setScale(0, RoundingMode.UP);
                String newPlant = plantMap.getNewPlant();
                Map<String, Double> consumptionByPlant = consumptionByPlantMaterial.get(material);
                consumptionByPlant.put(newPlant, consumptionOfNewPlant.doubleValue());
            }
        }));
    }

    private Map<String, Double> getPendingPRByMaterial(List<PendingPR> pendingPRList) {
        return pendingPRList.stream().collect(
                Collectors.groupingBy(PendingPR::getMaterial,
                        Collectors.summingDouble(pendingPR -> pendingPR.getQtyRequested() - pendingPR.getQtyOrdered())));
    }

    private Map<String, Double> getPendingPOByMaterial(List<PendingPO> pendingPOList) {
        return pendingPOList.stream().collect(
                Collectors.groupingBy(PendingPO::getMaterial,
                        Collectors.summingDouble(PendingPO::getQtyToBeDelivered)));
    }

    private void processDistributionCalculation(Map<String, Map<String, List<DistributionStock>>> distributionStockMapByPlantMaterial,
                                                Map<String, Map<String, Double>> consumptionByPlantMaterial, Map<String, Map<String, Double>> gitTransitMaterialPlant,
                                                Map<String, String> masterMaterialMap, List<Distribution> distributionList,
                                                List<DistributionPRDetails> distributionPRDetailsList) {
        processDistributionCalculation(distributionStockMapByPlantMaterial, consumptionByPlantMaterial, gitTransitMaterialPlant,
                masterMaterialMap, distributionList, distributionPRDetailsList, false);

    }

    private void processDistributionCalculation(Map<String, Map<String, List<DistributionStock>>> distributionStockMapByPlantMaterial,
                                                Map<String, Map<String, Double>> consumptionByPlantMaterial, Map<String, Map<String, Double>> gitTransitMaterialPlant,
                                                Map<String, String> masterMaterialMap, List<Distribution> distributionList,
                                                List<DistributionPRDetails> distributionPRDetailsList, boolean isRework) {
        if (!isRework) {
            newPlantsSet = setupNewPlantSet();
            oldPlantsSet = setupOldPlantSet();
        }
        masterMaterialMap.keySet().forEach(material -> {
            Distribution distribution = new Distribution();
            distribution.setMaterial(material);
            distribution.setMaterialDescription(masterMaterialMap.get(material));

            Map<String, List<DistributionStock>> distributionStockMapByPlant = distributionStockMapByPlantMaterial.get(material);
            Map<String, Double> consumptionByPlant = consumptionByPlantMaterial.get(material);
            Map<String, Double> gitTransitByPlant = gitTransitMaterialPlant.get(material);

            Map<String, Map<String, Double>> plantQty = new HashMap<>();
            double w15Stock, plantStock, plantConsumption, gitTransitQty;
            List<DistributionStock> distributionStockListForPlant;
            Map<String, Double> plantDataMap;
            for (Plants plant : Plants.values()) {
                distributionStockListForPlant = distributionStockMapByPlant != null ? distributionStockMapByPlant.get(plant.name()) : null;
                if (distributionStockListForPlant != null && !distributionStockListForPlant.isEmpty()) {
                    plantStock = distributionStockListForPlant.get(0).getQuantity();
                } else {
                    plantStock = 0D;
                }
                plantConsumption = consumptionByPlant != null && consumptionByPlant.get(plant.name()) != null
                        ? consumptionByPlant.get(plant.name()) : 0D;
                gitTransitQty = gitTransitByPlant != null && gitTransitByPlant.get(plant.name()) != null
                        ? gitTransitByPlant.get(plant.name()) : 0D;


                plantDataMap = new HashMap<>();
                plantDataMap.put("Stock", plantStock);
                plantDataMap.put("Consumption", plantConsumption);
                plantDataMap.put("GitTransit", gitTransitQty);
                plantQty.put(plant.name(), plantDataMap);
            }

            distributionStockListForPlant = distributionStockMapByPlant != null ? distributionStockMapByPlant.get("W15C") : null;
            if (distributionStockListForPlant != null && !distributionStockListForPlant.isEmpty()) {
                w15Stock = distributionStockListForPlant.get(0).getQuantity();
            } else {
                w15Stock = 0D;
            }
            plantDataMap = new HashMap<>();
            plantDataMap.put("Stock", w15Stock);
            plantQty.put("W15C", plantDataMap);
            calculateDistribution(distribution, distributionPRDetailsList, plantQty, isRework);
            distributionList.add(distribution);
        });
    }

    private Set<String> setupNewPlantSet() {
        Set<String> newPlantSet = new HashSet<>();
        newPlantSet.add(N34C.name());
        newPlantSet.add(N64C.name());
        newPlantSet.add(S66C.name());
        newPlantSet.add(S49C.name());
        newPlantSet.add(S33C.name());
        newPlantSet.add(S50C.name());
        newPlantSet.add(W14C.name());
        return newPlantSet;
    }

    private Set<String> setupOldPlantSet() {
        Set<String> newPlantSet = new HashSet<>();
        newPlantSet.add(N33C.name());
        newPlantSet.add(N66C.name());
        newPlantSet.add(S06C.name());
        newPlantSet.add(S16C.name());
        newPlantSet.add(S36C.name());
        newPlantSet.add(S54C.name());
        newPlantSet.add(W11C.name());
        return newPlantSet;
    }

    private Map<String, String> createMasterMaterialMap(List<DistributionStock> distributionStockList) {
        return new HashMap<>(distributionStockList.stream()
                .filter(ds -> "W15C".equalsIgnoreCase(ds.getPlant()) && ds.getQuantity() > 0D)
                .filter(ds -> !ds.getMaterial().endsWith("00-R"))
                .collect(Collectors.toMap(DistributionStock::getMaterial, DistributionStock::getMaterialDescription, (desc1, desc2) -> desc2)));
    }

    private void replaceMaterialNameEndingWith00R(List<DistributionStock> consumptionList) {
        consumptionList.forEach(ds -> {
            if (ds.getMaterial().endsWith("00-R")) {
                ds.setMaterial(ds.getMaterial().replace("00-R", "00"));
            }
        });
    }

    private Map<String, Map<String, Double>> getGitTransitMaterialPlant(List<GitTransit> gitTransitList) {
        return gitTransitList.stream().collect(
                Collectors.groupingBy(GitTransit::getMaterialNo,
                        Collectors.groupingBy(GitTransit::getPlant,
                                Collectors.summingDouble(GitTransit::getGitQty))));
    }

    private Map<String, Map<String, Double>> getConsumptionByPlantMaterial(List<DistributionStock> consumptionList) {
        return consumptionList.stream().collect(
                Collectors.groupingBy(DistributionStock::getMaterial,
                        Collectors.groupingBy(DistributionStock::getPlant,
                                Collectors.summingDouble(DistributionStock::getQuantity))));
    }

    private Map<String, Map<String, List<DistributionStock>>> getDistributionStockMapByPlantMaterial(List<DistributionStock> distributionStockList) {
        return distributionStockList.stream().collect(
                Collectors.groupingBy(DistributionStock::getMaterial,
                        Collectors.groupingBy(DistributionStock::getPlant)));
    }

    private Map<String, Map<String, Double>> getConsumptionByMonthMaterial(List<DistributionStock> consumptionList) {
        return consumptionList.stream().collect(
                Collectors.groupingBy(DistributionStock::getMaterial,
                        Collectors.groupingBy(DistributionStock::getMonth,
                                Collectors.summingDouble(DistributionStock::getQuantity))));
    }

    private Map<String, Double> getDistributionStockByQty(List<DistributionStock> distributionStockList) {
        return distributionStockList.stream().collect(
                Collectors.groupingBy(DistributionStock::getMaterial,
                        Collectors.summingDouble(DistributionStock::getQuantity)));
    }

    private List<DistributionPlanning> calculateDistributionPlanning(Map<String, Double> distributionStockByQty, Map<String, Map<String, Double>> consumptionByMonthMaterial,
                                                                     List<DistributionStock> materialLeadTimeList, Map<String, Double> pendingPOByMaterial, Map<String, Double> pendingPRByMaterial) {
        Map<String, Double> consumptionWithMaxQty = new HashMap<>();
        consumptionByMonthMaterial.keySet().forEach(material -> {
            Map<String, Double> consumptionByMonth = consumptionByMonthMaterial.get(material);
            OptionalDouble maxQty = consumptionByMonth.values().stream().mapToDouble(Double::doubleValue).max();
            consumptionWithMaxQty.put(material, maxQty.getAsDouble());
        });

        List<DistributionPlanning> distributionPlanningList = new ArrayList<>();
        consumptionWithMaxQty.keySet().forEach(material -> {
            List<DistributionStock> distributionStockList = materialLeadTimeList.stream().filter(row -> material.equalsIgnoreCase(row.getMaterial())).toList();
            if (distributionStockList != null && !distributionStockList.isEmpty()) {
                DistributionStock distributionStock = distributionStockList.get(0);
                String materialDescription = distributionStock.getMaterialDescription();
                String supplier = distributionStock.getSupplier();
                int leadTime = distributionStock.getLeadTime();
                Double stock = distributionStockByQty.get(material) != null ? distributionStockByQty.get(material) : 0;
                Double consumptionQty = consumptionWithMaxQty.get(material);
                Double pendingPOQty = pendingPOByMaterial.get(material) == null ? 0 : pendingPOByMaterial.get(material);
                Double pendingPRQty = pendingPRByMaterial.get(material) == null ? 0 : pendingPRByMaterial.get(material);

                Double qtyToArrange = getQtyToArrange(consumptionQty, stock, leadTime, pendingPOQty, pendingPRQty);
                distributionPlanningList.add(getDistributionPlanning(material, materialDescription, supplier, consumptionQty, stock, leadTime, qtyToArrange));
            } else {
                distributionPlanningList.add(getDistributionPlanning(material, "Lead time not available"));
            }
        });
        return distributionPlanningList;
    }

    private DistributionPlanning getDistributionPlanning(String material, String materialDescription, String supplier, Double consumptionQty, Double stock, int leadTime, Double qtyToArrange) {
        return new DistributionPlanning(material, materialDescription, supplier, Utils.getCorrectDecimalForDouble(consumptionQty, 0),
                Utils.getCorrectDecimalForDouble(stock, 0), String.valueOf(leadTime), Utils.getCorrectDecimalForDouble(qtyToArrange, 0));
    }

    private DistributionPlanning getDistributionPlanning(String material, String qtyToArrange) {
        return new DistributionPlanning(material, null, null, null, null, null, qtyToArrange);
    }

    private Double getQtyToArrange(Double consumptionQty, Double stock, int leadTime, Double pendingPOQty, Double pendingPRQty) {
        BigDecimal totalStock = BigDecimal.valueOf(stock).add(BigDecimal.valueOf(pendingPOQty)).add(BigDecimal.valueOf(pendingPRQty));
        BigDecimal perMonthConsumption = BigDecimal.valueOf(consumptionQty);
        BigDecimal availableStockInMonth = totalStock.divide(perMonthConsumption, 2, RoundingMode.UP);
        BigDecimal stockToArrangeForMonth = BigDecimal.valueOf(leadTime).subtract(availableStockInMonth);
        BigDecimal qtyToArrange = stockToArrangeForMonth.multiply(perMonthConsumption)
                .setScale(0, RoundingMode.UP);
        return Double.parseDouble(qtyToArrange.toString());
    }

    private void setMonthForMaterialConsumption(List<DistributionStock> consumptionList) {
        consumptionList.forEach(record -> {
            String postingDate = record.getPostingDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate postingDateLocal = LocalDate.parse(postingDate, formatter);
            String month = postingDateLocal.getMonth().name();
            record.setMonth(month);
        });
    }

    private void setN33nS06GroupPlants(List<DistributionStock> consumptionList) {
        List<String> n33cGroupList = Stream.of(N33CGroup.values()).map(N33CGroup::name).toList();
        List<String> s06cGroupList = Stream.of(S06CGroup.values()).map(S06CGroup::name).toList();

        consumptionList.forEach(ds -> {
            if (n33cGroupList.contains(ds.getPlant())) {
                ds.setPlant(N33CGroup.N33C.name());
            } else if (s06cGroupList.contains(ds.getPlant())) {
                ds.setPlant(S06CGroup.S06C.name());
            }
        });
    }

    private void calculateDistribution(Distribution distribution, List<DistributionPRDetails> distributionPRDetailsList,
                                       Map<String, Map<String, Double>> plantQty, boolean isRework) {
        double w15qty = plantQty.get("W15C").get("Stock");
        if (w15qty == 0D) {
            plantQty.get("W15C").put("Eligible", 0D);
            plantQty.keySet().forEach(plant -> plantQty.get(plant).put("StockToTransfer", 0D));
        } else {
            double totalRequiredStock = 0D;
            for (String plant : plantQty.keySet()) {
                if (!"W15C".equals(plant)) {
                    if (!isRework && newPlantsSet.contains(plant))
                        continue;
                    double availableStockAtPlant = plantQty.get(plant).get("Stock");
                    double consumptionAtPlant = plantQty.get(plant).get("Consumption");
                    if (availableStockAtPlant < consumptionAtPlant) {
                        totalRequiredStock += consumptionAtPlant - availableStockAtPlant;
                    } else if (!isRework) {
                        double stockToTransfer;
                        if (oldPlantsSet.contains(plant)) {
                            double totalRequiredStockAtPlant = 0D;
                            double excessStock = availableStockAtPlant - consumptionAtPlant;
                            List<String> newPlants = newPlantListByOldPlant.get(plant);
                            for (String p : newPlants) {
                                double availableStock = plantQty.get(p).get("Stock");
                                double consumption = plantQty.get(p).get("Consumption");
                                if (availableStock < consumption) {
                                    totalRequiredStockAtPlant += consumption - availableStock;
                                }
                            }
                            for (String p : newPlants) {
                                double availableStock = plantQty.get(p).get("Stock");
                                double consumption = plantQty.get(p).get("Consumption");
                                if (availableStock < consumption) {
                                    double stockNeeded = consumption - availableStock;
                                    double weightedAvgStock = Math.round(stockNeeded / totalRequiredStockAtPlant * 100);
                                    stockToTransfer = Math.round(excessStock * weightedAvgStock / 100);
                                    stockToTransfer = Math.min(stockToTransfer, stockNeeded);
                                    plantQty.get(p).put("StockToTransfer".concat(plant), stockToTransfer);
                                    double remainingStockForNewPlant = stockNeeded - stockToTransfer;
                                    plantQty.get(p).put("StockNeeded", remainingStockForNewPlant);
                                    totalRequiredStock += remainingStockForNewPlant;

                                    if (stockToTransfer != 0) {
                                        distributionPRDetailsList.add(getDistributionPRDetails(distribution, plant, p, stockToTransfer));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            double eligibleQtyToTransfer = Math.floor(w15qty * 0.8);
            plantQty.get("W15C").put("Eligible", eligibleQtyToTransfer);
            for (String plant : plantQty.keySet()) {
                double stockNeeded;
                double stockToTransfer;
                double weightedAvgStock;
                if (!"W15C".equals(plant)) {
                    double availableStockAtPlant = plantQty.get(plant).get("Stock");
                    double consumptionAtPlant = plantQty.get(plant).get("Consumption");
                    double gitTransitQty = plantQty.get(plant).get("GitTransit");
                    availableStockAtPlant += gitTransitQty;
                    if (availableStockAtPlant < consumptionAtPlant) {
                        if (!isRework) {
                            stockNeeded = newPlantsSet.contains(plant) && plantQty.get(plant).get("StockNeeded") != null ? plantQty.get(plant).get("StockNeeded") : consumptionAtPlant - availableStockAtPlant;
                        } else {
                            stockNeeded = consumptionAtPlant - availableStockAtPlant;
                        }
                        if (stockNeeded == 0) {
                            stockToTransfer = 0D;
                        } else {
                            weightedAvgStock = Math.round(stockNeeded / totalRequiredStock * 100);
                            stockToTransfer = Math.round(eligibleQtyToTransfer * weightedAvgStock / 100);
                            stockToTransfer = Math.max(1, Math.min(stockToTransfer, stockNeeded));
                        }
                        plantQty.get(plant).put("StockToTransfer", stockToTransfer);

                        if (stockToTransfer != 0) {
                            distributionPRDetailsList.add(getDistributionPRDetails(distribution, "W15C", plant, stockToTransfer));
                        }
                    } else {
                        plantQty.get(plant).put("StockToTransfer", 0D);
                    }
                }
            }
        }
        populateQtysInDistributionPlantC(distribution, plantQty);
    }

    private DistributionPRDetails getDistributionPRDetails(Distribution distribution, String fromPlant, String plant, double stockToTransfer) {
        DistributionPRDetails distributionPRDetails = new DistributionPRDetails();
        distributionPRDetails.setMaterial(distribution.getMaterial());
        distributionPRDetails.setStockToTransfer(Utils.getCorrectDecimalForDouble(stockToTransfer, 0));
        distributionPRDetails.setPlant(plant);
        distributionPRDetails.setFromPlant(fromPlant);
        return distributionPRDetails;
    }

    private void populateQtysInDistributionPlantC(Distribution distribution, Map<String, Map<String, Double>> plantQty) {
        double totalStock = getTotalStockQuantity(plantQty);
        double totalConsumption = getTotalConsumptionQuantity(plantQty);
        double totalStockToTransfer = getTotalStockToTransferQuantity(plantQty);
        double currentStockAvailability = totalStock - totalConsumption;

        distribution.setW15cStock(Utils.getCorrectDecimalForDouble(plantQty.get("W15C").get("Stock"), 0));
        distribution.setW15cEligible(Utils.getCorrectDecimalForDouble(plantQty.get("W15C").get("Eligible"), 0));

        setStockQuantity(distribution, plantQty);
        setConsumptionQuantity(distribution, plantQty);
        setStockToTransferQuantity(distribution, plantQty);
        setInTransitQuantity(distribution, plantQty);

        distribution.setTotalStockToTransfer(Utils.getCorrectDecimalForDouble(totalStockToTransfer, 0));
        distribution.setTotalStock(Utils.getCorrectDecimalForDouble(totalStock, 0));
        distribution.setTotalConsumption(Utils.getCorrectDecimalForDouble(totalConsumption, 0));
        distribution.setCurrentStockAvailability(Utils.getCorrectDecimalForDouble(currentStockAvailability, 0));
    }

    private double getTotalStockToTransferQuantity(Map<String, Map<String, Double>> plantQty) {
        double totalStockToTransfer = 0D;
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E06C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E16C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E26C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E33C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E41C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N06C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N33C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N48C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N57C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N66C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N76C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N13C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S06C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S16C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S26C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S36C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S54C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W06C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W17C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W31C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W41C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W51C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W66C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W76C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W11C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("StockToTransfer"), 0));
        totalStockToTransfer += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("StockToTransfer"), 0));
        return totalStockToTransfer;
    }

    private double getTotalConsumptionQuantity(Map<String, Map<String, Double>> plantQty) {
        double totalConsumption = 0D;
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E06C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E16C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E26C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E33C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E41C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N06C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N33C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N48C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N57C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N66C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N76C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N13C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S06C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S16C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S26C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S36C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S54C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W06C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W17C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W31C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W41C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W51C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W66C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W76C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W11C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("Consumption"), 0));
        totalConsumption += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("Consumption"), 0));
        return totalConsumption;
    }

    private double getTotalStockQuantity(Map<String, Map<String, Double>> plantQty) {
        double totalStock = 0D;
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get("W15C").get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E06C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E16C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E26C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E33C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E41C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N06C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N33C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N48C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N57C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N66C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N76C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N13C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S06C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S16C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S26C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S36C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S54C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W06C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W17C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W31C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W41C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W51C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W66C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W76C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W11C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("Stock"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("Stock"), 0));

        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E06C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E16C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E26C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E33C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(E41C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N06C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N33C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N48C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N57C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N66C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N76C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N13C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S06C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S16C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S26C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S36C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S54C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W06C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W17C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W31C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W41C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W51C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W66C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W76C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W11C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("GitTransit"), 0));
        totalStock += Double.parseDouble(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("GitTransit"), 0));
        return totalStock;
    }

    private void setStockToTransferQuantity(Distribution distribution, Map<String, Map<String, Double>> plantQty) {
        distribution.setE06cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(E06C.name()).get("StockToTransfer"), 0));
        distribution.setE16cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(E16C.name()).get("StockToTransfer"), 0));
        distribution.setE26cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(E26C.name()).get("StockToTransfer"), 0));
        distribution.setE33cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(E33C.name()).get("StockToTransfer"), 0));
        distribution.setE41cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(E41C.name()).get("StockToTransfer"), 0));
        distribution.setN06cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(N06C.name()).get("StockToTransfer"), 0));
        distribution.setN33cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(N33C.name()).get("StockToTransfer"), 0));
        distribution.setN48cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(N48C.name()).get("StockToTransfer"), 0));
        distribution.setN57cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(N57C.name()).get("StockToTransfer"), 0));
        distribution.setN66cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(N66C.name()).get("StockToTransfer"), 0));
        distribution.setN76cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(N76C.name()).get("StockToTransfer"), 0));
        distribution.setN13cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(N13C.name()).get("StockToTransfer"), 0));
        distribution.setS06cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(S06C.name()).get("StockToTransfer"), 0));
        distribution.setS16cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(S16C.name()).get("StockToTransfer"), 0));
        distribution.setS26cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(S26C.name()).get("StockToTransfer"), 0));
        distribution.setS36cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(S36C.name()).get("StockToTransfer"), 0));
        distribution.setS54cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(S54C.name()).get("StockToTransfer"), 0));
        distribution.setW06cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(W06C.name()).get("StockToTransfer"), 0));
        distribution.setW17cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(W17C.name()).get("StockToTransfer"), 0));
        distribution.setW31cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(W31C.name()).get("StockToTransfer"), 0));
        distribution.setW41cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(W41C.name()).get("StockToTransfer"), 0));
        distribution.setW51cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(W51C.name()).get("StockToTransfer"), 0));
        distribution.setW66cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(W66C.name()).get("StockToTransfer"), 0));
        distribution.setW76cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(W76C.name()).get("StockToTransfer"), 0));
        distribution.setW11cStockToTransfer(Utils.getCorrectDecimalForDouble(plantQty.get(W11C.name()).get("StockToTransfer"), 0));
        distribution.setN34cStockToTransferW15C(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("StockToTransfer"), 0));
        distribution.setN64cStockToTransferW15C(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("StockToTransfer"), 0));
        distribution.setS66cStockToTransferW15C(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("StockToTransfer"), 0));
        distribution.setS49cStockToTransferW15C(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("StockToTransfer"), 0));
        distribution.setS33cStockToTransferW15C(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("StockToTransfer"), 0));
        distribution.setS50cStockToTransferW15C(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("StockToTransfer"), 0));
        distribution.setW14cStockToTransferW15C(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("StockToTransfer"), 0));
        distribution.setN34cStockToTransferN33C(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("StockToTransferN33C"), 0));
        distribution.setN64cStockToTransferN66C(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("StockToTransferN66C"), 0));
        distribution.setS66cStockToTransferS06C(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("StockToTransferS06C"), 0));
        distribution.setS49cStockToTransferS16C(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("StockToTransferS16C"), 0));
        distribution.setS33cStockToTransferS36C(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("StockToTransferS36C"), 0));
        distribution.setS50cStockToTransferS54C(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("StockToTransferS54C"), 0));
        distribution.setW14cStockToTransferW11C(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("StockToTransferW11C"), 0));
    }

    private void setConsumptionQuantity(Distribution distribution, Map<String, Map<String, Double>> plantQty) {
        distribution.setE06cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(E06C.name()).get("Consumption"), 0));
        distribution.setE16cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(E16C.name()).get("Consumption"), 0));
        distribution.setE26cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(E26C.name()).get("Consumption"), 0));
        distribution.setE33cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(E33C.name()).get("Consumption"), 0));
        distribution.setE41cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(E41C.name()).get("Consumption"), 0));
        distribution.setN06cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N06C.name()).get("Consumption"), 0));
        distribution.setN33cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N33C.name()).get("Consumption"), 0));
        distribution.setN48cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N48C.name()).get("Consumption"), 0));
        distribution.setN57cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N57C.name()).get("Consumption"), 0));
        distribution.setN66cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N66C.name()).get("Consumption"), 0));
        distribution.setN76cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N76C.name()).get("Consumption"), 0));
        distribution.setN13cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N13C.name()).get("Consumption"), 0));
        distribution.setS06cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S06C.name()).get("Consumption"), 0));
        distribution.setS16cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S16C.name()).get("Consumption"), 0));
        distribution.setS26cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S26C.name()).get("Consumption"), 0));
        distribution.setS36cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S36C.name()).get("Consumption"), 0));
        distribution.setS54cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S54C.name()).get("Consumption"), 0));
        distribution.setW06cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W06C.name()).get("Consumption"), 0));
        distribution.setW17cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W17C.name()).get("Consumption"), 0));
        distribution.setW31cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W31C.name()).get("Consumption"), 0));
        distribution.setW41cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W41C.name()).get("Consumption"), 0));
        distribution.setW51cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W51C.name()).get("Consumption"), 0));
        distribution.setW66cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W66C.name()).get("Consumption"), 0));
        distribution.setW76cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W76C.name()).get("Consumption"), 0));
        distribution.setW11cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W11C.name()).get("Consumption"), 0));
        distribution.setN34cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("Consumption"), 0));
        distribution.setN64cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("Consumption"), 0));
        distribution.setS66cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("Consumption"), 0));
        distribution.setS49cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("Consumption"), 0));
        distribution.setS33cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("Consumption"), 0));
        distribution.setS50cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("Consumption"), 0));
        distribution.setW14cConsumption(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("Consumption"), 0));
    }

    private void setInTransitQuantity(Distribution distribution, Map<String, Map<String, Double>> plantQty) {
        distribution.setE06cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(E06C.name()).get("GitTransit"), 0));
        distribution.setE16cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(E16C.name()).get("GitTransit"), 0));
        distribution.setE26cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(E26C.name()).get("GitTransit"), 0));
        distribution.setE33cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(E33C.name()).get("GitTransit"), 0));
        distribution.setE41cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(E41C.name()).get("GitTransit"), 0));
        distribution.setN06cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N06C.name()).get("GitTransit"), 0));
        distribution.setN33cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N33C.name()).get("GitTransit"), 0));
        distribution.setN48cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N48C.name()).get("GitTransit"), 0));
        distribution.setN57cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N57C.name()).get("GitTransit"), 0));
        distribution.setN66cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N66C.name()).get("GitTransit"), 0));
        distribution.setN76cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N76C.name()).get("GitTransit"), 0));
        distribution.setN13cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N13C.name()).get("GitTransit"), 0));
        distribution.setS06cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S06C.name()).get("GitTransit"), 0));
        distribution.setS16cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S16C.name()).get("GitTransit"), 0));
        distribution.setS26cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S26C.name()).get("GitTransit"), 0));
        distribution.setS36cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S36C.name()).get("GitTransit"), 0));
        distribution.setS54cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S54C.name()).get("GitTransit"), 0));
        distribution.setW06cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W06C.name()).get("GitTransit"), 0));
        distribution.setW17cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W17C.name()).get("GitTransit"), 0));
        distribution.setW31cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W31C.name()).get("GitTransit"), 0));
        distribution.setW41cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W41C.name()).get("GitTransit"), 0));
        distribution.setW51cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W51C.name()).get("GitTransit"), 0));
        distribution.setW66cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W66C.name()).get("GitTransit"), 0));
        distribution.setW76cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W76C.name()).get("GitTransit"), 0));
        distribution.setW11cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W11C.name()).get("GitTransit"), 0));
        distribution.setN34cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("GitTransit"), 0));
        distribution.setN64cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("GitTransit"), 0));
        distribution.setS66cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("GitTransit"), 0));
        distribution.setS49cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("GitTransit"), 0));
        distribution.setS33cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("GitTransit"), 0));
        distribution.setS50cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("GitTransit"), 0));
        distribution.setW14cInTransit(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("GitTransit"), 0));

    }

    private void setStockQuantity(Distribution distribution, Map<String, Map<String, Double>> plantQty) {
        distribution.setE06cStock(Utils.getCorrectDecimalForDouble(plantQty.get(E06C.name()).get("Stock"), 0));
        distribution.setE16cStock(Utils.getCorrectDecimalForDouble(plantQty.get(E16C.name()).get("Stock"), 0));
        distribution.setE26cStock(Utils.getCorrectDecimalForDouble(plantQty.get(E26C.name()).get("Stock"), 0));
        distribution.setE33cStock(Utils.getCorrectDecimalForDouble(plantQty.get(E33C.name()).get("Stock"), 0));
        distribution.setE41cStock(Utils.getCorrectDecimalForDouble(plantQty.get(E41C.name()).get("Stock"), 0));
        distribution.setN06cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N06C.name()).get("Stock"), 0));
        distribution.setN33cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N33C.name()).get("Stock"), 0));
        distribution.setN48cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N48C.name()).get("Stock"), 0));
        distribution.setN57cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N57C.name()).get("Stock"), 0));
        distribution.setN66cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N66C.name()).get("Stock"), 0));
        distribution.setN76cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N76C.name()).get("Stock"), 0));
        distribution.setN13cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N13C.name()).get("Stock"), 0));
        distribution.setS06cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S06C.name()).get("Stock"), 0));
        distribution.setS16cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S16C.name()).get("Stock"), 0));
        distribution.setS26cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S26C.name()).get("Stock"), 0));
        distribution.setS36cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S36C.name()).get("Stock"), 0));
        distribution.setS54cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S54C.name()).get("Stock"), 0));
        distribution.setW06cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W06C.name()).get("Stock"), 0));
        distribution.setW17cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W17C.name()).get("Stock"), 0));
        distribution.setW31cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W31C.name()).get("Stock"), 0));
        distribution.setW41cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W41C.name()).get("Stock"), 0));
        distribution.setW51cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W51C.name()).get("Stock"), 0));
        distribution.setW66cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W66C.name()).get("Stock"), 0));
        distribution.setW76cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W76C.name()).get("Stock"), 0));
        distribution.setW11cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W11C.name()).get("Stock"), 0));
        distribution.setN34cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N34C.name()).get("Stock"), 0));
        distribution.setN64cStock(Utils.getCorrectDecimalForDouble(plantQty.get(N64C.name()).get("Stock"), 0));
        distribution.setS66cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S66C.name()).get("Stock"), 0));
        distribution.setS49cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S49C.name()).get("Stock"), 0));
        distribution.setS33cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S33C.name()).get("Stock"), 0));
        distribution.setS50cStock(Utils.getCorrectDecimalForDouble(plantQty.get(S50C.name()).get("Stock"), 0));
        distribution.setW14cStock(Utils.getCorrectDecimalForDouble(plantQty.get(W14C.name()).get("Stock"), 0));
    }
}
