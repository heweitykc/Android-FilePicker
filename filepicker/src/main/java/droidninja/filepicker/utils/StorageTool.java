package droidninja.filepicker.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

public class StorageTool {

    static public DocumentFile[] getFiles(Context context, String dir) {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        final String targetdir = externalStorageDirectory.getAbsolutePath() + dir;
        Log.e("FilePicker", "targetdir:" + targetdir);
        DocumentFile file = fileUriUtils.getDoucmentTree(context, targetdir);
        DocumentFile[] files = file.listFiles();
        return files;
    }

    static public boolean syncFile(Activity context, String uripath, String dstpath){
        DocumentFile file = fileUriUtils.getDoucmentFile(context, uripath);
        Uri uri = file.getUri();

        File dstfile = new File(dstpath);
        try{
            StorageTool.copyFile(context, uri, dstfile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static public String copyFile(Context context, Uri uri, File destFile) throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in != null) {
                out = new FileOutputStream(destFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.close();
                in.close();
                return destFile.getAbsolutePath();
            } else {
                throw new NullPointerException("Invalid input stream");
            }
        } catch (Exception e) {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {}
            throw e;
        }
    }

    public static String getMimeType(File file){
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        return fileNameMap.getContentTypeFor(file.getName());
    }

    public static String getFileRealNameFromUri(Context context, Uri fileUri) {
        if (context == null || fileUri == null) return null;
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, fileUri);
        if (documentFile == null) return null;
        return documentFile.getName();
    }

    public static long getFileSize(String filePath){
        FileChannel fc= null;
        long fileSize = 0;
        try {
            File f= new File(filePath);
            if (f.exists() && f.isFile()){
                FileInputStream fis= new FileInputStream(f);
                fc= fis.getChannel();
                fileSize = fc.size();
            }else{
                Log.e("getFileSize","file doesn't exist or is not a file");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null!=fc){
                try{
                    fc.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        return fileSize;
    }
}