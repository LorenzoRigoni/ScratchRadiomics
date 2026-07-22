package com.ds4h.scratchradiomics.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;

import java.awt.Polygon;

public class SamPostProcessor {

    /**
     * Converts a boolean matrix (segmentation mask) into an ImageJ ROI
     * and adds it to the active RoiManager.
     *
     * @param binaryMask Boolean matrix [height][width] representing the SAM segmentation mask
     * @param imp The ImagePlus image opened in Fiji to which the ROI will be applied
     * @param roiName The name used to identify the ROI in the RoiManager (e.g., "Scratch_SAM")
     * @return The created ROI (PolygonRoi)
     */
    public static Roi convertMaskToRoiAndAddToManager(boolean[][] binaryMask, ImagePlus imp, String roiName) {
        final int height = binaryMask.length;
        final int width = binaryMask[0].length;

        final ByteProcessor maskProcessor = new ByteProcessor(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (binaryMask[y][x]) {
                    maskProcessor.set(x, y, 255);
                }
            }
        }

        int seedX = -1;
        int seedY = -1;
        outerLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (maskProcessor.get(x, y) == 255) {
                    seedX = x;
                    seedY = y;
                    break outerLoop;
                }
            }
        }

        if (seedX == -1 || seedY == -1) {
            IJ.log("[ScratchRadiomics] The generated mask is empty. No ROI created");
            return null;
        }

        final Wand wand = new Wand(maskProcessor);
        wand.autoOutline(seedX, seedY, 255, 255, Wand.EIGHT_CONNECTED);

        if (wand.npoints == 0) {
            IJ.log("[ScratchRadiomics] Impossible to calculate ROI perimeter");
            return null;
        }

        final Polygon polygon = new Polygon(wand.xpoints, wand.ypoints, wand.npoints);
        final PolygonRoi roi = new PolygonRoi(polygon, Roi.POLYGON);
        roi.setName(roiName != null ? roiName : "Scratch_ROI");

        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new RoiManager();
        }

        roiManager.addRoi(roi);
        roiManager.select(roiManager.getCount() - 1);

        imp.setRoi(roi);

        return roi;
    }
}
