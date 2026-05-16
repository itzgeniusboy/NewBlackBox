package android.MetaCore

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object db {

    private const val CONNECT_TIMEOUT = 15000
    private const val READ_TIMEOUT = 15000

    @JvmStatic
    @Throws(IOException::class, IllegalArgumentException::class)
    fun isTrue(url: String): HttpURLConnection {
        require(url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)) {
            "Only HTTP/HTTPS URLs are supported: $url"
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        connection.instanceFollowRedirects = true
        return connection
    }
}