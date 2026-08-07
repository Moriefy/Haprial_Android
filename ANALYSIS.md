# Haprial Android 全面源码分析

> 逐行分析 + 文件关系 + 底层原理 + 架构体系

---

## 一、项目概览

**Haprial** 是一个博客管理 Android 客户端，用 Kotlin + Jetpack Compose 构建，通过 REST API 管理远程博客（文章、评论、图片、友链、回收站）。

**技术栈**: Kotlin 2.0 · Jetpack Compose · Material 3 · Salt UI · Retrofit · Room · Koin · Markwon · Coil

---

## 二、构建系统逐文件分析

### 2.1 `build.gradle.kts`（根项目）

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false   // AGP 8.7.3，Android 构建插件
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false  // Kotlin Android 插件
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false  // Compose 编译器插件（Kotlin 2.0 内置）
    id("org.jetbrains.compose") version "1.7.0" apply false  // JetBrains Compose Multiplatform（这里只用 Android 部分）
    id("com.google.devtools.ksp") version "2.0.0-1.0.24" apply false  // KSP：Room 注解处理器（替代 kapt，更快）
}
```

**原理**: `apply false` 表示根项目不应用这些插件，只声明版本号，子模块 `:app` 才真正 `apply`。这是 Gradle 的版本集中管理模式。

### 2.2 `settings.gradle.kts`

```kotlin
pluginManagement { ... }  // 插件仓库：Google Maven、Maven Central、Gradle Plugin Portal、JetBrains Compose Dev
dependencyResolutionManagement { ... }  // 依赖仓库：同上 + JitPack（第三方库）
rootProject.name = "Haprial"
include(":app")  // 只有一个模块
```

**关键**: `FAIL_ON_PROJECT_REPOS` 策略禁止子模块自行声明仓库，强制所有依赖源统一管理。

### 2.3 `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8  // Gradle 守护进程 JVM 参数
android.useAndroidX=true  // 使用 AndroidX 替代 Support Library
kotlin.code.style=official  // Kotlin 官方代码风格
android.nonTransitiveRClass=true  // R 类不传递（减少编译依赖）
```

### 2.4 `app/build.gradle.kts`

**逐段分析**:

**Plugins**:
```kotlin
id("com.android.application")        // Android 应用模块（非 library）
id("org.jetbrains.kotlin.android")   // Kotlin for Android
id("org.jetbrains.kotlin.plugin.compose")  // Compose 编译器
id("org.jetbrains.compose")          // JetBrains Compose
id("com.google.devtools.ksp")        // KSP 注解处理
```

**Android 配置**:
- `namespace = "com.haprial.app"` — R 类和 BuildConfig 的包名
- `compileSdk = 35` / `targetSdk = 35` — Android 15
- `minSdk = 26` — Android 8.0（覆盖 ~95% 设备）
- `signingConfigs` — release 签名用 `debug.keystore`（开发阶段，正式发布应替换）
- `isMinifyEnabled = true` / `isShrinkResources = true` — 开启 R8 混淆和资源缩减
- `jvmTarget = "17"` — Java 17 字节码

**依赖分析**:

| 类别 | 库 | 作用 |
|------|-----|------|
| UI | Compose BOM 2024.10 | 统一 Compose 版本 |
| UI | Salt UI 2.2.0 | 第三方 UI 组件库（TitleBar, BottomBar, Item 等） |
| 导航 | Navigation Compose 2.8.5 | 页面路由 |
| 网络 | Retrofit 2.11 + OkHttp 4.12 | REST API 客户端 |
| 数据库 | Room 2.6.1 + KSP | 本地缓存草稿 |
| DI | Koin 3.5.6 | 依赖注入 |
| Markdown | Markwon 4.6.2（含 tables/syntax/image/linkify/editor） | Markdown 实时渲染 |
| 图片 | Coil Compose 2.7 | 图片异步加载 |
| 存储 | DataStore Preferences 1.1.1 | 声明式键值存储（实际未使用，用了 SharedPreferences） |

**ProGuard** (`proguard-rules.pro`):
```proguard
-keep class com.haprial.app.data.model.** { *; }  // 保留数据模型（Gson 反射需要）
-keep class retrofit2.** { *; }  // 保留 Retrofit
-keepclassmembers interface * { @retrofit2.http.* <methods>; }  // 保留 API 接口方法
```
**原理**: Retrofit 通过动态代理 + 注解反射创建 API 实现，混淆后注解丢失会导致接口失效。

---

## 三、应用入口层

### 3.1 `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />           // 网络访问
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />   // Android 13+ 图片读取
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
```

- `android:name=".HaprialApp"` — 自定义 Application 类
- `android:networkSecurityConfig="@xml/network_security_config"` — 网络安全配置
- `android:windowSoftInputMode="adjustResize"` — 软键盘弹出时调整布局

**`network_security_config.xml`**:
```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">pluslogic.eu.org</domain>
    <domain includeSubdomains="true">comments.pluslogic.eu.org</domain>
