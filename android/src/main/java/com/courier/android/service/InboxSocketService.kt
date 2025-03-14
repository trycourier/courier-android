package com.courier.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.courier.android.Courier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class InboxSocketService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): InboxSocketService = this@InboxSocketService
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val shouldConnectSocket = intent?.getBooleanExtra("connect_socket", false) ?: false

        serviceScope.launch {
            if (shouldConnectSocket) {
                connectWebSocket()
            } else {
                disconnectWebSocket()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        disconnectWebSocket()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private suspend fun connectWebSocket() {
        Courier.shared.client?.inbox?.socket?.let { socket ->

            socket.receivedMessage = { message ->
                serviceScope.launch {
                    Courier.inboxEventBus.emitEvent(message)
                }
            }

            socket.receivedMessageEvent = { event ->
                println("Received event: $event")
            }

            socket.connect()
            socket.sendSubscribe()
            socket.keepAlive()

        }
    }

    private fun disconnectWebSocket() {
        Courier.shared.client?.inbox?.socket?.disconnect()
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, InboxSocketService::class.java)
            intent.putExtra("connect_socket", true)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, InboxSocketService::class.java)
            intent.putExtra("connect_socket", false)
            context.stopService(intent)
        }

    }

}