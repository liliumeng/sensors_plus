package dev.fluttercommunity.plus.sensors

import android.content.Context
import android.hardware.Sensor
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

/** SensorsPlugin  */
class SensorsPlugin : FlutterPlugin {
    private lateinit var methodChannel: MethodChannel

    private lateinit var accelerometerChannel: EventChannel
    private lateinit var userAccelChannel: EventChannel
    private lateinit var gyroscopeChannel: EventChannel
    private lateinit var magnetometerChannel: EventChannel
    private lateinit var barometerChannel: EventChannel

    private lateinit var accelerometerStreamHandler: StreamHandlerImpl
    private lateinit var userAccelStreamHandler: StreamHandlerImpl
    private lateinit var gyroscopeStreamHandler: StreamHandlerImpl
    private lateinit var magnetometerStreamHandler: StreamHandlerImpl
    private lateinit var barometerStreamHandler: StreamHandlerImpl

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        setupMethodChannel(binding.binaryMessenger)
        setupEventChannels(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        teardownMethodChannel()
        teardownEventChannels()
    }

    private fun setupMethodChannel(messenger: BinaryMessenger) {
        methodChannel = MethodChannel(messenger, METHOD_CHANNEL_NAME)
        methodChannel.setMethodCallHandler { call, result ->
            val streamHandler = when (call.method) {
                "setAccelerationSamplingPeriod" -> accelerometerStreamHandler
                "setUserAccelerometerSamplingPeriod" -> userAccelStreamHandler
                "setGyroscopeSamplingPeriod" -> gyroscopeStreamHandler
                "setMagnetometerSamplingPeriod" -> magnetometerStreamHandler
                "setBarometerSamplingPeriod" -> barometerStreamHandler
                else -> null
            }
            streamHandler?.samplingPeriod = call.arguments as Int
            if (streamHandler != null) {
                result.success(null)
            } else {
                result.notImplemented()
            }
        }
    }

    private fun teardownMethodChannel() {
        methodChannel.setMethodCallHandler(null)
    }

    // 本地 fork（隐私合规）：不再在插件注册时获取 SensorManager，
    // 仅创建 EventChannel；SensorManager 由 StreamHandlerImpl 在首次
    // 订阅（onListen）时才获取，避免冷启动阶段（隐私弹窗前）访问传感器服务。
    private fun setupEventChannels(context: Context, messenger: BinaryMessenger) {
        accelerometerChannel = EventChannel(messenger, ACCELEROMETER_CHANNEL_NAME)
        accelerometerStreamHandler = StreamHandlerImpl(
            context,
            Sensor.TYPE_ACCELEROMETER
        )
        accelerometerChannel.setStreamHandler(accelerometerStreamHandler)

        userAccelChannel = EventChannel(messenger, USER_ACCELEROMETER_CHANNEL_NAME)
        userAccelStreamHandler = StreamHandlerImpl(
            context,
            Sensor.TYPE_LINEAR_ACCELERATION
        )
        userAccelChannel.setStreamHandler(userAccelStreamHandler)

        gyroscopeChannel = EventChannel(messenger, GYROSCOPE_CHANNEL_NAME)
        gyroscopeStreamHandler = StreamHandlerImpl(
            context,
            Sensor.TYPE_GYROSCOPE
        )
        gyroscopeChannel.setStreamHandler(gyroscopeStreamHandler)

        magnetometerChannel = EventChannel(messenger, MAGNETOMETER_CHANNEL_NAME)
        magnetometerStreamHandler = StreamHandlerImpl(
            context,
            Sensor.TYPE_MAGNETIC_FIELD
        )
        magnetometerChannel.setStreamHandler(magnetometerStreamHandler)

        barometerChannel = EventChannel(messenger, BAROMETER_CHANNEL_NAME)
        barometerStreamHandler = StreamHandlerImpl(
            context,
            Sensor.TYPE_PRESSURE
        )
        barometerChannel.setStreamHandler(barometerStreamHandler)
    }

    private fun teardownEventChannels() {
        accelerometerChannel.setStreamHandler(null)
        userAccelChannel.setStreamHandler(null)
        gyroscopeChannel.setStreamHandler(null)
        magnetometerChannel.setStreamHandler(null)
        barometerChannel.setStreamHandler(null)

        accelerometerStreamHandler.onCancel(null)
        userAccelStreamHandler.onCancel(null)
        gyroscopeStreamHandler.onCancel(null)
        magnetometerStreamHandler.onCancel(null)
        barometerStreamHandler.onCancel(null)
    }

    companion object {
        private const val METHOD_CHANNEL_NAME =
            "dev.fluttercommunity.plus/sensors/method"
        private const val ACCELEROMETER_CHANNEL_NAME =
            "dev.fluttercommunity.plus/sensors/accelerometer"
        private const val GYROSCOPE_CHANNEL_NAME =
            "dev.fluttercommunity.plus/sensors/gyroscope"
        private const val USER_ACCELEROMETER_CHANNEL_NAME =
            "dev.fluttercommunity.plus/sensors/user_accel"
        private const val MAGNETOMETER_CHANNEL_NAME =
            "dev.fluttercommunity.plus/sensors/magnetometer"
        private const val BAROMETER_CHANNEL_NAME =
            "dev.fluttercommunity.plus/sensors/barometer"
    }
}
