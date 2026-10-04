@file:OptIn(ExperimentalMaterial3Api::class)

package com.marsa.absen.ui.screen.riwayat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.marsa.absen.data.remote.PhotoUrl
import com.marsa.absen.domain.model.PiketRiwayatItem
import com.marsa.absen.ui.screen.home.HomeViewModel
import com.marsa.absen.util.cleanDate
import com.marsa.absen.util.cleanTime
import com.marsa.absen.util.formatDurasi
import com.marsa.absen.util.toHhmm
import com.marsa.absen.util.toShortDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_ID = Locale.forLanguageTag("id-ID")

/** Tab Riwayat: judul + pilihan Reguler/Piket (hanya jika pegawai punya piket). */
@Composable
fun RiwayatTab(
    refreshKey: Int,
    home: HomeViewModel = hiltViewModel(),
    piketVm: PiketRiwayatViewModel = hiltViewModel()
) {
    var piketMode by rememberSaveable { mutableStateOf(false) }
    val hasPiket = !home.piket?.jadwal.isNullOrEmpty() || piketVm.state.items.isNotEmpty()

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Riwayat Absen", style = MaterialTheme.typography.headlineMedium)
            if (hasPiket) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !piketMode,
                        onClick = { piketMode = false },
                        label = { Text("Reguler") }
                    )
                    FilterChip(
                        selected = piketMode,
                        onClick = { piketMode = true },
                        label = { Text("Piket") }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (hasPiket && piketMode) {
                PiketRiwayatScreen(refreshKey = refreshKey, vm = piketVm)
            } else {
                RiwayatScreen(refreshKey = refreshKey)
            }
        }
    }
}

/** Tanggal mulai shift (bukan tanggal absen), agar shift malam tidak terpecah dua hari. */
private fun PiketRiwayatItem.shiftDate(): LocalDate? =
    (tanggalJadwal.cleanDate() ?: tanggalMasuk.cleanDate())?.take(10)
        ?.let { s -> runCatching { LocalDate.parse(s) }.getOrNull() }

private val PiketRiwayatItem.key: String
    get() = id ?: (tanggalMasuk.orEmpty() + jamMasuk.orEmpty())

@Composable
fun PiketRiwayatScreen(
    refreshKey: Int,
    vm: PiketRiwayatViewModel
) {
    val state = vm.state
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) vm.refresh()
    }

    val month = YearMonth.parse(monthKey)
    val monthItems = remember(state.items, monthKey) {
        state.items
            .filter { item -> item.shiftDate()?.let { YearMonth.from(it) == month } == true }
            .sortedWith(
                compareByDescending<PiketRiwayatItem> { it.shiftDate() }
                    .thenByDescending { it.jamMasuk.orEmpty() }
            )
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null && state.items.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Button(onClick = vm::refresh) { Text("Coba lagi") }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.error != null) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                MonthNav(
                    month = month,
                    canNext = month.isBefore(YearMonth.now()),
                    onPrev = { monthKey = month.minusMonths(1).toString() },
                    onNext = { monthKey = month.plusMonths(1).toString() }
                )

                PiketSummary(monthItems)

                if (monthItems.isEmpty()) {
                    Text(
                        text = "Belum ada piket di bulan ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    monthItems.forEach { item ->
                        PiketItemCard(
                            item = item,
                            expanded = openKey == item.key,
                            onToggle = { openKey = if (openKey == item.key) null else item.key }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthNav(month: YearMonth, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_ID)),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext, enabled = canNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
        }
    }
}

@Composable
private fun PiketSummary(items: List<PiketRiwayatItem>) {
    val total = items.size
    val terlambat = items.count { (it.menitTerlambat ?: 0) > 0 }
    val lupa = items.count { it.status == "lupa_pulang" }
    val menit = items.sumOf { it.durasiMenit ?: 0 }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ringkasan piket bulan ini", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatItem("Piket", total.toString(), Modifier.weight(1f))
                StatItem("Terlambat", terlambat.toString(), Modifier.weight(1f))
                StatItem("Belum pulang", lupa.toString(), Modifier.weight(1f))
                StatItem("Total jam", formatDurasi(menit), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PiketItemCard(item: PiketRiwayatItem, expanded: Boolean, onToggle: () -> Unit) {
    val late = item.menitTerlambat ?: 0
    val selesai = item.jamKeluar.cleanTime() != null
    val tanggal = item.shiftDate()
        ?.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", LOCALE_ID)) ?: "-"
    val jadwal = if (item.waktuMulai.cleanTime() != null && item.waktuSelesai.cleanTime() != null) {
        "${item.waktuMulai.toHhmm()}–${item.waktuSelesai.toHhmm()}"
    } else null

    Card(
        onClick = onToggle,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tanggal, style = MaterialTheme.typography.titleMedium)
                    val sub = listOfNotNull(item.namaLokasi, jadwal).joinToString(" • ")
                    if (sub.isNotEmpty()) {
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Tutup detail" else "Buka detail"
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TimeBlock("Masuk", item.jamMasuk.toHhmm(), item.tanggalMasuk.toShortDate())
                TimeBlock(
                    "Pulang",
                    item.jamKeluar.toHhmm(),
                    if (selesai) item.tanggalKeluar.toShortDate() else null
                )
                if (item.durasiMenit != null) {
                    TimeBlock("Durasi", formatDurasi(item.durasiMenit), null)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (late > 0) {
                    StatusBadge(
                        "Terlambat $late mnt",
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                when (item.status) {
                    "berjalan" -> StatusBadge(
                        "Sedang berjalan",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    "lupa_pulang" -> StatusBadge(
                        "Belum absen pulang",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    else -> if (late == 0) StatusBadge(
                        "Tepat waktu",
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PiketPhoto(
                        label = "Foto masuk",
                        url = PhotoUrl.piketMasuk(item.fotoMasuk),
                        modifier = Modifier.weight(1f)
                    )
                    PiketPhoto(
                        label = "Foto pulang",
                        url = if (selesai) PhotoUrl.piketKeluar(item.fotoKeluar) else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeBlock(label: String, value: String, date: String?) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (date != null) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, container: Color, content: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PiketPhoto(label: String, url: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Tidak ada foto",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
