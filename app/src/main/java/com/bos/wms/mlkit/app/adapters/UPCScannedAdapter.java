package com.bos.wms.mlkit.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.ScanUPCsForBolActivity;

import java.util.ArrayList;

public class UPCScannedAdapter extends ArrayAdapter<UPCScannedItemDataModel> implements View.OnClickListener{

    private ArrayList<UPCScannedItemDataModel> dataSet;
    Context mContext;

    private ScanUPCsForBolActivity activity;

    private ListView listView;

    // View lookup cache
    private static class ViewHolder {
        TextView upcText;
        ImageView deleteImage;
    }



    public UPCScannedAdapter(ArrayList<UPCScannedItemDataModel> data, Context context, ListView listView, ScanUPCsForBolActivity activity) {
        super(context, R.layout.scan_upcs_item, data);
        this.dataSet = data;
        this.mContext=context;
        this.listView = listView;
        this.activity = activity;

    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        UPCScannedItemDataModel dataModel = (UPCScannedItemDataModel)object;

        switch (v.getId())
        {
            case R.id.upcDeleteImage:
                dataSet.remove(dataModel);
                this.notifyDataSetChanged();
                activity.CheckMinUPCForChanges();
                break;
        }
    }

    private int lastPosition = -1;

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        listView.smoothScrollToPosition(dataSet.size() + 2);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        UPCScannedItemDataModel dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.scan_upcs_item, parent, false);
            viewHolder.upcText = (TextView) convertView.findViewById(R.id.upcText);
            viewHolder.deleteImage = (ImageView) convertView.findViewById(R.id.upcDeleteImage);

            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.up_from_bottom);
        result.startAnimation(animation);
        lastPosition = position;

        viewHolder.upcText.setText(dataModel.getUPC());
        viewHolder.deleteImage.setOnClickListener(this);
        viewHolder.deleteImage.setTag(position);
        // Return the completed view to render on screen
        return convertView;
    }
}
