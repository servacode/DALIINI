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
import com.servacode.directory.core.designsystem.DateTimeField
import com.servacode.directory.core.model.DamascusTime

@Composable
fun DutyScreen(viewModel: DutyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var start by remember { mutableStateOf<Long?>(null) }
    var end by remember { mutableStateOf<Long?>(null) }
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
                        viewModel.startNow(end ?: return@Button)
                    },
                ) {
                    Text("بدء الآن حتى وقت النهاية")
                }
                DateTimeField("وقت البداية", start, { start = it })
                DateTimeField("وقت النهاية", end, { end = it })
                Button(
                    onClick = {
                        viewModel.schedule(start ?: return@Button, end ?: return@Button)
                    },
                ) { Text("جدولة") }
                value.shifts.forEach { shift ->
                    Column(Modifier.fillMaxWidth()) {
                        Text(DamascusTime.period(shift.startsAtEpochMillis, shift.endsAtEpochMillis))
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
