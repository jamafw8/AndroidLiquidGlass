package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.Block
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.utils.rememberClockSdfShader
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Rectangle
import com.kyant.shapes.RoundedRectangle

@Composable
fun LockScreenContent() {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isSheetExpanded by remember { mutableStateOf(true) }

    var fontHeight by remember { mutableFloatStateOf(100f) }
    var fontWidth by remember { mutableFloatStateOf(100f) }
    var fontWeight by remember { mutableFloatStateOf(400f) }
    var fontSoftness by remember { mutableFloatStateOf(70f) }

    val sdfShader = rememberClockSdfShader(
        text = "10:09",
        fontHeight = fontHeight,
        fontWidth = fontWidth,
        fontWeight = fontWeight,
        fontSoftness = fontSoftness
    )

    val density = LocalDensity.current
    val widthDp = with(density) { sdfShader.width.toDp() }
    val heightDp = with(density) { sdfShader.height.toDp() }

    BackdropDemoScaffold { backdrop ->
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .graphicsLayer {
                            val offset = offset
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .draggable2D(rememberDraggable2DState { delta -> offset += delta })
                        .drawPlainBackdrop(
                            backdrop = backdrop,
                            shape = { Rectangle },
                            effects = {
                                colorControls(brightness = 0.08f, contrast = 1.0f, saturation = 1.1f)
                                blur(2f.dp.toPx())
                                with(sdfShader) { apply() }
                            },
                            onDrawBackdrop = { drawBackdrop ->
                                drawBackdrop()
                                drawRect(Color.White.copy(alpha = 0.15f))
                            }
                        )
                        .size(widthDp, heightDp)
                )
            }
            Box(Modifier.weight(1f))
        }

        Block {
            if (isSheetExpanded) {
                val sheetBackdrop = rememberLayerBackdrop()
                Column(
                    Modifier
                        .padding(16f.dp)
                        .padding(bottom = 72f.dp)
                        .navigationBarsPadding()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(32f.dp) },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            highlight = { Highlight.Plain },
                            exportedBackdrop = sheetBackdrop,
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
                        )
                        .padding(24f.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16f.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Height (HGHT)")
                        LiquidSlider(
                            value = { fontHeight },
                            onValueChange = { fontHeight = it },
                            valueRange = 100f..500f,
                            visibilityThreshold = 0.1f,
                            backdrop = sheetBackdrop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Width (wdth)")
                        LiquidSlider(
                            value = { fontWidth },
                            onValueChange = { fontWidth = it },
                            valueRange = 60f..100f,
                            visibilityThreshold = 0.1f,
                            backdrop = sheetBackdrop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Weight (wght)")
                        LiquidSlider(
                            value = { fontWeight },
                            onValueChange = { fontWeight = it },
                            valueRange = 1f..1000f,
                            visibilityThreshold = 0.1f,
                            backdrop = sheetBackdrop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Softness (SOFT)")
                        LiquidSlider(
                            value = { fontSoftness },
                            onValueChange = { fontSoftness = it },
                            valueRange = 0f..100f,
                            visibilityThreshold = 0.1f,
                            backdrop = sheetBackdrop
                        )
                    }
                }
            }
        }

        Block {
            LiquidButton(
                { isSheetExpanded = !isSheetExpanded },
                backdrop,
                Modifier
                    .padding(20f.dp)
                    .navigationBarsPadding()
                    .align(Alignment.BottomStart),
                tint = Color(0xFFFF8D28)
            ) {
                BasicText(
                    if (isSheetExpanded) "🔽" else "🔼",
                    style = TextStyle(Color.White, 15f.sp)
                )
            }

            LiquidButton(
                {
                    fontHeight = 100f
                    fontWidth = 100f
                    fontWeight = 400f
                    fontSoftness = 70f
                },
                backdrop,
                Modifier
                    .padding(20f.dp)
                    .navigationBarsPadding()
                    .align(Alignment.BottomEnd),
                tint = Color(0xFFFF8D28)
            ) {
                BasicText(
                    "Reset",
                    style = TextStyle(Color.White, 15f.sp)
                )
            }
        }
    }
}
