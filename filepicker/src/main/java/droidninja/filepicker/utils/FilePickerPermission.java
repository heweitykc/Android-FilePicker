package droidninja.filepicker.utils;

import android.annotation.SuppressLint;
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
import java.net.FileNameMap;
import java.net.URLConnection;

public class FilePickerPermission {

    static public final int REQUEST_CODE_FOR_PERMISSION = 11289;

    static public void requestStoragePermission(Activity context, String dir) {
        fileUriUtils.startFor(dir, context, FilePickerPermission.REQUEST_CODE_FOR_PERMISSION);
    }

    static public void requestRootStoragePermission(Activity context) {
        fileUriUtils.startForRoot(context, FilePickerPermission.REQUEST_CODE_FOR_PERMISSION);
    }

    static public boolean hasRootStoragePermission(Activity context) {
        final boolean isgrant = fileUriUtils.isGrantForRoot(context);
        return isgrant;
    }

    @SuppressLint("WrongConstant")
    static public boolean onActivityResult(Activity context, int requestCode, int resultCode, Intent data){
        if(requestCode != FilePickerPermission.REQUEST_CODE_FOR_PERMISSION){
            return false;
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            return false;
        }
        Uri uri = data.getData();
        if(uri == null) {
            return false;
        }

        int flags = data.getFlags();
        context.getContentResolver().takePersistableUriPermission(
                uri, flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        );
        return true;
    }

    public static String getMimeType(File file){
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String type = fileNameMap.getContentTypeFor(file.getName());
        return type;
    }

}