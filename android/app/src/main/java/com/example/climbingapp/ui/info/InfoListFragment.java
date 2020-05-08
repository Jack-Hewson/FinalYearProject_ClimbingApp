package com.example.climbingapp.ui.info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.climbingapp.R;
import com.example.climbingapp.ui.env.Logger;

public class InfoListFragment extends Fragment {
    private static final Logger LOGGER = new Logger();
    public View onCreateView(@NonNull LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_options, container, false);
    }
}
