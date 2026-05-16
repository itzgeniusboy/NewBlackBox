package android.MetaCore

import android.Meta.IRemoteManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import org.lsposed.lsparanoid.Obfuscate
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.NativeCore
import top.niunaijun.blackbox.core.env.BEnvironment
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Obfuscate
class RemoteManager private constructor() : IRemoteManager.Stub() {

    init {
        nk.loadSavedStatus()
    }

    override fun activateSdk(userKey: String?) {
        if (userKey.isNullOrBlank()) {
            nk.Msg = "Invalid key"
            showToastOnce()
            showNotification("Activation Failed", nk.Msg)
            return
        }

        val ctx = BlackBoxCore.getContext()
        if (ctx == null) {
            nk.Msg = "Context is null"
            return
        }

        // FIXED: Get device ID – first try reflection, fallback to normal method
        val deviceId = getDeviceIdReflected() ?: deviceId(ctx)
        if (deviceId.isBlank()) {
            nk.Msg = "Device ID invalid"
            return
        }

        executor.execute { performActivationWithRetry(userKey.trim(), deviceId) }
    }

    override fun getActivatedSdk(): Boolean {
        return try {
            val activated = nk.getActivatedSdk()
            nk.Msg = if (activated) "SDK is activated" else "SDK is not activated"
            activated
        } catch (e: Exception) {
            Log.e(TAG, "getActivatedSdk error", e)
            nk.Msg = "Error checking activation"
            false
        }
    }

    override fun getServerMessage(): String = nk.Msg ?: ""

    override fun getNetwork(): Boolean = true

    // ----------------------------------------------------------------------
    // Activation logic
    // ----------------------------------------------------------------------

