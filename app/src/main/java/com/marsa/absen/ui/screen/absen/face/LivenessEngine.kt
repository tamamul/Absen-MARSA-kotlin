package com.marsa.absen.ui.screen.absen.face

import kotlin.math.abs
import kotlin.math.hypot

/** Semua ambang batas ada di sini; ubah jika terlalu ketat atau longgar di HP-mu. */
object LivenessConfig {
    const val MIN_FACE = 0.30f          // tinggi wajah / tinggi frame
    const val MAX_FACE = 0.80f
    const val ALIGN_MS = 600L           // wajah harus pas selama ini sebelum tantangan
    const val CHALLENGE_MS = 8_000L     // batas waktu tantangan
    const val HOLD_MS = 900L            // tahan lurus & diam sebelum foto diambil
    const val HOLD_TIMEOUT_MS = 12_000L
    const val LOST_MS = 2_000L          // wajah hilang selama ini -> ulang dari posisi
    const val NOTICE_MS = 2_000L
    const val TURN_DEG = 20f

    /** +1 = menoleh ke KIRI (kiri pengguna) menghasilkan yaw positif. Ganti -1f jika terbalik. */
    const val YAW_LEFT_SIGN = 1f

    const val SMILE_ON = 0.8f
    const val EYE_OPEN = 0.6f
    const val EYE_CLOSED = 0.3f
    const val FRONT_YAW = 12f
    const val FRONT_PITCH = 15f
    const val FRONT_ROLL = 15f
}

class LivenessEngine {

    private var phase = Phase.ALIGN
    private var challenge: Challenge? = null
    private var lastChallenge: Challenge? = null
    private var attempt = 1

    private var alignSince = 0L
    private var challengeStart = 0L
    private var holdStart = 0L
    private var holdSince = 0L
    private var lostSince = 0L

    private var sawOpen = false
    private var sawClosed = false
    private var hits = 0

    private var stableFrames = 0
    private var prevCx = -1f
    private var prevCy = -1f

    private var ready = false
    private var hint = "Posisikan wajah di dalam oval"
    private var notice: String? = null
    private var noticeUntil = 0L

    private val frameTimes = ArrayDeque<Long>()

    fun reset() {
        phase = Phase.ALIGN
        challenge = null
        lastChallenge = null
        attempt = 1
        alignSince = 0L
        holdSince = 0L
        lostSince = 0L
        clearChallengeState()
        ready = false
        hint = "Posisikan wajah di dalam oval"
        notice = null
    }

    fun onFrame(m: FaceMetrics, now: Long): LivenessUi {
        frameTimes.addLast(now)
        while (frameTimes.isNotEmpty() && now - frameTimes.first() > 1000) frameTimes.removeFirst()

        val checks = evaluate(m)
        var progress = 0f

        when (phase) {
            Phase.ALIGN -> {
                if (checks.readyToStart) {
                    if (alignSince == 0L) alignSince = now
                } else {
                    alignSince = 0L
                }
                progress = if (alignSince == 0L) 0f
                else ((now - alignSince) / LivenessConfig.ALIGN_MS.toFloat()).coerceIn(0f, 1f)
                hint = positionHint(m, checks)
                if (alignSince != 0L && now - alignSince >= LivenessConfig.ALIGN_MS) {
                    phase = Phase.CHALLENGE
                    pickChallenge(now)
                }
            }

            Phase.CHALLENGE -> progress = stepChallenge(m, now)
            Phase.HOLD -> progress = stepHold(m, checks, now)
            Phase.CAPTURING -> progress = 1f
        }

        val overall = when (phase) {
            Phase.ALIGN -> 0.25f * progress
            Phase.CHALLENGE -> 0.25f + 0.35f * progress
            Phase.HOLD -> 0.60f + 0.35f * progress
            Phase.CAPTURING -> 1f
        }
        val secondsLeft = if (phase == Phase.CHALLENGE) {
            ((LivenessConfig.CHALLENGE_MS - (now - challengeStart) + 999) / 1000).toInt().coerceAtLeast(0)
        } else null

        return LivenessUi(
            phase = phase,
            challenge = challenge,
            hint = notice?.takeIf { now < noticeUntil } ?: hint,
            progress = progress,
            overall = overall,
            checks = checks,
            metrics = m,
            secondsLeft = secondsLeft,
            attempt = attempt,
            fps = frameTimes.size,
            readyToCapture = ready
        )
    }

    private fun evaluate(m: FaceMetrics): Checks {
        val b = m.box
        val single = m.faceCount == 1 && b != null
        val size = b != null && single && b.height in LivenessConfig.MIN_FACE..LivenessConfig.MAX_FACE
        val centered = b != null && abs(b.cx - 0.5f) < 0.18f && abs(b.cy - 0.48f) < 0.20f
        val frontal = abs(m.yaw) < LivenessConfig.FRONT_YAW &&
            abs(m.pitch) < LivenessConfig.FRONT_PITCH &&
            abs(m.roll) < LivenessConfig.FRONT_ROLL
        val eyes = m.eyesOpen?.let { it >= 0.5f } ?: true

        if (b != null) {
            stableFrames = if (prevCx >= 0f && hypot(b.cx - prevCx, b.cy - prevCy) < 0.02f) {
                stableFrames + 1
            } else 0
            prevCx = b.cx
            prevCy = b.cy
        } else {
            stableFrames = 0
            prevCx = -1f
        }
        return Checks(single, size, centered, frontal, eyes, stableFrames >= 3)
    }

