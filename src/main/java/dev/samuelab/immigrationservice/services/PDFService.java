package dev.samuelab.immigrationservice.services;


import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import dev.samuelab.immigrationservice.model.TableCell;
import dev.samuelab.immigrationservice.model.TableRow;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PDFService {

    private final Logger logger = LoggerFactory.getLogger(PDFService.class);
    // contains pages that will be extracted table content.
    // If this variable doesn't contain any page, all pages will be extracted
    private final List<Integer> extractedPages = new ArrayList<>();
    private final List<Integer> exceptedPages = new ArrayList<>();
    // contains avoided line idx-s for each page,
    // if this multimap contains only one element and key of this element equals -1
    // then all lines in extracted pages contains in multimap value will be avoided
    private final Multimap<Integer, Integer> pageNExceptedLinesMap = HashMultimap.create();

    private InputStream inputStream;
    private PDDocument document;
    private String password;

    public PDFService setSource(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public PDFService setSource(InputStream inputStream, String password) {
        this.inputStream = inputStream;
        this.password = password;
        return this;
    }

    public PDFService setSource(File file) {
        try {
            return this.setSource(new FileInputStream(file));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Invalid pdf file", ex);
        }
    }

    public PDFService setSource(String filePath) {
        return this.setSource(new File(filePath));
    }

    public PDFService setSource(File file, String password) {
        try {
            return this.setSource(new FileInputStream(file), password);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Invalid pdf file", ex);
        }
    }

    public PDFService setSource(String filePath, String password) {
        return this.setSource(new File(filePath), password);
    }

//    /**
//     * This page will be analyzed and extract its table content
//     *
//     * @param pageIdx
//     * @return
//     */
    public PDFService addPage(int pageIdx) {
        extractedPages.add(pageIdx);
        return this;
    }

    public PDFService exceptPage(int pageIdx) {
        exceptedPages.add(pageIdx);
        return this;
    }

//    /**
//     * Avoid a specific line in a specific page. LineIdx can be negative number, -1
//     * is the last line
//     *
//     * @param pageIdx
//     * @param lineIdxes
//     * @return
//     */
    public PDFService exceptLine(int pageIdx, int[] lineIdxes) {
        for (int lineIdx : lineIdxes) {
            pageNExceptedLinesMap.put(pageIdx, lineIdx);
        }
        return this;
    }

//    /**
//     * Avoid this line in all extracted pages. LineIdx can be negative number, -1 is
//     * the last line
//     *
//     * @param lineIdxes
//     * @return
//     */
    public PDFService exceptLine(int[] lineIdxes) {
        this.exceptLine(-1, lineIdxes);
        return this;
    }

    public String extract() throws IOException {
        Multimap<Integer, Range<Integer>> pageIdNLineRangesMap = LinkedListMultimap.create();
        Multimap<Integer, List<TextPosition>> pageIdNTextsMap = LinkedListMultimap.create();
        Multimap<Integer, List<Range<Integer>>> pageIdNColumnRangesMap = LinkedListMultimap.create();
        StringBuilder resultSb = new StringBuilder();

        try {
            this.document = this.password != null ? Loader.loadPDF(inputStream.readAllBytes(), this.password)
                    : Loader.loadPDF(inputStream.readAllBytes());

            for (int pageId = 0; pageId < document.getNumberOfPages(); pageId++) {
                boolean b = !exceptedPages.contains(pageId)
                        && (extractedPages.isEmpty() || extractedPages.contains(pageId));
                if (b) {
                    List<TextPosition> textPositions = extractTextPositions(pageId);// sorted by .getY() ASC
                    // extract line ranges
                    List<Range<Integer>> textPosionsOfRows = getLineRanges(pageId, textPositions); // get All rows
                    List<List<TextPosition>> textsInRows = new ArrayList<>();
                    // loop each row
                    for (Range<Integer> textPositionOfRow : textPosionsOfRows) {
                        // extract column ranges
                        List<TextPosition> textsByRow = getTextsByRow(textPositionOfRow, textPositions);
                        textsInRows.add(textsByRow);
                    }

                    // Calculate columnRanges
                    List<List<Range<Integer>>> columnRangesArray = new ArrayList<>(); // columRanges for each row
                    for (List<TextPosition> texts : textsInRows) {

                        List<Range<Integer>> columnRanges = getColumnRanges(texts);
                        columnRangesArray.add(columnRanges);
                    }
                    pageIdNLineRangesMap.putAll(pageId, textPosionsOfRows); // row
                    pageIdNTextsMap.putAll(pageId, textsInRows); // column
                    pageIdNColumnRangesMap.putAll(pageId, columnRangesArray);
                }
                // System.out.println(pageIdNTextsMap);

                // System.out.println(pageIdNColumnRangesMap);
            }

            // loop each page
            for (int pageId : pageIdNTextsMap.keySet()) {
                String resultStr = buildTable((List) pageIdNTextsMap.get(pageId), (List) pageIdNLineRangesMap.get(pageId), (List<List<Range<Integer>>>) pageIdNColumnRangesMap.get(pageId));
                resultSb.append(resultStr);
                resultSb.append("\n\n");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Parse pdf file fail", ex);
        } finally {
            if (this.document != null) {
                try {
                    this.document.close();
                } catch (IOException ex) {
                    logger.error(null, ex);
                }
            }
        }
        FileWriter csvWriter = new FileWriter("output.csv");
        csvWriter.write(String.valueOf(resultSb));
        return resultSb.toString();
    }

//    /**
//     * get all text in a row
//     *
//     * @param lineRanges
//     * @param textPositions
//     * @return
//     */
    private List<TextPosition> getTextsByRow(Range<Integer> lineRange, List<TextPosition> textPositions) {
        List<TextPosition> retVal = new ArrayList<>();
        int idx = 0;

        while (idx < textPositions.size()) {
            TextPosition textPosition = textPositions.get(idx);

            Range<Integer> textRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));

            if (lineRange.encloses(textRange)) {
                retVal.add(textPosition);
                idx++;
            } else {
                idx++;
            }
        }
        return retVal;
    }

//    /**
//     * @param texts
//     * @return
//     */
    private List<Range<Integer>> getColumnRanges(List<TextPosition> texts) {

        TrapRangeBuilder rangesBuilder = new TrapRangeBuilder();
        for (TextPosition text : texts) {

            // System.out.println(text.getX());

            Range<Integer> range = Range.closed((int) text.getX(), (int) (text.getX() + text.getWidth()));
            rangesBuilder.addRange(range);

        }
        return rangesBuilder.build();
    }

//    /**
//     * Texts in tableContent have been ordered by .getY() ASC
//     *
//     * @param pageIdx
//     * @param tableContent
//     * @param rowTrapRanges
//     * @param collection
//     * @return
//     */
    private String buildTable(List<List<TextPosition>> tableContents, List<Range<Integer>> rowTrapRanges,
                              List<List<Range<Integer>>> columnRanges) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableContents.size(); i++) {
            boolean flag = false;
            if (i != 0 && columnRanges.get(i).size() == 1 && columnRanges.get(i - 1).size() > 1) {
                Range<Integer> temp = columnRanges.get(i).get(0);
                columnRanges.get(i).clear();
                for (int j = 0; j < columnRanges.get(i - 1).size(); j++) {
                    if (columnRanges.get(i - 1).get(j).encloses(temp)) {
                        flag = true;
                        columnRanges.get(i).add(temp);
                    } else {
                        columnRanges.get(i).add(columnRanges.get(i - 1).get(j));
                    }

                }
                if (!flag)
                    columnRanges.get(i).clear();
                columnRanges.get(i).add(temp);
            }

            TableRow row = buildRow(i, tableContents.get(i), columnRanges.get(i));
            sb.append(row);
            sb.append("\n");
        }
        return sb.toString();
    }

