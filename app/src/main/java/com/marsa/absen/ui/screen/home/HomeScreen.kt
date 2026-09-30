package com.marsa.absen.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marsa.absen.domain.model.PegawaiProfil
import com.marsa.absen.util.greeting
import com.marsa.absen.util.initials
import com.marsa.absen.util.lateMinutes
import com.marsa.absen.util.toHhmm
import com.marsa.absen.util.todayLabel
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.marsa.absen.domain.model.AbsenMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onAbsen: (AbsenMode, PegawaiProfil) -> Unit,
    refreshKey: Int = 0,
    vm: HomeViewModel = hiltViewModel()
) {
    val state = vm.state
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(refreshKey) {
        if (refreshKey > 0) vm.refresh()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val profil = state.profil
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                profil == null -> ErrorState(
                    message = state.error ?: "Data tidak tersedia.",
                    onRetry = vm::refresh,
                    onLogout = { confirmLogout = true }
                )

                else -> HomeContent(
                    state = state,
                    profil = profil,
                    onLogoutClick = { confirmLogout = true },
                        onAbsenClick = {
                        val mode = if (state.status == AbsenStatus.BELUM_MASUK) {
                            AbsenMode.MASUK
                        } else {
                            AbsenMode.KELUAR
                        }
                        onAbsen(mode, profil)
                    }
                )
            }
        }
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
private fun HomeContent(
    state: HomeUiState,
    profil: PegawaiProfil,
    onLogoutClick: () -> Unit,
    onAbsenClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header(nama = profil.nama, onLogoutClick = onLogoutClick)

        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        ProfileCard(profil)
        StatusCard(state, profil)

        val (label, enabled) = when (state.status) {
            AbsenStatus.BELUM_MASUK -> "Absen Masuk" to true
            AbsenStatus.SUDAH_MASUK -> "Absen Pulang" to true
            AbsenStatus.SELESAI -> "Absensi hari ini selesai" to false
        }
        Button(
            onClick = onAbsenClick,
            enabled = enabled,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Filled.Fingerprint, contentDescription = null)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        ScheduleCard(profil)
    }
}

@Composable
private fun Header(nama: String?, onLogoutClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todayLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${greeting()},",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = nama ?: "-",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = onLogoutClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Keluar"
            )
        }
    }
}

@Composable
private fun ProfileCard(profil: PegawaiProfil) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profil.nama.initials(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column {
                Text(profil.nama ?: "-", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = listOfNotNull(profil.nip, profil.jabatan).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState, profil: PegawaiProfil) {
    val abs = state.absenHariIni
    val container = when (state.status) {
        AbsenStatus.BELUM_MASUK -> MaterialTheme.colorScheme.secondaryContainer
        AbsenStatus.SUDAH_MASUK -> MaterialTheme.colorScheme.primaryContainer
        AbsenStatus.SELESAI -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onContainer = when (state.status) {
        AbsenStatus.BELUM_MASUK -> MaterialTheme.colorScheme.onSecondaryContainer
        AbsenStatus.SUDAH_MASUK -> MaterialTheme.colorScheme.onPrimaryContainer
        AbsenStatus.SELESAI -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val title = when (state.status) {
        AbsenStatus.BELUM_MASUK -> "Belum absen masuk"
        AbsenStatus.SUDAH_MASUK -> "Sudah absen masuk"
        AbsenStatus.SELESAI -> "Absensi hari ini selesai"
    }
    val late = if (abs?.jamMasuk != null) lateMinutes(abs.jamMasuk, profil.jamMasuk) else 0L

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Status hari ini", style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TimeTile(label = "Masuk", value = abs?.jamMasuk.toHhmm())
                TimeTile(label = "Pulang", value = abs?.jamKeluar.toHhmm())
            }

            if (late > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Terlambat $late menit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeTile(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScheduleCard(profil: PegawaiProfil) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Lokasi dan jadwal", style = MaterialTheme.typography.titleMedium)
            InfoRow("Lokasi", profil.namaLokasi ?: "-")
            InfoRow("Radius absen", profil.radius?.let { "$it m" } ?: "-")
            InfoRow("Jam masuk", profil.jamMasuk.toHhmm())
            InfoRow(
                "Jam pulang",
                "${profil.jamPulang.toHhmm()} – ${profil.jamPulangMaksimal.toHhmm()}"
            )
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
private fun ErrorState(message: String, onRetry: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) { Text("Coba lagi") }
        TextButton(onClick = onLogout) { Text("Keluar") }
    }
}
