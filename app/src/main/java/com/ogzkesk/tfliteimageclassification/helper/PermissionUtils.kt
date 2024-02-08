package com.ogzkesk.tfliteimageclassification.helper

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {


    fun checkMediaPermission(context: Context): Boolean {
        if (isSdkApiGreaterThan32()) {
            return true
        }
        return hasMediaPermission(context)
    }

    private fun isSdkApiGreaterThan32(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    private fun hasMediaPermission(context: Context): Boolean {
        val externalStoragePermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(context, externalStoragePermission) ==
                PackageManager.PERMISSION_GRANTED
    }
}