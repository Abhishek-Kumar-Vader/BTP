package com.example.odiakeyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odiakeyboard.model.KeyData
import com.example.odiakeyboard.model.KeyType
import com.example.odiakeyboard.model.ShiftState

@Composable
fun KeyboardKey(
    key: KeyData,
    shiftState: ShiftState,
    onKeyPress: (KeyData) -> Unit,
    onBackspaceLongPressStart: () -> Unit = {},
    onBackspaceLongPressEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isCharacterOrSpace = key.keyType == KeyType.CHARACTER || key.keyType == KeyType.SPACE
    val isShiftEnabled = shiftState != ShiftState.OFF

    val backgroundColor = when {
        key.keyType == KeyType.SPACE -> colors.surface
        key.keyType == KeyType.SHIFT && isShiftEnabled -> colors.primary
        !isCharacterOrSpace -> colors.surfaceVariant
        else -> colors.surface
    }
    
    val textColor = when {
        key.keyType == KeyType.SHIFT && isShiftEnabled -> colors.onPrimary
        else -> colors.onSurface
    }

    val label = if (key.keyType == KeyType.CHARACTER && isShiftEnabled) {
        key.shiftedLabel
    } else {
        key.displayLabel
    }

    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .shadow(
                elevation = if (isPressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(6.dp),
                ambientColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .semantics { contentDescription = key.contentDescription }
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        if (key.keyType == KeyType.BACKSPACE) {
                            onBackspaceLongPressStart()
                        }
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                            if (key.keyType == KeyType.BACKSPACE) {
                                onBackspaceLongPressEnd()
                            }
                        }
                    },
                    onTap = { onKeyPress(key) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val fontSize = when (key.keyType) {
            KeyType.SPACE -> 12.sp
            KeyType.CHARACTER -> if (key.displayLabel.length > 1) 14.sp else 22.sp
            else -> 14.sp
        }
        
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (!isCharacterOrSpace) FontWeight.Medium else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}