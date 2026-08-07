package com.haprial.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi

/**
 * 统一标题栏 - 无操作按钮
 */
@OptIn(UnstableSaltApi::class)
@Composable
fun StandardTitleBar(title: String) {
    TitleBar(onBack = {}, text = title, showBackBtn = false)
}

/**
 * 统一标题栏 - 带右侧操作按钮
 */
@Composable
fun StandardTitleBar(
    title: String,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = SaltTheme.colors.text
        )
        Spacer(Modifier.weight(1f))
        actions()
    }
}
