package net_62v.external

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object OneCoreValidator {
    private const val DEFAULT_API_URL = "https://your-app-url.vercel.app/api/v1/license/verify"

    private val client: OkHttpClient = OkHttpClient.Builder().build()

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

        val json = JSONObject().apply {
            put("packageName", packageName)
            put("licenseKey", licenseKey)
        }

        val request = Request.Builder()
            .url(apiUrl)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "Network Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val raw = response.body?.string().orEmpty()
                    if (raw.isBlank()) {
                        callback(false, "Empty response from server")
                        return
                    }

                    try {
                        val jsonResponse = JSONObject(raw)
                        val success = jsonResponse.optBoolean("success", false)
                        val message = jsonResponse.optString("message", if (success) "License verified" else "License verification failed")
                        callback(success, message)
                    } catch (_: Exception) {
                        callback(false, "Invalid server response")
                    }
                }
            }
        })
    }
}
