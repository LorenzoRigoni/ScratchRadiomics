package com.ds4h.scratchradiomics.segmentation;

import com.ds4h.scratchradiomics.features.RadiomicsResultsDialog;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import org.scijava.app.StatusService;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import java.util.function.BiConsumer;

public class SegmentationGUI {

    private final ImagePlus image;
    private final UIService uiService;
    private final StatusService statusService;
    private MouseListener canvasClickListener;

    public SegmentationGUI(ImagePlus image, UIService uiService, StatusService statusService) {
        this.image = image;
        this.uiService = uiService;
        this.statusService = statusService;
    }

    /**
     * Activate listening on mouse click on the image Canvas.
     */
    public void enableInteractivePrompt(BiConsumer<Integer, Integer> onPointSelected) {
        if (image == null || image.getWindow() == null) {
            showNotification("No active image window found.");
            return;
        }

        final ImageWindow window = image.getWindow();
        final ImageCanvas canvas = window.getCanvas();

        detachMouseListener();

        updateStatus("Click on the scratch to start SAM algorithm...");

        canvasClickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final int realX = canvas.offScreenX(e.getX());
                final int realY = canvas.offScreenY(e.getY());

                updateStatus(
                        String.format("Prompt captured at coordinates: (%d, %d). Segmentation in progress...", realX, realY)
                );

                detachMouseListener();

                onPointSelected.accept(realX, realY);
            }
        };

        canvas.addMouseListener(canvasClickListener);
    }

    public void detachMouseListener() {
        if (image != null && image.getWindow() != null && canvasClickListener != null) {
            final ImageCanvas canvas = image.getWindow().getCanvas();
            canvas.removeMouseListener(canvasClickListener);
            canvasClickListener = null;
        }
    }

    public void showNotification(String message) {
        if (uiService != null) {
            uiService.showDialog(message);
        }
    }

    public void updateStatus(String status) {
        if (statusService != null) {
            statusService.showStatus(status);
        }
    }

    public void showResultsDialog(String imageName, String roiName, Map<String, Double> features) {
        SwingUtilities.invokeLater(() -> {
            final Frame parentFrame = (image != null && image.getWindow() != null) ? image.getWindow() : null;
            final RadiomicsResultsDialog dialog = new RadiomicsResultsDialog(parentFrame, imageName, roiName, features);
            dialog.setVisible(true);
        });
    }
}