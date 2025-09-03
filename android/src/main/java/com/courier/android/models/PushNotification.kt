package com.courier.android.models

data class PushNotification(
    // Message-level fields
    val data: Map<String, String>,
    val senderId: String? = null,
    val from: String? = null,
    val collapseKey: String? = null,
    val messageId: String? = null,
    val messageType: String? = null,
    val sentTime: Long = 0L,
    val ttl: Int = 0,
    val originalPriority: Int = 0, // mirrors RemoteMessage PRIORITY_* ints
    val deliveredPriority: Int = 0,
    val rawData: List<Byte>? = null,

    // Notification fields
    val notificationTitle: String? = null,
    val notificationBody: String? = null,
    val titleLocalizationKey: String? = null,
    val titleLocalizationArgs: List<String>? = null,
    val bodyLocalizationKey: String? = null,
    val bodyLocalizationArgs: List<String>? = null,
    val icon: String? = null,
    val imageUrl: String? = null,   // Uri -> String
    val sound: String? = null,
    val tag: String? = null,
    val color: String? = null,      // "#rrggbb"
    val clickAction: String? = null,
    val channelId: String? = null,
    val link: String? = null,       // Uri -> String
    val ticker: String? = null,
    val notificationPriority: Int? = null,
    val visibility: Int? = null,
    val notificationCount: Int? = null,
    val lightSettings: List<Int>? = null,   // [color, onMs, offMs]
    val eventTime: Long? = null,
    val sticky: Boolean = false,
    val localOnly: Boolean = false,
    val defaultSound: Boolean = false,
    val defaultVibrateTimings: Boolean = false,
    val defaultLightSettings: Boolean = false,
    val vibrateTimings: List<Long>? = null
)
