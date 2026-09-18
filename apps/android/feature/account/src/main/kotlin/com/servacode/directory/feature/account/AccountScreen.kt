package com.servacode.directory.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AccountScreen(
    onRatings: () -> Unit,
    onFacilities: () -> Unit,
    onAccountDeleted: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletion by viewModel.deletionState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(deletion) {
        if (deletion == DeletionUiState.Deleted) onAccountDeleted()
    }

    when (val value = state) {
        AccountUiState.Loading -> CircularProgressIndicator(Modifier.padding(24.dp))
        AccountUiState.SignedOut -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("حسابي", style = MaterialTheme.typography.headlineLarge)
            Text("سجّل الدخول لإدارة تقييماتك ومنشآتك.")
            Button(onClick = onSignIn) { Text("تسجيل الدخول") }
            OutlinedButton(onClick = onRegister) { Text("إنشاء حساب") }
        }
        is AccountUiState.Error -> Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("تعذر تحميل الحساب")
            Text(value.message)
            Button(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
        }
        is AccountUiState.Content -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("حسابي", style = MaterialTheme.typography.headlineLarge)
            Text(value.profile.name)
            Text(value.profile.phone)
            // No profile image: the contract has no upload operation for one (INT-017).
            if (value.provinces.isNotEmpty()) {
                Text("المحافظة", style = MaterialTheme.typography.titleMedium)
                value.provinces.forEach { province ->
                    TextButton(
                        onClick = { viewModel.changeProvince(province.id) },
                        enabled = province.id != value.profile.provinceId,
                    ) {
                        Text(if (province.id == value.profile.provinceId) "✓ ${province.nameAr}" else province.nameAr)
                    }
                }
            }
            value.message?.let { Text(it) }
            Button(onClick = onRatings) { Text("تقييماتي") }
            Button(onClick = onFacilities) { Text("منشآتي") }
            OutlinedButton(onClick = viewModel::logout) { Text("تسجيل الخروج") }
            OutlinedButton(
                onClick = { showDeleteConfirmation = true },
                enabled = deletion != DeletionUiState.Deleting,
            ) {
                Text("حذف الحساب")
            }
            (deletion as? DeletionUiState.Error)?.let { failure ->
                Text("تعذر طلب حذف الحساب. تحقق من ملكية المنشآت ثم أعد المحاولة.")
                Text(failure.message)
                TextButton(onClick = viewModel::clearDeletionError) { Text("إخفاء") }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("حذف الحساب نهائيًا؟") },
            text = {
                Text(
                    "سيتم إلغاء جلساتك وإزالة بيانات الحساب الشخصية. " +
                        "إذا كنت المالك الوحيد لمنشأة غير مغلقة، يجب نقل الملكية أو إغلاقها أولًا.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAccount()
                    },
                ) { Text("تأكيد الحذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("إلغاء") }
            },
        )
    }
}
