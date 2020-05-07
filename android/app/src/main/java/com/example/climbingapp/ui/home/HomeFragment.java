package com.example.climbingapp.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private FirebaseAPI firebaseAPI = new FirebaseAPI();
    private FileProcessor fileProcessor = new FileProcessor();
    private ProgressBar progressBar;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        String localModel = fileProcessor.getLocalModel();

        if (localModel == null) {
            localModel = "1";
        }

        String finalLocalModel = localModel;
        firebaseAPI.getLatestCloudModel(new FirebaseAPI.FirebaseCallback() {
            @Override
            public void onFirebaseCallback(StorageReference value) {
                String latestFilename = value.toString().split("/")[4];
                String modelVersion = latestFilename.split("\\.")[0];
                LOGGER.i("Comparing local and cloud model");
                LOGGER.i("Local model = " + finalLocalModel);
                LOGGER.i("cloud model = " + modelVersion);
                if (Integer.parseInt(finalLocalModel) < Integer.parseInt(modelVersion)) {
                    LOGGER.i("Cloud version is newer");
                    LinearLayout linearLayout = (LinearLayout) View.inflate(getContext(), R.layout.model_download, null);

                    ViewStub stub = view.findViewById(R.id.modelStub);
                    stub.inflate();
                    progressBar = view.findViewById(R.id.progressBar);

                    btnDownload = view.findViewById(R.id.downloadYes);

                    btnDownload.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            firebaseAPI.downloadModel(getContext(), progressBar);
                        }
                    });

                    TextView lblCurrent = view.findViewById(R.id.lblCurrentModel);
                    TextView lblCloud = view.findViewById(R.id.lblCloudModel);
                    lblCurrent.setText(finalLocalModel);
                    lblCloud.setText(modelVersion);
                } else {
                    LOGGER.i("Local version is the newest");
                }
            }
        });

        return view;
    }
}