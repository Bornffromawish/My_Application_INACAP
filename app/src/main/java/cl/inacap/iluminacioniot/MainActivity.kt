package cl.inacap.iluminacioniot

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Paleta única para todas las pantallas (rúbrica ítem 8: uniformidad gráfica)
private val BgColor = Color(0xFF0F1B2D)
private val CardColor = Color(0xFF17263D)
private val AccentColor = Color(0xFFF5B544)
private val TextColor = Color(0xFFE8EEF7)
private val MutedColor = Color(0xFFA9B8CF)

private val AppColors = darkColorScheme(
    primary = AccentColor, onPrimary = BgColor,
    background = BgColor, onBackground = TextColor,
    surface = CardColor, onSurface = TextColor,
    onSurfaceVariant = MutedColor, error = Color(0xFFFF8A80),
    outline = Color(0xFF3A4E6E)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = AppColors) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { App() }
            }
        }
    }
}

@Composable
fun App(vm: AppViewModel = viewModel()) {
    var started by rememberSaveable { mutableStateOf(false) }
    if (!started) StartScreen { started = true }
    else if (vm.user == null) LoginScreen(vm)
    else MainScreen(vm)
}

// ---------- Componentes reutilizables ----------

@Composable
fun CardBox(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun Heading(text: String) =
    Text(text, color = TextColor, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))

@Composable
fun SectionTitle(text: String) =
    Text(text, color = AccentColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

@Composable
fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedColor, fontSize = 14.sp)
        Text(value, fontSize = 14.sp)
    }
}

@Composable
fun LightChart(values: List<Int>) {
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - (v / 1023f) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, AccentColor, style = Stroke(width = 4.dp.toPx()))
    }
}

// ---------- Pantalla 1: Inicio (título e integrantes) ----------

