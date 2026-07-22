package com.ds4h.scratchradiomics.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ExcelExporter {

    /**
     * Genera un file Excel (.xlsx) formattato contenente i metadati dell'analisi
     * e tutte le feature radiomiche IBSI calcolate.
     *
     * @param destinationFile Il file .xlsx di destinazione
     * @param imageName       Il nome del file immagine analizzato
     * @param roiName         Il nome della ROI segmentata
     * @param features        La mappa contenente le feature (Nome -> Valore)
     * @throws IOException    In caso di errori nella scrittura del file
     */
    public static void exportToExcel(File destinationFile, String imageName, String roiName, Map<String, Double> features) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("IBSI Radiomics Report");

            final CellStyle headerStyle = workbook.createCellStyle();
            final Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);

            final CellStyle metaLabelStyle = workbook.createCellStyle();
            final Font metaFont = workbook.createFont();
            metaFont.setBold(true);
            metaLabelStyle.setFont(metaFont);

            final CellStyle numericStyle = workbook.createCellStyle();
            final DataFormat format = workbook.createDataFormat();
            numericStyle.setDataFormat(format.getFormat("0.0000"));
            numericStyle.setAlignment(HorizontalAlignment.RIGHT);

            int rowIdx = 0;

            final Row titleRow = sheet.createRow(rowIdx++);
            final Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SCRATCH-RADIOMICS - IBSI ANALYSIS REPORT");
            titleCell.setCellStyle(metaLabelStyle);

            rowIdx++;

            addRowMeta(sheet, rowIdx++, "Immagine Analizzata:", imageName, metaLabelStyle);
            addRowMeta(sheet, rowIdx++, "Regione di Interesse (ROI):", roiName, metaLabelStyle);

            final String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            addRowMeta(sheet, rowIdx++, "Data Generazione Report:", timestamp, metaLabelStyle);

            rowIdx++;

            final Row headerRow = sheet.createRow(rowIdx++);

            final Cell hCell0 = headerRow.createCell(0);
            hCell0.setCellValue("Feature Name / IBSI Code");
            hCell0.setCellStyle(headerStyle);

            final Cell hCell1 = headerRow.createCell(1);
            hCell1.setCellValue("Value");
            hCell1.setCellStyle(headerStyle);

            for (final Map.Entry<String, Double> entry : features.entrySet()) {
                final Row row = sheet.createRow(rowIdx++);

                final Cell nameCell = row.createCell(0);
                nameCell.setCellValue(entry.getKey());

                final Cell valCell = row.createCell(1);
                final Double val = entry.getValue();
                if (val != null && !Double.isNaN(val) && !Double.isInfinite(val)) {
                    valCell.setCellValue(val);
                    valCell.setCellStyle(numericStyle);
                } else {
                    valCell.setCellValue("N/A");
                }
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            try (final FileOutputStream fos = new FileOutputStream(destinationFile)) {
                workbook.write(fos);
            }
        }
    }

    private static void addRowMeta(Sheet sheet, int rowIdx, String label, String value, CellStyle labelStyle) {
        final Row row = sheet.createRow(rowIdx);
        final Cell cellLabel = row.createCell(0);
        cellLabel.setCellValue(label);
        cellLabel.setCellStyle(labelStyle);

        final Cell cellValue = row.createCell(1);
        cellValue.setCellValue(value != null ? value : "N/D");
    }
}
