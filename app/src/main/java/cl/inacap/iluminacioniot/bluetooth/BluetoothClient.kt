package cl.inacap.iluminacioniot.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

/** Cliente Bluetooth Classic (SPP) para el módulo HC-06. Requiere el permiso BLUETOOTH_CONNECT en Android 12+. */
@SuppressLint("MissingPermission")
class BluetoothClient(private val adapter: BluetoothAdapter?) {
    private var socket: BluetoothSocket? = null

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    fun pairedDevices(): List<BluetoothDevice> =
        try { adapter?.bondedDevices?.toList() ?: emptyList() } catch (e: SecurityException) { emptyList() }

    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            adapter?.cancelDiscovery()
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            s.connect()
            socket = s
            true
        } catch (e: Exception) {
            close()
            false
        }
    }

    /** Emite una línea por cada println() del Arduino ("Luz: 512", "Oscuro → LED ENCENDIDO", ...). */
    fun lines(): Flow<String> = flow {
        val reader = socket?.inputStream?.bufferedReader() ?: return@flow
        while (true) {
            val line = reader.readLine() ?: break
            emit(line)
        }
    }.flowOn(Dispatchers.IO)

    /** 'E' enciende y 'A' apaga el LED del pin 8, según el código del Arduino. */
    fun send(command: Char) {
        try {
            socket?.outputStream?.apply { write(command.code); flush() }
        } catch (e: Exception) { /* conexión perdida */ }
    }

    fun close() {
        try { socket?.close() } catch (e: Exception) { }
        socket = null
    }
}
