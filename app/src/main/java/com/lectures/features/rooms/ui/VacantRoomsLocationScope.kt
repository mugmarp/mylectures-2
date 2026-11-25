package com.lectures.features.rooms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.lectures.features.rooms.Campus
import com.lectures.features.timetable.domain.TimetableMatcher
import com.lectures.ui.LocalExtendedColors
import java.util.Locale

data class ProgrammeLocationDefault(
    val campus: String,
    val buildingCode: String?,
    val buildingName: String
)

fun programmeDefaults(programme: String?): ProgrammeLocationDefault {
    if (programme.isNullOrBlank()) {
        return ProgrammeLocationDefault(
            campus = Campus.KIHUMURO.displayName,
            buildingCode = null,
            buildingName = "All Buildings"
        )
    }
    val parsed = TimetableMatcher.parseGroupParts(programme)
    val head = programme.trim().split(Regex("[\\s\\-_]+")).firstOrNull()?.uppercase(Locale.ROOT) ?: ""
    val alias = mapOf(
        "MLC" to "MLS", "BNC" to "BNS", "BSPC" to "BSP", "PEM" to "PEEM",
        "CIV" to "CVE", "BAF" to "BSAF"
    )
    val code = parsed?.code ?: alias[head] ?: head

    return when (code) {
        "BCS", "BIT", "BSE" -> ProgrammeLocationDefault(
            campus = Campus.KIHUMURO.displayName,
            buildingCode = "FCI",
            buildingName = "FCI Building"
        )
        "BME", "EEE", "PEEM", "CVE", "MIE" -> ProgrammeLocationDefault(
            campus = Campus.KIHUMURO.displayName,
            buildingCode = "FAST",
            buildingName = "FAST Building"
        )
        "BS", "DLT" -> ProgrammeLocationDefault(
            campus = Campus.TOWN.displayName,
            buildingCode = "SCI",
            buildingName = "Science Block"
        )
        "PHA", "PHS", "MBR", "BNS", "MLS", "BSP", "DCM", "DEM", "DCAM" -> ProgrammeLocationDefault(
            campus = Campus.TOWN.displayName,
            buildingCode = "PHA",
            buildingName = "Pharmacy / Health Complex"
        )
        "BBA", "BSAF", "ECO", "BPSM", "BSAL", "BGWH", "BPCD" -> ProgrammeLocationDefault(
            campus = Campus.TOWN.displayName,
            buildingCode = "IMS",
            buildingName = "IMS / Business Building"
        )
        else -> ProgrammeLocationDefault(
            campus = Campus.KIHUMURO.displayName,
            buildingCode = null,
            buildingName = "All Buildings"
        )
    }
}

fun campusForProgramme(programme: String?): String? = programmeDefaults(programme).campus

@Composable
fun LocationDropdownSelector(
    label: String,
    value: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    badgeText: String? = null,
    menuAlignmentEnd: Boolean = false,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalExtendedColors.current
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = ext.surfaceCard
    val textPrimary = ext.textPrimary
    val textMuted = ext.textMuted
    val borderSubtle = ext.borderSubtle

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = surfaceColor,
            border = BorderStroke(1.2.dp, if (expanded) accentColor else borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { onExpandedChange(!expanded) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = label.uppercase(Locale.ROOT),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMuted,
                            letterSpacing = 0.5.sp
                        )
                        if (badgeText != null) {
                            Box(
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $label" else "Expand $label",
                    tint = textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            offset = if (menuAlignmentEnd) DpOffset(x = (-60).dp, y = 4.dp) else DpOffset(x = 0.dp, y = 4.dp),
            modifier = Modifier
                .widthIn(min = 220.dp, max = 285.dp)
                .heightIn(max = 280.dp)
                .background(surfaceColor, RoundedCornerShape(14.dp)),
            properties = PopupProperties(focusable = true)
        ) {
            menuContent()
        }
    }
}
