package com.courier.android.socket

import com.courier.android.Courier
import com.courier.android.modules.clientKey
import com.courier.android.repositories.Repository
import okhttp3.*
import java.util.concurrent.TimeUnit

internal object CourierInboxWebsocket {

    private var mInstance: CourierWebsocket? = null

    var onMessageReceived: ((text: String) -> Unit)? = null

    val shared: CourierWebsocket?
        get() {

            if (Courier.shared.clientKey == null) {
                disconnect()
                return mInstance
            }

            val url = "${Repository.inboxWebSocket}/?clientKey=${Courier.shared.clientKey!!}"

            if (mInstance?.url != url) {
                mInstance = CourierWebsocket(
                    url = url,
                    onMessageReceived = { onMessageReceived?.invoke(it) }
                )
            }

            return mInstance

        }

    fun connect(clientKey: String, userId: String) {

        val json = """
            {
                "action": "subscribe",
                "data": {
                    "channel": "$userId",
                    "clientKey": "$clientKey",
                    "event": "*",
                    "version": "4"
                }
            }
        """

        mInstance?.connect(json)

    }

    fun disconnect() {
        onMessageReceived = null
        mInstance?.disconnect()
        mInstance = null
    }

}

internal class CourierWebsocket(val url: String, var onMessageReceived: (text: String) -> Unit) : WebSocketListener() {

    private companion object {
        const val SOCKET_CLOSE_CODE = 1001
    }

    internal enum class ConnectionState {
        CONNECTING,
        OPENED,
        CLOSED,
        FAILURE
    }

    private val webSocket: WebSocket

    val isSocketConnected get() = state == ConnectionState.OPENED
    val isSocketConnecting get() = state == ConnectionState.CONNECTING

    private var state = ConnectionState.CLOSED

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

    internal fun connect(json: String) {

        if (isSocketConnecting || isSocketConnected) {
            return
        }

        // Set the connection state
        state = ConnectionState.CONNECTING

        // Send the connection request
        // This is specific to how Courier uses websockets
        webSocket.send(json)

    }

    fun disconnect(code: Int = SOCKET_CLOSE_CODE) {

        // Disconnect
        val didClose = webSocket.close(code, null)

        // Handle websocket already closed
        if (!didClose) {
            webSocket.cancel()
        }

    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        onMessageReceived(text)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        state = ConnectionState.OPENED
        Courier.log("Connecting Inbox Websocket")
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