</domain-config>
```
**原理**: 禁止对这两个域名的明文 HTTP 请求，强制 HTTPS。这是 Android 9+ 的网络安全策略。

### 3.2 `HaprialApp.kt` — Application 类

```kotlin
class HaprialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()              // Koin 日志
            androidContext(this@HaprialApp)  // 注入 Application Context
            modules(appModule)           // 加载 DI 模块
        }
    }
}
```

**原理**: `Application.onCreate()` 是 Android 进程启动时最早的生命周期回调。在此初始化 Koin，确保所有后续组件（Activity、ViewModel）都能通过依赖注入获取实例。

### 3.3 `MainActivity.kt` — 唯一 Activity

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        edgeToEdge()  // Salt UI 扩展：沉浸式状态栏/导航栏
        super.onCreate(savedInstanceState)
        setContent {
            HaprialTheme {       // 主题包装
                HaprialNavGraph()  // 导航图（整个 UI 入口）
            }
        }
    }
}
```

**原理**: 单 Activity 架构 — 整个 App 只有一个 Activity，所有页面都是 Composable 函数，通过 Navigation Compose 管理页面栈。`edgeToEdge()` 调用 `WindowCompat.setDecorFitsSystemWindows(window, false)` 实现全屏沉浸。

---

## 四、数据层

### 4.1 `Models.kt` — 数据模型

**API 响应模型**:

```kotlin
data class LoginResponse(val ok: Boolean, val token: String?, val message: String?, val error: String?)
```
- 登录成功返回 `ok=true` + `token`，失败返回 `error`

```kotlin
data class GenericResponse(val ok: Boolean, val error: String?, val github: GithubResult?)
```
- 通用响应，`github` 字段表明后端会同步操作到 GitHub 仓库

```kotlin
data class Article(
    val id: Int, val slug: String, val title: String, val date: String,
    val tags: String,       // JSON 数组字符串或逗号分隔
    val category: String, val excerpt: String,
    val content: String?,   // 详情才有
    val status: String,     // "published" / "draft"
    val pinned: Int,        // 0/1
    val createdAt: String?, val updatedAt: String?
)
```

**关键方法**:
```kotlin
fun tagList(): List<String> = try {
    if (tags.startsWith("[")) Gson().fromJson(tags, Array<String>::class.java).toList()
    else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
} catch (e: Exception) { emptyList() }
```
**原理**: 兼容两种标签格式 — JSON 数组 `["tag1","tag2"]` 和简单逗号分隔 `tag1,tag2`。每次调用都 new Gson()（性能隐患，应缓存）。

```kotlin
data class Comment(
    val id: Int, val parentId: Int, val depth: Int,  // 树形评论结构
    val nickname: String, val email: String?, val website: String?,
    val avatarHash: String?,     // Gravatar hash
    val contentHtml: String,     // 已渲染的 HTML
    val liked: Int, val isAdmin: Int, val pinned: Int, val createdAt: String
)
```
**原理**: 评论是树形结构，`parentId` 指向父评论，`depth` 表示嵌套层级。

**请求模型**:
```kotlin
data class ArticleCreateRequest(
    val title: String, val date: String, val tags: List<String>,
    val category: String, val excerpt: String, val content: String,
    val status: String, val pinned: Boolean
)
```

### 4.2 `HaprialApi.kt` — Retrofit API 接口

