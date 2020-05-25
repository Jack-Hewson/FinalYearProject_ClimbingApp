package com.bouldr.climbingapp.ui.tflite;

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

//Classifier contains the list of recognitions from the interpreter
public interface Classifier {
    List<Recognition> recognizeImage(Bitmap bitmap);
    void setNumThreads(int num_threads);

    void setUseNNAPI(boolean isChecked);

    class Recognition {
        private final String id;
        private final String title;
        private final Float confidence;
        public String getConfidence;
        private RectF location;

        public Recognition(final String id, final String title, final Float confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }
}
