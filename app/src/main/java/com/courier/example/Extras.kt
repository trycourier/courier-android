package com.courier.example

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
//import com.google.firebase.messaging.RemoteMessage
import com.google.gson.GsonBuilder
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun showAlert(context: Context, title: String, subtitle: String? = null, items: Map<String, String> = emptyMap(), action: String = "OK") = suspendCoroutine { continuation ->

    val alert = AlertDialog.Builder(context)
    alert.setTitle(title)

    subtitle?.let {
        alert.setMessage(it)
    }

    val layout = LinearLayout(context)
    layout.orientation = LinearLayout.VERTICAL

    val editTexts = items.map { item ->
        val editText = EditText(context)
        editText.setSingleLine()
        editText.hint = item.key
        editText.setText(item.value)
        layout.addView(editText)
        return@map editText
    }

    layout.setPadding(50, 40, 50, 10)

    alert.setView(layout)

    alert.setPositiveButton(action) { _, _ ->

        val values = editTexts.mapIndexed { index, editText ->
            items.keys.elementAt(index) to editText.text.toString()
        }.toMap()

        continuation.resume(values)

    }

    alert.setCancelable(false)
    alert.show()

}

//fun RemoteMessage.toJson(): JSONObject {
//    val json = JSONObject()
//    javaClass.declaredFields.forEach { field ->
//        field.isAccessible = true
//        val value = field.get(this)
//        json.put(field.name, value.toJsonValue())
//    }
//    return json
//}
//
//fun RemoteMessage.toJsonString(indentation: Int = 2): String {
//    return this.toJson().toString(indentation)
//}

private fun Any?.toJsonValue(): Any? {
    return when (this) {
//        is RemoteMessage -> toJson()
//        is RemoteMessage.Notification -> toJsonObject()
//        is Map<*, *> -> toJsonObject()
        is List<*> -> toJsonArray()
        is Array<*> -> toJsonArray()
        else -> this
    }
}

//fun RemoteMessage.Notification.toJsonObject(): JSONObject {
//    val json = JSONObject()
//    javaClass.declaredFields.forEach { field ->
//        field.isAccessible = true
//        val value = field.get(this)
//        json.put(field.name, value.toJsonValue())
//    }
//    return json
//}

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

fun InboxMessage.toJson(): String? {
    return GsonBuilder().setPrettyPrinting().create().toJson(this)
}

fun InboxAction.toJson(): String? {
    return GsonBuilder().setPrettyPrinting().create().toJson(this)
}

fun CourierPreferenceTopic.toJson(): String? {
    return GsonBuilder().setPrettyPrinting().create().toJson(this)
}