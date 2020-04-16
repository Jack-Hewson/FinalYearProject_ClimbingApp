package com.example.climbingapp.ui.customview;

import com.example.climbingapp.ui.tflite.Classifier;

import java.util.List;

public interface ResultsView {
    public void setResults(final List<Classifier.Recognition> results);
}
