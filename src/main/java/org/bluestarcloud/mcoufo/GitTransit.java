package org.bluestarcloud.mcoufo;

public class GitTransit {
    private String plant;
    private String materialNo;
    private String supplyingPlant;
    private Double gitQty;

    public GitTransit(String plant, String materialNo, String supplyingPlant, Double gitQty) {
        this.plant = plant;
        this.materialNo = materialNo;
        this.supplyingPlant = supplyingPlant;
        this.gitQty = gitQty;
    }

    public String getPlant() {
        return plant;
    }

    public void setPlant(String plant) {
        this.plant = plant;
    }

    public String getMaterialNo() {
        return materialNo;
    }

    public String getSupplyingPlant() {
        return supplyingPlant;
    }

    public Double getGitQty() {
        return gitQty;
    }
}
