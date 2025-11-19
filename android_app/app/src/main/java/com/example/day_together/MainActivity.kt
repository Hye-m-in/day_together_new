package com.example.day_together

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.day_together.navigation.AppNavigation
import com.example.day_together.ui.dialogs.InvitationDialog
import com.example.day_together.ui.gallery.GalleryScreen
import com.example.day_together.ui.home.HomeScreen
import com.example.day_together.ui.home.HomeViewModel
import com.example.day_together.ui.message.MessageScreen
import com.example.day_together.ui.message.MessageViewModel
import com.example.day_together.ui.navigation.BottomNavItem
import com.example.day_together.ui.settings.SettingsScreen
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.NavIconSelected
import com.example.day_together.ui.theme.NavIconUnselected
import com.google.firebase.firestore.ListenerRegistration

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

@Composable
fun MainScreen(appNavController: NavHostController) {
    val innerNavController = rememberNavController()
    val invitedChatRoomId = remember { mutableStateOf<String?>(null) }
    var invitationListener: ListenerRegistration? by remember { mutableStateOf(null) }

    // 로그인 상태에 따라 초대 리스너 등록/해제
    DisposableEffect(ChatRoomManager.auth.currentUser) {
        val userId = ChatRoomManager.auth.currentUser?.uid
        if (userId != null) {
            invitationListener = ChatRoomManager.listenForInvitations(userId) { pendingInvitationId ->
                invitedChatRoomId.value = pendingInvitationId
            }
        }
        onDispose {
            invitationListener?.remove()
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem.Home, BottomNavItem.Message, BottomNavItem.Gallery, BottomNavItem.Settings
    )

    val homeViewModel: HomeViewModel = viewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()
    // 채팅방 존재 여부 확인 (null이나 공백이 아니면 true)
    val hasChatRoom = !homeUiState.chatRoomId.isNullOrBlank()

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 탭이 'Home'으로 변경될 때마다 데이터 새로고침 (화면 갱신 보장)
    LaunchedEffect(currentRoute) {
        if (currentRoute == BottomNavItem.Home.route) {
            homeViewModel.loadInitialData()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                    // [수정됨] 홈 화면 + 반투명 오버레이 구성
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. 실제 홈 화면 (항상 뒤에 그려짐)
                        HomeScreen(
                            homeViewModel = homeViewModel,
                            invitedChatRoomId = invitedChatRoomId,
                            onAcceptInvitation = { }, // MainActivity 레벨에서 처리하므로 비워둠
                            onDismissInvitation = { }
                        )

                        // 2. 채팅방이 없을 때만 나타나는 반투명 터치 차단막
                        if (!hasChatRoom) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)) // 반투명 배경
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null // 클릭 리플 효과 제거
                                    ) {
                                        // 터치 이벤트를 소비하여 뒤쪽(홈 화면) 터치를 막음
                                    }
                            )
                        }
                    }
                }
                composable(BottomNavItem.Message.route) {
                    val messageViewModel: MessageViewModel = viewModel(
                        factory = MessageViewModel.MessageViewModelFactory(AppRepository)
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

        // [전역 초대 알림]
        if (invitedChatRoomId.value != null) {
            InvitationDialog(
                onAccept = {
                    val inviteId = invitedChatRoomId.value!!
                    // ViewModel을 통해 수락 요청
                    homeViewModel.acceptInvitation(inviteId) { acceptedChatRoomId ->
                        // 콜백: 수락 성공 시 실행됨
                        if (acceptedChatRoomId != null) {
                            // 1. 다이얼로그 닫기
                            invitedChatRoomId.value = null

                            // 2. 데이터 즉시 새로고침 (uiState의 chatRoomId 업데이트 -> hasChatRoom이 true가 됨 -> 오버레이 사라짐)
                            homeViewModel.loadInitialData()

                            // 3. 확실한 갱신을 위해 홈 화면으로 네비게이션 (이미 홈이어도 새로고침 효과)
                            innerNavController.navigate(BottomNavItem.Home.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                onDismiss = { invitedChatRoomId.value = null }
            )
        }
    }
}