package com.hopeland.androidpc.example.uhf;

import com.hc.mixthebluetooth.R;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import android.content.Intent;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/**
 * @author RFID_C main interface
 */
public class UHFMain extends UHFBaseActivity implements IAsynchronousMessage {

	private final int MSG_ENTER_READ = MSG_USER_BEG + 1;
	//private final int MSG_ENTER_UPGRADE = MSG_USER_BEG + 2;
	private final int MSG_ENTER_WRITE = MSG_USER_BEG + 3;
	private final int MSG_ENTER_CONFIG = MSG_USER_BEG + 4;
	private final int MSG_SHOW_UHF_VER = MSG_USER_BEG + 5;
	//private final int MSG_ENTER_LOCK = MSG_USER_BEG + 6;

	@Override
	protected void msgProcess(Message msg) {
		Intent intent;
		switch (msg.what) {
		case MSG_ENTER_READ:
			intent = new Intent();
			intent.setClass(UHFMain.this, ReadEPCActivity.class);
			startActivity(intent);
			break;
		case MSG_ENTER_WRITE:
			intent = new Intent();
			intent.setClass(UHFMain.this, WriteEPCActivity.class);
			startActivity(intent);
			break;
		case MSG_ENTER_CONFIG:
			intent = new Intent();
			intent.setClass(UHFMain.this, ConfigActivity.class);
			startActivity(intent);
			break;
		case MSG_SHOW_UHF_VER:
			if (msg.obj != null)
				showTip((String) msg.obj);
			break;
		default:
			break;
		}
		super.msgProcess(msg);
	}

	// Open read page
	public void OpenReadActivity(View v) {
		if (isFastClick()) {
			return;
		}

		new Thread() {
			@Override
			public void run() {
				sendMessage(MSG_SHOW_WAIT, getString(R.string.str_loading));
				sendMessage(MSG_ENTER_READ, null);
			};
		}.start();
	}

	// Open write page
	public void OpenWriteActivity(View v) {
		if (isFastClick()) {
			return;
		}
		new Thread() {
			@Override
			public void run() {
				sendMessage(MSG_SHOW_WAIT, getString(R.string.str_loading));
				sendMessage(MSG_ENTER_WRITE, null);
			};
		}.start();
	}

	// open configuration page
	public void OpenConfigActivity(View v) {
		if (isFastClick()) {
			return;
		}

		new Thread() {
			@Override
			public void run() {
				sendMessage(MSG_SHOW_WAIT, getString(R.string.str_loading));
				sendMessage(MSG_ENTER_CONFIG, null);
			};
		}.start();
	}

	// get version
	public void GetVersion(View v) {
		if (isFastClick()) {
			return;
		}
		
		//
		new Thread() {
			@Override
			public void run() {
				sendMessage(MSG_SHOW_WAIT,
						getString(R.string.str_please_waitting));
				String vStr = "- BaseBand: ";
				try {
					vStr += RFIDReader._Config.GetReaderBaseBandSoftVersion(ConnID);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					vStr += "null";
				}
				sendMessage(MSG_SHOW_UHF_VER, vStr);
				sendMessage(MSG_HIDE_WAIT, null);
			};
		}.start();

	}

	// exit
	public void BackIndex(View v) {
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// create
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.uhf_main);
		try {
			getWindow().setFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} catch (Exception ex) {
			Log.d("Debug", "The initialization of abnormal:" + ex.getMessage());
		}

		showCustomBar(getString(R.string.btn_MainMenu_UHF),
				getString(R.string.str_back), null,
				R.drawable.left, 0,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						BackIndex(v);
					}
				},
				null
		);
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
		sendMessage(MSG_HIDE_WAIT, null);
	}

	@Override
	protected void onResume() {
		// Standby recovery
		super.onResume();
	}

	@Override
	public void OutPutTags(Tag_Model model) {

	}

	@Override
	public void GPIControlMsg(int i, int j, int k) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OutPutTagsOver() {
		// TODO Auto-generated method stub

	}

	@Override
	public void PortClosing(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void PortConnecting(String s) {
		// TODO Auto-generated method stub

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
	public void OutPutScanData(byte[] scandata) {
		// TODO Auto-generated method stub
		//System.out.println(msg);
	}

}
