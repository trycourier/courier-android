package com.courier.android.socket

import com.courier.android.Courier
import com.courier.android.models.CourierException
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class CourierWebsocket(url: String, private val onMessageReceived: (text: String) -> Unit): WebSocketListener() {

    private companion object {
        const val SOCKET_CLOSE_CODE = 1001
    }

    internal enum class ConnectionState {
        OPENED,
        CLOSED,
        FAILURE
    }

    private val webSocket: WebSocket

    val isSocketConnected get() = state == ConnectionState.OPENED

    private var state = ConnectionState.CLOSED
        private set(value) {
            field = value
            connectionListener?.invoke(value)
        }

    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .hostnameVerifier { _, _ -> true }
        .build()

    init {

        // Create the socket
        webSocket = client.newWebSocket(
            request = Request.Builder().url(url).build(),
            listener = this
        )

    }

    private var connectionListener: ((ConnectionState) -> Unit)? = null

    suspend fun connect(json: String) = suspendCoroutine { continuation ->

        // Listener to the connection state
        connectionListener = { state ->

            // Clear the listener
            connectionListener = null

            // Handle the state
            when (state) {
                ConnectionState.OPENED -> {
                    continuation.resume(Unit)
                }
                else -> {
                    val e = CourierException.inboxWebSocketFail
                    continuation.resumeWithException(e)
                }
            }

        }

        // Send the connection request
        // This is specific to how Courier uses websockets
        webSocket.send(json)

    }

    suspend fun disconnect(code: Int = SOCKET_CLOSE_CODE) = suspendCoroutine { continuation ->

        // Listener to the connection state
        connectionListener = { state ->

            // Clear the listener
            connectionListener = null

            // Handle the state
            when (state) {
                ConnectionState.CLOSED -> {
                    continuation.resume(Unit)
                }
                else -> {
                    val e = CourierException.inboxWebSocketDisconnect
                    continuation.resumeWithException(e)
                }
            }

        }

        // Disconnect
        val didClose = webSocket.close(code, null)

        // Handle websocket already closed
        if (!didClose) {
            webSocket.cancel()
            connectionListener = null
            continuation.resume(Unit)
        }

    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        state = ConnectionState.OPENED
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        onMessageReceived(text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        Courier.log("Disconnecting Inbox Websocket")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        state = ConnectionState.CLOSED
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        state = ConnectionState.FAILURE
    }

}