package com.example.buck_score

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

fun saveImage(context: Context, bitmap: Bitmap): String {
    val file = File(context.filesDir, "img_${System.currentTimeMillis()}.png")
    val stream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.close()
    return file.absolutePath
}
