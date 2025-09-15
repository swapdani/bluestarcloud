package org.bluestarcloud.distribution;

public class PendingPO {
    private String material;
    private Double qtyToBeDelivered;

    public PendingPO(String material, Double qtyToBeDelivered) {
        this.material = material;
        this.qtyToBeDelivered = qtyToBeDelivered;
    }

    public String getMaterial() {
        return material;
    }

    public Double getQtyToBeDelivered() {
        return qtyToBeDelivered;
    }
}


