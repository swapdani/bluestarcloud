package org.bluestarcloud;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.bluestarcloud.distribution.DistributionProcessor;
import org.bluestarcloud.mcoufo.McoUfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Controller
@RequestMapping("/api")
public class ExcelController {
    private static final Logger logger = LogManager.getLogger(ExcelController.class);

    @Autowired
    McoUfo mcoUfo;

    @Autowired
    DistributionProcessor distributionProcessor;

    @PostMapping("/process")
    public ResponseEntity<byte[]> processExcel(
            @RequestParam("typeParam") String typeParam,
            @RequestParam("reWork") boolean reWork,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        logger.info("Processing started for report: " + typeParam + ", reWork: " + reWork);

        Workbook processedWorkbook;

        if ("UFO".equalsIgnoreCase(typeParam)) {
            processedWorkbook = mcoUfo.processUfo(file);
        } else if ("Distribution".equalsIgnoreCase(typeParam)) {
            processedWorkbook = distributionProcessor.processDistribution(file, reWork);
        } else {
            String error = "Invalid report type";
            logger.error(error);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(error.getBytes());
        }

        logger.info("Processing completed");
        // Convert workbook to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        processedWorkbook.write(bos);
        processedWorkbook.close();

        byte[] excelBytes = bos.toByteArray();

        // Send file as download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName() + "_processed.xlsx");

        logger.info("Sending processed file for download");
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
