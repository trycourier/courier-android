package com.courier.android.models


internal data class ProfilePatchPayload(
    val value: String,
    val op: String = "replace",
    val path: String = "/user_id"
)

internal data class CourierProfile(val patch: List<ProfilePatchPayload>)