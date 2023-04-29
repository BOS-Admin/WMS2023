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

/**
 * This class should be self explanatory
 */
public class TPOItemsDialogAdapter extends ArrayAdapter<TPOItemsDialogDataModel> implements View.OnClickListener{

    private ArrayList<TPOItemsDialogDataModel> dataSet;
    Context mContext;

    private ListView listView;

    // View lookup cache
    private static class ViewHolder {
        TextView locationText;
    }



    public TPOItemsDialogAdapter(ArrayList<TPOItemsDialogDataModel> data, Context context, ListView listView) {
        super(context, R.layout.tpo_location_item, data);
        this.dataSet = data;
        this.mContext=context;
        this.listView = listView;

    }

    /**
     * When the delete icon is pressed for each item, the item is removed from the dataModels list and the CheckMinUPCForChanges function in the main activity
     * is triggered
     * @param v
     */
    @Override
    public void onClick(View v) {

        /*int position=(Integer) v.getTag();
        Object object= getItem(position);
        UPCScannedItemDataModel dataModel = (UPCScannedItemDataModel)object;

        switch (v.getId())
        {
            case R.id.upcDeleteImage:
                dataSet.remove(dataModel);
                this.notifyDataSetChanged();
                activity.CheckMinUPCForChanges();
                break;
        }*/
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
        TPOItemsDialogDataModel dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.tpo_location_item, parent, false);
            viewHolder.locationText = (TextView) convertView.findViewById(R.id.locationText);

            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.up_from_bottom);
        result.startAnimation(animation);
        lastPosition = position;

        viewHolder.locationText.setText(dataModel.getLocation());
        // Return the completed view to render on screen
        return convertView;
    }
}
