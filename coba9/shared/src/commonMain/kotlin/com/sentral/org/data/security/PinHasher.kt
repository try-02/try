package com.sentral.org.data.security

/**
 * Kontrak KDF untuk hashing dan verifikasi PIN kasir secara aman.
 */
interface PinHasher {
    /** Menghasilkan format MCF: $pbkdf2-sha256$v=1$i=12000$salt$hash */
    fun hashPin(rawPin: String): String

    /** Memverifikasi rawPin terhadap format hash MCF yang tersimpan di DB */
    fun verifyPin(rawPin: String, storedHash: String): Boolean
}

/** Factory function expect untuk KMP */
expect fun createPinHasher(): PinHasher