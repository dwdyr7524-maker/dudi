package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ApiKeyStore {
    val hasApiKeyFlow: StateFlow<Boolean>
    val maskedApiKeyFlow: StateFlow<String>
    fun saveApiKey(rawKey: String)
    fun getApiKey(): String?
    fun clearApiKey()
    fun hasApiKey(): Boolean
    fun getMaskedApiKey(): String
}

/**
 * Encrypts the Anthropic API key using a hardware-backed AES-256-GCM key stored in AndroidKeyStore.
 * Only the random 12-byte IV and AES-GCM ciphertext are written to private SharedPreferences.
 */
class SecureApiKeyStore(context: Context) : ApiKeyStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hasApiKeyFlow = MutableStateFlow(false)
    override val hasApiKeyFlow: StateFlow<Boolean> = _hasApiKeyFlow.asStateFlow()

    private val _maskedApiKeyFlow = MutableStateFlow("")
    override val maskedApiKeyFlow: StateFlow<String> = _maskedApiKeyFlow.asStateFlow()

    init {
        refreshStatus()
    }

    override fun saveApiKey(rawKey: String) {
        val cleaned = rawKey.trim()
        if (cleaned.isEmpty()) {
            clearApiKey()
            return
        }
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(cleaned.toByteArray(StandardCharsets.UTF_8))

        val encodedCiphertext = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_CIPHERTEXT, encodedCiphertext)
            .putString(KEY_IV, encodedIv)
            .putString(KEY_MASK_HINT, maskKey(cleaned))
            .apply()

        refreshStatus()
    }

    override fun getApiKey(): String? {
        val encodedCiphertext = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val encodedIv = prefs.getString(KEY_IV, null) ?: return null

        return try {
            val secretKey = getExistingKey() ?: return null
            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decrypted = cipher.doFinal(ciphertext)
            String(decrypted, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    override fun clearApiKey() {
        prefs.edit()
            .remove(KEY_CIPHERTEXT)
            .remove(KEY_IV)
            .remove(KEY_MASK_HINT)
            .apply()
        refreshStatus()
    }

    override fun hasApiKey(): Boolean {
        return !prefs.getString(KEY_CIPHERTEXT, null).isNullOrBlank() &&
            !prefs.getString(KEY_IV, null).isNullOrBlank()
    }

    override fun getMaskedApiKey(): String {
        return prefs.getString(KEY_MASK_HINT, "") ?: ""
    }

    private fun refreshStatus() {
        val present = hasApiKey()
        _hasApiKeyFlow.value = present
        _maskedApiKeyFlow.value = if (present) getMaskedApiKey() else ""
    }

    private fun getExistingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }

    private fun getOrCreateKey(): SecretKey {
        getExistingKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "claude_classic_api_key_aes256"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128

        private const val PREFS_NAME = "claude_classic_secure_vault"
        private const val KEY_CIPHERTEXT = "encrypted_api_key"
        private const val KEY_IV = "encrypted_api_key_iv"
        private const val KEY_MASK_HINT = "api_key_mask_hint"

        fun maskKey(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.length <= 10) return "••••••••"
            val prefix = trimmed.take(7)
            val suffix = trimmed.takeLast(4)
            return "$prefix••••$suffix"
        }
    }
}
