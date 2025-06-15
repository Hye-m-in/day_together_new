package com.example.day_together.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.day_together.R
import com.example.day_together.ui.theme.*
import com.example.day_together.ui.auth.FamilyMemberSelection

data class EditProfileScreenState(
    val currentName: String = "홍길동",
    val currentBirthDate: String = "20000101",
    val currentIsLunar: Boolean = false,
    val currentEmail: String = "user@example.com",
    val currentFamilySelections: Map<String, Boolean> = mapOf("아버지" to true),
    val currentOtherFamilyText: String = "",
    var newName: String = currentName,
    var newBirthDate: String = currentBirthDate,
    var newIsLunar: Boolean = currentIsLunar,
    var oldPassword: String = "",
    var newPassword: String = "",
    var confirmNewPassword: String = "",
    var newFamilySelections: Map<String, Boolean> = currentFamilySelections,
    var newOtherFamilyText: String = currentOtherFamilyText,
    val simulateChangesMade: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    initialState: EditProfileScreenState = EditProfileScreenState()
) {
    var name by remember { mutableStateOf(initialState.newName) }
    var birthDate by remember { mutableStateOf(initialState.newBirthDate) }
    var isLunarCalendar by remember { mutableStateOf(initialState.newIsLunar) }
    val email = initialState.currentEmail
    var oldPassword by remember { mutableStateOf(initialState.oldPassword) }
    var newPassword by remember { mutableStateOf(initialState.newPassword) }
    var confirmNewPassword by remember { mutableStateOf(initialState.confirmNewPassword) }

    val familyMembers = listOf("할아버지", "할머니", "아버지", "어머니", "아들", "딸")
    var familyMemberSelections by remember { mutableStateOf(initialState.newFamilySelections.ifEmpty { familyMembers.associateWith { false } }) }
    var otherFamilyMemberChecked by remember { mutableStateOf(initialState.newOtherFamilyText.isNotBlank()) }
    var otherFamilyMemberText by remember { mutableStateOf(initialState.newOtherFamilyText) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(otherFamilyMemberChecked) {
        if (!otherFamilyMemberChecked) {
            otherFamilyMemberText = ""
        }
    }

    val isFamilyMemberSelected = familyMemberSelections.values.any { it } || (otherFamilyMemberChecked && otherFamilyMemberText.isNotBlank())

    val hasInfoChanged = name != initialState.currentName ||
            birthDate != initialState.currentBirthDate ||
            isLunarCalendar != initialState.currentIsLunar ||
            familyMemberSelections != initialState.currentFamilySelections ||
            (otherFamilyMemberChecked && otherFamilyMemberText != initialState.currentOtherFamilyText) ||
            (!otherFamilyMemberChecked && initialState.currentOtherFamilyText.isNotBlank())

    val isPasswordChangeAttemptValid = oldPassword.isNotBlank() &&
            newPassword.length >= 8 &&
            confirmNewPassword == newPassword

    val isPasswordChangeAttemptInvalid = (oldPassword.isNotBlank() || newPassword.isNotBlank() || confirmNewPassword.isNotBlank()) && !isPasswordChangeAttemptValid

    val isCompleteButtonEnabled = (hasInfoChanged || (oldPassword.isNotBlank() && isPasswordChangeAttemptValid)) &&
            name.isNotBlank() && birthDate.length == 8 && isFamilyMemberSelected &&
            !isPasswordChangeAttemptInvalid

    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
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
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { }
                )
                Spacer(modifier = Modifier.height(32.dp))

                EditProfileTextField(label = "이름", value = name, onValueChange = { name = it }, imeAction = ImeAction.Next, focusManager = focusManager)

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("생년월일", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp), color = TextPrimary, modifier = Modifier.padding(end = 12.dp))
                        SolarLunarCheckbox(text = "양력", checked = !isLunarCalendar, onCheckedChange = { if (it) isLunarCalendar = false })
                        Spacer(modifier = Modifier.width(16.dp))
                        SolarLunarCheckbox(text = "음력", checked = isLunarCalendar, onCheckedChange = { if (it) isLunarCalendar = true })
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = birthDate, onValueChange = { if (it.length <= 8) birthDate = it },
                        placeholder = { Text("ex)20040506", color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp), singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors( )
                    )
                }

                EditProfileTextField(label = "ID(Email)", value = email, onValueChange = { }, imeAction = ImeAction.Next, focusManager = focusManager, readOnly = true)
                EditProfileTextField(label = "기존 Password", value = oldPassword, onValueChange = { oldPassword = it }, keyboardType = KeyboardType.Password, isPassword = true, imeAction = ImeAction.Next, focusManager = focusManager, placeholder = "변경 시에만 입력")
                EditProfileTextField(label = "변경할 Password", value = newPassword, onValueChange = { newPassword = it }, keyboardType = KeyboardType.Password, isPassword = true, imeAction = ImeAction.Next, focusManager = focusManager, placeholder = "영문,숫자,특수기호 포함 8자 이상")
                EditProfileTextField(label = "변경할 Password 확인", value = confirmNewPassword, onValueChange = { confirmNewPassword = it }, keyboardType = KeyboardType.Password, isPassword = true, imeAction = ImeAction.Done, focusManager = focusManager, onDone = { focusManager.clearFocus() })

                Spacer(modifier = Modifier.height(24.dp))

                FamilyMemberSelection(
                    title = "가족 구성원 중 나는?",
                    members = familyMembers,
                    selections = familyMemberSelections,
                    onSelectionChange = { member, isSelected ->
                        familyMemberSelections = familyMemberSelections.toMutableMap().apply { this[member] = isSelected }
                    },
                    otherChecked = otherFamilyMemberChecked,
                    onOtherCheckedChange = { otherFamilyMemberChecked = it },
                    otherText = otherFamilyMemberText,
                    onOtherTextChange = { otherFamilyMemberText = it },
                    focusManager = focusManager
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { },
                    enabled = isCompleteButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                    )
                ) {
                    Text("완료", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { }) {
                    Text(
                        "회원탈퇴",
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline, color = TextPrimary.copy(alpha = 0.7f))
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EditProfileTextField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String? = null, keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false, imeAction: ImeAction,
    focusManager: FocusManager,
    onDone: (() -> Unit)? = null,
    readOnly: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            placeholder = { if (placeholder != null) Text(placeholder, color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (readOnly) TextPrimary.copy(alpha = 0.7f) else TextPrimary, fontSize = 15.sp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { onDone?.invoke() ?: focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                cursorColor = MaterialTheme.colorScheme.primary,
                disabledTextColor = TextPrimary.copy(alpha = 0.7f),
                disabledLabelColor = TextPrimary.copy(alpha = 0.5f),
                disabledPlaceholderColor = TextPrimary.copy(alpha = 0.4f)
            ),
            enabled = !readOnly
        )
    }
}

@Composable
private fun SolarLunarCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors( )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = TextPrimary)
    }
}

class EditProfileScreenStateProvider : PreviewParameterProvider<EditProfileScreenState> {
    override val values = sequenceOf(
        EditProfileScreenState(),
        EditProfileScreenState(
            newName = "김길동",
            oldPassword = "oldPassword123",
            newPassword = "newPassword456!",
            confirmNewPassword = "newPassword456!",
            simulateChangesMade = true
        ),
        EditProfileScreenState(
            oldPassword = "oldPassword123",
            newPassword = "newPassword456!",
            confirmNewPassword = "wrongConfirm"
        )
    )
}

@Preview(showBackground = true, name = "Edit Profile Screen States", widthDp = 390, heightDp = 844)
@Composable
fun EditProfileScreenAllStatesPreview(
    @PreviewParameter(EditProfileScreenStateProvider::class) state: EditProfileScreenState
) {
    Day_togetherTheme {
        EditProfileScreen(
            navController = rememberNavController(),
            initialState = state
        )
    }
}