package com.hopeland.androidpc.example.uhf;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hopeland.androidpc.example.PublicData;
import com.hc.mixthebluetooth.R;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Helper.Helper_ThreadPool;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import android.content.Context;
import android.media.*;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.*;

/**
 * @author RFID_lx read tags form
 */
public class ReadEPCActivity extends UHFBaseActivity implements
		IAsynchronousMessage {

	private ListView listView = null; // Data List Object
	private SimpleAdapter sa = null;
	private Button btn_Read = null;
	private TextView tv_TitleTagID = null;
	private TextView tv_TitleTagCount = null;
	private TextView lb_ReadTime = null;
	private TextView lb_ReadSpeed = null;
	private TextView lb_TagCount = null;
	private TextView lb_WeightVal = null;
	private Spinner sp_ReadType = null;

	private int readTime = 0;
	private int lastReadCount = 0;
	private int totalReadCount = 0; // Total number of readings
	private int speed = 0; // Reading speed
	private static int _ReadType = 0; // 0 means reading EPC, 1 for reading TID
	private HashMap<String, Tag_Model> hmList = new HashMap<String, Tag_Model>();
	private Object hmList_Lock = new Object();
	private Boolean IsFlushList = true; // Whether to refresh the list
	private Object beep_Lock = new Object();
	ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM,
			ToneGenerator.MAX_VOLUME);

	private final int MSG_RESULT_READ = MSG_USER_BEG + 1; // constant reading
	private final int MSG_FLUSH_READTIME = MSG_USER_BEG + 2;

	final String FILE_NAME = "/mnt/sdcard/UHF_Read_Result.csv";
	private  boolean bSaveResult = false;

	private  boolean isbusy=false;

	@Override
	protected void msgProcess(Message msg) {
		switch (msg.what) {
			case MSG_RESULT_READ:
				ShowList(); // Refresh list
				break;
			case MSG_FLUSH_READTIME:
				if (lb_ReadTime != null) { // Refresh Read Time
					readTime++;
					lb_ReadTime.setText("Time:" + readTime + "S");
				}
				break;
			default:
				super.msgProcess(msg);
				break;
		}
	}

	// read function
	public void Read(View v) {
		if(isbusy)
		{
			return;
		}
		isbusy = true;
		Button btnRead = (Button) v;
		String controlText = btnRead.getText().toString();
		deleteFile(FILE_NAME);
		if (controlText.equals(getString(R.string.btn_read))) {
			PingPong_Read();
			btnRead.setText(getString(R.string.btn_read_stop));
			sp_ReadType.setEnabled(false);
		} else {
			Pingpong_Stop();
			btnRead.setText(getString(R.string.btn_read));
			sp_ReadType.setEnabled(true);
			write("Data,Count\r\n");
			SaveData();

		}
		isbusy = false;

	}

	@Override
	public boolean deleteFile(String filePath) {
		File file = new File(filePath);
		if (file.isFile() && file.exists()) {
			return file.delete();
		}
		return false;
	}

	// intermittent reading
	public void PingPong_Read() {
		Clear(null);
		Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
			@Override
			public void run() {
				try {
					String rt = "";
					if (PublicData._IsCommand6Cor6B.equals("6C")) {// read 6c tags
						GetEPC_6C();
					} else {// read 6b tags
						GetEPC_6B();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//6C,Read tag
	private  int GetEPC_6C()
	{
		int ret=-1;
		String text = tv_TitleTagID.getText().toString();
		if(text.equals("EPC")) {
			ret = RFIDReader._Tag6C.GetEPC(ConnID,_NowAntennaNo, eReadType.Inventory);
		}
		else if(text.equals("TID"))
		{
			ret = RFIDReader._Tag6C.GetEPC_TID(ConnID,_NowAntennaNo, eReadType.Inventory);
		}
		else if(text.equals("UserData"))
		{
			ret = RFIDReader._Tag6C.GetEPC_TID_UserData(ConnID,_NowAntennaNo, eReadType.Inventory,0,6);
		}
		return  ret;
	}

	//6C,Read tag
	private  int GetEPC_6B()
	{
		int ret=-1;
		String text = tv_TitleTagID.getText().toString();
		if(text.equals("EPC")) {
			ret = RFIDReader._Tag6B.Get6B(ConnID, _NowAntennaNo, eReadType.Inventory.GetNum(), 0);
		}
		else if(text.equals("TID"))
		{
			ret = RFIDReader._Tag6B.Get6B(ConnID, _NowAntennaNo, eReadType.Inventory.GetNum(), 0);
		}
		else if(text.equals("UserData"))
		{
			ret = RFIDReader._Tag6B.Get6B_UserData(ConnID,_NowAntennaNo, eReadType.Inventory.GetNum(),1,0, 15);
		}
		return  ret;
	}

	// stop reading
	public void Pingpong_Stop() {
		try {
			RFIDReader._Config.Stop(ConnID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void Clear(View v) {
		totalReadCount = 0;
		readTime = 0;
		hmList.clear();
		ShowList();
	}

	// Return to main page
	public void Back(View v) {
		if (btn_Read.getText().toString()
				.equals(getString(R.string.btn_read_stop))) {
			showMsg(getString(R.string.uhf_please_stop), null);
			return;
		}
		ReadEPCActivity.this.finish();
	}

	//initialize
	protected void Init() {
		try {
			Thread.sleep(20);
			try {
				RFIDReader._Config.Stop(ConnID);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Thread.sleep(20);
			super.UHF_SetTagUpdateParam(); // Set repeat tag upload time to 20ms
		} catch (Exception ee) {
		}

		//Refresh thread
		Refush();

		listView = (ListView) this.findViewById(R.id.lv_Main);
		tv_TitleTagID = (TextView) findViewById(R.id.tv_TitleTagID);
		tv_TitleTagCount = (TextView) findViewById(R.id.tv_TitleTagCount);
		lb_ReadTime = (TextView) findViewById(R.id.lb_ReadTime);
		lb_ReadSpeed = (TextView) findViewById(R.id.lb_ReadSpeed);
		lb_TagCount = (TextView) findViewById(R.id.lb_TagCount);
		lb_WeightVal = (TextView) findViewById(R.id.txtRFIDWeight);

		btn_Read = (Button) findViewById(R.id.btn_Read);
		btn_Read.setText(getString(R.string.btn_read));
		sp_ReadType = (Spinner) findViewById(R.id.sp_ReadType);
		//
		sp_ReadType.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0,
									   View arg1, int arg2, long arg3) {
				int selectItem = sp_ReadType
						.getSelectedItemPosition();
				if (PublicData._IsCommand6Cor6B.equals("6C")) {// read 6c tags
					tv_TitleTagCount.setText("Count");
					if (selectItem == 0) {
						_ReadType = 0;
						tv_TitleTagID.setText("EPC");
					} else if (selectItem == 1) {
						_ReadType = 1;
						tv_TitleTagID.setText("TID");
					} else if (selectItem == 2) {
						_ReadType = 2;
						tv_TitleTagID.setText("UserData");
					}
				} else {
					tv_TitleTagCount.setText("Count");
					if (selectItem == 0) {
						_ReadType = 0;
						tv_TitleTagID.setText("EPC");
					} else if (selectItem == 1) {
						_ReadType = 1;
						tv_TitleTagID.setText("TID");
					} else if (selectItem == 2) {
						_ReadType = 2;
						tv_TitleTagID.setText("UserData");
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		//
		RFIDReader.setAsynchronousMessage(ConnID, this);
	}

	//Refresh display thread
	protected void Refush()
	{
		IsFlushList = true;
		// Refresh thread
		Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
			@Override
			public void run() {
				while (IsFlushList) {
					try {
						sendMessage(MSG_RESULT_READ, null);
						Thread.sleep(1000); // Refreshes once a second.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

		Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() { //buzzer sound
			@Override
			public void run() {
				while (IsFlushList) {
					synchronized (beep_Lock) {
						try {
							beep_Lock.wait();
						} catch (InterruptedException e) {
						}
					}
					if (IsFlushList) {
						toneGenerator
								.startTone(ToneGenerator.TONE_PROP_BEEP);
					}

				}
			}
		});
	}

	// Releasing resources
	protected void Dispose() {
		IsFlushList = false;
		synchronized (beep_Lock) {
			beep_Lock.notifyAll();
		}
	}

	//Show list
	protected void ShowList() {
		sa = new SimpleAdapter(this, GetData(), R.layout.epclist_item,
				new String[] { "EPC", "ReadCount" }, new int[] {
				R.id.EPCList_TagID, R.id.EPCList_ReadCount });
		listView.setAdapter(sa);
		listView.invalidate();
		if (lb_ReadTime != null) { // Refresh Read Time
			readTime++;
			lb_ReadTime.setText("Time:" + readTime / 1 + "S");
		}
		if (lb_ReadSpeed != null) { // Refresh Read Speed
			speed = totalReadCount - lastReadCount;
			if (speed < 0)
				speed = 0;
			lastReadCount = totalReadCount;
			if (lb_ReadSpeed != null) {
				lb_ReadSpeed.setText("SP:" + speed + "T/S");
			}
		}
		if (lb_TagCount != null) { // Refresh total count
			lb_TagCount.setText("Total:" + hmList.size());
		}

		if (lb_WeightVal != null) { // Refresh total count
			lb_WeightVal.setText("Weight:" + weightStr);
		}

	}

	// Obtaining updated data sources
	@SuppressWarnings({ "rawtypes", "unused" })
	protected List<Map<String, Object>> GetData() {
		List<Map<String, Object>> rt = new ArrayList<Map<String, Object>>();
		synchronized (hmList_Lock) {
			// if(hmList.size() > 0){ //
			Iterator iter = hmList.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
				Tag_Model val = (Tag_Model) entry.getValue();
				Map<String, Object> map = new HashMap<String, Object>();
				if (_ReadType == 0) {
					map.put("EPC", val._EPC);
					map.put("ReadCount", val._TotalCount);
					rt.add(map);
				} else if (_ReadType == 1) {
					if(!val._TID.equals("")) {
						map.put("EPC", val._TID);
						map.put("ReadCount", val._TotalCount);
						rt.add(map);
					}
				} else {
					if(!val._UserData.equals("")) {
						map.put("EPC", val._UserData);
						map.put("ReadCount", val._TotalCount);
						rt.add(map);
					}
				}
			}
			// }
		}
		return rt;
	}

	// Saving data sources
	@SuppressWarnings({ "rawtypes", "unused" })
	protected boolean SaveData() {
		if (!bSaveResult) {
			return false;
		}

		//List<Map<String, Object>> rt = new ArrayList<Map<String, Object>>();
		synchronized (hmList_Lock) {
			// if(hmList.size() > 0){ //
			Iterator iter = hmList.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
				Tag_Model val = (Tag_Model) entry.getValue();
				//Map<String, Object> map = new HashMap<String, Object>();

				if (_ReadType == 0) {
					write(val._EPC+",");
				} else if (_ReadType == 1) {
					write(val._TID+",");
				} else if (_ReadType == 3) {
					write(val._EPC+",");
				} else if (_ReadType == 4) {
					write(val._EPC+",");
				} else {
					write(val._UserData+",");
				}

				if (_ReadType == 3) {
//						float temp = UHFReader._Tag6C.EM_ConvertTemperature(val);
//						write(temp+"\r\n");
				} else if (_ReadType == 4) {
//						float temp = UHFReader._Tag6C.RFMicron_ConvertTemperature(val);
//						write(temp+"\r\n");
				} else {
					write(val._TotalCount+"\r\n");
				}

				//rt.add(map);
			}
		}
		//return rt;
		return true;
	}

	private void write(String content) {
		try {
			// Open the file output stream as an append
			FileOutputStream fileOut  = new FileOutputStream(FILE_NAME,true);
			// Write data
			fileOut.write(content.getBytes());
			// Close file output stream
			fileOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	String weightStr = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// create
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		this.setContentView(R.layout.uhf_read);

		showCustomBar(getString(R.string.tv_Read_Title),
				getString(R.string.str_back), null,
				R.drawable.left, 0,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Back(v);
					}
				},
				null
		);
		Bundle b = getIntent().getExtras();
		 // or other values
		if(b != null)
			weightStr = b.getString("weightStr");
	}

	@Override
	protected void onDestroy() {
		// release
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// standby lock screen
		Dispose();
		super.onPause();
	}

	@Override
	protected void onResume() {
		// Standby recovery
		super.onResume();
		Init();
	}

	@Override
	public void onBackPressed() {
		Back(null);
	}

	@Override
	public void OutPutTags(Tag_Model model) {
		try {
			synchronized (hmList_Lock) {
				if (hmList.containsKey(model._EPC + model._TID)) {
					Tag_Model tModel = hmList.get(model._EPC + model._TID);
					tModel._TotalCount++;
					model._TotalCount = tModel._TotalCount;
					hmList.remove(model._EPC + model._TID);
					hmList.put(model._EPC + model._TID, model);
				} else {
					model._TotalCount = 1;
					hmList.put(model._EPC + model._TID, model);
				}
			}
			synchronized (beep_Lock) {
				beep_Lock.notify();
			}
			totalReadCount++;
		} catch (Exception ex) {
			Log.d("Debug", "Tags output exceptions:" + ex.getMessage());
		}
	}

	@Override
	public void WriteDebugMsg(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void WriteLog(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void PortConnecting(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void PortClosing(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OutPutTagsOver() {
		// TODO Auto-generated method stub

	}

	@Override
	public void GPIControlMsg(int i, int j, int k) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OutPutScanData(byte[] scandata) {
		// TODO Auto-generated method stub
	}
}
