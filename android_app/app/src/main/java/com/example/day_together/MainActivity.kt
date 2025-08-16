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
                // 앱의 전체 화면 전환을 담당하는 AppNavigation Composable 호출
                AppNavigation()
            }
        }
    }
}

/**
 * 하단 네비게이션 바를 포함하는 메인 화면의 UI 틀 구성
 * 이 화면은 AppNavigation에 의해 호출됩
 *
 * @param appNavController 앱의 최상위 NavController.
 * 하단 바가 없는 화면(예: 개인정보 수정)으로 이동할 때 사용
 */
@Composable
fun MainScreen(appNavController: NavHostController) {
    // 하단 탭(Home, Message 등) 사이의 화면 전환만을 담당하는 내부 NavController 생성
    val innerNavController = rememberNavController()
    val context = LocalContext.current

    // DB 초대 관련 상태 변수. 이 상태는 HomeScreen으로 전달
    val invitedChatRoomId = remember { mutableStateOf<String?>(null) }

    // 화면이 처음 나타날 때 초대 여부를 확인하는 로직
    LaunchedEffect(Unit) {
        checkInvitationAndSetState(invitedChatRoomId)
    }

    // 하단 네비게이션 바에 표시될 아이템 리스트
    val bottomNavItems = listOf(
        BottomNavItem.Home, BottomNavItem.Message, BottomNavItem.Gallery, BottomNavItem.Settings
    )

    // Scaffold를 사용하여 기본적인 Material Design 레이아웃 구조 만듦
    Scaffold(
        bottomBar = {
            // 하단 네비게이션 바
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                // 현재 화면의 경로를 내부 NavController에서 가져와서 활성화된 탭 표시
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
                            // 하단 탭 클릭 시 내부 NavController를 사용하여 화면을 전환
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
        // 하단 탭에 따라 변경될 화면들을 NavHost로 관리
        NavHost(
            navController = innerNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // HomeScreen 호출 시 필요한 파라미터 전달
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    appNavController = appNavController,
                    invitedChatRoomId = invitedChatRoomId, // 초대 ID 상태 전달
                    onAcceptInvitation = { chatRoomId -> // 초대 수락 시 동작 전달
                        // TODO: 실제 DB에 수락 상태를 업데이트하는 로직 필요
                        Toast.makeText(context, "초대를 수락했습니다.", Toast.LENGTH_SHORT).show()
                        invitedChatRoomId.value = null // 다이얼로그 닫기
                    },
                    onDismissInvitation = { // 초대 거절/닫기 시 동작 전달
                        invitedChatRoomId.value = null // 다이얼로그 닫기
                    }
                )
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

    /*
    // 중복된 초대 다이얼로그 로직 삭제 -> HomeScreen 내부에서 처리되므로 MainScreen에서 삭제함
    invitedChatRoomId.value?.let { chatRoomId ->
        InvitationDialog(
            onAccept = { ... },
            onDismiss = { ... }
        )
    }
    */
}

/**
 * TODO : DB에서 초대 정보를 확인하고 상태를 업데이트하는 함수 구현
 */
private fun checkInvitationAndSetState(state: MutableState<String?>) {
    // 예시: AuthManager.checkInvitation { invitedId -> state.value = invitedId }
}