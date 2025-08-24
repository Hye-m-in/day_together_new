package com.example.day_together

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.ui.auth.LoginScreen // LoginScreen import 추가
import com.example.day_together.ui.gallery.GalleryScreen
import com.example.day_together.ui.home.HomeScreen
import com.example.day_together.ui.message.ChatInfoScreen
import com.example.day_together.ui.message.MessageScreen
import com.example.day_together.ui.message.MessageViewModel
import com.example.day_together.ui.message.MessageViewModelFactory
import com.example.day_together.ui.navigation.BottomNavItem
import com.example.day_together.ui.settings.SettingsScreen
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.NavIconSelected
import com.example.day_together.ui.theme.NavIconUnselected
import com.google.firebase.firestore.ListenerRegistration

// 앱의 모든 화면 경로를 MainActivity 내부에 정의
object AppDestinations {
    const val HOME_ROUTE = "home"
    const val MESSAGE_ROUTE = "message"
    const val CHAT_INFO_ROUTE = "chat_info"
    const val GALLERY_ROUTE = "gallery"
    const val SETTINGS_ROUTE = "settings"
    const val LOGIN_ROUTE = "login"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Day_togetherTheme {
                // 앱의 전체 화면 전환을 담당하는 NavHost 바로 호출
                AppNavigation()
            }
        }
    }
}

/**
 * 앱의 전체 내비게이션 그래프를 설정하는 Composable
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // NavHost를 사용해 앱의 모든 화면과 경로를 연결
    NavHost(navController = navController, startDestination = "main") {
        // MainScreen은 하단 바를 포함하는 메인 화면들의 컨테이너 역할
        composable("main") {
            MainScreen(appNavController = navController)
        }

        // ChatInfoScreen 경로 추가
        composable(AppDestinations.CHAT_INFO_ROUTE) {
            val messageViewModel: MessageViewModel = viewModel(
                factory = MessageViewModelFactory(AppRepository)
            )
            ChatInfoScreen(navController = navController, viewModel = messageViewModel)
        }

        // 로그인 화면 경로를 실제 LoginScreen과 연결
        composable(AppDestinations.LOGIN_ROUTE) {
            // PlaceholderLoginScreen 대신 실제 LoginScreen 호출
            LoginScreen(navController = navController)
        }
    }
}

/**
 * 하단 네비게이션 바를 포함하는 메인 화면의 UI 틀 구성
 */
@Composable
fun MainScreen(appNavController: NavHostController) {
    val innerNavController = rememberNavController()
    val invitedChatRoomId = remember { mutableStateOf<String?>(null) }
    var invitationListener: ListenerRegistration? by remember { mutableStateOf(null) }

    // 화면이 처음 나타날 때와 사용자가 바뀔 때마다 초대 리스너 설정
    DisposableEffect(ChatRoomManager.auth.currentUser) {
        val userId = ChatRoomManager.auth.currentUser?.uid
        if (userId != null) {
            invitationListener = ChatRoomManager.listenForInvitations(userId) { pendingInvitationId ->
                invitedChatRoomId.value = pendingInvitationId
            }
        }
        // 화면이 사라질 때 리스너 정리
        onDispose {
            invitationListener?.remove()
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem.Home, BottomNavItem.Message, BottomNavItem.Gallery, BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
        NavHost(
            navController = innerNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    appNavController = appNavController,
                    invitedChatRoomId = invitedChatRoomId,
                    onAcceptInvitation = { chatRoomId ->
                        ChatRoomManager.acceptInvitation(chatRoomId) { success, _ ->
                            if (success) {
                                invitedChatRoomId.value = null
                            }
                        }
                    },
                    onDismissInvitation = {
                        // TODO: 초대 거절 로직
                        invitedChatRoomId.value = null
                    }
                )
            }
            composable(BottomNavItem.Message.route) {
                val messageViewModel: MessageViewModel = viewModel(
                    factory = MessageViewModelFactory(AppRepository)
                )
                MessageScreen(navController = appNavController, viewModel = messageViewModel)
            }
            composable(BottomNavItem.Gallery.route) {
                GalleryScreen(navController = appNavController)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(appNavController = appNavController)
            }
        }
    }
}

