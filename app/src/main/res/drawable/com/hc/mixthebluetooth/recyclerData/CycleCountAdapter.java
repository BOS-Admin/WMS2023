package com.hc.mixthebluetooth.recyclerData;
import com.hc.mixthebluetooth.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import Model.CycleCountModelBin;

public class CycleCountAdapter extends ArrayAdapter<CycleCountModelBin> {

    public CycleCountAdapter(Context context, ArrayList<CycleCountModelBin> CycleCountModelBins) {
        super(context, 0, CycleCountModelBins);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        CycleCountModelBin Bin = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.cyclecount_listitem, parent, false);
        }
        // Lookup view for data population
        TextView txtBinCode = (TextView) convertView.findViewById(R.id.lblCycleCountListItemBinBarcode);
        TextView txtLocation1 = (TextView) convertView.findViewById(R.id.lblCycleCountListItemBinLocation1);
        TextView txtLocation2 = (TextView) convertView.findViewById(R.id.lblCycleCountListItemBinLocation2);
        TextView txtFailure = (TextView) convertView.findViewById(R.id.lblCycleCountItemFailure);
        RelativeLayout layoutBg= (RelativeLayout) convertView.findViewById(R.id.bgLytCycleCountListItem);
        // Populate the data into the template view using the data object
        txtBinCode.setText(Bin.getBinCode());
        txtLocation1.setText(Bin.getBinCode());
        txtLocation2.setText(Bin.getLocationID());
        int i=0;
        txtFailure.setText("");
        while (Bin.getNbFailures()>i){
            i++;
            txtFailure.append("X");
        }
        if(Bin.getIsDone()){
            layoutBg.setBackgroundColor(R.color.gray);
        }
        else if(Bin.getIsCurrent()){
            layoutBg.setBackgroundColor(R.color.lightBlue);
        }
        // Return the completed view to render on screen
        return convertView;
    }
}