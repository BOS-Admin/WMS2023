package com.hopeland.androidpc.example.uhf;

import java.util.HashMap;

import com.hopeland.androidpc.example.PublicData;
import com.hc.mixthebluetooth.R;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.RFIDReader;
import com.rfidread.RFID_Option;
import com.util.BaseActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;


/**
 * @author RFID_lx Activity base class
 */
public class UHFBaseActivity extends BaseActivity {

	protected static Boolean _UHFSTATE = false; // Whether the module is open
	// static int _PingPong_ReadTime = 10000; // default is 100:3
	// static int _PingPong_StopTime = 300;
	protected static int _NowAntennaNo = 1; // Reader Antenna No.
	protected static int _UpDataTime = 0; // Repeat tag upload time, control tag upload speed not too fast
	protected static int _Max_Power = 30; // Maximum transmitting power of the reader
	protected static int _Min_Power = 0; // Minimum transmitting power of the reader
	protected static int _ReaderAntennaCount = 1;//Number of reader antennas


	//Current connected ID
	public static String ConnID = "";
	//Current connection index
	public static int connectIndex = 0;
	//Current selection of antennas for tag reading, such as "1&2&4", currently select the 1,2,4 antenna to read the tag.

	//Antenna port value for incoming frame protocol
	public static HashMap<Integer, Integer> hm_Ant_value = new HashMap<Integer, Integer>() {
		{
			//Math.pow(2, 0),Currently supports up to 24 antennas
			put(1, 1);
			put(2, 2);
			put(3, 4);
			put(4, 8);
			put(5, 16);
			put(6, 32);
			put(7, 64);
			put(8, 128);
			put(9, 256);
			put(10, 512);
			put(11, 1024);
			put(12, 2048);
			put(13, 4096);
			put(14, 8192);
			put(15, 16384);
			put(16, 32768);
			put(17, 65536);
			put(18, 131072);
			put(19, 262144);
			put(20, 524288);
			put(21, 1048576);
			put(22, 2097152);
			put(23, 4194304);
			put(24, 8388608);
		}
	};


	/**
	 * Module initialization (serial port)
	 *
	 * @param log
	 *            Interface callback method
	 * @return Whether the initialization was successful
	 */
	public Boolean Rfid_RS232_Init(String connParam, IAsynchronousMessage log) {
		Boolean rt = false;
		try {
			if (_UHFSTATE == false) {
				rt = RFIDReader.CreateSerialConn(connParam, log);
				if (rt) {
					ConnID = PublicData.serialParam = connParam;
					connectIndex=0;
					_UHFSTATE = true;
				}
				Thread.sleep(500);
			} else {
				rt = true;
			}
		} catch (Exception ex) {
			Log.d("debug", "On the UHF electric abnormal:" + ex.getMessage());
		}
		return rt;
	}


	/**
	 * Module initialization (Tcp)
	 *
	 * @param log
	 *            Interface callback method
	 * @return Whether the initialization was successful
	 */
	public Boolean Rfid_Tcp_Init(String connParam,IAsynchronousMessage log) {
		Boolean rt = false;
		try {
			if (_UHFSTATE == false) {
				rt = RFIDReader.CreateTcpConn(connParam, log);
				if (rt) {
					ConnID = PublicData.tcpParam = connParam;
					connectIndex = 1;
					_UHFSTATE = true;
				}
				Thread.sleep(500);
			} else {
				rt = true;
			}
		} catch (Exception ex) {
			Log.d("debug", "On the UHF electric abnormal:" + ex.getMessage());
		}
		return rt;
	}


	/**
	 * Get a list of USB devices
	 *
	 * @return Number of devices
	 */
	public int  GetUsbDeviceList(){
		//Getting system services through Context
		PublicData.mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		//Get a list of Usb devices
		PublicData.sPortList = RFIDReader.GetUSBList(PublicData.mUsbManager);
		//usb connection parameter list
		PublicData.usbListStr = RFIDReader.GetUsbDeviceStrList(PublicData.sPortList);

		return PublicData.usbListStr.size();

	}

