@file:OptIn(ExperimentalMaterial3Api::class)

package com.marsa.absen.ui.screen.profil

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsa.absen.data.local.AppSettings
import com.marsa.absen.ui.SettingsViewModel
import com.marsa.absen.ui.components.Avatar
import com.marsa.absen.ui.screen.home.HomeViewModel
import com.marsa.absen.ui.theme.ThemeMode

@Composable
fun ProfilScreen(
    onLogout: () -> Unit,
    home: HomeViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val profil = home.state.profil
    val stored by settingsVm.settings.collectAsStateWithLifecycle()
    val settings = stored ?: AppSettings()
    val context = LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "-"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium)

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Avatar(nama = profil?.nama, foto = profil?.foto, size = 96.dp)
                if (profil != null) {
                    Text(profil.nama ?: "-", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = listOfNotNull(profil.nip, profil.jabatan).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Data profil belum dimuat", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = home::refresh) { Text("Muat ulang") }
                }
            }
        }

        if (profil != null) {
            SectionCard(title = "Data pegawai") {
                InfoRow("No. handphone", profil.noHandphone ?: "-")
                InfoRow("Lokasi kerja", profil.namaLokasi ?: "-")
                InfoRow(
                    "Mode absen",
                    profil.modeAbsen?.replaceFirstChar { it.uppercase() } ?: "-"
                )
            }
        }

        SectionCard(title = "Tampilan") {
            Text("Tema", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.SYSTEM to "Sistem",
                    ThemeMode.LIGHT to "Terang",
                    ThemeMode.DARK to "Gelap"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { settingsVm.setThemeMode(mode) },
                        label = { Text(label) }
                    )
                }
            }
            SwitchRow(
                title = "Warna dinamis",
                subtitle = if (dynamicSupported) "Ikuti warna wallpaper HP"
                else "Butuh Android 12 ke atas",
                checked = settings.dynamicColor && dynamicSupported,
                enabled = dynamicSupported,
                onChange = settingsVm::setDynamicColor
            )
            SwitchRow(
                title = "Mode AMOLED",
                subtitle = "Latar hitam pekat saat mode gelap",
                checked = settings.amoled,
                onChange = settingsVm::setAmoled
            )
        }

        SectionCard(title = "Tentang") {
            InfoRow("Aplikasi", "Absen Marsa")
            InfoRow("Versi", version)
        }

        OutlinedButton(
            onClick = { confirmLogout = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Keluar dari akun") }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Keluar dari akun?") },
            text = { Text("Kamu perlu login lagi untuk melanjutkan.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text("Keluar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
