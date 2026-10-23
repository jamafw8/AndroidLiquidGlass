package com.kyant.backdrop.catalog.utils

import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.asAndroidRuntimeShader
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.isRuntimeShaderSupported

actual fun SdfShader(imageBitmap: ImageBitmap): SdfShader {
    return SdfShaderImpl(imageBitmap)
}

actual fun ImageBitmapFromPixels(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888)
    return bitmap.asImageBitmap()
}

private var cachedFontFile: java.io.File? = null

actual fun renderClockSdfBitmapCpu(
    text: String,
    fontHeight: Float,
    fontWidth: Float,
    fontWeight: Float,
    fontSoftness: Float,
    fontBytes: ByteArray
): ImageBitmap {
    val file = cachedFontFile ?: run {
        val temp = java.io.File.createTempFile("sf_numeric", ".ttf")
        temp.deleteOnExit()
        temp.writeBytes(fontBytes)
        cachedFontFile = temp
        temp
    }

    val settings = "'HGHT' ${fontHeight.toInt()}, 'wdth' ${fontWidth.toInt()}, 'wght' ${fontWeight.toInt()}, 'SOFT' ${fontSoftness.toInt()}"
    val typeface = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        android.graphics.Typeface.Builder(file)
            .setFontVariationSettings(settings)
            .build()
    } else {
        android.graphics.Typeface.DEFAULT
    }

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.textSize = 260f
        this.color = android.graphics.Color.WHITE
    }

    val bounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val textW = paint.measureText(text).toInt()
    val textH = bounds.height()

    val paddingX = maxOf(60, textW / 6)
    val paddingY = maxOf(80, textH / 3)

    val width = textW + paddingX * 2
    val height = textH + paddingY * 2

    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val x = paddingX.toFloat()
    val y = paddingY.toFloat() - bounds.top
    canvas.drawText(text, x, y, paint)

    val maskPixels = IntArray(width * height)
    bitmap.getPixels(maskPixels, 0, width, 0, 0, width, height)
    bitmap.recycle()

    val sdfPixels = generateSdfPixelsFromMask(maskPixels, width, height)
    return ImageBitmapFromPixels(sdfPixels, width, height)
}

@Immutable
private class SdfShaderImpl(val sdfBitmap: ImageBitmap) : SdfShader {

    private val sdfTexture =
        BitmapShader(sdfBitmap.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

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
                asAndroidRuntimeShader().setInputBuffer("sdfTex", sdfTexture)
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("sdfTexSize", sdfBitmap.width.toFloat(), sdfBitmap.height.toFloat())
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("lightAngle", lightAngle)
            }
        }
    }
}
