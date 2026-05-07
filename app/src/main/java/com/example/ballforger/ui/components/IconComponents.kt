package com.example.ballforger.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter
import com.example.ballforger.utils.BottomRightTriangleShape
import com.example.ballforger.utils.TopLeftTriangleShape
import java.nio.ByteBuffer

@Composable
fun ByteArrayImage(byteArray: ByteArray?, modifier: Modifier = Modifier) {
    if (byteArray == null) return
    val painter = rememberAsyncImagePainter(model = ByteBuffer.wrap(byteArray))
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun SplitMaterialByteArrayIcon(iconData1: ByteArray, iconData2: ByteArray, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ByteArrayImage(
            byteArray = iconData1,
            modifier = Modifier.fillMaxSize().clip(TopLeftTriangleShape())
        )
        ByteArrayImage(
            byteArray = iconData2,
            modifier = Modifier.fillMaxSize().clip(BottomRightTriangleShape())
        )
    }
}