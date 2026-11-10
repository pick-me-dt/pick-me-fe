package com.pickmedt.car.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pickmedt.car.ui.theme.designDp

/**
 * Figma의 「크롭 박스 + 큰 이미지」 배치를 그대로 옮긴다.
 * box 크기 안에 image 크기로 그린 그림을 (offsetX, offsetY)만큼 옮기고 박스 밖은 자른다.
 */
@Composable
fun CroppedImage(
    @DrawableRes drawable: Int,
    boxWidth: Number,
    boxHeight: Number,
    imageWidth: Number,
    imageHeight: Number,
    offsetX: Number,
    offsetY: Number,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(boxWidth.designDp, boxHeight.designDp).clipToBounds()) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(offsetX.designDp, offsetY.designDp)
                .size(imageWidth.designDp, imageHeight.designDp),
        )
    }
}