```kotlin
interface HaprialApi {
    // 认证
    @POST("/api/admin/auth")    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>
    @GET("/api/admin/verify")   suspend fun verify(): Response<GenericResponse>

    // 文章 CRUD
    @GET("/api/admin/articles")              suspend fun getArticles(@Query("status") status: String = "all")
    @GET("/api/admin/articles/{id}")         suspend fun getArticle(@Path("id") id: Int)
    @POST("/api/admin/articles")             suspend fun createArticle(@Body article: ArticleCreateRequest)
    @PUT("/api/admin/articles/{id}")         suspend fun updateArticle(@Path("id") id: Int, @Body article: ArticleCreateRequest)
    @DELETE("/api/admin/articles/{id}")      suspend fun deleteArticle(@Path("id") id: Int)
    @POST("/api/admin/articles/{id}/publish") suspend fun togglePublish(@Path("id") id: Int)

    // 评论
    @GET("/api/admin/comments")              suspend fun getComments(@Query("page_slug") pageSlug: String?, @Query("limit") limit: Int = 200)
    @POST("/api/admin/comments")             suspend fun postComment(@Body comment: CommentPostRequest)
    @DELETE("/api/admin/comments/{id}")      suspend fun deleteComment(@Path("id") id: Int)
    @POST("/api/admin/comments/{id}/pin")    suspend fun pinComment(@Path("id") id: Int)
    @POST("/api/admin/comments/{id}/like")   suspend fun likeComment(@Path("id") id: Int)

    // 图片
    @GET("/api/admin/images/list")           suspend fun getImages(@Query("folder") folder: String?)
    @POST("/api/admin/images/upload")        suspend fun uploadImage(@Body body: Map<String, String>)
    @POST("/api/admin/images/copy")          suspend fun copyImage(@Body body: Map<String, String>)
    @DELETE("/api/admin/images/{path}")      suspend fun deleteImage(@Path("path") path: String)

    // 友链 / 统计 / 回收站
    @GET("/api/admin/friends")   suspend fun getFriends()
    @GET("/api/admin/stats")     suspend fun getStats()
    @GET("/api/admin/trash")     suspend fun getTrash()
    @POST("/api/admin/trash/{id}/restore") suspend fun restoreTrash(@Path("id") id: Int)
    @DELETE("/api/admin/trash/{id}")       suspend fun deleteTrash(@Path("id") id: Int)
    @POST("/api/admin/trash/empty")        suspend fun emptyTrash()
}
```

**原理**: Retrofit 通过注解将接口方法映射为 HTTP 请求。`suspend` 关键字使每个方法都是挂起函数，可在协程中直接调用。`Response<T>` 包装允许检查 HTTP 状态码（`isSuccessful`）。

### 4.3 `ApiClient.kt` — 网络客户端工厂

```kotlin
object ApiClient {
    private const val BASE_URL = "https://comments.pluslogic.eu.org"

    fun create(context: Context): HaprialApi {
        val tokenProvider = TokenProvider(context)

        val authInterceptor = Interceptor { chain ->
            val token = tokenProvider.getToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")  // JWT 认证
                    .build()
            } else { chain.request() }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // 日志输出完整请求/响应体
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)   // 自动附加 Token
            .addInterceptor(logging)           // 日志
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())  // JSON 序列化
            .build()
            .create(HaprialApi::class.java)  // 动态代理生成实现
    }
}
```

**原理**:
1. **OkHttp 拦截器链**: 请求经过 `authInterceptor`（附加 Token）→ `logging`（打印日志）→ 网络
2. **Retrofit 动态代理**: `create(HaprialApi::class.java)` 使用 `java.lang.reflect.Proxy` 在运行时生成接口实现，解析注解构建请求
3. **Bearer Token**: 标准 JWT 认证方式，`Authorization: Bearer <token>`

```kotlin
class TokenProvider(private val context: Context) {
    private val prefs = context.getSharedPreferences("haprial_auth", Context.MODE_PRIVATE)
    fun getToken(): String? = prefs.getString("token", null)
    fun saveToken(token: String) { prefs.edit().putString("token", token).apply() }
    fun clearToken() { prefs.edit().remove("token").apply() }
}
```
**原理**: 使用 SharedPreferences 持久化 JWT Token。`apply()` 是异步写入（vs `commit()` 同步）。

