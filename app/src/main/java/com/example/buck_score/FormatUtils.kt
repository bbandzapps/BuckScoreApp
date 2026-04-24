package com.example.buck_score

import android.graphics.Bitmap
import kotlin.math.roundToInt

fun doubleToBC(value: Double): String {
    val whole = value.toInt()
    val frac = ((value - whole) * 8).roundToInt()

    return when (frac) {
        0 -> "$whole"
        8 -> "${whole + 1}"
        else -> "$whole $frac/8"
    }
}

fun scaleBitmap(
    bitmap: Bitmap,
    maxWidth: Int,
    maxHeight: Int
): Bitmap {
    val ratio = minOf(
        maxWidth.toFloat() / bitmap.width,
        maxHeight.toFloat() / bitmap.height
    )

    val newWidth = (bitmap.width * ratio).toInt()
    val newHeight = (bitmap.height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}