package com.ziro.fit.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ziro.fit.ui.theme.*
import com.ziro.fit.viewmodel.AppearanceViewModel

private data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String
)

private val LANGUAGES = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("fr", "French", "Fran\u00e7ais"),
    LanguageOption("es", "Spanish", "Espa\u00f1ol"),
    LanguageOption("de", "German", "Deutsch"),
    LanguageOption("it", "Italian", "Italiano"),
    LanguageOption("pt", "Portuguese", "Portugu\u00eas"),
    LanguageOption("nl", "Dutch", "Nederlands"),
    LanguageOption("pl", "Polish", "Polski"),
    LanguageOption("ru", "Russian", "\u0420\u0443\u0441\u0441\u043a\u0438\u0439"),
    LanguageOption("ja", "Japanese", "\u65e5\u672c\u8a9e"),
    LanguageOption("ko", "Korean", "\ud55c\uad6d\uc5b4"),
    LanguageOption("zh", "Chinese", "\u4e2d\u6587")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Language") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose your preferred language",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    LANGUAGES.forEachIndexed { index, lang ->
                        val isSelected = uiState.selectedLanguage == lang.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setLanguage(lang.code) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setLanguage(lang.code) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = StrongBlue,
                                    unselectedColor = StrongTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = lang.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = StrongTextPrimary
                                )
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StrongTextSecondary
                                )
                            }
                        }
                        if (index < LANGUAGES.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = StrongDivider
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Language selection changes the app interface text. Some content may remain in English.",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