### 4.4 `AppDatabase.kt` — Room 本地数据库

```kotlin
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: Int,
    val title: String, val content: String, val tags: String,
    val category: String, val excerpt: String, val date: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Dao
interface ArticleDao {
    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getDraft(id: Int): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteDraft(id: Int)
}

@Database(entities = [DraftEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "haprial.db"
        ).build()
    }
}
```

**原理**:
- Room 在编译时通过 KSP 生成 `AppDatabase_Impl` 和 `ArticleDao_Impl`，将注解转换为 SQL 语句
- `OnConflictStrategy.REPLACE` = 主键冲突时覆盖（等价于 `INSERT OR REPLACE`）
- 仅用于草稿自动保存，不缓存文章列表（纯网络驱动）

---

## 五、依赖注入层

### 5.1 `AppModule.kt`

```kotlin
val appModule = module {
    // 单例：全局共享一个 Retrofit API 实例
    single { ApiClient.create(androidContext()) }
    // 单例：Room 数据库
    single { AppDatabase.create(androidContext()) }
    // 单例：DAO（从数据库实例获取）
    single { get<AppDatabase>().articleDao() }

    // ViewModel：每次注入创建新实例（但 Koin 会缓存在 ViewModelStoreOwner 中）
    viewModel { ArticleListViewModel(get(), get()) }   // 依赖 HaprialApi + ArticleDao
    viewModel { EditorViewModel(get(), get()) }         // 依赖 HaprialApi + ArticleDao
    viewModel { CommentListViewModel(get()) }            // 依赖 HaprialApi
    viewModel { ImageManagerViewModel(get()) }           // 依赖 HaprialApi
    viewModel { SettingsViewModel(get()) }               // 依赖 HaprialApi
    viewModel { TrashViewModel(get()) }                  // 依赖 HaprialApi
}
```

**依赖关系图**:
```
HaprialApi (单例)
├── ArticleListViewModel (HaprialApi + ArticleDao)
├── EditorViewModel (HaprialApi + ArticleDao)
├── CommentListViewModel (HaprialApi)
├── ImageManagerViewModel (HaprialApi)
├── SettingsViewModel (HaprialApi)
└── TrashViewModel (HaprialApi)

AppDatabase (单例)
└── ArticleDao (单例)
    ├── ArticleListViewModel
    └── EditorViewModel
```

**原理**: Koin 是服务定位器模式（非编译时 DI），`get()` 在运行时解析依赖。`viewModel {}` 注册的是 ViewModel 工厂，配合 Compose 的 `koinViewModel()` 使用。

---

## 六、UI 层 — 导航与主题

### 6.1 `NavGraph.kt` — 导航图

**路由定义**:
```kotlin
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Articles : Screen("articles", "文章", Icons.Default.Article)
    data object Comments : Screen("comments", "评论", Icons.Default.Comment)
    data object Images   : Screen("images", "图片", Icons.Default.Image)
    data object Trash    : Screen("trash", "回收站", Icons.Default.Delete)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}
```

**导航逻辑**:
```kotlin
@Composable
fun HaprialNavGraph() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }

    // 登录守卫：未登录显示登录页
    if (!isLoggedIn) { LoginScreen(onLoginSuccess = { isLoggedIn = true }); return }

    // 底部导航栏：只在顶级页面显示
    val showBottomBar = currentRoute in bottomScreens.map { it.route }

    Surface(...) {
        Column {
            Box(Modifier.weight(1f)) {
                NavHost(navController, Screen.Articles.route) {
                    composable(Screen.Articles.route) {
                        ArticleListScreen(
                            onArticleClick = { navController.navigate("editor/$it") },
                            onNewArticle = { navController.navigate("editor/0") }
                        )
                    }
                    composable("editor/{id}") {  // 带参数路由
                        EditorScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, ...)
                    }
                    composable(Screen.Comments.route) { CommentListScreen() }
                    composable(Screen.Images.route) { ImageManagerScreen() }
                    composable(Screen.Trash.route) { TrashScreen() }
                    composable(Screen.Settings.route) { SettingsScreen(onLogout = { isLoggedIn = false }) }
                }
            }
            if (showBottomBar) { BottomBar { ... } }  // Salt UI 底部栏
        }
    }
}
```

