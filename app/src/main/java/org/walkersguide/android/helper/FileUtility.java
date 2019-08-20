package org.walkersguide.android.helper;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.Date;


public class FileUtility {
    //noinspection SimpleDateFormat
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    public static void appendToLog(Context context, String fileName, String message) {
        File file = new File(
                context.getApplicationContext().getExternalFilesDir(null),
                String.format("%1$s.log", fileName));
        try {
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(
                    String.format(
                        "%1$s\t%2$s\n",
                        message,
                        sdf.format(new Date(System.currentTimeMillis())))
                    );
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
