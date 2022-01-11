package droidninja.filepicker.utils;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import droidninja.filepicker.R;

public class FilePickerPermission {

    static public final int REQUEST_CODE_FOR_PERMISSION = 11289;
    static final  String GUIDE_PREFERENCE_FILE = "guide_preference_file";
    static final  String GUIDE_PREFERENCE_KEY = "guide_click_key";

    static public void requestStoragePermission(Activity context, String dir) {
        fileUriUtils.startFor(dir, context, FilePickerPermission.REQUEST_CODE_FOR_PERMISSION);
    }

    static public void requestRootStoragePermission(Activity context) {
        guideDialog(context);
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

    static public void guideDialog(Activity mActivity) {
        Dialog dialog = new Dialog(mActivity, R.style.BottomDialog);
        LinearLayout root = (LinearLayout) LayoutInflater.from(mActivity).inflate(
                R.layout.guide_dialog, null);
        root.findViewById(R.id.guide).setOnClickListener((view1 -> {
//            requestRootStoragePermission(mActivity);
            fileUriUtils.startForRoot(mActivity, FilePickerPermission.REQUEST_CODE_FOR_PERMISSION);
            dialog.dismiss();
        }));

        dialog.setContentView(root);
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.BOTTOM);
        dialogWindow.getAttributes().width = mActivity.getResources().getDisplayMetrics().widthPixels;
        dialogWindow.getAttributes().height = mActivity.getResources().getDisplayMetrics().heightPixels;

        dialog.show();
    }

//    static public void saveGuideClick(Context context) {
//        SharedPreferences.Editor note = context.getSharedPreferences(GUIDE_PREFERENCE_FILE, Context.MODE_PRIVATE).edit();
//        note.putBoolean(GUIDE_PREFERENCE_KEY, true);
//        note.apply();
//    }
//
//    static public  boolean isGuideClick(Context context) {
//        SharedPreferences read = context.getSharedPreferences(GUIDE_PREFERENCE_FILE, Context.MODE_PRIVATE);
//        return read.getBoolean(GUIDE_PREFERENCE_KEY,false);
//    }
}