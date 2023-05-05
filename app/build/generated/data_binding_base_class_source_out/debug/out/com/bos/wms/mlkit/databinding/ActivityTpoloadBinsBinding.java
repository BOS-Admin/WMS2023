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
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.bos.wms.mlkit.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityTpoloadBinsBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final Button confirmBtn;

  @NonNull
  public final TextView currentTPOInfoTxt;

  @NonNull
  public final EditText insertBarcodeEditText;

  @NonNull
  public final Button scanBoxesTxt;

  @NonNull
  public final Button tpoInfoBtn;

  @NonNull
  public final LinearLayout tpoLoadBinsActivityLayout;

  @NonNull
  public final TextView tpoMenuTitle;

  @NonNull
  public final Button truckInfoBtn;

  private ActivityTpoloadBinsBinding(@NonNull LinearLayout rootView, @NonNull Button confirmBtn,
      @NonNull TextView currentTPOInfoTxt, @NonNull EditText insertBarcodeEditText,
      @NonNull Button scanBoxesTxt, @NonNull Button tpoInfoBtn,
      @NonNull LinearLayout tpoLoadBinsActivityLayout, @NonNull TextView tpoMenuTitle,
      @NonNull Button truckInfoBtn) {
    this.rootView = rootView;
    this.confirmBtn = confirmBtn;
    this.currentTPOInfoTxt = currentTPOInfoTxt;
    this.insertBarcodeEditText = insertBarcodeEditText;
    this.scanBoxesTxt = scanBoxesTxt;
    this.tpoInfoBtn = tpoInfoBtn;
    this.tpoLoadBinsActivityLayout = tpoLoadBinsActivityLayout;
    this.tpoMenuTitle = tpoMenuTitle;
    this.truckInfoBtn = truckInfoBtn;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityTpoloadBinsBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityTpoloadBinsBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_tpoload_bins, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityTpoloadBinsBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.confirmBtn;
      Button confirmBtn = ViewBindings.findChildViewById(rootView, id);
      if (confirmBtn == null) {
        break missingId;
      }

      id = R.id.currentTPOInfoTxt;
      TextView currentTPOInfoTxt = ViewBindings.findChildViewById(rootView, id);
      if (currentTPOInfoTxt == null) {
        break missingId;
      }

      id = R.id.insertBarcodeEditText;
      EditText insertBarcodeEditText = ViewBindings.findChildViewById(rootView, id);
      if (insertBarcodeEditText == null) {
        break missingId;
      }

      id = R.id.scanBoxesTxt;
      Button scanBoxesTxt = ViewBindings.findChildViewById(rootView, id);
      if (scanBoxesTxt == null) {
        break missingId;
      }

      id = R.id.tpoInfoBtn;
      Button tpoInfoBtn = ViewBindings.findChildViewById(rootView, id);
      if (tpoInfoBtn == null) {
        break missingId;
      }

      LinearLayout tpoLoadBinsActivityLayout = (LinearLayout) rootView;

      id = R.id.tpoMenuTitle;
      TextView tpoMenuTitle = ViewBindings.findChildViewById(rootView, id);
      if (tpoMenuTitle == null) {
        break missingId;
      }

      id = R.id.truckInfoBtn;
      Button truckInfoBtn = ViewBindings.findChildViewById(rootView, id);
      if (truckInfoBtn == null) {
        break missingId;
      }

      return new ActivityTpoloadBinsBinding((LinearLayout) rootView, confirmBtn, currentTPOInfoTxt,
          insertBarcodeEditText, scanBoxesTxt, tpoInfoBtn, tpoLoadBinsActivityLayout, tpoMenuTitle,
          truckInfoBtn);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
