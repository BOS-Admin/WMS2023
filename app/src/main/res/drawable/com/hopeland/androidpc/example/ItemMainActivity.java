package com.hopeland.androidpc.example;

import java.util.List;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.GridView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.hc.mixthebluetooth.R;
import com.hopeland.androidpc.example.uhf.ReadEPCActivity;
import com.hopeland.androidpc.example.uhf.UHFBaseActivity;
import com.hopeland.androidpc.example.uhf.UHFMain;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import java.util.ArrayList;
import java.util.HashMap;
/**
 * 
 * @author RFID_C Demo main menu
 */
public class ItemMainActivity extends UHFBaseActivity  implements
		IAsynchronousMessage {
	GridView gridView;
	//Connecting pop-up boxes
	Spinner connectType = null;
	long connectTypeIndex=0; //Index of the selected connection type
	EditText connectParam = null;
	Spinner bt4ConnectParam = null;
	String[] bt4DeviceName = null; //Bluetooth device name
	String connparam="";//connection parameter

	ArrayList<HashMap<String ,Object>> listItemArrayList=new ArrayList<HashMap<String,Object>>();
	int listIndex = 0;//Icon Index

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// create
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.item_main);

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		gridView = (GridView) findViewById(R.id.main_item_grid);
		ChangeLayout(getResources().getConfiguration());
		try {
			getWindow().setFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} catch (Exception ex) {
			Log.d("Debug", "The initialization of abnormal:" + ex.getMessage());
		}

		listItemArrayList.clear();
			onLogin();
		if(RFIDReader.isSupportBluetooth()){
//						startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
			AddBluetoothName();
		}
			if(true) {
				new Thread() {
					@Override
					public void run() {
						sendMessage(MSG_SHOW_WAIT,
								getString(R.string.str_please_waitting));
						String vStr = "Note:";
						boolean isConnectOK = false;
						if(InitConnect(3,"BTR-80021070009")) //connect reader
						{
							vStr = getString(R.string.str_LoginSuccess);
							isConnectOK = true;
						}
						else{
							vStr = getString(R.string.str_LoginFaild);
						}
						sendMessage(MSG_SHOW_TIP, vStr);
						sendMessage(MSG_HIDE_WAIT, null);
						//
						if(isConnectOK) {

								Intent intentA = new Intent();
								intentA.setClass(ItemMainActivity.this, ReadEPCActivity.class);
								startActivity(intentA);
							}
					};
				}.start();
				return;
			}
//		if (true) { //UHF
//			HashMap<String, Object> map = new HashMap<String,Object>();
//			map.put("itemImage", R.drawable.uhf);
//			map.put("itemText", getString(R.string.btn_MainMenu_UHF));
//			map.put("itemActivity", UHFMain.class);
//			listItemArrayList.add(map);
//		}

