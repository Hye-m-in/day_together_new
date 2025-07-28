package com.example.day_together

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.day_together.ui.gallery.GalleryScreen
import com.example.day_together.ui.home.HomeScreen
import com.example.day_together.ui.message.MessageScreen
import com.example.day_together.ui.navigation.BottomNavItem
import com.example.day_together.ui.settings.SettingsScreen
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.NavIconSelected
import com.example.day_together.ui.theme.NavIconUnselected

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Day_togetherTheme {

                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    // DB 초대 관련 로직을 MainScreen 안으로 가져옴
    val invitedChatRoomId = remember { mutableStateOf<String?>(null) }

    // 화면이 처음 나타날 때 초대 여부 확인
    LaunchedEffect(Unit) {
        checkInvitationAndSetState(invitedChatRoomId)
    }


    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Message,
        BottomNavItem.Gallery,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(

                        icon = {
                            Icon(
                                painter = painterResource(id = screen.iconResId), // vectorResource -> (아이콘 변경 최신 표시)painterResource
                                contentDescription = screen.label,
                                tint = if (isSelected) NavIconSelected else NavIconUnselected
                            )
                        },

                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(appNavController = navController)
            }
            composable(BottomNavItem.Message.route) {
                MessageScreen(navController = navController)
            }
            composable(BottomNavItem.Gallery.route) {
                GalleryScreen(navController = navController)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }

    // 초대 다이얼로그 로직을 Scaffold 밖에 배치하고 화면 위에 뜨도록
    invitedChatRoomId.value?.let { chatRoomId ->
        InvitationDialog(
            onAccept = {
                ChatRoomManager.acceptInvitation(chatRoomId) { success, message ->
                    if (success) {
                        val intent = Intent(context, ChatActivity::class.java)
                        intent.putExtra("chatRoomId", chatRoomId)
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, message ?: "입장 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                invitedChatRoomId.value = null
            },
            onDismiss = {
                invitedChatRoomId.value = null
            }
        )
    }

}

// DB 초대 확인 함수
private fun checkInvitationAndSetState(state: MutableState<String?>) {
    val userId = AuthManager.getCurrentUserId() ?: return

    FirebaseService.db.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            val invitedChatRoomId = document.getString("invitedChatRoomId")
            if (!invitedChatRoomId.isNullOrEmpty()) {
                state.value = invitedChatRoomId
            }
        }
}

// DB 초대 다이얼로그
@Composable
fun InvitationDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("초대 도착") },
        text = { Text("가족 채팅방에 초대받았습니다. 입장하시겠습니까?") },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("입장하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}