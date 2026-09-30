package com.marsa.absen.ui.screen.absen.face

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/** Menganalisis tiap frame kamera dengan ML Kit; hasil dikirim ke [onMetrics] (thread utama). */
class FaceAnalyzer(
    private val onMetrics: (FaceMetrics) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.2f)
            .enableTracking()
            .build()
    )

    @Volatile
    private var closed = false

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (closed || media == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val rotated = rotation == 90 || rotation == 270
        val w = if (rotated) imageProxy.height else imageProxy.width
        val h = if (rotated) imageProxy.width else imageProxy.height

        detector.process(InputImage.fromMediaImage(media, rotation))
            .addOnSuccessListener { faces ->
                if (!closed) onMetrics(toMetrics(faces, w, h))
            }
            .addOnFailureListener {
                if (!closed) onMetrics(FaceMetrics())
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        closed = true
        runCatching { detector.close() }
    }

    private fun toMetrics(faces: List<Face>, w: Int, h: Int): FaceMetrics {
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return FaceMetrics()
        val b = face.boundingBox
        return FaceMetrics(
            faceCount = faces.size,
            box = NormBox(
                left = b.left / w.toFloat(),
                top = b.top / h.toFloat(),
                right = b.right / w.toFloat(),
                bottom = b.bottom / h.toFloat()
            ),
            yaw = face.headEulerAngleY,
            pitch = face.headEulerAngleX,
            roll = face.headEulerAngleZ,
            leftEye = face.leftEyeOpenProbability,
            rightEye = face.rightEyeOpenProbability,
            smile = face.smilingProbability,
            trackingId = face.trackingId
        )
    }
}
