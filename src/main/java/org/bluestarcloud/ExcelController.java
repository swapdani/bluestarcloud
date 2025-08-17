package org.bluestarcloud;

import org.apache.poi.ss.usermodel.Workbook;
import org.bluestarcloud.mcoufo.McoUfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ExcelController {

    @Autowired
    McoUfo mcoUfo;

    @PostMapping("/process")
    public ResponseEntity<byte[]> processExcel(
//            @RequestParam("textParam") String textParam,
            @RequestParam("file") MultipartFile file) throws IOException {

        Workbook processedWorkbook = mcoUfo.processUfo(file);

        // Convert workbook to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        processedWorkbook.write(bos);
        processedWorkbook.close();

        byte[] excelBytes = bos.toByteArray();

        // Send file as download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName() + "_processed.xlsx");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
