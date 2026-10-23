package com.kyant.backdrop.catalog.utils

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.asSkikoRuntimeShader
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.isRuntimeShaderSupported

actual fun SdfShader(imageBitmap: ImageBitmap): SdfShader {
    return SdfShaderImpl(imageBitmap)
}

actual fun ImageBitmapFromPixels(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bitmap = org.jetbrains.skia.Bitmap()
    bitmap.allocN32Pixels(width, height)
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val argb = pixels[i]
        bytes[i * 4 + 0] = (argb and 0xFF).toByte()
        bytes[i * 4 + 1] = ((argb shr 8) and 0xFF).toByte()
        bytes[i * 4 + 2] = ((argb shr 16) and 0xFF).toByte()
        bytes[i * 4 + 3] = ((argb shr 24) and 0xFF).toByte()
    }
    bitmap.installPixels(bytes)
    return bitmap.asComposeImageBitmap()
}

private var cachedSkiaTypeface: org.jetbrains.skia.Typeface? = null

actual fun renderClockSdfBitmapCpu(
    text: String,
    fontHeight: Float,
    fontWidth: Float,
    fontWeight: Float,
    fontSoftness: Float,
    fontBytes: ByteArray
): ImageBitmap {
    val baseTypeface = cachedSkiaTypeface ?: run {
        val data = org.jetbrains.skia.Data.makeFromBytes(fontBytes)
        val tf = org.jetbrains.skia.FontMgr.default.makeFromData(data) ?: org.jetbrains.skia.Typeface.makeEmpty()
        cachedSkiaTypeface = tf
        tf
    }

    val variations = arrayOf(
        org.jetbrains.skia.FontVariation("HGHT", fontHeight),
        org.jetbrains.skia.FontVariation("wdth", fontWidth),
        org.jetbrains.skia.FontVariation("wght", fontWeight),
        org.jetbrains.skia.FontVariation("SOFT", fontSoftness)
    )
    val typeface = baseTypeface.makeClone(variations)

    val font = org.jetbrains.skia.Font(typeface, 260f)
    val bounds = font.measureText(text)
    val textW = bounds.width.toInt()
    val textH = bounds.height.toInt()

    val paddingX = maxOf(60, textW / 6)
    val paddingY = maxOf(80, textH / 3)

    val width = textW + paddingX * 2
    val height = textH + paddingY * 2

    val bitmap = org.jetbrains.skia.Bitmap()
    bitmap.allocN32Pixels(width, height)
    val canvas = org.jetbrains.skia.Canvas(bitmap)

    val paint = org.jetbrains.skia.Paint().apply {
        color = 0xFFFFFFFF.toInt()
    }

    val x = paddingX.toFloat() - bounds.left
    val y = paddingY.toFloat() - bounds.top
    canvas.drawString(text, x, y, font, paint)

    val pixels = IntArray(width * height)
    val bytes = bitmap.readPixels(bitmap.imageInfo, width * 4, 0, 0)!!
    for (i in pixels.indices) {
        val b = bytes[i * 4 + 0].toInt() and 0xFF
        val g = bytes[i * 4 + 1].toInt() and 0xFF
        val r = bytes[i * 4 + 2].toInt() and 0xFF
        val a = bytes[i * 4 + 3].toInt() and 0xFF
        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    val sdfPixels = generateSdfPixelsFromMask(pixels, width, height)
    return ImageBitmapFromPixels(sdfPixels, width, height)
}

@Immutable
private class SdfShaderImpl(val sdfBitmap: ImageBitmap) : SdfShader {

    private val sdfTexture = sdfBitmap.asSkiaBitmap().makeShader()

    override val width: Int
        get() = sdfBitmap.width

    override val height: Int
        get() = sdfBitmap.height

    override fun BackdropEffectScope.apply(
        refractionHeight: Float,
        lightAngle: Float,
    ) {
        if (isRuntimeShaderSupported()) {
            runtimeShaderEffect("SdfShader", SdfShaderString, "content") {
                asSkikoRuntimeShader().child("sdfTex", sdfTexture)
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("sdfTexSize", sdfBitmap.width.toFloat(), sdfBitmap.height.toFloat())
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("lightAngle", lightAngle)
            }
        }
    }
}
