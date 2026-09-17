package com.servacode.directory.feature.duty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DutyScreen(viewModel: DutyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("إدارة المناوبة", style = MaterialTheme.typography.headlineLarge)
        when (val value = state) {
            DutyUiState.Loading -> CircularProgressIndicator()
            DutyUiState.Error -> {
                Text("تعذر تحميل المناوبات أو أن التصنيف لا يدعمها")
                OutlinedButton(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
            }
            is DutyUiState.Content -> {
                value.message?.let { Text(it) }
                Button(
                    onClick = {
                        val end = endText.toLongOrNull() ?: return@Button
                        viewModel.startNow(end)
                    },
                ) {
                    Text("بدء الآن حتى وقت النهاية")
                }
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it.filter(Char::isDigit) },
                    label = { Text("وقت البداية epoch millis") },
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it.filter(Char::isDigit) },
                    label = { Text("وقت النهاية epoch millis") },
                )
                Button(
                    onClick = {
                        val start = startText.toLongOrNull() ?: return@Button
                        val end = endText.toLongOrNull() ?: return@Button
                        viewModel.schedule(start, end)
                    },
                ) { Text("جدولة") }
                value.shifts.forEach { shift ->
                    Column(Modifier.fillMaxWidth()) {
                        Text("${shift.startsAtEpochMillis} → ${shift.endsAtEpochMillis}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.endEarly(shift) }) {
                                Text("إنهاء مبكر")
                            }
                            OutlinedButton(onClick = { viewModel.cancel(shift.id) }) {
                                Text("إلغاء")
                            }
                        }
                    }
                }
            }
        }
    }
}
