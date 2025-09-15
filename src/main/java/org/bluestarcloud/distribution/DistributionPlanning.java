package org.bluestarcloud.distribution;

public class DistributionPlanning {
    private String material;
    private String description;
    private String supplier;
    private String consumptionQty;
    private String stock;
    private String leadTime;
    private String qtyToArrange;

    public DistributionPlanning(String material, String description, String supplier, String consumptionQty, String stock, String leadTime, String qtyToArrange) {
        this.material = material;
        this.description = description;
        this.supplier = supplier;
        this.consumptionQty = consumptionQty;
        this.stock = stock;
        this.leadTime = leadTime;
        this.qtyToArrange = qtyToArrange;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getLeadTime() {
        return leadTime;
    }

    public void setLeadTime(String leadTime) {
        this.leadTime = leadTime;
    }

    public String getQtyToArrange() {
        return qtyToArrange;
    }

    public void setQtyToArrange(String qtyToArrange) {
        this.qtyToArrange = qtyToArrange;
    }

    public String getConsumptionQty() {
        return consumptionQty;
    }

    public void setConsumptionQty(String consumptionQty) {
        this.consumptionQty = consumptionQty;
    }
}
