package com.example.day_together

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.day_together.data.repository.QuestionRepository
import com.example.day_together.navigation.AppNavigation
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

/**
 * MainActivity는 앱이 실행될 때 가장 먼저 시작되는 화면
 * Jetpack Compose를 사용하여 전체 UI 구성하는 역할
 */
class MainActivity : ComponentActivity() {
    /**
     * Activity가 생성될 때 호출되는 함수
     * 여기서 앱의 초기 화면 내용 설정
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent 블록 안에서 Composable 함수를 호출하여 UI 그림
        setContent {
            // 앱의 전체적인 디자인 테마 적용
            Day_togetherTheme {
                // navigation 패키지에 정의된 앱의 전체 화면 전환 로직을 호출
                AppNavigation()
            }
        }
    }
}

/**
 * 하단 네비게이션 바를 포함하는 메인 화면의 전체적인 UI 구조를 정의하는 Composable 함수
 * -> 로그인 후에 나타나는 핵심 화면들의 컨테이너 역할
 *
 * @param appNavController 앱의 최상위 NavController: 로그인 화면 등 메인 화면 바깥의 화면으로 이동할 때 사용
 */
@Composable
fun MainScreen(appNavController: NavHostController) {
    // MainScreen 내부(하단 바)의 화면 전환을 독립적으로 관리하기 위한 NavController
    val innerNavController = rememberNavController()
    // 새로운 채팅방 초대를 받았을 때, 해당 채팅방의 ID를 저장하는 상태 변수
    val invitedChatRoomId = remember { mutableStateOf<String?>(null) }
    // Firebase 실시간 리스너를 관리하기 위한 변수 -> 화면이 사라질 때 리스너 정리하는 데 사용
    var invitationListener: ListenerRegistration? by remember { mutableStateOf(null) }

    // Composable이 화면에 나타나거나 사라질 때 특정 코드 실행하기 위한 효과 핸들러
    // 여기서는 사용자가 로그인/로그아웃할 때마다 새로운 초대 리스너를 설정하거나 제거함
    DisposableEffect(ChatRoomManager.auth.currentUser) {
        val userId = ChatRoomManager.auth.currentUser?.uid
        if (userId != null) {
            // 현재 로그인한 사용자의 ID로 초대를 실시간으로 감지하는 리스너 설정
            invitationListener = ChatRoomManager.listenForInvitations(userId) { pendingInvitationId ->
                // 새로운 초대가 감지되면 상태 변수에 채팅방 ID를 저장하여 UI에 알림
                invitedChatRoomId.value = pendingInvitationId
            }
        }
        // 화면에서 벗어나거나 Composable이 재구성될 때 실행될 정리 로직
        onDispose {
            // 메모리 누수를 방지하기 위해 활성화된 리스너 제거
            invitationListener?.remove()
        }
    }

    // 하단 네비게이션 바에 표시될 아이템 목록
    val bottomNavItems = listOf(
        BottomNavItem.Home, BottomNavItem.Message, BottomNavItem.Gallery, BottomNavItem.Settings
    )

    // HomeViewModel을 NavHost보다 상위 스코프에서 생성 -> HomeScreen이 사라져도 ViewModel이 유지되고, onAcceptInvitation에서 동일한 ViewModel 참조 가능
    val homeViewModel: HomeViewModel = viewModel()


    Scaffold(
        // 하단 네비게이션 바 UI 정의
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                // 현재 네비게이션 스택의 최상단 항목을 실시간으로 감지
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                // 현재 보여지고 있는 화면의 목적지 정보를 가져옴
                val currentDestination = navBackStackEntry?.destination

                // bottomNavItems 목록에 있는 각 아이템을 순회하면서 NavigationBarItem을 만듦
                bottomNavItems.forEach { screen ->
                    // 현재 목적지가 현재 아이템의 경로 계층에 속하는지 확인하면서 선택 상태를 결정함
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.iconResId),
                                contentDescription = screen.label,
                                // 선택 상태에 따라 아이콘 색상을 다르게 설정함
                                tint = if (isSelected) NavIconSelected else NavIconUnselected
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            // 아이콘 클릭 시 해당 화면으로 이동
                            innerNavController.navigate(screen.route) {
                                // 백스택의 시작 목적지까지 꺼내서(pop) 중복 스택이 쌓이는 것을 방지함
                                popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                // 이미 스택에 해당 화면이 있으면 새로 만들지 않고 재사용
                                launchSingleTop = true
                                // 이전 상태 복원
                                restoreState = true
                            }
                        },
                        alwaysShowLabel = false, // 아이콘 아래에 라벨 텍스트를 항상 표시할지 여부
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent) // 선택 시 배경 효과를 투명하게 처리함
                    )
                }
            }
        }
    ) { innerPadding ->
        // Scaffold의 본문 영역에 들어갈 내용을 정의
        // 하단 바 내부의 화면 전환을 담당하는 NavHost
        NavHost(
            navController = innerNavController,
            startDestination = BottomNavItem.Home.route, // 시작 화면은 홈으로 설정
            modifier = Modifier.padding(innerPadding) // Scaffold가 제공하는 패딩을 적용하여 하단 바와 겹치지 않도록 함
        ) {
            // 홈경로일 때 HomeScreen을 보여줌
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    appNavController = appNavController,
                    homeViewModel = homeViewModel, // 상위 스코프의 ViewModel 전달
                    invitedChatRoomId = invitedChatRoomId,
                    onAcceptInvitation = { chatRoomId ->
                        ChatRoomManager.acceptInvitation(chatRoomId) { success, _ ->
                            if (success) {
                                // 초대 수락 성공 시, 개인 일정을 가족방으로 이전하도록 VM에 요청
                                homeViewModel.migratePersonalEventsToFamilyRoom(chatRoomId)
                                invitedChatRoomId.value = null // 성공 시 초대 상태 초기화
                            }
                        }
                    },
                    onDismissInvitation = {
                        // TODO: 초대 거절 로직 구현 필요
                        invitedChatRoomId.value = null
                    }
                )
            }
            // 메시지 경로일 때 MessageScreen 보여줌
            composable(BottomNavItem.Message.route) {
                val messageViewModel: MessageViewModel = viewModel(
                    factory = MessageViewModel.MessageViewModelFactory(
                        AppRepository,
                        QuestionRepository()
                    )
                )
                MessageScreen(navController = appNavController, viewModel = messageViewModel)
            }
            // 갤러리 경로일 때 GalleryScreen을 보여줌
            composable(BottomNavItem.Gallery.route) {
                GalleryScreen(navController = appNavController)
            }
            // 설정 경로일 때 SettingsScreen을 보여줌
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(appNavController = appNavController)
            }
        }
    }
}