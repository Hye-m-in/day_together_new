package com.example.day_together.ui.message

// 기본 UI 컴포넌트 임포트
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*

// 상태 저장 및 관찰을 위한 Compose API
import androidx.compose.runtime.*

// UI 요소 속성 정의 관련 임포트
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 네비게이션 기능
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// 리소스와 테마 임포트
import com.example.day_together.R
import com.example.day_together.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview

// 초대 다이얼로그
import com.example.day_together.ui.dialogs.InviteMemberDialog

// 가족 멤버를 표현하는 데이터 클래스 정의
data class FamilyMember(
    val id: String,
    val name: String,
    val profileImageRes: Int = R.drawable.ic_add_photo // 기본 이미지 리소스 설정
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(navController: NavController) {
    // 사용자 정보와 가족방 생성일
    val myUserName = "사용자 이름"
    val creationDate = "2024.01.01"

    // 참여 중인 가족 멤버 리스트
    val familyMembers = listOf(
        FamilyMember("1", "엄마"),
        FamilyMember("2", "아빠"),
        FamilyMember("3", "동생"),
        FamilyMember("4", "누나"),
        FamilyMember("5", "형")
    )

    // 초대 다이얼로그 표시 여부 상태 변수
    var showInviteDialog by remember { mutableStateOf(false) }

    // 초대 다이얼로그가 true일 때 화면에 표시
    if (showInviteDialog) {
        InviteMemberDialog(
            onDismissRequest = { showInviteDialog = false }, // 다이얼로그 닫기
            onInviteClick = { email ->
                println("초대 이메일: $email") // 입력된 이메일 로그 출력
                showInviteDialog = false // 다이얼로그 닫기
            }
        )
    }

    // 화면 상단 AppBar 포함한 Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                title = { }, // 타이틀 비워둠
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
            )
        },
        containerColor = ScreenBackground // 전체 배경색 설정
    ) { paddingValues ->
        // 세로 스크롤 가능한 LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // 프로필 및 개설일 정보 영역
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 사용자 프로필 이미지 (현재는 기본 이미지)
                    Image(
                        painter = painterResource(id = R.drawable.ic_add_photo),
                        contentDescription = "내 프로필 이미지",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 사용자 이름 텍스트
                    Text(
                        text = myUserName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = GothicA1
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 설명 텍스트와 개설일 날짜
                    Text(
                        text = "하루함께 개설일",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontFamily = GothicA1
                        )
                    )
                    Text(
                        text = creationDate,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontFamily = GothicA1,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = TextPrimary.copy(alpha = 0.5f), thickness = 1.dp)
            }

            // 참여 중인 가족 멤버 섹션 헤더
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_group),
                        contentDescription = "가족 멤버 아이콘",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "참여 중인 가족 멤버",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = GothicA1
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = TextPrimary.copy(alpha = 0.5f), thickness = 1.dp)
            }

            // 가족 멤버 목록 표시
            items(familyMembers) { member ->
                FamilyMemberItem(member = member) // 개별 아이템
                Divider(
                    color = TextPrimary.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }

            // '하루함께 초대하기' 버튼
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = TextPrimary.copy(alpha = 0.5f), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInviteDialog = true } // 클릭 시 다이얼로그 열기
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_message_invite),
                        contentDescription = "하루함께 초대하기 아이콘",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "하루함께 초대하기",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            fontFamily = GothicA1,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// 가족 멤버 항목 UI 정의
@Composable
fun FamilyMemberItem(member: FamilyMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 멤버의 프로필 이미지
        Image(
            painter = painterResource(id = member.profileImageRes),
            contentDescription = "${member.name} 프로필 이미지",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))

        // 멤버 이름 텍스트
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary,
                fontFamily = GothicA1
            )
        )
    }
}

// 미리 보기용 Preview 컴포저블 함수
@Preview(showBackground = true, name = "ChatInfoScreen Preview")
@Composable
fun ChatInfoScreenPreview() {
    Day_togetherTheme {
        ChatInfoScreen(navController = rememberNavController())
    }
}
