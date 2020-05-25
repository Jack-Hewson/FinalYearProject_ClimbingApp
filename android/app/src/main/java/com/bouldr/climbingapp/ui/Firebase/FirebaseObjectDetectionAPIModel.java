package com.bouldr.climbingapp.ui.Firebase;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Trace;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bouldr.climbingapp.ui.tflite.Classifier;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

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
import java.util.List;
import java.util.Vector;

// NOT USED BUT THIS IS THE FIREBASE INFERENCE.
// TFLiteObjectDetectionAPIModel is used instead
public class FirebaseObjectDetectionAPIModel implements Classifier {
    private static final int NUM_DETECTIONS = 10;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    private int inputSize;
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private ByteBuffer imgData;
    //  private Interpreter tflite;
    private FirebaseModelInterpreter firebaseInterpreter;
    private FirebaseModelInputOutputOptions inputOutputOptions;
    ArrayList<Recognition> recognitionsFB;
    private boolean isComplete = false;

    private FirebaseObjectDetectionAPIModel() {
    }

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static Classifier create(final AssetManager assetManager, final String modelFilename,
                                    final String labelFilename, final int inputSize,
                                    final boolean isQuantized, final Context context)
            throws IOException {

        final FirebaseObjectDetectionAPIModel d = new FirebaseObjectDetectionAPIModel();

        Toast.makeText(context, "Downloading model...", Toast.LENGTH_LONG).show();

        FirebaseCustomRemoteModel remoteModel = new FirebaseCustomRemoteModel.Builder("detectClimbing").build();
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        // Download complete. Depending on your app, you could enable
                        // the ML feature, or switch from the local model to the remote
                        // model, etc.
                        Toast.makeText(context, "Remote model successfully downloaded", Toast.LENGTH_LONG).show();
                        FirebaseModelInterpreterOptions options = new FirebaseModelInterpreterOptions.Builder(remoteModel).build();
                        try {
                            d.firebaseInterpreter = FirebaseModelInterpreter.getInstance(options);
                        } catch (FirebaseMLException e) {
                            e.printStackTrace();
                        }
                    }
                });

        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        InputStream labelsInput = assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            d.labels.add(line);
        }
        br.close();

        d.inputSize = inputSize;

        try {
            //        d.tflite = new Interpreter(loadModelFile(assetManager, modelFilename));
        } catch (Exception e) {
            //          throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;

        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1;
        } else {
            numBytesPerChannel = 4;
        }

        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        //    d.tflite.setNumThreads(NUM_THREADS);
        int[] outputLocations = new int[]{1, NUM_DETECTIONS, 4};
        int[] outputClasses = new int[]{1, NUM_DETECTIONS};
        int[] outputScores = new int[]{1, NUM_DETECTIONS};
        int[] numDetections = new int[]{1};
        int dataType = FirebaseModelDataType.FLOAT32;
        int[] inputValues = new int[]{1, inputSize, inputSize, 3};


        try {
            d.inputOutputOptions = new FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, dataType, inputValues)
                    .setOutputFormat(0, dataType, outputLocations)
                    .setOutputFormat(1, dataType, outputClasses)
                    .setOutputFormat(2, dataType, outputScores)
                    .setOutputFormat(3, dataType, numDetections)
                    .build();
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
        return d;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        isComplete = false;
        Trace.beginSection("recognizeImage");
        Trace.beginSection("preprocessBitmap");
        recognitionsFB = new ArrayList<>(NUM_DETECTIONS);
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    imgData.put((byte) ((pixelValue >> 16) & 0xff));
                    imgData.put((byte) ((pixelValue >> 8) & 0xff));
                    imgData.put((byte) (pixelValue & 0xff));
                } else {
                    imgData.putFloat((((pixelValue >> 16) & 0xff) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xff) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xff) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Trace.endSection();
        Trace.beginSection("feed");

        Object[] inputArray = {imgData};

        FirebaseModelInputs inputs = null;
        try {
            inputs = new FirebaseModelInputs.Builder()
                    .add(imgData)
                    .build();
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
        try {
            firebaseInterpreter.run(inputs, inputOutputOptions)
                    .addOnSuccessListener(
                            new OnSuccessListener<FirebaseModelOutputs>() {
                                @Override
                                public void onSuccess(FirebaseModelOutputs result) {
                                    float[][][] FB_Output = result.getOutput(0);
                                    float[][] FB_Classes = result.getOutput(1);
                                    float[][] FB_scores = result.getOutput(2);

                                    for (int i = 0; i < NUM_DETECTIONS; ++i) {
                                        final RectF detection = new RectF(
                                                FB_Output[0][i][1] * inputSize,
                                                FB_Output[0][i][0] * inputSize,
                                                FB_Output[0][i][3] * inputSize,
                                                FB_Output[0][i][2] * inputSize);
                                        int labelOffset = 1;
                                        recognitionsFB.add(new Recognition(
                                                "" + 1,
                                                labels.get((int) FB_Classes[0][i] + labelOffset),
                                                FB_scores[0][i],
                                                detection)
                                        );
                                    }
                                    isComplete = true;
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                    isComplete = true;
                                }
                            });
            Trace.endSection();
            while (isComplete == false) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
        }

        return recognitionsFB;
    }

    public void setNumThreads(int num_threads) {
        //  if (tflite != null) tflite.setNumThreads(num_threads);
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        //  if (tflite != null) tflite.setUseNNAPI(isChecked);
    }
}
