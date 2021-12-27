package droidninja.filepicker.utils

import android.content.Intent
import android.text.TextUtils
import android.util.Log

import java.io.File

import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by droidNinja on 08/03/17.
 */

object UploadUtils {

    fun upload(params: Map<String, String>, file:File): String? {
        val policy    = params["policy"]
        val signature = params["signature"]
        val accessid  = params["accessid"]
        val key       = params["key"]
        val domain    = params["domain"]
        val filename  = params["filename"]
        val mimetype  = params["mimetype"]

        if(policy == null || signature == null || accessid == null
            || key == null || domain == null || filename == null || mimetype == null ){
            return null;
        }

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)

        val body = file.asRequestBody(mimetype.toMediaTypeOrNull())

        requestBody.addFormDataPart("policy", policy)
        requestBody.addFormDataPart("OSSAccesskeyId", accessid)
        requestBody.addFormDataPart("signare", signature)
        requestBody.addFormDataPart("key", key)
        requestBody.addFormDataPart("file", filename, body)

        val request = Request.Builder()
            .url(domain)
            .post(requestBody.build())
            .build()

        client.newCall(request).execute().use {
                response -> return response.body?.string()
        }

    }
}