//		if (true) { //HF
//			HashMap<String, Object> map=new HashMap<String,Object>();
//			map.put("itemImage", R.drawable.hf);
//			map.put("itemText", getString(R.string.btn_MainMenu_HF));
//			map.put("itemActivity", HFMain.class);
//			listItemArrayList.add(map);
//		}
//
//		if (true) { // version
//			HashMap<String, Object> map = new HashMap<String,Object>();
//			map.put("itemImage", R.drawable.version);
//			map.put("itemText", getString(R.string.btn_MainMenu_Version));
//			map.put("itemActivity", null);
//			listItemArrayList.add(map);
//		}
//		if (true) { // serial number
//			HashMap<String, Object> map=new HashMap<String,Object>();
//			map.put("itemImage", R.drawable.serialno);
//			map.put("itemText", getString(R.string.btn_MainMenu_SerialNumber));
//			map.put("itemActivity", null);
//			listItemArrayList.add(map);
//		}


		//Generate an adapter ImageItem corresponding to the elements of a dynamic array.
		SimpleAdapter saImageItems = new SimpleAdapter(this,
				listItemArrayList,//Data sources
				R.layout.grid_item,// XML of item

				//The child of the dynamic array corresponding to ImageItem.
				new String[]{"itemImage", "itemText"},

				//An ImageView,TextView ID in the XML file of the ImageItem.
				new int[]{R.id.grid_item_image, R.id.grid_item_txt});
		//Add and display
		gridView.setAdapter(saImageItems);
		//Add Message Handling
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//Toast.makeText(getActivity(),name[position],Toast.LENGTH_LONG).show();
				if (isFastClick()) {
					return;
				}
                listIndex = position;
				HashMap<String, Object> map = listItemArrayList.get(listIndex);
				if (getString(R.string.btn_MainMenu_Version).equals(map.get("itemText"))) {
					GetVersion();
					return;
				}
				//For other options, a connection must be established first.
				if(!_UHFSTATE)
				{
					onLogin	();
					return;
				}
				//Check the serial number
				if (map.get("itemText").equals(getString(R.string.btn_MainMenu_SerialNumber))) {
					GetSerialNumber();
					return;
				}
				Intent intent = new Intent();
				intent.setClass(ItemMainActivity.this, (Class<?>) map.get("itemActivity"));
				startActivity(intent);
			}
		});

		showCustomBar(getString(R.string.tv_MainMenu_Title),
				getString(R.string.str_exit), null,
				R.drawable.left, 0,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Exit(v);
					}
				},
				null
		);
	}

	private void ChangeLayout(Configuration newConfig) {
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) { //The current screen is landscape
			gridView.setNumColumns(4);
		} else { // The current screen is vertical
			gridView.setNumColumns(3);
		}
	}

	public boolean ConnectRFID(String DeviceName){
		List<String> listName = RFIDReader.GetBT4DeviceStrList();
		if(listName.size()>0){
			bt4DeviceName = listName.toArray(new String[listName.size()]);
			ArrayAdapter<String> bt4Adapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, bt4DeviceName);
			//Adding an adapter to m_Spinner
			bt4ConnectParam.setAdapter(bt4Adapter);
		}
		return InitConnect(3,DeviceName);
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ChangeLayout(newConfig);
	}

	//connection dialog
	protected void onLogin()
	{
		// Create dialog box builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// Get the layout
		View view2 = View.inflate(ItemMainActivity.this, R.layout.connect_login, null);
		// Get the controls in the layout
		connectType = (Spinner) view2.findViewById(R.id.sp_connectType);
		connectParam = (EditText) view2.findViewById(R.id.connectParam);
		connectParam.setVisibility(View.VISIBLE);
		bt4ConnectParam =(Spinner) view2.findViewById(R.id.sp_bt4ConnectParam);
		bt4ConnectParam.setVisibility(View.GONE);
		final Button btnLogin= (Button) view2.findViewById(R.id.btn_ConnectReader);
        final Button btnSearch= (Button) view2.findViewById(R.id.btn_BluetoothSearch);
        btnSearch.setVisibility(View.GONE);
        // Set parameters
		builder.setTitle(getString(R.string.str_Login)).setIcon(R.drawable.ic_launcher)
				.setView(view2);//getString(R.string.str_Login)
		// Create Dialog Box
		final AlertDialog alertDialog = builder.create();
		btnLogin.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				connectTypeIndex = connectType.getSelectedItemId();
				connparam = connectParam.getText().toString().trim();
				if(connectTypeIndex ==3)
				{
					if(bt4DeviceName==null)
					{
						Toast toast = Toast.makeText(getApplicationContext(), "No connected Bluetooth", Toast.LENGTH_LONG);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
						return;
					}
					long index = bt4ConnectParam.getSelectedItemId();
					connparam = bt4DeviceName[(int)index];
				}
				//
				new Thread() {
					@Override
					public void run() {
						sendMessage(MSG_SHOW_WAIT,
								getString(R.string.str_please_waitting));
						String vStr = "Note:";
						boolean isConnectOK = false;
						if(InitConnect(connectTypeIndex,connparam)) //connect reader
						{
							vStr = getString(R.string.str_LoginSuccess);
							isConnectOK = true;
						}
						else{
							vStr = getString(R.string.str_LoginFaild);
						}
						sendMessage(MSG_SHOW_TIP, vStr);
						sendMessage(MSG_HIDE_WAIT, null);
                        //
						if(isConnectOK) {
							HashMap<String, Object> map = listItemArrayList.get(listIndex);
                            if (getString(R.string.btn_MainMenu_Version).equals(map.get("itemText"))) {
                                GetVersion();
                                return;
                            } else if (map.get("itemText").equals(getString(R.string.btn_MainMenu_SerialNumber))) {
                                GetSerialNumber();
                                return;
                            }
                            else {
                                Intent intentA = new Intent();
                                intentA.setClass(ItemMainActivity.this, (Class<?>) map.get("itemActivity"));
                                startActivity(intentA);
                            }
						}
					};
				}.start();
				alertDialog.dismiss();// The dialog box disappears

			}
		});
		alertDialog.show();
		//Default serial port connection parameter "/dev/ttySAC1:115200"; default tcp connection parameter "192.168.1.116:9090".
		connectType.setOnItemSelectedListener(new OnItemSelectedListener() {//Select the item's click to listen for events
			public void onItemSelected(AdapterView<?> arg0, View arg1,
									   int arg2, long arg3) {
				// TODO Auto-generated method stub
				connectTypeIndex = connectType.getSelectedItemId();
				if(connectTypeIndex==0)
				{
					connectParam.setText(PublicData.serialParam);
				}
				else if(connectTypeIndex==1)
				{
					connectParam.setText(PublicData.tcpParam);
				}
				else if(connectTypeIndex==2)
				{
					int usbNum = GetUsbDeviceList();
					if(usbNum>0)
					{
						//The first USB connection parameter is displayed by default.
						connectParam.setText(PublicData.usbListStr.get(0));
					}
				}
				else if(connectTypeIndex==3)
				{
					bt4DeviceName = null;
					btnSearch.setVisibility(View.VISIBLE);
					if(RFIDReader.isSupportBluetooth()){
//						startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                        AddBluetoothName();
					}
					else{
						sendMessage(MSG_SHOW_TIP, "The device does not support bluetooth!");
					}
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
			}
		});

        //Bluetooth search
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(RFIDReader.isSupportBluetooth()){
                    startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                }
                else{
                    sendMessage(MSG_SHOW_TIP, "The device does not support bluetooth!");
                }
            }
        });
	}

	//initialize
	protected boolean InitConnect(long index,String connparam)
	{
		boolean ret=false;
		if(index==0)
		{
			if(Rfid_RS232_Init(connparam,this))
			{
				ret=true;
			}
		}
		else if(index==1)
		{
			if(Rfid_Tcp_Init(connparam,this))
			{
				ret=true;
			}
		}
		else if(index==2)
		{
			if(Rfid_Usb_Init(connparam,this))
			{
				ret=true;
			}
		}
		else if(index==3)
		{
			if(Rfid_BT4_Init(connparam,this))
			{
				ret=true;
			}
		}
		return ret;
	}

    //Adding a Bluetooth Name
    private void AddBluetoothName()
    {
        connectParam.setVisibility(View.GONE);
        bt4ConnectParam.setVisibility(View.VISIBLE);
        List<String> listName = RFIDReader.GetBT4DeviceStrList();
        if(listName.size()>0){
            bt4DeviceName = listName.toArray(new String[listName.size()]);
            ArrayAdapter<String> bt4Adapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, bt4DeviceName);
            //Adding an adapter to m_Spinner
            bt4ConnectParam.setAdapter(bt4Adapter);
        }
    }

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if((!_UHFSTATE)&&(connectTypeIndex == 3)) //Not connected and the connection method is Bluetooth
		{
            AddBluetoothName();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		UHF_Dispose();
		System.exit(0);
	}
    /**
     *  Get version
     */
	public void GetVersion() {
	    try {
            String sdkVersion = RFIDReader.GetVER();
            showMsg("APP:" + getVersion() + "\n" + "SDK:" + sdkVersion + "\n", null);
        }
        catch (Exception ex){}
	}

	/**
	 * Get reader serial number
	 */
	public void GetSerialNumber() {
		try {
			String serial = RFIDReader.GetSerialNum(ConnID);
			showMsg(serial, null);
		}
		catch (Exception ex){}
	}

	/**
	 * exit the app
	 */
	public void Exit(View v) {
        UHF_Dispose();
		ItemMainActivity.this.finish();
		System.exit(0);
	}


	@Override
	public void GPIControlMsg(int arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OutPutTags(Tag_Model arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OutPutTagsOver() {
		// TODO Auto-generated method stub

	}

	@Override
	public void PortClosing(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void PortConnecting(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void WriteDebugMsg(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void WriteLog(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OutPutScanData(byte[] scandata) {
		// TODO Auto-generated method stub
		//System.out.println(msg);
	}

}
