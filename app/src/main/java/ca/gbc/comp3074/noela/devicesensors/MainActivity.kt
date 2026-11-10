package ca.gbc.comp3074.noela.devicesensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    // getDefaultSensor() returns null when the device has no such sensor
    private var temperatureSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null

    // Compose-observable state: changing these re-composes the UI
    private var temperature by mutableStateOf<Float?>(null)
    private var gyroX by mutableStateOf(0f)
    private var gyroY by mutableStateOf(0f)
    private var gyroZ by mutableStateOf(0f)

    private var temperatureAvailable by mutableStateOf(false)
    private var gyroscopeAvailable by mutableStateOf(false)

    // Extension: sensor details (name, vendor, range, resolution)
    private var temperatureInfo by mutableStateOf<String?>(null)
    private var gyroscopeInfo by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        temperatureAvailable = temperatureSensor != null
        gyroscopeAvailable = gyroscopeSensor != null

        temperatureInfo = temperatureSensor?.describe()
        gyroscopeInfo = gyroscopeSensor?.describe()

        setContent {
            MaterialTheme {
                SensorScreen(
                    temperature = temperature,
                    temperatureAvailable = temperatureAvailable,
                    temperatureInfo = temperatureInfo,
                    gyroX = gyroX,
                    gyroY = gyroY,
                    gyroZ = gyroZ,
                    gyroscopeAvailable = gyroscopeAvailable,
                    gyroscopeInfo = gyroscopeInfo,
                    onResetGyroscope = {
                        // Extension: zero the displayed values until the next sensor event
                        gyroX = 0f
                        gyroY = 0f
                        gyroZ = 0f
                    }
                )
            }
        }
    }

    // Register listeners while the activity is in the foreground
    override fun onResume() {
        super.onResume()

        temperatureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Stop receiving events when the activity is no longer active (saves battery)
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                temperature = event.values[0]          // degrees Celsius
            }

            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0]                // rad/s around X
                gyroY = event.values[1]                // rad/s around Y
                gyroZ = event.values[2]                // rad/s around Z
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required for this lab.
    }
}

private fun Sensor.describe(): String =
    "$name • $vendor\nMax range: $maximumRange • Resolution: $resolution"

@Composable
fun SensorScreen(
    temperature: Float?,
    temperatureAvailable: Boolean,
    temperatureInfo: String?,
    gyroX: Float,
    gyroY: Float,
    gyroZ: Float,
    gyroscopeAvailable: Boolean,
    gyroscopeInfo: String?,
    onResetGyroscope: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Android Sensor Monitor",
                style = MaterialTheme.typography.headlineMedium
            )

            TemperatureCard(
                temperature = temperature,
                available = temperatureAvailable,
                info = temperatureInfo
            )

            GyroscopeCard(
                x = gyroX,
                y = gyroY,
                z = gyroZ,
                available = gyroscopeAvailable,
                info = gyroscopeInfo
            )

            Button(
                onClick = onResetGyroscope,
                enabled = gyroscopeAvailable
            ) {
                Text("Reset gyroscope values")
            }
        }
    }
}

/**
 * Extension: one reusable card used by every sensor, so all readings share the same UI.
 */
@Composable
fun SensorValueCard(
    title: String,
    info: String?,
    highlight: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Color(0xFFFFE0B2)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(8.dp))

            content()

            if (info != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = info, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun TemperatureCard(
    temperature: Float?,
    available: Boolean,
    info: String? = null
) {
    SensorValueCard(title = "Ambient Temperature", info = info) {
        when {
            !available -> Text("Temperature sensor is not available.")

            temperature == null -> Text("Waiting for sensor reading...")

            else -> Text(
                text = "%.1f °C".format(temperature),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun GyroscopeCard(
    x: Float,
    y: Float,
    z: Float,
    available: Boolean,
    info: String? = null
) {
    // Extension: highlight the card when rotation is fast (> 1.0 rad/s on any axis)
    val moving = available && (kotlin.math.abs(x) > 1.0f ||
            kotlin.math.abs(y) > 1.0f || kotlin.math.abs(z) > 1.0f)

    SensorValueCard(title = "Gyroscope", info = info, highlight = moving) {
        if (!available) {
            Text("Gyroscope is not available.")
        } else {
            Text("X: %.3f rad/s".format(x))
            Text("Y: %.3f rad/s".format(y))
            Text("Z: %.3f rad/s".format(z))
            if (moving) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Fast rotation detected", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
