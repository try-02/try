package com.sentral.org.ui.screen.pos

import java.text.NumberFormat
import java.util.Locale

private val formatterAngka: NumberFormat =
    NumberFormat.getIntegerInstance(Locale("in", "ID"))

/* Format uang tunggal untuk seluruh layar kasir: Rp 1.234.567 */
fun formatRupiah(nilai: Long): String = "Rp ${formatterAngka.format(nilai)}"