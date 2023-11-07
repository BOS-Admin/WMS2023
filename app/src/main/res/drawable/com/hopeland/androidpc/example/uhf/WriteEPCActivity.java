package com.hopeland.androidpc.example.uhf;

import java.util.regex.*;

import android.content.Context;
import android.content.res.Configuration;

import com.hopeland.androidpc.example.PublicData;
import com.hc.mixthebluetooth.R;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Helper.Helper_String;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

/**
 * @author RFID_lx write tag
 */
public class WriteEPCActivity extends UHFBaseActivity implements
		IAsynchronousMessage {

	private static int _WriteType = 0;
	private boolean in_reading = false;

	private EditText tb_Write_MatchTID = null;
	private EditText tb_Access_Password = null;
	private Spinner sp_Write_WriteType = null;
	private EditText tb_Write_WriteData = null;

	private final int MSG_MATCH_READ = MSG_USER_BEG + 1; // constant reading

	@Override
	protected void msgProcess(Message msg) {

		switch (msg.what) {
			case MSG_MATCH_READ:
				ShowMatchTID(msg.obj.toString()); // Refresh list
				break;

			default:
				super.msgProcess(msg);
				break;
		}
	}

	//display TID
	public void ShowMatchTID(String tid) {
		tb_Write_MatchTID.setText(tid);
	}

	//initialize
	protected void Init() {
		// initial object
		tb_Write_MatchTID = (EditText) findViewById(R.id.tb_Write_MatchTID);
		tb_Access_Password = (EditText) findViewById(R.id.tb_Access_Password);
		tb_Access_Password.setText("00000000");
		tb_Write_WriteData = (EditText) findViewById(R.id.tb_Write_WriteData);
		sp_Write_WriteType = (Spinner) findViewById(R.id.sp_Write_WriteType);

		// Write Data Type
		sp_Write_WriteType.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0,
									   View arg1, int arg2, long arg3) {

				int selectItem = sp_Write_WriteType
						.getSelectedItemPosition();
				if (selectItem == 0) {
					_WriteType = 0;
				} else if (selectItem == 1) {
					_WriteType = 1;
				}
				else if(selectItem == 2){
					_WriteType = 2;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
		//
		RFIDReader.HP_CONNECT.get(ConnID).myLog = this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO create
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.uhf_wirte);
		ChangeLayout(getResources().getConfiguration());

		try {
			getWindow().setFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} catch (Exception ex) {
			Log.d("Debug", "The initialization of abnormal:" + ex.getMessage());
		}

		showCustomBar(getString(R.string.btn_UHFMenu_Write),
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
		// TODO release
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO standby lock screen
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Standby recovery
		super.onResume();
		Init();
	}

	// Read the TID of the tag to be written.
	public void ReadMactchTag(View v) {

		sendMessage(MSG_MATCH_READ,"");
		in_reading = true;
		if (PublicData._IsCommand6Cor6B.equals("6C")) {// read 6c tag
			RFIDReader._Tag6C.GetEPC_TID(ConnID,_NowAntennaNo, eReadType.Inventory);
		} else { // 6B
			RFIDReader._Tag6B.Get6B(ConnID,_NowAntennaNo,1,0);
		}

		new Thread() {
			public void run() {
				int wait = 50;
				int timout = 2000;

				showWait(getString(R.string.waiting));
				try {
					for (int i=0; i<timout; i+=wait)
					{
						Thread.sleep(wait);
						if (!in_reading) {
							break;
						}
					}
					RFIDReader._Config.Stop(ConnID);
				} catch (Exception e) {
				}
				hideWait();
			};
		}.start();
	}

	// Writing tag data
	public void WirteData(View v) {
		int dataLen = 0; // Write data length
		String strInput = tb_Write_WriteData.getText().toString();
		if (!CheckInput(strInput)) { // Illegal input
			showMsg(getString(R.string.uhf_data_error), null);
			return;
		}
		if(_WriteType == 2) //access password of tag
		{
			if(tb_Access_Password.getText().length()!=8){
				showMsg(getString(R.string.uhf_data_len_error), null);
				return;
			}
		}
		dataLen = tb_Write_WriteData.getText().length() % 4 == 0 ? tb_Write_WriteData
				.getText().length() / 4
				: tb_Write_WriteData.getText().length() / 4 + 1;
		if (dataLen > 0) { // The length of the write data must be greater than 0.
			if (PublicData._IsCommand6Cor6B.equals("6C")) {// write 6c tag
				int ret = -1;
				if (_WriteType == 0) { // write EPC
					ret = RFIDReader._Tag6C.WriteEPC_MatchTID(ConnID,_NowAntennaNo,tb_Write_WriteData.getText().toString(),tb_Write_MatchTID.getText().toString(),0, tb_Access_Password.getText().toString());
				} else if (_WriteType == 1) { // write UserData
					ret = RFIDReader._Tag6C.WriteUserData_MatchTID(ConnID,_NowAntennaNo,tb_Write_WriteData.getText().toString(),0,tb_Write_MatchTID.getText().toString(),0, tb_Access_Password.getText().toString());
				}
				else if(_WriteType == 2){//write access password
					ret = RFIDReader._Tag6C.WriteAccessPassWord_MatchTID(ConnID,_NowAntennaNo,tb_Write_WriteData.getText().toString(),tb_Write_MatchTID.getText().toString(),
							0,tb_Access_Password.getText().toString());
				}
				if (ret == 0) {
					showMsg(getString(R.string.str_success), null);
				} else {
					showMsg(getString(R.string.str_faild), null);
				}
			} else { // write 6b tag
				Write6B(tb_Write_WriteData.getText().toString());
			}
		} else {
			showMsg(getString(R.string.uhf_data_error), null);
		}

	}

	// write 6B
	public void Write6B(String sEPC) {
		if (RFIDReader._Tag6B.Write6B(ConnID, _NowAntennaNo, tb_Write_MatchTID.getText().toString(), 8, sEPC) == 0) {
			showMsg(getString(R.string.str_success), null);
		} else {
			showMsg(getString(R.string.str_faild), null);
		}
	}

	public boolean CheckInput(String strInput) {
		boolean rt = false;
		Pattern p = Pattern.compile("^[a-f,A-F,0-9]*$");
		Matcher m = p.matcher(strInput);
		rt = m.matches();
		return rt;
	}

	// Return to main page
	public void Back(View v) {
		this.finish();
	}

	@Override
	public void OutPutTags(Tag_Model model) {
		if (in_reading) {
			sendMessage(MSG_MATCH_READ, model._TID);
			in_reading = false;
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
