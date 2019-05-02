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
    Bitmap scacledBitmap;

    public StylizeActivity(Context context) {
        tensorFlowInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(),MODEL_FILE);
    }


    public String styleTransfer(Context context, final Integer[] styles, String inputFilePath, final String outputDir, final int quality, float styleFactor, boolean convertToGrey) {
//        int desiredSize = 256;
        //tensorFlowInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(),MODEL_FILE);
        String outputFilePath = null;
        inputBitmap = BitmapFactory.decodeFile(inputFilePath);
        int inputWidth = inputBitmap.getWidth();
        int inputHeight = inputBitmap.getHeight();

        scacledBitmap =  Bitmap.createScaledBitmap(inputBitmap, inputWidth*quality/100, inputHeight*quality/100, false);

        Log.i("Availabel memory", String.valueOf(getAvailabelMemory()));

        // free up inputBitmap
       inputBitmap = null;

        int desiredWidth = 128*(int)Math.floor(scacledBitmap.getWidth()/128);
        int desiredHeight = 128*(int)Math.floor(scacledBitmap.getHeight()/128);

        int previewWidth = scacledBitmap.getWidth();
        int previewHeight = scacledBitmap.getHeight();

        int desiredSize = Math.max(desiredWidth, desiredHeight);

        // scale to square
        croppedBitmap = Bitmap.createScaledBitmap(scacledBitmap, desiredSize, desiredSize, false);

        // free up memory
        scacledBitmap = null;

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
            if(getAvailabelMemory()<croppedBitmap.getWidth()*croppedBitmap.getHeight()*4.5){
                Log.i("memory", String.valueOf(getAvailabelMemory()));
                throw new Exception("Out of memory");
            }


            intValues = new int[croppedBitmap.getWidth()*croppedBitmap.getHeight()];
            floatValues = new float[croppedBitmap.getWidth()*croppedBitmap.getHeight()*3];

            croppedBitmap.getPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());

            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3] = ((val >> 16) & 0xFF) / 255f;
                floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255f;
                floatValues[i * 3 + 2] = (val & 0xFF) / 255f;
            }

            Log.i("floatValues", floatValues.toString());


            tensorFlowInferenceInterface.feed(INPUT_NODE, floatValues, 1, croppedBitmap.getWidth(), croppedBitmap.getHeight(), 3);
            tensorFlowInferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);
            // Execute the output node's dependency sub-graph.
            tensorFlowInferenceInterface.run(new String[] {OUTPUT_NODE}, false);
            // Copy the data from TensorFlow back into our array
            Log.i("outputTensor",tensorFlowInferenceInterface.getStatString());

            tensorFlowInferenceInterface.fetch(OUTPUT_NODE, floatValues);

            for (int i = 0; i < intValues.length; ++i) {
                intValues[i] =
                        0xFF000000
                                | (((int) (floatValues[i * 3 ] * 255f)) << 16)
                                | (((int) (floatValues[i * 3  + 1] * 255f)) << 8)
                                | ((int) (floatValues[i * 3 + 2] * 255f));
            }


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
                croppedBitmap = null;
                intValues = null;
                floatValues = null;
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
        inputBitmap = null;
        scacledBitmap = null;
        croppedBitmap = null;
        intValues = null;
        floatValues = null;
    }
}
