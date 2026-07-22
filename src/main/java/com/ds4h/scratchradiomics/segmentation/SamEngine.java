package com.ds4h.scratchradiomics.segmentation;
import ai.onnxruntime.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public SamEngine() throws OrtException, IOException {
        this.env = OrtEnvironment.getEnvironment();
        final File tempModelFile = loadModelFromResources();
        final OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        this.session = env.createSession(tempModelFile.getAbsolutePath(), options);
    }

    /**
     * Extracts a temporary file from JAR resources since ONNX Runtime requires a file path.
     */
    private File loadModelFromResources() throws IOException {
        final InputStream is = getClass().getResourceAsStream("/models/sam_vit_b.onnx");
        if (is == null) {
            throw new IOException("Model resource not found: " + "/models/sam_vit_b.onnx");
        }

        final File tempFile = File.createTempFile("sam_vit_b_", ".onnx");
        tempFile.deleteOnExit();

        try (final FileOutputStream fos = new FileOutputStream(tempFile)) {
            final byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    /**
     * Runs SAM inference given the image embeddings and user click point.
     *
     * @param imageEmbeddings Float array [1, 256, 64, 64]
     * @param pointX X coordinate of click in original image
     * @param pointY Y coordinate of click in original image
     * @param origWidth Original image width
     * @param origHeight Original image height
     * @return 2D boolean array representing the segmented binary mask
     */
    public boolean[][] predictMask(float[] imageEmbeddings, float pointX, float pointY, int origWidth, int origHeight) throws OrtException {
        final Map<String, OnnxTensor> inputs = new HashMap<>();

        final long[] embShape = new long[]{1, 256, 64, 64};
        inputs.put("image_embeddings", OnnxTensor.createTensor(env, FloatBuffer.wrap(imageEmbeddings), embShape));

        final float[] coordsData = new float[]{pointX, pointY};
        final long[] coordsShape = new long[]{1, 1, 2};
        inputs.put("point_coords", OnnxTensor.createTensor(env, FloatBuffer.wrap(coordsData), coordsShape));

        final float[] labelsData = new float[]{1.0f};
        final long[] labelsShape = new long[]{1, 1};
        inputs.put("point_labels", OnnxTensor.createTensor(env, FloatBuffer.wrap(labelsData), labelsShape));

        final float[] maskData = new float[256 * 256];
        final long[] maskShape = new long[]{1, 1, 256, 256};
        inputs.put("mask_input", OnnxTensor.createTensor(env, FloatBuffer.wrap(maskData), maskShape));

        final float[] hasMaskData = new float[]{0.0f};
        final long[] hasMaskShape = new long[]{1};
        inputs.put("has_mask_input", OnnxTensor.createTensor(env, FloatBuffer.wrap(hasMaskData), hasMaskShape));

        final float[] origSizeData = new float[]{(float) origHeight, (float) origWidth};
        final long[] origSizeShape = new long[]{2};
        inputs.put("orig_im_size", OnnxTensor.createTensor(env, FloatBuffer.wrap(origSizeData), origSizeShape));

        try (final OrtSession.Result result = session.run(inputs)) {
            if (result.get("masks").isPresent()) {
                final float[][][][] rawMasks = (float[][][][]) result.get("masks").get().getValue();

                final boolean[][] binaryMask = new boolean[origHeight][origWidth];
                for (int y = 0; y < origHeight; y++) {
                    for (int x = 0; x < origWidth; x++) {
                        binaryMask[y][x] = rawMasks[0][0][y][x] > 0.0f;
                    }
                }
                return binaryMask;
            } else {
                throw new OrtException("No masks found");
            }
        } finally {
            for (final OnnxTensor tensor : inputs.values()) {
                tensor.close();
            }
        }
    }

    @Override
    public void close() throws OrtException {
        if (session != null) session.close();
        if (env != null) env.close();
    }
}
