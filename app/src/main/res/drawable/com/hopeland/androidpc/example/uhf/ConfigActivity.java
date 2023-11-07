package com.hopeland.androidpc.example.uhf;

import java.util.*;


import android.annotation.SuppressLint;

import com.hopeland.androidpc.example.PublicData;
import com.hc.mixthebluetooth.R;;
import com.rfidread.Enumeration.eRF_Range;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.content.res.Configuration;

/**
 * @author RFID_lx configuration
 */
public class ConfigActivity extends UHFBaseActivity implements
		IAsynchronousMessage {

	private Spinner sp_Configration_Power = null;
	//	private Spinner sp_PingPongReadTime = null;
//	private Spinner sp_PingPongStopTime = null;
	private Spinner sp_Frequency = null;
	private Spinner sp_BaseSpeedType = null;
	private Spinner sp_BaseSpeedQValue = null;
	private Spinner sp_Carrier = null;
	private Spinner sp_TagType = null;


	private boolean usingBackBattery = false;

	private ArrayAdapter<String> adapter = null;
	private String[] powers_G = { "0", "1","2", "3", "4", "5","6", "7", "8", "9","10", "11", "12", "13","14", "15", "16", "17","18", "19", "20", "21","22", "23", "24", "25", "26","27", "28", "29", "30", "31", "32", "33" };

	//Hide the LinearLayout area.
	private boolean isVisible = true;
	private LinearLayout layout_1;

	private  Button btn_Configration_Open;
	private CheckBox cb01= null;
	private CheckBox cb02= null;
	private Spinner sp_Configration_Power2 = null;
	private CheckBox cb03= null;
	private Spinner sp_Configration_Power3 = null;
	private CheckBox cb04= null;
	private Spinner sp_Configration_Power4 = null;
	private CheckBox cb05= null;
	private Spinner sp_Configration_Power5 = null;
	private CheckBox cb06= null;
	private Spinner sp_Configration_Power6 = null;
	private CheckBox cb07= null;
	private Spinner sp_Configration_Power7 = null;
	private CheckBox cb08= null;
	private Spinner sp_Configration_Power8 = null;

	//
	private CheckBox cb11= null;
	private CheckBox cb12= null;
	private CheckBox cb13= null;
	private CheckBox cb14= null;
	private CheckBox cb15= null;
	private CheckBox cb16= null;
	private CheckBox cb17= null;
	private CheckBox cb18= null;

	@SuppressLint("UseSparseArrays")
	@SuppressWarnings("serial")
	private HashMap<Integer, Integer> mm_PingPong = new HashMap<Integer, Integer>() {
		{
			put(0, 0);
			put(100, 1);
			put(200, 2);
			put(300, 3);
			put(500, 4);
			put(800, 5);
			put(1000, 6);
			put(5000, 7);
			put(10000, 8);
		}
	};

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void Init() {
		sp_Configration_Power = (Spinner) this
				.findViewById(R.id.sp_Configration_Power);
//			sp_PingPongReadTime = (Spinner) this
//					.findViewById(R.id.sp_PingPongReadTime);
//			sp_PingPongStopTime = (Spinner) this
//					.findViewById(R.id.sp_PingPongStopTime);
		sp_Frequency = (Spinner) this.findViewById(R.id.sp_Frequency);
		sp_BaseSpeedType = (Spinner) this
				.findViewById(R.id.sp_BaseSpeedType);
		sp_BaseSpeedQValue = (Spinner) this
				.findViewById(R.id.sp_BaseSpeedQValue);
		sp_Carrier = (Spinner) this.findViewById(R.id.sp_Carrier);
		sp_TagType = (Spinner) this.findViewById(R.id.sp_TagType1);
		//
		layout_1 = (LinearLayout) findViewById(R.id.linearLayout_id);
		layout_1.setVisibility(View.GONE);//This sentence hides the LinearLayout area of the layout.
		//
		btn_Configration_Open = (Button) findViewById(R.id.btn_Configration_Open);
		cb01 = (CheckBox) findViewById(R.id.cb01);
		cb02 = (CheckBox) findViewById(R.id.cb02);
		sp_Configration_Power2 = (Spinner) this.findViewById(R.id.sp_Configration_Power2);
		cb03 = (CheckBox) findViewById(R.id.cb03);
		sp_Configration_Power3 = (Spinner) this.findViewById(R.id.sp_Configration_Power3);
		cb04 = (CheckBox) findViewById(R.id.cb04);
		sp_Configration_Power4 = (Spinner) this.findViewById(R.id.sp_Configration_Power4);
		cb05 = (CheckBox) findViewById(R.id.cb05);
		sp_Configration_Power5 = (Spinner) this.findViewById(R.id.sp_Configration_Power5);
		cb06 = (CheckBox) findViewById(R.id.cb06);
		sp_Configration_Power6 = (Spinner) this.findViewById(R.id.sp_Configration_Power6);
		cb07 = (CheckBox) findViewById(R.id.cb07);
		sp_Configration_Power7 = (Spinner) this.findViewById(R.id.sp_Configration_Power7);
		cb08 = (CheckBox) findViewById(R.id.cb08);
		sp_Configration_Power8 = (Spinner) this.findViewById(R.id.sp_Configration_Power8);

		//
		cb11 = (CheckBox) findViewById(R.id.cb11);
		cb12 = (CheckBox) findViewById(R.id.cb12);
		cb13 = (CheckBox) findViewById(R.id.cb13);
		cb14 = (CheckBox) findViewById(R.id.cb14);
		cb15 = (CheckBox) findViewById(R.id.cb15);
		cb16 = (CheckBox) findViewById(R.id.cb16);
		cb17 = (CheckBox) findViewById(R.id.cb17);
		cb18 = (CheckBox) findViewById(R.id.cb18);


//			sp_PingPongReadTime.setSelection(mm_PingPong
//					.get(PublicData._PingPong_ReadTime));
//			sp_PingPongStopTime.setSelection(mm_PingPong
//					.get(PublicData._PingPong_StopTime));

		UHF_GetReaderProperty();
		GetBaseBand();// Get baseband

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Power value range
		if (_Max_Power > 0) {
			String[] powerValue = new String[_Max_Power - _Min_Power + 1];
			for (int i = 0; i < _Max_Power - _Min_Power + 1; i++) {
				powerValue[i] = (_Min_Power + i) + "";
			}
			adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, powerValue);
		} else {
			adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, powers_G);
		}
		sp_Configration_Power.setAdapter(adapter);
		sp_Configration_Power2.setAdapter(adapter);
		sp_Configration_Power3.setAdapter(adapter);
		sp_Configration_Power4.setAdapter(adapter);
		sp_Configration_Power5.setAdapter(adapter);
		sp_Configration_Power6.setAdapter(adapter);
		sp_Configration_Power7.setAdapter(adapter);
		sp_Configration_Power8.setAdapter(adapter);

		if (_ReaderAntennaCount == 1) {
			btn_Configration_Open.setEnabled(false);
			SetReaderAnt1();
		} else if (_ReaderAntennaCount == 2) {
			SetReaderAnt2();
		} else if (_ReaderAntennaCount == 4) {
			SetReaderAnt4();
		} else if (_ReaderAntennaCount >= 8) {
			SetReaderAnt8();
		}

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//GetPower(); // Get output power
		Button_GetFrequency(null); // get Frequency band

		//antenna
		if (_ReaderAntennaCount >= 1) {
			String[] antCountValue = new String[_ReaderAntennaCount];
			for (int i = 0; i < _ReaderAntennaCount; i++) {
				antCountValue[i] = (i + 1) + "";
			}
			adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, antCountValue);
			sp_Carrier.setAdapter(adapter);
		}
		//
		if (PublicData._IsCommand6Cor6B.equals("6C")) {
			sp_TagType.setSelection(0);
		} else {
			sp_TagType.setSelection(1);
		}
		//
		try {
			for (int antNo = 0; antNo < _ReaderAntennaCount; antNo++) {
				if ((_NowAntennaNo & (1 << antNo)) != 0)
					SwitchAnt(antNo + 1);
			}
		} catch (Exception ex) {
		}
		//
		RFIDReader.setAsynchronousMessage(ConnID,  this);
	}

	// get current power
	protected boolean GetPower() {
		boolean rt = false;
		String sPower  = RFIDReader._Config.GetANTPowerParam2(ConnID);

		//Log.d("GetPower:", sPower);
		String[] arrParam = sPower.split("\\&");
		if (arrParam != null && arrParam.length > 0) {
			for (String paramItem : arrParam) {
				String[] arrItem = paramItem.split(",");
				if (arrItem.length == 2) {
					if (arrItem[0].equals("1")) {
						if (sp_Configration_Power != null) {
							try{
								sp_Configration_Power.setSelection(Integer
										.parseInt(arrItem[1]) - _Min_Power);
							}
							catch(Exception ex)
							{
							}

						}
					}
					if (arrItem[0].equals("2")) {
						if (sp_Configration_Power2 != null) {
							try{
								sp_Configration_Power2.setSelection(Integer
										.parseInt(arrItem[1]) - _Min_Power);
							}
							catch(Exception ex)
							{
							}

						}
					}
					if (arrItem[0].equals("3")) {
						if (sp_Configration_Power3 != null) {
							try{
								sp_Configration_Power3.setSelection(Integer
										.parseInt(arrItem[1]) - _Min_Power);
							}
							catch(Exception ex)
							{
							}

						}
					}
					if (arrItem[0].equals("4")) {
						if (sp_Configration_Power4 != null) {
							try{
								sp_Configration_Power4.setSelection(Integer
										.parseInt(arrItem[1]) - _Min_Power);
							}
							catch(Exception ex)
							{
							}

						}
					}
					if (arrItem[0].equals("5")) {
						if (sp_Configration_Power5 != null) {
							try{
								sp_Configration_Power5.setSelection(Integer
										.parseInt(arrItem[1]) - _Min_Power);
							}
							catch(Exception ex)
							{
							}

						}
					}
					if (arrItem[0].equals("6")) {
						if (sp_Configration_Power6 != null) {
							try{
								sp_Configration_Power6.setSelection(Integer
										.parseInt(arrItem[1]) - _Min_Power);
							}
							catch(Exception ex)
							{
							}

						}
					}
					if (arrItem[0].equals("7")) {
						if (sp_Configration_Power7 != null) {
							try{
								sp_Configration_Power7.setSelection(Integer
										.parseInt(arrItem[1]) - _Min_Power);
							}
							catch(Exception ex)
							{
							}

						}
					}
					if (arrItem[0].equals("8")) {
						if (sp_Configration_Power8 != null) {
							try{
								sp_Configration_Power8.setSelection(Integer
										.parseInt(arrItem[1]) - _Min_Power);
							}
							catch(Exception ex)
							{
							}

						}
					}
				}
			}
			rt = true;
		}
		return rt;
	}

	// Return to main page
	public void Back(View v) {
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// create
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.uhf_configration);
		//ChangeLayout(getResources().getConfiguration());

		try {
			getWindow().setFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} catch (Exception ex) {
			Log.d("Debug", "The initialization of abnormal:" + ex.getMessage());
		}

		showCustomBar(getString(R.string.btn_UHFMenu_Configration),
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
	}


	private void ChangeLayout(android.content.res.Configuration newConfig) {
		LinearLayout main = (LinearLayout)findViewById(R.id.view_split_main);
		LinearLayout left = (LinearLayout)findViewById(R.id.view_split_left);
		LinearLayout right = (LinearLayout)findViewById(R.id.view_split_right);

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) { //The current screen is landscape
			main.setOrientation(LinearLayout.HORIZONTAL);

			LinearLayout.LayoutParams leftParam = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, (float) 0.5);
			LinearLayout.LayoutParams rightParam = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) 0.5);

			leftParam.setMargins(0, 0, 10, 0);
			rightParam.setMargins(10, 0, 0, 0);

			left.setLayoutParams(leftParam);
			right.setLayoutParams(rightParam);

		} else { // The current screen is vertical
			main.setOrientation(LinearLayout.VERTICAL);

			LinearLayout.LayoutParams leftParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			LinearLayout.LayoutParams rightParam = (new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

			leftParam.setMargins(0, 0, 0, 0);
			rightParam.setMargins(0, 0, 0, 0);

			left.setLayoutParams(leftParam);
			right.setLayoutParams(rightParam);
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ChangeLayout(newConfig);
	}



	@Override
	protected void onDestroy() {
		// release
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// standby lock screen
		super.onPause();
	}

	@Override
	protected void onResume() {
		// Standby recovery
		super.onResume();
		try {
			Init();
		} catch (Exception ex) {
		}
	}

	@Override
	public void OutPutTags(Tag_Model model) {

	}

	// ------------------------- button event -------------------------------


	public void SwitchAnt(int antStr)
	{
		switch(antStr)
		{
			case 1:
				cb11.setChecked(true);
				break;
			case 2:
				cb12.setChecked(true);
				break;
			case 3:
				cb13.setChecked(true);
				break;
			case 4:
				cb14.setChecked(true);
				break;
			case 5:
				cb15.setChecked(true);
				break;
			case 6:
				cb16.setChecked(true);
				break;
			case 7:
				cb17.setChecked(true);
				break;
			case 8:
				cb18.setChecked(true);
				break;
		}
	}


	//Set only #1 antenna
	public void SetReaderAnt1()
	{
		cb02.setEnabled(false);
		sp_Configration_Power2.setEnabled(false);
		cb03.setEnabled(false);
		sp_Configration_Power3.setEnabled(false);
		cb04.setEnabled(false);
		sp_Configration_Power4.setEnabled(false);
		cb05.setEnabled(false);
		sp_Configration_Power5.setEnabled(false);
		cb06.setEnabled(false);
		sp_Configration_Power6.setEnabled(false);
		cb07.setEnabled(false);
		sp_Configration_Power7.setEnabled(false);
		cb08.setEnabled(false);
		sp_Configration_Power8.setEnabled(false);
		//
		cb12.setEnabled(false);
		cb13.setEnabled(false);
		cb14.setEnabled(false);
		cb15.setEnabled(false);
		cb16.setEnabled(false);
		cb17.setEnabled(false);
		cb18.setEnabled(false);
	}

	//Set only #2 antenna
	public void SetReaderAnt2()
	{
		cb02.setEnabled(true);
		sp_Configration_Power2.setEnabled(true);
		cb03.setEnabled(false);
		sp_Configration_Power3.setEnabled(false);
		cb04.setEnabled(false);
		sp_Configration_Power4.setEnabled(false);
		cb05.setEnabled(false);
		sp_Configration_Power5.setEnabled(false);
		cb06.setEnabled(false);
		sp_Configration_Power6.setEnabled(false);
		cb07.setEnabled(false);
		sp_Configration_Power7.setEnabled(false);
		cb08.setEnabled(false);
		sp_Configration_Power8.setEnabled(false);
		//
		cb12.setEnabled(false);
		cb13.setEnabled(false);
		cb14.setEnabled(false);
		cb15.setEnabled(false);
		cb16.setEnabled(false);
		cb17.setEnabled(false);
		cb18.setEnabled(false);
	}



	//Set only #4 antenna
	public void SetReaderAnt4()
	{
		cb02.setChecked(true);
		cb03.setChecked(true);
		cb04.setChecked(true);
		cb05.setEnabled(false);
		sp_Configration_Power5.setEnabled(false);
		cb06.setEnabled(false);
		sp_Configration_Power6.setEnabled(false);
		cb07.setEnabled(false);
		sp_Configration_Power7.setEnabled(false);
		cb08.setEnabled(false);
		sp_Configration_Power8.setEnabled(false);
		//
		cb15.setEnabled(false);
		cb16.setEnabled(false);
		cb17.setEnabled(false);
		cb18.setEnabled(false);
	}

	//Set only #8 antenna
	public void SetReaderAnt8()
	{
		cb02.setChecked(true);
		cb03.setChecked(true);
		cb04.setChecked(true);
		cb05.setChecked(true);
		cb06.setChecked(true);
		cb07.setChecked(true);
		cb08.setChecked(true);
	}



	// open power
	public void Button_OpenPower(View v) {
		if (isFastClick()) {
			return;
		}
		if (isVisible) {
			isVisible = false;
			layout_1.setVisibility(View.VISIBLE);//This sentence shows the layout of the LinearLayout area.
			btn_Configration_Open.setText("︽ ");
		} else {
			layout_1.setVisibility(View.GONE);//This sentence hides the LinearLayout area of the layout.
			isVisible = true;
			btn_Configration_Open.setText("︾ ");
		}

	}

	//
	public void Button_ReadAnt_Set(View v) {
		if (isFastClick()) {
			return;
		}
		_NowAntennaNo = 0;
		if(cb11.isChecked())
		{
			_NowAntennaNo += 1;
		}
		if(cb12.isChecked())
		{
			_NowAntennaNo += 2;
		}
		if(cb13.isChecked())
		{
			_NowAntennaNo += 4;
		}
		if(cb14.isChecked())
		{
			_NowAntennaNo += 8;
		}
		if(cb15.isChecked())
		{
			_NowAntennaNo += 16;
		}
		if(cb16.isChecked())
		{
			_NowAntennaNo += 32;
		}
		if(cb17.isChecked())
		{
			_NowAntennaNo += 64;
		}
		if(cb18.isChecked())
		{
			_NowAntennaNo += 128;
		}

		showMsg(getString(R.string.str_success), null);
	}
	// Get power
	public void Button_GetPower(View v) {
		if (isFastClick()) {
			return;
		}
		if (GetPower()) {
			showMsg(getString(R.string.str_success), null);
		} else {
			showMsg(getString(R.string.str_faild), null);
		}

	}

	// Set power
	public void Button_SetPower(View v) {
		if (isFastClick()) {
			return;
		}
		try {
			HashMap<Integer, Integer> dicPower = new HashMap<>();

			if (cb01.isChecked()) {
				dicPower.put(1, _Min_Power + sp_Configration_Power.getSelectedItemPosition());
			}

			if (cb02.isChecked() && _NowAntennaNo >= 2) {
				dicPower.put(2, _Min_Power + sp_Configration_Power2.getSelectedItemPosition());
			}

			if (cb03.isChecked() && _NowAntennaNo >= 3) {
				dicPower.put(3, _Min_Power + sp_Configration_Power3.getSelectedItemPosition());
			}

			if (cb04.isChecked() && _NowAntennaNo >= 4) {
				dicPower.put(4, _Min_Power + sp_Configration_Power4.getSelectedItemPosition());
			}

			if (cb05.isChecked() && _NowAntennaNo >= 5) {
				dicPower.put(5, _Min_Power + sp_Configration_Power5.getSelectedItemPosition());
			}

			if (cb06.isChecked() && _NowAntennaNo >= 6) {
				dicPower.put(6, _Min_Power + sp_Configration_Power6.getSelectedItemPosition());
			}

			if (cb07.isChecked() && _NowAntennaNo >= 7) {
				dicPower.put(7, _Min_Power + sp_Configration_Power7.getSelectedItemPosition());
			}

			if (cb08.isChecked() && _NowAntennaNo >= 8) {
				dicPower.put(8, _Min_Power + sp_Configration_Power8.getSelectedItemPosition());
			}

			int rt = RFIDReader._Config.SetANTPowerParam(ConnID, dicPower);
			if (rt == 0) {
				showMsg(getString(R.string.str_success), null);
			} else {
				showMsg(getString(R.string.str_faild), null);
			}
		}catch (Exception ex){}
	}

	// Get frequency band
	public void Button_GetFrequency(View v) {
		try {
			Integer frequencyNo = RFIDReader._Config.GetReaderRF(ConnID).GetNum();
			sp_Frequency.setSelection(frequencyNo);

			if (GetPower()) { //check power and frequency at the same time
				if (v != null) {
					showMsg(getString(R.string.str_success), null);
				}
			} else {
				showMsg(getString(R.string.str_faild), null);
			}
		} catch (Exception ex) {
			if (v != null)
				showMsg(getString(R.string.str_faild), null);
		}


	}

	// Set frequency
	public void Button_SetFrequency(View v) {
		if (isFastClick()) {
			return;
		}
		int param = sp_Frequency.getSelectedItemPosition();
		if (RFIDReader._Config.SetReaderRF(ConnID, eRF_Range.GetEnum(param)) == 0) {

			Button_SetPower(v); //Set power and frequency at the same time
			showMsg(getString(R.string.str_success), null);
		} else {
			showMsg(getString(R.string.str_faild), null);
		}
	}

	//restore factory settings
	public void Button_Configration_RF(View v) {
		if (isFastClick()) {
			return;
		}
		if (RFIDReader._Config.SetReaderRestoreFactory(ConnID) == 0) {
			showMsg(getString(R.string.str_success), null);
		} else {
			showMsg(getString(R.string.str_faild), null);
		}
	}

	// read 6C or 6B tags
	public void Button_TagType_Set(View v) {
		if (isFastClick()) {
			return;
		}
		//
		PublicData._IsCommand6Cor6B = sp_TagType.getSelectedItem().toString();
		showMsg(getString(R.string.str_success), null);
	}



	//get baseband parameters
	public void Button_GetBaseBand(View v) {
		if (isFastClick()) {
			return;
		}
		GetBaseBand();
	}

	@SuppressWarnings("serial")
	private void GetBaseBand() {
		String rt = RFIDReader._Config.GetEPCBaseBandParam(ConnID);
		String[] strParam = rt.split("\\|");
		if (strParam.length > 0) {
			for (int i = 0; i < strParam.length; i++) {
				if (i == 0) {
					int iType = Integer.parseInt(strParam[i]);
					if (iType == 255)
						iType = 4;
					sp_BaseSpeedType.setSelection(iType);
				} else if (i == 1) {

					HashMap<String, Integer> mm = new HashMap<String, Integer>() {
						{
							put("4", 1);
							put("0", 0);
						}
					};
					if (mm.containsKey(strParam[i])) {
						sp_BaseSpeedQValue.setSelection(mm.get(strParam[i]));
					}
				}
			}
		}
	}

	// set baseband parameters
	public void Button_SetBaseBand(View v) {
		if (isFastClick()) {
			return;
		}
		String param = "";
		int typePosition = sp_BaseSpeedType.getSelectedItemPosition();
		if (typePosition == 4)
			typePosition = 255;
		int qValue = Integer.parseInt(sp_BaseSpeedQValue.getSelectedItem()
				.toString());
		if (RFIDReader._Config.SetEPCBaseBandParam(ConnID, typePosition, qValue, null, null) == 0) {
			showMsg(getString(R.string.str_success), null);
		} else {
			showMsg(getString(R.string.str_faild), null);
		}
	}

	public void Button_Test(View v) {
		if (isFastClick()) {
			return;
		}
		int antNo = 1<< sp_Carrier.getSelectedItemPosition();
		showMsg(RFIDReader._Config.GetAntennaStandingWaveRatio(ConnID, antNo), null);
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
