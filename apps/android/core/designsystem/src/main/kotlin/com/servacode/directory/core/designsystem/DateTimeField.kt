package com.servacode.directory.core.designsystem

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.servacode.directory.core.model.DamascusTime
import java.time.LocalTime

/**
 * A moment an owner picks: a day, then a time, on Damascus clocks. What comes out is the
 * instant the API carries, converted by [DamascusTime] and nowhere else; nobody types a
 * timestamp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    epochMillis: Long?,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableStateOf(PickerStep.CLOSED) }
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val initial = epochMillis?.let(DamascusTime::localDateTime) ?: DamascusTime.now()

    OutlinedButton(onClick = { step = PickerStep.DATE }, modifier = modifier) {
        Text(
            if (epochMillis == null) {
                "$label: اختر التاريخ والوقت"
            } else {
                "$label: ${DamascusTime.format(epochMillis)}"
            },
        )
    }

    when (step) {
        PickerStep.DATE -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = DamascusTime.pickerMillis(initial.toLocalDate()),
            )
            DatePickerDialog(
                onDismissRequest = { step = PickerStep.CLOSED },
                confirmButton = {
                    TextButton(
                        enabled = state.selectedDateMillis != null,
                        onClick = {
                            pickedDay = state.selectedDateMillis
                            step = PickerStep.TIME
                        },
                    ) { Text("التالي") }
                },
                dismissButton = { TextButton(onClick = { step = PickerStep.CLOSED }) { Text("إلغاء") } },
            ) {
                DatePicker(state = state)
            }
        }
        PickerStep.TIME -> {
            val state = rememberTimePickerState(
                initialHour = initial.hour,
                initialMinute = initial.minute,
                is24Hour = true,
            )
            AlertDialog(
                onDismissRequest = { step = PickerStep.CLOSED },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pickedDay?.let { day ->
                                onValueChange(
                                    DamascusTime.toEpochMillis(
                                        DamascusTime.dateFromPicker(day),
                                        LocalTime.of(state.hour, state.minute),
                                    ),
                                )
                            }
                            step = PickerStep.CLOSED
                        },
                    ) { Text("تأكيد") }
                },
                dismissButton = { TextButton(onClick = { step = PickerStep.CLOSED }) { Text("إلغاء") } },
                text = { TimePicker(state = state) },
            )
        }
        PickerStep.CLOSED -> Unit
    }
}

private enum class PickerStep { CLOSED, DATE, TIME }
