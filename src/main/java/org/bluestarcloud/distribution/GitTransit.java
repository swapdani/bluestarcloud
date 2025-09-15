package org.bluestarcloud.distribution;

public class GitTransit {
    private String plant;
    private String materialNo;
    private Double gitQty;

    public GitTransit(String plant, String materialNo, Double gitQty) {
        this.plant = plant;
        this.materialNo = materialNo;
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

    public Double getGitQty() {
        return gitQty;
    }
}
