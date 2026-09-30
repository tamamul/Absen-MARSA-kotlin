package com.marsa.absen.ui.screen.absen.face

/** Kotak wajah dalam koordinat ternormalisasi 0..1 (belum dicerminkan). */
data class NormBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val cx: Float get() = (left + right) / 2f
    val cy: Float get() = (top + bottom) / 2f
    val height: Float get() = bottom - top
}

/** Data mentah satu frame dari ML Kit. */
data class FaceMetrics(
    val faceCount: Int = 0,
    val box: NormBox? = null,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val leftEye: Float? = null,
    val rightEye: Float? = null,
    val smile: Float? = null,
    val trackingId: Int? = null
) {
    val eyesOpen: Float?
        get() = when {
            leftEye != null && rightEye != null -> (leftEye + rightEye) / 2f
            else -> leftEye ?: rightEye
        }

    /** Perkiraan ekspresi dari senyum dan mata (bukan model emosi sungguhan). */
    val expression: String
        get() = when {
            faceCount == 0 -> "—"
            (smile ?: 0f) > 0.7f -> "😄 Senang"
            (smile ?: 0f) > 0.35f -> "🙂 Sedikit tersenyum"
            (eyesOpen ?: 1f) < 0.3f -> "😴 Mata terpejam"
            else -> "😐 Netral"
        }
}

data class Checks(
    val single: Boolean = false,
    val size: Boolean = false,
    val centered: Boolean = false,
    val frontal: Boolean = false,
    val eyesOpen: Boolean = false,
    val stable: Boolean = false
) {
    val readyToStart: Boolean get() = single && size && centered && frontal && eyesOpen
    val allOk: Boolean get() = readyToStart && stable
}

enum class Phase { ALIGN, CHALLENGE, HOLD, CAPTURING }

enum class Challenge(val instruction: String, val emoji: String) {
    BLINK("Kedipkan mata", "😉"),
    TURN_LEFT("Toleh ke kiri", "⬅️"),
    TURN_RIGHT("Toleh ke kanan", "➡️"),
    SMILE("Tersenyum lebar", "😄")
}

data class LivenessUi(
    val phase: Phase = Phase.ALIGN,
    val challenge: Challenge? = null,
    val hint: String = "Posisikan wajah di dalam oval",
    val progress: Float = 0f,
    val overall: Float = 0f,
    val checks: Checks = Checks(),
    val metrics: FaceMetrics = FaceMetrics(),
    val secondsLeft: Int? = null,
    val attempt: Int = 1,
    val fps: Int = 0,
    val readyToCapture: Boolean = false
)
