package dev.samuelab.immigrationservice.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class PDFHelper {

    public static Stream<String> convertPDFToCSV(MultipartFile pdfFile) throws IOException {
        File pdf = convertMultipartFileToFile(pdfFile);
        PDDocument document = Loader.loadPDF(pdf);


        // Create a PDFTextStripper object
        PDFTextStripper stripper = new PDFTextStripper();
        String contents = stripper.getText(document);
        Stream<String> lines = Arrays.stream(contents.split(stripper.getLineSeparator()));

        document.close();
        return lines.filter(s -> !s.startsWith("No.") && StringUtils.isAlphanumeric(s.replaceAll(" ","")));

    }

    public static File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        boolean fileCreated = convFile.createNewFile();
        if (!fileCreated) {
            boolean fileDeleted = convFile.delete();
            if (fileDeleted)
                org.apache.commons.io.FileUtils.writeByteArrayToFile(convFile, file.getBytes());
        } else {
            org.apache.commons.io.FileUtils.writeByteArrayToFile(convFile, file.getBytes());
        }
        return convFile;
    }
}
