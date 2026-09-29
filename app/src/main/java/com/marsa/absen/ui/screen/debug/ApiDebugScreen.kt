package com.marsa.absen.ui.screen.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Layar sementara untuk menguji API dan mengambil contoh JSON. Dihapus sebelum rilis. */
@Composable
fun ApiDebugScreen(
    modifier: Modifier = Modifier,
    vm: ApiDebugViewModel = hiltViewModel()
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val tokenSaved by vm.tokenSaved.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Uji API Absen Marsa", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (tokenSaved) "Token: tersimpan ✓" else "Token: belum ada",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Login (username)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { vm.login(login.trim(), password) },
            enabled = !vm.loading && login.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (vm.loading) "Memuat…" else "Login") }

        OutlinedButton(onClick = vm::profile, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) {
            Text("GET auth/profile")
        }
        OutlinedButton(onClick = vm::pegawaiProfil, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) {
            Text("GET pegawai/profil")
        }
        OutlinedButton(onClick = vm::absenHariIni, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) {
            Text("GET absen/hari-ini")
        }
        OutlinedButton(onClick = vm::riwayat, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) {
            Text("GET absen/riwayat")
        }
        OutlinedButton(onClick = vm::logout, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) {
            Text("POST auth/logout")
        }
        OutlinedButton(onClick = vm::clearToken, modifier = Modifier.fillMaxWidth()) {
            Text("Hapus token lokal")
        }

        Button(
            onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("api", vm.output))
                Toast.makeText(context, "Hasil tersalin", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Salin hasil") }

        SelectionContainer {
            Text(
                text = vm.output,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}
