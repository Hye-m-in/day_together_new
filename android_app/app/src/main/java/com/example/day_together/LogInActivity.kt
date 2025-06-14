package com.example.day_together

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.day_together.ui.theme.Day_togetherTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private lateinit var googleSignInClient: GoogleSignInClient
private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 이미 로그인되어 있으면 바로 MainActivity 이동
        if (AuthManager.isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }


        // Google Sign-In 옵션 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id)) // firebase console → OAuth 2.0 클라이언트 ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // LaunchActivityResult 등록
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                AuthManager.loginWithGoogleIdToken(account.idToken!!) { success, msg ->
                    if (success) {
                        Toast.makeText(this, "구글 로그인 성공", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "로그인 실패: $msg", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_SIGNIN_ERROR", "예외 발생", e)
                Toast.makeText(this, "구글 로그인 예외: ${e.message}", Toast.LENGTH_LONG).show()

            }
        }

        setContent {
            Day_togetherTheme {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("로그인", style = MaterialTheme.typography.headlineMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(value = email, onValueChange = { email = it }, label = { Text("이메일") })
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("비밀번호") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(this@LoginActivity, "이메일과 비밀번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.length < 6) {
                            Toast.makeText(this@LoginActivity, "비밀번호는 최소 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        AuthManager.loginUser(email, password) { success, message ->
                            if (success) {
                                Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, "로그인 실패: $message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("로그인")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Google로 로그인")
                    }

                    Button(onClick = {
                        startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
                    }) {
                        Text("회원가입 하러가기")
                    }
                }
            }
        }
    }
}