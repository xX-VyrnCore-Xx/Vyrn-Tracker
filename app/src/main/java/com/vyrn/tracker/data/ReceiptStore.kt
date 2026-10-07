package com.vyrn.tracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Foto delle ricevute: copiate (ridimensionate e ruotate) nella memoria privata dell'app. */
object ReceiptStore {
    private const val MAX_SIDE = 2000

    private fun dir(ctx: Context): File = File(ctx.filesDir, "receipts").also { it.mkdirs() }

    fun file(ctx: Context, name: String): File = File(dir(ctx), name)

    fun delete(ctx: Context, name: String) {
        file(ctx, name).delete()
    }

    /** File temporaneo in cui l'app fotocamera scrive lo scatto. */
    fun cameraTarget(ctx: Context): File = File(File(ctx.cacheDir, "camera").also { it.mkdirs() }, "capture.jpg")

    fun cameraUri(ctx: Context): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", cameraTarget(ctx))

    /** Salva l'immagine di [uri] e restituisce il nome del file, oppure null se non leggibile. */
    suspend fun saveFrom(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
            val decoded = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext null
            val orientation = ctx.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val bitmap = if (degrees == 0f) decoded else
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
            val name = "${UUID.randomUUID()}.jpg"
            FileOutputStream(file(ctx, name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            name
        } catch (e: Exception) {
            null
        }
    }

    /** Carica la foto ridimensionata a [maxSide] pixel sul lato lungo, o null se il file non esiste. */
    fun loadBitmap(ctx: Context, name: String, maxSide: Int): Bitmap? = try {
        val f = file(ctx, name)
        if (!f.exists()) null else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    } catch (e: Exception) {
        null
    }

    fun purgeUnused(ctx: Context, referenced: Set<String>) {
        dir(ctx).listFiles()?.filter { it.name !in referenced }?.forEach { it.delete() }
    }
}
