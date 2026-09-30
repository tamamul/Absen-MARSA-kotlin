package com.marsa.absen.ui.screen.absen.face

import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Kamera depan dengan deteksi wajah + liveness + auto capture.
 * Foto disimpan ke file cache lalu diserahkan lewat onPhoto.
 */
@Composable
fun FaceCamera(
    manualAllowed: Boolean,
    onPhoto: (File) -> Unit,
    onFailure: (String) -> Unit,
    onManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val engine = remember { LivenessEngine() }

    var ui by remember {
        mutableStateOf(LivenessUi())
    }

    val captureGuard = remember {
        AtomicBoolean(false)
    }

    var showMonitor by rememberSaveable {
        mutableStateOf(true)
    }

    var showManual by remember {
        mutableStateOf(false)
    }

    val currentOnPhoto by rememberUpdatedState(onPhoto)
    val currentOnFailure by rememberUpdatedState(onFailure)

    val cam = remember {
        LifecycleCameraController(context).apply {

            cameraSelector =
                CameraSelector.DEFAULT_FRONT_CAMERA

            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                    CameraController.IMAGE_ANALYSIS
            )

            imageAnalysisBackpressureStrategy =
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    DisposableEffect(lifecycleOwner) {

        val executor =
            Executors.newSingleThreadExecutor()

        val analyzer = FaceAnalyzer { metrics ->

            val next = engine.onFrame(
                metrics,
                SystemClock.elapsedRealtime()
            )

            ui = next

            if (
                next.readyToCapture &&
                captureGuard.compareAndSet(false, true)
            ) {

                val file = File.createTempFile(
                    "absen_",
                    ".jpg",
                    context.cacheDir
                )

                cam.takePicture(
                    ImageCapture.OutputFileOptions.Builder(
                        file
                    ).build(),

                    ContextCompat.getMainExecutor(context),

                    object : ImageCapture.OnImageSavedCallback {

                        override fun onImageSaved(
                            output: ImageCapture.OutputFileResults
                        ) {
                            currentOnPhoto(file)
                        }

                        override fun onError(
                            exception: ImageCaptureException
                        ) {

                            file.delete()

                            captureGuard.set(false)

                            engine.reset()

                            currentOnFailure(
                                "Gagal mengambil foto: ${exception.message}"
                            )
                        }
                    }
                )
            }
        }

        cam.setImageAnalysisAnalyzer(
            executor,
            analyzer
        )

        cam.bindToLifecycle(
            lifecycleOwner
        )

        onDispose {

            cam.clearImageAnalysisAnalyzer()

            cam.unbind()

            analyzer.close()

            executor.shutdown()
        }
    }

    /*
     * Tombol foto manual baru muncul setelah 25 detik
     * jika server mengizinkan.
     */
    LaunchedEffect(Unit) {

        delay(25_000)

        showManual = true
    }

    /*
     * ==================================================
     * LAYOUT UTAMA
     * ==================================================
     *
     * Kamera berada di atas.
     * Monitor berada DI LUAR area kamera.
     * Jadi monitor tidak menutupi wajah.
     */
Column(
    modifier = modifier
) {

    // =========================
    // AREA KAMERA — UKURAN TETAP
    // =========================
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
    ) {

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cam
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        FaceOverlay(
            ui = ui,
            modifier = Modifier.fillMaxSize()
        )

        // =========================
        // INSTRUKSI + TAHAP + PROGRESS
        // =========================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            val headline =
                when (ui.phase) {
                    Phase.CHALLENGE ->
                        ui.hint +
                            (
                                ui.secondsLeft?.let {
                                    "  (${it}d)"
                                } ?: ""
                            )

                    else -> ui.hint
                }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = headline,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Text(
                    text = stageLabel(ui),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 4.dp
                    )
                )
            }

            LinearProgressIndicator(
                progress = { ui.overall },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
            )
        }
    }

    // =========================
    // MONITOR
    // =========================
    if (showMonitor) {
        MonitorPanel(ui)
    }

    // =========================
    // TOMBOL
    // =========================
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        TextButton(
            onClick = {
                showMonitor = !showMonitor
            }
        ) {
            Text(
                text = if (showMonitor) {
                    "Sembunyikan monitor"
                } else {
                    "Tampilkan monitor"
                },
                color = Color.White
            )
        }

        if (manualAllowed && showManual) {
            TextButton(
                onClick = onManual
            ) {
                Text(
                    text = "Foto manual",
                    color = Color.White
                )
            }
        }
    }
}
}
/*
 * =========================
 * STAGE LABEL
 * =========================
 */
