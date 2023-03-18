package com.bos.wms.mlkit.app;
import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.util.Printer;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.app.interfaces.PrinterListener;
import com.google.android.gms.tasks.Task;
import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;

import java.util.ArrayList;
import java.util.List;

public class ZebraPrinter {

    private String theBtMacAddress;

    private List<String> printQueue;

    private boolean isCurrentlyPrinting = false;

    private static boolean firstConnectionEstablished = false;

    public static Context applicationContext = null;

    private PrinterListener onPrinterConnectionFail;

    public ZebraPrinter(String theBtMacAddress)
    {
        this.theBtMacAddress = theBtMacAddress;
        printQueue = new ArrayList<>();
    }

    public void setOnPrinterConnectionFailListener(PrinterListener listener){
        this.onPrinterConnectionFail = listener;
    }

    /**
     * Opens a connection via bluetooth to the portable printer and prints the bol number and serial, also the UserCode of the
     * user that printed the paper, to help in printer sharing
     * @param bol
     * @param serial
     */
    public void printBolData(String bol, String serial) {
        String zplData = "";
        if(applicationContext == null){
            zplData = "^XA\n" +
                    "^BY3\n" +
                    "^BUN,100,Y, N, Y\n" +
                    "^CF0,40\n" +
                    "^FO50,10^FD" + serial + "^FS\n" +
                    "^FO70,160^FDBOL# " + bol + "^FS\n" +
                    "^LL50\n" +
                    "^XZ\n";
        }else {
            String userCode = General.getGeneral(applicationContext).UserCode;
            zplData = "^XA\n" +
                    "^BY3\n" +
                    "^BUN,100,Y, N, Y\n" +
                    "^CF0,40\n" +
                    "^FO50,10^FD" + serial + "^FS\n" +
                    "^CF0,30\n" +
                    "^FO20,160^FDBOL# " + bol + "^FS\n" +
                    "^FO250,160^FD" + userCode + "^FS\n" +
                    "^LL50\n" +
                    "^XZ\n";
        }
        printQueue.add(zplData);

        if(!isCurrentlyPrinting){
           progressPrintingQueue(0);
        }
    }

