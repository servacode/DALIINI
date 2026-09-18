package com.servacode.directory.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.designsystem.generated.DirectoryTokens

private val gutter = DirectoryTokens.SpacingXl.dp
private val gap = DirectoryTokens.SpacingMd.dp

/** "Check this field" under every field the backend named; the backend's text is not shown. */
@Composable
private fun FieldHint(failure: FormFailure?, vararg fields: String) {
    if (failure != null && fields.any { it in failure.fields }) {
        Text("تحقق من هذا الحقل", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun FailureText(failure: FormFailure?) {
    failure?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun PhoneField(value: String, onChange: (String) -> Unit) = OutlinedTextField(
    value = value,
    onValueChange = onChange,
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    label = { Text("رقم الهاتف") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
)

@Composable
private fun SecretField(value: String, label: String, onChange: (String) -> Unit, numeric: Boolean = false) =
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password,
        ),
    )

@Composable
private fun Submit(label: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (busy) CircularProgressIndicator() else Button(onClick = onClick, enabled = enabled) { Text(label) }
}

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onRegister: () -> Unit,
    onRecovery: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    Column(
        Modifier.fillMaxSize().padding(gutter).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Text("تسجيل الدخول", style = MaterialTheme.typography.headlineLarge)
        PhoneField(phone) { phone = it }
        FieldHint(state.failure, "phone")
        SecretField(password, "كلمة المرور", { password = it })
        FieldHint(state.failure, "password")
        FailureText(state.failure)
        Submit("دخول", state.busy, phone.isNotBlank() && password.isNotEmpty()) {
            viewModel.submit(phone, password)
        }
        TextButton(onClick = onRecovery) { Text("نسيت كلمة المرور؟") }
        TextButton(onClick = onRegister) { Text("إنشاء حساب جديد") }
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(state.step) { if (state.step == ChallengeStep.DONE) onRegistered() }

    Column(
        Modifier.fillMaxSize().padding(gutter).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Text("إنشاء حساب", style = MaterialTheme.typography.headlineLarge)
        when (state.step) {
            ChallengeStep.DETAILS -> {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("الاسم") },
                )
                FieldHint(state.failure, "displayName")
                PhoneField(phone) { phone = it }
                FieldHint(state.failure, "phone")
                Text("المحافظة", style = MaterialTheme.typography.titleMedium)
                state.provinces.forEach { province ->
                    TextButton(onClick = { viewModel.chooseProvince(province.id) }) {
                        Text(if (province.id == state.provinceId) "✓ ${province.nameAr}" else province.nameAr)
                    }
                }
                FieldHint(state.failure, "provinceId")
                FailureText(state.failure)
                Submit(
                    "إرسال رمز التحقق",
                    state.busy,
                    name.isNotBlank() && phone.isNotBlank() && state.provinceId != null,
                ) { viewModel.start(name, phone) }
            }
            ChallengeStep.CODE -> {
                Text("أدخل الرمز المرسل إلى هاتفك")
                SecretField(code, "رمز التحقق", { code = it }, numeric = true)
                FieldHint(state.failure, "code", "challengeId")
                FailureText(state.failure)
                Submit("تحقق", state.busy, code.isNotBlank()) { viewModel.verify(code) }
            }
            ChallengeStep.PASSWORD -> {
                SecretField(password, "كلمة المرور", { password = it })
                FieldHint(state.failure, "password")
                FailureText(state.failure)
                Submit("إنشاء الحساب", state.busy, password.isNotEmpty()) { viewModel.complete(password) }
            }
            ChallengeStep.DONE -> CircularProgressIndicator()
        }
    }
}

@Composable
fun RecoveryScreen(
    onDone: () -> Unit,
    viewModel: RecoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(gutter).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Text("استعادة كلمة المرور", style = MaterialTheme.typography.headlineLarge)
        when (state.step) {
            ChallengeStep.DETAILS -> {
                PhoneField(phone) { phone = it }
                FieldHint(state.failure, "phone")
                FailureText(state.failure)
                Submit("إرسال رمز التحقق", state.busy, phone.isNotBlank()) { viewModel.start(phone) }
            }
            ChallengeStep.CODE -> {
                SecretField(code, "رمز التحقق", { code = it }, numeric = true)
                FieldHint(state.failure, "code", "challengeId")
                FailureText(state.failure)
                Submit("تحقق", state.busy, code.isNotBlank()) { viewModel.verify(code) }
            }
            ChallengeStep.PASSWORD -> {
                SecretField(password, "كلمة المرور الجديدة", { password = it })
                FieldHint(state.failure, "password")
                FailureText(state.failure)
                Submit("حفظ", state.busy, password.isNotEmpty()) { viewModel.reset(password) }
            }
            ChallengeStep.DONE -> {
                Text("تم تغيير كلمة المرور. سجّل الدخول بها الآن.")
                Button(onClick = onDone) { Text("تسجيل الدخول") }
            }
        }
    }
}
