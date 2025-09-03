/**
 * package com.example.day_together
 *
 * import android.annotation.SuppressLint
 * import android.widget.Toast
 * import androidx.compose.foundation.clickable
 * import androidx.compose.foundation.layout.Column
 * import androidx.compose.foundation.layout.Spacer
 * import androidx.compose.foundation.layout.fillMaxSize
 * import androidx.compose.foundation.layout.height
 * import androidx.compose.foundation.layout.padding
 * import androidx.compose.material3.AlertDialog
 * import androidx.compose.material3.Button
 * import androidx.compose.material3.MaterialTheme
 * import androidx.compose.material3.Scaffold
 * import androidx.compose.material3.Text
 * import androidx.compose.material3.TextButton
 * import androidx.compose.runtime.Composable
 * import androidx.compose.runtime.MutableState
 * import androidx.compose.ui.Modifier
 * import androidx.compose.ui.platform.LocalContext
 * import androidx.compose.ui.unit.dp
 * import androidx.navigation.NavController
 * import com.example.day_together.ChatRoomManager.auth
 * import com.example.day_together.ChatRoomManager.db
 *
 * /**
 *  * HomeScreen.kt
 *  *화면 전환을 위해 NavController 사용
 *  * LoginActivity, ChatActivity, InvitationActivity를 직접 호출하지 않고, NavController를 통해 정의된 경로로 이동
 *  */
 *
 * @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
 * @Composable
 * fun HomeScreen(
 *     // NavController를 파라미터로 추가해 화면 전환 담당
 *     navController: NavController,
 *     invitedChatRoomId: MutableState<String?>,
 *     onAcceptInvitation: (String) -> Unit,
 *     onDismissInvitation: () -> Unit
 * ) {
 *     val context = LocalContext.current
 *
 *     val currentUserId = auth.currentUser?.uid
 *
 *     Scaffold {
 *         Column(
 *             modifier = Modifier
 *                 .fillMaxSize()
 *                 .padding(16.dp)
 *         ) {
 *             Button(onClick = {
 *                 if (currentUserId != null) {
 *                     // 사용자의 초대 상태가 있는지 확인
 *                     db.collection("users").document(currentUserId).get()
 *                         .addOnSuccessListener { doc ->
 *                             val invitedId = doc.getString("invitedChatRoomId")
 *                             if (invitedId != null) {
 *                                 // 초대 상태가 있는 경우, NavController를 사용해 화면 이동
 *                                 goToChatOrInvitation(navController, invitedId)
 *                             } else {
 *                                 Toast.makeText(context, "참여 중인 채팅방이 없습니다", Toast.LENGTH_SHORT).show()
 *                                 // 채팅방 ID가 없으므로 채팅방 목록 화면 등으로 이동하는 로직 필요
 *                                 // 임시로 "chat_list_route"로 이동하도록 가정
 *                                 navController.navigate("chat_list_route")
 *                             }
 *                         }
 *                 }
 *             }) {
 *                 Text("채팅하러 가기")
 *             }
 *
 *             Spacer(modifier = Modifier.height(16.dp))
 *
 *
 *             Text(
 *                 text = "로그아웃",
 *                 color = MaterialTheme.colorScheme.primary,
 *                 style = MaterialTheme.typography.bodyMedium,
 *                 modifier = Modifier
 *                     .clickable {
 *                         // 로그아웃 로직 유지
 *                         AuthManager.logoutUser()
 *
 *
 *                         // NavController를 사용하여 로그인 화면으로 이동
 *                         navController.navigate("login_route") {
 *                             // 백스택을 모두 지워서 로그인 화면에서 뒤로가기를 눌렀을 홈 화면으로 다시 돌아오는 것을 방지
 *                             popUpTo(navController.graph.startDestinationId) {
 *                                 inclusive = true
 *                             }
 *                         }
 *                     }
 *             )
 *
 *             // 초대 다이얼로그 표시
 *             invitedChatRoomId.value?.let { chatRoomId ->
 *                 InvitationDialog(
 *                     onAccept = { onAcceptInvitation(chatRoomId) },
 *                     onDismiss = onDismissInvitation
 *                 )
 *             }
 *         }
 *     }
 * }
 *
 * // 채팅방 상태에 따라 Chat 또는 Invitation으로 이동하는 함수
 * fun goToChatOrInvitation(navController: NavController, chatRoomId: String) {
 *     val uid = auth.currentUser?.uid ?: return
 *     val context = navController.context // Toast를 위해 context가 필요하면 NavController에서 얻을 수 있음
 *
 *     val invitationRef = db.collection("users")
 *         .document(uid)
 *         .collection("invitations")
 *         .document(chatRoomId)
 *
 *     invitationRef.get()
 *         .addOnSuccessListener { document ->
 *             val status = document.getString("status")
 *             if (status == "accepted") {
 *                 // 수락된 경우: 채팅 화면으로 이동 (경로에 chatRoomId를 전달)
 *                 // NavHost에 "chat_route/{chatRoomId}" 형태의 경로가 정의되어 있어야 함
 *                 navController.navigate("chat_route/$chatRoomId")
 *             } else {
 *                 // 아직 수락하지 않은 경우: 초대 수락 화면으로 이동
 *                 // NavHost에 "invitation_route/{chatRoomId}" 형태의 경로가 정의되어 있어야 함
 *                 navController.navigate("invitation_route/$chatRoomId")
 *             }
 *         }
 *         .addOnFailureListener { e ->
 *             Toast.makeText(context, "초대 정보 조회 실패: ${e.message}", Toast.LENGTH_SHORT).show()
 *         }
 * }
 *
 * @Composable
 * fun InvitationDialog(
 *     onAccept: () -> Unit,
 *     onDismiss: () -> Unit
 * ) {
 *     AlertDialog(
 *         onDismissRequest = onDismiss,
 *         title = { Text("초대 도착") },
 *         text = { Text("가족 채팅방에 초대받았습니다. 입장하시겠습니까?") },
 *         confirmButton = {
 *             Button(onClick = onAccept) {
 *                 Text("입장하기")
 *             }
 *         },
 *         dismissButton = {
 *             TextButton(onClick = onDismiss) {
 *                 Text("나중에")
 *             }
 *         }
 *     )
 * }
 */