package com.example.indicoffline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

@Composable
fun LicensesScreen(onNavigateBack: () -> Unit, onNavigateToThirdParty: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Open Source Licenses",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "Uktam.ai is powered by the following open-source technologies, custom-quantized and optimized to run smoothly on standard smartphone hardware:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            item {
                LicenseCard(
                    title = "llama.cpp",
                    url = "https://github.com/ggerganov/llama.cpp",
                    license = "MIT License"
                )
                LicenseCard(
                    title = "Sherpa-ONNX",
                    url = "https://github.com/k2-fsa/sherpa-onnx",
                    license = "Apache License 2.0"
                )
                LicenseCard(
                    title = "AI4Bharat Indic Conformer",
                    url = "https://huggingface.co/ai4bharat/indic-conformer-600m-multilingual",
                    license = "MIT License"
                )
                LicenseCard(
                    title = "Sarvam Translate",
                    url = "https://huggingface.co/sarvamai/sarvam-translate",
                    license = "GPL-3.0 License"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Android & Third-Party Library Licenses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clickable { onNavigateToThirdParty() }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LicenseCard(title: String, url: String, license: String) {
    val uriHandler = LocalUriHandler.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .clickable { uriHandler.openUri(url) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = license,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ThirdPartyLicensesScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Third-Party Licenses",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        LibrariesContainer(
            modifier = Modifier.fillMaxSize()
        )
    }
}

