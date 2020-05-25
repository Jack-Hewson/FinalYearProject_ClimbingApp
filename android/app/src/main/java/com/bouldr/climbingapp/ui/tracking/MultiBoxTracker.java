package com.bouldr.climbingapp.ui.tracking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.bouldr.climbingapp.R;
import com.bouldr.climbingapp.ui.env.BorderedText;
import com.bouldr.climbingapp.ui.env.ImageUtils;
import com.bouldr.climbingapp.ui.info.InfoFragment;
import com.bouldr.climbingapp.ui.tflite.Classifier.Recognition;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MultiBoxTracker {
    // Text size in dp
    private static final float TEXT_SIZE_DP = 18;

    //Minimum side size of box drawn
    private static final float MIN_SIZE = 16.0f;

    //Creates the colours the 7 boxes can be
    private static final int[] COLOURS = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
    };

    //LinkedList of current highlight squares
    final List<Pair<Float, RectF>> screenRects = new LinkedList<>();
    //LinkedList of current objects being tracked
    private final List<TrackedRecognition> trackedObjects = new LinkedList<>();
    //paint object for boxes
    private final Paint boxPaint = new Paint();
    //text that borders the box
    private final BorderedText borderedText;

    private Matrix frameToCanvasMatrix;
    //box frame's width
    private int frameWidth;
    //box frame's height
    private int frameHeight;
    //orientation of app
    private int sensorOrientation;
    //button overlay for highlight object's info button
    LinearLayout buttonOverlay;

    @SuppressLint("ClickableViewAccessibility")
    public MultiBoxTracker(final Context context) {
        //contains colours that are currently available
        for (final int colour : COLOURS) {
            Queue<Integer> availableColours = new LinkedList<>();
            availableColours.add(colour);
        }
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);
        boxPaint.setStrokeMiter(100);

        float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DP,
                context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
    }

    public synchronized void setFrameConfiguration(final int width, final int height,
                                                   final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        this.sensorOrientation = sensorOrientation;
    }

    public synchronized void trackResults(final List<Recognition> results, final long timestamp) {
        processResults(results);
    }

    private Matrix getFrameToCanvasMatrix() {
        return frameToCanvasMatrix;
    }

    //Draws the box
    @SuppressLint("ClickableViewAccessibility")
    public synchronized void draw(final Canvas canvas, Context context, View view) {
        buttonOverlay = view.findViewById(R.id.object_layout);
        final boolean rotated = sensorOrientation % 180 == 90;
        //adjustments made for canvas size and frame sizes
        final float multiplier = Math.min(
                canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));

        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
                frameWidth, frameHeight,
                (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                sensorOrientation,
                false);

        //Loops through every object being tracked and draws the box and creates the invisible button
        for (final TrackedRecognition recognition : trackedObjects) {
            final RectF trackedPos = new RectF(recognition.location);
            getFrameToCanvasMatrix().mapRect(trackedPos);
            boxPaint.setColor(recognition.colour);

            float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);

            final String labelString = !TextUtils.isEmpty(recognition.title)
                    ? String.format("%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
                    : String.format("%.2f", (100 * recognition.detectionConfidence));

            borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
                    labelString + "%", boxPaint);

            //invisible button created where the box is created, clicking button will open the fragment
            //displaying the information about the hold
            Button myButton = new Button(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.height = (int) trackedPos.height();
            lp.width = (int) trackedPos.height();
            lp.setMargins((int) trackedPos.left, (int) trackedPos.top, (int) trackedPos.right, (int) trackedPos.bottom);
            myButton.setLayoutParams(lp);
            myButton.getBackground().setAlpha(0);
            buttonOverlay.addView(myButton, lp);
            myButton.setOnClickListener(new View.OnClickListener() {
                @Override
                //Opens fragment with information about that particular hold
                public void onClick(View v) {
                    setFragment(context, recognition.title);
                }
            });
        }
    }

    //opens information fragment
    private void setFragment(Context context, String title) {
        Fragment fragment = new InfoFragment(title);
        FragmentTransaction transaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activity_camera, fragment);
        //Closing the info fragment will return the user to the camera
        transaction.addToBackStack("main");
        transaction.commit();
    }

    //Processes the results of every object detection, if any objects detected then draw is called
    private void processResults(final List<Recognition> results) {
        final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<>();
        //Clears the screen of current boxes
        screenRects.clear();
        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

        //Loops through all recognitions
        for (final Recognition result : results) {
            //Skips if current result has no location on the screnn
            if (result.getLocation() == null) {
                continue;
            }

            final RectF detectionFrameRect = new RectF(result.getLocation());
            final RectF detectionScreenRect = new RectF();
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);
            screenRects.add(new Pair<>(result.getConfidence(), detectionScreenRect));

            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                continue;
            }

            rectsToTrack.add(new Pair<>(result.getConfidence(), result));
        }

        trackedObjects.clear();
        if (rectsToTrack.isEmpty()) {
            return;
        }

        //Adds recognitions to static class TrackedRecognition
        for (final Pair<Float, Recognition> potential : rectsToTrack) {
            final TrackedRecognition trackedRecognition = new TrackedRecognition();
            trackedRecognition.detectionConfidence = potential.first;
            trackedRecognition.location = new RectF(potential.second.getLocation());
            trackedRecognition.title = potential.second.getTitle();
            trackedRecognition.colour = COLOURS[trackedObjects.size()];
            trackedObjects.add(trackedRecognition);

            if (trackedObjects.size() >= COLOURS.length) {
                break;
            }
        }
    }

    private static class TrackedRecognition {
        RectF location;
        float detectionConfidence;
        int colour;
        String title;
    }
}