//    /**
//     *
//     * @param rowIdx
//     * @param rowContent
//     * @param columnTrapRanges
//     * @return
//     */
    private TableRow buildRow(int rowIdx, List<TextPosition> rowContent, List<Range<Integer>> columnTrapRanges) {
        TableRow retVal = new TableRow(rowIdx);
        // Sort rowContent
        Collections.sort(rowContent, new Comparator<TextPosition>() {
            @Override
            public int compare(TextPosition o1, TextPosition o2) {
                int retVal = 0;
                if (o1.getX() < o2.getX()) {
                    retVal = -1;
                } else if (o1.getX() > o2.getX()) {
                    retVal = 1;
                }
                return retVal;
            }
        });
        int idx = 0;
        int columnIdx = 0;
        List<TextPosition> cellContent = new ArrayList<>();
        while (idx < rowContent.size()) {
            TextPosition textPosition = rowContent.get(idx);
            Range<Integer> columnTrapRange = columnTrapRanges.get(columnIdx);
            Range<Integer> textRange = Range.closed((int) textPosition.getX(),
                    (int) (textPosition.getX() + textPosition.getWidth()));
            if (columnTrapRange.encloses(textRange)) {
                cellContent.add(textPosition);
                idx++;
            } else {
                TableCell cell = buildCell(columnIdx, cellContent);
                retVal.getCells().add(cell);
                // next column: clear cell content
                cellContent.clear();
                columnIdx++;
            }
        }
        if (!cellContent.isEmpty() && columnIdx < columnTrapRanges.size()) {
            TableCell cell = buildCell(columnIdx, cellContent);
            retVal.getCells().add(cell);
        }
        // return
        return retVal;
    }

    private TableCell buildCell(int columnIdx, List<TextPosition> cellContent) {

        String regex = "(?<=[\\d])(,)(?=[\\d])";
        Pattern p = Pattern.compile(regex);

        cellContent.sort((o1, o2) -> {
            int retVal = 0;
            if (o1.getX() < o2.getX()) {
                retVal = -1;
            } else if (o1.getX() > o2.getX()) {
                retVal = 1;
            }
            return retVal;
        });

        StringBuilder cellContentBuilder = new StringBuilder();
        for (TextPosition textPosition : cellContent) {
            cellContentBuilder.append(textPosition.getUnicode());
        }
        String cellContentString = cellContentBuilder.toString();
        // remove comma in number 1,234.87
        Matcher m = p.matcher(cellContentString);
        cellContentString = m.replaceAll("");
        return new TableCell(columnIdx, cellContentString);
    }

    private List<TextPosition> extractTextPositions(int pageId) throws IOException {
        TextPositionExtractor extractor = new TextPositionExtractor(document, pageId);
        return extractor.extract();
    }

    private boolean isExceptedLine(int lineIdx, int pageIdx) {
        return this.pageNExceptedLinesMap.containsEntry(pageIdx, lineIdx)
                || this.pageNExceptedLinesMap.containsEntry(-1, lineIdx);
    }

    private List<Range<Integer>> getLineRanges(int pageId, List<TextPosition> pageContent) {
        TrapRangeBuilder lineTrapRangeBuilder = new TrapRangeBuilder();
        for (TextPosition textPosition : pageContent) {
            Range<Integer> lineRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));
            // add to builder
            lineTrapRangeBuilder.addRange(lineRange);
        }
        List<Range<Integer>> lineTrapRanges = lineTrapRangeBuilder.build();
        return removeExceptedLines(pageId, lineTrapRanges);
    }

    private List<Range<Integer>> removeExceptedLines(int pageIdx, List<Range<Integer>> lineTrapRanges) {
        List<Range<Integer>> retVal = new ArrayList<>();
        for (int lineIdx = 0; lineIdx < lineTrapRanges.size(); lineIdx++) {
            boolean isExceptedLine = isExceptedLine(lineIdx, pageIdx)
                    || isExceptedLine(lineIdx - lineTrapRanges.size(), pageIdx);
            if (!isExceptedLine) {
                retVal.add(lineTrapRanges.get(lineIdx));
            }
        }
        return retVal;
    }

    private static class TextPositionExtractor extends PDFTextStripper {

        private final List<TextPosition> textPositions = new ArrayList<>();
        private final int pageId;

        private TextPositionExtractor(PDDocument document, int pageId) throws IOException {
            super();
            super.setSortByPosition(true);
            super.document = document;
            this.pageId = pageId;
        }

        public void stripPage(int pageId) throws IOException {
            this.setStartPage(pageId + 1);
            this.setEndPage(pageId + 1);
            try (Writer writer = new OutputStreamWriter(new ByteArrayOutputStream())) {
                writeText(document, writer);
            }
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            this.textPositions.addAll(textPositions);
        }

//        /**
//         * and order by textPosition.getY() ASC
//         *
//         * @return
//         * @throws IOException
//         */
        private List<TextPosition> extract() throws IOException {
            this.stripPage(pageId);
            // sort
            textPositions.sort(new Comparator<TextPosition>() {
                @Override
                public int compare(TextPosition o1, TextPosition o2) {
                    int retVal = 0;
                    if (o1.getY() < o2.getY()) {
                        retVal = -1;
                    } else if (o1.getY() > o2.getY()) {
                        retVal = 1;
                    }
                    return retVal;

                }
            });
            return this.textPositions;
        }
    }
}