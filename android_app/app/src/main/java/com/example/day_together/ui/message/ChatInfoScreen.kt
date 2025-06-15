package com.example.day_together.ui.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.day_together.R
import com.example.day_together.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.day_together.ui.dialogs.InviteMemberDialog


data class FamilyMember(
    val id: String,
    val name: String,
    val profileImageRes: Int = R.drawable.ic_profile_placeholder
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(navController: NavController) {
    val myUserName = "사용자 이름"
    val creationDate = "2024.01.01"
    val familyMembers = listOf(
        FamilyMember("1", "엄마"),
        FamilyMember("2", "아빠"),
        FamilyMember("3", "동생"),
        FamilyMember("4", "누나"),
        FamilyMember("5", "형")
    )

    var showInviteDialog by remember { mutableStateOf(false) }

    if (showInviteDialog) {
        InviteMemberDialog(
            onDismissRequest = { showInviteDialog = false },
            onInviteClick = { email ->
                println("초대 이메일: $email")
                showInviteDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
        containerColor = ScreenBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "내 프로필 이미지",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = myUserName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = GothicA1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "하루함께 개설일",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontFamily = GothicA1)
                    )
                    Text(
                        text = creationDate,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontFamily = GothicA1, fontWeight = FontWeight.Medium)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = TextPrimary.copy(alpha = 0.5f), thickness = 1.dp)
            }

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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = GothicA1)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = TextPrimary.copy(alpha = 0.5f), thickness = 1.dp)
            }

            items(familyMembers) { member ->
                FamilyMemberItem(member = member)
                Divider(color = TextPrimary.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(start = 40.dp))
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = TextPrimary.copy(alpha = 0.5f), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInviteDialog = true }
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
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontFamily = GothicA1, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FamilyMemberItem(member: FamilyMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = member.profileImageRes),
            contentDescription = "${member.name} 프로필 이미지",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontFamily = GothicA1)
        )
    }
}



@Preview(showBackground = true, name = "ChatInfoScreen Preview")
@Composable
fun ChatInfoScreenPreview() {
    Day_togetherTheme {
        ChatInfoScreen(navController = rememberNavController())
    }
}