@Composable
fun StartScreen(onEnter: () -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
        Spacer(Modifier.height(72.dp))
        Text("TI3042 · Aplicaciones Móviles para IoT", color = MutedColor, fontSize = 14.sp)
        Text("Iluminación Inteligente IoT", color = TextColor, fontSize = 36.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
        Text("Luz automática con Arduino y control remoto por Bluetooth", color = MutedColor, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        CardBox {
            SectionTitle("Integrantes")
            listOf("Bryan González", "Martina Pizarro", "Esperanza Pavez").forEach { Text(it, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        Text("Docente: Igor Cáceres Padilla", color = MutedColor, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.weight(1f))
        Button(onEnter, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Ingresar") }
    }
}

// ---------- Pantalla 2: Login (control de acceso local) ----------

@Composable
fun LoginScreen(vm: AppViewModel) {
    var u by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Heading("Iniciar sesión")
        Text("Acceso local a la aplicación", color = MutedColor, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(u, { u = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(p, { p = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, visualTransformation = PasswordVisualTransformation())
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button({ vm.login(u, p) }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Ingresar") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton({ vm.register(u, p) }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Crear cuenta") }
        Text("Las contraseñas se guardan cifradas en el dispositivo.", color = MutedColor, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
    }
}

// ---------- Pantallas con barra inferior ----------

@Composable
fun MainScreen(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Proyecto", "Sensor", "Control", "Bluetooth")
    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            NavigationBar(containerColor = BgColor) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i }, icon = {},
                        label = { Text(t, fontSize = 13.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = AccentColor, unselectedTextColor = MutedColor,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState())) {
            when (tab) {
                0 -> ProjectTab()
                1 -> SensorTab(vm)
                2 -> ControlTab(vm)
                else -> BluetoothTab(vm)
            }
            TextButton({ vm.logout() }, Modifier.padding(top = 16.dp)) { Text("Cerrar sesión (${vm.user})", color = MutedColor) }
        }
    }
}

// Pantalla 3: descripción del proyecto e información del sensor
@Composable
fun ProjectTab() {
    Heading("Proyecto")
    CardBox {
        SectionTitle("Problemática")
        Text("La iluminación se enciende y apaga de forma manual, lo que genera uso innecesario de energía.", fontSize = 14.sp)
    }
    CardBox {
        SectionTitle("Solución")
        Text("Un sensor LDR mide la luz ambiental; el Arduino enciende un LED cuando está oscuro y envía los datos por Bluetooth.", fontSize = 14.sp)
    }
    CardBox {
        SectionTitle("Sensor KY-018")
        KeyValueRow("Tipo", "Fotorresistor (LDR)")
        KeyValueRow("Resistencia", "10 kΩ (divisor de voltaje)")
        KeyValueRow("Conexión", "S → A0, VCC → 5V, GND → GND")
    }
}

// Pantalla 4: información del sensor en tiempo real
@Composable
fun SensorTab(vm: AppViewModel) {
    Heading("Monitoreo")
    val l = vm.light
    CardBox {
        Text("Nivel de luz (A0)", color = MutedColor, fontSize = 14.sp)
        Text(l?.toString() ?: "--", color = AccentColor, fontSize = 56.sp, fontWeight = FontWeight.SemiBold)
        Text(
            when {
                l == null -> "Sin datos"
                l >= vm.threshold -> "Oscuro · LED automático encendido"
                else -> "Claro · LED automático apagado"
            }
        )
        if (vm.demo) Text("Datos simulados (modo demostración)", color = MutedColor, fontSize = 12.sp)
    }
    CardBox {
        SectionTitle("Últimas lecturas")
        LightChart(vm.recent.toList())
    }
    CardBox {
        SectionTitle("Historial")
        if (vm.recent.isEmpty()) Text("Sin lecturas todavía", color = MutedColor, fontSize = 14.sp)
        else vm.recent.takeLast(5).reversed().forEach { KeyValueRow("Luz", "$it") }
    }
}

// Pantalla 5: control remoto del Arduino
@Composable
fun ControlTab(vm: AppViewModel) {
    Heading("Control remoto")
    CardBox {
        KeyValueRow("Módulo", if (vm.demo) "Simulado" else if (vm.connected) "HC-06 · Conectado" else "Sin conexión")
        KeyValueRow("LED Bluetooth (pin 8)", if (vm.ledOn) "Encendido" else "Apagado")
    }
    val enabled = vm.connected || vm.demo
    Spacer(Modifier.height(24.dp))
    Button({ vm.setLed(true) }, Modifier.fillMaxWidth().height(64.dp), enabled = enabled, shape = RoundedCornerShape(12.dp)) { Text("Encender (E)") }
    Spacer(Modifier.height(12.dp))
    OutlinedButton({ vm.setLed(false) }, Modifier.fillMaxWidth().height(64.dp), enabled = enabled, shape = RoundedCornerShape(12.dp)) { Text("Apagar (A)") }
    CardBox {
        SectionTitle("Respuesta del Arduino")
        Text(vm.lastMessage.ifEmpty { "—" }, color = MutedColor, fontSize = 14.sp)
    }
}

// Pantalla 6: conexión Bluetooth
@SuppressLint("MissingPermission")
@Composable
fun BluetoothTab(vm: AppViewModel) {
    var devices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) devices = vm.pairedDevices()
    }
    Heading("Conexión Bluetooth")
    CardBox {
        KeyValueRow("Estado", if (vm.connected) "Conectado" else "Sin conexión")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Modo demostración (datos simulados)", color = MutedColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Switch(vm.demo, { vm.toggleDemo(it) })
        }
    }
    Spacer(Modifier.height(12.dp))
    Button({
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        else devices = vm.pairedDevices()
    }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Ver dispositivos emparejados") }
    devices.forEach { d ->
        OutlinedButton({ vm.connect(d) }, Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp)) { Text(d.name ?: d.address) }
    }
    Text("Empareja antes el HC-06 desde los ajustes del teléfono y cambia su PIN por defecto.", color = MutedColor, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
}
