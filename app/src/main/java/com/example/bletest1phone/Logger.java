package com.example.bletest1phone;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Logger {
    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");
    private static final String TAG = "BLE_LOG";
    private File logFile;
    private File path;

    public Logger() {
    }

    public Logger(String fileName) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            Log.d( TAG,"외부 메모리 읽기 쓰기 불가능");
        }
//        System.out.println("DIRECTORY : " + path + " // FILE NAME :"+fileName);
        this.logFile = new File(path, fileName);
    }

    public Logger(File logFile) {
        this.logFile = logFile;
    }

    public void log(String str) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(this.logFile, true);
//            String date = new Date().toString();
            Log.d(TAG, str);
            fw.write(str);
            fw.write(LINE_SEPARATOR);
        } catch (IOException e) {
            System.err.println("Couldn't log this : " + str);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}