package com.example.climbingapp.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.climbingapp.FileProcessor;
import com.example.climbingapp.R;
import com.example.climbingapp.ui.Firebase.FirebaseAPI;
import com.example.climbingapp.ui.env.Logger;
import com.google.firebase.storage.StorageReference;

@SuppressLint("ValidFragment")
public class HomeFragment extends Fragment {
    private static final Logger LOGGER = new Logger();
    private Button btnDownload;
    FirebaseAPI firebaseAPI = new FirebaseAPI();
    FileProcessor fileProcessor = new FileProcessor();

    public View onCreateView(@NonNull LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnDownload = view.findViewById(R.id.downloadYes);

        String localModel = fileProcessor.getLocalModel();

        firebaseAPI.getLatestCloudModel(new FirebaseAPI.FirebaseCallback() {
            @Override
            public void onFirebaseCallback(StorageReference value) {
                String latestFilename = value.toString().split("/")[4];
                String modelVersion = latestFilename.split("\\.")[0];
                LOGGER.i("Comparing local and cloud model");
                LOGGER.i("Local model = " + localModel);
                LOGGER.i("cloud model = " + modelVersion);
                if (Integer.parseInt(localModel) < Integer.parseInt(modelVersion)){
                    LOGGER.i("Cloud version is newer");
                } else {
                    LOGGER.i("Local version is the newest");
                }
            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAPI.downloadModel(getContext());
            }
        });


        return view;
    }
}