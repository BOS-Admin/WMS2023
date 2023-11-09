package com.hc.mixthebluetooth.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.recyclerAdapterBasic.FastScrollLinearLayoutManager;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BasFragment;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.activity.CommunicationActivity;
import com.hc.mixthebluetooth.activity.single.FragmentParameter;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.recyclerData.FragmentMessAdapter;
import com.hc.mixthebluetooth.recyclerData.itemHolder.FragmentMessageItem;
import com.hc.mixthebluetooth.storage.Storage;

import java.util.ArrayList;
import java.util.List;

public class FragmentMessage extends BasFragment {


    @ViewById(R.id.recycler_message_fragment)
    private RecyclerView mRecyclerView;




    private DefaultNavigationBar mTitle;//activity的头部

    private Runnable mRunnable;//循环发送的线程

    private Handler mHandler;

    private FragmentMessAdapter mAdapter;

    private List<FragmentMessageItem> mDataList = new ArrayList<>();

    private DeviceModule module;

    private Storage mStorage;

    private FragmentParameter mFragmentParameter;

    private int mUnsentNumber = 0,mCacheByteNumber = 0;//mUnsentNumber: 等待发送的剩余字节数；mCacheByteNumber: 缓存的字节数

    private boolean isShowMyData,isSendHex,isShowTime,isReadHex,isAutoClear;//弹出窗的五个选择

    private int mFoldLayoutHeight = 0;

    @Override
    public int setFragmentViewId() {
        return R.layout.fragment_message;
    }

    @Override
    public void initAll() {
        initRecycler();
    }

    @Override
    public void initAll(View view, Context context) {
        mStorage = new Storage(context);
        mFragmentParameter = FragmentParameter.getInstance();
        setListState();
        super.initAll(view, context);
    }

    @Override
    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    @Override
    public void updateState(int state) {


    }


    @SuppressLint("SetTextI18n")
    @Override
    public String readData(int state,Object o, final byte[] data) {
        switch (state){
            case CommunicationActivity.FRAGMENT_STATE_DATA:
                if (module == null) {
                    module = (DeviceModule) o;
                }
                if (data != null) {
                    mDataList.clear();
                    if(mFragmentParameter==null){
                        mFragmentParameter=FragmentParameter.getInstance();

                    }
                    String msg=Analysis.getByteToString(data,mFragmentParameter.getCodeFormat(getContext()),false);
                    String[] msgs=msg.split("=",10);
                    toast(msgs[0]);
//                    mDataList.add(new FragmentMessageItem(Analysis.getByteToString(data,mFragmentParameter.getCodeFormat(getContext()),isReadHex), isShowTime?Analysis.getTime():null, false, module,isShowMyData));
                    mDataList.add(new FragmentMessageItem(msgs[0], isShowTime?Analysis.getTime():null, false, module,isShowMyData));
//                    mAdapter.notifyDataSetChanged();
//                    mRecyclerView.smoothScrollToPosition(mDataList.size());

                    setClearRecycler(data.length);//判断是否清屏（清除缓存）
                    return  msgs[0];
                }
                break;
            case CommunicationActivity.FRAGMENT_STATE_NUMBER:
                 setUnsentNumberTv();
                break;
            case CommunicationActivity.FRAGMENT_STATE_SEND_SEND_TITLE:
                mTitle = (DefaultNavigationBar) o;
                break;
            case CommunicationActivity.FRAGMENT_STATE_SERVICE_VELOCITY:
                int velocity = (int) o;
                 break;
        }
return  "";
    }


    //弹出提示框，警告AT指令设置无效
    private void dataScreening(String data) {
        String str = "AT+";
        if (data.length()<str.length())
            return;
        String temp = data.substring(0,str.length());

    }


    private void setListState() {


    }
private String mDataET;
    private void setUnsentNumberTv(){

    }

    private void setClearRecycler(int readNumber) {
        mCacheByteNumber += readNumber;
        if (isAutoClear){//开启清除缓存
            if (mCacheByteNumber>400000){//只缓存400K
                mDataList.clear();
                mAdapter.notifyDataSetChanged();
                mCacheByteNumber = 0;
            }
        }
    }

    private void sendData(FragmentMessageItem item) {
        if (mHandler == null)
            return;
        Message message = mHandler.obtainMessage();
        message.what = CommunicationActivity.DATA_TO_MODULE;
        message.obj = item;
        mHandler.sendMessage(message);
        if (isShowMyData) {
            mDataList.add(item);
            mAdapter.notifyDataSetChanged();
            mRecyclerView.smoothScrollToPosition(mDataList.size());
        }

        //发送完计数
        int number = Analysis.changeHexString(true, "".toString().replaceAll(" ", "")).length()/3;
        if (isSendHex)
            number = number%2 == 0?number/2:(number+1)/2;
        mUnsentNumber += number;
    }

    private void initRecycler(){
        mAdapter = new FragmentMessAdapter(getContext(),mDataList,R.layout.item_message_fragment);
        mRecyclerView.setLayoutManager(new FastScrollLinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);
    }



}