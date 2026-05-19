package net_62v.external

import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object OneCoreValidator {
    private const val DEFAULT_API_URL = "https://your-app-url.vercel.app/api/v1/license/verify"
    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 10000

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var apiUrl: String = DEFAULT_API_URL

    @JvmStatic
    fun setApiUrl(url: String) {
        if (url.isBlank()) return
        apiUrl = url
    }

    @JvmStatic
    fun validateLicense(packageName: String, licenseKey: String, callback: (Boolean, String) -> Unit) {
        if (packageName.isBlank()) {
            callback(false, "Package name is empty")
            return
        }
        if (licenseKey.isBlank()) {
            callback(false, "License key is empty")
            return
        }

        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                val payload = JSONObject().apply {
                    put("packageName", packageName)
                    put("licenseKey", licenseKey)
                }.toString()

                connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doInput = true
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

                BufferedWriter(OutputStreamWriter(connection.outputStream, Charsets.UTF_8)).use { writer ->
                    writer.write(payload)
                    writer.flush()
                }

                val code = connection.responseCode
                val raw = if (code in 200..299) {
                    connection.inputStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                }

                val json = JSONObject(raw)
                val success = json.optBoolean("success", false)
                val message = json.optString("message", if (success) "License verified" else "License verification failed")

                callback(success, message)
            } catch (e: Exception) {
                callback(false, "Network Error: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }
}
