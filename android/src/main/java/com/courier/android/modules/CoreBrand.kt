package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.models.CourierBrand
import com.courier.android.models.CourierException
import com.courier.android.repositories.BrandsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class CoreBrand {

    private val brandRepo = BrandsRepository()

    internal suspend fun getBrand(brandId: String): CourierBrand {

        if (Courier.shared.clientKey == null && Courier.shared.jwt == null) {
            throw CourierException.missingAccessToken
        }

        if (Courier.shared.userId == null) {
            throw CourierException.missingUserId
        }

        return brandRepo.getBrand(
            clientKey = Courier.shared.clientKey,
            jwt = Courier.shared.jwt,
            userId = Courier.shared.userId!!,
            brandId = brandId
        )

    }

}

/**
 * Extensions
 */

suspend fun Courier.getBrand(brandId: String): CourierBrand {
    return brand.getBrand(brandId)
}

fun Courier.getBrand(brandId: String, onSuccess: (CourierBrand) -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        val brand = brand.getBrand(brandId)
        onSuccess(brand)
    } catch (e: Exception) {
        onFailure(e)
    }
}