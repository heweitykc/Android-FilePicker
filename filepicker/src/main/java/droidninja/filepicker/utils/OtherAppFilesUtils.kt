package droidninja.filepicker.utils

import android.text.TextUtils

import java.io.File

import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.R

/**
 * Created by droidNinja on 08/03/17.
 */

object OtherAppFilesUtils {

    fun isTxtFile(path: String): Boolean {
        val types = arrayOf("txt")
        return FilePickerUtils.contains(types, path)
    }

}
