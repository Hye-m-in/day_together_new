package com.example.day_together

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.day_together.navigation.AppNavigation
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

                AppNavigation()
            }
        }
    }
}

/**
 * 하단 네비게이션 바를 포함하는 메인 화면의 UI 틀을 구성
 * 이 화면은 AppNavigation에 의해 호출
 *
 * @param appNavController 앱의 최상위 NavController
 * 하단 바가 없는 화면(예: 개인정보 수정)으로 이동할 때 사용
 */
@Composable
fun MainScreen(appNavController: NavHostController) {
    // 하단 탭(Home, Message 등) 사이의 화면 전환만을 담당하는 내부 NavController 생성
    val innerNavController = rememberNavController()
    val context = LocalContext.current

    // DB 초대 관련 로직
    val invitedChatRoomId = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        checkInvitationAndSetState(invitedChatRoomId)
    }

    val bottomNavItems = listOf(
        BottomNavItem.Home, BottomNavItem.Message, BottomNavItem.Gallery, BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                // 현재 화면의 경로를 '내부' NavController에서 가져와야
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.iconResId),
                                contentDescription = screen.label,
                                tint = if (isSelected) NavIconSelected else NavIconUnselected
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            // 하단 탭 클릭 시 '내부' NavController를 사용하여 이동
                            innerNavController.navigate(screen.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
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
        // 하단 탭 내부 화면들을 정의하는 NavHost. '내부' NavController 사용
        NavHost(
            navController = innerNavController, // '서울 지도' 사용
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(appNavController = appNavController)
            }
            composable(BottomNavItem.Message.route) {
                MessageScreen(navController = appNavController)
            }
            composable(BottomNavItem.Gallery.route) {
                GalleryScreen(navController = appNavController)
            }
            composable(BottomNavItem.Settings.route) {

                SettingsScreen(appNavController = appNavController)
            }
        }
    }

    // 초대 다이얼로그
    invitedChatRoomId.value?.let { chatRoomId ->
        InvitationDialog(
            onAccept = {
                // TODO: 실제 로직 연결
                Toast.makeText(context, "초대를 수락했습니다.", Toast.LENGTH_SHORT).show()
                invitedChatRoomId.value = null
            },
            onDismiss = {
                invitedChatRoomId.value = null
            }
        )
    }
}

// DB 초대 확인 함수
// TODO : 실제 구현 필요
private fun checkInvitationAndSetState(state: MutableState<String?>) {
    // AuthManager.checkInvitation { invitedId -> state.value = invitedId }
}

// DB 초대 다이얼로그
@Composable
fun InvitationDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("초대 도착") },
        text = { Text("가족 채팅방에 초대받았습니다. 입장하시겠습니까?") },
        confirmButton = { Button(onClick = onAccept) { Text("입장하기") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("나중에") } }
    )
}
