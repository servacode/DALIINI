package com.servacode.directory.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AccountScreen(
    onRatings: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val value = state) {
        AccountUiState.Loading -> CircularProgressIndicator(Modifier.padding(24.dp))
        AccountUiState.Error -> Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("تعذر تحميل الحساب")
            Button(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
        }
        is AccountUiState.Content -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("حسابي", style = MaterialTheme.typography.headlineLarge)
            Text(value.profile.name)
            Text(value.profile.phone)
            Button(onClick = onRatings) { Text("تقييماتي") }
        }
    }
}