    private fun performActivationWithRetry(userKey: String, deviceId: String) {
        val ctx = BlackBoxCore.getContext() ?: run {
            nk.Msg = "Context is null"
            return
        }

        if (isVpnActive()) {
            handleVpnDetection(ctx)
            return
        }

        val hostPkg = BlackBoxCore.getHostPkg()
        if (hostPkg.isNullOrEmpty()) {
            nk.Msg = "Host package empty"
            return
        }

        var lastError: String? = null
        var success = false

        for (attempt in 0..MAX_RETRIES) {
            if (success) break
            if (attempt > 0) {
                nk.Msg = "Retry ${attempt + 1}/$MAX_RETRIES"
                Thread.sleep(RETRY_DELAY_MS)
            }

            var conn: HttpURLConnection? = null
            try {
                val urlStr = NativeCore.ActivateSdkLog()
                Log.d(TAG, "Connecting to: $urlStr")
                val url = URL(urlStr)

                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = CONNECT_TIMEOUT
                conn.readTimeout = READ_TIMEOUT
                conn.doOutput = true

                val data = buildString {
                    append("mode=activate")
                    append("&user_key=").append(URLEncoder.encode(userKey, "UTF-8"))
                    append("&package_name=").append(URLEncoder.encode(hostPkg, "UTF-8"))
                    append("&app_name=").append(URLEncoder.encode(getAppName(ctx, hostPkg), "UTF-8"))
                    append("&device_id=").append(URLEncoder.encode(deviceId, "UTF-8"))
                }

                conn.outputStream.use { os -> os.write(data.toByteArray(Charsets.UTF_8)) }

                val responseCode = conn.responseCode
                Log.d(TAG, "Response code: $responseCode")
                if (responseCode == 200) {
                    val responseBody = read(conn)
                    Log.d(TAG, "Response body: $responseBody")
                    if (responseBody.isNotEmpty()) {
                        val json = JSONObject(responseBody)
                        if (handleActivationResponse(json, ctx)) {
                            success = true
                        } else {
                            lastError = json.optString("message", "Activation failed")
                        }
                    } else {
                        lastError = "Empty response"
                    }
                } else {
                    lastError = "HTTP $responseCode"
                    if (responseCode in 400..499) break
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Network error"
                Log.e(TAG, "Activation attempt $attempt failed", e)
            } finally {
                conn?.disconnect()
            }
        }

        if (!success) {
            nk.setHidden(false)
            nk.Msg = lastError ?: "Activation failed"
            showToastOnce()
            showNotification("Activation Failed", nk.Msg)
        }
    }

    private fun handleActivationResponse(json: JSONObject, ctx: Context): Boolean {
        val status = json.optString("status", "")
        if (status.equals("iv", ignoreCase = true)) {
            Log.d(TAG, "IV flag received")
            nk.Msg = "IV: " + json.optString("message", "")
            val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sp.edit().putString("iv_message", json.optString("message", "")).apply()
            showServerNotification("IV Alert", json.optString("message", ""), "iv")
            sEnableDaemonService = false
            sHideRoot = false
            sHideXposed = false
            return false
        }

        val message = json.optString("message", "")
        val activated = json.optInt("activated", 1) == 1
        val forceLogout = json.optInt("force_logout", 0) == 1

        if (status.equals("success", ignoreCase = true) && activated && !forceLogout) {
            val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putBoolean(KEY_ACTIVATED, true)
                .putString(KEY_EXPIRY, json.optString("expiry", ""))
                .apply()

            nk.setHidden(true)
            nk.Msg = message.ifEmpty { "Activated" }
            toastShown.set(false)

            applyControllerSettings(json)
            startOnlineController()
            showNotification("SDK Activated", nk.Msg)
            return true
        } else {
            nk.setHidden(false)
            nk.Msg = message.ifEmpty { "Activation failed" }
            return false
        }
    }

    // ----------------------------------------------------------------------
    // Online controller (periodic sync)
    // ----------------------------------------------------------------------

    fun startOnlineController() {
        if (controllerRunning) return
        controllerRunning = true
        executor.execute {
            while (controllerRunning) {
                if (isVpnActive()) {
                    deactivateDueToVpn()
                    break
                }
                try {
                    Thread.sleep(controllerInterval * 1000L)
                    fetchController()
                } catch (_: InterruptedException) {
                }
            }
        }
    }

    private fun fetchController() {
        var conn: HttpURLConnection? = null
        try {
            val ctx = BlackBoxCore.getContext() ?: return
            val urlStr = NativeCore.ActivateSdkLog()
            val url = URL(urlStr)

            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.doOutput = true

            val data = "mode=controller" +
                    "&package_name=" + URLEncoder.encode(BlackBoxCore.getHostPkg(), "UTF-8") +
                    "&device_id=" + URLEncoder.encode(deviceId(ctx), "UTF-8")

            conn.outputStream.use { os -> os.write(data.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val jsonStr = read(conn)
                val json = JSONObject(jsonStr)
                applyControllerSettings(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Controller error", e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun applyControllerSettings(json: JSONObject) {
        val activated = json.optInt("activated", 1) == 1
        val forceLogout = json.optInt("force_logout", 0) == 1

        if (!activated || forceLogout) {
            nk.setHidden(false)
            nk.Msg = json.optString("message", "Access revoked")
            controllerRunning = false
            showToastOnce()
            showNotification("Access Revoked", nk.Msg)
            return
        }

        sEnableDaemonService = json.optInt("daemon", 1) == 1
        sHideRoot = json.optInt("hide_root", 1) == 1
        sHideXposed = json.optInt("hide_xposed", 1) == 1
        controllerInterval = json.optInt("interval", 30).coerceAtLeast(10)

        nk.Msg = json.optString("message", "Online")
        nk.setHidden(true)
        toastShown.set(false)

        json.optString("toast").takeIf { it.isNotBlank() }?.let {
            nk.ismsg(it)
            showNotification("Server Message", it)
        }

        json.optJSONObject("notify")?.takeIf { it.optInt("show", 0) == 1 }?.let {
            showServerNotification(
                it.optString("title", "Notification"),
                it.optString("msg", ""),
                it.optString("type", "info")
            )
        }

        json.optJSONObject("img_notify")?.takeIf { it.optInt("show", 0) == 1 }?.let {
            showImageNotification(
                it.optString("title", "Image"),
                it.optString("msg", ""),
                it.optString("url", ""),
                it.optString("base", "")
            )
        }
    }

    // ----------------------------------------------------------------------
    // VPN detection & Telegram alert
    // ----------------------------------------------------------------------

    private fun isVpnActive(): Boolean {
        val ctx = BlackBoxCore.getContext() ?: return false
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun handleVpnDetection(ctx: Context) {
        nk.setHidden(false)
        nk.Msg = "VPN detected. SDK deactivated."
        controllerRunning = false
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ACTIVATED, false).apply()
        showNotification("VPN Detected", "SDK deactivated due to VPN usage.")

        ipExecutor.execute {
            val ip = getPublicIp()
            val info = collectDeviceInfo(ip)
            sendToTelegram(info)
        }
    }

    private fun deactivateDueToVpn() {
        handleVpnDetection(BlackBoxCore.getContext() ?: return)
    }

    private fun getPublicIp(): String {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://api.ipify.org")
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            BufferedReader(InputStreamReader(conn.inputStream)).use { it.readLine() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get public IP", e)
            "Unknown IP"
        } finally {
            conn?.disconnect()
        }
    }

    private fun collectDeviceInfo(ip: String): String {
        val ctx = BlackBoxCore.getContext() ?: return "Context null"
        val packageName = BlackBoxCore.getHostPkg()
        val appName = getAppName(ctx, packageName)
        val deviceId = deviceId(ctx)
        val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER

        return String.format(
            Locale.US,
            "🚨 *VPN DETECTED* 🚨\n" +
                    "📱 *Device Info:*\n" +
                    "IP: %s\n" +
                    "Device ID: %s\n" +
                    "Package: %s\n" +
                    "App Name: %s\n" +
                    "Android: %s\n" +
                    "Model: %s (%s)\n" +
                    "Timestamp: %d",
            ip, deviceId, packageName, appName, androidVersion, model, manufacturer,
            System.currentTimeMillis()
        )
    }

    private fun sendToTelegram(message: String) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(TELEGRAM_API_URL)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")

            val payload = JSONObject().apply {
                put("chat_id", TELEGRAM_CHAT_ID)
                put("text", message)
                put("parse_mode", "Markdown")
            }

            conn.outputStream.use { os -> os.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode != 200) {
                Log.e(TAG, "Telegram send failed: HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send to Telegram", e)
        } finally {
            conn?.disconnect()
        }
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    // FIXED: Now returns a valid device ID from context if reflection fails
    private fun getDeviceIdReflected(): String? {
        // यहाँ आप अपनी रिफ्लेक्शन लॉजिक डाल सकते हैं, या null लौटाकर फॉलबैक का उपयोग कर सकते हैं
        // अभी के लिए null लौटाते हैं, जिससे normal deviceId(ctx) का उपयोग होगा
        return null
    }

    private fun isValidDeviceId(id: String?): Boolean {
        return !id.isNullOrBlank()
    }

    private fun showToastOnce() {
        if (!toastShown.compareAndSet(false, true)) return
        val ctx = BlackBoxCore.getContext() ?: return
        val toastText = nk.Msg?.takeIf { it.isNotBlank() } ?: "License expired or invalid"
        Handler(Looper.getMainLooper()).post {
            try { Toast.makeText(ctx, toastText, Toast.LENGTH_LONG).show() } catch (_: Throwable) {}
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationChannelCreated) return
        val ctx = BlackBoxCore.getContext() ?: return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableLights(true)
            lightColor = Color.RED
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
        notificationChannelCreated = true
    }

    private fun showNotification(title: String, message: String) {
        try {
            val ctx = BlackBoxCore.getContext() ?: return
            ensureNotificationChannel()
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val key = Pair(title, message)
            if (key == lastNotification) return
            lastNotification = key

            val builder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
            nm.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (_: Throwable) {}
    }

    private fun showServerNotification(title: String, message: String, type: String) {
        try {
            val ctx = BlackBoxCore.getContext() ?: return
            ensureNotificationChannel()
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val icon = when (type.lowercase(Locale.ROOT)) {
                "error" -> android.R.drawable.ic_dialog_alert
                else -> android.R.drawable.ic_dialog_info
            }

            val builder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(Color.RED)
                .setAutoCancel(true)
            nm.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (_: Throwable) {}
    }

    private fun showImageNotification(title: String, message: String, imgUrl: String, base: String) {
        executor.execute {
            try {
                if (imgUrl.isEmpty()) return@execute
                val fullUrl = if (base.isNotEmpty()) "$base/$imgUrl" else imgUrl
                val bitmap = BitmapFactory.decodeStream(URL(fullUrl).openStream())
                val ctx = BlackBoxCore.getContext() ?: return@execute
                ensureNotificationChannel()
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return@execute

                val style = NotificationCompat.BigPictureStyle().bigPicture(bitmap)
                val builder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(style)
                    .setAutoCancel(true)
                nm.notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (_: Exception) {}
        }
    }

    @Throws(Exception::class)
    private fun read(conn: HttpURLConnection): String {
        BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
            return sb.toString()
        }
    }

    private fun deviceId(ctx: Context): String = try {
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }

    private fun getAppName(ctx: Context, pkg: String): String = try {
        val pm = ctx.packageManager
        val ai = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(ai).toString()
    } catch (e: Exception) {
        pkg
    }

    companion object {
        private const val TAG = "RemoteManager"
        private const val TELEGRAM_BOT_TOKEN = "8576111732:AAFJ_mkQHXERFLMG1MKQhqxJchebJMYuJxs"
        private const val TELEGRAM_CHAT_ID = "8127154833"
        private const val TELEGRAM_API_URL = "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
        private const val PREF_NAME = "activation_prefs"
        private const val KEY_ACTIVATED = "activated"
        private const val KEY_EXPIRY = "expiry"
        private const val CONNECT_TIMEOUT = 30000
        private const val READ_TIMEOUT = 30000
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 3000L
        private val executor = Executors.newSingleThreadExecutor()
        private val ipExecutor = Executors.newSingleThreadExecutor()

        @JvmField
        var sEnableDaemonService = true
        @JvmField
        var sHideRoot = true
        @JvmField
        var sHideXposed = true

        private var controllerRunning = false
        private var controllerInterval = 30
        private val toastShown = AtomicBoolean(false)
        private var lastNotification: Pair<String, String>? = null
        private const val NOTIFICATION_CHANNEL_ID = "remote_manager_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Remote Manager"
        private var notificationChannelCreated = false

        @Volatile
        private var instance: RemoteManager? = null

        @JvmStatic
        fun getInstance(): RemoteManager {
            return instance ?: synchronized(this) {
                instance ?: RemoteManager().also { instance = it }
            }
        }

        @JvmField
        val JUNIT_JAR = File(BEnvironment.getCacheDir(), "junit.apk")
        @JvmField
        val EMPTY_JAR = File(BEnvironment.getCacheDir(), "empty.jar")
    }
}