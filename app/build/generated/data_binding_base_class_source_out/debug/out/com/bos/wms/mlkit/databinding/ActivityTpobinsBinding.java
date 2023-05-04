// Generated by view binder compiler. Do not edit!
package com.bos.wms.mlkit.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Guideline;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.bos.wms.mlkit.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityTpobinsBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final ListView binItemsList;

  @NonNull
  public final ImageButton currentModifyMode;

  @NonNull
  public final Guideline guideline;

  @NonNull
  public final EditText insertBarcodeEditText;

  @NonNull
  public final TextView scanBarcodeHelpText;

  @NonNull
  public final ImageButton shipTPOBinsIcon;

  @NonNull
  public final LinearLayout tpoBinsActivityLayout;

  @NonNull
  public final TextView tpoMenuTitle;

  @NonNull
  public final Guideline verticalCenterLine;

  private ActivityTpobinsBinding(@NonNull LinearLayout rootView, @NonNull ListView binItemsList,
      @NonNull ImageButton currentModifyMode, @NonNull Guideline guideline,
      @NonNull EditText insertBarcodeEditText, @NonNull TextView scanBarcodeHelpText,
      @NonNull ImageButton shipTPOBinsIcon, @NonNull LinearLayout tpoBinsActivityLayout,
      @NonNull TextView tpoMenuTitle, @NonNull Guideline verticalCenterLine) {
    this.rootView = rootView;
    this.binItemsList = binItemsList;
    this.currentModifyMode = currentModifyMode;
    this.guideline = guideline;
    this.insertBarcodeEditText = insertBarcodeEditText;
    this.scanBarcodeHelpText = scanBarcodeHelpText;
    this.shipTPOBinsIcon = shipTPOBinsIcon;
    this.tpoBinsActivityLayout = tpoBinsActivityLayout;
    this.tpoMenuTitle = tpoMenuTitle;
    this.verticalCenterLine = verticalCenterLine;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityTpobinsBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityTpobinsBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_tpobins, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityTpobinsBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.binItemsList;
      ListView binItemsList = ViewBindings.findChildViewById(rootView, id);
      if (binItemsList == null) {
        break missingId;
      }

      id = R.id.currentModifyMode;
      ImageButton currentModifyMode = ViewBindings.findChildViewById(rootView, id);
      if (currentModifyMode == null) {
        break missingId;
      }

      id = R.id.guideline;
      Guideline guideline = ViewBindings.findChildViewById(rootView, id);
      if (guideline == null) {
        break missingId;
      }

      id = R.id.insertBarcodeEditText;
      EditText insertBarcodeEditText = ViewBindings.findChildViewById(rootView, id);
      if (insertBarcodeEditText == null) {
        break missingId;
      }

      id = R.id.scanBarcodeHelpText;
      TextView scanBarcodeHelpText = ViewBindings.findChildViewById(rootView, id);
      if (scanBarcodeHelpText == null) {
        break missingId;
      }

      id = R.id.shipTPOBinsIcon;
      ImageButton shipTPOBinsIcon = ViewBindings.findChildViewById(rootView, id);
      if (shipTPOBinsIcon == null) {
        break missingId;
      }

      LinearLayout tpoBinsActivityLayout = (LinearLayout) rootView;

      id = R.id.tpoMenuTitle;
      TextView tpoMenuTitle = ViewBindings.findChildViewById(rootView, id);
      if (tpoMenuTitle == null) {
        break missingId;
      }

      id = R.id.vertical_center_line;
      Guideline verticalCenterLine = ViewBindings.findChildViewById(rootView, id);
      if (verticalCenterLine == null) {
        break missingId;
      }

      return new ActivityTpobinsBinding((LinearLayout) rootView, binItemsList, currentModifyMode,
          guideline, insertBarcodeEditText, scanBarcodeHelpText, shipTPOBinsIcon,
          tpoBinsActivityLayout, tpoMenuTitle, verticalCenterLine);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}