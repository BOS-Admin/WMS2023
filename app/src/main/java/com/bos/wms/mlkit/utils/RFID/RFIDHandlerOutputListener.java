package com.bos.wms.mlkit.utils.RFID;

import com.rfidread.Models.Tag_Model;

public interface RFIDHandlerOutputListener {

    public void onTagRead(String rfid, Tag_Model model);
    public void onTagProcessed(String rfid, Tag_Model model, int count, boolean isTheOnlyTagRead);
}
