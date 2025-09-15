package org.bluestarcloud.distribution;

import java.util.HashMap;
import java.util.Map;

public class DistributionPlanningHeaders {

    private final static Map<Integer, String> distributionPlanningHeaders = new HashMap<>();

    public static Map<Integer, String> getDistributionPlanningHeaders() {
        int rowCount = 0;
        distributionPlanningHeaders.put(rowCount++, "Material");
        distributionPlanningHeaders.put(rowCount++, "Description");
        distributionPlanningHeaders.put(rowCount++, "Supplier");
        distributionPlanningHeaders.put(rowCount++, "Stock");
        distributionPlanningHeaders.put(rowCount++, "Lead Time");
        distributionPlanningHeaders.put(rowCount, "Qty To Arrange");
        return distributionPlanningHeaders;
    }
}
