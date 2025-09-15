package org.bluestarcloud.distribution;

public class PendingPR {
    private String material;
    private Double qtyRequested;
    private Double qtyOrdered;

    public PendingPR(String material, Double qtyRequested, Double qtyOrdered) {
        this.material = material;
        this.qtyRequested = qtyRequested;
        this.qtyOrdered = qtyOrdered;
    }

    public String getMaterial() {
        return material;
    }

    public Double getQtyRequested() {
        return qtyRequested;
    }

    public Double getQtyOrdered() {
        return qtyOrdered;
    }
}
