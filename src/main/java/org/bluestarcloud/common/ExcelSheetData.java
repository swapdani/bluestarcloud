package org.bluestarcloud.common;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ExcelSheetData {
    private Map<Integer, String> headerMap; // index -> column name
    private List<Map<String, String>> rows; // each row is a map of header -> value

    public ExcelSheetData(Map<Integer, String> headerMap, List<Map<String, String>> rows) {
        this.headerMap = headerMap;
        this.rows = rows;
    }

    public Map<Integer, String> getHeaderMap() {
        return headerMap;
    }

    public List<Map<String, String>> getRows() {
        return rows;
    }
}
