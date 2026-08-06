package com.haprial.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haprial.app.data.api.ApiClient
import com.moriafly.salt.ui.*
import kotlinx.coroutines.launch

@OptIn(UnstableSaltUiApi::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val ctx = LocalContext.current
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var checkingToken by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val prefs = remember { ctx.getSharedPreferences("haprial_auth", 0) }

    // Check saved token on init
    LaunchedEffect(Unit) {
        val savedToken = prefs.getString("token", null)
        if (!savedToken.isNullOrBlank()) {
            try {
                val api = ApiClient.create(ctx)
                val resp = api.verify()
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    onLoginSuccess()
                    return@LaunchedEffect
                }
            } catch (_: Exception) {}
            prefs.edit().remove("token").apply()
        }
        checkingToken = false
    }

    if (checkingToken) {
        Box(Modifier.fillMaxSize().background(SaltTheme.colors.background), Alignment.Center) {
            // Loading indicator - use Material3 CircularProgressIndicator as SaltUI doesn't have one
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

            // Password input using SaltUI ItemEditPassword
            RoundedColumn {
                ItemEditPassword(
                    text = password,
                    onChange = { password = it },
                    hint = "管理密码"
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = SaltTheme.colors.error, style = SaltTheme.textStyles.sub)
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password.isBlank()) return@Button
                    loading = true
                    scope.launch {
                        try {
                            val resp = ApiClient.create(ctx).login(mapOf("password" to password))
                            if (resp.isSuccessful && resp.body()?.ok == true) {
                                ctx.getSharedPreferences("haprial_auth", 0)
                                    .edit().putString("token", resp.body()!!.token!!).apply()
                                onLoginSuccess()
                            } else error = resp.body()?.error ?: "登录失败"
                        } catch (_: Exception) { error = "网络错误" }
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
