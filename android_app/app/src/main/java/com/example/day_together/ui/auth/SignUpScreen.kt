package com.example.day_together.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.day_together.MainActivity
import com.example.day_together.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val activity = context.findActivity()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                authViewModel.onProfileImageChanged(uri)
            }
        }
    )

    LaunchedEffect(key1 = uiState.isSignUpAndLoginSuccess) {
        if (uiState.isSignUpAndLoginSuccess) {
            Toast.makeText(context, "회원가입 및 로그인 성공!", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            activity?.finish()
        }
    }

    LaunchedEffect(key1 = uiState.signUpResult) {
        uiState.signUpResult?.let { result ->
            if (result is AuthResult.Failure) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                authViewModel.clearSignUpResult()
            }
        }
    }

    val isFamilyMemberSelected = uiState.familyMemberSelections.values.any { it } ||
            (uiState.otherFamilyMemberChecked && uiState.otherFamilyMemberText.isNotBlank())

    val isSignUpButtonEnabled = uiState.signUpName.isNotBlank() &&
            uiState.signUpBirthDate.length == 8 &&
            uiState.signUpEmail.isNotBlank() &&
            uiState.signUpPassword.isNotBlank() &&
            uiState.signUpConfirmPassword.isNotBlank() &&
            uiState.signUpEmailError == null &&
            uiState.signUpPasswordError == null &&
            uiState.signUpConfirmPasswordError == null &&
            uiState.signUpBirthDateError == null &&
            isFamilyMemberSelected && !uiState.isLoading

    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { activity?.finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreenBackground)
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.profileImageUri != null) {
                        AsyncImage(
                            model = uiState.profileImageUri,
                            contentDescription = "선택된 프로필 이미지",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    SignUpTextField(label = "이름", value = uiState.signUpName, onValueChange = authViewModel::onSignUpNameChange, imeAction = ImeAction.Next, focusManager = focusManager)
                }

                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = "생년월일",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SolarLunarCheckbox(text = "양력", checked = !uiState.signUpIsLunar, onCheckedChange = { if (it) authViewModel.onSignUpIsLunarChange(false) })
                        Spacer(modifier = Modifier.width(16.dp))
                        SolarLunarCheckbox(text = "음력", checked = uiState.signUpIsLunar, onCheckedChange = { if (it) authViewModel.onSignUpIsLunarChange(true) })
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.signUpBirthDate,
                        onValueChange = authViewModel::onSignUpBirthDateChange,
                        placeholder = { Text("ex)20040506", color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if(uiState.signUpBirthDateError != null) ErrorRed else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if(uiState.signUpBirthDateError != null) ErrorRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        isError = uiState.signUpBirthDateError != null,
                        supportingText = {
                            val error = uiState.signUpBirthDateError
                            if (error != null) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    )
                }

                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SignUpTextField(label = "ID(Email)", value = uiState.signUpEmail, onValueChange = authViewModel::onSignUpEmailChange, placeholder = "이메일@도메인.com", keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, focusManager = focusManager, error = uiState.signUpEmailError)
                }

                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SignUpTextField(
                        label = "Password", value = uiState.signUpPassword, onValueChange = authViewModel::onSignUpPasswordChange,
                        placeholder = "영문,숫자,특수기호 포함 8자 이상",
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        imeAction = ImeAction.Next, focusManager = focusManager, error = uiState.signUpPasswordError,
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "비밀번호 보기 토글")
                            }
                        }
                    )
                }

                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    SignUpTextField(
                        label = "비밀번호 확인", value = uiState.signUpConfirmPassword, onValueChange = authViewModel::onSignUpConfirmPasswordChange,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        imeAction = ImeAction.Done, focusManager = focusManager, onDone = { focusManager.clearFocus() },
                        error = uiState.signUpConfirmPasswordError,
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "비밀번호 확인 보기 토글")
                            }
                        }
                    )
                }

                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    FamilyMemberSelection(
                        title = "가족 구성원 중 나는?",
                        members = listOf("할아버지", "할머니", "아버지", "어머니", "아들", "딸"),
                        selections = uiState.familyMemberSelections,
                        onSelectionChange = authViewModel::onFamilyMemberSelectionChange,
                        otherChecked = uiState.otherFamilyMemberChecked,
                        onOtherCheckedChange = authViewModel::onOtherFamilyMemberCheckedChange,
                        otherText = uiState.otherFamilyMemberText,
                        onOtherTextChange = authViewModel::onOtherFamilyMemberTextChange,
                        focusManager = focusManager
                    )
                }

                Button(
                    onClick = authViewModel::signUp,
                    enabled = isSignUpButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                    )
                ) {
                    Text(if (uiState.isLoading) "가입 중..." else "회원가입", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}