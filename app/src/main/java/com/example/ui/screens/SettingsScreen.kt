package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TvDevice
import com.example.network.SamsungTvWebSocket
import com.example.ui.components.GlassButton
import com.example.ui.components.MeshGradientBackground
import com.example.ui.theme.*
import com.example.ui.viewmodel.RemoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ActiveModal {
    RECONNECT, SCAN, MANUAL, FORGET, FEEDBACK, EXIT
}

@Composable
fun SettingsScreen(
    viewModel: RemoteViewModel,
    onBack: () -> Unit,
    onExitApp: () -> Unit,
    showToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticsEnabled by viewModel.hapticsEnabledState.collectAsState()
    val screenAlwaysOn by viewModel.screenAlwaysOnState.collectAsState()
    val dynamicMeshEnabled by viewModel.dynamicMeshEnabledState.collectAsState()
    val remoteScaleSize by viewModel.remoteScaleSizeState.collectAsState()
    val savedTv by viewModel.savedTvState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Active Modals & Dialog Controllers
    var activeModal by remember { mutableStateOf<ActiveModal?>(null) }
    var manualIpText by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf("") }

    val discoveredTvs by viewModel.discoveredTvs.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val reconnectProgress by viewModel.reconnectModalState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Trigger SSDP scanning when opening SSDP modal
    LaunchedEffect(activeModal) {
        if (activeModal == ActiveModal.SCAN) {
            viewModel.startSsdpDiscovery()
        } else {
            viewModel.stopSsdpDiscovery()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Core background mirror
        MeshGradientBackground(modifier = Modifier.fillMaxSize(), enabled = dynamicMeshEnabled)

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassButton(
                    onClick = {
                        viewModel.triggerVibration(10)
                        onBack()
                    },
                    modifier = Modifier.size(38.dp),
                    cornerRadius = 19.dp,
                    testTag = "settings_back"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Pengaturan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Scrollable settings sheet elements matching CSS 1:1
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // TV Connected Info Card
                item {
                    TvInfoCard(
                        tv = savedTv,
                        isConnected = connectionState == SamsungTvWebSocket.ConnectionState.CONNECTED
                    )
                }

                // KONEKSI GROUP
                item {
                    SettingsGroupHeader("Koneksi")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                    ) {
                        SettingsRow(
                            icon = Icons.Default.Wifi,
                            title = "Hubungkan ulang TV",
                            description = "Cari ulang & sambungkan ke TV ini",
                            onClick = {
                                viewModel.triggerVibration(10)
                                activeModal = ActiveModal.RECONNECT
                                viewModel.reconnectTv()
                            },
                            testTag = "set_reconnect"
                        )
                        Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                        SettingsRow(
                            icon = Icons.Default.Search,
                            title = "Pindai TV lain",
                            description = "Tambahkan TV baru di jaringan",
                            onClick = {
                                viewModel.triggerVibration(10)
                                activeModal = ActiveModal.SCAN
                            },
                            testTag = "set_scan"
                        )
                        Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                        SettingsRow(
                            icon = Icons.Default.Edit,
                            title = "Sambungkan manual",
                            description = "Masukkan IP TV secara langsung",
                            onClick = {
                                viewModel.triggerVibration(10)
                                manualIpText = savedTv?.ip ?: ""
                                activeModal = ActiveModal.MANUAL
                            },
                            testTag = "set_manual"
                        )
                        Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                        SettingsRow(
                            icon = Icons.Default.Refresh,
                            title = "Lupakan TV ini",
                            description = "Hapus token & data koneksi tersimpan",
                            onClick = {
                                viewModel.triggerVibration(10)
                                activeModal = ActiveModal.FORGET
                            },
                            testTag = "set_forget"
                        )
                    }
                }

                // TAMPILAN & PENGALAMAN GROUP
                item {
                    SettingsGroupHeader("Tampilan & Pengalaman")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                    ) {
                        SettingsToggleRow(
                            icon = Icons.Default.Vibration,
                            title = "Getar saat tombol ditekan",
                            description = "Haptic feedback tiap tap",
                            checked = hapticsEnabled,
                            onCheckedChange = { viewModel.toggleHaptics(it) },
                            testTag = "set_toggle_haptics"
                        )
                        Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                        SettingsToggleRow(
                            icon = Icons.Default.LightMode,
                            title = "Tetap terang di tangan",
                            description = "Cegah layar HP redup saat dipakai",
                            checked = screenAlwaysOn,
                            onCheckedChange = { viewModel.toggleScreenAlwaysOn(it) },
                            testTag = "set_toggle_screen"
                        )
                        Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                        SettingsToggleRow(
                            icon = Icons.Default.NightsStay,
                            title = "Latar belakang dinamis",
                            description = "Efek aurora di background app",
                            checked = dynamicMeshEnabled,
                            onCheckedChange = { viewModel.toggleDynamicMesh(it) },
                            testTag = "set_toggle_mesh"
                        )
                    }
                }

                // REMOTE SIZING SCALE SELECTOR
                item {
                    SettingsGroupHeader("Ukuran Tampilan Remote")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sizes = listOf(
                                "compact" to "Kompak",
                                "fit" to "Pas di layar",
                                "large" to "Besar"
                            )
                            sizes.forEach { size ->
                                Box(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .then(
                                            if (remoteScaleSize == size.first) {
                                                Modifier.background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            AccentNav.copy(alpha = 0.22f),
                                                            AccentNav2.copy(alpha = 0.18f)
                                                        )
                                                    )
                                                )
                                            } else {
                                                Modifier.background(Color.White.copy(alpha = 0.05f))
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (remoteScaleSize == size.first) AccentNav.copy(alpha = 0.4f) else GlassBorder,
                                            RoundedCornerShape(999.dp)
                                        )
                                        .testTag("scale_chip_${size.first}")
                                        .clickable {
                                            viewModel.setRemoteScaleSize(size.first)
                                            showToast("Ukuran diatur ke \"${size.second}\"")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = size.second,
                                        color = if (remoteScaleSize == size.first) Color.White else TextDim,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (remoteScaleSize == size.first) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sesuaikan ukuran tombol agar pas dengan layar HP kamu, tanpa terpotong di tepi.",
                            color = TextFaint,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // LAINNYA GROUP
                item {
                    SettingsGroupHeader("Lainnya")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                    ) {
                        SettingsValueRow(
                            icon = Icons.Default.Info,
                            title = "Tentang aplikasi",
                            description = "Versi, lisensi, dan info build",
                            value = "v1.0.0",
                            onClick = { viewModel.triggerVibration(10) },
                            testTag = "set_info"
                        )
                        Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                        SettingsRow(
                            icon = Icons.Default.Feedback,
                            title = "Kirim masukan",
                            description = "Laporkan bug atau saran fitur",
                            onClick = {
                                viewModel.triggerVibration(10)
                                feedbackText = ""
                                activeModal = ActiveModal.FEEDBACK
                            },
                            testTag = "set_feedback"
                        )
                        Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                        SettingsRow(
                            icon = Icons.Default.ExitToApp,
                            title = "Keluar dari aplikasi",
                            description = "Tutup remote sepenuhnya",
                            isDanger = true,
                            onClick = {
                                viewModel.triggerVibration(10)
                                activeModal = ActiveModal.EXIT
                            },
                            testTag = "set_exit"
                        )
                    }
                }

                // Bottom App info signature label
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TV Remote · versi 1.0.0 (build 26062201)",
                            fontSize = 11.sp,
                            color = TextFaint,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ==========================================
        //  MODAL DIALOG IMPLEMENTATIONS
        // ==========================================

        if (activeModal != null) {
            Dialog(
                onDismissRequest = {
                    if (activeModal != ActiveModal.RECONNECT) {
                        activeModal = null
                    }
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                // Background overlay backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable {
                            if (activeModal != ActiveModal.RECONNECT) {
                                activeModal = null
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Modal bottom sheet container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1A1C22))
                            .border(1.dp, GlassBorderStrong, RoundedCornerShape(24.dp))
                            .clickable(enabled = false) {}
                            .padding(20.dp)
                    ) {
                        when (activeModal) {
                            ActiveModal.RECONNECT -> {
                                DialogReconnectBody(
                                    reconnectProgress = reconnectProgress,
                                    ipAddress = savedTv?.ip ?: "192.168.1.42",
                                    onDismiss = {
                                        viewModel.resetReconnectModal()
                                        activeModal = null
                                    }
                                )
                            }
                            ActiveModal.SCAN -> {
                                DialogScanBody(
                                    discoveredTvs = discoveredTvs,
                                    isDiscovering = isDiscovering,
                                    onSelect = { tv ->
                                        viewModel.connectToTv(tv)
                                        showToast("Menghubungkan ke ${tv.name}…")
                                        activeModal = null
                                    },
                                    onDismiss = { activeModal = null }
                                )
                            }
                            ActiveModal.MANUAL -> {
                                DialogManualBody(
                                    ipText = manualIpText,
                                    onIpChange = { manualIpText = it },
                                    onSubmit = {
                                        viewModel.connectManual(manualIpText)
                                        showToast("Menyambungkan ke $manualIpText…")
                                        activeModal = null
                                    },
                                    onDismiss = { activeModal = null }
                                )
                            }
                            ActiveModal.FORGET -> {
                                DialogForgetBody(
                                    tvName = savedTv?.name ?: "Samsung UA32N4300",
                                    onConfirm = {
                                        viewModel.forgetTv()
                                        showToast("TV ini sudah dilupakan")
                                        activeModal = null
                                    },
                                    onDismiss = { activeModal = null }
                                )
                            }
                            ActiveModal.FEEDBACK -> {
                                DialogFeedbackBody(
                                    feedbackText = feedbackText,
                                    onTextChange = { feedbackText = it },
                                    onSubmit = {
                                        showToast("Terima kasih atas masukannya")
                                        activeModal = null
                                    },
                                    onDismiss = { activeModal = null }
                                )
                            }
                            ActiveModal.EXIT -> {
                                DialogExitBody(
                                    onConfirm = {
                                        activeModal = null
                                        onExitApp()
                                    },
                                    onDismiss = { activeModal = null }
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvInfoCard(
    tv: TvDevice?,
    isConnected: Boolean
) {
    val displayTv = tv ?: TvDevice(ip = "Tidak Tersambung", name = "Samsung Smart TV", macAddress = "A1:B2:C3:D4:E5", signalStrength = "Bagus")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        AccentNav.copy(alpha = 0.10f),
                        AccentNav2.copy(alpha = 0.07f)
                    )
                )
            )
            .border(1.dp, AccentNav.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = "TV Icon",
                    tint = AccentNav,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = displayTv.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = displayTv.model,
                    color = TextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Connection Pill Status
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isConnected) ConnectedColor.copy(alpha = 0.15f) else DisconnectedColor.copy(
                        alpha = 0.15f
                    )
                )
                .padding(vertical = 3.dp, horizontal = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isConnected) ConnectedColor else DisconnectedColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "Terhubung" else "Sesi Terputus",
                    color = if (isConnected) Color(0xFF7CEBD6) else Color(0xFFFF8A8A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // TV Meta parameters grid list
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val items = listOf(
                Pair("Alamat IP", displayTv.ip),
                Pair("Port Websocket", if (isConnected) "${displayTv.port} (WS/WSS)" else "Fallback Port"),
                Pair("Alamat MAC", displayTv.macAddress),
                Pair("Sinyal Wi-Fi", displayTv.signalStrength)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    TvMetaItem(label = items[0].first, value = items[0].second)
                    Spacer(modifier = Modifier.height(10.dp))
                    TvMetaItem(label = items[2].first, value = items[2].second)
                }
                Column(modifier = Modifier.weight(1f)) {
                    TvMetaItem(label = items[1].first, value = items[1].second)
                    Spacer(modifier = Modifier.height(10.dp))
                    TvMetaItem(label = items[3].first, value = items[3].second)
                }
            }
        }
    }
}

@Composable
fun TvMetaItem(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            color = TextFaint,
            letterSpacing = 0.06.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextFaint,
        letterSpacing = 0.1.sp,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    description: String,
    isDanger: Boolean = false,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDanger) Color(0xFFFF8A8A) else TextDim,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isDanger) Color(0xFFFF8A8A) else TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = TextFaint,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Go",
            tint = TextFaint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextDim,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = TextFaint,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentNav,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextDim,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = TextFaint,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Text(
            text = value,
            color = TextDim,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
//  DIALOG COMPONENT INTERNAL BODIES
// ==========================================

@Composable
fun DialogReconnectBody(
    reconnectProgress: RemoteViewModel.ModalProgress,
    ipAddress: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (reconnectProgress == RemoteViewModel.ModalProgress.LOADING) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentNav.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Search",
                    tint = AccentNav,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Menghubungkan ulang…",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Mencoba menyambung ke TV di $ipAddress",
                fontSize = 12.5.sp,
                color = TextDim,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = AccentNav,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Mengecek port 8001 & 8002…",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "Batal", color = TextDim, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            // Success State Handshake
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentNav.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = AccentNav,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Berhasil terhubung",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Samsung Smart TV siap dikendalikan.",
                fontSize = 12.5.sp,
                color = TextDim,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentNav),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "Selesai", color = Color(0xFF06201C), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DialogScanBody(
    discoveredTvs: List<TvDevice>,
    isDiscovering: Boolean,
    onSelect: (TvDevice) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AccentNav.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Scan",
                tint = AccentNav,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isDiscovering && discoveredTvs.isEmpty()) "Memindai jaringan…" else "${discoveredTvs.size} TV ditemukan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isDiscovering && discoveredTvs.isEmpty()) "Mencari TV Samsung di jaringan Wi-Fi yang sama" else "Pilih TV untuk menyambungkan.",
            fontSize = 12.5.sp,
            color = TextDim,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isDiscovering && discoveredTvs.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = AccentNav,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Mengirim SSDP discovery…",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }
        } else {
            // Devices Lists Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (discoveredTvs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Tidak ada TV baru ditemukan", color = TextFaint, fontSize = 12.5.sp)
                    }
                } else {
                    discoveredTvs.forEach { tv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, GlassBorder, RoundedCornerShape(13.dp))
                                .clickable { onSelect(tv) }
                                .padding(11.dp, 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "Active target TV",
                                tint = AccentNav,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = tv.name, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${tv.ip} · terdeteksi", color = TextFaint, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Tutup", color = TextDim, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DialogManualBody(
    ipText: String,
    onIpChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AccentNav.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Manual IP",
                tint = AccentNav,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Sambungkan manual",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Masukkan alamat IP TV yang terlihat di Menu > Network > Network Status pada TV.",
            fontSize = 12.5.sp,
            color = TextDim,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = ipText,
            onValueChange = onIpChange,
            placeholder = { Text(text = "contoh: 192.168.1.42", color = TextFaint) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedIndicatorColor = AccentNav,
                unfocusedIndicatorColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_ip_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Batal", color = TextDim, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = AccentNav),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("manual_ip_submit")
            ) {
                Text(text = "Sambungkan", color = Color(0xFF06201C), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DialogForgetBody(
    tvName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFF8A8A).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Forget",
                tint = Color(0xFFFF8A8A),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Lupakan TV ini?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Token & data koneksi ke $tvName akan dihapus. Kamu perlu menyetujui ulang permintaan koneksi di TV saat menyambung lagi.",
            fontSize = 12.5.sp,
            color = TextDim,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Batal", color = TextDim, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A5A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Lupakan", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DialogFeedbackBody(
    feedbackText: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AccentNav.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Feedback,
                contentDescription = "Feedback",
                tint = AccentNav,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Kirim masukan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ceritakan bug yang kamu temukan atau fitur yang kamu inginkan.",
            fontSize = 12.5.sp,
            color = TextDim,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = feedbackText,
            onValueChange = onTextChange,
            placeholder = { Text(text = "Tulis masukanmu di sini…", color = TextFaint) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedIndicatorColor = AccentNav,
                unfocusedIndicatorColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Batal", color = TextDim, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSubmit,
                enabled = feedbackText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentNav, disabledContainerColor = AccentNav.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Kirim", color = Color(0xFF06201C), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DialogExitBody(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFF8A8A).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Exit",
                tint = Color(0xFFFF8A8A),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Keluar dari aplikasi?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Remote akan ditutup sepenuhnya. Koneksi ke TV tetap tersimpan untuk dipakai lagi nanti.",
            fontSize = 12.5.sp,
            color = TextDim,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Batal", color = TextDim, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A5A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "Keluar", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
