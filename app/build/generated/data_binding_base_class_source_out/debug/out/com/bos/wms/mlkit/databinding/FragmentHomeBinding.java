// Generated by view binder compiler. Do not edit!
package com.bos.wms.mlkit.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.bos.wms.mlkit.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentHomeBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final Button btnBolRecognition;

  @NonNull
  public final Button btnBrandOCR;

  @NonNull
  public final Button btnEmptyBox;

  @NonNull
  public final Button btnLocationCheck;

  @NonNull
  public final Button btnMenuFillPallete;

  @NonNull
  public final Button btnMenuItemPricing;

  @NonNull
  public final Button btnMenuPGPricing;

  @NonNull
  public final Button btnMenuPicking;

  @NonNull
  public final Button btnMenuPutAwayPallete;

  @NonNull
  public final Button btnMenuShipmentCartonReceiving;

  @NonNull
  public final Button btnMenuShipmentCartonReceivingV2;

  @NonNull
  public final Button btnMenuShipmentPalleteReceiving;

  @NonNull
  public final Button btnMenuShipmentReceivingPalleteCount;

  @NonNull
  public final Button btnPASBrandOCR;

  @NonNull
  public final Button btnSerialGenerator;

  @NonNull
  public final Button btnSerialMissing;

  @NonNull
  public final Button btnUPCPricing;

  @NonNull
  public final LinearLayout homeFragmentLayout;

  private FragmentHomeBinding(@NonNull LinearLayout rootView, @NonNull Button btnBolRecognition,
      @NonNull Button btnBrandOCR, @NonNull Button btnEmptyBox, @NonNull Button btnLocationCheck,
      @NonNull Button btnMenuFillPallete, @NonNull Button btnMenuItemPricing,
      @NonNull Button btnMenuPGPricing, @NonNull Button btnMenuPicking,
      @NonNull Button btnMenuPutAwayPallete, @NonNull Button btnMenuShipmentCartonReceiving,
      @NonNull Button btnMenuShipmentCartonReceivingV2,
      @NonNull Button btnMenuShipmentPalleteReceiving,
      @NonNull Button btnMenuShipmentReceivingPalleteCount, @NonNull Button btnPASBrandOCR,
      @NonNull Button btnSerialGenerator, @NonNull Button btnSerialMissing,
      @NonNull Button btnUPCPricing, @NonNull LinearLayout homeFragmentLayout) {
    this.rootView = rootView;
    this.btnBolRecognition = btnBolRecognition;
    this.btnBrandOCR = btnBrandOCR;
    this.btnEmptyBox = btnEmptyBox;
    this.btnLocationCheck = btnLocationCheck;
    this.btnMenuFillPallete = btnMenuFillPallete;
    this.btnMenuItemPricing = btnMenuItemPricing;
    this.btnMenuPGPricing = btnMenuPGPricing;
    this.btnMenuPicking = btnMenuPicking;
    this.btnMenuPutAwayPallete = btnMenuPutAwayPallete;
    this.btnMenuShipmentCartonReceiving = btnMenuShipmentCartonReceiving;
    this.btnMenuShipmentCartonReceivingV2 = btnMenuShipmentCartonReceivingV2;
    this.btnMenuShipmentPalleteReceiving = btnMenuShipmentPalleteReceiving;
    this.btnMenuShipmentReceivingPalleteCount = btnMenuShipmentReceivingPalleteCount;
    this.btnPASBrandOCR = btnPASBrandOCR;
    this.btnSerialGenerator = btnSerialGenerator;
    this.btnSerialMissing = btnSerialMissing;
    this.btnUPCPricing = btnUPCPricing;
    this.homeFragmentLayout = homeFragmentLayout;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentHomeBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentHomeBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_home, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentHomeBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnBolRecognition;
      Button btnBolRecognition = ViewBindings.findChildViewById(rootView, id);
      if (btnBolRecognition == null) {
        break missingId;
      }

      id = R.id.btnBrandOCR;
      Button btnBrandOCR = ViewBindings.findChildViewById(rootView, id);
      if (btnBrandOCR == null) {
        break missingId;
      }

      id = R.id.btnEmptyBox;
      Button btnEmptyBox = ViewBindings.findChildViewById(rootView, id);
      if (btnEmptyBox == null) {
        break missingId;
      }

      id = R.id.btnLocationCheck;
      Button btnLocationCheck = ViewBindings.findChildViewById(rootView, id);
      if (btnLocationCheck == null) {
        break missingId;
      }

      id = R.id.btnMenuFillPallete;
      Button btnMenuFillPallete = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuFillPallete == null) {
        break missingId;
      }

      id = R.id.btnMenuItemPricing;
      Button btnMenuItemPricing = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuItemPricing == null) {
        break missingId;
      }

      id = R.id.btnMenuPGPricing;
      Button btnMenuPGPricing = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuPGPricing == null) {
        break missingId;
      }

      id = R.id.btnMenuPicking;
      Button btnMenuPicking = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuPicking == null) {
        break missingId;
      }

      id = R.id.btnMenuPutAwayPallete;
      Button btnMenuPutAwayPallete = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuPutAwayPallete == null) {
        break missingId;
      }

      id = R.id.btnMenuShipmentCartonReceiving;
      Button btnMenuShipmentCartonReceiving = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuShipmentCartonReceiving == null) {
        break missingId;
      }

      id = R.id.btnMenuShipmentCartonReceivingV2;
      Button btnMenuShipmentCartonReceivingV2 = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuShipmentCartonReceivingV2 == null) {
        break missingId;
      }

      id = R.id.btnMenuShipmentPalleteReceiving;
      Button btnMenuShipmentPalleteReceiving = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuShipmentPalleteReceiving == null) {
        break missingId;
      }

      id = R.id.btnMenuShipmentReceivingPalleteCount;
      Button btnMenuShipmentReceivingPalleteCount = ViewBindings.findChildViewById(rootView, id);
      if (btnMenuShipmentReceivingPalleteCount == null) {
        break missingId;
      }

      id = R.id.btnPASBrandOCR;
      Button btnPASBrandOCR = ViewBindings.findChildViewById(rootView, id);
      if (btnPASBrandOCR == null) {
        break missingId;
      }

      id = R.id.btnSerialGenerator;
      Button btnSerialGenerator = ViewBindings.findChildViewById(rootView, id);
      if (btnSerialGenerator == null) {
        break missingId;
      }

      id = R.id.btnSerialMissing;
      Button btnSerialMissing = ViewBindings.findChildViewById(rootView, id);
      if (btnSerialMissing == null) {
        break missingId;
      }

      id = R.id.btnUPCPricing;
      Button btnUPCPricing = ViewBindings.findChildViewById(rootView, id);
      if (btnUPCPricing == null) {
        break missingId;
      }

      LinearLayout homeFragmentLayout = (LinearLayout) rootView;

      return new FragmentHomeBinding((LinearLayout) rootView, btnBolRecognition, btnBrandOCR,
          btnEmptyBox, btnLocationCheck, btnMenuFillPallete, btnMenuItemPricing, btnMenuPGPricing,
          btnMenuPicking, btnMenuPutAwayPallete, btnMenuShipmentCartonReceiving,
          btnMenuShipmentCartonReceivingV2, btnMenuShipmentPalleteReceiving,
          btnMenuShipmentReceivingPalleteCount, btnPASBrandOCR, btnSerialGenerator,
          btnSerialMissing, btnUPCPricing, homeFragmentLayout);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
