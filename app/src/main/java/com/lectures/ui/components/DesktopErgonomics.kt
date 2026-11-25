package com.lectures.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion

/**
 * Adds desktop mouse/trackpad hover pointer cursor and subtle surface highlight
 * without interfering with mobile touch behavior.
 * Keeps the pointer cursor active when [showHandCursor] is true even if [enabled]
 * suppresses the hover tint (e.g. for already-selected cards).
 */
fun Modifier.desktopHoverable(
    enabled: Boolean = true,
    hoverContainerColor: Color? = null,
    defaultContainerColor: Color = Color.Transparent,
    pointerIcon: PointerIcon = PointerIcon.Hand,
    showHandCursor: Boolean = true
): Modifier = composed {
    val withCursor = if (showHandCursor) this.pointerHoverIcon(pointerIcon) else this
    if (!enabled) return@composed withCursor

    val reducedMotion = rememberPrefersReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val targetHover = hoverContainerColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val animatedBg by animateColorAsState(
        targetValue = if (isHovered) targetHover else defaultContainerColor,
        animationSpec = MotionSpec.stateTween(MotionSpec.HOVER_MS, reducedMotion),
        label = "desktop_hover_bg"
    )
    withCursor
        .hoverable(interactionSource = interactionSource)
        .drawWithContent {
            drawContent()
            if (animatedBg.alpha > 0f) {
                drawRect(animatedBg)
            }
        }
}

/**
 * Marks a composable with a hand cursor on mouse/trackpad hover.
 */
fun Modifier.desktopHandCursor(enabled: Boolean = true): Modifier =
    if (enabled) this.pointerHoverIcon(PointerIcon.Hand) else this

/**
 * Intercepts Escape key presses on desktop/Chromebook hardware keyboards to dismiss active inspectors or sheets.
 */
fun Modifier.onEscapeKey(onEscape: () -> Unit): Modifier =
    this.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            onEscape()
            true
        } else {
            false
        }
    }

/**
 * Adaptive modal container:
 * - Compact phones (< 600dp): native ModalBottomSheet with drag handle
 * - Tablets, Chromebooks & Desktops (>= 600dp): spacious centered Dialog with smooth scale+fade entrance
 *   and Escape-to-dismiss, eliminating awkward full-width bottom drag sheets on large monitors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheetOrDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetTestTag: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    desktopMaxWidth: Dp = 640.dp,
    desktopMaxHeight: Dp = 760.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()

    if (!windowInfo.preferDesktopDialogs) {
        val baseMod = if (sheetTestTag != null) modifier.testTag(sheetTestTag) else modifier
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = containerColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = baseMod
        ) {
            content()
        }
    } else {
        val dialogMaxW = if (windowInfo.isWideWorkspace) desktopMaxWidth else minOf(desktopMaxWidth, 560.dp)
        val dialogMaxH = minOf(desktopMaxHeight, (windowInfo.heightDp - 48).coerceAtLeast(360).dp)
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            var entered by remember { mutableStateOf(reducedMotion) }
            LaunchedEffect(Unit) { entered = true }
            val dialogScale by animateFloatAsState(
                targetValue = if (entered) 1f else 0.95f,
                animationSpec = if (reducedMotion) snap() else MotionSpec.standardSpring(),
                label = "desktop_dialog_scale"
            )
            val dialogAlpha by animateFloatAsState(
                targetValue = if (entered) 1f else 0f,
                animationSpec = if (reducedMotion) snap() else tween(MotionSpec.DIALOG_ENTER_MS),
                label = "desktop_dialog_alpha"
            )

            val surfaceMod = Modifier
                .graphicsLayer {
                    scaleX = dialogScale
                    scaleY = dialogScale
                    alpha = dialogAlpha
                }
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(min = 400.dp, max = dialogMaxW)
                .heightIn(max = dialogMaxH)
                .fillMaxWidth()
                .onEscapeKey(onDismissRequest)
                .let { if (sheetTestTag != null) it.testTag(sheetTestTag) else it }
                .then(modifier)

            Surface(
                modifier = surfaceMod,
                shape = RoundedCornerShape(24.dp),
                color = containerColor,
                tonalElevation = 4.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        content = content
                    )
                }
            }
        }
    }
}
