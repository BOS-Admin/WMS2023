package com.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ErrorException implements Thread.UncaughtExceptionHandler {
    public ErrorException(Context context) {
        ctx=context;
    }
    Context ctx;
    // Method to handle the
    // uncaught exception
    public void uncaughtException(Thread t, Throwable ex) {
        try {
            //异常文件log.txt，可以判断返回给我们的服务器
            ex.printStackTrace(new PrintStream(new File("log.txt")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Writer w = new StringWriter();
        ex.printStackTrace(new PrintWriter(w));
        String smsg = w.toString();
        showError(smsg);
        Logging.LogError(ctx,smsg);
        //保存文件之后，自杀,myPid() : 获取自己的pid
        android.os.Process.killProcess(android.os.Process.myPid());

    }
    private void showError(String Error) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setMessage(Error)
                    .setPositiveButton("Close Error", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
        }
        catch (Exception ex){
            Logging.LogError(ctx,"showError:"+ ex.getMessage());
        }

    }

}
