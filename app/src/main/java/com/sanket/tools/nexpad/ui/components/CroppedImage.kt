package com.sanket.tools.nexpad.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun CroppedImage(
    @DrawableRes imageRes: Int,
    srcOffsetX: Int, 
    srcOffsetY: Int,
    cropWidth: Int, 
    cropHeight: Int,
    targetWidthDp: Int,
    targetHeightDp: Int,
    modifier: Modifier = Modifier
) {
    val bitmap = ImageBitmap.imageResource(id = imageRes)
    
    Canvas(modifier = modifier.size(targetWidthDp.dp, targetHeightDp.dp)) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(srcOffsetX, srcOffsetY),
            srcSize = IntSize(cropWidth, cropHeight),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}
