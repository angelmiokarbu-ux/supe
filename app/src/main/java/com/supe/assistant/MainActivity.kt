package com.supe.assistant

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.supe.assistant.data.ai.LocalAssistantEngine
import com.supe.assistant.data.model.ReminderItem
import com.supe.assistant.data.model.ReminderType
import com.supe.assistant.data.repository.ReminderRepository
import com.supe.assistant.service.BillNotificationListenerService
import com.supe.assistant.service.ReminderNotificationHelper
import com.supe.assistant.ui.screens.AddReminderDialog
import com.supe.assistant.ui.screens.AssistantChatScreen
import com.supe.assistant.ui.screens.DashboardScreen
import com.supe.assistant.ui.screens.NotificationBillsScreen
import com.supe.assistant.ui.screens.VoiceRecordingSheet
import com.supe.assistant.ui.theme.SupeTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var repository: ReminderRepository
    private var tempCameraUri: Uri? = null
    private var tempCameraFile: File? = null

    private var openVoiceFromIntent by mutableStateOf(false)
    private var openNoteFromIntent by mutableStateOf(false)
    private var initialTabFromIntent by mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    private var onImageCapturedCallback: ((String) -> Unit)? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.absolutePath?.let { path ->
                onImageCapturedCallback?.invoke(path)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val destination = File(filesDir, "gallery_${System.currentTimeMillis()}.jpg")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
                onImageCapturedCallback?.invoke(destination.absolutePath)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = ReminderRepository(this)
        ReminderNotificationHelper.createNotificationChannels(this)

        requestRequiredPermissions()
        handleLaunchIntent(intent)

        setContent {
            SupeTheme {
                MainAppContent(
                    repository = repository,
                    initialTab = initialTabFromIntent,
                    triggerVoiceOnStart = openVoiceFromIntent,
                    triggerNoteOnStart = openNoteFromIntent,
                    onResetVoiceTrigger = { openVoiceFromIntent = false },
                    onResetNoteTrigger = { openNoteFromIntent = false },
                    onCaptureCamera = { onCaptured ->
                        onImageCapturedCallback = onCaptured
                        val file = File(filesDir, "camera_${System.currentTimeMillis()}.jpg")
                        tempCameraFile = file
                        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                        tempCameraUri = uri
                        takePictureLauncher.launch(uri)
                    },
                    onPickGallery = { onPicked ->
                        onImageCapturedCallback = onPicked
                        pickImageLauncher.launch("image/*")
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            "com.supe.assistant.ACTION_QUICK_VOICE" -> openVoiceFromIntent = true
            "com.supe.assistant.ACTION_QUICK_NOTE" -> openNoteFromIntent = true
        }
        if (intent.getStringExtra("EXTRA_NAVIGATE_TAB") == "bills") {
            initialTabFromIntent = 1
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    repository: ReminderRepository,
    initialTab: Int,
    triggerVoiceOnStart: Boolean,
    triggerNoteOnStart: Boolean,
    onResetVoiceTrigger: () -> Unit,
    onResetNoteTrigger: () -> Unit,
    onCaptureCamera: ((String) -> Unit) -> Unit,
    onPickGallery: ((String) -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }

    // State for prefilling AddDialog from camera or gallery
    var pendingImagePath by remember { mutableStateOf<String?>(null) }
    var pendingReminderType by remember { mutableStateOf(ReminderType.TEXT) }

    val reminders by repository.reminders.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val chatMessages by repository.chatMessages.collectAsState()
    val isLoading by repository.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        repository.loadAll()
    }

    LaunchedEffect(triggerVoiceOnStart) {
        if (triggerVoiceOnStart) {
            showVoiceSheet = true
            onResetVoiceTrigger()
        }
    }

    LaunchedEffect(triggerNoteOnStart) {
        if (triggerNoteOnStart) {
            pendingImagePath = null
            pendingReminderType = ReminderType.TEXT
            showAddDialog = true
            onResetNoteTrigger()
        }
    }

    // Register receiver to refresh when notification is posted
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                scope.launch { repository.loadAll() }
            }
        }
        val filter = IntentFilter(BillNotificationListenerService.ACTION_NOTIFICATION_CAPTURED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTabIndex) {
                            0 -> "Supe Pengingat"
                            1 -> "Notifikasi Tagihan"
                            else -> "Tanya Supe"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(imageVector = Icons.Default.ListAlt, contentDescription = "Pengingat") },
                    label = { Text("Pengingat") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = "Notifikasi") },
                    label = { Text("Notifikasi") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = "Tanya Supe") },
                    label = { Text("Tanya Supe") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "ScreenTransition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> DashboardScreen(
                        reminders = reminders,
                        isLoading = isLoading,
                        onToggleCompletion = { item ->
                            scope.launch {
                                repository.toggleReminderCompletion(item.id, !item.isCompleted)
                            }
                        },
                        onDeleteReminder = { item ->
                            scope.launch {
                                repository.deleteReminder(item.id)
                                snackbarHostState.showSnackbar("Pengingat dihapus.")
                            }
                        },
                        onOpenTextInput = {
                            pendingImagePath = null
                            pendingReminderType = ReminderType.TEXT
                            showAddDialog = true
                        },
                        onOpenVoiceInput = {
                            showVoiceSheet = true
                        },
                        onOpenCameraInput = {
                            onCaptureCamera { photoPath ->
                                pendingImagePath = photoPath
                                pendingReminderType = ReminderType.CAMERA
                                showAddDialog = true
                            }
                        },
                        onOpenGalleryInput = {
                            onPickGallery { photoPath ->
                                pendingImagePath = photoPath
                                pendingReminderType = ReminderType.IMAGE
                                showAddDialog = true
                            }
                        }
                    )
                    1 -> NotificationBillsScreen(
                        notifications = notifications,
                        onConvertToReminder = { notif ->
                            scope.launch {
                                val result = repository.convertNotificationToReminder(notif)
                                result.fold(
                                    onSuccess = {
                                        snackbarHostState.showSnackbar("Tagihan berhasil dijadikan pengingat.")
                                    },
                                    onFailure = {
                                        snackbarHostState.showSnackbar("Gagal menambahkan pengingat.")
                                    }
                                )
                            }
                        }
                    )
                    2 -> AssistantChatScreen(
                        messages = chatMessages,
                        reminders = reminders,
                        onSendMessage = { query ->
                            scope.launch {
                                repository.addChatMessage(query, isFromUser = true)
                                val answer = LocalAssistantEngine.generateAnswer(query, reminders)
                                repository.addChatMessage(answer, isFromUser = false)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            initialType = pendingReminderType,
            initialImageUri = pendingImagePath,
            onDismiss = { showAddDialog = false },
            onSave = { reminder ->
                scope.launch {
                    repository.addReminder(reminder)
                    showAddDialog = false
                    snackbarHostState.showSnackbar("Pengingat berhasil disimpan.")
                }
            }
        )
    }

    if (showVoiceSheet) {
        VoiceRecordingSheet(
            onDismiss = { showVoiceSheet = false },
            onSaveReminder = { reminder ->
                scope.launch {
                    repository.addReminder(reminder)
                    showVoiceSheet = false
                    snackbarHostState.showSnackbar("Catatan suara berhasil disimpan.")
                }
            }
        )
    }
}
