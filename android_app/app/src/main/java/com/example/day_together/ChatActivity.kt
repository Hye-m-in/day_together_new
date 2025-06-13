package com.example.day_together

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.day_together.ui.theme.Day_togetherTheme
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Tasks
import java.util.*

data class ChatMessage(
    val content: String,
    val sender: String,
    val timestamp: Date = Date()
)

class ChatActivity : ComponentActivity() {

    private val db = FirebaseService.db
    private val auth = FirebaseService.auth
    private var currentUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인 상태가 아닙니다", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val messages = mutableStateListOf<ChatMessage>()
        var chatRoomId by mutableStateOf<String?>(null)
        var showInviteDialog by mutableStateOf(false)
        var invitedUserIdInput by mutableStateOf("")

        // 사용자 이름 불러오기
        val uid = auth.currentUser?.uid ?: return
        db.collection("members")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                currentUserName = document.getString("name") ?: "Unknown"
            }

        // 채팅방 ID 가져오기 및 메시지 리스닝
        fetchAcceptedChatRoomId(
            onFound = { id ->
                chatRoomId = id
                listenForMessages(id, messages)
            },
            onNotFound = {
                chatRoomId = null
            }
        )

        setContent {
            Day_togetherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (chatRoomId != null) {
                            ChatScreen(
                                messages = messages,
                                onSend = { sendMessage(chatRoomId!!, it, currentUserName) },
                                currentUserName = currentUserName,
                                onInviteClick = { showInviteDialog = true }
                            )

                            if (showInviteDialog) {
                                InviteDialog(
                                    invitedUserIdInput = invitedUserIdInput,
                                    onValueChange = { invitedUserIdInput = it },
                                    onDismiss = { showInviteDialog = false },
                                    onInvite = {
                                        showInviteDialog = false
                                        val inviterId = auth.currentUser?.uid ?: return@InviteDialog
                                        val invitees = invitedUserIdInput
                                            .split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }

                                        ChatRoomManager.inviteMembers(
                                            chatRoomId = chatRoomId!!,
                                            inviterUserId = inviterId,
                                            invitedUserId = invitees,
                                            onComplete = { success, error ->
                                                runOnUiThread {
                                                    Toast.makeText(
                                                        this@ChatActivity,
                                                        if (success) "초대 성공" else "초대 실패: $error",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        } else {
                            EmptyChatRoomScreen(onInviteClick = {
                                showInviteDialog = true
                                val inviterId = auth.currentUser?.uid ?: return@EmptyChatRoomScreen
                                createNewChatRoom(inviterId) { newRoomId ->
                                    chatRoomId = newRoomId
                                    listenForMessages(newRoomId, messages)
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    private fun fetchAcceptedChatRoomId(onFound: (String) -> Unit, onNotFound: () -> Unit) {
        val user = auth.currentUser ?: return onNotFound()

        db.collection("users")
            .document(user.uid)
            .collection("invitations")
            .whereEqualTo("status", "accepted")
            .limit(1) // 여러 개일 수 있으니 제한 두기
            .get()
            .addOnSuccessListener { documents ->
                val chatRoomId = documents.firstOrNull()?.getString("chatRoomId")
                if (chatRoomId != null) onFound(chatRoomId) else onNotFound()
            }
            .addOnFailureListener { onNotFound() }
    }

    private fun createNewChatRoom(inviterUserId: String, onComplete: (String) -> Unit) {
        val newChatRoomRef = db.collection("chatRooms").document()
        val chatRoomId = newChatRoomRef.id

        val data = hashMapOf(
            "members" to listOf(inviterUserId),
            "invitedUsers" to emptyList<String>(),
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        newChatRoomRef.set(data)
            .addOnSuccessListener { onComplete(chatRoomId) }
            .addOnFailureListener {
                Toast.makeText(this, "채팅방 생성 실패", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun sendMessage(chatRoomId: String, text: String, sender: String) {
        if (sender.isBlank()) return

        val message = hashMapOf(
            "sender" to sender,
            "content" to text,
            "timestamp" to Date()
        )
        db.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
    }

    private fun listenForMessages(chatRoomId: String, messages: SnapshotStateList<ChatMessage>) {
        db.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val newMessages = snapshot.documents.map { doc ->
                        ChatMessage(
                            content = doc.getString("content") ?: "",
                            sender = doc.getString("sender") ?: "unknown",
                            timestamp = doc.getDate("timestamp") ?: Date()
                        )
                    }
                    messages.clear()
                    messages.addAll(newMessages)
                }
            }
    }

    @Composable
    fun ChatScreen(
        messages: List<ChatMessage>,
        onSend: (String) -> Unit,
        currentUserName: String,
        onInviteClick: () -> Unit
    ) {
        var input by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (messages.isEmpty()) {
                // 빈 채팅방 안내
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("아직 대화가 없습니다.\n가족을 초대해 대화를 시작해보세요.", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onInviteClick) {
                            Text("가족 초대")
                        }
                    }
                }
            } else {
                // 메세지 목록 표시
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(messages.size) { index ->
                        val msg = messages[index]
                        MessageBubble(message = msg, isMine = msg.sender == currentUserName)
                    }
                }

                // 입력창
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f).padding(8.dp),
                        placeholder = { Text("메시지를 입력하세요") }
                    )
                    Button(onClick = {
                        if (input.isNotBlank()) {
                            onSend(input)
                            input = ""
                        }
                    }) {
                        Text("전송")
                    }
                }
            }
        }
    }

    @Composable
    fun MessageBubble(message: ChatMessage, isMine: Boolean) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Text(
                text = message.sender,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    @Composable
    fun EmptyChatRoomScreen(onInviteClick: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("아직 채팅방이 없습니다.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onInviteClick) {
                    Text("가족 초대하기")
                }
            }
        }
    }

    @Composable
    fun InviteDialog(
        invitedUserIdInput: String,
        onValueChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onInvite: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("가족 초대") },
            text = {
                Column {
                    Text("초대할 사용자의 UID 또는 이메일을 입력하세요")
                    TextField(
                        value = invitedUserIdInput,
                        onValueChange = onValueChange,
                        placeholder = { Text("예: user123 또는 user@email.com") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = onInvite) {
                    Text("초대")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("취소")
                }
            }
        )
    }
}