package com.haprial.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haprial.app.ui.articles.ArticleListScreen
import com.haprial.app.ui.editor.EditorScreen
import com.haprial.app.ui.comments.CommentListScreen
import com.haprial.app.ui.images.ImageManagerScreen
import com.haprial.app.ui.settings.SettingsScreen
import com.haprial.app.ui.settings.LoginScreen
import com.haprial.app.ui.trash.TrashScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Articles : Screen("articles", "文章", Icons.Default.Article)
    data object Comments : Screen("comments", "评论", Icons.Default.Comment)
    data object Images : Screen("images", "图片", Icons.Default.Image)
    data object Trash : Screen("trash", "回收站", Icons.Default.Delete)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}
val bottomScreens = listOf(Screen.Articles, Screen.Comments, Screen.Images, Screen.Trash, Screen.Settings)

@Composable
fun HaprialNavGraph() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) { LoginScreen(onLoginSuccess = { isLoggedIn = true }); return }

    val showBottomBar = currentRoute in bottomScreens.map { it.route }
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController, Screen.Articles.route, Modifier.padding(padding),
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
            composable(Screen.Images.route) { ImageManagerScreen() }
            composable(Screen.Trash.route) { TrashScreen() }
            composable(Screen.Settings.route) { SettingsScreen(onLogout = { isLoggedIn = false }) }
        }
    }
}
