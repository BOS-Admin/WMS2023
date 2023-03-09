// Generated by view binder compiler. Do not edit!
package com.bos.wms.mlkit.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.bos.wms.mlkit.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityMainMenu1Binding implements ViewBinding {
  @NonNull
  private final CoordinatorLayout rootView;

  @NonNull
  public final Button btnCount;

  @NonNull
  public final EditText textBranch;

  @NonNull
  public final EditText textUser;

  @NonNull
  public final Toolbar toolbar;

  private ActivityMainMenu1Binding(@NonNull CoordinatorLayout rootView, @NonNull Button btnCount,
      @NonNull EditText textBranch, @NonNull EditText textUser, @NonNull Toolbar toolbar) {
    this.rootView = rootView;
    this.btnCount = btnCount;
    this.textBranch = textBranch;
    this.textUser = textUser;
    this.toolbar = toolbar;
  }

  @Override
  @NonNull
  public CoordinatorLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityMainMenu1Binding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityMainMenu1Binding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_main_menu1, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityMainMenu1Binding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnCount;
      Button btnCount = ViewBindings.findChildViewById(rootView, id);
      if (btnCount == null) {
        break missingId;
      }

      id = R.id.textBranch;
      EditText textBranch = ViewBindings.findChildViewById(rootView, id);
      if (textBranch == null) {
        break missingId;
      }

      id = R.id.textUser;
      EditText textUser = ViewBindings.findChildViewById(rootView, id);
      if (textUser == null) {
        break missingId;
      }

      id = R.id.toolbar;
      Toolbar toolbar = ViewBindings.findChildViewById(rootView, id);
      if (toolbar == null) {
        break missingId;
      }

      return new ActivityMainMenu1Binding((CoordinatorLayout) rootView, btnCount, textBranch,
          textUser, toolbar);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}