package com.sayemshafayet.onereogamelauncher.data.orgl

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Light obfuscation for on-disk RetroAchievements secrets.
 * Uses an embedded key — not a substitute for a user-held passphrase.
 */
object OrglRaSecretBox {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    /** Material mixed into the AES key — not secret against a determined reverse engineer. */
    private const val KEY_MATERIAL =
        "OneRetroGameLauncher/ra-credentials/v1/embedded-obfuscation-key"

    private val key: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256").digest(KEY_MATERIAL.toByteArray(Charsets.UTF_8))
        SecretKeySpec(digest, "AES")
    }

    fun seal(plaintext: String): String {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(cipherText, 0, packed, iv.size, cipherText.size)
        return Base64.getEncoder().encodeToString(packed)
    }

    fun open(blob: String): String? {
        if (blob.isBlank()) return null
        return try {
            val packed = Base64.getDecoder().decode(blob)
            if (packed.size <= IV_BYTES) return null
            val iv = packed.copyOfRange(0, IV_BYTES)
            val cipherText = packed.copyOfRange(IV_BYTES, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
