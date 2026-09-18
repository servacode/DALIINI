package com.servacode.directory.feature.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun MyFacilitiesScreen(
    onAdd: () -> Unit,
    onManage: (String) -> Unit,
    onDuty: (String) -> Unit,
    viewModel: MyFacilitiesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("منشآتي", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = onAdd) { Text("إضافة منشأة") }
        }
        when (val value = state) {
            MyFacilitiesUiState.Loading -> CircularProgressIndicator()
            is MyFacilitiesUiState.Error -> {
                Text("تعذر تحميل المنشآت")
                Text(value.message)
                OutlinedButton(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
            }
            is MyFacilitiesUiState.Content -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(value.items, key = { it.summary.id }) { owned ->
                    val item = owned.summary
                    Column(Modifier.fillMaxWidth()) {
                        Text(item.nameAr, style = MaterialTheme.typography.titleLarge)
                        Text("${item.category.nameAr} • ${item.province.nameAr}")
                        Text("الحالة: ${item.status}")
                        item.requiredAction?.let { Text("الإجراء المطلوب: $it") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onManage(item.id) }) { Text("إدارة") }
                            if (owned.supportsDuty) {
                                OutlinedButton(onClick = { onDuty(item.id) }) { Text("المناوبة") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageFacilityScreen(
    onEdit: (String) -> Unit,
    onDuty: (String) -> Unit,
    viewModel: ManageFacilityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var managerId by remember { mutableStateOf("") }
    var closureStart by remember { mutableStateOf("") }
    var closureEnd by remember { mutableStateOf("") }
    var closureReason by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val value = state) {
            ManageFacilityUiState.Loading -> CircularProgressIndicator()
            is ManageFacilityUiState.Error -> {
                Text("تعذر تحميل إدارة المنشأة")
                Text(value.message)
                OutlinedButton(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
            }
            is ManageFacilityUiState.Content -> {
                Text(value.facility.summary.nameAr, style = MaterialTheme.typography.headlineLarge)
                Text("الحالة: ${value.facility.summary.status}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onEdit(value.facility.summary.id) }) { Text("تعديل البيانات") }
                    if (value.supportsDuty) {
                        OutlinedButton(onClick = { onDuty(value.facility.summary.id) }) { Text("المناوبة") }
                    }
                }
                value.message?.let { Text(it) }
                Text("الإغلاقات المؤقتة", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = closureStart,
                    onValueChange = { closureStart = it.filter(Char::isDigit) },
                    label = { Text("بداية الإغلاق epoch millis") },
                )
                OutlinedTextField(
                    value = closureEnd,
                    onValueChange = { closureEnd = it.filter(Char::isDigit) },
                    label = { Text("نهاية الإغلاق epoch millis") },
                )
                OutlinedTextField(
                    value = closureReason,
                    onValueChange = { closureReason = it },
                    label = { Text("سبب الإغلاق - اختياري") },
                )
                Button(
                    onClick = {
                        val start = closureStart.toLongOrNull() ?: return@Button
                        val end = closureEnd.toLongOrNull() ?: return@Button
                        viewModel.createTemporaryClosure(
                            start,
                            end,
                            closureReason.trim().ifBlank { null },
                        )
                    },
                ) { Text("إضافة إغلاق مؤقت") }
                value.closures.forEach { closure ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(closure.reason ?: "إغلاق مؤقت")
                        OutlinedButton(
                            onClick = { viewModel.deleteTemporaryClosure(closure.id) },
                        ) { Text("حذف") }
                    }
                }
                Text("المدراء", style = MaterialTheme.typography.titleMedium)
                value.members.forEach { member ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${member.name} • ${member.role}")
                        if (member.role.name == "MANAGER") {
                            OutlinedButton(onClick = { viewModel.removeMember(member.userId) }) {
                                Text("إزالة")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = managerId,
                    onValueChange = { managerId = it },
                    label = { Text("معرف حساب المدير") },
                )
                Button(
                    onClick = {
                        viewModel.addManager(managerId)
                        managerId = ""
                    },
                ) { Text("إضافة مدير") }
            }
        }
    }
}
