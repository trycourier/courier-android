package com.courier.example

import com.courier.android.client.CourierClient

enum class CourierEnvironment(val label: String) {
    PRODUCTION("Production"),
    PRODUCTION_EU("Production EU"),
    STAGING("Staging"),
    DEV("Dev"),
    CUSTOM("Custom");

    fun urls(): CourierClient.ApiUrls? {
        return when (this) {
            PRODUCTION -> CourierClient.ApiUrls.us()
            PRODUCTION_EU -> CourierClient.ApiUrls.eu()
            STAGING -> CourierClient.ApiUrls(
                rest = "https://api.courierstaging.com",
                graphql = "https://api.courierstaging.com/client/q",
                inboxGraphql = "http://inbox.courierstaging.com/",
                inboxWebSocket = "wss://inbox-staging-ws-alb-490231599.us-east-1.elb.amazonaws.com"
            )
            DEV -> CourierClient.ApiUrls(
                rest = "https://api.courierdev.com",
                graphql = "https://api.courierdev.com/client/q",
                inboxGraphql = "https://inbox.courierdev.com/q",
                inboxWebSocket = "wss://9mrugsdnk1.execute-api.us-east-1.amazonaws.com/dev"
            )
            CUSTOM -> null
        }
    }

    companion object {
        fun fromLabel(label: String): CourierEnvironment {
            return values().firstOrNull { it.label == label } ?: PRODUCTION
        }
    }
}
