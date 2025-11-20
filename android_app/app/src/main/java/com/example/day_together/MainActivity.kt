package com.example.day_together

import android.widget.Toast
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
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
    // Context를 Composable 스코프 내에서 미리 변수에 담아둠
    val context = LocalContext.current
    val innerNavController = rememberNavController()
    val invitedChatRoomId = remember { mutableStateOf<String?>(null) }
    var invitationListener: ListenerRegistration? by remember { mutableStateOf(null) }

    // 로그인 상태에 따라 초대 리스너 등록/해제
    DisposableEffect(ChatRoomManager.auth.currentUser) {
        val userId = ChatRoomManager.auth.currentUser?.uid
        if (userId != null) {
            invitationListener =
                ChatRoomManager.listenForInvitations(userId) { pendingInvitationId ->
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

    // 시작 화면 결정 로직
    // 채팅방이 있으면 Home, 없으면 Message 화면을 시작점으로
    // 이렇게 하면 채팅방 없는 유저는 아예 홈 화면에 진입할 수 없음
    val startDest = if (hasChatRoom) BottomNavItem.Home.route else BottomNavItem.Message.route

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 탭이 'Home'으로 변경될 때마다 데이터 새로고침 (화면 갱신 보장)
    LaunchedEffect(currentRoute) {
        if (currentRoute == BottomNavItem.Home.route) {
            homeViewModel.loadInitialData()
        }
    }

    // 가장 바깥 Box
    Box(modifier = Modifier.fillMaxSize()) {

        // [1층] 앱의 메인 화면들 (Scaffold)
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { screen ->
                        val isSelected =
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        // 홈 버튼 활성화/비활성화 로직
                        // 화면이 'Home'인데 채팅방이 없다면 -> 비활성화(false)
                        // 나머지 화면은 항상 활성화(true)
                        val isEnabled = if (screen == BottomNavItem.Home) hasChatRoom else true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = screen.iconResId),
                                    contentDescription = screen.label,
                                    // 비활성화되면 회색(Gray)으로 보이게 처리
                                    tint = if (isSelected) NavIconSelected
                                    else if (!isEnabled) Color.Gray.copy(alpha = 0.5f)
                                    else NavIconUnselected
                                )
                            },
                            selected = isSelected,
                            // enabled 속성 적용 (false면 클릭 안됨)
                            enabled = isEnabled,
                            onClick = {
                                // 비활성화 상태면 클릭 이벤트 무시
                                if (isEnabled) {
                                    innerNavController.navigate(screen.route) {
                                        popUpTo(innerNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                // 위에서 계산한 startDest를 사용하여 시작 화면을 동적으로 결정
                startDestination = startDest,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.route) {
                    // 홈 화면
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        invitedChatRoomId = invitedChatRoomId,
                        onAcceptInvitation = { }, // MainActivity 레벨에서 처리하므로 비워둠
                        onDismissInvitation = { }
                    )
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
        } // Scaffold 끝

        // 기존 홈 화면 초대 알림 -> 화면 전역 초대 알림
        // 채팅방 유무와 상관없이, 초대장이 오면 무조건 띄움 (화면 어디에 있든)
        if (invitedChatRoomId.value != null) {
            // Box로 감싸서 화면 중앙 정렬 및 최상단(zIndex) 보장
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f), // 혹시 모를 겹침 방지용 최상단 설정
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                InvitationDialog(
                    onAccept = {
                        val inviteId = invitedChatRoomId.value!!

                        // 로딩 표시가 필요하다면 여기서 상태 변수를 true로 변경 (선택 사항)
                        homeViewModel.acceptInvitation(inviteId) { acceptedChatRoomId ->
                            // 콜백 실행
                            if (acceptedChatRoomId != null) {
                                // [성공 시]
                                Log.d("Invitation", "초대 수락 성공: $acceptedChatRoomId")

                                invitedChatRoomId.value = null // 1. 다이얼로그 닫기
                                homeViewModel.loadInitialData() // 2. 데이터 갱신

                                // 수락 성공 시, 이제 방이 생겼으므로 'Home'으로 자동 이동!
                                innerNavController.navigate(BottomNavItem.Home.route) {
                                    popUpTo(innerNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                // [실패 시]
                                Log.e("Invitation", "초대 수락 실패")

                                // 실패했더라도 일단 다이얼로그를 닫고 로그를 남김
                                invitedChatRoomId.value = null

                                // 미리 받아둔 context 변수를 사용하여 Toast 표시
                                Toast.makeText(context, "초대 수락에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDismiss = {
                        val inviteId = invitedChatRoomId.value!!
                        homeViewModel.rejectInvitation(inviteId) {
                            // 거절 로직 완료 후 다이얼로그 닫기
                            invitedChatRoomId.value = null
                        }
                    }
                )
            }
        }

    }
}