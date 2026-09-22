package com.sentral.org.data.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun createPinHasher(): PinHasher = AndroidPinHasher()

@OptIn(ExperimentalEncodingApi::class)
internal class AndroidPinHasher : PinHasher {
    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 12_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private const val PREFIX = "\$pbkdf2-sha256\$v=1"
    }

    override fun hashPin(rawPin: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)

        val hash = pbkdf2(rawPin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)

        val saltBase64 = Base64.encode(salt)
        val hashBase64 = Base64.encode(hash)

        return "$PREFIX\$i=$ITERATIONS\$$saltBase64\$$hashBase64"
    }

    override fun verifyPin(
        rawPin: String,
        storedHash: String,
    ): Boolean {
        return try {
            val parts = storedHash.split("$")
            // Format: ["", "pbkdf2-sha256", "v=1", "i=12000", "saltBase64", "hashBase64"]
            if (parts.size != 6) return false
            if (!parts[1].startsWith("pbkdf2-sha256")) return false

            val iterations = parts[3].removePrefix("i=").toIntOrNull() ?: return false
            val salt = Base64.decode(parts[4])
            val expectedHash = Base64.decode(parts[5])

            val actualHash = pbkdf2(rawPin.toCharArray(), salt, iterations, expectedHash.size * 8)

            java.security.MessageDigest.isEqual(actualHash, expectedHash)
        } catch (_: Exception) {
            false
        }
    }

    private fun pbkdf2(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int,
    ): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLengthBits)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        return skf.generateSecret(spec).encoded
    }
}
