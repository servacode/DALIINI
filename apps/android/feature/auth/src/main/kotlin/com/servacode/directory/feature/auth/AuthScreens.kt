package com.servacode.directory.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.servacode.directory.core.designsystem.BrandColors
import com.servacode.directory.core.designsystem.BrandMark
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.Sizes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryChipRow
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPasswordField
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.Space

/**
 * Screens 12 to 14: signing in, creating an account, and getting back into one.
 *
 * The account in this app is a phone number and a password, verified by a code sent to that
 * number — that is what the API offers, and these screens ask for exactly that and nothing more.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onRegister: () -> Unit,
    onRecovery: () -> Unit,
    /**
     * Null where signing in is the tab itself.
     *
     * Opened from a screen, this page has somewhere to go back to. Standing in for the account
     * tab it has not: back from the root of a tab is a way out of the app, and an arrow that
     * promises a previous screen there is a lie.
     */
    onBack: (() -> Unit)? = null,
    /** The app's own bar, where this page is a tab rather than a screen on top of one. */
    bottomBar: @Composable () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    AuthPage(title = AuthCopy.SIGN_IN, onBack = onBack, bottomBar = bottomBar, mark = true) {
        PhoneField(phone, state.failure) { phone = it }
        DirectoryPasswordField(
            value = password,
            onValueChange = { password = it },
            label = AuthCopy.PASSWORD,
            error = fieldError(state.failure, "password"),
        )
        FailureText(state.failure)
        DirectoryPrimaryButton(
            text = AuthCopy.SIGN_IN,
            onClick = { viewModel.submit(phone, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = phone.isNotBlank() && password.isNotEmpty(),
            loading = state.busy,
        )
        DirectoryTextButton(AuthCopy.FORGOT, onRecovery, Modifier.fillMaxWidth())
        DirectoryTextButton(AuthCopy.CREATE, onRegister, Modifier.fillMaxWidth())
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(state.step) { if (state.step == ChallengeStep.DONE) onRegistered() }

    AuthPage(title = AuthCopy.CREATE_ACCOUNT, onBack = onBack) {
        when (state.step) {
            ChallengeStep.DETAILS -> {
                DirectoryTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = AuthCopy.NAME,
                    leadingIcon = DirectoryIcons.person,
                    error = fieldError(state.failure, "displayName"),
                )
                PhoneField(phone, state.failure) { phone = it }
                Text(
                    text = AuthCopy.PROVINCE,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                DirectoryChipRow {
                    state.provinces.forEach { province ->
                        DirectoryFilterChip(
                            text = province.nameAr,
                            selected = province.id == state.provinceId,
                            onClick = { viewModel.chooseProvince(province.id) },
                        )
                    }
                }
                fieldError(state.failure, "provinceId")?.let { ErrorText(it) }
                FailureText(state.failure)
                DirectoryPrimaryButton(
                    text = AuthCopy.SEND_CODE,
                    onClick = { viewModel.start(name, phone) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && phone.isNotBlank() && state.provinceId != null,
                    loading = state.busy,
                )
            }
            ChallengeStep.CODE -> CodeStep(
                code = code,
                onCode = { code = it },
                failure = state.failure,
                busy = state.busy,
                onVerify = { viewModel.verify(code) },
            )
            ChallengeStep.PASSWORD -> {
                DirectoryPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = AuthCopy.PASSWORD,
                    error = fieldError(state.failure, "password"),
                )
                FailureText(state.failure)
                DirectoryPrimaryButton(
                    text = AuthCopy.CREATE_ACCOUNT,
                    onClick = { viewModel.complete(password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotEmpty(),
                    loading = state.busy,
                )
            }
            ChallengeStep.DONE -> DirectoryPrimaryButton(
                text = AuthCopy.CREATE_ACCOUNT,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                loading = true,
            )
        }
    }
}

@Composable
fun RecoveryScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthPage(title = AuthCopy.RECOVERY, onBack = onBack) {
        when (state.step) {
            ChallengeStep.DETAILS -> {
                Text(
                    text = AuthCopy.RECOVERY_NOTE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                PhoneField(phone, state.failure) { phone = it }
                FailureText(state.failure)
                DirectoryPrimaryButton(
                    text = AuthCopy.SEND_CODE,
                    onClick = { viewModel.start(phone) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = phone.isNotBlank(),
                    loading = state.busy,
                )
            }
            ChallengeStep.CODE -> CodeStep(
                code = code,
                onCode = { code = it },
                failure = state.failure,
                busy = state.busy,
                onVerify = { viewModel.verify(code) },
            )
            ChallengeStep.PASSWORD -> {
                DirectoryPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = AuthCopy.NEW_PASSWORD,
                    error = fieldError(state.failure, "password"),
                )
                FailureText(state.failure)
                DirectoryPrimaryButton(
                    text = AuthCopy.SAVE,
                    onClick = { viewModel.reset(password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotEmpty(),
                    loading = state.busy,
                )
            }
            ChallengeStep.DONE -> {
                Text(
                    text = AuthCopy.RECOVERED,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                DirectoryPrimaryButton(
                    text = AuthCopy.SIGN_IN,
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** The one frame all three wear: a way back, the name of what is being done, then the form. */
@Composable
private fun AuthPage(
    title: String,
    onBack: (() -> Unit)?,
    bottomBar: @Composable () -> Unit = {},
    /** Whether the app's mark opens the page, as it does where signing in is the whole screen. */
    mark: Boolean = false,
    content: @Composable () -> Unit,
) {
    DirectoryPage(
        // The same deep green the app wears elsewhere, so the top of this page belongs to the
        // app rather than to the form.
        topBar = { AuthTopBar(onBack = onBack) },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.xl)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            if (mark) {
                Spacer(Modifier.height(Space.xl))
                BrandMark(size = Sizes.hero / 3)
            } else {
                Spacer(Modifier.height(Space.lg))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Space.sm))
            content()
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

/** A band of the app's own green, with a way back only where there is one. */
@Composable
private fun AuthTopBar(onBack: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandColors.bar)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(Sizes.touchTarget),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (onBack != null) {
            DirectoryIconButton(
                icon = DirectoryIcons.back,
                label = AuthCopy.BACK,
                onClick = onBack,
                tint = BrandColors.onBar,
            )
        }
    }
}

/** The code sent to the phone, in both the account and the recovery flows. */
@Composable
private fun CodeStep(
    code: String,
    onCode: (String) -> Unit,
    failure: FormFailure?,
    busy: Boolean,
    onVerify: () -> Unit,
) {
    Text(
        text = AuthCopy.CODE_SENT,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    DirectoryPasswordField(
        value = code,
        onValueChange = onCode,
        label = AuthCopy.CODE,
        error = fieldError(failure, "code", "challengeId"),
        numeric = true,
    )
    FailureText(failure)
    DirectoryPrimaryButton(
        text = AuthCopy.VERIFY,
        onClick = onVerify,
        modifier = Modifier.fillMaxWidth(),
        enabled = code.isNotBlank(),
        loading = busy,
    )
}

@Composable
private fun PhoneField(value: String, failure: FormFailure?, onChange: (String) -> Unit) {
    DirectoryTextField(
        value = value,
        onValueChange = onChange,
        label = AuthCopy.PHONE,
        leadingIcon = DirectoryIcons.phone,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        error = fieldError(failure, "phone"),
    )
}

/** "Check this field" under every field the backend named; the backend's text is not shown. */
private fun fieldError(failure: FormFailure?, vararg fields: String): String? =
    if (failure != null && fields.any { it in failure.fields }) AuthCopy.FIELD_ERROR else null

@Composable
private fun FailureText(failure: FormFailure?) {
    failure?.let { ErrorText(it.message) }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The words of the account screens, provisional until product copy is approved. */
object AuthCopy {
    const val SIGN_IN = "تسجيل الدخول"
    const val BACK = "رجوع"
    const val CREATE = "إنشاء حساب جديد"
    const val CREATE_ACCOUNT = "إنشاء حساب"
    const val RECOVERY = "استعادة كلمة المرور"
    const val RECOVERY_NOTE = "أدخل رقم هاتفك وسنرسل إليك رمز تحقق."
    const val PHONE = "رقم الهاتف"
    const val NAME = "الاسم"
    const val PROVINCE = "المحافظة"
    const val PASSWORD = "كلمة المرور"
    const val NEW_PASSWORD = "كلمة المرور الجديدة"
    const val CODE = "رمز التحقق"
    const val CODE_SENT = "أدخل الرمز المرسل إلى هاتفك"
    const val SEND_CODE = "إرسال رمز التحقق"
    const val VERIFY = "تحقق"
    const val SAVE = "حفظ"
    const val FORGOT = "نسيت كلمة المرور؟"
    const val RECOVERED = "تم تغيير كلمة المرور. سجّل الدخول بها الآن."
    const val FIELD_ERROR = "تحقق من هذا الحقل"
}
