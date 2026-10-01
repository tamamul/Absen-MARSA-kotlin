package com.marsa.absen.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

/** Ambang pemeriksaan foto. Ubah jika terlalu ketat (foto bagus ditolak) atau terlalu longgar. */
object QualityConfig {
    const val MIN_SHARPNESS = 30f     // varians Laplacian pada area wajah
    const val MIN_BRIGHTNESS = 50f    // rata-rata terang 0..255
    const val MAX_BRIGHTNESS = 215f
    const val MIN_FACE_HEIGHT = 0.30f // tinggi wajah / tinggi foto
    const val MIN_EYE_OPEN = 0.4f
    const val MAX_YAW = 20f
}

data class QualityReport(
    val ok: Boolean,
    val sharpness: Float,
    val brightness: Float,
    val problems: List<String>
)

suspend fun assessPhoto(file: File): QualityReport = withContext(Dispatchers.Default) {
    val bmp = BitmapFactory.decodeFile(file.path)
        ?: return@withContext QualityReport(false, 0f, 0f, listOf("foto tidak dapat dibaca"))

    val w = bmp.width
    val h = bmp.height
    val pixels = IntArray(w * h)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)
    val luma = IntArray(w * h) { i ->
        val p = pixels[i]
        (0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)).toInt()
    }

    val problems = mutableListOf<String>()
    var region = Rect(0, 0, w, h)

    // Jika detektor gagal dijalankan (null), pemeriksaan wajah dilewati; ketajaman & cahaya tetap dicek.
    val faces = detectFaces(bmp)
    if (faces != null) {
        when {
            faces.isEmpty() -> problems += "wajah tidak terdeteksi jelas"
            faces.size > 1 -> problems += "terdeteksi lebih dari satu wajah"
            else -> {
                val f = faces[0]
                val box = f.boundingBox
                region = expand(box, w, h, 0.10f)
                if (box.height().toFloat() / h < QualityConfig.MIN_FACE_HEIGHT) {
                    problems += "wajah terlalu kecil/jauh"
                }
                val eyes = listOfNotNull(f.leftEyeOpenProbability, f.rightEyeOpenProbability)
                if (eyes.isNotEmpty() && eyes.average() < QualityConfig.MIN_EYE_OPEN) {
                    problems += "mata tertutup"
                }
                if (abs(f.headEulerAngleY) > QualityConfig.MAX_YAW) {
                    problems += "wajah tidak menghadap lurus"
                }
            }
        }
    }

    val sharp = laplacianVariance(luma, w, region)
    val bright = meanLuma(luma, w, region)
    if (sharp < QualityConfig.MIN_SHARPNESS) {
        problems += "foto buram (ketajaman ${sharp.roundToInt()}, minimal ${QualityConfig.MIN_SHARPNESS.roundToInt()})"
    }
    if (bright < QualityConfig.MIN_BRIGHTNESS) problems += "terlalu gelap"
    if (bright > QualityConfig.MAX_BRIGHTNESS) problems += "terlalu terang/silau"

    bmp.recycle()
    QualityReport(problems.isEmpty(), sharp, bright, problems)
}

private suspend fun detectFaces(bmp: Bitmap): List<Face>? {
    val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )
    return try {
        detector.process(InputImage.fromBitmap(bmp, 0)).awaitResult()
    } finally {
        detector.close()
    }
}

private suspend fun <T> Task<T>.awaitResult(): T? = suspendCancellableCoroutine<T?> { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resume(null) }
}

private fun expand(r: Rect, w: Int, h: Int, f: Float): Rect {
    val dx = (r.width() * f).toInt()
    val dy = (r.height() * f).toInt()
    return Rect(
        (r.left - dx).coerceAtLeast(0),
        (r.top - dy).coerceAtLeast(0),
        (r.right + dx).coerceAtMost(w),
        (r.bottom + dy).coerceAtMost(h)
    )
}

/** Varians Laplacian: makin kecil makin buram. */
private fun laplacianVariance(g: IntArray, w: Int, r: Rect): Float {
    var sum = 0.0
    var sumSq = 0.0
    var n = 0
    for (y in (r.top + 1) until (r.bottom - 1)) {
        for (x in (r.left + 1) until (r.right - 1)) {
            val i = y * w + x
            val v = 4 * g[i] - g[i - 1] - g[i + 1] - g[i - w] - g[i + w]
            sum += v
            sumSq += v.toDouble() * v
            n++
        }
    }
    if (n == 0) return 0f
    val mean = sum / n
    return (sumSq / n - mean * mean).toFloat()
}

private fun meanLuma(g: IntArray, w: Int, r: Rect): Float {
    var sum = 0L
    var n = 0
    for (y in r.top until r.bottom) {
        for (x in r.left until r.right) {
            sum += g[y * w + x]
            n++
        }
    }
    return if (n == 0) 0f else sum.toFloat() / n
}
