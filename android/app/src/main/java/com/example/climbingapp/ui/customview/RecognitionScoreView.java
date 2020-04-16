package com.example.climbingapp.ui.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.example.climbingapp.ui.env.Logger;
import com.example.climbingapp.ui.tflite.Classifier.Recognition;

import java.util.List;

public class RecognitionScoreView extends View implements ResultsView{
    private static final Logger LOGGER = new Logger();
    private static final float TEXT_SIZE_DIP = 14;
    private final float textSizePx;
    private final Paint fgPaint;
    private final Paint bgPaint;
    private List<Recognition> results;

    public RecognitionScoreView(final Context context, final AttributeSet set) {
        super(context, set);

        textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        fgPaint = new Paint();
        fgPaint.setTextSize(textSizePx);

        bgPaint = new Paint();
        bgPaint.setColor(0xcc4285f4);
    }

    @Override
    public void setResults(final List<Recognition> results) {
        LOGGER.i("RUNNING HERE R = ");
        this.results = results;
        postInvalidate();
    }

    @Override
    public void onDraw(final Canvas canvas) {
        final int x = 10;
        int y = (int) (fgPaint.getTextSize() * 1.5f);
        LOGGER.i("RUNNING HERE  = ");
        canvas.drawPaint(bgPaint);

        if (results != null) {
            for (final Recognition recog : results) {
                LOGGER.i("RECOG = " + recog.getTitle());
                canvas.drawText(recog.getTitle() + ": " + recog.getConfidence, x, y, fgPaint);
                y += (int) (fgPaint.getTextSize() * 1.5f);
            }
        }
    }
}
