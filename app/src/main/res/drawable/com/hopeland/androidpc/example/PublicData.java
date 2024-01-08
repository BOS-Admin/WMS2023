package com.hopeland.androidpc.example;

import java.util.List;
import android.hardware.usb.UsbManager;
import com.rfidread.usbserial.driver.UsbSerialPort;

/**
 * 
 * @author RFID_C Public data repository area
 * 
 */
public class PublicData {

	public static String _IsCommand6Cor6B = "6C";// 6C indicates operation of the 6C tag. 6B indicates operation of the 6B tag.
	public static int _PingPong_ReadTime = 10000; // default is 100:3
	public static int _PingPong_StopTime = 300;
	
	public static String serialParam = "/dev/ttySAC1:115200";//serial port information
	public static String tcpParam = "192.168.1.116:9090";//tcp connection information
	public static String usbParam = "";
	public static String bt4Param = "";
	
	//usb devices list
	public static List<UsbSerialPort> sPortList = null;
	public static List<String> usbListStr= null;//usb connection parameter list
	public static UsbManager mUsbManager = null;
}
