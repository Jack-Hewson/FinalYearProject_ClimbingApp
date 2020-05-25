package com.bouldr.climbingapp.ui.labeller;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bouldr.climbingapp.R;

//DialogFragment for the list of hold names that can be used for labelling
@SuppressLint("ValidFragment")
public class holdNameDialogFragment extends DialogFragment {
    ListView listView;
    String[] listItem;
    String holdValue = null;
    ImageObject.Holds hold;

    @SuppressLint("ValidFragment")
    public holdNameDialogFragment(ImageObject.Holds hold){
        this.hold = hold;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.holdname_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = getView().findViewById(R.id.holdList);
        listItem = getResources().getStringArray(R.array.hold_list);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, listItem);
        //Loads the list of hold names and displays it
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //Selecting a hold from the list will set that as the hold's label name
                holdValue = adapter.getItem(position);
                Toast.makeText(getActivity().getApplicationContext(), holdValue, Toast.LENGTH_SHORT).show();
            }
        });

        Button btnDone = view.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Selecting "done" will close the DialogFragment but only if the user has selected
                //a hold from the list
                if(holdValue != null) {
                    hold.setHoldName(holdValue);
                    dismiss();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
