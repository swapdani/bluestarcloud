package org.bluestarcloud.distribution;

public class DistributionPRDetails {
    final private String U = "U";
    private String material;
    private String stockToTransfer;
    private String fromPlant;
    private String plant;
    final private String S51 = "S51";
    final private String MANJUSHREE = "Manjushree";
    final private String MSL = "MSL";

    public String getU() {
        return U;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getStockToTransfer() {
        return stockToTransfer;
    }

    public void setStockToTransfer(String stockToTransfer) {
        this.stockToTransfer = stockToTransfer;
    }

    public String getFromPlant() {
        return fromPlant;
    }

    public void setFromPlant(String fromPlant) {
        this.fromPlant = fromPlant;
    }

    public String getPlant() {
        return plant;
    }

    public void setPlant(String plant) {
        this.plant = plant;
    }

    public String getS51() {
        return S51;
    }

    public String getMANJUSHREE() {
        return MANJUSHREE;
    }

    public String getMSL() {
        return MSL;
    }
}
