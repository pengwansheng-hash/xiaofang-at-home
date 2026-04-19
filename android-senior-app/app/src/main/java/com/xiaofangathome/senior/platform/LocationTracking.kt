package com.xiaofangathome.senior.platform

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.xiaofangathome.senior.data.LocationSample
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private const val LOCATION_SAMPLES_FILE_NAME = "location_samples.json"
private const val MAX_LOCATION_SAMPLE_COUNT = 24 * 370
private const val ACTION_SAMPLE_HOURLY_LOCATION = "com.xiaofangathome.senior.SAMPLE_HOURLY_LOCATION"

interface LocationSampleRepository {
    fun loadAll(): List<LocationSample>
    fun append(sample: LocationSample)
}

interface LocationTrackingScheduler {
    fun sync(enabled: Boolean): String
}

class FileLocationSampleRepository(
    private val context: Context,
) : LocationSampleRepository {
    private val file = File(context.filesDir, LOCATION_SAMPLES_FILE_NAME)

    override fun loadAll(): List<LocationSample> {
        if (!file.exists()) return emptyList()
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
        if (raw.isBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    LocationSample(
                        id = item.optString("id"),
                        latitude = item.optDouble("latitude"),
                        longitude = item.optDouble("longitude"),
                        accuracyMeters = item.takeIf { !it.isNull("accuracyMeters") }?.optDouble("accuracyMeters")?.toFloat(),
                        provider = item.optString("provider", "unknown"),
                        sampledAt = item.optLong("sampledAt", 0L),
                    ),
                )
            }
        }.sortedByDescending { it.sampledAt }
    }

    override fun append(sample: LocationSample) {
        val updated = (listOf(sample) + loadAll())
            .sortedByDescending { it.sampledAt }
            .take(MAX_LOCATION_SAMPLE_COUNT)
        val array = JSONArray()
        updated.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("latitude", item.latitude)
                    .put("longitude", item.longitude)
                    .put("accuracyMeters", item.accuracyMeters)
                    .put("provider", item.provider)
                    .put("sampledAt", item.sampledAt),
            )
        }
        file.parentFile?.mkdirs()
        file.writeText(array.toString(), Charsets.UTF_8)
    }
}

class AlarmManagerLocationTrackingScheduler(
    private val context: Context,
    private val preferences: SharedPreferences,
) : LocationTrackingScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun sync(enabled: Boolean): String {
        val pendingIntent = buildPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)

        if (!enabled) {
            pendingIntent.cancel()
            return "位置采集已经关闭，后续不会再按小时记录。"
        }

        val triggerAtMillis = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_HOUR,
            pendingIntent,
        )
        preferences.edit().putLong("location_tracking_next_sample_at", triggerAtMillis).apply()
        return "位置采集已经打开，后续会每小时尝试记录一次当前位置。"
    }

    private fun buildPendingIntent(flags: Int): PendingIntent {
        val intent = Intent(context, LocationSamplingReceiver::class.java).apply {
            action = ACTION_SAMPLE_HOURLY_LOCATION
        }
        return PendingIntent.getBroadcast(
            context,
            ACTION_SAMPLE_HOURLY_LOCATION.hashCode(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class LocationSamplingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SAMPLE_HOURLY_LOCATION) return

        SeniorAppServices.ensureInitialized(context.applicationContext)
        if (!SeniorAppServices.preferencesStore.load().hourlyLocationTrackingEnabled) return
        if (!hasLocationTrackingPermission(context)) return

        val pendingResult = goAsync()
        sampleCurrentLocation(context.applicationContext) { sample ->
            if (sample != null) {
                SeniorAppServices.locationSampleRepository.append(sample)
            }
            pendingResult.finish()
        }
    }
}

fun hasForegroundLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

fun hasBackgroundLocationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
}

fun hasLocationTrackingPermission(context: Context): Boolean {
    return hasForegroundLocationPermission(context) && hasBackgroundLocationPermission(context)
}

private fun sampleCurrentLocation(
    context: Context,
    onComplete: (LocationSample?) -> Unit,
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null || !hasForegroundLocationPermission(context)) {
        onComplete(null)
        return
    }

    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { provider -> runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false) }
    val fallbackLocation = bestLastKnownLocation(locationManager, providers.ifEmpty { listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER) })
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || providers.isEmpty()) {
        onComplete(fallbackLocation?.toLocationSample())
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    val handler = Handler(Looper.getMainLooper())
    val completed = AtomicBoolean(false)
    val cancellationSignal = CancellationSignal()

    fun finish(sample: LocationSample?) {
        if (completed.compareAndSet(false, true)) {
            handler.removeCallbacksAndMessages(null)
            onComplete(sample)
        }
    }

    handler.postDelayed({
        cancellationSignal.cancel()
        finish(fallbackLocation?.toLocationSample())
    }, 10_000L)

    requestCurrentLocation(locationManager, providers, 0, cancellationSignal, executor) { location ->
        finish(location?.toLocationSample() ?: fallbackLocation?.toLocationSample())
    }
}

private fun requestCurrentLocation(
    locationManager: LocationManager,
    providers: List<String>,
    index: Int,
    cancellationSignal: CancellationSignal,
    executor: java.util.concurrent.Executor,
    onComplete: (Location?) -> Unit,
) {
    if (index >= providers.size) {
        onComplete(null)
        return
    }
    val provider = providers[index]
    locationManager.getCurrentLocation(provider, cancellationSignal, executor) { location ->
        if (location != null) {
            onComplete(location)
        } else {
            requestCurrentLocation(locationManager, providers, index + 1, cancellationSignal, executor, onComplete)
        }
    }
}

private fun bestLastKnownLocation(
    locationManager: LocationManager,
    providers: List<String>,
): Location? {
    return providers
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }
}

private fun Location.toLocationSample(): LocationSample {
    return LocationSample(
        id = "location_${time}_${latitude}_${longitude}",
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        provider = provider ?: "unknown",
        sampledAt = System.currentTimeMillis(),
    )
}
