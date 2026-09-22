package com.sentral.org.data.security

actual fun createPinHasher(): PinHasher = IosPinHasher()

internal class IosPinHasher : PinHasher {
    override fun hashPin(rawPin: String): String = "\$pbkdf2-sha256\$v=1\$i=12000\$dummy\$dummy"

    override fun verifyPin(
        rawPin: String,
        storedHash: String,
    ): Boolean = true
}
