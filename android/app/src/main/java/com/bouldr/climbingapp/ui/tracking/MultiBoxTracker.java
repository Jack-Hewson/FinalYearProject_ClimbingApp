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
    private static final float TEXT_SIZE_DP = 18;
    private static final float MIN_SIZE = 16.0f;
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

    final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
    private final Queue<Integer> availableColours = new LinkedList<Integer>();
    private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
    private final Paint boxPaint = new Paint();
    private final float textSizePx;
    private final BorderedText borderedText;
    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private int frameHeight;
    private int sensorOrientation;
    LinearLayout buttonOverlay;

    @SuppressLint("ClickableViewAccessibility")
    public MultiBoxTracker(final Context context) {
        for (final int colour : COLOURS) {
            availableColours.add(colour);
        }
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);
        boxPaint.setStrokeMiter(100);

        textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DP,
                context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
    }

    public synchronized void setFrameConfiguration(final int width, final int height,
                                                   final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        this.sensorOrientation = sensorOrientation;
    }
/**
    public synchronized void drawDebug(final Canvas canvas) {
        final Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60.0f);

        final Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);

        for (final Pair<Float, RectF> detection : screenRects) {
            final RectF rect = detection.second;
            canvas.drawRect(rect, boxPaint);
            canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
            borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
        }
    }
*/
    public synchronized void trackResults(final List<Recognition> results, final long timestamp) {
        processResults(results);
    }

    private Matrix getFrameToCanvasMatrix() {
        return frameToCanvasMatrix;
    }

    @SuppressLint("ClickableViewAccessibility")
    public synchronized void draw(final Canvas canvas, Context context, View view) {
        buttonOverlay = view.findViewById(R.id.object_layout);
        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier = Math.min(
                canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
                frameWidth, frameHeight,
                (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                sensorOrientation,
                false);
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


            Button myButton = new Button(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.height = (int) trackedPos.height();
            lp.width = (int) trackedPos.height();
            lp.setMargins((int) trackedPos.left, (int) trackedPos.top, (int) trackedPos.right, (int) trackedPos.bottom);
            myButton.setLayoutParams(lp);
            //myButton.setBackgroundColor(Color.parseColor("#1A000000"));
            myButton.getBackground().setAlpha(0);
            buttonOverlay.addView(myButton, lp);
            myButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setFragment(context, recognition.title);
                }
            });
        }
    }

    private void setFragment(Context context, String title){
        Fragment fragment = new InfoFragment(title);
        FragmentTransaction transaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activity_camera, fragment);
        transaction.addToBackStack("main");
        transaction.commit();
    }

    private void processResults(final List<Recognition> results) {
        final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();

        screenRects.clear();
        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

        for (final Recognition result : results) {
            if (result.getLocation() == null) {
                continue;
            }
            final RectF detectionFrameRect = new RectF(result.getLocation());

            final RectF detectionScreenRect = new RectF();
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

            screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                continue;
            }

            rectsToTrack.add(new Pair<Float, Recognition>(result.getConfidence(), result));
        }

        trackedObjects.clear();
        if (rectsToTrack.isEmpty()) {
            return;
        }

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