    /**
     * Proceeds the printing queue, everytime this is called, the first item in the printQueue list is printed and removed from the list
     * then the function will be called again and again until the printQueue list is empty
     * @param currentTrail
     */
    private void progressPrintingQueue(int currentTrail){
        if(currentTrail >= 2){
            printQueue.clear();
            isCurrentlyPrinting = false;
            Logger.Error("PRINTER", "Stopped Printing Queue After 2 Failed Trials!");
            this.onPrinterConnectionFail.onConnectionResult(false);
            return;
        }else if(currentTrail >= 1){
            Logger.Debug("PRINTER", "Retrying Connection To Printer, Trail: " + currentTrail);
        }
        if(printQueue.size() == 0){
            isCurrentlyPrinting = false;
            return;
        }else {
            isCurrentlyPrinting = true;
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth&reg; MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(theBtMacAddress);

                    // Initialize
                    Looper.prepare();

                    // Open the connection - physical connection is established here.
                    thePrinterConn.open();

                    String toPrint = printQueue.get(0);

                    Logger.Debug("PRINTER", "Connection to printer established, Printing " + toPrint);


                    // Send the data to printer as a byte array.
                    thePrinterConn.write(toPrint.getBytes());

                    // Make sure the data got to the printer before closing the connection
                    Thread.sleep(500);

                    // Close the insecure connection to release resources.
                    thePrinterConn.close();

                    printQueue.remove(0);

                    Logger.Debug("PRINTER", "Done Printing Item In Queue: " + toPrint);

                    progressPrintingQueue(0);

                    Looper.myLooper().quit();

                } catch (Exception e) {
                    // Handle communications error here.
                    Logger.Error("PRINTER", "Couldn't Print " + printQueue.get(0));
                    Logger.Error("PRINTER", e.getMessage());
                    e.printStackTrace();
                    progressPrintingQueue(currentTrail + 1);
                }
            }
        }).start();
    }

    /**
     * This attempts a connection to the printer with the current mac address, and listens for the result
     * @param resultListener
     */
    public void AttemptConnection(PrinterListener resultListener){
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth&reg; MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(theBtMacAddress);

                    // Initialize
                    Looper.prepare();

                    // Open the connection - physical connection is established here.
                    thePrinterConn.open();

                    Logger.Debug("PRINTER", "Connection to printer established, MAC: " + theBtMacAddress + " Sending Configurations Now!");

                    resultListener.onConnectionResult(true);

                    // Send the data to printer as a byte array.
                    thePrinterConn.write("! U1 setvar \"media.type\" \"label\"".getBytes());

                    Thread.sleep(100);

                    // Send the data to printer as a byte array.
                    thePrinterConn.write("! U1 setvar \"media.sense_mode\" \"gap\"".getBytes());

                    Thread.sleep(100);

                    // Send the data to printer as a byte array.
                    thePrinterConn.write("~jc^xa^jus^xz".getBytes());

                    Thread.sleep(500);

                    // Close the insecure connection to release resources.
                    thePrinterConn.close();

                    Logger.Debug("PRINTER", "Done sending configurations.");

                    Looper.myLooper().quit();
                } catch (Exception e) {
                    // Handle communications error here.
                    Logger.Error("PRINTER", "Couldn't Establish A Connection With Printer, MAC: " + theBtMacAddress);
                    Logger.Error("PRINTER", e.getMessage());
                    e.printStackTrace();
                    resultListener.onConnectionResult(false);
                }
            }
        }).start();
    }

    /**
     * This static function attempts a connection with a mac address at first to unlock the ui later on when everything is loaded
     * @param mac
     */
    public static void establishFirstConnection(String mac){
        AttemptConnection(mac, (result)-> {
            firstConnectionEstablished = result;
        });
    }

    /**
     * Returns a boolean value if the first connection to a printer was successful
     * @return
     */
    public static boolean isFirstConnectionEstablished(){
        return firstConnectionEstablished;
    }

    /**
     * This function helps subclasses change the static value if the first connection result to a printer is changed
     * @param established
     */
    public static void setFirstConnectionEstablished(boolean established){
        firstConnectionEstablished = established;
    }

    /**
     * This attempts a connection to the printer with the corresponding mac address, and listens for the result
     * @param mac
     * @param resultListener
     */
    public static void AttemptConnection(String mac, PrinterListener resultListener){
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth&reg; MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);

                    // Initialize
                    Looper.prepare();

                    // Open the connection - physical connection is established here.
                    thePrinterConn.open();

                    Logger.Debug("PRINTER", "Connection to printer established, MAC: " + mac + " Sending Configurations Now!");

                    // Send the data to printer as a byte array.
                    thePrinterConn.write("! U1 setvar \"media.type\" \"label\"".getBytes());

                    Thread.sleep(100);

                    // Send the data to printer as a byte array.
                    thePrinterConn.write("! U1 setvar \"media.sense_mode\" \"gap\"".getBytes());

                    Thread.sleep(100);

                    // Send the data to printer as a byte array.
                    thePrinterConn.write("~jc^xa^jus^xz".getBytes());

                    Thread.sleep(500);

                    // Close the insecure connection to release resources.
                    thePrinterConn.close();

                    Logger.Debug("PRINTER", "Done sending configurations.");

                    resultListener.onConnectionResult(true);

                    Looper.myLooper().quit();
                } catch (Exception e) {
                    // Handle communications error here.
                    Logger.Error("PRINTER", "Couldn't Establish A Connection With Printer, MAC: " + mac);
                    Logger.Error("PRINTER", e.getMessage());
                    e.printStackTrace();
                    resultListener.onConnectionResult(false);
                }
            }
        }).start();
    }

    public void setMacAddress(String mac){
        this.theBtMacAddress = mac;
    }

    public String getMacAddress(){
        return theBtMacAddress;
    }

}