**原理**:
- `NavHost` = 导航宿主，管理 Composable 页面栈
- `popUpTo(findStartDestination)` + `saveState` + `restoreState` = 底部栏切换时保存/恢复页面状态
- `launchSingleTop` = 避免重复入栈同一页面
- 登录状态用 `remember` 临时变量（非持久化，App 重启需重新登录 — 但 `LoginScreen` 会检查 saved token）

### 6.2 `Theme.kt` — 主题系统

```kotlin
var currentThemeMode = mutableStateOf("system")  // 全局可观察状态

@Composable
fun HaprialTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        val prefs = ctx.getSharedPreferences("haprial_theme", Context.MODE_PRIVATE)
        currentThemeMode.value = prefs.getString("theme", "system") ?: "system"
    }

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    SaltTheme(configs = SaltConfigs.default(isDarkTheme = darkTheme)) {
        content()
    }
}
```

**原理**:
- 使用 `mutableStateOf` 作为全局主题状态，SettingsScreen 修改时立即反映
- 从 SharedPreferences 读取持久化的主题偏好
- `SaltTheme` 替代 MaterialTheme，提供 Salt UI 的颜色/字体/形状系统

### 6.3 `Color.kt` — 颜色定义

定义了完整的亮色/暗色主题色板：
- `Primary = Color(0xFF3D5A6E)` — 深蓝灰（主色调）
- `Secondary = Color(0xFF4A7B6A)` — 深绿（辅助色）
- `Tertiary = Color(0xFF8E6B9E)` — 紫色（强调色）
- `DarkPrimary = Color(0xFF9AB0BF)` — 暗色模式主色（较亮的蓝灰）

---

## 七、UI 层 — 各页面逐文件分析

### 7.1 文章列表 `ArticleListScreen.kt` + `ArticleListViewModel.kt`

**ViewModel 状态管理**:
```kotlin
data class ArticleListState(val articles: List<Article>, val isLoading: Boolean, val error: String?)

class ArticleListViewModel(private val api: HaprialApi, private val dao: ArticleDao) : ViewModel() {
    private val _state = MutableStateFlow(ArticleListState())
    val state: StateFlow<ArticleListState> = _state
    init { loadArticles() }

    fun loadArticles() { /* GET /api/admin/articles → 更新 _state */ }
    fun deleteArticle(id: Int) { /* DELETE → reload */ }
    fun togglePublish(id: Int) { /* POST /publish → reload */ }
}
```

**Screen 功能**:
1. **搜索**: 标题 + 内容全文搜索（客户端过滤）
2. **筛选**: 状态（全部/已发布/草稿）、分类、年份、标签（ModalBottomSheet）
3. **分页**: 每页 10 条，客户端分页
4. **操作**: 发布/下架切换、删除（下拉菜单）

**数据流**:
```
API → ViewModel._state → Compose collectAsState → remember 过滤 → remember 分页 → LazyColumn 渲染
```

**原理**: `remember(key1, key2, ...)` 当任何 key 变化时重新计算，实现响应式过滤和分页。

### 7.2 编辑器 `EditorScreen.kt` + `EditorViewModel.kt`

**这是最复杂的页面**，混合了 Compose 和 Android View。

**ViewModel 状态**:
```kotlin
data class EditorState(
    val title: String, val content: String, val tags: String,
    val category: String, val excerpt: String, val date: String,
    val isSaving: Boolean, val isSaved: Boolean, val error: String?, val isNew: Boolean
)
```

**核心功能**:

1. **Markwon 实时渲染**:
```kotlin
val markwon = Markwon.builder(ctx)
    .usePlugin(TablePlugin.create(ctx))    // 表格支持
    .usePlugin(LinkifyPlugin.create())     // 自动链接
    .build()
val markwonEditor = MarkwonEditor.builder(markwon).build()
```
**原理**: Markwon 在 EditText 的 `TextWatcher` 中实时将 Markdown 语法转换为 Span（粗体、斜体、代码块等），在编辑的同时看到渲染效果。

