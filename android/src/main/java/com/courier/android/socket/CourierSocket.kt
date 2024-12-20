package com.courier.android.socket

import com.courier.android.Courier
import com.courier.android.models.CourierException
import com.courier.android.utils.log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlin.coroutines.resume

open class CourierSocket(internal val url: String) {

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }

    private var webSocket: WebSocket? = null
    private val client: OkHttpClient = OkHttpClient()

    var onOpen: (() -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null
    var onClose: ((code: Int, reason: String?) -> Unit)? = null
    var onError: ((error: Exception) -> Unit)? = null

    private var pingJob: Job? = null

    suspend fun connect() {

        // Disconnect if already connected
        disconnect()

        // Validate URL
        val request = Request.Builder().url(url).build()

        return suspendCancellableCoroutine { continuation ->

            val listener = object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    this@CourierSocket.webSocket = webSocket
                    onOpen?.invoke()
                    continuation.resume(Unit)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onMessageReceived?.invoke(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    onMessageReceived?.invoke(bytes.utf8())
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(NORMAL_CLOSURE_STATUS, null)
                    onClose?.invoke(code, reason)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onError?.invoke(CourierException.inboxWebSocketFail)
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(t))
                    }
                }

            }

            webSocket = client.newWebSocket(request, listener)
            client.dispatcher.executorService.shutdown()

            return@suspendCancellableCoroutine continuation.invokeOnCancellation {
                disconnect()
            }

        }

    }

    fun disconnect() {
        stopPing()
        webSocket?.close(NORMAL_CLOSURE_STATUS, null)
    }

    suspend fun send(message: Map<String, Any>) = withContext(Dispatchers.IO) {
        val json = Gson().toJson(message)
        return@withContext webSocket?.send(json)
    }

    fun keepAlive(interval: Long = 300_000L) {
        stopPing()
        pingJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(interval)
                try {
                    send(mapOf(
                        "action" to "keepAlive"
                    ))
                } catch (e: Exception) {
                    Courier.shared.client?.log(e.localizedMessage ?: "Error occurred on Keep Alive")
                }
            }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        pingJob = null
    }

}