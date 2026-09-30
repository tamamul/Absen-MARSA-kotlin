package com.marsa.absen.ui.screen.absen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.marsa.absen.domain.model.AbsenMode
import com.marsa.absen.domain.model.AbsenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable
import com.marsa.absen.ui.screen.absen.face.FaceCamera

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun AbsenScreen(
    request: AbsenRequest,
    onClose: (success: Boolean) -> Unit,
    vm: AbsenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val state = vm.state
    var manual by rememberSaveable { mutableStateOf(false) }

    fun hasPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasPermissions()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted = hasPermissions() }

    val requestPermissions = {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            )
        )
    }

    LifecycleResumeEffect(Unit) {
        granted = hasPermissions()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { if (!granted) requestPermissions() }
    LaunchedEffect(granted) { if (granted) vm.start(request) }

    DisposableEffect(Unit) {
        onDispose {
            if (activity?.isChangingConfigurations != true) vm.reset()
        }
    }

    val success = state.step == AbsenStep.DONE && state.success
    BackHandler(enabled = state.step != AbsenStep.SUBMITTING) { onClose(success) }

    val title = if (request.mode == AbsenMode.MASUK) "Absen Masuk" else "Absen Pulang"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onClose(success) },
                enabled = state.step != AbsenStep.SUBMITTING
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Tutup")
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!granted) {
                PermissionContent(
                    onRequest = requestPermissions,
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                )
            } else {
                when (state.step) {
                    AbsenStep.LOCATING -> CenterLoading("Mencari lokasi…")

                    AbsenStep.SUBMITTING -> CenterLoading("Mengirim absen…")
                    AbsenStep.CHECKING -> CenterLoading("Memeriksa kualitas foto…")

                    AbsenStep.LOCATION_ERROR -> LocationErrorContent(
                        state = state,
                        namaLokasi = request.profil.namaLokasi,
                        onRetry = vm::locate
                    )

                                        AbsenStep.CAMERA -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LocationBanner(state)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val cameraModifier = Modifier
                                .aspectRatio(3f / 4f)
                                .clip(MaterialTheme.shapes.extraLarge)
                            if (manual) {
                                CameraCapture(
                                    onPhoto = vm::onPhotoCaptured,
                                    onFailure = vm::onCameraError,
                                    modifier = cameraModifier
                                )
                            } else {
                                FaceCamera(
                                    manualAllowed = request.profil.faceRequired != "1",
                                    onPhoto = vm::onPhotoCaptured,
                                    onFailure = vm::onCameraError,
                                    onManual = { manual = true },
                                    modifier = cameraModifier
                                )
                            }
                        }
                        if (state.message != null) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    AbsenStep.REVIEW -> ReviewContent(
                        state = state,
                        onRetake = vm::retake,
                        onSubmit = vm::submit
                    )

                    AbsenStep.DONE -> DoneContent(
                        state = state,
                        onFinish = { onClose(state.success) },
                        onRetry = vm::backToReview
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterLoading(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        CircularProgressIndicator()
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PermissionContent(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("Izin diperlukan", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Absen memerlukan izin Lokasi (pilih \"Lokasi tepat\") dan Kamera " +
                "untuk memastikan kamu berada di area absen dan mengambil foto.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRequest) { Text("Berikan izin") }
        TextButton(onClick = onOpenSettings) { Text("Buka pengaturan aplikasi") }
    }
}

@Composable
private fun LocationErrorContent(
    state: AbsenUiState,
    namaLokasi: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            Icons.Filled.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = state.problem ?: "Lokasi tidak dapat diverifikasi.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (state.problemJarak != null) {
            Text(
                text = "Jarak kamu ±${state.problemJarak} m dari ${namaLokasi ?: "lokasi absen"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Button(onClick = onRetry) { Text("Cek ulang") }
    }
}

@Composable
private fun LocationBanner(state: AbsenUiState) {
    val loc = state.loc
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null)
            Column {
                Text(
                    text = loc?.message ?: "Lokasi terverifikasi",
                    style = MaterialTheme.typography.titleMedium
                )
                val info = listOfNotNull(
                    loc?.jarak?.let { "jarak ±$it m" },
                    loc?.radius?.let { "radius $it m" },
                    loc?.accuracy?.let { "akurasi ±${it.roundToInt()} m" },
                    if (state.verifying) "memperbarui…" else null
                ).joinToString(" • ")
                if (info.isNotEmpty()) {
                    Text(info, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ReviewContent(
    state: AbsenUiState,
    onRetake: () -> Unit,
    onSubmit: () -> Unit
) {
    val photo = state.photo
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo) {
        value = photo?.let { f ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "Foto absen",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(3f / 4f)
                        .clip(MaterialTheme.shapes.extraLarge)
                )
            } else {
                CircularProgressIndicator()
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) { Text("Ulangi foto") }
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) { Text("Kirim absen") }
        }
    }
}

@Composable
private fun DoneContent(
    state: AbsenUiState,
    onFinish: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = if (state.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = if (state.success) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
        Text(
            text = if (state.success) "Berhasil" else "Gagal",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = state.message ?: "",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (state.success) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Selesai") }
        } else {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Coba lagi") }
            OutlinedButton(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Tutup") }
        }
    }
}
