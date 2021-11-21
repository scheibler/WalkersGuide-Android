package org.walkersguide.android.util;

import java.io.BufferedReader;
import java.io.FileReader;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedWriter;
import java.io.FileWriter;

import timber.log.Timber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.walkersguide.android.util.GlobalInstance;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.content.ContentResolver;
import androidx.core.content.FileProvider;


public class FileUtility {

    public static void deleteFolder(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    deleteFolder(child);
                }
            }
            fileOrDirectory.delete();
        }
    }


    /**
     * copy file
     */

    public static boolean copyFile(File source, File destination) {
        return copyFile(Uri.fromFile(source), Uri.fromFile(destination));
    }

    public static boolean copyFile(File source, Uri destination) {
        return copyFile(Uri.fromFile(source), destination);
    }

    public static boolean copyFile(Uri source, File destination) {
        return copyFile(source, Uri.fromFile(destination));
    }

    private static boolean copyFile(Uri source, Uri destination) {
        ContentResolver resolver = GlobalInstance.getContext().getContentResolver();
        ParcelFileDescriptor sourceFileDescriptor = null;
        InputStream in = null;
        ParcelFileDescriptor destinationFileDescriptor = null;
        OutputStream out = null;
        byte[] buffer = new byte[1024];
        int length;
        boolean success = true;

        try {
            // open input file
            sourceFileDescriptor = resolver.openFileDescriptor(source, "r");
            in = new FileInputStream(sourceFileDescriptor.getFileDescriptor());
            // open destination file
            destinationFileDescriptor = resolver.openFileDescriptor(destination, "w");
            out = new FileOutputStream(destinationFileDescriptor.getFileDescriptor());
            // copy
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch(IOException e) {
            Timber.e("file copy failed: " + e.getMessage());
            success = false;
        }

        // close destination file
        if (out != null) {
            try {
                out.flush();
                out.close();
            } catch (IOException e) {}
        }
        if (destinationFileDescriptor != null) {
            try {
                destinationFileDescriptor.close();
            } catch (IOException e) {}
        }
        // close source file
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {}
        }
        if (sourceFileDescriptor != null) {
            try {
                sourceFileDescriptor.close();
            } catch (IOException e) {}
        }

        return success;
    }


    /**
     * read and write text files
     */

    public static JSONObject readJsonObjectFromTextFile(File file) throws IOException, JSONException {
        return new JSONObject(readStringFromTextFile(file));
    }

    private synchronized static String readStringFromTextFile(File file) throws IOException {
        BufferedReader in = null;
        StringBuilder text = new StringBuilder();
        String line;
        IOException ioException = null;
        try {
            in = new BufferedReader(new FileReader(file));
            while ((line = in.readLine()) != null) {
                text.append(line + "\n");
            }
        } catch (IOException e) {
            ioException = e;
        } finally {
            if (in != null) {
                try {
                in.close();
                } catch (IOException e) {}
            }
        }
        if (ioException != null) {
            throw ioException;
        }
        return text.toString();
    }

    public static void writeJsonObjectToTextFile(File file, JSONObject contents) throws IOException, JSONException {
        writeStringToTextFile(file, contents.toString());
    }

    private synchronized static void writeStringToTextFile(File file, String contents) throws IOException {
        BufferedWriter writer = null;
        IOException ioException = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(contents);
        } catch (IOException e) {
            ioException = e;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {}
            }
        }
        if (ioException != null) {
            throw ioException;
        }
    }


}
