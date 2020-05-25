package com.bouldr.climbingapp.ui.info;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bouldr.climbingapp.R;

//DialogFragment that displays the information for whatever hold is selected in either the object
//detector of the list of hold names
public class InfoFragment extends DialogFragment {
    private String holdName;

    public InfoFragment(String holdName) {
        this.holdName = holdName;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.hold_information, container, false);

        Button btnClose = view.findViewById(R.id.holdClose);
        TextView txtHoldTitle = view.findViewById(R.id.holdTitle);
        TextView txtHoldDiff = view.findViewById(R.id.holdDiff);
        TextView txtHoldText = view.findViewById(R.id.holdText);
        txtHoldDiff.setMovementMethod(new ScrollingMovementMethod());
        ImageView imgHold = view.findViewById(R.id.holdImage);

        int titleInt = getContext().getResources().getIdentifier("holdTitle" + holdName.replaceAll("\\s+", ""), "string", getContext().getPackageName());
        int diffInt = getContext().getResources().getIdentifier("holdDifficulty" + holdName.replaceAll("\\s+", ""), "string", getContext().getPackageName());
        int textInt = getContext().getResources().getIdentifier("holdText" + holdName.replaceAll("\\s+", ""), "string", getContext().getPackageName());
        int imgInt = getContext().getResources().getIdentifier((holdName.toLowerCase()).replaceAll("\\s+", ""), "drawable", getContext().getPackageName());

        txtHoldTitle.setText(titleInt);
        txtHoldDiff.setText(diffInt);
        txtHoldText.setText(textInt);
        imgHold.setImageResource(imgInt);

        //Closes the dialogFragment
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                fm.popBackStack("main", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });

        return view;
    }
}