2. **AndroidView 桥接**:
```kotlin
AndroidView(
    factory = { c ->
        EditText(c).apply {
            addTextChangedListener(MarkwonEditorTextWatcher.withProcess(markwonEditor))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    s?.toString()?.let { vm.updateContent(it) }
                }
            })
        }
    },
    update = { et ->
        if (et.text.toString() != state.content) et.setText(state.content)
        vm.pendingInsertion?.let { (prefix, suffix) ->
            // Markdown 工具栏插入逻辑
        }
    }
)
```
**原理**: `AndroidView` 是 Compose 中嵌入传统 View 的桥梁。`factory` 创建 View，`update` 在状态变化时更新 View。这里因为 Markwon 需要操作 EditText 的 Spannable，纯 Compose 无法实现。

3. **自动保存**:
```kotlin
private fun startAutoSave() {
    autoSaveJob = viewModelScope.launch {
        while (true) {
            delay(30_000)  // 每 30 秒
            dao.saveDraft(DraftEntity(articleId, ...))
        }
    }
}
```

4. **Markdown 工具栏**:
```kotlin
fun MarkdownToolbar(onAction: (String, String) -> Unit) {
    listOf(
        "B" to ("**" to "**"),    // 粗体
        "I" to ("*" to "*"),       // 斜体
        "S" to ("~~" to "~~"),     // 删除线
        "H2" to ("## " to ""),     // 二级标题
        "🔗" to ("[" to "](url)"), // 链接
        "📷" to ("![" to "](url)"),// 图片
        // ...
    )
}
```

5. **预览**:
```kotlin
if (showPreview) {
    Dialog(...) {
        AndroidView(factory = { c ->
            ScrollView(c).apply {
                val tv = TextView(c)
                addView(tv)
                markwon.setMarkdown(tv, vm.getContent())  // 一次性渲染完整 Markdown
            }
        })
    }
}
```

### 7.3 评论管理 `CommentListScreen.kt` + `CommentListViewModel.kt`

**树形评论构建**:
```kotlin
data class CommentNode(val comment: Comment, val children: List<CommentNode>)

fun buildCommentTree(comments: List<Comment>): List<CommentNode> {
    val byParent = comments.groupBy { it.parentId }
    fun buildChildren(parentId: Int): List<CommentNode> {
        return (byParent[parentId] ?: emptyList()).map { child ->
            CommentNode(child, buildChildren(child.id))
        }
    }
    return buildChildren(0)  // 从 parentId=0（根评论）开始
}
```
**原理**: 递归构建树结构。`groupBy` 创建 parentId → children 映射，递归遍历构建嵌套列表。

**双面板布局**:
```
┌─────────────┬──────────────────┐
│ 文章列表     │ 评论树            │
│ (35% 宽)    │ (65% 宽)         │
│             │                  │
│ 文章 1      │ 评论 1           │
│ 文章 2      │   ├─ 回复 1.1    │
│ ...         │   └─ 回复 1.2    │
└─────────────┴──────────────────┘
```

**评论渲染**:
```kotlin
fun LazyListScope.renderCommentNode(node: CommentNode, depth: Int, vm: CommentListViewModel) {
    item(key = node.comment.id) {
        CommentItem(comment = node.comment, depth = depth, ...)
    }
    node.children.forEach { child ->
        renderCommentNode(child, depth + 1, vm)  // 递归渲染
    }
}
```
**原理**: 通过 `LazyListScope` 扩展函数，将树形结构扁平化为 LazyColumn 的 item 列表。`depth` 控制缩进 (`padding(start = (depth * 16).dp)`)。

**回复功能**: 使用 `YesNoDialog` 弹出回复框，限制最大嵌套深度为 2 (`if (depth < 2)`)。

### 7.4 图片管理 `ImageManagerScreen.kt` + `ImageManagerViewModel.kt`

**文件夹导航**:
```kotlin
fun enterFolder(name: String) {
    loadImages(if (currentFolder.isEmpty()) name else "$currentFolder/$name")
}
fun goBack() {
    loadImages(currentFolder.split("/").dropLast(1).joinToString("/"))
}
```

