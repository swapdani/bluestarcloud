package org.bluestarcloud.distribution;

public class PlantMapping {
    private String plant;
    private String newPlant;
    private String percentage;

    public PlantMapping(String plant, String newPlant, String percentage) {
        this.plant = plant;
        this.newPlant = newPlant;
        this.percentage = percentage;
    }

    public String getPlant() {
        return plant;
    }

    public String getNewPlant() {
        return newPlant;
    }

    public String getPercentage() {
        return percentage;
    }
}
