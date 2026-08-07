package com.haprial.app.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.haprial.app.ui.components.StandardTitleBar
import com.haprial.app.ui.components.TitleBarButton
import com.haprial.app.data.model.Friend
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
import org.koin.androidx.compose.koinViewModel

@OptIn(UnstableSaltApi::class)
@Composable
fun FriendsScreen(vm: FriendsViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // 统一标题栏
            StandardTitleBar(title = "友链") {
                TitleBarButton(onClick = { vm.loadFriends() }) {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Refresh),
                        contentDescription = "刷新"
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(state.error!!, color = Color(0xFFE53935))
                }
                state.friends.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("暂无友链", color = SaltTheme.colors.subText)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.friends, key = { it.id }) { friend ->
                        FriendItem(friend)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableSaltApi::class)
@Composable
private fun FriendItem(friend: Friend) {
    RoundedColumn {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (!friend.avatar.isNullOrEmpty()) {
                AsyncImage(
                    model = friend.avatar,
                    contentDescription = friend.name,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                )
            } else {
                Box(
                    Modifier
                        .size(48.dp)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = SaltTheme.colors.highlight
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    friend.name,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!friend.desc.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        friend.desc,
                        style = SaltTheme.textStyles.sub,
                        color = SaltTheme.colors.subText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    friend.url,
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.highlight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
