package com.courier.android.socket

import com.courier.android.Courier
import com.courier.android.models.CourierException
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlin.coroutines.resume

internal open class CourierSocket(internal val url: String, internal val onClose: (code: Int, reason: String?) -> Unit, internal val onError: (e: Exception) -> Unit) {

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }

    private var webSocket: WebSocket? = null
    private val client: OkHttpClient = OkHttpClient()
    var onMessageReceived: ((String) -> Unit)? = null
    private var onOpen: (() -> Unit)? = null

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
                    onClose(code, reason)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Courier.error(t.message)
                    onError(CourierException.inboxWebSocketFail)
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
        webSocket?.close(NORMAL_CLOSURE_STATUS, null)
    }

    suspend fun send(message: Map<String, Any>) = withContext(Dispatchers.IO) {
        val json = Gson().toJson(message)
        return@withContext webSocket?.send(json)
    }

}