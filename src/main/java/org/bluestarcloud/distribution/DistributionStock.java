package org.bluestarcloud.distribution;

public class DistributionStock {
    private String plant;
    private String division;
    private String material;
    private String materialDescription;
    private Double quantity;
    private String postingDate;
    private String supplier;
    private int leadTime;
    private String month;
    private String materialGroup;

    public DistributionStock(String plant, String material, String materialDescription, Double quantity) {
        this.plant = plant;
        this.material = material;
        this.materialDescription = materialDescription;
        this.quantity = quantity;
    }

    public DistributionStock(String plant, String division, String material, String materialDescription, Double quantity, String positingDate, String materialGroup) {
        this.plant = plant;
        this.division = division;
        this.material = material;
        this.materialDescription = materialDescription;
        this.quantity = quantity;
        this.postingDate = positingDate;
        this.materialGroup = materialGroup;
    }

    public DistributionStock(String material, String materialDescription, String supplier, int leadTime) {
        this.material = material;
        this.materialDescription = materialDescription;
        this.supplier = supplier;
        this.leadTime = leadTime;
    }

    public String getPlant() {
        return plant;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getMaterial() {
        return material;
    }

    public String getMaterialDescription() {
        return materialDescription;
    }

    public Double getQuantity() {
        return quantity;
    }

    public String getDivision() {
        return division;
    }

    public void setPlant(String plant) {
        this.plant = plant;
    }

    public String getPostingDate() {
        return postingDate;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getSupplier() {
        return supplier;
    }

    public int getLeadTime() {
        return leadTime;
    }

    public String getMaterialGroup() {
        return materialGroup;
    }
}
