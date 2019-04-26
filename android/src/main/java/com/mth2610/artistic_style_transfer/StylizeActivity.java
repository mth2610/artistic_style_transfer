package com.mth2610.artistic_style_transfer;

import android.content.Context;
import android.content.res.AssetManager;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;

import java.util.ArrayList;

import java.io.File;
import java.io.FileOutputStream;
import android.util.Log;

public class StylizeActivity {
    private static final String MODEL_FILE = "stylize_quantized.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;
//    private final float[] styleVals = new float[NUM_STYLES];
    private int[] intValues;
    private float[] floatValues;
    TensorFlowInferenceInterface tensorFlowInferenceInterface;
    Bitmap inputBitmap;
    Bitmap croppedBitmap;
    Matrix cropToFrameTransform;

    public StylizeActivity(Context context) {
        tensorFlowInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(),MODEL_FILE);
    }

    public String styleTransfer(Context context, final Integer[] styles, final String inutFilePath, final String outputDir) {
//        int desiredSize = 256;

        //tensorFlowInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(),MODEL_FILE);
        Bitmap inputBitmap = BitmapFactory.decodeFile(inutFilePath);
        int previewWidth = inputBitmap.getWidth();
        int previewHeight = inputBitmap.getHeight();
//        int desiredWidth = previewWidth;
//        int desiredHeight = previewWidth;
        int desiredWidth = 256;
        int desiredHeight = 256;
        Bitmap croppedBitmap = Bitmap.createBitmap(desiredWidth, desiredHeight, Config.ARGB_8888);
//        copyBitMap.setConfig(Config.ARGB_8888);
        Matrix frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        desiredWidth, desiredHeight,
                        0, true);

        cropToFrameTransform = new Matrix();
        final Canvas canvas = new Canvas(croppedBitmap);
        frameToCropTransform.invert(cropToFrameTransform);
        canvas.drawBitmap(inputBitmap, frameToCropTransform, null);

        float[] styleVals = {
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f
        };

        int numStyles = styles.length;

        for(int i=0; i < numStyles; i++){
            styleVals[styles[i]] = (float) 1.0/numStyles;
        }

        intValues = new int[desiredWidth*desiredHeight];
        floatValues = new float[desiredWidth*desiredHeight*3];
        croppedBitmap.getPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        Log.i("floatValues", floatValues.toString());
//        Log.i("inferenceInterface", inferenceInterface.toString());

        tensorFlowInferenceInterface.feed(INPUT_NODE, floatValues, 1, croppedBitmap.getWidth(), croppedBitmap.getHeight(), 3);
        tensorFlowInferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);

        // Execute the output node's dependency sub-graph.
        tensorFlowInferenceInterface.run(new String[] {OUTPUT_NODE}, false);
        // Copy the data from TensorFlow back into our array.
        tensorFlowInferenceInterface.fetch(OUTPUT_NODE, floatValues);

        for (int i = 0; i < intValues.length; ++i) {
            intValues[i] =
                    0xFF000000
                            | (((int) (floatValues[i * 3] * 255)) << 16)
                            | (((int) (floatValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (floatValues[i * 3 + 2] * 255));
        }
        croppedBitmap.setPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());

        String outputFileName = String.valueOf(System.currentTimeMillis()) + ".png";
        File outputFile = new File(outputDir +"/" + outputFileName);

        if (outputFile.exists()) outputFile.delete();
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outputFile.getAbsolutePath();
    }
}
