package com.hc.mixthebluetooth.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hc.mixthebluetooth.R;

import java.util.List;

public class ItemsAdapter extends  RecyclerView.Adapter<ItemsAdapter.ViewHolder> {

        // ... view holder defined above...

        // Store a member variable for the contacts
        private List<Item> mContacts;

        // Pass in the contact array into the constructor
        public ItemsAdapter(List<Item> contacts) {
            mContacts = contacts;
        }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the data model based on position
        Item contact = mContacts.get(position);

        // Set item views based on your views and data model
        TextView textView1 = holder.step1Text;
        textView1.setText(contact.getStep1());
        TextView textView2 = holder.step2Text;
        textView2.setText(contact.getStep2());
        TextView textView3 = holder.step3Text;
        textView3.setText(contact.getStep3());
        TextView textView4 = holder.step4Text;
        textView4.setText(contact.getStep4());
       // Button button = holder.messageButton;
      //  button.setText(contact.isOnline() ? "Message" : "Offline");
      //  button.setEnabled(contact.isOnline());
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView step1Text;
        public TextView step2Text;
        public TextView step3Text;
        public TextView step4Text;
        public Button messageButton;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            step1Text = (TextView) itemView.findViewById(R.id.step1);
            step2Text = (TextView) itemView.findViewById(R.id.step2);
            step3Text = (TextView) itemView.findViewById(R.id.step3);
            step4Text = (TextView) itemView.findViewById(R.id.step4);
         //   messageButton = (Button) itemView.findViewById(R.id.message_button);
        }
    }
}