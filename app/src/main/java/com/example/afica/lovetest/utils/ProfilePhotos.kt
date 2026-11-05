package com.example.afica.lovetest.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

object ProfilePhotos {
    fun importPhoto(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Invalid image")
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 1600) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Cannot decode image")
        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        val corrected = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        val directory = File(context.filesDir, "profile_photos").apply { mkdirs() }
        val output = File(directory, UUID.randomUUID().toString() + ".png")
        try {
            output.outputStream().use {
                if (!corrected.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Cannot save image")
            }
        } finally {
            if (corrected !== original) corrected.recycle()
            original.recycle()
        }
        return output.absolutePath
    }
}
