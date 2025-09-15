package org.bluestarcloud.distribution;

import java.util.Map;

public class DistributionSparesConsumption {

    private final String material;
    private final String materialDescription;
    private final String npc;
    private Long grandTotal;

    private Map<String, Consumption> plantWiseConsumption;

    public DistributionSparesConsumption(String material, String materialDescription, String npc) {
        this.material = material;
        this.materialDescription = materialDescription;
        this.npc = npc;
    }

    public void setPlantWiseConsumption(Map<String, Consumption> plantWiseConsumption) {
        this.plantWiseConsumption = plantWiseConsumption;
    }

    public void setGrandTotal(Long grandTotal) {
        this.grandTotal = grandTotal;
    }

    public Long getGrandTotal() {
        return grandTotal;
    }

    public String getMaterial() {
        return material;
    }

    public String getMaterialDescription() {
        return materialDescription;
    }

    public String getNpc() {
        return npc;
    }

    public Map<String, Consumption> getPlantWiseConsumption() {
        return plantWiseConsumption;
    }
}

class Consumption {

    private final String consumption;
    private final String forecastNewRpc;
    private final String currentStockParentRpc;
    private final String stockAvailabilityStatus;

    public Consumption(String consumption, String forecastNewRpc, String currentStockParentRpc, String stockAvailabilityStatus) {
        this.consumption = consumption;
        this.forecastNewRpc = forecastNewRpc;
        this.currentStockParentRpc = currentStockParentRpc;
        this.stockAvailabilityStatus = stockAvailabilityStatus;
    }

    public String getConsumption() {
        return consumption;
    }

    public String getForecastNewRpc() {
        return forecastNewRpc;
    }

    public String getCurrentStockParentRpc() {
        return currentStockParentRpc;
    }

    public String getStockAvailabilityStatus() {
        return stockAvailabilityStatus;
    }
}
