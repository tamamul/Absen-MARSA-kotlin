package com.marsa.absen.ui.screen.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.marsa.absen.domain.model.AbsenMode
import com.marsa.absen.domain.model.LocationCheck
import com.marsa.absen.domain.model.PegawaiProfil
import com.marsa.absen.util.greeting
import com.marsa.absen.util.initials
import com.marsa.absen.util.lateMinutes
import com.marsa.absen.util.toHhmm
import com.marsa.absen.util.todayLabel
import kotlinx.coroutines.delay
import java.time.LocalTime
import androidx.compose.foundation.layout.WindowInsets
import com.marsa.absen.ui.components.Avatar
import androidx.compose.material.icons.filled.Schedule
import com.marsa.absen.domain.model.PiketAktif
import com.marsa.absen.util.toDateTimeLabel

private val PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.CAMERA
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onAbsen: (AbsenMode, PegawaiProfil, LocationCheck.Inside?) -> Unit,
    refreshKey: Int = 0,
    vm: HomeViewModel = hiltViewModel()
) {
    val state = vm.state
    val location = vm.location
    val piket = vm.piket
    val context = LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    // Jam berjalan agar tombol pulang aktif otomatis saat waktunya tiba
    val now by produceState(LocalTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalTime.now()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.checkLocation() }

    LaunchedEffect(Unit) {
        if (!vm.hasLocationPermission()) permissionLauncher.launch(PERMISSIONS)
    }
    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) vm.refresh()
    }
    LifecycleResumeEffect(Unit) {
        vm.checkLocationIfStale()
        vm.refreshPiket()
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = {
                vm.refresh()
                vm.checkLocation()
            },
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
                    location = location,
                    piket = piket,
                    now = now,
                    onLogoutClick = { confirmLogout = true },
                    onAbsenClick = {
                        val mode = if (state.status == AbsenStatus.BELUM_MASUK) {
                            AbsenMode.MASUK
                        } else {
                            AbsenMode.KELUAR
                        }
                        onAbsen(mode, profil, location as? LocationCheck.Inside)
                    },
                    onLocationRefresh = vm::checkLocation,
                    onGrant = { permissionLauncher.launch(PERMISSIONS) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
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
    location: LocationCheck,
    piket: PiketAktif?,
    now: LocalTime,
    onLogoutClick: () -> Unit,
    onAbsenClick: () -> Unit,
    onLocationRefresh: () -> Unit,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit
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
        LocationCard(location, onLocationRefresh, onGrant, onOpenSettings)

        val (label, enabled) = when (state.status) {
            AbsenStatus.BELUM_MASUK -> "Absen Masuk" to true
            AbsenStatus.SUDAH_MASUK ->
                if (state.pulangDibuka(now)) {
                    "Absen Pulang" to true
                } else {
                    "Absen pulang mulai ${profil.jamPulang.toHhmm()}" to false
                }
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
        if (piket != null) PiketCard(piket)
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
            Avatar(nama = profil.nama, foto = profil.foto, size = 56.dp)
            
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
    val late = lateMinutes(abs?.jamMasuk, profil.jamMasuk)

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
private fun LocationCard(
    location: LocationCheck,
    onRefresh: () -> Unit,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val info: Pair<String, String?> = when (location) {
        LocationCheck.Idle -> "Lokasi" to "Belum diperiksa"
        LocationCheck.Checking -> "Memeriksa lokasi…" to null
        LocationCheck.NoPermission -> "Izin lokasi belum diberikan" to "Diperlukan agar bisa absen"
        LocationCheck.Unauthorized -> "Sesi berakhir" to null
        is LocationCheck.Inside -> "Di dalam area absen" to listOfNotNull(
            location.lokasi,
            location.jarak?.let { "±$it m" },
            location.radius?.let { "radius $it m" }
        ).joinToString(" • ")
        is LocationCheck.Problem ->
            (if (location.outside) "Di luar area absen" else "Lokasi bermasalah") to location.message
    }
    val (title, detail) = info

    val container = when (location) {
        is LocationCheck.Inside -> MaterialTheme.colorScheme.secondaryContainer
        is LocationCheck.Problem -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val icon = if (location is LocationCheck.Inside) Icons.Filled.LocationOn
    else Icons.Filled.LocationOff

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (!detail.isNullOrBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodyMedium)
                }
                if (location is LocationCheck.NoPermission) {
                    Row {
                        TextButton(onClick = onGrant) { Text("Izinkan") }
                        TextButton(onClick = onOpenSettings) { Text("Pengaturan") }
                    }
                }
            }
            when (location) {
                LocationCheck.Checking ->
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)

                LocationCheck.NoPermission -> Unit

                else -> IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Periksa ulang lokasi")
                }
            }
        }
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

@Composable
private fun PiketCard(piket: PiketAktif) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null)
                Text("Jadwal piket aktif", style = MaterialTheme.typography.titleMedium)
            }
            Text(piket.namaLokasi ?: "-", style = MaterialTheme.typography.headlineMedium)

            PiketLine("Mulai", piket.mulai.toDateTimeLabel())
            PiketLine("Selesai", piket.selesai.toDateTimeLabel())
            PiketLine("Batas absen pulang", piket.tutup.toDateTimeLabel())

            if (!piket.keterangan.isNullOrBlank()) {
                Text(piket.keterangan, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {},
                enabled = false,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Absen piket segera tersedia") }
        }
    }
}

@Composable
private fun PiketLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
