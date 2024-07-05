package com.courier.android.client

import com.courier.android.Env
import com.courier.android.ExampleServer

internal class ClientBuilder {

    companion object {

        suspend fun build(useJWT: Boolean = true, tenantId: String? = null): CourierClient {

            var jwt: String? = null

            if (useJWT) {
                jwt = ExampleServer.generateJWT(
                    authKey = Env.COURIER_AUTH_KEY,
                    userId = Env.COURIER_USER_ID
                )
            }

            return CourierClient(
                jwt = jwt,
                clientKey = Env.COURIER_CLIENT_KEY,
                userId = Env.COURIER_USER_ID,
                tenantId = tenantId,
                showLogs = true,
            )

        }

    }

}