    private fun stepChallenge(m: FaceMetrics, now: Long): Float {
        val c = challenge ?: return 0f

        if (m.faceCount != 1) {
            if (lostSince == 0L) lostSince = now
            if (now - lostSince > LivenessConfig.LOST_MS) {
                resetToAlign("Wajah hilang. Posisikan wajah lagi", now)
                return 0f
            }
        } else {
            lostSince = 0L
        }

        var progress = 0f
        var done = false
        when (c) {
            Challenge.BLINK -> {
                val e = m.eyesOpen
                if (e != null && m.faceCount == 1) {
                    if (e >= LivenessConfig.EYE_OPEN) {
                        if (sawClosed) done = true else sawOpen = true
                    } else if (e <= LivenessConfig.EYE_CLOSED && sawOpen) {
                        sawClosed = true
                    }
                }
                progress = when {
                    done -> 1f
                    sawClosed -> 0.6f
                    sawOpen -> 0.2f
                    else -> 0f
                }
            }

            Challenge.TURN_LEFT, Challenge.TURN_RIGHT -> {
                val sign = if (c == Challenge.TURN_LEFT) LivenessConfig.YAW_LEFT_SIGN
                else -LivenessConfig.YAW_LEFT_SIGN
                val y = m.yaw * sign
                progress = (y / LivenessConfig.TURN_DEG).coerceIn(0f, 1f)
                hits = if (y >= LivenessConfig.TURN_DEG) hits + 1 else 0
                done = hits >= 2
            }

            Challenge.SMILE -> {
                val s = m.smile ?: 0f
                progress = (s / LivenessConfig.SMILE_ON).coerceIn(0f, 1f)
                hits = if (s >= LivenessConfig.SMILE_ON) hits + 1 else 0
                done = hits >= 3
            }
        }

        if (done) {
            phase = Phase.HOLD
            holdSince = 0L
            holdStart = now
            hint = "Bagus! Sekarang lihat lurus ke kamera"
            return 1f
        }
        if (now - challengeStart > LivenessConfig.CHALLENGE_MS) {
            attempt++
            pickChallenge(now)
            notice = "Waktu habis. Coba: ${challenge?.instruction}"
            noticeUntil = now + LivenessConfig.NOTICE_MS
            return 0f
        }
        hint = c.instruction
        return progress
    }

    private fun stepHold(m: FaceMetrics, checks: Checks, now: Long): Float {
        if (now - holdStart > LivenessConfig.HOLD_TIMEOUT_MS) {
            resetToAlign("Waktu habis. Ulangi dari awal", now)
            return 0f
        }
        return if (checks.allOk) {
            if (holdSince == 0L) holdSince = now
            val p = ((now - holdSince) / LivenessConfig.HOLD_MS.toFloat()).coerceIn(0f, 1f)
            hint = "Tahan… jangan bergerak"
            if (p >= 1f) {
                phase = Phase.CAPTURING
                ready = true
                hint = "Mengambil foto…"
            }
            p
        } else {
            holdSince = 0L
            hint = positionHint(m, checks)
            0f
        }
    }

    private fun pickChallenge(now: Long) {
        val options = Challenge.entries.filter { it != lastChallenge }
        val next = options.random()
        challenge = next
        lastChallenge = next
        clearChallengeState()
        challengeStart = now
        lostSince = 0L
        hint = next.instruction
    }

    private fun clearChallengeState() {
        sawOpen = false
        sawClosed = false
        hits = 0
    }

    private fun resetToAlign(message: String, now: Long) {
        phase = Phase.ALIGN
        challenge = null
        alignSince = 0L
        holdSince = 0L
        lostSince = 0L
        clearChallengeState()
        notice = message
        noticeUntil = now + LivenessConfig.NOTICE_MS
    }

    private fun positionHint(m: FaceMetrics, c: Checks): String = when {
        m.faceCount == 0 -> "Wajah tidak terdeteksi. Hadapkan wajah ke kamera"
        m.faceCount > 1 -> "Pastikan hanya satu wajah di layar"
        !c.size -> if ((m.box?.height ?: 0f) < LivenessConfig.MIN_FACE) "Dekatkan wajah ke kamera"
        else "Jauhkan sedikit wajah dari kamera"
        !c.centered -> "Geser wajah ke tengah oval"
        !c.frontal -> "Hadap lurus ke kamera"
        !c.eyesOpen -> "Buka mata lebar-lebar"
        else -> "Tahan posisi…"
    }
}
