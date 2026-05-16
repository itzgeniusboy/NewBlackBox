package android.MetaCore

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import org.lsposed.lsparanoid.Obfuscate
import android.webkit.WebViewClient

/**
 * Fixed version of AdvancedPopupHelper.
 * Usage: AdvancedPopupHelper.show(activity)
 */
@Obfuscate
object AdvancedPopupHelper {

    private val handler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun show(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        handler.post { showPopup(activity) }
    }

    private fun showPopup(activity: Activity) {
        try {
            val dialog = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
            dialog.setCancelable(false)

            val webView = WebView(activity)
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            webView.setBackgroundColor(0x00000000)
            webView.webViewClient = WebViewClient()

            val deviceInfo = """
                {
                    "manufacturer": "${Build.MANUFACTURER}",
                    "model": "${Build.MODEL}",
                    "release": "${Build.VERSION.RELEASE}",
                    "sdk": ${Build.VERSION.SDK_INT}
                }
            """.trimIndent()

            webView.addJavascriptInterface(
                PopupJsInterface(dialog, deviceInfo),
                "Android"
            )

            val htmlContent = """
                <html>
                <head><meta charset="UTF-8"></head>
                <body>
                    <h1>Popup Content</h1>
                    <script>Android.getDeviceInfo();</script>
                </body>
                </html>
            """.trimIndent() // Replace with your actual HTML

            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            dialog.setContentView(webView)
            dialog.show()

            dialog.window?.apply {
                setLayout(dp(activity, 250), dp(activity, 400))
                setGravity(Gravity.CENTER)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0.6f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dp(activity: Activity, value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }

    private class PopupJsInterface(
        private val dialog: Dialog,
        private val deviceInfo: String
    ) {
        @android.webkit.JavascriptInterface
        fun close() { dialog.dismiss() }

        @android.webkit.JavascriptInterface
        fun getDeviceInfo(): String = deviceInfo
    }
}