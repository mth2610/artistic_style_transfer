package com.mth2610.artistic_style_transfer;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import android.media.ExifInterface;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileOutputStream;
import android.util.Log;

public class StylizeActivity {
    private static final String MODEL_FILE = "stylize_quantized.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;
    private int[] intValues;
    private float[] floatValues;
    TensorFlowInferenceInterface tensorFlowInferenceInterface;
    Bitmap inputBitmap;
    Bitmap croppedBitmap;
//    Bitmap scacledBitmap;

    public StylizeActivity(Context context) {
        tensorFlowInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(),MODEL_FILE);
    }

    public String styleTransfer(Context context, final Integer[] styles, String inputFilePath, final String outputDir, final int quality, float styleFactor, boolean convertToGrey) {
//        int desiredSize = 256;
        //tensorFlowInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(),MODEL_FILE);
        Log.i("Availabel memory 1", String.valueOf(getAvailabelMemory()));
        String outputFilePath = null;
        inputBitmap = BitmapFactory.decodeFile(inputFilePath);
        int inputWidth = inputBitmap.getWidth();
        int inputHeight = inputBitmap.getHeight();
        Log.i("Availabel memory 2", String.valueOf(getAvailabelMemory()));
        croppedBitmap =  Bitmap.createScaledBitmap(inputBitmap, inputWidth*quality/100, inputHeight*quality/100, false);
        Log.i("Availabel memory 3", String.valueOf(getAvailabelMemory()));

        Log.i("Availabel memory 4", String.valueOf(getAvailabelMemory()));

        int desiredWidth = 128*(int)Math.floor(croppedBitmap.getWidth()/128);
        int desiredHeight = 128*(int)Math.floor(croppedBitmap.getHeight()/128);

        int previewWidth = croppedBitmap.getWidth();
        int previewHeight = croppedBitmap.getHeight();
        int desiredSize = Math.min(desiredWidth, desiredHeight);
        // scale to square
        croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, desiredSize, desiredSize, false);
        Log.i("Availabel memory 5", String.valueOf(getAvailabelMemory()));

        // free up memory
//        croppedBitmap = croppedBitmap;

        Log.i("Availabel memory 6", String.valueOf(getAvailabelMemory()));

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
            styleVals[styles[i]] = (float) 1.0*styleFactor/numStyles;
        }

        System.gc();

        try{
            Log.i("Availabel memory 7", String.valueOf(getAvailabelMemory()));
            if(getAvailabelMemory()<croppedBitmap.getWidth()*croppedBitmap.getHeight()*5){
                Log.i("memory", String.valueOf(getAvailabelMemory()));
                throw new Exception("Out of memory");
            }
            Log.i("Availabel memory 8", String.valueOf(getAvailabelMemory()));

            // force garbage collector to run
            intValues = new int[croppedBitmap.getWidth()*croppedBitmap.getHeight()];
            floatValues = new float[croppedBitmap.getWidth()*croppedBitmap.getHeight()*3];

            croppedBitmap.getPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());

            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3] = ((val >> 16) & 0xFF) / 255f;
                floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255f;
                floatValues[i * 3 + 2] = (val & 0xFF) / 255f;
            }

            Log.i("Availabel memory 9", String.valueOf(getAvailabelMemory()));

            tensorFlowInferenceInterface.feed(INPUT_NODE, floatValues, 1, croppedBitmap.getWidth(), croppedBitmap.getHeight(), 3);
            tensorFlowInferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);
            // Execute the output node's dependency sub-graph.

            Log.i("Availabel memory 10 ***", String.valueOf(getAvailabelMemory()));
            tensorFlowInferenceInterface.run(new String[] {OUTPUT_NODE}, false);
            // Copy the data from TensorFlow back into our array
            tensorFlowInferenceInterface.fetch(OUTPUT_NODE, floatValues);
            Log.i("Availabel memory 11", String.valueOf(getAvailabelMemory()));

            for (int i = 0; i < intValues.length; ++i) {
                intValues[i] =
                        0xFF000000
                                | (((int) (floatValues[i * 3 ] * 255f)) << 16)
                                | (((int) (floatValues[i * 3  + 1] * 255f)) << 8)
                                | ((int) (floatValues[i * 3 + 2] * 255f));
            }

            floatValues = null;

            Log.i("Availabel memory 12", String.valueOf(getAvailabelMemory()));

            croppedBitmap.setPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());

            String outputFileName = String.valueOf(System.currentTimeMillis()) + ".jpeg";
            File outputFile = new File(outputDir +"/" + outputFileName);
            if (outputFile.exists()) outputFile.delete();

            // scacle back
            croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, previewWidth, previewHeight, false);

            if(convertToGrey==true) {
                Canvas canvas = new Canvas(croppedBitmap);
                Paint paint = new Paint();
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
                paint.setColorFilter(f);
                canvas.drawBitmap(croppedBitmap, 0, 0, paint);
            }

            try {
                FileOutputStream out = new FileOutputStream(outputFile);
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                ExifInterface inputExif = new ExifInterface(inputFilePath);
                ExifInterface outputExif = new ExifInterface(outputFile.getAbsolutePath());
                Log.i("inputOri", String.valueOf(inputExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)));
                outputExif.setAttribute(ExifInterface.TAG_ORIENTATION, inputExif.getAttribute(ExifInterface.TAG_ORIENTATION));
                outputExif.saveAttributes();
                outputFilePath = outputFile.getAbsolutePath();

                // free up memory
                croppedBitmap.recycle();
                croppedBitmap = null;
                inputBitmap.recycle();
                inputBitmap = null;
                intValues = null;
                floatValues = null;

                // force garbage collector to run
                System.gc();
            } catch (Exception e) {
                e.printStackTrace();
                // free up memory
                freeUpMemory();
            }

        } catch (Exception e) {
            e.printStackTrace();
            freeUpMemory();
        }
        return outputFilePath;
    }

    long getAvailabelMemory(){
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemInByte=(runtime.totalMemory() - runtime.freeMemory());
        final long maxHeapSizeInByte=runtime.maxMemory();
        final long availHeapSizeInByte = maxHeapSizeInByte - usedMemInByte;
        return availHeapSizeInByte;
    }

    void freeUpMemory(){
        inputBitmap.recycle();
        inputBitmap = null;
        croppedBitmap.recycle();
        croppedBitmap = null;
        intValues = null;
        floatValues = null;
    }
}


