package com.haprial.app.data.auth

import android.content.Context
import com.haprial.app.data.api.ApiClient
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.api.TokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 集中管理认证状态。
 * 解决 LoginScreen 和 NavGraph 各自独立检查登录状态的问题。
 */
class AuthStateManager(context: Context) {
    private val tokenProvider = TokenProvider(context)
    private val api: HaprialApi = ApiClient.create(context)

    /**
     * 检查是否有保存的 token 且仍然有效。
     * 返回 true 表示已登录且 token 有效。
     */
    suspend fun checkAuth(): Boolean {
        val token = tokenProvider.getToken() ?: return false
        if (token.isBlank()) return false
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.verify()
                resp.isSuccessful && resp.body()?.ok == true
            } catch (_: Exception) {
                tokenProvider.clearToken()
                false
            }
        }
    }

    /**
     * 尝试登录，成功返回 true 并保存 token。
     */
    suspend fun login(password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.login(mapOf("password" to password))
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    tokenProvider.saveToken(resp.body()!!.token!!)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(resp.body()?.error ?: "登录失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }
    }

    /**
     * 退出登录，清除 token。
     */
    fun logout() {
        tokenProvider.clearToken()
    }
}
