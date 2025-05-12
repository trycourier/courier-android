package com.courier.android.models

import org.json.JSONArray
import org.json.JSONObject

data class SemanticProperty(
    val name: String,
    val value: String
)

data class SemanticProperties(
    val properties: List<SemanticProperty>
)

fun SemanticProperties.toJson(): JSONObject {
    val jsonObject = JSONObject()
    val propertiesArray = JSONArray()

    for (property in properties) {
        val propObject = JSONObject()
        propObject.put("name", property.name)
        propObject.put("value", property.value)
        propertiesArray.put(propObject)
    }

    jsonObject.put("properties", propertiesArray)
    return jsonObject
}
