package com.pickmedt.car.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.pickmedt.car.R
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

/** Pleos 공식 ivi_title (Figma 노드 I2:36000). 앱 아이콘 80 + "pickme" */
@Composable
fun PickmeTitleBar(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(80.designDp)
            .padding(start = 40.designDp, end = 24.designDp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_pickme_app), contentDescription = null, modifier = Modifier.size(80.designDp))
        Spacer(Modifier.width(16.designDp))
        Text(
            text = "pickme",
            style = PickmeType.appTitle,
            color = PickmeColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.designDp, vertical = 8.designDp),
        )
    }
}
