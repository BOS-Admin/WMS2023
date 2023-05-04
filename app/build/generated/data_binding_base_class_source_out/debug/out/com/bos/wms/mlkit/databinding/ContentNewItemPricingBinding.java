// Generated by view binder compiler. Do not edit!
package com.bos.wms.mlkit.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.bos.wms.mlkit.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ContentNewItemPricingBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final TextView UPCPricingListView;

  @NonNull
  public final Button btnUPCPricingDone;

  @NonNull
  public final TextView lblError;

  @NonNull
  public final TextView lblItemSerial;

  @NonNull
  public final LinearLayout linearLayout;

  @NonNull
  public final RecyclerView recyclerView;

  @NonNull
  public final TextView textView;

  @NonNull
  public final EditText txtItemSerial;

  private ContentNewItemPricingBinding(@NonNull ConstraintLayout rootView,
      @NonNull TextView UPCPricingListView, @NonNull Button btnUPCPricingDone,
      @NonNull TextView lblError, @NonNull TextView lblItemSerial,
      @NonNull LinearLayout linearLayout, @NonNull RecyclerView recyclerView,
      @NonNull TextView textView, @NonNull EditText txtItemSerial) {
    this.rootView = rootView;
    this.UPCPricingListView = UPCPricingListView;
    this.btnUPCPricingDone = btnUPCPricingDone;
    this.lblError = lblError;
    this.lblItemSerial = lblItemSerial;
    this.linearLayout = linearLayout;
    this.recyclerView = recyclerView;
    this.textView = textView;
    this.txtItemSerial = txtItemSerial;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ContentNewItemPricingBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ContentNewItemPricingBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.content_new_item_pricing, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ContentNewItemPricingBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.UPCPricingListView;
      TextView UPCPricingListView = ViewBindings.findChildViewById(rootView, id);
      if (UPCPricingListView == null) {
        break missingId;
      }

      id = R.id.btnUPCPricingDone;
      Button btnUPCPricingDone = ViewBindings.findChildViewById(rootView, id);
      if (btnUPCPricingDone == null) {
        break missingId;
      }

      id = R.id.lblError;
      TextView lblError = ViewBindings.findChildViewById(rootView, id);
      if (lblError == null) {
        break missingId;
      }

      id = R.id.lblItemSerial;
      TextView lblItemSerial = ViewBindings.findChildViewById(rootView, id);
      if (lblItemSerial == null) {
        break missingId;
      }

      id = R.id.linearLayout;
      LinearLayout linearLayout = ViewBindings.findChildViewById(rootView, id);
      if (linearLayout == null) {
        break missingId;
      }

      id = R.id.recyclerView;
      RecyclerView recyclerView = ViewBindings.findChildViewById(rootView, id);
      if (recyclerView == null) {
        break missingId;
      }

      id = R.id.textView;
      TextView textView = ViewBindings.findChildViewById(rootView, id);
      if (textView == null) {
        break missingId;
      }

      id = R.id.txtItemSerial;
      EditText txtItemSerial = ViewBindings.findChildViewById(rootView, id);
      if (txtItemSerial == null) {
        break missingId;
      }

      return new ContentNewItemPricingBinding((ConstraintLayout) rootView, UPCPricingListView,
          btnUPCPricingDone, lblError, lblItemSerial, linearLayout, recyclerView, textView,
          txtItemSerial);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
