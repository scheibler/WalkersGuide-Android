package org.walkersguide.android.helper;

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


public class FileUtility {

    public static boolean copyFile(File sourceFile, File destinationFile) {
        // prepare
        //
        // source file
        if (! sourceFile.exists() || ! sourceFile.isFile()) {
            return false;
        }
        // destination file
        if (destinationFile.exists()) {
            // remove existing
            boolean deleted = destinationFile.delete();
            if (! deleted) {
                return false;
            }
        } else {
            if (! destinationFile.getParentFile().exists()) {
                return false;
            }
        }

        // copy
        InputStream in = null;
        OutputStream out = null;
        byte[] buffer = new byte[1024];
        int length;
        boolean success = true;
        try {
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(destinationFile);
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch(IOException e) {
            Timber.e("file copy failed: " + e.getMessage());
            success = false;
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {}
            }
            return success;
        }
    }

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
