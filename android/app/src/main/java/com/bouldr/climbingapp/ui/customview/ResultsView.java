package com.bouldr.climbingapp.ui.customview;

import com.bouldr.climbingapp.ui.tflite.Classifier;

import java.util.List;

public interface ResultsView {
    public void setResults(final List<Classifier.Recognition> results);
}
