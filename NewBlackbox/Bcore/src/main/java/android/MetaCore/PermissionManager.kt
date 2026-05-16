package android.MetaCore

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.lsposed.lsparanoid.Obfuscate
import top.niunaijun.blackbox.BlackBoxCore

@Obfuscate
object PermissionManager {

    private const val TAG = "PermissionManager"
    private const val REQUEST_STORAGE = 1001
    private const val REQUEST_OVERLAY = 1002
    private const val REQUEST_UNKNOWN_SOURCES = 1003
    private const val PREFS_NAME = "activation_prefs"          // TODO: match your actual name
    private const val KEY_PERMISSIONS_GRANTED = "perms_granted"

    interface PermissionCallback {
        fun onAllPermissionsGranted()
        fun onPermissionFailed(reason: String)
        fun onActivationResult(success: Boolean, message: String)
    }

    @JvmStatic
    fun checkAndRequestAllPermissions(activity: Activity, callback: PermissionCallback) {
        if (areAllPermissionsGranted(activity)) {
            callback.onAllPermissionsGranted()
            return
        }
        requestStoragePermission(activity, callback)
    }

    private fun areAllPermissionsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) return false
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) return false
        }
        return true
    }

    private fun requestStoragePermission(activity: Activity, callback: PermissionCallback) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_STORAGE
        )
    }

    private fun requestOverlayPermission(activity: Activity, callback: PermissionCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
            activity.startActivityForResult(intent, REQUEST_OVERLAY)
        } else {
            requestUnknownSourcesPermission(activity, callback)
        }
    }

    private fun requestUnknownSourcesPermission(activity: Activity, callback: PermissionCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
            activity.startActivityForResult(intent, REQUEST_UNKNOWN_SOURCES)
        } else {
            onAllPermissionsGranted(activity, callback)
        }
    }

    private fun onAllPermissionsGranted(context: Context, callback: PermissionCallback) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PERMISSIONS_GRANTED, true).apply()
        callback.onAllPermissionsGranted()
    }

    @JvmStatic
    fun handleActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?, callback: PermissionCallback) {
        when (requestCode) {
            REQUEST_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        requestOverlayPermission(activity, callback)
                    } else {
                        callback.onPermissionFailed("Storage permission denied")
                    }
                } else {
                    // For requestPermissions, we handle in onRequestPermissionsResult
                }
            }
            REQUEST_OVERLAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(activity)) {
                    requestUnknownSourcesPermission(activity, callback)
                } else {
                    callback.onPermissionFailed("Overlay permission denied")
                }
            }
            REQUEST_UNKNOWN_SOURCES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.packageManager.canRequestPackageInstalls()) {
                    onAllPermissionsGranted(activity, callback)
                } else {
                    callback.onPermissionFailed("Unknown sources permission denied")
                }
            }
        }
    }

    @JvmStatic
    fun handleRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray, callback: PermissionCallback) {
        if (requestCode == REQUEST_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                requestOverlayPermission(activity, callback)
            } else {
                callback.onPermissionFailed("Storage permission denied")
            }
        }
    }

    @JvmStatic
    fun activateSdk(userKey: String, callback: PermissionCallback) {
        RemoteManager.getInstance().activateSdk(userKey)
        // Simple polling (for demo). In production, use a proper listener.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(object : Runnable {
            var attempts = 0
            override fun run() {
                val msg = nk.Msg
                if (msg != null && (msg.contains("successful") || msg.contains("failed"))) {
                    callback.onActivationResult(msg.contains("successful"), msg)
                } else {
                    attempts++
                    if (attempts < 10) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 500)
                    } else {
                        callback.onActivationResult(false, "Activation timeout")
                    }
                }
            }
        }, 500)
    }

    @JvmStatic
    fun getActivationStatus(): Boolean = RemoteManager.getInstance().getActivatedSdk()

    @JvmStatic
    fun getServerMessage(): String = RemoteManager.getInstance().getServerMessage()
}