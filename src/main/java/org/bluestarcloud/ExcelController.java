package org.bluestarcloud;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ExcelController {

    @PostMapping("/process")
    public ResponseEntity<byte[]> processExcel(
            @RequestParam("textParam") String textParam,
            @RequestParam("file") MultipartFile file) throws IOException {

        System.out.println("Inside controller");
        // TODO: Read uploaded file if needed
        // Workbook uploadedWorkbook = new XSSFWorkbook(file.getInputStream());

        // Create a new Excel file
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Processed Data");

        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Processed Text");
        row.createCell(1).setCellValue(textParam);

        // Add more processing logic here...

        // Convert workbook to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        byte[] excelBytes = bos.toByteArray();

        // Send file as download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=processed.xlsx");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
