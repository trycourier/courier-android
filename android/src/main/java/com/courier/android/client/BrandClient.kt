package com.courier.android.client

import com.courier.android.models.CourierBrandResponse
import com.courier.android.utils.dispatch
import com.courier.android.utils.toGraphQuery
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BrandClient(private val options: CourierClient.Options): CourierApiClient() {

    suspend fun getBrand(brandId: String): CourierBrandResponse {

        val query = """
            query GetBrand {
                brand(brandId: \"${brandId}\") {
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
            .url(BASE_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        return http.newCall(request).dispatch<CourierBrandResponse>()

    }

}