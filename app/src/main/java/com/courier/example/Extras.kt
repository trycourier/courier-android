package com.courier.example
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class DialogItem(val key: String, val title: String)

suspend fun showDialog(activity: Activity, title: String, items: List<DialogItem>) = suspendCoroutine<SharedPreferences> { continuation ->

    val sharedPrefs = activity.getPreferences(Context.MODE_PRIVATE)

    val alert = AlertDialog.Builder(activity)
    alert.setTitle(title)

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

    }

    alert.setCancelable(false)
    alert.show()

}

fun RemoteMessage.toJson(): JSONObject {
    val json = JSONObject()
    javaClass.declaredFields.forEach { field ->
        field.isAccessible = true
        val value = field.get(this)
        json.put(field.name, value.toJsonValue())
    }
    return json
}

fun RemoteMessage.toJsonString(indentation: Int = 2): String {
    return this.toJson().toString(indentation)
}

private fun Any?.toJsonValue(): Any? {
    return when (this) {
        is RemoteMessage -> toJson()
        is RemoteMessage.Notification -> toJsonObject()
        is Map<*, *> -> toJsonObject()
        is List<*> -> toJsonArray()
        is Array<*> -> toJsonArray()
        else -> this
    }
}

fun RemoteMessage.Notification.toJsonObject(): JSONObject {
    val json = JSONObject()
    javaClass.declaredFields.forEach { field ->
        field.isAccessible = true
        val value = field.get(this)
        json.put(field.name, value.toJsonValue())
    }
    return json
}

fun Map<*, *>.toJsonObject(): JSONObject {
    val json = JSONObject()
    forEach { (key, value) ->
        json.put(key.toString(), value.toJsonValue())
    }
    return json
}

fun Collection<*>.toJsonArray(): JSONArray {
    val json = JSONArray()
    forEach { item ->
        json.put(item.toJsonValue())
    }
    return json
}

fun Array<*>.toJsonArray(): JSONArray {
    val json = JSONArray()
    forEach { item ->
        json.put(item.toJsonValue())
    }
    return json
}