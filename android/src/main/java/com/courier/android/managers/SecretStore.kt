package com.courier.android.managers

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal sealed class SecretRead {
    data class Found(val value: String) : SecretRead()

    // Nothing stored, or stored with a key this device no longer has (restored from a backup)
    object Missing : SecretRead()

    // The keystore couldn't be used, so the caller should fall back to plain storage
    object Unavailable : SecretRead()
}

internal interface SecretStore {
    fun read(key: String): SecretRead
    fun write(key: String, value: String): Boolean
    fun delete(key: String)
}

/**
 * Encrypts secrets with an AES key held in the Android Keystore. The key never leaves the
 * keystore, so a copied or backed up preferences file can't be read on another device.
 */
internal class KeystoreSecretStore(context: Context) : SecretStore {

    companion object {
        private const val PREFS = "Courier SDK Secrets"
        private const val KEY_ALIAS = "courier_credentials_key"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // isUserSignedIn reads the token often, so decrypted values are kept for the process
    private val cache = mutableMapOf<String, String>()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    @Synchronized
    override fun read(key: String): SecretRead {
        cache[key]?.let { return SecretRead.Found(it) }
        val stored = prefs.getString(key, null) ?: return SecretRead.Missing
        val secretKey = try {
            secretKey()
        } catch (e: Exception) {
            return SecretRead.Unavailable
        }
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, bytes, 0, IV_SIZE))
            val value = String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
            cache[key] = value
            SecretRead.Found(value)
        } catch (e: Exception) {
            // Encrypted with a key this device doesn't have, so it can never be read here
            prefs.edit().remove(key).commit()
            SecretRead.Missing
        }
    }

    @Synchronized
    override fun write(key: String, value: String): Boolean {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val saved = prefs.edit().putString(key, Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()
            if (saved) cache[key] = value
            saved
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    override fun delete(key: String) {
        cache.remove(key)
        prefs.edit().remove(key).commit()
    }

}
