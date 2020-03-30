package com.example.climbingapp.ui.tflite;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Trace;

import com.example.climbingapp.ui.env.Logger;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TFLiteObjectDetectionAPIModel implements Classifier {
    private static final Logger LOGGER = new Logger();
    private static final int NUM_DETECTIONS = 10;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    private int inputSize;
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;
    private ByteBuffer imgData;
    private Interpreter tflite;
    private TFLiteObjectDetectionAPIModel() {}

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
        throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel= inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset,declaredLength);
    }

    public static Classifier create(final AssetManager assetManager, final String modelFilename,
                                    final String labelFilename, final int inputSize,
                                    final boolean isQuantized)
        throws IOException {
        final TFLiteObjectDetectionAPIModel d = new TFLiteObjectDetectionAPIModel();

        InputStream labelsInput = null;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(actualFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            LOGGER.w(line);
            d.labels.add(line);
        }
        br.close();

        d.inputSize = inputSize;

        try {
            d.tflite = new Interpreter(loadModelFile(assetManager, modelFilename));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;

        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1;
        }
        else {
            numBytesPerChannel = 4;
        }
        d.imgData = ByteBuffer.allocateDirect(1* d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        d.tflite.setNumThreads(NUM_THREADS);
        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];
        return d;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        Trace.beginSection("recognizeImage");
        Trace.beginSection("preprocessBitmap");
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j <inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    imgData.put((byte) ((pixelValue >> 16) & 0xff));
                    imgData.put((byte) ((pixelValue >> 8) & 0xff));
                    imgData.put((byte) (pixelValue & 0xff));
                }
                else {
                    imgData.putFloat((((pixelValue >> 16) & 0xff) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xff) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xff) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Trace.endSection();

        Trace.beginSection("feed");

        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);
        Trace.endSection();

        Trace.beginSection("run");
        tflite.runForMultipleInputsOutputs(inputArray, outputMap);
        Trace.endSection();

        final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            final RectF detection = new RectF(
                    outputLocations[0][i][1] * inputSize,
                    outputLocations[0][i][0] * inputSize,
                    outputLocations[0][i][3] * inputSize,
                    outputLocations[0][i][2] * inputSize);
            int labelOffset = 1;
            recognitions.add(new Recognition(
                    "" + 1,
                    labels.get((int) outputClasses[0][i] + labelOffset),
                    outputScores[0][i],
                    detection)
            );
        }
        Trace.endSection();
        return recognitions;
    }

    @Override
    public void enableStatLogging(final boolean logStats) {}

    @Override
    public String getStatString() {
        return "";
    }

    @Override
    public void close() {}

    public void setNumThreads(int num_threads){
        if (tflite != null) tflite.setNumThreads(num_threads);
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        if (tflite != null) tflite.setUseNNAPI(isChecked);
    }
}
