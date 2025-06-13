package com.example.day_together

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day_together.ui.theme.Day_togetherTheme

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Day_togetherTheme {
                var name by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var position by remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("회원가입", style = MaterialTheme.typography.headlineMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(value = name, onValueChange = { name = it }, label = { Text("이름") })
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(value = email, onValueChange = { email = it }, label = { Text("이메일") })
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(value = password, onValueChange = { password = it }, label = { Text("비밀번호") })
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(value = position, onValueChange = { position = it }, label = { Text("포지션 (ex. 아빠)") })
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        // 이름 유효성 검사
                        if (name.length < 2) {
                            Toast.makeText(this@SignUpActivity, "이름은 두 글자 이상이어야 합니다", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // 이메일 유효성 검사
                        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
                        if (!email.matches(emailRegex)) {
                            Toast.makeText(this@SignUpActivity, "유효한 이메일 주소를 입력해주세요", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // 비밀번호 유효성 검사
                        val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#\$%^&*()_+=<>?]).{8,}$")
                        if (!password.matches(passwordRegex)) {
                            Toast.makeText(
                                this@SignUpActivity,
                                "비밀번호는 영어+숫자+특수문자를 포함한 8자리 이상이어야 합니다",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        AuthManager.registerUser(name, email, password, position) { success, message ->
                            if (success) {
                                Toast.makeText(this@SignUpActivity, "회원가입 성공", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                            } else {
                                Toast.makeText(this@SignUpActivity, "회원가입 실패: $message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("회원가입")
                    }

                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("이름") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = position,
            onValueChange = { position = it },
            label = { Text("가족 역할 (예: 아빠, 엄마, 딸 등)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                isLoading = true
                AuthManager.registerUser(
                    name = name,
                    email = email,
                    password = password,
                    position = position
                ) { success, errorMessage ->
                    isLoading = false
                    if (success) {
                        onRegistered()
                    } else {
                        // 실패 처리
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "가입 중..." else "회원가입")
        }
    }
}