private fun stageLabel(
    ui: LivenessUi
): String {

    return when (ui.phase) {

        Phase.ALIGN ->
            "Tahap 1/3 • Posisi wajah"

        Phase.CHALLENGE ->
            "Tahap 2/3 • Tantangan liveness (percobaan ${ui.attempt})"

        Phase.HOLD ->
            "Tahap 3/3 • Tahan posisi"

        Phase.CAPTURING ->
            "Mengambil foto…"
    }
}

/*
 * =========================
 * FACE OVERLAY
 * =========================
 */
@Composable
private fun FaceOverlay(
    ui: LivenessUi,
    modifier: Modifier = Modifier
) {

    val good =
        Color(0xFF4CAF50)

    val warn =
        Color(0xFFFFC107)

    val color =
        when {

            ui.checks.allOk ->
                good

            ui.metrics.faceCount > 0 ->
                warn

            else ->
                Color.White
        }

    Canvas(
        modifier = modifier
    ) {

        /*
         * Hanya kotak pelacak wajah.
         * Oval panduan dihapus.
         */
        ui.metrics.box?.let { b ->

            val left =
                (1f - b.right) *
                    size.width

            val right =
                (1f - b.left) *
                    size.width

            drawRoundRect(

                color =
                    color.copy(alpha = 0.9f),

                topLeft =
                    Offset(
                        left,
                        b.top * size.height
                    ),

                size =
                    Size(
                        right - left,
                        (b.bottom - b.top) *
                            size.height
                    ),

                cornerRadius =
                    CornerRadius(
                        16.dp.toPx()
                    ),

                style =
                    Stroke(
                        width =
                            2.dp.toPx()
                    )
            )
        }
    }
}

/*
 * =========================
 * PERCENTAGE
 * =========================
 */
private fun pct(
    v: Float?
): String {

    return v?.let {

        "${(it * 100).roundToInt()}%"

    } ?: "--"
}

/*
 * =========================
 * MONITOR PANEL
 * =========================
 */
@Composable
private fun MonitorPanel(
    ui: LivenessUi
) {

    val m =
        ui.metrics

    val c =
        ui.checks

    fun Boolean.mark(): String {
        return if (this) {
            "✓"
        } else {
            "✗"
        }
    }

    val lines = listOf(
    "Wajah ${m.faceCount}" +
        "${m.trackingId?.let { " (ID $it)" } ?: ""}" +
        " • ${ui.fps} fps • " +
        "Ukuran ${((m.box?.height ?: 0f) * 100).roundToInt()}% ${c.size.mark()} • " +
        "Tengah ${c.centered.mark()} • " +
        "Lurus ${c.frontal.mark()}",

    "Mata ${c.eyesOpen.mark()} • " +
        "Stabil ${c.stable.mark()} • " +
        "Yaw ${m.yaw.roundToInt()}° • " +
        "Pitch ${m.pitch.roundToInt()}° • " +
        "Roll ${m.roll.roundToInt()}° • " +
        "Mata L ${pct(m.leftEye)} • " +
        "R ${pct(m.rightEye)}"
)

    Surface(
        shape =
            RoundedCornerShape(16.dp),

        color =
            Color.Black.copy(alpha = 0.6f)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),

            verticalArrangement =
                Arrangement.spacedBy(2.dp)
        ) {

            lines.forEach { line ->

                Text(
                    text = line,

                    color = Color.White,

                    fontFamily =
                        FontFamily.Monospace,

                    fontSize = 8.sp
                )
            }
        }
    }
}
