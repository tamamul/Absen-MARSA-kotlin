@file:OptIn(ExperimentalMaterial3Api::class)

package com.marsa.absen.ui.screen.galeri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.marsa.absen.data.remote.PhotoUrl
import com.marsa.absen.domain.model.GaleriItem
import com.marsa.absen.domain.model.uid
import com.marsa.absen.util.cleanTime
import com.marsa.absen.util.toHhmm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID = Locale.forLanguageTag("id-ID")

@Composable
fun GaleriScreen(
    refreshKey: Int = 0,
    vm: GaleriViewModel = hiltViewModel()
) {
    val state = vm.state
    val today = LocalDate.now()

    var showKeluar by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var viewerId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.onEnter() }
    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) vm.refresh()
    }

    val viewerItem = viewerId?.let { id -> state.items.firstOrNull { it.uid == id } }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GaleriHeader(
                    date = state.date,
                    today = today,
                    count = if (state.loading) null else state.items.size,
                    showKeluar = showKeluar,
                    error = state.error.takeIf { state.items.isNotEmpty() },
                    onPrev = { vm.setDate(state.date.minusDays(1)) },
                    onNext = { vm.setDate(state.date.plusDays(1)) },
                    onPick = { showPicker = true },
                    onMode = { showKeluar = it }
                )
            }

            when {
                state.loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                state.items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.error != null) {
                            Text(
                                text = state.error,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = vm::refresh) { Text("Coba lagi") }
                        } else {
                            Text(
                                text = "Belum ada yang absen pada tanggal ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> items(items = state.items, key = { it.uid }) { item ->
                    GaleriCard(
                        item = item,
                        showKeluar = showKeluar,
                        onClick = { viewerId = item.uid }
                    )
                }
            }
        }
    }

    if (showPicker) {
        DateDialog(
            current = state.date,
            today = today,
            onPick = vm::setDate,
            onDismiss = { showPicker = false }
        )
    }

    if (viewerItem != null) {
        PhotoViewer(
            item = viewerItem,
            initialKeluar = showKeluar,
            onClose = { viewerId = null }
        )
    }
}

@Composable
private fun GaleriHeader(
    date: LocalDate,
    today: LocalDate,
    count: Int?,
    showKeluar: Boolean,
    error: String?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit,
    onMode: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Galeri Kehadiran", style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Hari sebelumnya")
            }
            TextButton(onClick = onPick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            IconButton(onClick = onNext, enabled = date.isBefore(today)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Hari berikutnya")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showKeluar,
                onClick = { onMode(false) },
                label = { Text("Foto masuk") }
            )
            FilterChip(
                selected = showKeluar,
                onClick = { onMode(true) },
                label = { Text("Foto pulang") }
            )
        }

        if (count != null) {
            Text(
                text = "$count pegawai hadir" + if (date == today) " hari ini" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun GaleriCard(item: GaleriItem, showKeluar: Boolean, onClick: () -> Unit) {
    val jam = if (showKeluar) item.jamKeluar else item.jamMasuk
    val belumPulang = showKeluar && item.jamKeluar.cleanTime() == null
    val url = if (showKeluar) PhotoUrl.keluar(item.fotoKeluar) else PhotoUrl.masuk(item.fotoMasuk)

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (url != null && !belumPulang) {
                    AsyncImage(
                        model = url,
                        contentDescription = item.nama,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = if (belumPulang) "Belum pulang" else "Tidak ada foto",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
         
            }
          
        }
    }
}

@Composable
private fun DateDialog(
    current: LocalDate,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { ms ->
                    onPick(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun PhotoViewer(item: GaleriItem, initialKeluar: Boolean, onClose: () -> Unit) {
    var keluar by remember { mutableStateOf(initialKeluar) }
    val url = if (keluar) PhotoUrl.keluar(item.fotoKeluar) else PhotoUrl.masuk(item.fotoMasuk)
    val belumPulang = keluar && item.jamKeluar.cleanTime() == null

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                    Column {
                        Text(
                            text = item.nama ?: "-",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = item.jabatan.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (url != null && !belumPulang) {
                        AsyncImage(
                            model = url,
                            contentDescription = item.nama,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = if (belumPulang) "Belum absen pulang" else "Tidak ada foto",
                            color = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    FilterChip(
                        selected = !keluar,
                        onClick = { keluar = false },
                        label = { Text("Foto masuk") }
                    )
                    FilterChip(
                        selected = keluar,
                        onClick = { keluar = true },
                        label = { Text("Foto pulang") }
                    )
                }
            }
        }
    }
}