	/**
	 * Module initialization (Usb)
	 *
	 * @param log
	 *            Interface callback method
	 * @return Whether the initialization was successful
	 */
	public Boolean Rfid_Usb_Init(String connParam, IAsynchronousMessage log) {
		Boolean rt = false;
		try {
			if (_UHFSTATE == false) {

				if (!RFIDReader.GetUsbPermission(this, connParam)) {
					return false;
				}

				rt = RFIDReader.CreateUsbConn(connParam,log);
				if (rt) {
					ConnID = PublicData.usbParam = connParam;
					connectIndex = 2;
					_UHFSTATE = true;
				}
				Thread.sleep(500);
			} else {
				rt = true;
			}
		} catch (Exception ex) {
			Log.d("debug", "On the UHF electric abnormal:" + ex.getMessage());
		}
		return rt;
	}


	/**
	 * Module initialization (Bluetooth)
	 *
	 * @param log
	 *            Interface callback method
	 * @return Whether the initialization was successful
	 */
	public Boolean Rfid_BT4_Init(String connParam, IAsynchronousMessage log) {
		Boolean rt = false;
		try {
			if (_UHFSTATE == false) {
				rt = RFIDReader.CreateBT4Conn(connParam, log);
				if (rt) {
					ConnID = PublicData.bt4Param = connParam;
					connectIndex = 3;
					_UHFSTATE = true;
				}
				Thread.sleep(500);
			} else {
				rt = true;
			}
		} catch (Exception ex) {
			Log.d("debug", "On the UHF electric abnormal:" + ex.getMessage());
		}
		return rt;
	}



	/**
	 * release UHF module
	 */
	public void UHF_Dispose() {
		if (_UHFSTATE == true) {
			RFIDReader.CloseConn(ConnID);
			_UHFSTATE = false;
		}
	}

	/**
	 * Get reader property
	 */
	@SuppressLint("UseSparseArrays")
	@SuppressWarnings("serial")
	protected void UHF_GetReaderProperty() {
		String propertyStr = RFIDReader.GetReaderProperty(ConnID);
		//Log.d("Debug", "Get Reader Property:" + propertyStr);
		String[] propertyArr = propertyStr.split("\\|");
		if (propertyArr.length > 3) {
			try {
				_Min_Power = Integer.parseInt(propertyArr[0]);
				_Max_Power = Integer.parseInt(propertyArr[1]);
				_ReaderAntennaCount = Integer.parseInt(propertyArr[2]);
				//_NowAntennaNo = hm_Ant_value.get(_ReaderAntennaCount);
			} catch (Exception ex) {
				Log.d("Debug", "Get Reader Property failure and conversion failed!");
			}
		} else {
			Log.d("Debug", "Get Reader Property failure");
		}
	}

	/**
	 * Set tag upload parameters
	 */
	protected void UHF_SetTagUpdateParam() {
		// Check if the current settings are the same, if not, then set it.
		String	searchRT  = "";
		try {
			searchRT = RFIDReader._Config.GetTagUpdateParam(ConnID);
			String[] arrRT = searchRT.split("\\|");
			if (arrRT.length >= 2) {
				int nowUpDataTime = Integer.parseInt(arrRT[0]);
				int rssiFilter = Integer.parseInt(arrRT[1]);
				Log.d("Debug", "Check the label to upload time:" + nowUpDataTime);
				if (_UpDataTime != nowUpDataTime) {
					String param="1,"+_UpDataTime;
					RFIDReader._Config.SetTagUpdateParam(ConnID,_UpDataTime, rssiFilter); // Set repeat tag upload time to 20ms
					Log.d("Debug", "Sets the label upload time...");
				} else {
				}
			} else {
				Log.d("Debug", "Query tags while uploading failure...");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
