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
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;

import android.util.Log;

public class StylizeActivity {
    private static final String MODEL_FILE = "stylize_quantized.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;
    private int[] intValues;
    private FloatBuffer floatValues;

    TensorFlowInferenceInterface tensorFlowInferenceInterface;
    Bitmap inputBitmap;
    Bitmap croppedBitmap;
    Float MEAN = 0.0f;
    Float STD = 100f;
//    Bitmap scacledBitmap;

    public StylizeActivity(Context context) {
        tensorFlowInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(),MODEL_FILE);
    }

    public String styleTransfer(Context context, final Integer[] styles, String inputFilePath, final String outputDir, final int quality, float styleFactor, boolean convertToGrey) {
        String outputFilePath = null;
        inputBitmap = BitmapFactory.decodeFile(inputFilePath);
        int inputWidth = inputBitmap.getWidth();
        int inputHeight = inputBitmap.getHeight();
        croppedBitmap =  Bitmap.createScaledBitmap(inputBitmap, inputWidth*quality/100, inputHeight*quality/100, false);

        int desiredWidth = 128*(int)Math.floor(croppedBitmap.getWidth()/128);
        int desiredHeight = 128*(int)Math.floor(croppedBitmap.getHeight()/128);

        int previewWidth = croppedBitmap.getWidth();
        int previewHeight = croppedBitmap.getHeight();
        int desiredSize = Math.min(desiredWidth, desiredHeight);
        // scale to square
        croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, desiredSize, desiredSize, false);
        Log.i("Availabel memory 5", String.valueOf(getAvailabelMemory()));

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

        try{

            if(getAvailabelMemory()<croppedBitmap.getWidth()*croppedBitmap.getHeight()*5){
                Log.i("memory", String.valueOf(getAvailabelMemory()));
                throw new Exception("Out of memory");
            }
            
            floatValues = FloatBuffer.allocate(1*croppedBitmap.getWidth()*croppedBitmap.getHeight()*3);
            intValues = new int[1*croppedBitmap.getWidth()*croppedBitmap.getHeight()];
            croppedBitmap.getPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());
            for (int i = 0; i < intValues.length; ++i) {
                int pixelValue = intValues[i];
                floatValues.put((((pixelValue >> 16) & 0xFF) )/ STD);
                floatValues.put(((pixelValue >> 8) & 0xFF) / STD);
                floatValues.put((pixelValue & 0xFF) / STD);
            }

            tensorFlowInferenceInterface.feed(INPUT_NODE, floatValues.array(), 1, croppedBitmap.getWidth(), croppedBitmap.getHeight(), 3);
            tensorFlowInferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);
            // Execute the output node's dependency sub-graph.

            tensorFlowInferenceInterface.run(new String[] {OUTPUT_NODE}, false);

            // Copy the data from TensorFlow back into our array
            floatValues.clear();
            float[] outputFloats = new float[1*croppedBitmap.getWidth()*croppedBitmap.getHeight()*3];
            tensorFlowInferenceInterface.fetch(OUTPUT_NODE, outputFloats);

            for (int i = 0; i < croppedBitmap.getWidth()*croppedBitmap.getHeight(); ++i) {
                intValues[i] =
                        0xFF000000
                                | (((int) (outputFloats[i * 3] * STD)) << 16)
                                | (((int) (outputFloats[i * 3  + 1] * STD)) << 8)
                                | ((int) (outputFloats[i * 3 + 2] * STD));
            }

            floatValues.clear();
            croppedBitmap.setPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());

            if(convertToGrey==true) {
                Canvas canvas = new Canvas(croppedBitmap);
                Paint paint = new Paint();
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
                paint.setColorFilter(f);
                canvas.drawBitmap(croppedBitmap, 0, 0, paint);
            }

            String outputFileName = String.valueOf(System.currentTimeMillis()) + ".jpeg";
            File outputFile = new File(outputDir +"/" + outputFileName);
            if (outputFile.exists()) outputFile.delete();

            // scacle back
            croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, previewWidth, previewHeight, false);

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
                floatValues.clear();
                intValues = null;
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