package com.example.ballforger.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

fun uriToCompressedByteArray(context: Context, uri: Uri): ByteArray? {
    // ... (Giữ nguyên code cũ của hàm này)
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        val maxDim = 256
        val ratio = Math.min(maxDim.toFloat() / originalBitmap.width, maxDim.toFloat() / originalBitmap.height)
        val width = Math.round(ratio * originalBitmap.width)
        val height = Math.round(ratio * originalBitmap.height)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// THÊM HÀM MỚI NÀY:
suspend fun urlToCompressedByteArray(urlString: String): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val inputStream = connection.inputStream
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        if (originalBitmap == null) return@withContext null

        val maxDim = 256
        val ratio = Math.min(maxDim.toFloat() / originalBitmap.width, maxDim.toFloat() / originalBitmap.height)
        val width = Math.round(ratio * originalBitmap.width)
        val height = Math.round(ratio * originalBitmap.height)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}