**图片网格**: `LazyVerticalGrid(GridCells.Adaptive(100.dp))` — 自适应列数，每格最小 100dp。

**图片保存到相册**:
```kotlin
val contentValues = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Haprial")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
}
val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
// 写入 bitmap → 设置 IS_PENDING = 0
```
**原理**: Android 10+ 使用 Scoped Storage，通过 `MediaStore` API + `ContentValues` 保存图片，无需 `WRITE_EXTERNAL_STORAGE` 权限。

### 7.5 设置 `SettingsScreen.kt` + `SettingsViewModel.kt`

**功能**:
1. **博客统计**: 已发布/草稿/评论/友链数量
2. **主题切换**: 跟随系统/浅色/深色（写入 SharedPreferences + 更新全局状态）
3. **GitHub 同步**: 仅展示，不可操作
4. **退出登录**: 清除 Token + 回到登录页

### 7.6 登录 `LoginScreen.kt`

**登录流程**:
```
App 启动 → 检查 saved token
  ├── 有 token → GET /api/admin/verify
  │     ├── 成功 → 直接进入主页
  │     └── 失败 → 清除 token → 显示登录页
  └── 无 token → 显示登录页
        └── 输入密码 → POST /api/admin/auth → 保存 token → 进入主页
```

**注意**: 登录直接创建新的 `ApiClient.create(ctx)`，而不是使用 Koin 注入的单例。这意味着登录时的 API 调用和登录后的 API 调用是不同的 Retrofit 实例。

### 7.7 回收站 `TrashScreen.kt` + `TrashViewModel.kt`

功能：查看已删除文章、恢复、永久删除、清空回收站。

---

## 八、资源文件

| 文件 | 内容 |
|------|------|
| `colors.xml` | `ic_launcher_background = #3D5A6E`（主色调） |
| `strings.xml` | `app_name = "Haprial"` |
| `themes.xml` | Material Light NoActionBar 基础主题 |
| `network_security_config.xml` | 强制 HTTPS |
| `ic_launcher_foreground.xml` | 启动图标前景 |
| `ic_launcher.xml` | 自适应图标定义 |

---

## 九、CI/CD

### `.github/workflows/build.yml`

```yaml
on:
  push:
    branches: [main]    # push 到 main 触发
  workflow_dispatch:      # 手动触发

jobs:
  build:
    steps:
      - setup-java@v4 (JDK 17)
      - setup-gradle@v5
      - ./gradlew assembleRelease
      - upload-artifact (app-release-unsigned.apk)
```

**注意**: 构建的是 **unsigned** APK，需要额外签名才能安装。

---

## 十、整体架构体系

### 10.1 分层架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌───────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ ArticleList│ │ Editor   │ │ Comments │ │ Images       │  │
│  │ Screen    │ │ Screen   │ │ Screen   │ │ Screen       │  │
│  └─────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘  │
│        │             │            │               │          │
│  ┌─────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌──────┴───────┐  │
│  │ Article   │ │ Editor   │ │ Comment  │ │ ImageManager │  │
│  │ ViewModel │ │ ViewModel│ │ ViewModel│ │ ViewModel    │  │
│  └─────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘  │
├────────┼─────────────┼────────────┼───────────────┼──────────┤
│        │      Domain Layer (implicit)             │          │
│        │      (ViewModels contain business logic)  │          │
├────────┼─────────────┼────────────┼───────────────┼──────────┤
│                    Data Layer                                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              HaprialApi (Retrofit)                   │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │    │
│  │  │ ApiClient │  │ Models   │  │ TokenProvider    │  │    │
│  │  │ (OkHttp)  │  │ (Gson)   │  │ (SharedPrefs)    │  │    │
│  │  └──────────┘  └──────────┘  └──────────────────┘  │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │           AppDatabase (Room)                         │    │
│  │  ┌──────────┐  ┌──────────────┐                     │    │
│  │  │ DraftDao │  │ DraftEntity  │                     │    │
│  │  └──────────┘  └──────────────┘                     │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                    DI Layer (Koin)                           │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ AppModule: single{ApiClient}, single{DB}, viewModel{}│   │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                    Infrastructure                            │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐  │
│  │ AndroidManifest│ │ Theme/Color │ │ CI/CD (GH Actions)│  │
│  └──────────────┘ └──────────────┘ └────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 10.2 数据流

