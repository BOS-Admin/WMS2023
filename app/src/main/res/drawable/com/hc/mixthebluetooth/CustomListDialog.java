package com.hc.mixthebluetooth;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.util.General1;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomListDialog extends DialogFragment {

    public CustomListDialog() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = view.findViewById(R.id.data_list);
        TextView txtTitle = view.findViewById(R.id.list_title);
        Collections.sort(General1.DialogData);
        ArrayAdapter<String> arrayAdapter
                = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,General1.DialogData );
        txtTitle.setText(General1.dialogTitle+":"+General1.DialogData.stream().count());
        listView.setAdapter(arrayAdapter);
    }
}