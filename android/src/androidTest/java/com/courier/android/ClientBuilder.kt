package com.courier.android

import com.courier.android.client.CourierClient

internal class ClientBuilder {

    companion object {

        suspend fun build(useJWT: Boolean = true, connectionId: String? = null, tenantId: String? = null): CourierClient {

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
                connectionId = connectionId,
                tenantId = tenantId,
                showLogs = true,
            )

        }

    }

}