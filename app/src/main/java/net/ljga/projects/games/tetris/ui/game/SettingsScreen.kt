package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.ljga.projects.games.tetris.R

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
                        imageVector = Icons.Default.ArrowBack,
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
                val languages = listOf(
                    "system" to stringResource(R.string.lang_system),
                    "en" to stringResource(R.string.lang_en),
                    "es" to stringResource(R.string.lang_es)
                )

                Column(Modifier.selectableGroup()) {
                    languages.forEach { (code, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (languageCode == code),
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.preferenceDataStore.setLanguageCode(code)
                                            // Trigger activity recreation or configuration change elsewhere if needed
                                        }
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (languageCode == code),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color.Cyan, unselectedColor = Color.Gray)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                modifier = Modifier.padding(start = 16.dp)
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
