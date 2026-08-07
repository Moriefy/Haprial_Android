package com.haprial.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haprial.app.data.auth.AuthStateManager
import com.haprial.app.ui.articles.ArticleListScreen
import com.haprial.app.ui.editor.EditorScreen
import com.haprial.app.ui.comments.CommentListScreen
import com.haprial.app.ui.friends.FriendsScreen
import com.haprial.app.ui.images.ImageManagerScreen
import com.haprial.app.ui.settings.SettingsScreen
import com.haprial.app.ui.settings.LoginScreen
import com.haprial.app.ui.trash.TrashScreen
import com.moriafly.salt.ui.*
import org.koin.compose.koinInject

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Articles : Screen("articles", "文章", Icons.Default.Article)
    data object Comments : Screen("comments", "评论", Icons.Default.Comment)
    data object Friends : Screen("friends", "友链", Icons.Default.People)
    data object Images : Screen("images", "图片", Icons.Default.Image)
    data object Trash : Screen("trash", "回收站", Icons.Default.Delete)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}
val bottomScreens = listOf(Screen.Articles, Screen.Comments, Screen.Friends, Screen.Images, Screen.Trash, Screen.Settings)

@OptIn(UnstableSaltApi::class)
@Composable
fun HaprialNavGraph(authManager: AuthStateManager = koinInject()) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) { LoginScreen(onLoginSuccess = { isLoggedIn = true }); return }

    val showBottomBar = currentRoute in bottomScreens.map { it.route }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Main content area
            Box(Modifier.weight(1f)) {
                NavHost(
                    navController, Screen.Articles.route,
                    enterTransition = { fadeIn(animationSpec = tween(150)) },
                    exitTransition = { fadeOut(animationSpec = tween(150)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                    popExitTransition = { fadeOut(animationSpec = tween(150)) }
                ) {
                    composable(Screen.Articles.route) {
                        ArticleListScreen(onArticleClick = { navController.navigate("editor/$it") }, onNewArticle = { navController.navigate("editor/0") })
                    }
                    composable("editor/{id}") {
                        EditorScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Comments.route) { CommentListScreen() }
                    composable(Screen.Friends.route) { FriendsScreen() }
                    composable(Screen.Images.route) { ImageManagerScreen() }
                    composable(Screen.Trash.route) { TrashScreen() }
                    composable(Screen.Settings.route) { SettingsScreen(onLogout = { isLoggedIn = false; authManager.logout() }) }
                }
            }

            // SaltUI Bottom Bar
            if (showBottomBar) {
                BottomBar {
                    bottomScreens.forEach { screen ->
                        BottomBarItem(
                            state = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(screen.icon),
                            text = screen.title
                        )
                    }
                }
            }
        }
    }
}
