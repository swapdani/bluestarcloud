package org.bluestarcloud.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
    private static final Logger logger = LogManager.getLogger(Utils.class);

    public static String getYYYYMMDD(String date, String dateSeperater) {
        String newSeperater = "/";
        date = date.replace(dateSeperater, newSeperater);
        String[] dateArray = date.split(newSeperater);
        return dateArray.length == 3 ? dateArray[2].trim() + dateArray[1].trim() + dateArray[0].trim() : "Incorrect Date Format";
    }

    public static String getPlantMaterialKey(String plant, String materialNo) {
        return getKey(plant, materialNo);
    }

    public static String getMaterialVendorKey(String material, String vendor) {
        return getKey(material, vendor);
    }

    private static String getKey(String val1, String val2) {
        if (val1 != null && val2 != null) {
            val1 = val1.trim();
            val2 = val2.trim();
        } else {
            logger.error("Error in getting key.");
        }
        return val1 + "_" + val2;
    }


    public static String getCorrectDecimalForDouble(Double number, int decimalPoints) {
        return number != null ? number % 1 != 0 ? String.format("%." + decimalPoints + "f", number) : String.format("%.0f", number) : "0";
    }

    public static BigDecimal getPercentage(long numerator, long denominator, int scale) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 5, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(scale, RoundingMode.HALF_UP);
    }
}
