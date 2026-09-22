package com.sentral.org.backup.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sentral.org.backup.model.PosBackupException
import java.io.File

class DatabaseValidator {

    private inline fun <T> SQLiteConnection.query(
        sql: String,
        block: (SQLiteStatement) -> T,
    ): T {
        val statement = prepare(sql)
        return try {
            block(statement)
        } finally {
            statement.close()
        }
    }

    fun verifyIntegrity(sqliteFile: File) {
        val driver = BundledSQLiteDriver()
        val connection = driver.open(sqliteFile.absolutePath)
        try {
            val result = connection.query("PRAGMA integrity_check;") { stmt ->
                if (stmt.step()) stmt.getText(0) else "error"
            }
            if (result.lowercase() != "ok") {
                throw PosBackupException.SQLiteIntegrityFailed(result)
            }
        } finally {
            connection.close()
        }
    }

    fun verifySchemaAndGetStats(sqliteFile: File): SchemaStats {
        val driver = BundledSQLiteDriver()
        val connection = driver.open(sqliteFile.absolutePath)
        try {
            val requiredTables = listOf(
                "transaksi", "item_transaksi", "produk", "kasir",
                "shift", "pergerakan_kas", "keranjang", "item_keranjang",
                "printer", "profil_toko", "room_master_table",
            )
            val existingTables = mutableSetOf<String>()
            connection.query("SELECT name FROM sqlite_master WHERE type='table';") { stmt ->
                while (stmt.step()) {
                    existingTables.add(stmt.getText(0))
                }
            }
            val missing = requiredTables.filter { it !in existingTables }
            if (missing.isNotEmpty()) {
                throw PosBackupException.IncompatibleSchema("Tabel database wajib tidak ditemukan: $missing")
            }

            var identityHash = ""
            connection.query("SELECT identity_hash FROM room_master_table WHERE id = 42;") { stmt ->
                if (stmt.step()) {
                    identityHash = stmt.getText(0)
                }
            }
            if (identityHash.isBlank()) {
                throw PosBackupException.IncompatibleSchema("room_master_table identity hash kosong")
            }

            var totalTrx = 0L
            var totalProduk = 0L
            var totalKasir = 0L
            var totalShift = 0L

            connection.query("SELECT COUNT(*) FROM transaksi;") { stmt ->
                if (stmt.step()) totalTrx = stmt.getLong(0)
            }
            connection.query("SELECT COUNT(*) FROM produk;") { stmt ->
                if (stmt.step()) totalProduk = stmt.getLong(0)
            }
            connection.query("SELECT COUNT(*) FROM kasir;") { stmt ->
                if (stmt.step()) totalKasir = stmt.getLong(0)
            }
            connection.query("SELECT COUNT(*) FROM shift;") { stmt ->
                if (stmt.step()) totalShift = stmt.getLong(0)
            }

            return SchemaStats(
                identityHash = identityHash,
                totalTransactions = totalTrx,
                totalProducts = totalProduk,
                totalCashiers = totalKasir,
                totalShifts = totalShift,
            )
        } finally {
            connection.close()
        }
    }
}

data class SchemaStats(
    val identityHash: String,
    val totalTransactions: Long,
    val totalProducts: Long,
    val totalCashiers: Long,
    val totalShifts: Long,
)