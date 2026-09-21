package com.sentral.org.backup.crypto

import com.sentral.org.backup.model.BackupMetadata
import com.sentral.org.backup.model.PosBakHeader
import com.sentral.org.backup.model.PosBackupException
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupCryptoEngine {

    companion object {
        val MAGIC_BYTES = byteArrayOf('P'.code.toByte(), 'O'.code.toByte(), 'S'.code.toByte(), 'B'.code.toByte(), 'A'.code.toByte(), 'K'.code.toByte())
        const val CURRENT_FORMAT_VERSION = 1
        const val KDF_ITERATIONS = 100_000
        const val SALT_LENGTH_BYTES = 16
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        private const val BUFFER_SIZE = 64 * 1024
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun encrypt(
        sourceDbFile: File,
        outputStream: OutputStream,
        password: CharArray,
        metadata: BackupMetadata,
    ) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { random.nextBytes(it) }
        val metadataJson = json.encodeToString(metadata)

        val header = PosBakHeader(
            formatVersion = CURRENT_FORMAT_VERSION,
            schemaVersion = metadata.schemaVersion,
            createdAt = metadata.createdAt,
            kdfIterations = KDF_ITERATIONS,
            salt = salt,
            iv = iv,
            metadataJson = metadataJson,
        )

        val headerBytes = serializeHeader(header)

        // KDF PBKDF2-HMAC-SHA256 -> AES-256 Key
        val secretKey = deriveKey(password, salt, KDF_ITERATIONS)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        cipher.updateAAD(headerBytes)

        // 1. Tulis Header Kanonikal Terbuka
        outputStream.write(headerBytes)

        // 2. Tulis Payload Terenkripsi
        val cipherOut = CipherOutputStream(outputStream, cipher)
        FileInputStream(sourceDbFile).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                cipherOut.write(buffer, 0, bytesRead)
            }
        }
        cipherOut.flush()
        // Penutupan cipherOut menyelesaikan komputasi GCM tag
    }

    fun decrypt(
        sourceEncryptedFile: File,
        destinationDbFile: File,
        password: CharArray,
    ): BackupMetadata {
        if (destinationDbFile.exists()) destinationDbFile.delete()

        FileInputStream(sourceEncryptedFile).use { fileIn ->
            val (header, headerBytes) = parseHeader(fileIn)

            if (header.formatVersion > CURRENT_FORMAT_VERSION) {
                throw PosBackupException.UnsupportedVersion(header.formatVersion)
            }

            val secretKey = deriveKey(password, header.salt, header.kdfIterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, header.iv))
            cipher.updateAAD(headerBytes)

            try {
                FileOutputStream(destinationDbFile).use { fileOut ->
                    val cipherOut = CipherOutputStream(fileOut, cipher)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (fileIn.read(buffer).also { bytesRead = it } != -1) {
                        cipherOut.write(buffer, 0, bytesRead)
                    }
                    cipherOut.flush()
                    cipherOut.close() // Memvalidasi GCM Auth Tag
                }
            } catch (e: IOException) {
                destinationDbFile.delete()
                throw PosBackupException.WrongPasswordOrCorrupted(e)
            } catch (e: Exception) {
                destinationDbFile.delete()
                throw PosBackupException.WrongPasswordOrCorrupted(e)
            }

            return try {
                json.decodeFromString<BackupMetadata>(header.metadataJson)
            } catch (_: Exception) {
                throw PosBackupException.InvalidFormat("Metadata file backup tidak terbaca")
            }
        }
    }

    private fun serializeHeader(header: PosBakHeader): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val out = DataOutputStream(byteStream)

        out.write(MAGIC_BYTES)
        out.writeInt(header.formatVersion)
        out.writeInt(header.schemaVersion)
        out.writeLong(header.createdAt)
        out.writeInt(header.kdfIterations)

        out.writeInt(header.salt.size)
        out.write(header.salt)

        out.writeInt(header.iv.size)
        out.write(header.iv)

        val metaBytes = header.metadataJson.encodeToByteArray()
        out.writeInt(metaBytes.size)
        out.write(metaBytes)

        out.flush()
        return byteStream.toByteArray()
    }

    private fun parseHeader(inputStream: InputStream): Pair<PosBakHeader, ByteArray> {
        val byteStream = ByteArrayOutputStream()
        val dataIn = DataInputStream(object : InputStream() {
            override fun read(): Int {
                val b = inputStream.read()
                if (b != -1) byteStream.write(b)
                return b
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = inputStream.read(b, off, len)
                if (n > 0) byteStream.write(b, off, n)
                return n
            }
        })

        val magic = ByteArray(MAGIC_BYTES.size)
        dataIn.readFully(magic)
        if (!magic.contentEquals(MAGIC_BYTES)) {
            throw PosBackupException.InvalidFormat("File bukan format cadangan POSBAK yang valid")
        }

        val formatVersion = dataIn.readInt()
        val schemaVersion = dataIn.readInt()
        val createdAt = dataIn.readLong()
        val kdfIterations = dataIn.readInt()

        val saltSize = dataIn.readInt()
        val salt = ByteArray(saltSize).also { dataIn.readFully(it) }

        val ivSize = dataIn.readInt()
        val iv = ByteArray(ivSize).also { dataIn.readFully(it) }

        val metaSize = dataIn.readInt()
        val metaBytes = ByteArray(metaSize).also { dataIn.readFully(it) }
        val metadataJson = metaBytes.decodeToString()

        val header = PosBakHeader(
            formatVersion = formatVersion,
            schemaVersion = schemaVersion,
            createdAt = createdAt,
            kdfIterations = kdfIterations,
            salt = salt,
            iv = iv,
            metadataJson = metadataJson,
        )

        return Pair(header, byteStream.toByteArray())
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}