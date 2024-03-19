package com.courier.example.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import com.courier.android.Courier
import com.courier.android.modules.fcmToken
import com.courier.android.modules.isPushPermissionGranted
import com.courier.android.modules.requestNotificationPermission
import com.courier.example.R


class PushFragment : Fragment(R.layout.fragment_push) {

    private lateinit var fcmTokenItem: LinearLayoutCompat
    private lateinit var fcmToken: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fcmTokenItem = view.findViewById(R.id.fcmTokenItem)
        fcmToken = view.findViewById(R.id.fcmToken)

        fcmToken.text = Courier.shared.fcmToken ?: "No token found"

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
            fcmToken.text = Courier.shared.fcmToken ?: "No token found"
        }

        val requestPermissionsButton = view.findViewById<Button>(R.id.requestPermissions)
        requestPermissionsButton.setOnClickListener {

            if (!Courier.shared.isPushPermissionGranted(requireContext())) {
                Courier.shared.requestNotificationPermission(requireActivity())
            }

        }

    }

}