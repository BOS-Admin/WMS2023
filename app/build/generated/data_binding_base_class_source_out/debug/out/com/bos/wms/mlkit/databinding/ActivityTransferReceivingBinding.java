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
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.bos.wms.mlkit.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityTransferReceivingBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final ConstraintLayout activityScanRfid;

  @NonNull
  public final Button btnDone;

  @NonNull
  public final Button btnEnd;

  @NonNull
  public final TextView lblBox;

  @NonNull
  public final TextView lblCount;

  @NonNull
  public final TextView lblError;

  @NonNull
  public final TextView lblScanError;

  @NonNull
  public final ListView listView;

  @NonNull
  public final EditText textBox;

  @NonNull
  public final EditText textBranch;

  @NonNull
  public final EditText textCount;

  @NonNull
  public final EditText textUser;

  private ActivityTransferReceivingBinding(@NonNull ConstraintLayout rootView,
      @NonNull ConstraintLayout activityScanRfid, @NonNull Button btnDone, @NonNull Button btnEnd,
      @NonNull TextView lblBox, @NonNull TextView lblCount, @NonNull TextView lblError,
      @NonNull TextView lblScanError, @NonNull ListView listView, @NonNull EditText textBox,
      @NonNull EditText textBranch, @NonNull EditText textCount, @NonNull EditText textUser) {
    this.rootView = rootView;
    this.activityScanRfid = activityScanRfid;
    this.btnDone = btnDone;
    this.btnEnd = btnEnd;
    this.lblBox = lblBox;
    this.lblCount = lblCount;
    this.lblError = lblError;
    this.lblScanError = lblScanError;
    this.listView = listView;
    this.textBox = textBox;
    this.textBranch = textBranch;
    this.textCount = textCount;
    this.textUser = textUser;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityTransferReceivingBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityTransferReceivingBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_transfer_receiving, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityTransferReceivingBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      ConstraintLayout activityScanRfid = (ConstraintLayout) rootView;

      id = R.id.btnDone;
      Button btnDone = ViewBindings.findChildViewById(rootView, id);
      if (btnDone == null) {
        break missingId;
      }

      id = R.id.btnEnd;
      Button btnEnd = ViewBindings.findChildViewById(rootView, id);
      if (btnEnd == null) {
        break missingId;
      }

      id = R.id.lblBox;
      TextView lblBox = ViewBindings.findChildViewById(rootView, id);
      if (lblBox == null) {
        break missingId;
      }

      id = R.id.lblCount;
      TextView lblCount = ViewBindings.findChildViewById(rootView, id);
      if (lblCount == null) {
        break missingId;
      }

      id = R.id.lblError;
      TextView lblError = ViewBindings.findChildViewById(rootView, id);
      if (lblError == null) {
        break missingId;
      }

      id = R.id.lblScanError;
      TextView lblScanError = ViewBindings.findChildViewById(rootView, id);
      if (lblScanError == null) {
        break missingId;
      }

      id = R.id.listView;
      ListView listView = ViewBindings.findChildViewById(rootView, id);
      if (listView == null) {
        break missingId;
      }

      id = R.id.textBox;
      EditText textBox = ViewBindings.findChildViewById(rootView, id);
      if (textBox == null) {
        break missingId;
      }

      id = R.id.textBranch;
      EditText textBranch = ViewBindings.findChildViewById(rootView, id);
      if (textBranch == null) {
        break missingId;
      }

      id = R.id.textCount;
      EditText textCount = ViewBindings.findChildViewById(rootView, id);
      if (textCount == null) {
        break missingId;
      }

      id = R.id.textUser;
      EditText textUser = ViewBindings.findChildViewById(rootView, id);
      if (textUser == null) {
        break missingId;
      }

      return new ActivityTransferReceivingBinding((ConstraintLayout) rootView, activityScanRfid,
          btnDone, btnEnd, lblBox, lblCount, lblError, lblScanError, listView, textBox, textBranch,
          textCount, textUser);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}