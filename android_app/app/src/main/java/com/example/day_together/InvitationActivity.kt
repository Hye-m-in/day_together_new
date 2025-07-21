package com.example.day_together

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day_together.ui.theme.Day_togetherTheme

class InvitationActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatRoomId = intent.getStringExtra("chatRoomId")

        setContent {
            Day_togetherTheme {
                Scaffold {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("가족 채팅방에 초대받았습니다.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("초대된 채팅방 ID: $chatRoomId")
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (chatRoomId != null) {
                                    ChatRoomManager.acceptInvitation(chatRoomId) { success, message ->
                                        if (success) {
                                            val intent = Intent(
                                                this@InvitationActivity,
                                                ChatActivity::class.java
                                            )
                                            intent.putExtra("chatRoomId", chatRoomId)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(
                                                this@InvitationActivity,
                                                message ?: "입장 실패",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("입장하기")
                        }
                    }
                }
            }
        }
    }
}