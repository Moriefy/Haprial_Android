package com.haprial.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 本地 TitleBarButton 替代组件。
 * Salt UI 2.2.0 中不存在 TitleBarButton（3.0.0-alpha01 才加入），
 * 此组件提供相同的视觉效果。
 */
@Composable
fun TitleBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true
    ) {
        content()
    }
}
