package com.courier.android.shared

import com.courier.android.Courier
import com.courier.android.Env
import com.courier.android.ExampleServer
import com.courier.android.models.remove
import com.courier.android.modules.accessToken
import com.courier.android.modules.addAuthenticationListener
import com.courier.android.modules.clientKey
import com.courier.android.modules.signIn
import com.courier.android.modules.signOut
import com.courier.android.modules.userId
import junit.framework.TestCase.assertEquals

internal class UserBuilder {

    companion object {

        suspend fun authenticate(userId: String = Env.COURIER_USER_ID, useJWT: Boolean = true, tenantId: String? = null) {

            // Add listener. Just to make sure the listener is working
            val listener = Courier.shared.addAuthenticationListener { uid ->
                print(uid ?: "No userId found")
            }

            // Sign the user out, if there is one
            Courier.shared.signOut()

            var jwt: String? = null

            if (useJWT) {
                jwt = ExampleServer.generateJWT(
                    authKey = Env.COURIER_AUTH_KEY,
                    userId = userId
                )
            }

            val accessToken = jwt ?: Env.COURIER_AUTH_KEY

            Courier.shared.signIn(
                accessToken = accessToken,
                clientKey = Env.COURIER_CLIENT_KEY,
                userId = userId,
                tenantId = tenantId,
            )

            // Remove the listener
            listener.remove()

            // Check values
            assertEquals(Courier.shared.accessToken, accessToken)
            assertEquals(Courier.shared.userId, userId)
            assertEquals(Courier.shared.clientKey, Env.COURIER_CLIENT_KEY)

        }

    }

}