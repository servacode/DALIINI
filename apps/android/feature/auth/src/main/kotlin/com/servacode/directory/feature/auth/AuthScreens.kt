package com.servacode.directory.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.servacode.directory.core.designsystem.BrandColors
import com.servacode.directory.core.model.DirectoryBrand
import com.servacode.directory.core.designsystem.BrandSymbol
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.Sizes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.Radius
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
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

    SignInPage(
        onBack = onBack,
        onRegister = onRegister,
        bottomBar = bottomBar,
    ) {
        PhoneField(phone, state.failure, filled = true) { phone = it }
        DirectoryPasswordField(
            value = password,
            onValueChange = { password = it },
            label = AuthCopy.PASSWORD,
            error = fieldError(state.failure, "password"),
            filled = true,
        )
        // Where it belongs: under the field it is about, not as a full-width button competing
        // with the one thing this page is for.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            DirectoryTextButton(AuthCopy.FORGOT, onRecovery)
        }
        FailureText(state.failure)
        DirectoryPrimaryButton(
            text = AuthCopy.SIGN_IN,
            onClick = { viewModel.submit(phone, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = phone.isNotBlank() && password.isNotEmpty(),
            loading = state.busy,
        )
    }
}

/**
 * The page a person meets before they have an account here.
 *
 * It had a near-black band across the top with nothing in it, two outlined white boxes on a
 * white page, and three full-width buttons of equal weight — so nothing led and the heaviest
 * thing on the screen was a disabled grey slab.
 *
 * What it is now: a soft green field at the top carrying the mark and the app's name, the form
 * on a white card that overlaps it, and one green button. The green is the brand's own
 * (`action.primary`), which is the green of the mark and of every button in the app; the band
 * that was there used a near-black that appears nowhere else, which is why it never belonged.
 */
