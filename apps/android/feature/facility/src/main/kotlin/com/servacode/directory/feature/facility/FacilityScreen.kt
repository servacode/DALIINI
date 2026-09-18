package com.servacode.directory.feature.facility

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.model.AvailabilityLabel

@Composable
fun FacilityScreen(
    onMap: () -> Unit,
    onDirections: (Double, Double) -> Unit,
    onRatings: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: FacilityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val value = state) {
        FacilityUiState.Loading -> CircularProgressIndicator(Modifier.padding(24.dp))
        is FacilityUiState.Error -> Column(Modifier.padding(24.dp)) {
            Text("تعذر تحميل المنشأة")
            Text(value.message)
        }
        is FacilityUiState.Content -> Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(value.value.summary.nameAr, style = MaterialTheme.typography.headlineLarge)
            Text(value.value.summary.category.nameAr)
            Text(AvailabilityLabel.of(value.value.summary), style = MaterialTheme.typography.titleMedium)
            if (value.value.imageUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(value.value.imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = value.value.summary.nameAr,
                            modifier = Modifier.fillParentMaxWidth(0.82f).height(220.dp),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
            value.value.addressAr?.let { Text(it) }
            value.value.phone?.let { Text(it) }
            value.value.descriptionAr?.let { Text(it) }
            if (value.value.hours.isNotEmpty()) {
                Text("ساعات العمل", style = MaterialTheme.typography.titleMedium)
                value.value.hours.forEach { hour ->
                    Text("${weekdayName(hour.weekday)}: ${hour.opensAt.take(5)} – ${hour.closesAt.take(5)}")
                }
            }
            if (value.value.specialties.isNotEmpty()) Text(value.value.specialties.joinToString(" • "))
            if (value.value.services.isNotEmpty()) Text(value.value.services.joinToString(" • "))
            Text("التقييم ${value.value.summary.ratingAverage ?: "—"} (${value.value.summary.ratingCount})")
            if (value.signedIn) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { stars ->
                        TextButton(onClick = { viewModel.rate(stars) }) {
                            Text(if ((value.myRating ?: 0) >= stars) "★" else "☆")
                        }
                    }
                }
                value.ratingMessage?.let { Text(it) }
            } else {
                TextButton(onClick = onSignIn) { Text("سجّل الدخول لتقييم المنشأة") }
            }
            if (value.stale) Text("غير متصل — بعض البيانات وحالة مفتوح/مناوب قد تكون قديمة")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMap,
                    enabled = value.value.latitude != null && value.value.longitude != null,
                ) { Text("عرض على الخريطة") }
                OutlinedButton(
                    onClick = {
                        val latitude = value.value.latitude ?: return@OutlinedButton
                        val longitude = value.value.longitude ?: return@OutlinedButton
                        onDirections(latitude, longitude)
                    },
                    enabled = value.value.latitude != null && value.value.longitude != null,
                ) { Text("الاتجاهات") }
                OutlinedButton(onClick = onRatings) { Text("تقييماتي") }
            }
        }
    }
}

/** 0 is Monday, matching the backend's weekday numbering. */
private fun weekdayName(weekday: Int): String = when (weekday) {
    0 -> "الاثنين"
    1 -> "الثلاثاء"
    2 -> "الأربعاء"
    3 -> "الخميس"
    4 -> "الجمعة"
    5 -> "السبت"
    6 -> "الأحد"
    else -> "—"
}
