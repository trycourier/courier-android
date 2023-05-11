package com.courier.android.models

internal data class CourierBrandResponse(
    val data: CourierBrandData
)

internal data class CourierBrandData(
    val brand: CourierBrand
)

data class CourierBrand(
    val settings: CourierBrandSettings?
)

data class CourierBrandSettings(
    val colors: CourierBrandColors?,
    val inapp: CourierBrandInApp?
)

data class CourierBrandColors(
    val primary: String?
)

data class CourierBrandInApp(
    val disableCourierFooter: Boolean?
)