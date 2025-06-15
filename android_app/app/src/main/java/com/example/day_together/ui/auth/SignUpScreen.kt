package com.example.day_together.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.day_together.R
import com.example.day_together.ui.theme.*


data class SignUpScreenState(
    val name: String = "",
    val birthDate: String = "",
    val isLunar: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val familyMemberSelections: Map<String, Boolean> = emptyMap(),
    val otherFamilyMemberText: String = "",
    val simulateAllFilled: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    initialState: SignUpScreenState = SignUpScreenState()
) {
    var name by remember { mutableStateOf(initialState.name) }
    var birthDate by remember { mutableStateOf(initialState.birthDate) }
    var isLunarCalendar by remember { mutableStateOf(initialState.isLunar) }
    var email by remember { mutableStateOf(initialState.email) }
    var password by remember { mutableStateOf(initialState.password) }
    var confirmPassword by remember { mutableStateOf(initialState.confirmPassword) }

    val familyMembers = listOf("할아버지", "할머니", "아버지", "어머니", "아들", "딸")
    var familyMemberSelections by remember { mutableStateOf(initialState.familyMemberSelections.ifEmpty { familyMembers.associateWith { false } }) }
    var otherFamilyMemberChecked by remember { mutableStateOf(initialState.otherFamilyMemberText.isNotBlank()) }
    var otherFamilyMemberText by remember { mutableStateOf(initialState.otherFamilyMemberText) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(otherFamilyMemberChecked) {
        if (!otherFamilyMemberChecked) {
            otherFamilyMemberText = ""
        }
    }

    val isFamilyMemberSelected = familyMemberSelections.values.any { it } || (otherFamilyMemberChecked && otherFamilyMemberText.isNotBlank())

    val isSignUpButtonEnabled = name.isNotBlank() &&
            birthDate.length == 8 &&
            email.isNotBlank() && email.contains("@") &&
            password.length >= 8 &&
            confirmPassword == password &&
            isFamilyMemberSelected

    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { /* 제목 없음 */ },
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
                        .clickable { /* TODO: 프로필 이미지 선택 로직 */ }
                )
                Spacer(modifier = Modifier.height(32.dp))

                SignUpTextField(label = "이름", value = name, onValueChange = { name = it }, imeAction = ImeAction.Next, focusManager = focusManager)


                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "생년월일",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                            color = TextPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        SolarLunarCheckbox(text = "양력", checked = !isLunarCalendar, onCheckedChange = { if (it) isLunarCalendar = false })
                        Spacer(modifier = Modifier.width(16.dp))
                        SolarLunarCheckbox(text = "음력", checked = isLunarCalendar, onCheckedChange = { if (it) isLunarCalendar = true })
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { if (it.length <= 8) birthDate = it },
                        placeholder = { Text("ex)20040506", color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            cursorColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }

                SignUpTextField(label = "ID(Email)", value = email, onValueChange = { email = it }, placeholder = "이메일@도메인.com", keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, focusManager = focusManager)
                SignUpTextField(label = "Password", value = password, onValueChange = { password = it }, placeholder = "영문,숫자,특수기호 포함 8자 이상", keyboardType = KeyboardType.Password, isPassword = true, imeAction = ImeAction.Next, focusManager = focusManager)
                SignUpTextField(label = "비밀번호 확인", value = confirmPassword, onValueChange = { confirmPassword = it }, keyboardType = KeyboardType.Password, isPassword = true, imeAction = ImeAction.Done, focusManager = focusManager, onDone = { focusManager.clearFocus() })

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
                    onClick = { /* TODO: 회원가입 로직 */ },
                    enabled = isSignUpButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                    )
                ) {
                    Text("회원가입", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SignUpTextField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String? = null, keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false, imeAction: ImeAction,
    focusManager: FocusManager,
    onDone: (() -> Unit)? = null
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
            placeholder = { if (placeholder != null) Text(placeholder, color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 15.sp),
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
                cursorColor = MaterialTheme.colorScheme.primary,
            )
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
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = TextPrimary.copy(alpha = 0.7f),
                checkmarkColor = ScreenBackground
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = TextPrimary
        )
    }
}

const val MAX_FAMILY_MEMBER_OTHER_LENGTH = 10

@Composable
fun FamilyMemberSelection(
    title: String,
    members: List<String>,
    selections: Map<String, Boolean>,
    onSelectionChange: (String, Boolean) -> Unit,
    otherChecked: Boolean,
    onOtherCheckedChange: (Boolean) -> Unit,
    otherText: String,
    onOtherTextChange: (String) -> Unit,
    focusManager: FocusManager
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column {
                val firstRowMembers = members.take(4)
                val secondRowMembers = members.drop(4).take(2)

                Row(modifier = Modifier.fillMaxWidth()) {
                    firstRowMembers.forEach { member ->
                        FamilyMemberItem(
                            text = member,
                            selected = selections[member] ?: false,
                            onSelectedChange = { onSelectionChange(member, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    secondRowMembers.forEach { member ->
                        FamilyMemberItem(
                            text = member,
                            selected = selections[member] ?: false,
                            onSelectedChange = { onSelectionChange(member, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val otherItemWeight = (4 - secondRowMembers.size).toFloat().coerceAtLeast(1f)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(otherItemWeight) // 가중치 적용
                            .clickable { onOtherCheckedChange(!otherChecked) }
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        Checkbox(
                            checked = otherChecked,
                            onCheckedChange = onOtherCheckedChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = TextPrimary.copy(alpha = 0.6f),
                                checkmarkColor = ScreenBackground
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("기타", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = TextPrimary)
                        Spacer(Modifier.width(4.dp))
                        BasicTextField(
                            value = otherText,
                            onValueChange = {
                                if (it.length <= MAX_FAMILY_MEMBER_OTHER_LENGTH) {
                                    onOtherTextChange(it)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .background(
                                    color = if (otherChecked) ScreenBackground.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    BorderStroke(0.5.dp, if (otherChecked) TextPrimary.copy(alpha = 0.4f) else Color.Transparent),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp), // 내부 패딩
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 13.sp),
                            enabled = otherChecked,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberItem(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onSelectedChange(!selected) }
            .padding(vertical = 2.dp, horizontal = 2.dp)
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = TextPrimary.copy(alpha = 0.6f),
                checkmarkColor = ScreenBackground
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = TextPrimary)
    }
}

// --- Preview 함수들 ---
class SignUpScreenStateProvider : PreviewParameterProvider<SignUpScreenState> {
    override val values = sequenceOf(
        SignUpScreenState(),
        SignUpScreenState(
            name = "홍길동", birthDate = "20000101", isLunar = false, email = "test@example.com",
            password = "password123!", confirmPassword = "password123!",
            familyMemberSelections = mapOf("아버지" to true),
            simulateAllFilled = true
        )
    )
}

@Preview(showBackground = true, name = "SignUp Screen States", widthDp = 390, heightDp = 844)
@Composable
fun SignUpScreenAllStatesPreview(
    @PreviewParameter(SignUpScreenStateProvider::class) state: SignUpScreenState
) {
    Day_togetherTheme {
        SignUpScreen(
            navController = rememberNavController(),
            initialState = state
        )
    }
}