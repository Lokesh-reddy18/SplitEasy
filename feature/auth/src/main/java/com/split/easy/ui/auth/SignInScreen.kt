package com.split.easy.ui.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.core.designsystem.R
import com.split.easy.ui.theme.SplitEasyTheme
import kotlinx.coroutines.launch

private const val LEGAL_DOCS = "https://github.com/Lokesh-reddy18/SplitEasy/blob/main/docs/legal"
private const val TERMS_URL = "$LEGAL_DOCS/TERMS.md"
private const val PRIVACY_URL = "$LEGAL_DOCS/PRIVACY.md"

@Composable
fun SignInRoute(
    /** The "Web client" OAuth ID from google-services.json, supplied by the app. */
    webClientId: String,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    SignInScreen(
        state = state,
        onContinueWithGoogle = {
            viewModel.onSignInStarted()
            scope.launch {
                when (val outcome = requestGoogleIdToken(context, webClientId)) {
                    is GoogleSignInOutcome.Token -> viewModel.onGoogleIdToken(outcome.idToken)
                    GoogleSignInOutcome.Cancelled -> viewModel.onSignInCancelled()
                    is GoogleSignInOutcome.Error -> viewModel.onSignInError(outcome.error)
                }
            }
        },
    )
}

/** Sign in (01): what SplitEasy does, and "Continue with Google". */
@Composable
fun SignInScreen(state: SignInUiState, onContinueWithGoogle: () -> Unit) {
    val colors = SplitEasyTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.size(40.dp))
            Box(
                Modifier
                    .size(56.dp)
                    .background(colors.primary, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                stringResource(R.string.sign_in_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                ),
                color = colors.ink,
                modifier = Modifier
                    .padding(top = 32.dp)
                    .semantics { heading() },
            )
            Text(
                stringResource(R.string.sign_in_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
                color = colors.muted,
                modifier = Modifier.padding(top = 14.dp),
            )
            Column(
                Modifier.padding(top = 36.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Feature(
                    R.drawable.ic_offline,
                    R.string.sign_in_offline_title,
                    R.string.sign_in_offline_body,
                )
                Feature(
                    R.drawable.ic_swap,
                    R.string.sign_in_fewest_title,
                    R.string.sign_in_fewest_body,
                )
                Feature(
                    R.drawable.ic_upi_card,
                    R.string.sign_in_upi_title,
                    R.string.sign_in_upi_body,
                )
            }
        }

        state.error?.let { error ->
            Text(
                stringResource(error.message()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.owe,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
        }
        Button(
            onClick = onContinueWithGoogle,
            enabled = !state.isSigningIn,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.ink,
                contentColor = colors.surface,
                disabledContainerColor = colors.ink.copy(alpha = 0.6f),
                disabledContentColor = colors.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            if (state.isSigningIn) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = colors.surface,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_nav_profile),
                    null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                stringResource(R.string.sign_in_continue),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        LegalText(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun Feature(@DrawableRes icon: Int, title: Int, body: Int) {
    val colors = SplitEasyTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(40.dp)
                .background(colors.primaryTint, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                null,
                tint = colors.onPrimaryTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Column {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
            )
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
    }
}

/** "By continuing you agree to the Terms and Privacy Policy", with both as links. */
@Composable
private fun LegalText(modifier: Modifier) {
    val colors = SplitEasyTheme.colors
    val terms = stringResource(R.string.sign_in_terms)
    val privacy = stringResource(R.string.sign_in_privacy)
    val full = stringResource(R.string.sign_in_legal, terms, privacy)
    val linkStyle =
        TextLinkStyles(
            SpanStyle(color = colors.primaryText, textDecoration = TextDecoration.Underline),
        )
    val text = buildAnnotatedString {
        val termsAt = full.indexOf(terms)
        val privacyAt = full.indexOf(privacy)
        append(full.substring(0, termsAt))
        withLink(LinkAnnotation.Url(TERMS_URL, linkStyle)) { append(terms) }
        append(full.substring(termsAt + terms.length, privacyAt))
        withLink(LinkAnnotation.Url(PRIVACY_URL, linkStyle)) { append(privacy) }
        append(full.substring(privacyAt + privacy.length))
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.muted,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

private fun SignInError.message() = when (this) {
    SignInError.NETWORK -> R.string.sign_in_error_network
    SignInError.NO_ACCOUNT -> R.string.sign_in_error_no_account
    SignInError.FAILED -> R.string.sign_in_error_failed
}
