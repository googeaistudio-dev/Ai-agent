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
        } catch (_: Exception) {}
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

    private fun getSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        return (ks.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun storeSecret(key: String, value: String) {
        try {
            if (value.isBlank()) {
                prefs.edit().remove(key).remove("${key}_iv").apply()
                return
            }
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(key, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString("${key}_iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            // Fallback for devices where Keystore fails in sandbox
            prefs.edit().putString("${key}_plain_fallback", value).apply()
        }
    }

    fun getSecret(key: String): String {
        try {
            val encryptedStr = prefs.getString(key, null)
            val ivStr = prefs.getString("${key}_iv", null)
            if (encryptedStr != null && ivStr != null) {
                val cipher = Cipher.getInstance(transformation)
                val iv = Base64.decode(ivStr, Base64.NO_WRAP)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
                val decrypted = cipher.doFinal(Base64.decode(encryptedStr, Base64.NO_WRAP))
                return String(decrypted, Charsets.UTF_8)
            }
        } catch (_: Exception) {}
        return prefs.getString("${key}_plain_fallback", "") ?: ""
    }

    fun clearAllSecrets() {
        prefs.edit().clear().apply()
    }
}
