package com.courier.example

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class DialogItem(val key: String, val title: String)

suspend fun showSDKConfig(activity: Activity, items: List<DialogItem>) = suspendCoroutine<SharedPreferences> { continuation ->

    val sharedPrefs = activity.getPreferences(Context.MODE_PRIVATE)

    val alert = AlertDialog.Builder(activity)
    alert.setTitle("Configure SDK")

    val layout = LinearLayout(activity)
    layout.orientation = LinearLayout.VERTICAL

    val editTexts = items.map { item ->
        val editText = EditText(activity)
        editText.setSingleLine()
        editText.hint = item.title
        editText.setText(sharedPrefs.getString(item.key, ""))
        layout.addView(editText)
        return@map editText
    }

    layout.setPadding(50, 40, 50, 10)

    alert.setView(layout)

    alert.setPositiveButton("Save") { _, _ ->

        with(sharedPrefs.edit()) {
            editTexts.forEachIndexed { index, editText ->
                putString(items[index].key, editText.text.toString())
            }
            apply()
            continuation.resume(sharedPrefs)
        }

        Toast.makeText(activity, "SDK Configured", Toast.LENGTH_LONG).show()

    }

    alert.setCancelable(false)
    alert.show()

}