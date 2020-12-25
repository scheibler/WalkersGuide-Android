package org.walkersguide.android.helper;

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

}
