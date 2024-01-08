package com.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class StationSocket {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    public String ErrorMsg = "";

   public ArrayList<String> Messages=new ArrayList<>();
   public ArrayList<String> RecMessages=new ArrayList<>();
   public ArrayList<Date> TimeSent=new ArrayList<>();
   public ArrayList<Date>TimeReceived=new ArrayList<>();

    public StationSocket(){

    }

    public void startConnection(String ip, int port) throws IOException {

            clientSocket = new Socket(ip, port);
            clientSocket.setSoTimeout(3000);


        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (Exception e) {

            ErrorMsg = "Error Registering Streams "+e.getMessage();

        }


    }

    public String sendMessage(String msg) throws Exception {
        try{
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            byte[] SentBytes;
            SentBytes=stringToByteArrayFastest(msg);
            out.write(SentBytes);
            byte[] recBytes= new byte[100];
            int  recLen = in.read(recBytes);
            return receiveBytes(recBytes,recLen);
        }catch (Exception e){
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return e.getMessage()+"\n"+sw.toString();
        }


    }


    public void stopConnection() throws IOException {
        if(in!=null)
            in.close();
        if(out!=null)
            out.close();
        if(clientSocket!=null)
            clientSocket.close();
    }

    public static byte[] stringToByteArrayFastest(String hex) throws Exception {
        char[] hexCh = hex.toCharArray();
        if (hex.length() % 2 == 1)
            throw new Exception("The binary key cannot have an odd number of digits");
        byte[] arr = new byte[hex.length() >> 1];
        for (int i = 0; i < hex.length()>>1; ++i) {
            arr[i] = (byte) ((GetHexVal(hexCh[i << 1]) << 4) + (GetHexVal(hexCh[(i << 1) + 1])));
        }
        return arr;

    }

    public static int GetHexVal(char hex) {
        int val = (int) hex;
        return val - (val < 58 ? 48 : (val < 97 ? 55 : 87));
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }




    public String receiveBytes(byte[] resultBuff,int l){
        byte[] tbuff = new byte[l]; // temp buffer size = bytes already read + bytes last read
        System.arraycopy(resultBuff, 0, tbuff, 0, l);  // copy current lot
       return bytesToHex(tbuff); // call the temp buffer as your result buff
    }


    public boolean isConnected(){
        return clientSocket!=null && clientSocket.isConnected();
    }
}