```
用户操作 → Composable 事件 → ViewModel 方法 → API 调用 (suspend)
                                                    ↓
                                              HTTP Request
                                              (Bearer Token)
                                                    ↓
                                              Server Response
                                                    ↓
ViewModel._state 更新 → StateFlow → collectAsState → Recomposition → UI 更新
```

### 10.3 认证流程

```
┌──────────┐    POST /auth     ┌──────────┐
│ Login    │ ─────────────────→│  Server  │
│ Screen   │←───────────────── │          │
└──────────┘   {token: "jwt"}  └──────────┘
      │
      │ saveToken(token)
      ▼
┌──────────┐                   ┌──────────┐
│ Shared   │   Interceptor     │  OkHttp  │
│ Prefs    │ ─────────────────→│  Client  │
└──────────┘  Authorization:   └──────────┘
              Bearer <token>
```

### 10.4 关键设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 架构模式 | MVVM (无 Repository 层) | 小项目，ViewModel 直接调 API |
| DI 框架 | Koin | 轻量，无编译时处理，适合小项目 |
| UI 框架 | Compose + Salt UI | 现代声明式 UI，Salt UI 提供统一风格组件 |
| 编辑器 | AndroidView (EditText + Markwon) | Markwon 依赖 Android Spannable，无法纯 Compose |
| 本地存储 | Room (仅草稿) + SharedPreferences (Token/主题) | 草稿需结构化存储，Token/主题是简单键值 |
| 网络 | Retrofit + OkHttp | Android 标准方案，类型安全 API |
| 主题 | 全局 mutableStateOf + SharedPreferences | 即时切换 + 持久化 |

### 10.5 潜在问题与改进点

1. **Token 验证**: `LoginScreen` 和 `NavGraph` 各自独立检查登录状态，可能不一致
2. **错误处理**: 大多 `catch (_: Exception) {}` 吞掉异常，用户看不到错误原因
3. **tagList()**: 每次调用都 `new Gson()`，应缓存
4. **DataStore**: 引入了 DataStore 依赖但实际未使用，用的是 SharedPreferences
5. **图片上传**: API 接口定义了 `uploadImage(@Body Map<String, String>)`，但 UI 中没有上传入口
6. **友链管理**: API 有 `getFriends()` 但没有对应的 UI 页面
7. **签名**: release 用 debug.keystore，正式发布需替换
8. **内存**: 图片预览直接加载原图到内存，大图可能 OOM

---

## 十一、文件依赖关系总结

```
HaprialApp.kt ─── di/AppModule.kt
    │                   │
    │                   ├── data/api/ApiClient.kt ── data/api/HaprialApi.kt
    │                   │                                    │
    │                   │                           data/model/Models.kt
    │                   │
    │                   ├── data/db/AppDatabase.kt ── data/model/Models.kt (DraftEntity)
    │                   │
    │                   ├── ui/articles/ArticleListViewModel.kt
    │                   ├── ui/editor/EditorViewModel.kt
    │                   ├── ui/comments/CommentListViewModel.kt
    │                   ├── ui/images/ImageManagerViewModel.kt
    │                   ├── ui/settings/SettingsViewModel.kt
    │                   └── ui/trash/TrashViewModel.kt
    │
MainActivity.kt ─── ui/navigation/NavGraph.kt
    │                      │
    │                      ├── ui/articles/ArticleListScreen.kt
    │                      ├── ui/editor/EditorScreen.kt
    │                      ├── ui/comments/CommentListScreen.kt
    │                      ├── ui/images/ImageManagerScreen.kt
    │                      ├── ui/settings/SettingsScreen.kt
    │                      ├── ui/settings/LoginScreen.kt
    │                      └── ui/trash/TrashScreen.kt
    │
    └── ui/theme/Theme.kt ── ui/theme/Color.kt
```

---

*分析完成。共 28 个源码文件，约 2000 行 Kotlin 代码。*
