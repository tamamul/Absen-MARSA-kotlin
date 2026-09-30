package com.marsa.absen.ui.screen.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.marsa.absen.domain.model.DayRecord
import com.marsa.absen.util.toHhmm
import com.marsa.absen.util.toLocalTimeOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID = Locale.forLanguageTag("id-ID")
private val WEEKDAYS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    refreshKey: Int = 0,
    vm: RiwayatViewModel = hiltViewModel()
) {
    val state = vm.state
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(LocalDate.now().toString()) }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) vm.refresh()
    }

    val month = YearMonth.parse(monthKey)
    val selected = selectedKey?.let { LocalDate.parse(it) }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null && state.records.isEmpty() -> Column(
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

            else -> {
                val monthRecords = state.records.values
                    .filter { YearMonth.from(it.date) == month }
                    .sortedByDescending { it.date }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Riwayat Absen", style = MaterialTheme.typography.headlineMedium)

                    if (state.error != null) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MonthHeader(
                                month = month,
                                canNext = month.isBefore(YearMonth.now()),
                                onPrev = {
                                    monthKey = month.minusMonths(1).toString()
                                    selectedKey = null
                                },
                                onNext = {
                                    monthKey = month.plusMonths(1).toString()
                                    selectedKey = null
                                }
                            )
                            MonthGrid(
                                month = month,
                                records = state.records,
                                selected = selected,
                                onSelect = { selectedKey = it.toString() }
                            )
                            Legend()
                        }
                    }

                    SummaryCard(monthRecords)

                    if (selected != null) {
                        DayDetail(date = selected, record = state.records[selected])
                    }

                    if (monthRecords.isNotEmpty()) {
                        Text("Daftar bulan ini", style = MaterialTheme.typography.titleMedium)
                        monthRecords.forEach { rec ->
                            DayRow(
                                rec = rec,
                                selected = rec.date == selected,
                                onClick = { selectedKey = rec.date.toString() }
                            )
                        }
                    } else {
                        Text(
                            text = "Belum ada data absen di bulan ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", ID)),
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
private fun MonthGrid(
    month: YearMonth,
    records: Map<LocalDate, DayRecord>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    val length = month.lengthOfMonth()
    val offset = month.atDay(1).dayOfWeek.value - 1 // Senin = 0
    val rows = (offset + length + 6) / 7
    val today = LocalDate.now()

    Column {
        Row {
            WEEKDAYS.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        for (r in 0 until rows) {
            Row {
                for (c in 0 until 7) {
                    val day = r * 7 + c - offset + 1
                    if (day in 1..length) {
                        val date = month.atDay(day)
                        DayCell(
                            day = day,
                            rec = records[date],
                            isSelected = date == selected,
                            isToday = date == today,
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    rec: DayRecord?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.medium
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(shape)
            .background(bg)
            .then(
                if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = day.toString(), style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.height(8.dp)
            ) {
                if (rec != null) {
                    Dot(
                        if (rec.lateMinutes > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    if (rec.lupaPulang) Dot(MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .padding(top = 2.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(MaterialTheme.colorScheme.primary, "Tepat waktu")
        LegendItem(MaterialTheme.colorScheme.error, "Terlambat")
        LegendItem(MaterialTheme.colorScheme.tertiary, "Belum pulang")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun SummaryCard(records: List<DayRecord>) {
    val hadir = records.size
    val terlambat = records.count { it.lateMinutes > 0 }
    val lupa = records.count { it.lupaPulang }
    val seconds = records.mapNotNull { it.item.jamMasuk.toLocalTimeOrNull()?.toSecondOfDay() }
    val avg = if (seconds.isEmpty()) "--:--"
    else LocalTime.ofSecondOfDay(seconds.average().toLong()).toString().take(5)

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
            Text("Ringkasan bulan ini", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Hadir", hadir.toString(), Modifier.weight(1f))
                Stat("Terlambat", terlambat.toString(), Modifier.weight(1f))
                Stat("Belum pulang", lupa.toString(), Modifier.weight(1f))
                Stat("Rata² masuk", avg, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
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
private fun DayDetail(date: LocalDate, record: DayRecord?) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID)),
                style = MaterialTheme.typography.titleMedium
            )

            if (record == null) {
                Text(
                    text = "Tidak ada data absen pada tanggal ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("Masuk", style = MaterialTheme.typography.labelLarge)
                    Text(
                        record.item.jamMasuk.toHhmm(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Column {
                    Text("Pulang", style = MaterialTheme.typography.labelLarge)
                    Text(
                        record.item.jamKeluar.toHhmm(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            if (record.lateMinutes > 0) {
                Text(
                    text = "Terlambat ${record.lateMinutes} menit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (record.lupaPulang) {
                Text(
                    text = "Belum absen pulang",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoTile(
                    label = "Foto masuk",
                    url = PhotoUrl.masuk(record.item.fotoMasuk),
                    modifier = Modifier.weight(1f)
                )
                PhotoTile(
                    label = "Foto pulang",
                    url = PhotoUrl.keluar(record.item.fotoKeluar),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PhotoTile(label: String, url: String?, modifier: Modifier = Modifier) {
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

@Composable
private fun DayRow(rec: DayRecord, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = rec.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", ID)),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${rec.item.jamMasuk.toHhmm()} – ${rec.item.jamKeluar.toHhmm()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                rec.lateMinutes > 0 -> Text(
                    text = "Terlambat ${rec.lateMinutes} mnt",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )

                rec.lupaPulang -> Text(
                    text = "Belum pulang",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )

                else -> Text(
                    text = "Tepat waktu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
