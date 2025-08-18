package org.bluestarcloud.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ExcelStreamingReader {
    private static final Logger logger = LogManager.getLogger(ExcelService.class);

    public ExcelSheetData readExcel(InputStream inputStream, int sheetIndex) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(inputStream)) {
            XSSFReader reader = new XSSFReader(pkg);

            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);

            // pick sheet
            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) reader.getSheetsData();
            int i = 0;
            ExcelSheetData data = null;
            while (iter.hasNext()) {
                try (InputStream sheetStream = iter.next()) {
                    if (i == sheetIndex) {
                        data = processSheet(styles, strings, sheetStream);
                        break;
                    }
                }
                i++;
            }
            logger.info("Excel data read");
            return data;
        }
    }

    private ExcelSheetData processSheet(StylesTable styles,
                                        ReadOnlySharedStringsTable strings,
                                        InputStream sheetInputStream) throws Exception {

        SheetHandler handler = new SheetHandler(strings);
        XMLReader parser = fetchSheetParser(handler);
        InputSource sheetSource = new InputSource(sheetInputStream);
        parser.parse(sheetSource);

        return new ExcelSheetData(handler.getHeaderMap(), handler.getRowDataList());
    }

    private XMLReader fetchSheetParser(SheetHandler handler)
            throws SAXException, ParserConfigurationException {

        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        XMLReader parser = saxFactory.newSAXParser().getXMLReader();
        parser.setContentHandler(handler);
        return parser;
    }

    /**
     * SAX Handler that builds headerMap + rowDataList
     */
    private static class SheetHandler extends DefaultHandler {
        private final ReadOnlySharedStringsTable strings;
        private final Map<Integer, String> headerMap = new HashMap<>();
        private final List<Map<String, String>> rowDataList = new ArrayList<>();

        private List<String> currentRow = new ArrayList<>();
        private StringBuilder value = new StringBuilder();
        private boolean inValue = false;
        private String cellType;
        private int currentRowNum = -1;
        private int currentCol = -1;
        private int lastCol = -1;  // track last seen column index

        SheetHandler(ReadOnlySharedStringsTable strings) {
            this.strings = strings;
        }

        @Override
        public void startElement(String uri, String localName,
                                 String qName, Attributes attributes) {
            if ("row".equals(qName)) {
                currentRow = new ArrayList<>();
                String rowNum = attributes.getValue("r");
                currentRowNum = rowNum != null ? Integer.parseInt(rowNum) : -1;
                lastCol = -1;
            } else if ("c".equals(qName)) {
                value.setLength(0);
                inValue = true;
                cellType = attributes.getValue("t");

                // determine column index from "r" (e.g., "C5")
                String cellRef = attributes.getValue("r");
                currentCol = (cellRef != null) ? new CellReference(cellRef).getCol() : (lastCol + 1);

                // pad missing columns with ""
                while (lastCol + 1 < currentCol) {
                    currentRow.add("");  // empty cell
                    lastCol++;
                }
            } else if ("v".equals(qName) || "t".equals(qName)) {
                value.setLength(0);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inValue) {
                value.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("v".equals(qName) || "t".equals(qName)) {
                String cellValue = value.toString();

                if ("s".equals(cellType)) {
                    try {
                        int idx = Integer.parseInt(cellValue);
                        cellValue = strings.getItemAt(idx).toString();
                    } catch (Exception ignored) {
                    }
                } else if ("b".equals(cellType)) {
                    cellValue = "1".equals(cellValue) ? "TRUE" : "FALSE";
                }
                // other types → leave as raw

                currentRow.add(cellValue);
                lastCol = currentCol;
                inValue = false;
            } else if ("row".equals(qName)) {
                // pad trailing empty cells until max header size
                int headerSize = headerMap.size();
                while (currentRow.size() < headerSize) {
                    currentRow.add("");
                }

                if (currentRowNum == 1) {
                    for (int c = 0; c < currentRow.size(); c++) {
                        headerMap.put(c, currentRow.get(c));
                    }
                } else if (currentRowNum > 1) {
                    Map<String, String> rowData = new HashMap<>();
                    for (int c = 0; c < currentRow.size(); c++) {
                        String header = headerMap.get(c);
                        if (header != null) {
                            rowData.put(header, currentRow.get(c));
                        }
                    }
                    rowDataList.add(rowData);
                }
            }
        }

        public Map<Integer, String> getHeaderMap() {
            return headerMap;
        }

        public List<Map<String, String>> getRowDataList() {
            return rowDataList;
        }
    }
}
