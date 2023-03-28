// Generated by view binder compiler. Do not edit!
package com.bos.wms.mlkit.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.bos.wms.mlkit.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityUpcPricingBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final Button confirmBtn;

  @NonNull
  public final ConstraintLayout constraintLayout;

  @NonNull
  public final Guideline guideline;

  @NonNull
  public final Guideline horizontalGuideLineTwo;

  @NonNull
  public final EditText insertBarcode;

  @NonNull
  public final TextView lblItemSerial;

  @NonNull
  public final TextView lblUPC;

  @NonNull
  public final Button scannedItemSerial;

  @NonNull
  public final Button scannedItemUPC;

  @NonNull
  public final ListView scannedItemsListView;

  @NonNull
  public final ConstraintLayout upcPricingActivityLayout;

  private ActivityUpcPricingBinding(@NonNull ConstraintLayout rootView, @NonNull Button confirmBtn,
      @NonNull ConstraintLayout constraintLayout, @NonNull Guideline guideline,
      @NonNull Guideline horizontalGuideLineTwo, @NonNull EditText insertBarcode,
      @NonNull TextView lblItemSerial, @NonNull TextView lblUPC, @NonNull Button scannedItemSerial,
      @NonNull Button scannedItemUPC, @NonNull ListView scannedItemsListView,
      @NonNull ConstraintLayout upcPricingActivityLayout) {
    this.rootView = rootView;
    this.confirmBtn = confirmBtn;
    this.constraintLayout = constraintLayout;
    this.guideline = guideline;
    this.horizontalGuideLineTwo = horizontalGuideLineTwo;
    this.insertBarcode = insertBarcode;
    this.lblItemSerial = lblItemSerial;
    this.lblUPC = lblUPC;
    this.scannedItemSerial = scannedItemSerial;
    this.scannedItemUPC = scannedItemUPC;
    this.scannedItemsListView = scannedItemsListView;
    this.upcPricingActivityLayout = upcPricingActivityLayout;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityUpcPricingBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityUpcPricingBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_upc_pricing, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityUpcPricingBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.confirmBtn;
      Button confirmBtn = ViewBindings.findChildViewById(rootView, id);
      if (confirmBtn == null) {
        break missingId;
      }

      id = R.id.constraintLayout;
      ConstraintLayout constraintLayout = ViewBindings.findChildViewById(rootView, id);
      if (constraintLayout == null) {
        break missingId;
      }

      id = R.id.guideline;
      Guideline guideline = ViewBindings.findChildViewById(rootView, id);
      if (guideline == null) {
        break missingId;
      }

      id = R.id.horizontalGuideLineTwo;
      Guideline horizontalGuideLineTwo = ViewBindings.findChildViewById(rootView, id);
      if (horizontalGuideLineTwo == null) {
        break missingId;
      }

      id = R.id.insertBarcode;
      EditText insertBarcode = ViewBindings.findChildViewById(rootView, id);
      if (insertBarcode == null) {
        break missingId;
      }

      id = R.id.lblItemSerial;
      TextView lblItemSerial = ViewBindings.findChildViewById(rootView, id);
      if (lblItemSerial == null) {
        break missingId;
      }

      id = R.id.lblUPC;
      TextView lblUPC = ViewBindings.findChildViewById(rootView, id);
      if (lblUPC == null) {
        break missingId;
      }

      id = R.id.scannedItemSerial;
      Button scannedItemSerial = ViewBindings.findChildViewById(rootView, id);
      if (scannedItemSerial == null) {
        break missingId;
      }

      id = R.id.scannedItemUPC;
      Button scannedItemUPC = ViewBindings.findChildViewById(rootView, id);
      if (scannedItemUPC == null) {
        break missingId;
      }

      id = R.id.scannedItemsListView;
      ListView scannedItemsListView = ViewBindings.findChildViewById(rootView, id);
      if (scannedItemsListView == null) {
        break missingId;
      }

      ConstraintLayout upcPricingActivityLayout = (ConstraintLayout) rootView;

      return new ActivityUpcPricingBinding((ConstraintLayout) rootView, confirmBtn,
          constraintLayout, guideline, horizontalGuideLineTwo, insertBarcode, lblItemSerial, lblUPC,
          scannedItemSerial, scannedItemUPC, scannedItemsListView, upcPricingActivityLayout);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
