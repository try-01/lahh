package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.SamsungTvWebSocket
import com.example.ui.components.DpadControl
import com.example.ui.components.GlassButton
import com.example.ui.components.MeshGradientBackground
import com.example.ui.theme.*
import com.example.ui.viewmodel.RemoteViewModel

@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val savedTv by viewModel.savedTvState.collectAsState()
    val dynamicMeshEnabled by viewModel.dynamicMeshEnabledState.collectAsState()
    val remoteScaleSize by viewModel.remoteScaleSizeState.collectAsState()

    val spacing = when (remoteScaleSize) {
        "compact" -> 12.dp
        "large" -> 22.dp
        else -> 16.dp
    }

    val buttonHeight = when (remoteScaleSize) {
        "compact" -> 46.dp
        "large" -> 62.dp
        else -> 54.dp
    }

    Box(modifier = modifier.fillMaxSize()) {
        MeshGradientBackground(modifier = Modifier.fillMaxSize(), enabled = dynamicMeshEnabled)

        Column(modifier = Modifier.fillMaxSize()) {
            HeaderBar(
                connectionState = connectionState,
                tvName = savedTv?.name ?: "SAMSUNG REMOTE",
                onSettingsClick = onNavigateToSettings
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                item {
                    ZonePowerSourceSleep(
                        onPower = { viewModel.sendRemoteKey("KEY_POWER") },
                        onSource = { viewModel.sendRemoteKey("KEY_SOURCE") },
                        onSleep = { viewModel.sendRemoteKey("KEY_CH_ALL") },
                        onVibrate = { viewModel.triggerVibration(10) },
                        height = buttonHeight
                    )
                }

                item {
                    ZoneNavigation(
                        onUp = { viewModel.sendRemoteKey("KEY_UP") },
                        onDown = { viewModel.sendRemoteKey("KEY_DOWN") },
                        onLeft = { viewModel.sendRemoteKey("KEY_LEFT") },
                        onRight = { viewModel.sendRemoteKey("KEY_RIGHT") },
                        onOk = { viewModel.sendRemoteKey("KEY_ENTER") },
                        onReturn = { viewModel.sendRemoteKey("KEY_RETURN") },
                        onHome = { viewModel.sendRemoteKey("KEY_HOME") },
                        onExit = { viewModel.sendRemoteKey("KEY_EXIT") },
                        onVibrate = { viewModel.triggerVibration(10) },
                        height = buttonHeight,
                        spacing = spacing
                    )
                }

                item {
                    ZoneVolumeChannel(
                        onVolUp = { viewModel.sendRemoteKey("KEY_VOLUP") },
                        onVolDown = { viewModel.sendRemoteKey("KEY_VOLDOWN") },
                        onMute = { viewModel.sendRemoteKey("KEY_MUTE") },
                        onChUp = { viewModel.sendRemoteKey("KEY_CHUP") },
                        onChDown = { viewModel.sendRemoteKey("KEY_CHDOWN") },
                        onChList = { viewModel.sendRemoteKey("KEY_CH_LIST") },
                        onPreCh = { viewModel.sendRemoteKey("KEY_PRECH") },
                        onVibrate = { viewModel.triggerVibration(10) },
                        height = buttonHeight,
                        spacing = spacing
                    )
                }

                item {
                    ZoneNumericNumpad(
                        onNumClick = { num -> viewModel.sendRemoteKey("KEY_$num") },
                        onDashClick = { viewModel.sendRemoteKey("KEY_DASH") },
                        onDeleteClick = { viewModel.sendRemoteKey("KEY_PRECH") },
                        onVibrate = { viewModel.triggerVibration(10) },
                        height = buttonHeight,
                        spacing = spacing
                    )
                }

                item {
                    ZoneSmartHubColors(
                        onRed = { viewModel.sendRemoteKey("KEY_RED") },
                        onGreen = { viewModel.sendRemoteKey("KEY_GREEN") },
                        onYellow = { viewModel.sendRemoteKey("KEY_YELLOW") },
                        onBlue = { viewModel.sendRemoteKey("KEY_BLUE") },
                        onVibrate = { viewModel.triggerVibration(10) },
                        height = buttonHeight,
                        spacing = spacing
                    )
                }

                item {
                    ZoneMediaPlayback(
                        onRewind = { viewModel.sendRemoteKey("KEY_REWIND") },
                        onPlay = { viewModel.sendRemoteKey("KEY_PLAY") },
                        onPause = { viewModel.sendRemoteKey("KEY_PAUSE") },
                        onStop = { viewModel.sendRemoteKey("KEY_STOP") },
                        onFastForward = { viewModel.sendRemoteKey("KEY_FF") },
                        onVibrate = { viewModel.triggerVibration(10) },
                        height = buttonHeight,
                        spacing = spacing
                    )
                }

                item {
                    ZoneMenuFunction(
                        onMenu = { viewModel.sendRemoteKey("KEY_MENU") },
                        onGuide = { viewModel.sendRemoteKey("KEY_GUIDE") },
                        onInfo = { viewModel.sendRemoteKey("KEY_INFO") },
                        onSettings = { viewModel.sendRemoteKey("KEY_SETTINGS") },
                        onPsize = { viewModel.sendRemoteKey("KEY_PICTURE_SIZE") },
                        onCcVd = { viewModel.sendRemoteKey("KEY_SUBTITLE") },
                        onVibrate = { viewModel.triggerVibration(10) },
                        height = buttonHeight,
                        spacing = spacing
                    )
                }

                item {
                    ZoneAppShortcuts(
                        onNetflix = { viewModel.sendRemoteKey("KEY_NETFLIX") },
                        onPrime = { viewModel.sendRemoteKey("KEY_AMAZON") },
                        onYoutube = { viewModel.sendRemoteKey("KEY_YOUTUBE") },
                        onVibrate = { viewModel.triggerVibration(10) },
                        height = buttonHeight,
                        spacing = spacing
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderBar(
    connectionState: SamsungTvWebSocket.ConnectionState,
    tvName: String,
    onSettingsClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_anim"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale_anim"
    )

    val statusStyle = when (connectionState) {
        SamsungTvWebSocket.ConnectionState.CONNECTED -> Triple("Terhubung", ConnectedColor, true)
        SamsungTvWebSocket.ConnectionState.CONNECTING -> Triple("Menyambung\u2026", ConnectingColor, true)
        SamsungTvWebSocket.ConnectionState.DISCONNECTED -> Triple("Terputus", DisconnectedColor, false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
            .background(GlassSurface, RoundedCornerShape(999.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(999.dp))
            .padding(start = 14.dp, top = 9.dp, end = 8.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .scale(if (statusStyle.third) scale else 1.0f)
                .size(8.dp)
                .background(
                    statusStyle.second.copy(alpha = if (statusStyle.third) alpha else 1.0f),
                    CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = statusStyle.first,
            color = TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = tvName.uppercase(),
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f).padding(end = 10.dp)
        )

        GlassButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(34.dp),
            cornerRadius = 17.dp,
            testTag = "header_settings"
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextDim,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SectionLabel(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 2.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextFaint,
            letterSpacing = 0.12.sp
        )
    }
}

@Composable
fun ZoneLabelGradient(title: String, startColor: Color, endColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 2.dp)
                .background(Brush.linearGradient(listOf(startColor, endColor)), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextFaint,
            letterSpacing = 0.12.sp
        )
    }
}

@Composable
fun ZonePowerSourceSleep(
    onPower: () -> Unit,
    onSource: () -> Unit,
    onSleep: () -> Unit,
    onVibrate: () -> Unit,
    height: Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassButton(
            onClick = onPower,
            modifier = Modifier
                .weight(1f)
                .height(height),
            customGradient = Brush.linearGradient(
                listOf(
                    com.example.ui.theme.AccentPower,
                    com.example.ui.theme.AccentPower
                )
            ),
            customBorderColor = Color.Transparent,
            onPressUpdate = onVibrate,
            testTag = "btn_power"
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "POWER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.04.sp
                )
            }
        }

        GlassButton(
            onClick = onSource,
            modifier = Modifier
                .weight(1f)
                .height(height),
            onPressUpdate = onVibrate,
            testTag = "btn_source"
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Input,
                    contentDescription = "Source",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SOURCE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDim,
                    letterSpacing = 0.04.sp
                )
            }
        }

        GlassButton(
            onClick = onSleep,
            modifier = Modifier
                .weight(1f)
                .height(height),
            onPressUpdate = onVibrate,
            testTag = "btn_sleep"
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = "Sleep",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SLEEP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDim,
                    letterSpacing = 0.04.sp
                )
            }
        }
    }
}

