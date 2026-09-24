package com.example.reelscraper.player

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Lightweight embedded proxy bound exclusively to localhost (127.0.0.1).
 * Injects required Referer, User-Agent, and Cookie headers for dynamic streams
 * (like HLS/DASH or direct media with hotlink protection) and proxies HTTP Range requests.
 */
class LocalStreamingProxy(
    private val client: OkHttpClient
) {
    private var serverSocket: ServerSocket? = null
    private var proxyJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var serverPort: Int = 0
        private set

    val isRunning: Boolean get() = serverSocket?.isClosed == false && serverPort > 0

    @Synchronized
    fun start(): Int {
        if (isRunning) return serverPort
        try {
            // Bind to loopback only on an ephemeral port
            val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            serverPort = socket.localPort

            proxyJob = scope.launch {
                while (isActive && !socket.isClosed) {
                    try {
                        val clientSocket = socket.accept()
                        launch {
                            handleClient(clientSocket)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
            Log.d("LocalStreamingProxy", "Started proxy on 127.0.0.1:$serverPort")
        } catch (e: Exception) {
            Log.e("LocalStreamingProxy", "Failed to start proxy", e)
            serverPort = 0
        }
        return serverPort
    }

    @Synchronized
    fun stop() {
        try {
            proxyJob?.cancel()
            serverSocket?.close()
        } catch (_: Exception) {
        } finally {
            serverSocket = null
            serverPort = 0
        }
    }

    fun buildProxyUrl(
        targetUrl: String,
        referer: String? = null,
        userAgent: String? = null,
        cookie: String? = null
    ): String {
        val port = if (isRunning) serverPort else start()
        if (port == 0) return targetUrl // fallback to direct if proxy couldn't start

        val encodedUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8.name())
        val builder = StringBuilder("http://127.0.0.1:$port/proxy?url=$encodedUrl")
        if (!referer.isNullOrBlank()) {
            builder.append("&referer=").append(URLEncoder.encode(referer, StandardCharsets.UTF_8.name()))
        }
        if (!userAgent.isNullOrBlank()) {
            builder.append("&ua=").append(URLEncoder.encode(userAgent, StandardCharsets.UTF_8.name()))
        }
        if (!cookie.isNullOrBlank()) {
            builder.append("&cookie=").append(URLEncoder.encode(cookie, StandardCharsets.UTF_8.name()))
        }
        return builder.toString()
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val out: OutputStream = socket.getOutputStream()

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0]
                val path = parts[1]

                // Parse incoming request headers (e.g. Range)
                val incomingHeaders = mutableMapOf<String, String>()
                var headerLine = reader.readLine()
                while (!headerLine.isNullOrBlank()) {
                    val colon = headerLine.indexOf(':')
                    if (colon != -1) {
                        val key = headerLine.substring(0, colon).trim().lowercase()
                        val value = headerLine.substring(colon + 1).trim()
                        incomingHeaders[key] = value
                    }
                    headerLine = reader.readLine()
                }

                val uri = Uri.parse("http://localhost$path")
                if (uri.path != "/proxy") {
                    send404(out)
                    return
                }

                val encodedTargetUrl = uri.getQueryParameter("url") ?: return
                val targetUrl = URLDecoder.decode(encodedTargetUrl, StandardCharsets.UTF_8.name())
                val targetReferer = uri.getQueryParameter("referer")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                val targetUa = uri.getQueryParameter("ua")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                val targetCookie = uri.getQueryParameter("cookie")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }

                // Build OkHttp request to remote target
                val reqBuilder = Request.Builder().url(targetUrl)

                if (method.equals("HEAD", ignoreCase = true)) {
                    reqBuilder.head()
                } else {
                    reqBuilder.get()
                }

                // Inject headers
                if (!targetReferer.isNullOrBlank()) {
                    reqBuilder.header("Referer", targetReferer)
                }
                if (!targetUa.isNullOrBlank()) {
                    reqBuilder.header("User-Agent", targetUa)
                } else {
                    reqBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                }
                if (!targetCookie.isNullOrBlank()) {
                    reqBuilder.header("Cookie", targetCookie)
                }

                // Forward Range header if requested by ExoPlayer
                val range = incomingHeaders["range"]
                if (!range.isNullOrBlank()) {
                    reqBuilder.header("Range", range)
                }

                val response = client.newCall(reqBuilder.build()).execute()
                response.use { resp ->
                    val code = resp.code
                    val message = resp.message.ifBlank { if (code == 200) "OK" else if (code == 206) "Partial Content" else "Response" }

                    val headerSb = StringBuilder("HTTP/1.1 $code $message\r\n")
                    for (i in 0 until resp.headers.size) {
                        val name = resp.headers.name(i)
                        val value = resp.headers.value(i)
                        // Skip transfer-encoding or connection to let socket stream smoothly
                        if (!name.equals("Connection", ignoreCase = true)) {
                            headerSb.append("$name: $value\r\n")
                        }
                    }
                    headerSb.append("Connection: close\r\n\r\n")

                    out.write(headerSb.toString().toByteArray(StandardCharsets.UTF_8))
                    out.flush()

                    if (!method.equals("HEAD", ignoreCase = true)) {
                        resp.body?.byteStream()?.use { input ->
                            val buffer = ByteArray(32 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                            }
                            out.flush()
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Client disconnected or read completed
        }
    }

    private fun send404(out: OutputStream) {
        try {
            val response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
            out.write(response.toByteArray(StandardCharsets.UTF_8))
            out.flush()
        } catch (_: Exception) {}
    }
}
