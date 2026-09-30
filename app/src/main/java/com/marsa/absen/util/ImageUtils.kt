package com.marsa.absen.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Memutar sesuai EXIF, memperkecil sisi terpanjang ke [maxSide] px, simpan sebagai JPEG.
 * File asli dihapus; mengembalikan file hasil.
 */
suspend fun compressPhoto(src: File, maxSide: Int = 1280, quality: Int = 80): File =
    withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.path, bounds)

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

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

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val scale = maxSide.toFloat() / max(decoded.width, decoded.height)
        if (scale < 1f) matrix.postScale(scale, scale)

        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val dst = File(src.parentFile, "absen_${System.currentTimeMillis()}.jpg")
        FileOutputStream(dst).use { result.compress(Bitmap.CompressFormat.JPEG, quality, it) }

        if (decoded !== result) decoded.recycle()
        src.delete()
        dst
    }
