package com.util

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class  Logging {


    companion object {
        @JvmStatic
        fun LogDebug(context: Context,str:String){
            var outputDirectory:File= getOutputDirectory(context)
            val FolerPath = File(
                outputDirectory,
                "/Debug/"
            )
            Log.e("LogFile",FolerPath.path)

            var timee = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date())
            var data=timee+ ":"+str +"\n\r"
            FolerPath.mkdirs()
            var fileName = SimpleDateFormat("yyyyMMdd HH'.txt'").format(Date())
            fileName="Log_"+fileName
            val logFile = File(
                FolerPath, fileName
            )

            logFile.appendText(data)
        }
        @JvmStatic
        private fun getOutputDirectory(context: Context): File {
            val dest = context.applicationContext.getExternalFilesDir(null);
            return dest!!;
        }
        @JvmStatic
        fun LogError(context: Context,str:String){
            var outputDirectory:File= getOutputDirectory(context)
            val FolerPath = File(
                outputDirectory,
                "/Error/"
            )
            FolerPath.mkdirs()
            Log.e("LogFile",FolerPath.path)
            var timee = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date())
            var data=timee+ ":"+str +"\n\r"
            var fileName = SimpleDateFormat("yyyyMMdd HH'.txt'").format(Date())
            fileName="Log_"+fileName

            val logFile = File(
                FolerPath, fileName
            )

            logFile.appendText(data)
        }
    }
}