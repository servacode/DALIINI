package com.servacode.directory.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.servacode.directory.designsystem.generated.DirectoryTokens

@Composable
fun DirectoryPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = DirectoryTokens.SpacingLg.dp,
            vertical = DirectoryTokens.SpacingMd.dp,
        ),
        colors = ButtonDefaults.buttonColors(),
    ) {
        Text(text)
    }
}
