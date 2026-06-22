package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.RemoteScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentNav
import com.example.ui.theme.GlassBorderStrong
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.RemoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer(onExitApp = { finish() })
            }
        }
    }
}

@Composable
fun MainAppContainer(
    onExitApp: () -> Unit,
    viewModel: RemoteViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf("remote") }
    val screenAlwaysOn by viewModel.screenAlwaysOnState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Custom overlay Toast structures matching `#toast` in HTML
    var toastMessage by remember { mutableStateOf("") }
    var showToastTrigger by remember { mutableStateOf(false) }

    fun triggerCustomToast(msg: String) {
        if (msg.isBlank()) return
        toastMessage = msg
        showToastTrigger = true
    }

    // Auto dismiss Toast after 2.2 seconds matching the web spec timer
    LaunchedEffect(showToastTrigger) {
        if (showToastTrigger) {
            viewModel.triggerVibration(10)
            delay(2200)
            showToastTrigger = false
        }
    }

    // Dynamic Wake Lock modifier linked to the user settings preference
    LaunchedEffect(screenAlwaysOn) {
        val window = (context as? ComponentActivity)?.window
        if (window != null) {
            if (screenAlwaysOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars // Respect notch & navigation spaces natively
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant horizontal slide screen navigator using AnimatedContent
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState == "settings") {
                        // Slide forward from right to left
                        slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                    } else {
                        // Slide backward from left to right
                        slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                    }
                },
                label = "screen_navigation"
            ) { screen ->
                when (screen) {
                    "remote" -> {
                        RemoteScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { currentScreen = "settings" }
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = "remote" },
                            onExitApp = onExitApp,
                            showToast = { triggerCustomToast(it) }
                        )
                    }
                }
            }

            // Custom Glassmorphic Toast banner overlay aligning absolutely with .toast elements
            AnimatedVisibility(
                visible = showToastTrigger,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(com.example.ui.theme.GlassSurface)
                        .border(1.dp, GlassBorderStrong, RoundedCornerShape(999.dp))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing green dot matching HTML indicator
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(AccentNav, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = toastMessage,
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
