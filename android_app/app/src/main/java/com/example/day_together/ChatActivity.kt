package com.example.day_together

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

// 채팅 메세지 데이터 클래스
data class ChatMessage(
    val content: String,
    val sender: String,
    val timestamp: Date = Date(),
    val type: String
)

class ChatActivity : ComponentActivity() {
    private val db = FirebaseService.db
    private val auth = FirebaseService.auth
    private var currentUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 로그인 여부 확인
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인 상태가 아닙니다", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 사용자 이름 불러오기
        val uid = auth.currentUser!!.uid
        db.collection("users")
            .document(uid).get()
            .addOnSuccessListener { document ->
                currentUserName = document.getString("name") ?: "Unknown"
            }

        setContent {
            Day_togetherTheme {
                val chatRoomId = remember { mutableStateOf<String?>(null) } // 현재 채팅방 ID
                val isMessagesLoading = remember { mutableStateOf(true) }   // 메세지 로딩 상태
                val messages = remember { mutableStateListOf<ChatMessage>() }     // 메세지 목록
                val showInviteDialog = remember { mutableStateOf(false) }   // 초대 다이얼로그 표시 여부
                val invitedUserIdInput = remember { mutableStateOf("") }    // 초대할 사용자 입력 값

                // 최초 진입 시 chatRoomId 확인 및 메세지 수신 시작
                LaunchedEffect(Unit) {
                    fetchAcceptedChatRoomId(
                        onFound = {
                            chatRoomId.value = it
                            listenForMessages(it, messages, isMessagesLoading)  // 메세지 실시간 수신 시작
                        },
                        onNotFound = {
                            chatRoomId.value = null
                            isMessagesLoading.value = false
                        }
                    )
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (chatRoomId.value != null) {  // 채팅방 존재하는 경우
                            when {
                                isMessagesLoading.value -> {
                                    // 메세지 로딩 표시
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                else -> {
                                    // 채팅 화면 UI 출력
                                    ChatScreen(
                                        messages = messages,
                                        currentUserName = currentUserName,
                                        onSend = {
                                            sendMessage(chatRoomId.value!!, it, currentUserName)
                                        },
                                        onInviteClick = {
                                            showInviteDialog.value = true
                                        }
                                    )
                                }
                            }


                            // 초대 다이얼로그가 활성화된 경우
                            if (showInviteDialog.value) {
                                InviteDialog(
                                    invitedUserIdInput = invitedUserIdInput.value,
                                    onValueChange = { invitedUserIdInput.value = it },
                                    onDismiss = { showInviteDialog.value = false },
                                    onInvite = {
                                        val invitees = invitedUserIdInput.value.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        if (invitees.isEmpty()) return@InviteDialog

                                        val inviterId = auth.currentUser?.uid ?: return@InviteDialog
                                        showInviteDialog.value = false

                                        //기존 firestore에 초대 저장
                                        ChatRoomManager.inviteMembers(
                                            chatRoomId = chatRoomId.value!!,
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

                                                // 초대 성공하면 cloud function 호출(푸시 알림 발송)
                                                if (success) {
                                                    sendFamilyInvites(
                                                        emails = invitees,
                                                        roomName = "가족 채팅방",
                                                    ) { successFunc, errMsg ->
                                                        runOnUiThread {
                                                            if (!successFunc) {
                                                                Toast.makeText(this@ChatActivity, "Cloud Function 실패: $errMsg", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        } else {
                            // 채팅방 생성되지 않은 경우
                            EmptyChatRoomScreen(onInviteClick = {
                                showInviteDialog.value = true
                                // 채팅방 생성 후 메세지 수신 시작
                                val inviterId = auth.currentUser?.uid ?: return@EmptyChatRoomScreen
                                createNewChatRoom(inviterId) { newRoomId ->
                                    chatRoomId.value = newRoomId
                                    listenForMessages(newRoomId, messages, isMessagesLoading)
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    /**
     * 수락된 초대가 있는지 확인한 뒤 채팅방 ID를 가져오는 함수.
     * 초대받은 경우 invitations 컬렉션에서 status=accepted를 검색하고,
     * 없을 경우 자신이 만든 채팅방(chatRooms 컬렉션에서 members 배열에 uid 포함)을 검색함.
     */
    private fun fetchAcceptedChatRoomId(onFound: (String) -> Unit, onNotFound: () -> Unit) {
        val user = auth.currentUser ?: return onNotFound()

        db.collection("users")
            //초대받은 경우: invitatoins에서 accepted 찾기
            .document(user.uid)
            .collection("invitations")
            .whereEqualTo("status", "accepted")
            .limit(1) // 여러 개일 수 있으니 제한 두기. 첫 번째만 가져오기
            .get()
            .addOnSuccessListener { documents ->
                val chatRoomId = documents.firstOrNull()?.getString("chatRoomId")
                if (chatRoomId != null){
                    Log.d("DEBUG", "chatRoomId from members: $chatRoomId")
                onFound(chatRoomId)}
                else {
                    //초대받은 채팅방이 없다면, 내가 만든 채팅방 조회
                    db.collection("chatRooms")
                        .whereArrayContains("members", user.uid)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { chatRooms ->
                            val ownChatRoomId = chatRooms.firstOrNull()?.id
                            if (ownChatRoomId != null) {
                                Log.d("DEBUG", "chatRoomId from members: $ownChatRoomId")
                                onFound(ownChatRoomId)
                            } else {
                                onNotFound()
                            }
                        }
                        .addOnFailureListener {
                            onNotFound()
                        }
                }
            }
            .addOnFailureListener { onNotFound() }
    }

    // 새로운 채팅방을 생성하는 함수.
    // 현재 로그인된 사용자를 멤버로 추가하고, invitedUsers는 빈 리스트로 초기화.
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


    // 채팅방에 메시지 전송
    private fun sendMessage(chatRoomId: String, text: String, sender: String) {
        if (text.isBlank() || sender.isBlank()) return

        val message = hashMapOf(
            "sender" to sender,
            "content" to text,
            "timestamp" to Date(),
            "type" to "text"
        )
        db.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
    }

    // 이미지 파일 전송
    private fun sendImageMessage(chatRoomId: String, sender: String, imageUri: Uri, context: Context){
        val storageRef = FirebaseStorage.getInstance().reference
        val fileName = "${System.currentTimeMillis()}_${sender}.jpg"
        val imageRef = storageRef.child("chat_images/$chatRoomId/$fileName")

        imageRef.putFile(imageUri)
            .continueWithTask { task ->
                if(!task.isSuccessful){
                    throw task.exception ?: Exception("Upload failed")
                }
                imageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                val imageMessage = hashMapOf(
                    "sender" to sender,
                    "timestamp" to Date(),
                    "type" to "image",
                    "imageUrl" to uri.toString()
                )
                Firebase.firestore.collection("chatRooms")
                    .document(chatRoomId)
                    .collection("messages")
                    .add(imageMessage)
            }.addOnFailureListener { e ->
                Toast.makeText(context, "이미지 전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 채팅방에서 실시간으로 메세지 수신 및 UI 리스트 업데이트
    private fun listenForMessages(chatRoomId: String, messages: SnapshotStateList<ChatMessage>, isMessagesLoading: MutableState<Boolean>) {
        isMessagesLoading.value = true  // 시작할 때 로딩 표시

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
                            timestamp = doc.getDate("timestamp") ?: Date(),
                            type = doc.getString("type") ?: "text"

                        )
                    }
                    Log.d("DEBUG", "messages loaded: ${newMessages.size}")
                    messages.clear()
                    messages.addAll(newMessages)
                    isMessagesLoading.value = false  // 메시지 불러오기 끝
                } else{
                    Log.d("DEBUG", "No snapshot received.")
                }
            }
    }

    // 입력된 이메일 리스트로 가족 초대 Cloud Function 호출
    // 이메일과 채팅방 이름(roomName)을 전달하고 결과 콜백을 통해 성공 여부 반환
    private fun sendFamilyInvites(
        emails: List<String>,
        roomName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser ?: return onResult(false, "인증되지 않은 사용자입니다.")
        user.getIdToken(true).addOnSuccessListener {
            val data = hashMapOf(
                "emails" to emails,
                "roomName" to roomName
            )
            com.google.firebase.functions.FirebaseFunctions
                .getInstance("us-central1")
                .getHttpsCallable("sendFamilyInvites")
                .call(data)
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e -> onResult(false, e.message) }
        }.addOnFailureListener {
            onResult(false, "토큰 획득 실패: ${it.message}")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatScreen(
        messages: List<ChatMessage>,
        onSend: (String) -> Unit,
        currentUserName: String,
        onInviteClick: () -> Unit
    ) {
        var input by remember { mutableStateOf("") }

        Scaffold(
            // 상단 앱 바 : 타일과 초대 버튼
            topBar = {
                TopAppBar(
                    title = { Text("가족 채팅방") },
                    actions = {
                        IconButton(onClick = onInviteClick) {
                            Icon(Icons.Default.Add, contentDescription = "가족 초대")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                if (messages.isEmpty()) {
                    // 메세지가 없을 경우 안내 문구와 초대 버튼 표시
                    EmptyChatRoomScreen (onInviteClick = onInviteClick)
                } else {
                    // 메시지 목록 표시
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(messages.size) { index ->
                            val msg = messages[index]
                            MessageBubble(message = msg, isMine = msg.sender == currentUserName)
                        }
                    }
                    // 입력창(항상 하단에 표시)
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
    }

    // 채팅 메세지 좌우 정렬해서 화면 출력
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

    // 채팅방 비어 있을 때 보여주는 안내 UI
    @Composable
    fun EmptyChatRoomScreen(onInviteClick: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "아직 대화가 없습니다.\n가족을 초대해 대화를 시작해보세요.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onInviteClick) {
                    Text("가족 초대")
                }
            }
        }
    }

    // 가족 초대용 이메일 입력 다이얼로그
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
                    Text("초대할 사용자의 이메일을 입력하세요")
                    TextField(
                        value = invitedUserIdInput,
                        onValueChange = onValueChange,
                        placeholder = { Text("예: user@email.com") }
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