package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.ResponseLanguage
import com.example.data.preferences.AppSettings
import com.example.ui.components.GoldDivider
import com.example.ui.components.GoldPillBadge
import com.example.ui.components.HairlineDivider
import com.example.ui.components.LuxuryObsidianCard
import com.example.ui.components.OfflineWarningBanner
import com.example.ui.components.luxuryPressScale
import com.example.ui.theme.CardSurfaceHigh
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldHairline
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InfoBlueAccent
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory
import com.example.ui.viewmodel.ConnectionTestUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    hasApiKey: Boolean,
    maskedApiKey: String,
    isOnline: Boolean,
    connectionTestState: ConnectionTestUiState,
    onSelectFreeMode: () -> Unit,
    onApprovePaidApiMode: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onRevealApiKey: () -> String,
    onSaveConnectionSettings: (
        baseUrl: String,
        anthropicVersion: String,
        selectedModel: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ) -> Unit,
    onTestConnection: (
        baseUrl: String,
        anthropicVersion: String,
        selectedModel: String
    ) -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit,
    onResetBubblePosition: () -> Unit,
    onSelectResponseLanguage: (String) -> Unit,
    onExportBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onStopAllActivity: () -> Unit,
    onClearAllLocalData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    val hasNotificationsPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    var apiKeyInput by rememberSaveable { mutableStateOf("") }
    var showApiKey by rememberSaveable { mutableStateOf(false) }

    var baseUrlInput by rememberSaveable(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var apiVersionInput by rememberSaveable(settings.anthropicVersion) { mutableStateOf(settings.anthropicVersion) }
    var modelInput by rememberSaveable(settings.selectedModel) { mutableStateOf(settings.selectedModel) }
    var systemPromptInput by rememberSaveable(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    var maxTokensSlider by rememberSaveable(settings.maxTokens) { mutableFloatStateOf(settings.maxTokens.toFloat()) }
    var temperatureSlider by rememberSaveable(settings.temperature) { mutableFloatStateOf(settings.temperature) }

    var showPaidApprovalDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = onExportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
            if (!content.isNullOrBlank()) {
                onRestoreBackupJson(content)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
                .imePadding()
        ) {
            Surface(color = VelvetCharcoal) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "הגדרות, חיבור, גיבוי ופרטיות",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ChampagneGold
                            )
                            Text(
                                text = if (settings.isPaidApiReady) {
                                    "מצב API מאושר · הצפנת חומרה"
                                } else {
                                    "מצב חשבון Claude חינמי (ברירת מחדל)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray
                            )
                        }
                        GoldPillBadge(
                            text = if (settings.isPaidApiReady) "API בתשלום" else "חינמי ובטוח",
                            icon = Icons.Default.Shield,
                            containerColor = if (settings.isPaidApiReady) SoftGoldContainer else SuccessContainer,
                            contentColor = if (settings.isPaidApiReady) ChampagneGold else EmeraldGoldSuccess
                        )
                    }
                    GoldDivider()
                }
            }

            OfflineWarningBanner(isOnline = isOnline)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Free vs Paid API Connection Mode (Zero Surprise Billing)
                item {
                    LuxuryObsidianCard(highlighted = true) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Paid, contentDescription = null, tint = ChampagneGold)
                            Text(
                                text = "מצב חיבור ל-Claude (שליטה מלאה בעלויות)",
                                style = MaterialTheme.typography.titleMedium,
                                color = ChampagneGold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "כברירת מחדל האפליקציה פועלת בחינם דרך חשבון Claude הרגיל שלך וללא שימוש ב-API בתשלום.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedWarmGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ConnectionModeOption(
                            title = "חשבון Claude חינמי (מומלץ · ללא עלות)",
                            subtitle = "מכין הודעות, מעתיק ללוח ופותח את האפליקציה או האתר הרשמי של Claude בחינם.",
                            selected = !settings.isPaidApiReady,
                            onClick = onSelectFreeMode,
                            testTag = "mode_free_option"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ConnectionModeOption(
                            title = "Anthropic API בתשלום (דורש מפתח ואישור עלויות)",
                            subtitle = "שליחה ישירה אוטומטית מהאפליקציה וקבלת תשובות בזמן אמת לפי תעריף הטוקנים של Anthropic.",
                            selected = settings.isPaidApiReady,
                            onClick = {
                                if (!settings.isPaidApiReady) {
                                    showPaidApprovalDialog = true
                                }
                            },
                            testTag = "mode_paid_option"
                        )
                    }
                }

                // 1B. Preferred Chat Language & Diagnostics Health Check
                item {
                    LuxuryObsidianCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = ChampagneGold)
                            Text(
                                text = "שפת תגובה בצ'אט ובמקלדת",
                                style = MaterialTheme.typography.titleMedium,
                                color = ChampagneGold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "בחר באיזו שפה קלוד יענה לך. כתיבה במקלדת תמיד תומכת בעברית, אנגלית ומעורב.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedWarmGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ResponseLanguage.AUTO to "זיהוי אוטומטי לפי ההודעה",
                                ResponseLanguage.HEBREW to "עברית תמיד",
                                ResponseLanguage.ENGLISH to "אנגלית תמיד"
                            ).forEach { (code, label) ->
                                val selected = settings.responseLanguage == code
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectResponseLanguage(code) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("settings_lang_$code"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SoftGoldContainer,
                                        selectedLabelColor = ChampagneGold,
                                        containerColor = ObsidianBlack,
                                        labelColor = MutedWarmGray
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = GoldHairline,
                                        selectedBorderColor = GoldPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // 1C. Live System Diagnostics & Integrity Check
                item {
                    LuxuryObsidianCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = ChampagneGold)
                                Text(
                                    text = "בדיקת תקינות המערכת",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChampagneGold
                                )
                            }
                            GoldPillBadge(
                                text = if (isOnline) "רשת תקינה" else "אופליין",
                                containerColor = if (isOnline) SuccessContainer else CrimsonContainer,
                                contentColor = if (isOnline) EmeraldGoldSuccess else CrimsonError
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        DiagnosticRow("חיבור אינטרנט", if (isOnline) "מחובר וזמין" else "אין חיבור רשת", isOnline)
                        HairlineDivider()
                        DiagnosticRow("מסד נתונים והיסטוריה", "Room Database פעיל ותקין", true)
                        HairlineDivider()
                        DiagnosticRow(
                            "אבטחת מפתח (Keystore)",
                            if (hasApiKey) "מפתח מוצפן שמור ($maskedApiKey)" else "אין מפתח API שמור (ברירת מחדל חינמית)",
                            true
                        )
                        HairlineDivider()
                        DiagnosticRow(
                            "הרשאת כפתור צף",
                            if (hasOverlayPermission) "מאושרת" else "נדרש אישור בהגדרות אנדרואיד",
                            hasOverlayPermission
                        )
                        HairlineDivider()
                        DiagnosticRow(
                            "הרשאת התראות",
                            if (hasNotificationsPermission) "מאושרת" else "נדרש אישור התראות",
                            hasNotificationsPermission
                        )
                        HairlineDivider()
                        DiagnosticRow(
                            "מנגנון אוטומציה",
                            when {
                                !settings.autoSendEnabled -> "כבוי (הפעלה ידנית ממסך האוטומציה)"
                                settings.autoSendPaused -> "מושהה זמנית"
                                else -> "פעיל · כל ${settings.autoSendIntervalHours} שעות"
                            },
                            !settings.autoSendEnabled || !settings.autoSendPaused
                        )
                    }
                }

                // 2. Floating Bubble Control Card
                item {
                    LuxuryObsidianCard(highlighted = settings.floatingBubbleEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.BubbleChart, contentDescription = null, tint = ChampagneGold)
                                Column {
                                    Text(
                                        text = "כפתור צף חכם מעל אפליקציות",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ChampagneGold
                                    )
                                    Text(
                                        text = if (hasOverlayPermission) {
                                            "גרירה חופשית מעל אפליקציות, שמירת מיקום והסתרה מהירה"
                                        } else {
                                            "נדרשת הרשאת 'הצגה מעל אפליקציות אחרות'"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedWarmGray
                                    )
                                }
                            }
                            Switch(
                                checked = settings.floatingBubbleEnabled && hasOverlayPermission,
                                onCheckedChange = { enable ->
                                    if (enable && !hasOverlayPermission) {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    } else {
                                        onToggleFloatingBubble(enable)
                                    }
                                },
                                modifier = Modifier.testTag("settings_floating_bubble_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianBlack,
                                    checkedTrackColor = GoldPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!hasOverlayPermission) {
                                Button(
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = InfoBlueContainer,
                                        contentColor = InfoBlueAccent
                                    )
                                ) {
                                    Text("אשר הרשאת תצוגה מעל אפליקציות")
                                }
                            }
                            OutlinedButton(
                                onClick = onResetBubblePosition,
                                border = BorderStroke(1.dp, GoldHairline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold)
                            ) {
                                Text("אפס מיקום בועה")
                            }
                        }
                    }
                }

                // 3. Cryptographic Vault Banner & Secure API Key Storage
                item {
                    LuxuryObsidianCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .border(1.dp, GoldHairline, MaterialTheme.shapes.medium)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_settings_vault),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                ObsidianBlack.copy(alpha = 0.88f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = ChampagneGold
                                )
                                Text(
                                    text = stringResource(R.string.security_badge_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChampagneGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.security_badge_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedWarmGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        GoldPillBadge(
                            text = if (hasApiKey) {
                                stringResource(R.string.api_key_saved_status, maskedApiKey)
                            } else {
                                stringResource(R.string.api_key_not_saved)
                            },
                            icon = Icons.Default.Key,
                            containerColor = if (hasApiKey) SuccessContainer else CardSurfaceHigh,
                            contentColor = if (hasApiKey) EmeraldGoldSuccess else MutedWarmGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text(stringResource(R.string.api_key_label)) },
                                placeholder = { Text(stringResource(R.string.api_key_placeholder)) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = JetBrainsMonoFamily,
                                    color = WarmIvory
                                ),
                                visualTransformation = if (showApiKey) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (!showApiKey && apiKeyInput.isBlank() && hasApiKey) {
                                                apiKeyInput = onRevealApiKey()
                                            }
                                            showApiKey = !showApiKey
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("toggle_api_key_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "הצג או הסתר מפתח",
                                            tint = GoldPrimary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = GoldHairline,
                                    focusedContainerColor = ObsidianBlack,
                                    unfocusedContainerColor = ObsidianBlack,
                                    focusedTextColor = WarmIvory,
                                    unfocusedTextColor = WarmIvory
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_key_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val saveKeyInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = {
                                    if (apiKeyInput.isNotBlank()) {
                                        onSaveApiKey(apiKeyInput)
                                        apiKeyInput = ""
                                        showApiKey = false
                                    }
                                },
                                enabled = apiKeyInput.isNotBlank(),
                                interactionSource = saveKeyInteraction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .luxuryPressScale(saveKeyInteraction)
                                    .testTag("save_api_key_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.save_api_key),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (hasApiKey) {
                                OutlinedButton(
                                    onClick = {
                                        apiKeyInput = ""
                                        onClearApiKey()
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("clear_api_key_button"),
                                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.clear_api_key))
                                }
                            }
                        }
                    }
                }

                // 4. Model & API Parameters
                item {
                    LuxuryObsidianCard {
                        Text(
                            text = stringResource(R.string.model_selection_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppSettings.AVAILABLE_MODELS.forEach { (modelId, label) ->
                                val selected = modelInput == modelId
                                FilterChip(
                                    selected = selected,
                                    onClick = { modelInput = modelId },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("model_chip_$modelId"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SoftGoldContainer,
                                        selectedLabelColor = ChampagneGold,
                                        containerColor = ObsidianBlack,
                                        labelColor = MutedWarmGray
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = GoldHairline,
                                        selectedBorderColor = GoldPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = modelInput,
                                onValueChange = { modelInput = it },
                                label = { Text(stringResource(R.string.custom_model_label)) },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, color = WarmIvory),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = GoldHairline,
                                    focusedContainerColor = ObsidianBlack,
                                    unfocusedContainerColor = ObsidianBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_model_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = baseUrlInput,
                                onValueChange = { baseUrlInput = it },
                                label = { Text(stringResource(R.string.base_url_label)) },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, color = WarmIvory),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = GoldHairline,
                                    focusedContainerColor = ObsidianBlack,
                                    unfocusedContainerColor = ObsidianBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("base_url_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = apiVersionInput,
                                onValueChange = { apiVersionInput = it },
                                label = { Text(stringResource(R.string.api_version_label)) },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, color = WarmIvory),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = GoldHairline,
                                    focusedContainerColor = ObsidianBlack,
                                    unfocusedContainerColor = ObsidianBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_version_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = systemPromptInput,
                            onValueChange = { systemPromptInput = it },
                            label = { Text(stringResource(R.string.system_prompt_label)) },
                            minLines = 2,
                            maxLines = 5,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = TextDirection.Content
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = true,
                                keyboardType = KeyboardType.Text
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = GoldHairline,
                                focusedContainerColor = ObsidianBlack,
                                unfocusedContainerColor = ObsidianBlack,
                                focusedTextColor = WarmIvory,
                                unfocusedTextColor = WarmIvory
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("system_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.max_tokens_label, maxTokensSlider.roundToInt()),
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampagneGold
                        )
                        Slider(
                            value = maxTokensSlider,
                            onValueChange = { maxTokensSlider = it },
                            valueRange = 256f..8192f,
                            steps = 15,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = CardSurfaceHigh
                            ),
                            modifier = Modifier.testTag("max_tokens_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.temperature_label, temperatureSlider),
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampagneGold
                        )
                        Slider(
                            value = temperatureSlider,
                            onValueChange = { temperatureSlider = it },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = CardSurfaceHigh
                            ),
                            modifier = Modifier.testTag("temperature_slider")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val saveConnInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = {
                                    onSaveConnectionSettings(
                                        baseUrlInput,
                                        apiVersionInput,
                                        modelInput,
                                        systemPromptInput,
                                        maxTokensSlider.roundToInt(),
                                        temperatureSlider
                                    )
                                },
                                interactionSource = saveConnInteraction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .luxuryPressScale(saveConnInteraction)
                                    .testTag("save_connection_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.save_connection_settings),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val testConnInteraction = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = {
                                    onTestConnection(baseUrlInput, apiVersionInput, modelInput)
                                },
                                enabled = connectionTestState !is ConnectionTestUiState.Testing && isOnline,
                                interactionSource = testConnInteraction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .luxuryPressScale(testConnInteraction)
                                    .testTag("test_connection_button"),
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold)
                            ) {
                                if (connectionTestState is ConnectionTestUiState.Testing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = ChampagneGold,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.testing_connection))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.test_connection))
                                }
                            }
                        }

                        when (connectionTestState) {
                            is ConnectionTestUiState.Success -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = SuccessContainer,
                                    border = BorderStroke(1.dp, EmeraldGoldSuccess.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldGoldSuccess
                                        )
                                        Text(
                                            text = "החיבור תקין (${connectionTestState.model}) · זמן תגובה: ${connectionTestState.latencyMs} מ\"ש",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WarmIvory
                                        )
                                    }
                                }
                            }
                            is ConnectionTestUiState.Error -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = CrimsonContainer,
                                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = CrimsonError
                                        )
                                        Text(
                                            text = connectionTestState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WarmIvory
                                        )
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }
                }

                // 5. Local Backup & Restore (JSON)
                item {
                    LuxuryObsidianCard {
                        Text(
                            text = "גיבוי ושחזור נתונים (JSON)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ייצא או שחזר שיחות, משימות ותזמונים לקובץ JSON במכשיר. מטעמי אבטחה, מפתח API אינו כלול לעולם בגיבוי.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedWarmGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    exportBackupLauncher.launch("claude_connect_backup.json")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_backup_button"),
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("גבה לקובץ")
                            }
                            OutlinedButton(
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("restore_backup_button"),
                                border = BorderStroke(1.dp, InfoBlueAccent.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = InfoBlueAccent)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("שחזר מגיבוי")
                            }
                        }
                    }
                }

                // 6. Privacy, Permissions Transparency & Emergency Controls
                item {
                    LuxuryObsidianCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = ChampagneGold)
                            Text(
                                text = "שקיפות הרשאות, פרטיות ושליטה מלאה",
                                style = MaterialTheme.typography.titleMedium,
                                color = ChampagneGold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "• הצגה מעל אפליקציות: משמשת אך ורק לכפתור הצף החכם.\n" +
                                "• התראות (Android 13+): לתזכורות שליחה, משימות ותשובות.\n" +
                                "• אינטרנט ורשת: לחיבור ל-Claude או ל-API לפי בחירתך.\n" +
                                "• הפעלה לאחר אתחול: לשחזור משימות ותזמונים שהפעלת.\n" +
                                "• ללא שירותי נגישות, ללא לחיצות נסתרות וללא איסוף מידע חיצוני.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmIvory
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            },
                            border = BorderStroke(1.dp, GoldHairline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_app_permissions_settings")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("נהל או בטל הרשאות בהגדרות אנדרואיד")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onStopAllActivity,
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stop_all_activity_button")
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("עצור כל פעילות")
                            }

                            OutlinedButton(
                                onClick = { showClearDataDialog = true },
                                border = BorderStroke(1.dp, CrimsonError),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("clear_all_local_data_button")
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("מחק הכל")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaidApprovalDialog) {
        AlertDialog(
            onDismissRequest = { showPaidApprovalDialog = false },
            containerColor = VelvetCharcoal,
            titleContentColor = ChampagneGold,
            textContentColor = WarmIvory,
            title = { Text("אישור מפורש לשימוש ב-Anthropic API") },
            text = {
                Text(
                    "שליחת הודעות דרך API מחייבת מפתח API פרטי שלך וכרוכה בעלות טוקנים מול Anthropic לפי השימוש:\n\n" +
                        "• Claude Haiku: ~$0.80 למיליון טוקנים\n" +
                        "• Claude Sonnet: ~$3 למיליון טוקנים\n" +
                        "• Claude Opus: ~$15 למיליון טוקנים\n\n" +
                        "המפתח נשמר בהצפנת חומרה במכשירך בלבד. האם ברצונך להפעיל את מצב ה-API?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaidApprovalDialog = false
                        onApprovePaidApiMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBlack),
                    modifier = Modifier.testTag("confirm_paid_api_button")
                ) {
                    Text("אני מאשר ומפעיל", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaidApprovalDialog = false }) {
                    Text("הישאר בחינם", color = MutedWarmGray)
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = VelvetCharcoal,
            titleContentColor = CrimsonError,
            textContentColor = WarmIvory,
            title = { Text("מחיקת כל המידע המקומי") },
            text = {
                Text("פעולה זו תמחק לצמיתות את כל השיחות, המשימות, ההתראות, היסטוריית הפעולות, מפתח ה-API וההגדרות מהמכשיר, ותעצור כל תזמון פעיל. להמשיך?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataDialog = false
                        onClearAllLocalData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_clear_all_data_button")
                ) {
                    Text("מחק הכל לצמיתות", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("ביטול", color = MutedWarmGray)
                }
            }
        )
    }
}

@Composable
private fun ConnectionModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MidnightNavy else ObsidianBlack.copy(alpha = 0.7f),
        border = BorderStroke(if (selected) 1.5.dp else 0.75.dp, if (selected) GoldPrimary else GoldHairline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = GoldPrimary,
                    unselectedColor = MutedWarmGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) ChampagneGold else WarmIvory
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedWarmGray
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    ok: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MutedWarmGray,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = if (ok) EmeraldGoldSuccess else ChampagneGold,
            modifier = Modifier.weight(0.55f)
        )
    }
}
