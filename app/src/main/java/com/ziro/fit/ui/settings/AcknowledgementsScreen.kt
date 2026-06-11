package com.ziro.fit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziro.fit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcknowledgementsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acknowledgements") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Data & Acknowledgements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = StrongTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ziro Fit is built upon amazing open source projects and data providers.",
                style = MaterialTheme.typography.bodyMedium,
                color = StrongTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Open Source section
            Text(
                text = "Open Source",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = StrongBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            AcknowledgementLink(title = "Jetpack Compose", url = "https://developer.android.com/jetpack/compose")
            Spacer(modifier = Modifier.height(4.dp))
            AcknowledgementLink(title = "Hilt", url = "https://dagger.dev/hilt/")
            Spacer(modifier = Modifier.height(4.dp))
            AcknowledgementLink(title = "Retrofit", url = "https://square.github.io/retrofit/")
            Spacer(modifier = Modifier.height(4.dp))
            AcknowledgementLink(title = "Coil", url = "https://coil-kt.github.io/coil/")
            Spacer(modifier = Modifier.height(4.dp))
            AcknowledgementLink(title = "ZXing", url = "https://github.com/zxing/zxing")
            Spacer(modifier = Modifier.height(4.dp))
            AcknowledgementLink(title = "Supabase", url = "https://supabase.com")

            Spacer(modifier = Modifier.height(24.dp))

            // Data Attribution
            Text(
                text = "Data Attribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = StrongBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Exercise database and illustrations provided by ExerciseDB and WGER Open Source.",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary
            )
        }
    }
}

@Composable
private fun AcknowledgementLink(
    title: String,
    url: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StrongSecondaryBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = StrongTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open",
                tint = StrongTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
