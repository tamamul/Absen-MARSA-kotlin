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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextOverflow

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
    var ui by remember { mutableStateOf(LivenessUi()) }
    val captureGuard = remember { AtomicBoolean(false) }
    var showMonitor by rememberSaveable { mutableStateOf(true) }
    var showManual by remember { mutableStateOf(false) }
    val currentOnPhoto by rememberUpdatedState(onPhoto)
    val currentOnFailure by rememberUpdatedState(onFailure)

    val cam = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                    CameraController.IMAGE_ANALYSIS
            )
            imageAnalysisBackpressureStrategy =
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()

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
                    ImageCapture.OutputFileOptions.Builder(file).build(),
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

        cam.setImageAnalysisAnalyzer(executor, analyzer)
        cam.bindToLifecycle(lifecycleOwner)

        onDispose {
            cam.clearImageAnalysisAnalyzer()
            cam.unbind()
            analyzer.close()
            executor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        delay(25_000)
        showManual = true
    }

   Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(20.dp))
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

        // Header: petunjuk + tahap + progress
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = when (ui.phase) {
                        Phase.CHALLENGE ->
                            ui.hint + (ui.secondsLeft?.let { " • ${it}d" } ?: "")
                        else -> ui.hint
                    },
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.4f)
            ) {
                Text(
                    text = stageLabel(ui),
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 3.dp
                    )
                )
            }

            LinearProgressIndicator(
                progress = { ui.overall },
                color = Color(0xFF4CAF50),
                trackColor = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .clip(CircleShape)
            )
        }

        // Monitor sebagai overlay di bawah kamera
        if (showMonitor) {
            MonitorPanel(
                ui = ui,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { showMonitor = !showMonitor }) {
            Text(
                text = if (showMonitor) "Sembunyikan monitor" else "Tampilkan monitor",
                fontSize = 13.sp
            )
        }

        if (manualAllowed && showManual) {
            TextButton(onClick = onManual) {
                Text(text = "Foto manual", fontSize = 13.sp)
            }
        }
    }
}
    }
}

private fun stageLabel(ui: LivenessUi): String {
    return when (ui.phase) {
        Phase.ALIGN -> "Tahap 1/3 • Posisi wajah"
        Phase.CHALLENGE -> "Tahap 2/3 • Liveness"
        Phase.HOLD -> "Tahap 3/3 • Tahan posisi"
        Phase.CAPTURING -> "Mengambil foto…"
    }
}

@Composable
private fun FaceOverlay(
    ui: LivenessUi,
    modifier: Modifier = Modifier
) {
    val color = when {
        ui.checks.allOk -> Color(0xFF4CAF50)
        ui.metrics.faceCount > 0 -> Color(0xFFFFC107)
        else -> Color.White
    }

    Canvas(modifier = modifier) {
        // Oval panduan di tengah (sedikit ke atas)
        val ovalW = size.width * 0.68f
        val ovalH = ovalW * 1.3f
        val ovalRect = Rect(
            offset = Offset(
                (size.width - ovalW) / 2f,
                (size.height - ovalH) / 2f - size.height * 0.04f
            ),
            size = Size(ovalW, ovalH)
        )

        // Area gelap di luar oval
        val hole = Path().apply { addOval(ovalRect) }
        clipPath(hole, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.35f))
        }

        drawOval(
            color = color,
            topLeft = ovalRect.topLeft,
            size = ovalRect.size,
            style = Stroke(3.dp.toPx())
        )

        // Kotak deteksi wajah (mirror horizontal untuk kamera depan)
        ui.metrics.box?.let { b ->
            val left = (1f - b.right) * size.width
            val right = (1f - b.left) * size.width

            drawRoundRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(left, b.top * size.height),
                size = Size(right - left, (b.bottom - b.top) * size.height),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(1.5.dp.toPx())
            )
        }
    }
}

private fun pct(v: Float?): String {
    return v?.let {
        "${(it * 100).roundToInt()}%"
    } ?: "--"
}

@Composable
private fun MonitorPanel(
    ui: LivenessUi,
    modifier: Modifier = Modifier
) {
    val m = ui.metrics
    val c = ui.checks

    fun Boolean.mark() = if (this) "✓" else "✗"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            MonitorLine(
                "Wajah ${m.faceCount}" +
                    (m.trackingId?.let { " • ID $it" } ?: "") +
                    " • ${ui.fps} fps"
            )
            MonitorLine(
                "Ukuran ${((m.box?.height ?: 0f) * 100).roundToInt()}% ${c.size.mark()}" +
                    " • Tengah ${c.centered.mark()}" +
                    " • Lurus ${c.frontal.mark()}"
            )
            MonitorLine(
                "Mata ${c.eyesOpen.mark()} • Stabil ${c.stable.mark()}" +
                    " • L ${pct(m.leftEye)} • R ${pct(m.rightEye)}"
            )
            MonitorLine(
                "Yaw ${m.yaw.roundToInt()}° • Pitch ${m.pitch.roundToInt()}°" +
                    " • Roll ${m.roll.roundToInt()}°"
            )
        }
    }
}

@Composable
private fun MonitorLine(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
