package com.split.easy

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.split.easy.core.designsystem.R as DesignR
import com.split.easy.domain.model.Session
import com.split.easy.ui.auth.SignInRoute
import com.split.easy.ui.navigation.SplitEasyApp
import com.split.easy.ui.theme.SplitEasyTheme
import com.split.easy.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the system splash screen until we know whether someone is signed in, so a signed-in
        // user never sees the sign-in screen flash.
        installSplashScreen().setKeepOnScreenCondition { viewModel.session.value == null }
        super.onCreate(savedInstanceState)
        // Live sync only while the app is on screen; it stops in the background (M4-Q8).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.syncWhileVisible() }
        }
        enableEdgeToEdge()
        setContent {
            val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
            val darkTheme = themeSetting.isDark(isSystemInDarkTheme())

            // Status and navigation bar icons follow the app's theme, not just the system's.
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            val session by viewModel.session.collectAsStateWithLifecycle()

            SplitEasyTheme(themeSetting) {
                when (val current = session) {
                    // Still checking; the splash screen is showing (see installSplashScreen above).
                    null -> Unit

                    Session.SignedOut -> SignInRoute(
                        webClientId = getString(R.string.default_web_client_id),
                    )

                    is Session.Preparing -> PreparingScreen()

                    // Keyed by account, so a different account never sees the previous one's screens.
                    is Session.SignedIn -> key(current.user.userId) { SplitEasyApp() }
                }
            }
        }
    }
}

/** Shown for a moment while the previous account's data is cleared (M4-Q2). */
@Composable
private fun PreparingScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(SplitEasyTheme.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SplitEasyTheme.colors.primary)
            Text(
                stringResource(DesignR.string.sign_in_preparing),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitEasyTheme.colors.muted,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