//    int numRowSegments = desiredWidth/128;
//    int numColSegments = desiredHeight/128;
//
//        for(int x = 0; x < numRowSegments; ++x) {
//        for (int y = 0; y < numColSegments; ++y) {
//        // 128*128 = 16384
//        intValues = new int[16384];
//        // 128*128*3 = 49152
//        floatValues = new float[49152];
//        croppedBitmap.getPixels(intValues, 0, 128,128*x, 128*y, 128, 128);
//        for (int i = 0; i < intValues.length; ++i) {
//final int val = intValues[i];
//        floatValues[i * 3] = ((val >> 16) & 0xFF) / 255f;
//        floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255f;
//        floatValues[i * 3 + 2] = (val & 0xFF) / 255f;
//        }
//
//        tensorFlowInferenceInterface.feed(INPUT_NODE, floatValues, 1, 128, 128, 3);
//        tensorFlowInferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);
//        tensorFlowInferenceInterface.run(new String[] {OUTPUT_NODE}, false);
//        tensorFlowInferenceInterface.fetch(OUTPUT_NODE, floatValues);
//
//        for (int i = 0; i < intValues.length; ++i) {
//        intValues[i] =
//        0xFF000000
//        | (((int) (floatValues[i * 3 ] * 255f)) << 16)
//        | (((int) (floatValues[i * 3  + 1] * 255f)) << 8)
//        | ((int) (floatValues[i * 3 + 2] * 255f));
//        }
//        croppedBitmap.setPixels(intValues, 0, 128,128*x, 128*y, 128, 128);
//        }
//        }
//
//        String outputFileName = String.valueOf(System.currentTimeMillis()) + ".jpeg";
//        File outputFile = new File(outputDir +"/" + outputFileName);
//        if (outputFile.exists()) outputFile.delete();
//
//        // scacle back
//        croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, previewWidth, previewHeight, false);
//
//        if(convertToGrey==true) {
//        Canvas canvas = new Canvas(croppedBitmap);
//        Paint paint = new Paint();
//        ColorMatrix cm = new ColorMatrix();
//        cm.setSaturation(0);
//        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
//        paint.setColorFilter(f);
//        canvas.drawBitmap(croppedBitmap, 0, 0, paint);
//        }
//
//        try {
//        FileOutputStream out = new FileOutputStream(outputFile);
//        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//        out.flush();
//        out.close();
//        ExifInterface inputExif = new ExifInterface(inputFilePath);
//        ExifInterface outputExif = new ExifInterface(outputFile.getAbsolutePath());
//        Log.i("inputOri", String.valueOf(inputExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)));
//        outputExif.setAttribute(ExifInterface.TAG_ORIENTATION, inputExif.getAttribute(ExifInterface.TAG_ORIENTATION));
//        outputExif.saveAttributes();
//        outputFilePath = outputFile.getAbsolutePath();
//
//        // free up memory
//        croppedBitmap = null;
//        intValues = null;
//        floatValues = null;
//        } catch (Exception e) {
//        e.printStackTrace();
//        // free up memory
//        freeUpMemory();
//        }
