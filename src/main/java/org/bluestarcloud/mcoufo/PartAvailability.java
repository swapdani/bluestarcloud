package org.bluestarcloud.mcoufo;

public class PartAvailability {
    private String plant;
    private String sLoc;
    private String materialNo;
    private String materialDesc;
    private Double availableQty;

    PartAvailability(String plant, String sLoc, String materialNo, String materialDesc, Double availableQty) {
        this.plant = plant;
        this.sLoc = sLoc;
        this.materialNo = materialNo;
        this.materialDesc = materialDesc;
        this.availableQty = availableQty;
    }

    String getPlant() {
        return plant;
    }

    String getMaterialNo() {
        return materialNo;
    }

    Double getAvailableQty() {
        return availableQty;
    }
}
