package com.ds4h.scratchradiomics;

import com.ds4h.scratchradiomics.features.IbsiFeatureExtractor;
import com.ds4h.scratchradiomics.segmentation.SamEngine;
import com.ds4h.scratchradiomics.segmentation.SamPostProcessor;
import com.ds4h.scratchradiomics.segmentation.SegmentationGUI;
import ij.ImagePlus;
import ij.gui.Roi;

import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins > ScratchRadiomics")
public class ScratchRadiomicsPlugin implements Command {

    @Parameter
    private UIService uiService;

    @Parameter
    private StatusService statusService;

    @Parameter(label = "Input Image", required = true)
    private ImagePlus currentImage;

    @Parameter(label = "ROI Name")
    private String roiName;

    @Parameter(label = "Enable Manual Click Prompt")
    private boolean enableClickPrompt;

    @Parameter(visibility = ItemVisibility.MESSAGE, label = "--- IBSI Radiomics Options ---")
    private final String radiomicsHeader = "";

    @Parameter(label = "Extract IBSI Features")
    private boolean extractFeatures;

    @Parameter(label = "Export Report to Excel (.xlsx)")
    private boolean exportExcel;

    @Override
    public void run() {
        if (currentImage == null) {
            uiService.showDialog("Open an image on Fiji");
            return;
        }

        final SegmentationGUI gui = new SegmentationGUI(currentImage, uiService, statusService);

        if (enableClickPrompt) {
            gui.enableInteractivePrompt((x, y) -> processSegmentation(x, y, gui));
        } else {
            final int centerX = currentImage.getWidth() / 2;
            final int centerY = currentImage.getHeight() / 2;
            processSegmentation(centerX, centerY, gui);
        }
    }

    private void processSegmentation(int clickX, int clickY, SegmentationGUI gui) {
        try (final SamEngine samEngine = new SamEngine()) {
            gui.updateStatus("Loading SAM inference...");

            final float[] dummyEmbeddings = new float[256 * 64 * 64];

            final boolean[][] mask = samEngine.predictMask(
                    dummyEmbeddings, clickX, clickY,
                    currentImage.getWidth(), currentImage.getHeight()
            );

            final Roi roi = SamPostProcessor.convertMaskToRoiAndAddToManager(mask, currentImage, roiName);

            if (roi != null) {
                gui.updateStatus("Segmentation completed with success!");

                if (extractFeatures) {
                    gui.updateStatus("Calculating IBSI radiomic features...");
                    final Map<String, Double> ibsiFeatures = IbsiFeatureExtractor.extractFeatures(currentImage, roi);
                    gui.showResultsDialog(currentImage.getTitle(), roiName, ibsiFeatures);
                    gui.updateStatus("Radiomic analysis completed with success!");
                }
            } else {
                gui.showNotification("No identified region in the selected point");
            }

        } catch (Exception e) {
            gui.showNotification("Error during the segmentation: " + e.getMessage());
        }
    }
}
