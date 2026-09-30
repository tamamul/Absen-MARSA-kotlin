package com.marsa.absen.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Lokasi terkini dengan akurasi tinggi; null jika gagal. Izin harus sudah diberikan. */
@SuppressLint("MissingPermission")
suspend fun Context.currentLocation(): Location? = suspendCancellableCoroutine { cont ->
    val cts = CancellationTokenSource()
    cont.invokeOnCancellation { cts.cancel() }
    LocationServices.getFusedLocationProviderClient(this)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
        .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
}

/** True jika lokasi berasal dari aplikasi lokasi palsu (mock location). */
@Suppress("DEPRECATION")
val Location.isFake: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider

fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
    val result = FloatArray(1)
    Location.distanceBetween(lat1, lng1, lat2, lng2, result)
    return result[0]
}
