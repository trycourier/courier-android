package com.courier.android.shared

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.managers.UserManager
import com.courier.android.modules.accessToken
import com.courier.android.modules.clientKey
import com.courier.android.modules.isUserSignedIn
import com.courier.android.modules.signIn
import com.courier.android.modules.signOut
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

// The access token and client key are stored encrypted, sessions from earlier versions move
// over on first read, and a secret this device can't decrypt reads as signed out
@RunWith(AndroidJUnit4::class)
class CredentialStorageTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val plainPrefs get() = context.getSharedPreferences("Courier SDK", Context.MODE_PRIVATE)
    private val secretPrefs get() = context.getSharedPreferences("Courier SDK Secrets", Context.MODE_PRIVATE)

    // A fresh store, the way a relaunched process starts with nothing decrypted yet
    private fun relaunchStore() {
        UserManager.secretStore = null
    }

    @Before
    fun setup() = runBlocking {
        Courier.initialize(context)
        Courier.shared.signOut()
        plainPrefs.edit().clear().commit()
        secretPrefs.edit().clear().commit()
        relaunchStore()
    }

    @Test
    fun signInStoresTheSecretsEncrypted() = runBlocking {

        val token = "token-${UUID.randomUUID()}"
        val clientKey = "key-${UUID.randomUUID()}"
        Courier.shared.signIn(userId = "storage-user", accessToken = token, clientKey = clientKey, showLogs = false)

        assertNull(plainPrefs.getString("courier_access_token", null))
        assertNull(plainPrefs.getString("courier_client_key", null))

        val stored = secretPrefs.all.values.joinToString()
        assertFalse("the token is stored as plain text", stored.contains(token))
        assertFalse("the client key is stored as plain text", stored.contains(clientKey))

        relaunchStore()
        assertEquals(token, Courier.shared.accessToken)
        assertEquals(clientKey, Courier.shared.clientKey)

        Courier.shared.signOut()

    }

    @Test
    fun sessionFromAnEarlierVersionMovesToEncryptedStorage() {

        // How 6.1.3 and earlier saved the session
        val token = "legacy-${UUID.randomUUID()}"
        plainPrefs.edit()
            .putString("courier_user_id", "legacy-user")
            .putString("courier_access_token", token)
            .commit()

        assertTrue(Courier.shared.isUserSignedIn)
        assertEquals(token, Courier.shared.accessToken)
        assertNull(plainPrefs.getString("courier_access_token", null))

        relaunchStore()
        assertEquals(token, Courier.shared.accessToken)

    }

    @Test
    fun secretFromAnotherDeviceReadsAsSignedOut() {

        // A backup restored to a new device: the session is there, but the keystore key isn't
        plainPrefs.edit().putString("courier_user_id", "restored-user").commit()
        secretPrefs.edit().putString("courier_access_token", "c29tZXRoaW5nIGVuY3J5cHRlZCBlbHNld2hlcmUxMjM0NTY=").commit()

        assertNull(Courier.shared.accessToken)
        assertFalse(Courier.shared.isUserSignedIn)

    }

    @Test
    fun signOutRemovesTheSecrets() = runBlocking {

        Courier.shared.signIn(userId = "storage-user", accessToken = "token-${UUID.randomUUID()}", showLogs = false)
        assertTrue(secretPrefs.all.isNotEmpty())

        Courier.shared.signOut()

        assertTrue(secretPrefs.all.isEmpty())
        relaunchStore()
        assertNull(Courier.shared.accessToken)

    }

}
