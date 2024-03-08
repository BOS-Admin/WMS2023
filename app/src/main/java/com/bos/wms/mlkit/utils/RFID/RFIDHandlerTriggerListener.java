package com.bos.wms.mlkit.utils.RFID;

import java.util.ArrayList;

public interface RFIDHandlerTriggerListener {
    public void onTriggerPressed();
    public void onTriggerReleased();

    public void onTriggerReleasedRFIDs(ArrayList<String> rfids);

}
