package com.ds4h.scratchradiomics.features;


import com.ds4h.scratchradiomics.export.ExcelExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.Map;

public class RadiomicsResultsDialog extends JDialog {

    private final Map<String, Double> features;
    private final String imageName;
    private final String roiName;

    public RadiomicsResultsDialog(Frame parent, String imageName, String roiName, Map<String, Double> features) {
        super(parent, "ScratchRadiomics - IBSI Radiomics Features", true);
        this.imageName = imageName;
        this.roiName = roiName;
        this.features = features;

        initGUI();
    }

    private void initGUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(550, 450);
        setLocationRelativeTo(getOwner());

        final JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        headerPanel.add(new JLabel("Immagine: " + imageName));
        headerPanel.add(new JLabel("ROI: " + roiName));
        add(headerPanel, BorderLayout.NORTH);

        final String[] columnNames = {"Feature Name / IBSI Code", "Value"};
        final DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        features.forEach((key, value) -> {
            final Object[] row = {key, String.format("%.4f", value)};
            tableModel.addRow(row);
        });

        final JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        final JButton btnExport = new JButton("Export in Excel (.xlsx)");
        btnExport.addActionListener(e -> onExportExcel());

        final JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        buttonPanel.add(btnExport);
        buttonPanel.add(btnClose);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void onExportExcel() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Radiomic Report in Excel");
        fileChooser.setSelectedFile(new File("ScratchRadiomics_Report.xlsx"));

        final int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
            }

            try {
                ExcelExporter.exportToExcel(fileToSave, imageName, roiName, features);
                JOptionPane.showMessageDialog(this,
                        "Report exported with success in:\n" + fileToSave.getAbsolutePath(),
                        "Exportation completed", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error during the exportation in Excel: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