@Composable
fun ZoneNavigation(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    onReturn: () -> Unit,
    onHome: () -> Unit,
    onExit: () -> Unit,
    onVibrate: () -> Unit,
    height: Dp,
    spacing: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Navigasi", AccentNav)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            DpadControl(
                onUp = onUp,
                onDown = onDown,
                onLeft = onLeft,
                onRight = onRight,
                onOk = onOk,
                onVibrate = onVibrate
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                onClick = onReturn,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 4.dp),
                cornerRadius = 15.dp,
                onPressUpdate = onVibrate,
                testTag = "nav_back"
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            GlassButton(
                onClick = onHome,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 4.dp),
                cornerRadius = 15.dp,
                onPressUpdate = onVibrate,
                testTag = "nav_home"
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            GlassButton(
                onClick = onExit,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 4.dp),
                cornerRadius = 15.dp,
                onPressUpdate = onVibrate,
                testTag = "nav_exit"
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ZoneVolumeChannel(
    onVolUp: () -> Unit,
    onVolDown: () -> Unit,
    onMute: () -> Unit,
    onChUp: () -> Unit,
    onChDown: () -> Unit,
    onChList: () -> Unit,
    onPreCh: () -> Unit,
    onVibrate: () -> Unit,
    height: Dp,
    spacing: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Volume & Channel", AccentNav2)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBoxCell(
                onClick = onVolDown,
                onVibrate = onVibrate,
                modifier = Modifier.weight(1f),
                testTag = "vol_down"
            ) {
                Text(text = "\u2212", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "VOL", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.sp)
            }
            GlassBoxCell(
                onClick = onVolUp,
                onVibrate = onVibrate,
                modifier = Modifier.weight(1f),
                testTag = "vol_up"
            ) {
                Text(text = "+", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            GlassBoxCell(
                onClick = onMute,
                onVibrate = onVibrate,
                modifier = Modifier.weight(1f),
                testTag = "vol_mute"
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeMute,
                    contentDescription = "Mute",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBoxCell(
                onClick = onChDown,
                onVibrate = onVibrate,
                modifier = Modifier.weight(1f),
                testTag = "ch_down"
            ) {
                Text(text = "\u2212", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "CH", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.sp)
            }
            GlassBoxCell(
                onClick = onChUp,
                onVibrate = onVibrate,
                modifier = Modifier.weight(1f),
                testTag = "ch_up"
            ) {
                Text(text = "+", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            GlassBoxCell(
                onClick = onChList,
                onVibrate = onVibrate,
                modifier = Modifier.weight(1f),
                testTag = "ch_list"
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Chan List",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                onClick = onPreCh,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 6.dp),
                onPressUpdate = onVibrate,
                testTag = "btn_prech"
            ) {
                Text(text = "PRE-CH", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextDim, letterSpacing = 0.04.sp)
            }
            GlassButton(
                onClick = onChList,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 6.dp),
                onPressUpdate = onVibrate,
                testTag = "btn_chlist"
            ) {
                Text(text = "CH LIST", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextDim, letterSpacing = 0.04.sp)
            }
        }
    }
}

@Composable
fun RowScope.GlassBoxCell(
    onClick: () -> Unit,
    onVibrate: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    if (isPressed) {
        LaunchedEffect(Unit) {
            onVibrate()
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isPressed) GlassSurfacePressed else GlassSurface)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun ZoneNumericNumpad(
    onNumClick: (Int) -> Unit,
    onDashClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onVibrate: () -> Unit,
    height: Dp,
    spacing: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Angka", AccentWarn)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    for (col in 1..3) {
                        val num = row * 3 + col
                        GlassButton(
                            onClick = { onNumClick(num) },
                            modifier = Modifier
                                .weight(1f)
                                .height(height),
                            onPressUpdate = onVibrate,
                            testTag = "num_$num"
                        ) {
                            Text(
                                text = num.toString(),
                                color = TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                GlassButton(
                    onClick = onDashClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(height),
                    onPressUpdate = onVibrate,
                    testTag = "num_dash"
                ) {
                    Text(
                        text = "\u2212",
                        color = TextDim,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                GlassButton(
                    onClick = { onNumClick(0) },
                    modifier = Modifier
                        .weight(1f)
                        .height(height),
                    onPressUpdate = onVibrate,
                    testTag = "num_0"
                ) {
                    Text(
                        text = "0",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                GlassButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(height),
                    onPressUpdate = onVibrate,
                    testTag = "num_del"
                ) {
                    Text(
                        text = "\u232b",
                        color = TextDim,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ZoneSmartHubColors(
    onRed: () -> Unit,
    onGreen: () -> Unit,
    onYellow: () -> Unit,
    onBlue: () -> Unit,
    onVibrate: () -> Unit,
    height: Dp,
    spacing: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZoneLabelGradient("Smart Hub Color Keys", Color(0xFFFF5252), Color(0xFF4CD97B))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                onClick = onRed,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 8.dp),
                cornerRadius = 14.dp,
                customGradient = Brush.linearGradient(listOf(ColorKeyRedStart, ColorKeyRedEnd)),
                customBorderColor = ColorKeyRedText.copy(alpha = 0.35f),
                onPressUpdate = onVibrate,
                testTag = "color_red"
            ) {
                Text(text = "A", color = ColorKeyRedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            GlassButton(
                onClick = onGreen,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 8.dp),
                cornerRadius = 14.dp,
                customGradient = Brush.linearGradient(listOf(ColorKeyGreenStart, ColorKeyGreenEnd)),
                customBorderColor = ColorKeyGreenText.copy(alpha = 0.35f),
                onPressUpdate = onVibrate,
                testTag = "color_green"
            ) {
                Text(text = "B", color = ColorKeyGreenText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            GlassButton(
                onClick = onYellow,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 8.dp),
                cornerRadius = 14.dp,
                customGradient = Brush.linearGradient(listOf(ColorKeyYellowStart, ColorKeyYellowEnd)),
                customBorderColor = ColorKeyYellowText.copy(alpha = 0.35f),
                onPressUpdate = onVibrate,
                testTag = "color_yellow"
            ) {
                Text(text = "C", color = ColorKeyYellowText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            GlassButton(
                onClick = onBlue,
                modifier = Modifier
                    .weight(1f)
                    .height(height - 8.dp),
                cornerRadius = 14.dp,
                customGradient = Brush.linearGradient(listOf(ColorKeyBlueStart, ColorKeyBlueEnd)),
                customBorderColor = ColorKeyBlueText.copy(alpha = 0.35f),
                onPressUpdate = onVibrate,
                testTag = "color_blue"
            ) {
                Text(text = "D", color = ColorKeyBlueText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ZoneMediaPlayback(
    onRewind: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onFastForward: () -> Unit,
    onVibrate: () -> Unit,
    height: Dp,
    spacing: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Media", AccentMedia)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val keys = listOf(
                Icons.Default.FastRewind to onRewind,
                Icons.Default.PlayArrow to onPlay,
                Icons.Default.Pause to onPause,
                Icons.Default.Stop to onStop,
                Icons.Default.FastForward to onFastForward
            )

            keys.forEachIndexed { idx, pair ->
                GlassButton(
                    onClick = pair.second,
                    modifier = Modifier
                        .weight(1f)
                        .height(height - 2.dp),
                    cornerRadius = 15.dp,
                    customGradient = Brush.linearGradient(
                        listOf(
                            AccentMedia.copy(alpha = 0.14f),
                            AccentMedia2.copy(alpha = 0.10f)
                        )
                    ),
                    customBorderColor = AccentMedia.copy(alpha = 0.25f),
                    onPressUpdate = onVibrate,
                    testTag = "media_$idx"
                ) {
                    Icon(
                        imageVector = pair.first,
                        contentDescription = "Media Control",
                        tint = Color(0xFFD9C2FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ZoneMenuFunction(
    onMenu: () -> Unit,
    onGuide: () -> Unit,
    onInfo: () -> Unit,
    onSettings: () -> Unit,
    onPsize: () -> Unit,
    onCcVd: () -> Unit,
    onVibrate: () -> Unit,
    height: Dp,
    spacing: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Menu & Info", TextDim)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            val firstRow = listOf(
                "MENU" to onMenu,
                "GUIDE" to onGuide,
                "INFO" to onInfo
            )
            val secondRow = listOf(
                "SETTINGS" to onSettings,
                "P.SIZE" to onPsize,
                "CC/VD" to onCcVd
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                firstRow.forEach { item ->
                    GlassButton(
                        onClick = item.second,
                        modifier = Modifier
                            .weight(1f)
                            .height(height + 4.dp),
                        onPressUpdate = onVibrate,
                        testTag = "menu_${item.first.lowercase()}"
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val icon = when (item.first) {
                                "MENU" -> Icons.Default.Menu
                                "GUIDE" -> Icons.Default.Tv
                                else -> Icons.Default.Info
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = item.first,
                                tint = TextPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(text = item.first, fontSize = 10.sp, color = TextDim, letterSpacing = 0.03.sp)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                secondRow.forEach { item ->
                    GlassButton(
                        onClick = item.second,
                        modifier = Modifier
                            .weight(1f)
                            .height(height + 4.dp),
                        onPressUpdate = onVibrate,
                        testTag = "menu_${item.first.replace(".", "").replace("/", "").lowercase()}"
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val icon = when (item.first) {
                                "SETTINGS" -> Icons.Default.Settings
                                "P.SIZE" -> Icons.Default.AspectRatio
                                else -> Icons.Default.ClosedCaption
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = item.first,
                                tint = TextPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(text = item.first, fontSize = 10.sp, color = TextDim, letterSpacing = 0.03.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneAppShortcuts(
    onNetflix: () -> Unit,
    onPrime: () -> Unit,
    onYoutube: () -> Unit,
    onVibrate: () -> Unit,
    height: Dp,
    spacing: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZoneLabelGradient("App Pintasan", Color(0xFFE50914), Color(0xFF00A8E1))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                onClick = onNetflix,
                modifier = Modifier
                    .weight(1f)
                    .height(height),
                cornerRadius = 16.dp,
                customGradient = Brush.linearGradient(
                    listOf(
                        Color(0xFFE50914).copy(alpha = 0.18f),
                        Color(0xFFE50914).copy(alpha = 0.06f)
                    )
                ),
                customBorderColor = Color(0xFFE50914).copy(alpha = 0.32f),
                onPressUpdate = onVibrate,
                testTag = "app_netflix"
            ) {
                Text(
                    text = "NETFLIX",
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.02.sp
                )
            }

            GlassButton(
                onClick = onPrime,
                modifier = Modifier
                    .weight(1f)
                    .height(height),
                cornerRadius = 16.dp,
                customGradient = Brush.linearGradient(
                    listOf(
                        Color(0xFF00A8E1).copy(alpha = 0.18f),
                        Color(0xFF00A8E1).copy(alpha = 0.06f)
                    )
                ),
                customBorderColor = Color(0xFF00A8E1).copy(alpha = 0.32f),
                onPressUpdate = onVibrate,
                testTag = "app_prime"
            ) {
                Text(
                    text = "PRIME",
                    color = Color(0xFF6FD2F2),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.02.sp
                )
            }

            GlassButton(
                onClick = onYoutube,
                modifier = Modifier
                    .weight(1f)
                    .height(height),
                cornerRadius = 16.dp,
                customGradient = Brush.linearGradient(
                    listOf(
                        Color(0xFFFF0000).copy(alpha = 0.18f),
                        Color(0xFFFF0000).copy(alpha = 0.06f)
                    )
                ),
                customBorderColor = Color(0xFFFF0000).copy(alpha = 0.32f),
                onPressUpdate = onVibrate,
                testTag = "app_youtube"
            ) {
                Text(
                    text = "YOUTUBE",
                    color = Color(0xFFFF8080),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.02.sp
                )
            }
        }
    }
}
