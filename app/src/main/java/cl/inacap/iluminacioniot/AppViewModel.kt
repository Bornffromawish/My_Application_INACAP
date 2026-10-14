package cl.inacap.iluminacioniot

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.iluminacioniot.bluetooth.BluetoothClient
import cl.inacap.iluminacioniot.data.AppDatabase
import cl.inacap.iluminacioniot.data.Reading
import cl.inacap.iluminacioniot.data.User
import cl.inacap.iluminacioniot.security.PasswordHasher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.sin

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val bt = BluetoothClient(app.getSystemService(BluetoothManager::class.java)?.adapter)

    var user by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)
    var connected by mutableStateOf(false)
    var light by mutableStateOf<Int?>(null)
    var lastMessage by mutableStateOf("")
    var ledOn by mutableStateOf(false)
    var demo by mutableStateOf(false)
    val recent = mutableStateListOf<Int>()

    private var lastSaved = 0L
    private var demoJob: Job? = null

    /** Mismo umbral del Arduino: valor alto = oscuro (el sensor está invertido). */
    val threshold = 500

    private fun pushReading(value: Int) {
        light = value
        recent.add(value)
        if (recent.size > 30) recent.removeAt(0)
    }

    fun register(username: String, password: String) = viewModelScope.launch {
        if (username.isBlank() || password.length < 6) {
            error = "Usuario vacío o contraseña de menos de 6 caracteres"; return@launch
        }
        val salt = PasswordHasher.newSalt()
        try {
            db.userDao().insert(User(username = username.trim(), salt = PasswordHasher.encode(salt), hash = PasswordHasher.hash(password, salt)))
            user = username.trim(); error = null
        } catch (e: Exception) { error = "El usuario ya existe" }
    }

    fun login(username: String, password: String) = viewModelScope.launch {
        val u = db.userDao().find(username.trim())
        val ok = u != null && PasswordHasher.hash(password, PasswordHasher.decode(u.salt)) == u.hash
        if (ok) { user = username.trim(); error = null } else error = "Usuario o contraseña incorrectos"
    }

    fun logout() { toggleDemo(false); bt.close(); connected = false; user = null }

    fun pairedDevices(): List<BluetoothDevice> = bt.pairedDevices()

    /** Modo demostración: genera lecturas simuladas para mostrar las pantallas sin el Arduino. */
    fun toggleDemo(on: Boolean) {
        demo = on
        demoJob?.cancel()
        if (on) {
            recent.clear(); lastMessage = ""
            demoJob = viewModelScope.launch {
                var t = 0
                while (true) {
                    pushReading((500 + 450 * sin(t / 5.0)).toInt().coerceIn(0, 1023))
                    t++
                    delay(500)
                }
            }
        } else {
            light = null; recent.clear()
        }
    }

    fun connect(device: BluetoothDevice) = viewModelScope.launch {
        toggleDemo(false)
        connected = bt.connect(device)
        if (connected) {
            bt.lines().collect { line ->
                if (line.startsWith("Luz:")) {
                    line.removePrefix("Luz:").trim().toIntOrNull()?.let { value ->
                        pushReading(value)
                        val now = System.currentTimeMillis()
                        if (now - lastSaved > 5_000) { // guarda 1 lectura cada 5 s, no cada 500 ms
                            lastSaved = now
                            db.readingDao().insert(Reading(value = value, timestamp = now))
                        }
                    }
                } else if (line.contains("Bluetooth")) {
                    lastMessage = line
                    if (line.contains("ENCENDIDO")) ledOn = true
                    if (line.contains("APAGADO")) ledOn = false
                }
            }
            connected = false
        }
    }

    fun setLed(on: Boolean) {
        if (demo) {
            ledOn = on
            lastMessage = if (on) "LED Bluetooth ENCENDIDO (simulado)" else "LED Bluetooth APAGADO (simulado)"
        } else {
            bt.send(if (on) 'E' else 'A')
        }
    }

    override fun onCleared() { bt.close() }
}
