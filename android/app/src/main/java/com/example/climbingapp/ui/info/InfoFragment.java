package com.example.climbingapp.ui.info;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.climbingapp.R;
import com.example.climbingapp.ui.env.Logger;

import java.io.IOException;
import java.io.InputStream;

public class InfoFragment extends Fragment {

    private static final Logger LOGGER = new Logger();
    TextView txtHoldTitle;
    TextView txtHoldDiff;
    TextView txtHoldText;
    ImageView imgHold;

    String holdName;

    public InfoFragment(String holdName) {
        this.holdName = holdName;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.hold_information, container, false);

        Button btnClose = view.findViewById(R.id.holdClose);
        txtHoldTitle = view.findViewById(R.id.holdTitle);
        txtHoldDiff = view.findViewById(R.id.holdDiff);
        txtHoldText = view.findViewById(R.id.holdText);
        imgHold = view.findViewById(R.id.holdImage);

        int titleInt = getContext().getResources().getIdentifier("holdTitle" + holdName, "string", getContext().getPackageName());
        int diffInt = getContext().getResources().getIdentifier("holdDifficulty" + holdName, "string", getContext().getPackageName());
        int textInt = getContext().getResources().getIdentifier("holdText" + holdName, "string", getContext().getPackageName());
        int imgInt = getContext().getResources().getIdentifier(holdName, "holdImages", getContext().getPackageName());

        Bitmap assetFile = getBitmapFromAssets(holdName);

        txtHoldTitle.setText(titleInt);
        txtHoldDiff.setText(diffInt);
        txtHoldText.setText(textInt);
        imgHold.setImageResource(R.drawable.sloper);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.i("Close pressed");
                FragmentManager fm = getActivity().getSupportFragmentManager();
                fm.popBackStack("main", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });

        return view;
    }

    public Bitmap getBitmapFromAssets(String imgName) {
        try {
            AssetManager assetManager = getContext().getAssets();
            InputStream istr = assetManager.open("/holdImages/" + imgName + ".jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(istr);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}