package dev.fluttercommunity.plus.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink

internal class StreamHandlerImpl(
    private val appContext: Context,
    private val sensorType: Int
) : EventChannel.StreamHandler {
    private var sensorEventListener: SensorEventListener? = null

    private var sensor: Sensor? = null

    // 本地 fork（隐私合规）：SensorManager 延迟到首次订阅（onListen）时才获取，
    // 避免插件注册阶段（隐私弹窗前）访问传感器服务。
    private var sensorManager: SensorManager? = null

    private fun obtainSensorManager(): SensorManager {
        val cached = sensorManager
        if (cached != null) {
            return cached
        }
        val manager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager = manager
        return manager
    }

    private var timestampMicroAtBoot: Long = System.currentTimeMillis() * 1000 - SystemClock.elapsedRealtimeNanos() / 1000

    var samplingPeriod = 200000
        set(value) {
            field = value
            updateRegistration()
        }

    override fun onListen(arguments: Any?, events: EventSink) {
        val sensorManager = obtainSensorManager()
        sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor != null) {
            sensorEventListener = createSensorEventListener(events)
            sensorManager.registerListener(sensorEventListener, sensor, samplingPeriod)
        } else {
            events.error(
                "NO_SENSOR",
                "Sensor not found",
                "It seems that your device has no ${getSensorName(sensorType)} sensor"
            )
        }
    }

    override fun onCancel(arguments: Any?) {
        if (sensor != null) {
            sensorManager?.unregisterListener(sensorEventListener)
            sensorEventListener = null
        }
    }

    private fun updateRegistration() {
        if (sensorEventListener != null) {
            sensorManager?.unregisterListener(sensorEventListener)
            sensorManager?.registerListener(sensorEventListener, sensor, samplingPeriod)
        }
    }

    private fun getSensorName(sensorType: Int): String {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_LINEAR_ACCELERATION -> "User Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
            Sensor.TYPE_PRESSURE -> "Barometer"
            else -> "Undefined"
        }
    }

    private fun createSensorEventListener(events: EventSink): SensorEventListener {
        return object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent) {
                val sensorValues = DoubleArray(event.values.size + 1)
                event.values.forEachIndexed { index, value ->
                    sensorValues[index] = value.toDouble()
                }

                val timestampMicro = timestampMicroAtBoot + (event.timestamp / 1000)
                sensorValues[event.values.size] = timestampMicro.toDouble()

                events.success(sensorValues)
            }
        }
    }
}
