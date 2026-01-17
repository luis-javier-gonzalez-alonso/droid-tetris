package net.ljga.projects.games.tetris.ui.game

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.launch
import net.ljga.projects.games.tetris.R
import java.util.Locale

enum class Language(val locale: String, @StringRes val nameRes: Int) {
    SYSTEM("system", R.string.lang_system),
    ENGLISH("en", R.string.lang_en),
    SPANISH("es", R.string.lang_es)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val languageCode by viewModel.preferenceDataStore.languageCode.collectAsState(initial = "system")
    val isClassicMode by viewModel.preferenceDataStore.isClassicMode.collectAsState(initial = false)
    val touchSensitivity by viewModel.preferenceDataStore.touchSensitivity.collectAsState(initial = 2.0f)

    val coroutineScope = rememberCoroutineScope()

    val userPreferredLanguages = remember {
        val size = LocaleListCompat.getAdjustedDefault().size()
        (0 until size).map { LocaleListCompat.getAdjustedDefault()[it]?.language }.toSet()
    }

    val availableLanguages = remember {
        Language.entries.filter { lang ->
            lang.locale == "system" || Locale.forLanguageTag(lang.locale).language in userPreferredLanguages
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Language Section
            SettingsSection(title = stringResource(R.string.language_title)) {
                var expanded by remember { mutableStateOf(false) }
                val currentLanguage = Language.entries.firstOrNull { it.locale == languageCode } ?: Language.SYSTEM

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Cyan)
                    ) {
                        Text(
                            text = stringResource(currentLanguage.nameRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2C3E50))
                    ) {
                        availableLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(lang.nameRes), color = Color.White) },
                                onClick = {
                                    expanded = false
                                    coroutineScope.launch {
                                        viewModel.preferenceDataStore.setLanguageCode(lang.locale)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Game Mode Section
            SettingsSection(title = stringResource(R.string.game_mode_title)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            coroutineScope.launch {
                                viewModel.preferenceDataStore.setClassicMode(!isClassicMode)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.classic_mode_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Switch(
                        checked = isClassicMode,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                viewModel.preferenceDataStore.setClassicMode(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Cyan,
                            checkedTrackColor = Color(0xFF006064)
                        )
                    )
                }
            }

            // Controls Section
            SettingsSection(title = stringResource(R.string.controls_title)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sensitivity_label, touchSensitivity),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Slider(
                        value = touchSensitivity,
                        onValueChange = { newValue ->
                            coroutineScope.launch {
                                viewModel.preferenceDataStore.setTouchSensitivity(newValue)
                            }
                        },
                        valueRange = 1f..5f,
                        steps = 3, // 1->2->3->4->5
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Cyan,
                            activeTrackColor = Color.Cyan
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CA1AF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD700),
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
            content()
        }
    }
}
