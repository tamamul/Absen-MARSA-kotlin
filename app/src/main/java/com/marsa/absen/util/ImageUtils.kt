package com.marsa.absen.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

private fun exifMatrix(orientation: Int): Matrix {
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(-90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
    }
    return m
}

/**
 * Hasil akhir selalu POTRET [outW]x[outH] (default 360x480) JPEG:
 * putar sesuai EXIF, potong rasio 3:4 dari tengah, perkecil.
 * File asli dihapus; mengembalikan file hasil.
 */
suspend fun preparePortraitPhoto(
    src: File,
    outW: Int = 360,
    outH: Int = 480,
    quality: Int = 85
): File = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(src.path, bounds)

    var sample = 1
    while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= outH) sample *= 2

    val decoded = BitmapFactory.decodeFile(
        src.path,
        BitmapFactory.Options().apply { inSampleSize = sample }
    ) ?: return@withContext src

    val orientation = runCatching {
        ExifInterface(src.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val oriented = Bitmap.createBitmap(
        decoded, 0, 0, decoded.width, decoded.height, exifMatrix(orientation), true
    )

    // Potong ke rasio potret (lebar:tinggi = outW:outH) dari tengah
    val target = outW.toFloat() / outH
    val ratio = oriented.width.toFloat() / oriented.height
    val cw: Int
    val ch: Int
    if (ratio > target) {
        ch = oriented.height
        cw = (ch * target).roundToInt()
    } else {
        cw = oriented.width
        ch = (cw / target).roundToInt()
    }
    val x = ((oriented.width - cw) / 2).coerceAtLeast(0)
    val y = ((oriented.height - ch) / 2).coerceAtLeast(0)
    val cropped = Bitmap.createBitmap(
        oriented, x, y,
        cw.coerceAtMost(oriented.width - x),
        ch.coerceAtMost(oriented.height - y)
    )
    val scaled = Bitmap.createScaledBitmap(cropped, outW, outH, true)

    val dst = File(src.parentFile, "absen_${System.currentTimeMillis()}.jpg")
    FileOutputStream(dst).use { scaled.compress(Bitmap.CompressFormat.JPEG, quality, it) }

    src.delete()
    dst
}
