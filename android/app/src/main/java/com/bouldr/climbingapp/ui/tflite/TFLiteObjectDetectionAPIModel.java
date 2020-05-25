/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.bouldr.climbingapp.ui.tflite;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;

import com.bouldr.climbingapp.ui.labeller.FileProcessor;
import com.bouldr.climbingapp.ui.Firebase.FirebaseAPI;
import com.google.firebase.storage.StorageReference;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
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

// Wrapper for frozen detection models trained using the Tensorflow Object Detection API
public class TFLiteObjectDetectionAPIModel implements Classifier {
    // Max return of this many results
    private static final int NUM_DETECTIONS = 10;
    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    // Config values.
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    private ByteBuffer imgData;
    //TensorFlow is the interpreter
    private Interpreter tfLite;

    private TFLiteObjectDetectionAPIModel() { }

    //Memory-map the model that was downloaded from the cloud
    private static MappedByteBuffer loadModelFile(File file)
            throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannel = inputStream.getChannel();
        long declaredLength = file.length();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, declaredLength);
    }

    // Memory-map the model file in Assets
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Initializes a native TensorFlow session for classifying images.
    //
    // assetManager - The asset manager to be used to load assets.
    // modelFilename - The filepath of the model GraphDef protocol buffer.
    // labelFilename - The filepath of label file for classes.
    // inputSize - The size of image input
    // isQuantized - Boolean representing model is quantized or not
    public static Classifier create(final AssetManager assetManager, final String modelFilename,
                                    final String labelFilename, final int inputSize,
                                    final boolean isQuantized)
            throws IOException {
        final TFLiteObjectDetectionAPIModel d = new TFLiteObjectDetectionAPIModel();
        FirebaseAPI firebaseAPI = new FirebaseAPI();
        FileProcessor fileProcessor = new FileProcessor();
        int numBytesPerChannel;

        InputStream labelsInput;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(actualFilename);
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            d.labels.add(line);
        }
        br.close();

        d.inputSize = inputSize;
        firebaseAPI.getLatestCloudModel(new FirebaseAPI.FirebaseCallback() {
            @Override
            public void onFirebaseCallback(StorageReference value) {
            }
        });

        String localModel = fileProcessor.getLocalModel();

        try {
            //If local model does not have the value of 1 then it must be a model from the cloud
            //so run the loadModelFile for that model
            if (!localModel.equals("1")) {
                String dirPath = "/data/user/0/com.bouldr.climbingapp/files/fireBaseModels/" + localModel + ".tflite";
                File file = new File(dirPath);
                d.tfLite = new Interpreter(loadModelFile(file));
            } else {
                //If local model has the value of 1 then it is the packaged model so run localModelFile
                //with the asset files
                d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Checks if the model is quantized
        d.isModelQuantized = isQuantized;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        //Sets the image data from the input size
        d.imgData = ByteBuffer.allocateDirect(d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        //Sets the number of threads that the interpreter can use
        d.tfLite.setNumThreads(NUM_THREADS);
        //Sets the variables for the outputs of the interpreter
        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];
        return d;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        // Preprocess the image data from 0-255 int to normalized float based on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();
        //Loops through every pixel on the camera's input and allocated to imgData
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }

        // Copy the input data into TensorFlow.
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

        // Run the inference call.
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        // Show the best detections.
        // after scaling them back to the input size.
        final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * inputSize,
                            outputLocations[0][i][0] * inputSize,
                            outputLocations[0][i][3] * inputSize,
                            outputLocations[0][i][2] * inputSize);
            // SSD Mobilenet V1 Model assumes class 0 is background class
            // in label file and class labels start from 1 to number_of_classes+1,
            // while outputClasses correspond to class index from 0 to number_of_classes
            int labelOffset = 1;
            recognitions.add(
                    new Recognition(
                            "" + i,
                            labels.get((int) outputClasses[0][i] + labelOffset),
                            outputScores[0][i],
                            detection));
        }
        return recognitions;
    }

    //Number of threads being used can be changed by the user
    public void setNumThreads(int num_threads) {
        if (tfLite != null) tfLite.setNumThreads(num_threads);
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        if (tfLite != null) tfLite.setUseNNAPI(isChecked);
    }
}
