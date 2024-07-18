package com.courier.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.courier.android.ClientBuilder
import com.courier.android.Env
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrandClientTests {

    @Test
    fun getBrand() = runBlocking {

        val client = ClientBuilder.build()

        val res = client.brands.getBrand(
            brandId = Env.COURIER_BRAND_ID,
        )

        assertTrue(res.data.brand.settings != null)

    }

}