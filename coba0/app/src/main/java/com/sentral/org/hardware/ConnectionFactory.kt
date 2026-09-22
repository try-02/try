package com.sentral.org.hardware

import android.bluetooth.BluetoothManager
import android.content.Context
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.data.model.PrinterConnectionType

internal class ConnectionFactory(
    private val context: Context,
) {

    private const val TCP_TIMEOUT_MS = 5000

    fun buildConnection(printerConfig: PrinterEntity): DeviceConnection? {
        val connectionType = safeConnectionType(printerConfig.tipeKoneksi) ?: return null
        return when (connectionType) {
            PrinterConnectionType.BLUETOOTH -> buildBluetoothConnection(printerConfig)
            PrinterConnectionType.WIFI -> buildWifiConnection(printerConfig)
            PrinterConnectionType.USB -> null // TODO: USB permission flow
        }
    }

    private fun buildBluetoothConnection(printerConfig: PrinterEntity): DeviceConnection? {
        val address = printerConfig.alamatBluetooth ?: return null
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return null
        val adapter = bluetoothManager.adapter ?: return null
        val device = adapter.getRemoteDevice(address)
        return BluetoothConnection(device)
    }

    private fun buildWifiConnection(printerConfig: PrinterEntity): DeviceConnection? {
        val address = printerConfig.alamatWifi ?: return null
        val port = printerConfig.portWifi ?: 9100
        return TcpConnection(address, port, TCP_TIMEOUT_MS)
    }

    private fun safeConnectionType(value: String): PrinterConnectionType? =
        PrinterConnectionType.entries.firstOrNull { it.name == value }
}