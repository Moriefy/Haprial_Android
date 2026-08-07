package com.haprial.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haprial.app.data.auth.AuthStateManager
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemButton
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemEditPassword
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(UnstableSaltApi::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, authManager: AuthStateManager = koinInject()) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var checkingToken by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // 检查已保存的 token
    LaunchedEffect(Unit) {
        if (authManager.checkAuth()) {
            onLoginSuccess()
            return@LaunchedEffect
        }
        checkingToken = false
    }

    if (checkingToken) {
        Box(Modifier.fillMaxSize().background(SaltTheme.colors.background), Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Default.Lock),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SaltTheme.colors.highlight
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Haprial",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "博客管理系统",
                color = SaltTheme.colors.subText,
                style = SaltTheme.textStyles.sub
            )
            Spacer(Modifier.height(32.dp))

            RoundedColumn {
                ItemEditPassword(
                    text = password,
                    onChange = { password = it },
                    hint = "管理密码"
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFE53935), style = SaltTheme.textStyles.sub)
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password.isBlank()) return@Button
                    loading = true
                    error = null
                    scope.launch {
                        val result = authManager.login(password)
                        result.fold(
                            onSuccess = { onLoginSuccess() },
                            onFailure = { error = it.message }
                        )
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && password.isNotBlank()
            ) {
                if (loading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("登录")
                }
            }
        }
    }
}
