package com.orange.nowatermark

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.orange.nowatermark.data.DataManager
import com.orange.nowatermark.ui.AnalyseScreen
import com.orange.nowatermark.ui.AudioConvertScreen
import com.orange.nowatermark.ui.BatchScreen
import com.orange.nowatermark.ui.CropScreen
import com.orange.nowatermark.ui.DonateScreen
import com.orange.nowatermark.ui.ExtractAudioScreen
import com.orange.nowatermark.ui.HomeScreen
import com.orange.nowatermark.ui.PrivacyDialog
import com.orange.nowatermark.ui.PrivacyScreen
import com.orange.nowatermark.ui.ProfileScreen
import com.orange.nowatermark.ui.RemoveMusicScreen
import com.orange.nowatermark.ui.TutorialScreen
import com.orange.nowatermark.ui.WatermarkScreen
import com.orange.nowatermark.ui.glass.glass
import com.orange.nowatermark.ui.theme.AppTheme

enum class Screen {
    HOME, ANALYSE, BATCH, REMOVE_MUSIC, WATERMARK, CROP, EXTRACT_AUDIO, AUDIO_CONVERT,
    TUTORIAL, PRIVACY, PROFILE, DONATE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataManager.init(this)
        DataManager.loadSettings()
        setContent { AppTheme { AppRoot() } }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.HOME) }
    var privacyAgreed by remember { mutableStateOf(DataManager.isPrivacyAgreed()) }
    val showNav = screen == Screen.HOME || screen == Screen.PROFILE

    BackHandler(enabled = screen != Screen.HOME && screen != Screen.PROFILE) {
        screen = Screen.HOME
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = if (showNav) 118.dp else 0.dp)) {
            when (screen) {
                Screen.HOME -> HomeScreen(onNavigate = { screen = it })
                Screen.ANALYSE -> AnalyseScreen(onBack = { screen = Screen.HOME })
                Screen.BATCH -> BatchScreen(onBack = { screen = Screen.HOME })
                Screen.REMOVE_MUSIC -> RemoveMusicScreen(onBack = { screen = Screen.HOME })
                Screen.WATERMARK -> WatermarkScreen(onBack = { screen = Screen.HOME })
                Screen.CROP -> CropScreen(onBack = { screen = Screen.HOME })
                Screen.EXTRACT_AUDIO -> ExtractAudioScreen(onBack = { screen = Screen.HOME })
                Screen.AUDIO_CONVERT -> AudioConvertScreen(onBack = { screen = Screen.HOME })
                Screen.TUTORIAL -> TutorialScreen(onBack = { screen = Screen.HOME })
                Screen.PRIVACY -> PrivacyScreen(onBack = { screen = Screen.PROFILE })
                Screen.PROFILE -> ProfileScreen(onNavigate = { screen = it })
                Screen.DONATE -> DonateScreen(onBack = { screen = Screen.PROFILE })
            }
        }

        if (showNav) {
            FloatingNav(
                current = screen,
                onSelect = { screen = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 48.dp, vertical = 18.dp)
            )
        }
    }

    if (!privacyAgreed) {
        PrivacyDialog(
            onAgree = {
                DataManager.setPrivacyAgreed(true)
                privacyAgreed = true
            },
            onExit = { (context as? Activity)?.finish() }
        )
    }
}

@Composable
private fun FloatingNav(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(28.dp), Color.White.copy(alpha = 0.62f), elevation = 24.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavItem("首页", Icons.Outlined.Home, current == Screen.HOME, Modifier.weight(1f)) {
            onSelect(Screen.HOME)
        }
        NavItem("我的", Icons.Outlined.Person, current == Screen.PROFILE, Modifier.weight(1f)) {
            onSelect(Screen.PROFILE)
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        if (selected) Color(0xFF0A6CFF) else Color(0xFF5B6472),
        label = "nav-tint"
    )
    Row(
        modifier = modifier
            .background(
                if (selected) Color(0xFF0A6CFF).copy(alpha = 0.10f) else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.width(7.dp))
        Text(label, color = tint, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
    }
}
