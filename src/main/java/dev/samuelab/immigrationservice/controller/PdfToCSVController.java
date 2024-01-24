package dev.samuelab.immigrationservice.controller;

import dev.samuelab.immigrationservice.dto.ResponseMessage;
import dev.samuelab.immigrationservice.services.PDFService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class PdfToCSVController {
    @Autowired
    PDFService pdfService;

    @PostMapping("/pdf")
    public ResponseEntity<ResponseMessage> uploadPDF(@RequestParam("file") MultipartFile file) throws IOException {
        pdfService.setSource(file.getInputStream());
        pdfService.extract();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ResponseMessage(pdfService.extract()));
    }

}
