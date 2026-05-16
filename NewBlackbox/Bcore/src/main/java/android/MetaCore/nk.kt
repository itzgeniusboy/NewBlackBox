package android.MetaCore

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.lsposed.lsparanoid.Obfuscate
import top.niunaijun.blackbox.BlackBoxCore
import java.text.SimpleDateFormat
import java.util.*

@Obfuscate
object nk {

    // ========== Configuration ==========
    private const val ACTIVATION_URL_DEFAULT = "https://akshit.dynamicflash.xyz/api/connect.php"
    private const val PREFERENCE_NAME = "activation_prefs"
    private const val KEY_ACTIVATED = "activated"
    private const val KEY_EXPIRY = "expiry"
    private const val KEY_HIDDEN_STATUS = "hidden_status"

    // Date formats (trying multiple)
    private val DATE_FORMATS = arrayOf(
        "yyyy-MM-dd",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    // Time constants
    private const val DAY_MILLIS = 86400000L
    private const val HOUR_MILLIS = 3600000L

    @Volatile
    var ActivationUrl: String = ACTIVATION_URL_DEFAULT
        set(value) { field = value }

    @JvmField
    @Volatile
    var Msg: String = "Default message"

    @Volatile
    private var hiddenFlag: Boolean = false

    // ----------------------------------------------------------------------
    // Public API (called from RemoteManager and others)
    // ----------------------------------------------------------------------

    @JvmStatic
    fun GAH(): Boolean = hiddenFlag

    @JvmStatic
    fun getActivatedSdk(): Boolean {
        if (!GAH()) {
            Msg = "SDK not activated"
            return false
        }
        val context = BlackBoxCore.getContext() ?: return false
        val sp = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        if (!sp.getBoolean(KEY_ACTIVATED, false)) {
            Msg = "SDK not activated"
            return false
        }
        val expiryStr = sp.getString(KEY_EXPIRY, null) ?: run {
            Msg = "No expiry date set"
            return true // No expiry → treat as active (change if needed)
        }
        return try {
            val expiryDate = parseDate(expiryStr)
            val currentTime = System.currentTimeMillis()
            val expiryTime = expiryDate.time
            if (currentTime < expiryTime) {
                val daysLeft = (expiryTime - currentTime) / DAY_MILLIS
                Msg = "License valid for $daysLeft days"
                true
            } else {
                sp.edit().putBoolean(KEY_ACTIVATED, false).apply()
                Msg = "License expired on: $expiryStr"
                false
            }
        } catch (e: Exception) {
            Msg = "Error parsing expiry"
            true // fallback
        }
    }

    // Helper to parse date with multiple formats
    private fun parseDate(dateStr: String): Date {
        for (format in DATE_FORMATS) {
            try {
                return SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)!!
            } catch (ignored: Exception) {}
        }
        throw IllegalArgumentException("Unparseable date: $dateStr")
    }

    @JvmStatic
    fun getServerMessage(): String = Msg

    /** Show a toast on the main thread */
    @JvmStatic
    fun ismsg(msg: String?) {
        val ctx = BlackBoxCore.getContext() ?: return
        Handler(Looper.getMainLooper()).post {
            try { Toast.makeText(ctx, msg ?: "", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
        }
    }

    /** Set hidden flag (from server response) */
    @JvmStatic
    fun setHidden(status: String?) {
        if (status == null) return
        val value = status.equals("true", ignoreCase = true)
        hiddenFlag = value
        BlackBoxCore.getContext()?.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_HIDDEN_STATUS, status)?.apply()
        Msg = if (value) "Hidden flag set to true" else "Hidden flag set to: $status"
    }

    @JvmStatic
    fun setHidden(value: Boolean) = setHidden(value.toString())

    /** Return the activation URL (never null) */
    @JvmStatic
    fun getUrlHidden(): String = ActivationUrl.ifEmpty { ACTIVATION_URL_DEFAULT }

    /** Chinese method (kept for compatibility) */
    @JvmStatic
    fun 获取接口地址(): String = ACTIVATION_URL_DEFAULT

    /** Check if the app is considered a system app (i.e., activated) */
    @JvmStatic
    fun isSystemApp(): Boolean {
        if (!GAH()) {
            Msg = "System app check failed"
            return false
        }
        if (!getActivatedSdk()) {
            return false
        }
        Msg = "Activation successful"
        return true
    }

    /** Manual expiry check (for debugging) */
    @JvmStatic
    fun checkExpiryManually(): String {
        val context = BlackBoxCore.getContext() ?: return "No context"
        val sp = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val expiryStr = sp.getString(KEY_EXPIRY, null) ?: return "No expiry set"
        return try {
            val expiryDate = parseDate(expiryStr)
            val remaining = expiryDate.time - System.currentTimeMillis()
            if (remaining > 0) {
                val days = remaining / DAY_MILLIS
                val hours = (remaining % DAY_MILLIS) / HOUR_MILLIS
                "License valid for $days days $hours hours"
            } else {
                val overdue = -remaining / DAY_MILLIS
                "Expired $overdue days ago"
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    /** Restore saved status from SharedPreferences (call once at startup) */
    @JvmStatic
    fun loadSavedStatus() {
        try {
            val context = BlackBoxCore.getContext() ?: return
            val sp = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val savedStatus = sp.getString(KEY_HIDDEN_STATUS, "false") ?: "false"
            setHidden(savedStatus)
            getActivatedSdk() // updates Msg
        } catch (_: Exception) {}
    }
}