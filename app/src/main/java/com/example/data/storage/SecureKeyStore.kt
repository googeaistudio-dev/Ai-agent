package com.example.data.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStore(private val context: Context) {

    private val keyAlias = "NafsiAI_SecureKey"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val prefs = context.getSharedPreferences("nafsi_secure_store", Context.MODE_PRIVATE)

    init {
        try {
            ensureKeyExists()
        } catch (_: Throwable) {}
    }

    private fun ensureKeyExists() {
        val ks = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        if (!ks.containsAlias(keyAlias)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance(androidKeyStore).apply { load(null) }
            val entry = ks.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (_: Throwable) {
            null
        }
    }

    fun storeSecret(key: String, value: String) {
        try {
            if (value.isBlank()) {
                prefs.edit().remove(key).remove("${key}_iv").remove("${key}_plain_fallback").commit()
                return
            }
            val secretKey = getSecretKey()
            if (secretKey != null) {
                try {
                    val cipher = Cipher.getInstance(transformation)
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                    val iv = cipher.iv
                    val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

                    prefs.edit()
                        .putString(key, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                        .putString("${key}_iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                        .putString("${key}_plain_fallback", value)
                        .commit()
                    return
                } catch (_: Throwable) {}
            }
            prefs.edit().putString("${key}_plain_fallback", value).commit()
        } catch (_: Throwable) {
            try {
                prefs.edit().putString("${key}_plain_fallback", value).commit()
            } catch (_: Throwable) {}
        }
    }

    fun getSecret(key: String): String {
        try {
            val encryptedStr = prefs.getString(key, null)
            val ivStr = prefs.getString("${key}_iv", null)
            val secretKey = getSecretKey()
            if (encryptedStr != null && ivStr != null && secretKey != null) {
                val cipher = Cipher.getInstance(transformation)
                val iv = Base64.decode(ivStr, Base64.NO_WRAP)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decrypted = cipher.doFinal(Base64.decode(encryptedStr, Base64.NO_WRAP))
                return String(decrypted, Charsets.UTF_8)
            }
        } catch (_: Throwable) {}
        return prefs.getString("${key}_plain_fallback", "") ?: ""
    }

    fun clearAllSecrets() {
        try {
            prefs.edit().clear().apply()
        } catch (_: Throwable) {}
    }
}
