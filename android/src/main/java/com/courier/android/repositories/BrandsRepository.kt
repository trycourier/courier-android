package com.courier.android.repositories

import com.courier.android.models.CourierBrand
import com.courier.android.models.CourierBrandResponse
import com.courier.android.utils.dispatch
import com.courier.android.utils.toGraphQuery
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class BrandsRepository : Repository() {

    internal suspend fun getBrand(clientKey: String?, jwt: String?, userId: String, brandId: String): CourierBrand {

        val query = """
            query GetBrand(${'$'}brandId: String = \"${brandId}\") {
                brand(brandId: ${'$'}brandId) {
                    settings {
                        colors {
                            primary
                            secondary
                            tertiary
                        }
                        inapp {
                            borderRadius
                            disableCourierFooter
                        }
                    }
                }
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(baseGraphQL)
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<CourierBrandResponse>()
        return res.data.brand

    }

}