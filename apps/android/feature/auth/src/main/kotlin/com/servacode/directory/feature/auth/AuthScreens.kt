package com.servacode.directory.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.BrandSymbol
import com.servacode.directory.core.designsystem.DirectoryBrandHeader
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPasswordField
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.DirectoryBrand

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

    AuthFrame(
        onBack = onBack,
        // Over the form, because a page of two fields could be anything until it is named.
        title = AuthCopy.SIGN_IN,
        bottomBar = bottomBar,
        footer = {
            // The two ways off this page, one under the other and set alike: a link inside the
            // card and a sentence under it read as two different kinds of thing, and they are
            // not — both are "I cannot sign in from here".
            DirectoryTextButton(AuthCopy.FORGOT, onRecovery)
            CreateAccountLine(onRegister = onRegister)
        },
    ) {
        PhoneField(phone, state.failure, filled = true) { phone = it }
        DirectoryPasswordField(
            value = password,
            onValueChange = { password = it },
            label = AuthCopy.PASSWORD,
            error = fieldError(state.failure, "password"),
            filled = true,
        )
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
 * The one frame all three account pages wear.
 *
 * It had a near-black band across the top with nothing in it, two outlined white boxes on a
 * white page, and three full-width buttons of equal weight — so nothing led and the heaviest
 * thing on the screen was a disabled grey slab.
 *
 * What it is now: a soft green field at the top carrying the mark and the app's name, and the
 * form on a white card in the middle of what is left, where the eye lands and the thumb
 * reaches. The green is the brand's own (`action.primary`), which is the green of the mark and
 * of every button in the app; the band that was there used a near-black that appears nowhere
 * else, which is why it never belonged.
 *
 * Creating an account and recovering a password wear it too, and carry the mark and the name
 * for the same reason the first page does: the reader should know whose account they are
 * opening while they are opening it.
 */
@Composable
private fun AuthFrame(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    bottomBar: @Composable () -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
    form: @Composable ColumnScope.() -> Unit,
) {
    DirectoryPage(
        background = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            Welcome(onBack = onBack)
            // The card follows the mark down the page rather than sitting in the middle of
            // whatever is left: centred, a short form left a band of empty green-to-white
            // between the two, and the page read as two unrelated halves. Anything under the
            // card — the links, a keyboard — pushes into the space below it instead.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.base)
                    .padding(top = Space.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                DirectoryCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Space.sm),
                        content = form,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                    content = { footer() },
                )
                Spacer(Modifier.height(Space.lg))
            }
        }
    }
}

/** The soft green field the page opens with: the mark, the app's name, and one line of welcome. */
@Composable
private fun Welcome(onBack: (() -> Unit)?) {
    DirectoryBrandHeader {
        // The row is there whether or not there is a way back, so the mark sits at the same
        // height on all three pages. Signing in had no arrow and its logo rode that much higher
        // than the one on the page beside it.
        Box(modifier = Modifier.fillMaxWidth().height(Sizes.touchTarget)) {
            if (onBack != null) {
                DirectoryIconButton(
                    icon = DirectoryIcons.back,
                    label = AuthCopy.BACK,
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = Space.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            BrandSymbol(size = AUTH_MARK)
            Text(
                text = AuthCopy.APP_NAME,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
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

/** Big enough to be the brand rather than a decoration, small enough to leave the form the page. */
private val AUTH_MARK = 104.dp

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
    onSignIn: () -> Unit = onBack,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    LaunchedEffect(state.step) { if (state.step == ChallengeStep.DONE) onRegistered() }

    AuthFrame(onBack = onBack, title = AuthCopy.CREATE_ACCOUNT) {
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
                // The one refusal here that is not about what they typed: the number is theirs
                // and already has an account. Saying so without a way to it is a dead end.
                if (state.failure?.code == TAKEN) {
                    DirectorySecondaryButton(
                        text = AuthCopy.SIGN_IN,
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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

    AuthFrame(onBack = onBack, title = AuthCopy.RECOVERY) {
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
    // Not a password: it arrives in a message the reader is looking at, it is typed once, and
    // it is worthless a few minutes later. Hiding it only makes it harder to copy correctly.
    DirectoryTextField(
        value = code,
        onValueChange = onCode,
        label = AuthCopy.CODE,
        leadingIcon = DirectoryIcons.verified,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        error = fieldError(failure, "code", "challengeId"),
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

/** The backend's code for a number that already has an account (`accounts/services.py`). */
private const val TAKEN = "PHONE_ALREADY_REGISTERED"

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
