package org.bluestarcloud.distribution;

import java.util.HashMap;
import java.util.Map;

public class DistributionHeaders {
    private final static Map<Integer, String> distributionHeaders = new HashMap<>();

    public static Map<Integer, String> getDistributionHeaders() {
        int rowCount = 0;
        distributionHeaders.put(rowCount++, "Material");
        distributionHeaders.put(rowCount++, "Material Description");
        distributionHeaders.put(rowCount++, "W15C Stock");
        distributionHeaders.put(rowCount++, "W15C Eligibility");
        distributionHeaders.put(rowCount++, "Total Stock To Transfer");
        distributionHeaders.put(rowCount++, "Total Stock");
        distributionHeaders.put(rowCount++, "Total Consumption");
        distributionHeaders.put(rowCount++, "Current Stock Availability Status");

        distributionHeaders.put(rowCount++, "E06C Stock");
        distributionHeaders.put(rowCount++, "E06C In Transit");
        distributionHeaders.put(rowCount++, "E06C Consumption");
        distributionHeaders.put(rowCount++, "E06C Stock To Transfer");

        distributionHeaders.put(rowCount++, "E16C Stock");
        distributionHeaders.put(rowCount++, "E16C In Transit");
        distributionHeaders.put(rowCount++, "E16C Consumption");
        distributionHeaders.put(rowCount++, "E16C Stock To Transfer");

        distributionHeaders.put(rowCount++, "E26C Stock");
        distributionHeaders.put(rowCount++, "E26C In Transit");
        distributionHeaders.put(rowCount++, "E26C Consumption");
        distributionHeaders.put(rowCount++, "E26C Stock To Transfer");

        distributionHeaders.put(rowCount++, "E33C Stock");
        distributionHeaders.put(rowCount++, "E33C In Transit");
        distributionHeaders.put(rowCount++, "E33C Consumption");
        distributionHeaders.put(rowCount++, "E33C Stock To Transfer");

        distributionHeaders.put(rowCount++, "E41C Stock");
        distributionHeaders.put(rowCount++, "E41C In Transit");
        distributionHeaders.put(rowCount++, "E41C Consumption");
        distributionHeaders.put(rowCount++, "E41C Stock To Transfer");

        distributionHeaders.put(rowCount++, "N06C Stock");
        distributionHeaders.put(rowCount++, "N06C In Transit");
        distributionHeaders.put(rowCount++, "N06C Consumption");
        distributionHeaders.put(rowCount++, "N06C Stock To Transfer");

        distributionHeaders.put(rowCount++, "N13C Stock");
        distributionHeaders.put(rowCount++, "N13C In Transit");
        distributionHeaders.put(rowCount++, "N13C Consumption");
        distributionHeaders.put(rowCount++, "N13C Stock To Transfer");

        distributionHeaders.put(rowCount++, "N33C Stock");
        distributionHeaders.put(rowCount++, "N33C In Transit");
        distributionHeaders.put(rowCount++, "N33C Consumption");
        distributionHeaders.put(rowCount++, "N33C Stock To Transfer");

        distributionHeaders.put(rowCount++, "N34C Stock");
        distributionHeaders.put(rowCount++, "N34C In Transit");
        distributionHeaders.put(rowCount++, "N34C Consumption");
        distributionHeaders.put(rowCount++, "N34C Stock To Transfer From W15C");
        distributionHeaders.put(rowCount++, "N34C Stock To Transfer From N33C");

        distributionHeaders.put(rowCount++, "N48C Stock");
        distributionHeaders.put(rowCount++, "N48C In Transit");
        distributionHeaders.put(rowCount++, "N48C Consumption");
        distributionHeaders.put(rowCount++, "N48C Stock To Transfer");

        distributionHeaders.put(rowCount++, "N57C Stock");
        distributionHeaders.put(rowCount++, "N57C In Transit");
        distributionHeaders.put(rowCount++, "N57C Consumption");
        distributionHeaders.put(rowCount++, "N57C Stock To Transfer");

        distributionHeaders.put(rowCount++, "N66C Stock");
        distributionHeaders.put(rowCount++, "N66C In Transit");
        distributionHeaders.put(rowCount++, "N66C Consumption");
        distributionHeaders.put(rowCount++, "N66C Stock To Transfer");

        distributionHeaders.put(rowCount++, "N64C Stock");
        distributionHeaders.put(rowCount++, "N64C In Transit");
        distributionHeaders.put(rowCount++, "N64C Consumption");
        distributionHeaders.put(rowCount++, "N64C Stock To Transfer From W15C");
        distributionHeaders.put(rowCount++, "N64C Stock To Transfer From N66C");

        distributionHeaders.put(rowCount++, "N76C Stock");
        distributionHeaders.put(rowCount++, "N76C In Transit");
        distributionHeaders.put(rowCount++, "N76C Consumption");
        distributionHeaders.put(rowCount++, "N76C Stock To Transfer");

        distributionHeaders.put(rowCount++, "S06C Stock");
        distributionHeaders.put(rowCount++, "S06C In Transit");
        distributionHeaders.put(rowCount++, "S06C Consumption");
        distributionHeaders.put(rowCount++, "S06C Stock To Transfer");

        distributionHeaders.put(rowCount++, "S66C Stock");
        distributionHeaders.put(rowCount++, "S66C In Transit");
        distributionHeaders.put(rowCount++, "S66C Consumption");
        distributionHeaders.put(rowCount++, "S66C Stock To Transfer From W15C");
        distributionHeaders.put(rowCount++, "S66C Stock To Transfer From S06C");

        distributionHeaders.put(rowCount++, "S16C Stock");
        distributionHeaders.put(rowCount++, "S16C In Transit");
        distributionHeaders.put(rowCount++, "S16C Consumption");
        distributionHeaders.put(rowCount++, "S16C Stock To Transfer");

        distributionHeaders.put(rowCount++, "S49C Stock");
        distributionHeaders.put(rowCount++, "S49C In Transit");
        distributionHeaders.put(rowCount++, "S49C Consumption");
        distributionHeaders.put(rowCount++, "S49C Stock To Transfer From W15C");
        distributionHeaders.put(rowCount++, "S49C Stock To Transfer From S16C");

        distributionHeaders.put(rowCount++, "S26C Stock");
        distributionHeaders.put(rowCount++, "S26C In Transit");
        distributionHeaders.put(rowCount++, "S26C Consumption");
        distributionHeaders.put(rowCount++, "S26C Stock To Transfer");

        distributionHeaders.put(rowCount++, "S36C Stock");
        distributionHeaders.put(rowCount++, "S36C In Transit");
        distributionHeaders.put(rowCount++, "S36C Consumption");
        distributionHeaders.put(rowCount++, "S36C Stock To Transfer");

        distributionHeaders.put(rowCount++, "S33C Stock");
        distributionHeaders.put(rowCount++, "S33C In Transit");
        distributionHeaders.put(rowCount++, "S33C Consumption");
        distributionHeaders.put(rowCount++, "S33C Stock To Transfer From W15C");
        distributionHeaders.put(rowCount++, "S33C Stock To Transfer From S36C");

        distributionHeaders.put(rowCount++, "S54C Stock");
        distributionHeaders.put(rowCount++, "S54C In Transit");
        distributionHeaders.put(rowCount++, "S54C Consumption");
        distributionHeaders.put(rowCount++, "S54C Stock To Transfer");

        distributionHeaders.put(rowCount++, "S50C Stock");
        distributionHeaders.put(rowCount++, "S50C In Transit");
        distributionHeaders.put(rowCount++, "S50C Consumption");
        distributionHeaders.put(rowCount++, "S50C Stock To Transfer From W15C");
        distributionHeaders.put(rowCount++, "S50C Stock To Transfer From S54C");

        distributionHeaders.put(rowCount++, "W06C Stock");
        distributionHeaders.put(rowCount++, "W06C In Transit");
        distributionHeaders.put(rowCount++, "W06C Consumption");
        distributionHeaders.put(rowCount++, "W06C Stock To Transfer");

        distributionHeaders.put(rowCount++, "W11C Stock");
        distributionHeaders.put(rowCount++, "W11C In Transit");
        distributionHeaders.put(rowCount++, "W11C Consumption");
        distributionHeaders.put(rowCount++, "W11C Stock To Transfer");

        distributionHeaders.put(rowCount++, "W14C Stock");
        distributionHeaders.put(rowCount++, "W14C In Transit");
        distributionHeaders.put(rowCount++, "W14C Consumption");
        distributionHeaders.put(rowCount++, "W14C Stock To Transfer From W15C");
        distributionHeaders.put(rowCount++, "W14C Stock To Transfer From W11C");

        distributionHeaders.put(rowCount++, "W17C Stock");
        distributionHeaders.put(rowCount++, "W17C In Transit");
        distributionHeaders.put(rowCount++, "W17C Consumption");
        distributionHeaders.put(rowCount++, "W17C Stock To Transfer");

        distributionHeaders.put(rowCount++, "W31C Stock");
        distributionHeaders.put(rowCount++, "W31C In Transit");
        distributionHeaders.put(rowCount++, "W31C Consumption");
        distributionHeaders.put(rowCount++, "W31C Stock To Transfer");

        distributionHeaders.put(rowCount++, "W41C Stock");
        distributionHeaders.put(rowCount++, "W41C In Transit");
        distributionHeaders.put(rowCount++, "W41C Consumption");
        distributionHeaders.put(rowCount++, "W41C Stock To Transfer");

        distributionHeaders.put(rowCount++, "W51C Stock");
        distributionHeaders.put(rowCount++, "W51C In Transit");
        distributionHeaders.put(rowCount++, "W51C Consumption");
        distributionHeaders.put(rowCount++, "W51C Stock To Transfer");

        distributionHeaders.put(rowCount++, "W66C Stock");
        distributionHeaders.put(rowCount++, "W66C In Transit");
        distributionHeaders.put(rowCount++, "W66C Consumption");
        distributionHeaders.put(rowCount++, "W66C Stock To Transfer");

        distributionHeaders.put(rowCount++, "W76C Stock");
        distributionHeaders.put(rowCount++, "W76C In Transit");
        distributionHeaders.put(rowCount++, "W76C Consumption");
        distributionHeaders.put(rowCount, "W76C Stock To Transfer");

        return distributionHeaders;
    }
}
