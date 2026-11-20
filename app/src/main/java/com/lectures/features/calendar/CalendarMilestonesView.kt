package com.lectures.features.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.components.desktopHoverable
import java.util.Locale

@Composable
internal fun AcademicMilestoneCard(
    milestone: AcademicMilestone,
    isLiveNow: Boolean,
    isSelectedDate: Boolean,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(outerPadding)
            .desktopHoverable()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isLiveNow -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                isSelectedDate -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            isLiveNow -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            isSelectedDate -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            else -> null
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (milestone.phase) {
                        AcademicPhase.EXAMINATIONS -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        AcademicPhase.MID_SEM_TESTS -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        AcademicPhase.REVISION -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                        AcademicPhase.TEACHING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        AcademicPhase.ORIENTATION -> Color(0xFF10B981).copy(alpha = 0.15f)
                        AcademicPhase.RECESS -> Color(0xFF6B7280).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = milestone.phase.badgeLabel.uppercase(Locale.ROOT),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (milestone.phase) {
                            AcademicPhase.EXAMINATIONS -> Color(0xFFEF4444)
                            AcademicPhase.MID_SEM_TESTS -> Color(0xFFF59E0B)
                            AcademicPhase.REVISION -> Color(0xFF8B5CF6)
                            AcademicPhase.TEACHING -> MaterialTheme.colorScheme.primary
                            AcademicPhase.ORIENTATION -> Color(0xFF10B981)
                            AcademicPhase.RECESS -> Color(0xFF6B7280)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (isLiveNow) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Ongoing Now",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isSelectedDate) {
                    Text(
                        text = "Selected Date",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = milestone.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = milestone.dateDisplay,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = milestone.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
