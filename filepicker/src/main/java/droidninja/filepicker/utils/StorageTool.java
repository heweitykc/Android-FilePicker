package droidninja.filepicker.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StorageTool {

    static public final int REQUEST_CODE_FOR_DIR = 112;

    static public void requestStoragePermission(Activity context, String dir) {
        fileUriUtils.startFor(dir, context, StorageTool.REQUEST_CODE_FOR_DIR);
    }

    static public void requestRootStoragePermission(Activity context) {
        fileUriUtils.startForRoot(context, StorageTool.REQUEST_CODE_FOR_DIR);
    }

    static public boolean hasRootStoragePermission(Activity context) {
        final boolean isgrant = fileUriUtils.isGrantForRoot(context);
        return isgrant;
    }


    static public DocumentFile[] getFiles(Context context, String dir) {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        final String externalDir = externalStorageDirectory.getAbsolutePath();
        DocumentFile file = fileUriUtils.getDoucmentTree(context, externalDir + dir);
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

    static public void savePermission(Activity context, int code, Intent data){
        if(code != StorageTool.REQUEST_CODE_FOR_DIR){
            return;
        }
        if(data == null) {
            return;
        }
        Uri uri = data.getData();
        if(uri == null) {
            return;
        }
        if(code == StorageTool.REQUEST_CODE_FOR_DIR){
            int flags = data.getFlags();
            context.getContentResolver().takePersistableUriPermission(
                    uri, flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            );
        }
    }

}