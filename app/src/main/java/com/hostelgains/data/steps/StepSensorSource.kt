package com.hostelgains.data.steps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Hardware sensor source listening to [Sensor.TYPE_STEP_COUNTER].
 * Reports continuous step count since the device booted.
 */
class StepSensorSource(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val isSensorAvailable: Boolean
        get() = stepCounterSensor != null

    /**
     * Cold flow emitting raw cumulative step counts from the hardware step counter.
     */
    val rawStepFlow: Flow<Long> = callbackFlow {
        if (sensorManager == null || stepCounterSensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val rawSteps = event.values.firstOrNull()?.toLong() ?: return
                    trySend(rawSteps)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
