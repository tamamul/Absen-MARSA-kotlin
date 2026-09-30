package com.marsa.absen.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.marsa.absen.data.remote.PhotoUrl
import com.marsa.absen.util.initials

/** Foto profil bulat; jika belum ada, gagal dimuat, atau sedang memuat: tampil inisial. */
@Composable
fun Avatar(
    nama: String?,
    foto: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val url = PhotoUrl.profil(foto)
    val initials: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = nama.initials(),
                style = if (size >= 80.dp) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(size)
    ) {
        if (url == null) {
            initials()
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = nama,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { initials() },
                error = { initials() }
            )
        }
    }
}
