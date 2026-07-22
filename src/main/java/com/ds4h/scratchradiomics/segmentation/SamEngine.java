package com.ds4h.scratchradiomics.segmentation;
import ai.onnxruntime.*;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Point;
import java.awt.Rectangle;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage the initialization of ONNX Runtime engine and run
 * the inferences of SAM model on ImageJ images.
 */
public class SamEngine implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;

    private static final int TARGET_SIZE = 1024;
    private static final float[] MEAN = {123.675f, 116.28f, 103.53f};
    private static final float[] STD = {58.395f, 57.12f, 57.375f};

    /**
     * Initialize the ONNX session by upload the model from the file.
     *
     * @param modelPath The .onnx file path with the SAM model
     * @throws OrtException If the file loading fails
     */
    public SamEngine(String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        final OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
        this.session = env.createSession(modelPath, options);
    }

    /**
     * Run the segmentation of a scratch wound assay by using SAM in auto/point mode.
     *
     * @param imp ImageJ source image
     * @param pointPrompt Optional point insert by the user (could be null)
     * @param boxPrompt Optional scratch bounding box (could be null)
     * @return ByteProcessor with the binarized mask (0 or 255) in the original dimensions
     * @throws OrtException If ONNX inference fails
     */
    public ByteProcessor segmentScratch(ImagePlus imp, Point pointPrompt, Rectangle boxPrompt) throws OrtException {
        final int origWidth = imp.getWidth();
        final int origHeight = imp.getHeight();

        final float[] inputData = preprocessImage(imp.getProcessor());
        final long[] shape = new long[]{1, 3, TARGET_SIZE, TARGET_SIZE};

        final OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape);

        final Map<String, OnnxTensor> container = new HashMap<>();
        final String inputName = session.getInputNames().iterator().next();
        container.put(inputName, inputTensor);

        try (final OrtSession.Result results = session.run(container)) {
            final OnnxValue outputValue = results.get(0);
            final float[][][][] maskArray = (float[][][][]) outputValue.getValue();

            return postprocessMask(maskArray, origWidth, origHeight);
        } finally {
            inputTensor.close();
        }
    }

    /**
     * Converts a ImageProcessor in a normalized NCHW buffer.
     */
    private float[] preprocessImage(ImageProcessor ip) {
        final ImageProcessor resized = ip.resize(TARGET_SIZE, TARGET_SIZE, true);
        final ImageProcessor rgb = resized.convertToRGB();

        final int pixelCount = TARGET_SIZE * TARGET_SIZE;
        final float[] tensorData = new float[3 * pixelCount];

        final int[] pixels = (int[]) rgb.getPixels();

        for (int i = 0; i < pixelCount; i++) {
            final int c = pixels[i];
            final float r = (c >> 16) & 0xff;
            final float g = (c >> 8) & 0xff;
            final float b = c & 0xff;

            tensorData[i] = (r - MEAN[0]) / STD[0]; // Red Channel
            tensorData[pixelCount + i] = (g - MEAN[1]) / STD[1]; // Green Channel
            tensorData[2 * pixelCount + i] = (b - MEAN[2]) / STD[2]; // Blue Channel
        }

        return tensorData;
    }

    /**
     * Resize and binarize the tensor output mask.
     */
    private ByteProcessor postprocessMask(float[][][][] maskArray, int origWidth, int origHeight) {
        final ByteProcessor mask1024 = new ByteProcessor(TARGET_SIZE, TARGET_SIZE);
        final byte[] maskPixels = (byte[]) mask1024.getPixels();

        for (int y = 0; y < TARGET_SIZE; y++) {
            for (int x = 0; x < TARGET_SIZE; x++) {
                final float val = maskArray[0][0][y][x];
                maskPixels[y * TARGET_SIZE + x] = (byte) (val > 0.0f ? 255 : 0);
            }
        }

        final ImageProcessor finalMask = mask1024.resize(origWidth, origHeight, false);
        return finalMask.convertToByteProcessor();
    }

    @Override
    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (OrtException e) {
            IJ.error("Error while closing OrtEnvironment", e.getMessage());
        }
    }
}
