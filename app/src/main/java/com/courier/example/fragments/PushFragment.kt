package com.courier.example.fragments

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.models.CourierPushEvent
import com.courier.android.models.CourierPushProvider
import com.courier.android.modules.*
import com.courier.android.utils.trackNotification
import com.courier.example.R
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class PushFragment : Fragment(R.layout.fragment_push) {

    private lateinit var fcmTokenItem: LinearLayoutCompat
    private lateinit var fcmToken: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fcmTokenItem = view.findViewById(R.id.fcmTokenItem)
        fcmToken = view.findViewById(R.id.fcmToken)

        fcmToken.text = Courier.shared.fcmToken

        fcmTokenItem.setOnClickListener {

            // Copy to clipboard and show toast
            (context?.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.let {
                val clipData = ClipData.newPlainText("FCM Token", Courier.shared.fcmToken)
                it.setPrimaryClip(clipData)
                Toast.makeText(context, "FCM Token copied", Toast.LENGTH_SHORT).show()
            }

        }

        val refreshTokensButton = view.findViewById<Button>(R.id.refreshTokens)
        refreshTokensButton.setOnClickListener {
            fcmToken.text = Courier.shared.fcmToken
        }

        val requestPermissionsButton = view.findViewById<Button>(R.id.requestPermissions)
        requestPermissionsButton.setOnClickListener {

            if (!Courier.shared.isPushPermissionGranted(requireContext())) {
                Courier.shared.requestNotificationPermission(requireActivity())
            }

        }

    }

}