@Composable
private fun SignInPage(
    onBack: (() -> Unit)?,
    onRegister: () -> Unit,
    bottomBar: @Composable () -> Unit,
    form: @Composable ColumnScope.() -> Unit,
) {
    DirectoryPage(
        background = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            Welcome(onBack = onBack)
            DirectoryCard(
                modifier = Modifier
                    .padding(horizontal = Space.base)
                    // Lifted into the panel above it, so the two read as one shape rather than
                    // as a coloured band with a page under it.
                    .offset(y = -Space.xl),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                    content = form,
                )
            }
            CreateAccountLine(onRegister = onRegister)
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

/** The soft green field the page opens with: the mark, the app's name, and one line of welcome. */
@Composable
private fun Welcome(onBack: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = Radius.xl, bottomEnd = Radius.xl))
            .background(BrandColors.softer)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        if (onBack != null) {
            DirectoryIconButton(
                icon = DirectoryIcons.back,
                label = AuthCopy.BACK,
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(Space.sm),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl)
                .padding(top = Space.xxl, bottom = Space.huge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            BrandSymbol(size = Sizes.actionCircle + Space.lg)
            Text(
                text = AuthCopy.APP_NAME,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = AuthCopy.WELCOME,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One sentence, with the action in it, instead of a third full-width button. */
@Composable
private fun CreateAccountLine(onRegister: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = AuthCopy.NO_ACCOUNT,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DirectoryTextButton(AuthCopy.CREATE, onRegister)
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    LaunchedEffect(state.step) { if (state.step == ChallengeStep.DONE) onRegistered() }

    AuthPage(title = AuthCopy.CREATE_ACCOUNT, onBack = onBack) {
        when (state.step) {
            // The number, and nothing else. Nothing is asked about the person before they have
            // shown they can receive on it, and the province is taken from where they are
            // standing — one less question for an answer the app already has.
            ChallengeStep.DETAILS -> {
                Text(
                    text = AuthCopy.PHONE_FIRST,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                PhoneField(phone, state.failure) { phone = it }
                fieldError(state.failure, "provinceId")?.let { ErrorText(it) }
                FailureText(state.failure)
                DirectoryPrimaryButton(
                    text = AuthCopy.SEND_CODE,
                    onClick = { viewModel.start(phone) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = phone.isNotBlank() && state.provinceId != null,
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
            // The number is proved; now the person.
            ChallengeStep.PASSWORD -> {
                DirectoryTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = AuthCopy.FULL_NAME,
                    leadingIcon = DirectoryIcons.person,
                    error = fieldError(state.failure, "displayName"),
                )
                DirectoryPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = AuthCopy.PASSWORD,
                    error = fieldError(state.failure, "password"),
                )
                ConfirmationField(
                    value = confirmation,
                    password = password,
                    onValueChange = { confirmation = it },
                )
                FailureText(state.failure)
                DirectoryPrimaryButton(
                    text = AuthCopy.CREATE_ACCOUNT,
                    onClick = { viewModel.complete(name, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && password.isNotEmpty() && confirmation == password,
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

/**
 * The second time, to catch a typo before it locks someone out.
 *
 * It says so while they are typing rather than when they press the button: a mismatch found on
 * submit means retyping both, and the field that is wrong is the one that should say so.
 */
@Composable
private fun ConfirmationField(
    value: String,
    password: String,
    onValueChange: (String) -> Unit,
) {
    DirectoryPasswordField(
        value = value,
        onValueChange = onValueChange,
        label = AuthCopy.CONFIRM_PASSWORD,
        error = if (value.isNotEmpty() && value != password) AuthCopy.MISMATCH else null,
    )
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
    var confirmation by remember { mutableStateOf("") }

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
                ConfirmationField(
                    value = confirmation,
                    password = password,
                    onValueChange = { confirmation = it },
                )
                FailureText(state.failure)
                DirectoryPrimaryButton(
                    text = AuthCopy.RESET_PASSWORD,
                    onClick = { viewModel.reset(password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotEmpty() && confirmation == password,
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
                BrandSymbol(size = Sizes.hero / 3)
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

/** A quiet band in the brand's soft tint, with a way back only where there is one. */
@Composable
private fun AuthTopBar(onBack: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandColors.softer)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(Sizes.touchTarget),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (onBack != null) {
            DirectoryIconButton(
                icon = DirectoryIcons.back,
                label = AuthCopy.BACK,
                onClick = onBack,
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
private fun PhoneField(
    value: String,
    failure: FormFailure?,
    filled: Boolean = false,
    onChange: (String) -> Unit,
) {
    DirectoryTextField(
        value = value,
        onValueChange = onChange,
        label = AuthCopy.PHONE,
        leadingIcon = DirectoryIcons.phone,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        error = fieldError(failure, "phone"),
        filled = filled,
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
    const val APP_NAME = DirectoryBrand.NAME
    const val WELCOME = DirectoryBrand.TAGLINE
    const val NO_ACCOUNT = "ليس لديك حساب؟"
    const val CREATE = "إنشاء حساب جديد"
    const val CREATE_ACCOUNT = "إنشاء حساب"
    const val RECOVERY = "استعادة كلمة المرور"
    const val RECOVERY_NOTE = "أدخل رقم هاتفك وسنرسل إليك رمز تحقق."
    const val PHONE = "رقم الهاتف"
    const val FULL_NAME = "الاسم الكامل"
    const val CONFIRM_PASSWORD = "تأكيد كلمة المرور"
    const val MISMATCH = "الكلمتان غير متطابقتين"
    const val RESET_PASSWORD = "إعادة تعيين كلمة المرور"
    const val PHONE_FIRST = "أدخل رقم هاتفك وسنرسل إليك رمز تحقق."
    const val PASSWORD = "كلمة المرور"
    const val NEW_PASSWORD = "كلمة المرور الجديدة"
    const val CODE = "رمز التحقق"
    const val CODE_SENT = "أدخل الرمز المرسل إلى هاتفك"
    const val SEND_CODE = "إرسال رمز التحقق"
    const val VERIFY = "تحقق"
    const val FORGOT = "نسيت كلمة المرور؟"
    const val RECOVERED = "تم تغيير كلمة المرور. سجّل الدخول بها الآن."
    const val FIELD_ERROR = "تحقق من هذا الحقل"
}
