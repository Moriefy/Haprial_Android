package com.haprial.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.haprial.app.data.api.ApiClient
import kotlinx.coroutines.launch

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
            // Token invalid, clear it
            prefs.edit().remove("token").apply()
        }
        checkingToken = false
    }

    if (checkingToken) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("Haprial", style = MaterialTheme.typography.headlineLarge)
            Text("博客管理系统", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(password, { password = it }, label = { Text("管理密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), isError = error != null)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                if (password.isBlank()) return@Button; loading = true
                scope.launch {
                    try {
                        val resp = ApiClient.create(ctx).login(mapOf("password" to password))
                        if (resp.isSuccessful && resp.body()?.ok == true) {
                            ctx.getSharedPreferences("haprial_auth", 0).edit().putString("token", resp.body()!!.token!!).apply()
                            onLoginSuccess()
                        } else error = resp.body()?.error ?: "登录失败"
                    } catch (_: Exception) { error = "网络错误" }
                    loading = false
                }
            }, Modifier.fillMaxWidth().height(48.dp), enabled = !loading && password.isNotBlank()) {
                if (loading) CircularProgressIndicator(Modifier.size(24.dp)) else Text("登录")
            }
        }
    }
}
