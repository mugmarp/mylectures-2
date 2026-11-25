package com.lectures.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
private fun PickerDesktopDialogWrapper(
    onDismiss: () -> Unit,
    desktopMinWidth: Dp = 360.dp,
    desktopMaxWidth: Dp = 480.dp,
    content: @Composable (Modifier) -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val reducedMotion = rememberPrefersReducedMotion()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        var entered by remember { mutableStateOf(reducedMotion || !windowInfo.preferDesktopDialogs) }
        LaunchedEffect(Unit) { entered = true }
        val dialogScale by animateFloatAsState(
            targetValue = if (entered) 1f else 0.95f,
            animationSpec = if (reducedMotion || !windowInfo.preferDesktopDialogs) snap() else MotionSpec.standardSpring(),
            label = "picker_dialog_scale"
        )
        val dialogAlpha by animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = if (reducedMotion || !windowInfo.preferDesktopDialogs) snap() else tween(MotionSpec.DIALOG_ENTER_MS),
            label = "picker_dialog_alpha"
        )
        val dialogModifier = Modifier
            .graphicsLayer {
                scaleX = dialogScale
                scaleY = dialogScale
                alpha = dialogAlpha
            }
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .widthIn(min = desktopMinWidth, max = desktopMaxWidth)
                        .heightIn(max = (windowInfo.heightDp - 48).coerceAtLeast(360).dp)
                } else {
                    Modifier.padding(horizontal = 4.dp)
                }
            )
            .onEscapeKey(onDismiss)

        content(dialogModifier)
    }
}

/**
 * Dedicated Time Picker Dialog featuring the Material 3 circular dial TimePicker,
 * with interactive clock face, rotating clock hand for hours and minutes, and AM/PM toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DedicatedTimePickerDialog(
    initialTime: String = "14:00",
    title: String = "Select Time",
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialParts = initialTime.split(":")
    val initialHour = initialParts.getOrNull(0)?.toIntOrNull() ?: 14
    val initialMinute = initialParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )

    val ext = LocalExtendedColors.current

    PickerDesktopDialogWrapper(
        onDismiss = onDismiss,
        desktopMinWidth = 360.dp,
        desktopMaxWidth = 440.dp
    ) { dialogModifier ->
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = dialogModifier
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Circular Clock Dial (Material 3 TimePicker)
                TimePicker(
                    state = timePickerState
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor()
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val formatted = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                            onTimeSelected(formatted)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor()
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Calendar Date Picker Dialog tailored for Academic Tasks & Deadlines.
 * Features quick pick chips ("Today", "Tomorrow") at the top, and a full interactive monthly calendar grid as the main centerpiece.
 * Eliminates mandatory time picking friction and ensures students can select real calendar dates effortlessly.
 */
@Composable
fun DedicatedCalendarDatePickerDialog(
    initialDate: String = "Tomorrow",
    title: String = "Select Due Date",
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val todayCal = remember { Calendar.getInstance() }
    val tomorrowCal = remember {
        (Calendar.getInstance().clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    }

    // Determine initial active date
    val initialCal = remember(initialDate) {
        val cal = Calendar.getInstance()
        val trimmed = initialDate.split("•", "-").firstOrNull()?.trim() ?: initialDate.trim()
        when {
            trimmed.equals("today", ignoreCase = true) -> cal
            trimmed.equals("tomorrow", ignoreCase = true) -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal
            }
            else -> {
                try {
                    val patterns = listOf(
                        "EEE, MMM d, yyyy",
                        "MMM d, yyyy",
                        "EEE, MMM d",
                        "yyyy-MM-dd",
                        "dd/MM/yyyy"
                    )
                    val locales = listOf(Locale.US, Locale.getDefault()).distinct()
                    var parsed: java.util.Date? = null
                    outer@ for (pattern in patterns) {
                        for (locale in locales) {
                            try {
                                val sdf = SimpleDateFormat(pattern, locale).apply { isLenient = false }
                                val pos = java.text.ParsePosition(0)
                                val candidate = sdf.parse(trimmed, pos)
                                if (candidate != null && pos.errorIndex == -1 && pos.index == trimmed.length) {
                                    parsed = candidate
                                    break@outer
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    if (parsed != null) {
                        cal.time = parsed
                        if (cal.get(Calendar.YEAR) < 2000) {
                            cal.set(Calendar.YEAR, todayCal.get(Calendar.YEAR))
                        }
                    }
                } catch (_: Exception) {}
                cal
            }
        }
    }

    var viewYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var viewMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0..11
    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedDayOfMonth by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }

    // Helper to calculate whether selected is today or tomorrow
    val isSelectedToday = remember(selectedYear, selectedMonth, selectedDayOfMonth) {
        selectedYear == todayCal.get(Calendar.YEAR) &&
        selectedMonth == todayCal.get(Calendar.MONTH) &&
        selectedDayOfMonth == todayCal.get(Calendar.DAY_OF_MONTH)
    }

    val isSelectedTomorrow = remember(selectedYear, selectedMonth, selectedDayOfMonth) {
        selectedYear == tomorrowCal.get(Calendar.YEAR) &&
        selectedMonth == tomorrowCal.get(Calendar.MONTH) &&
        selectedDayOfMonth == tomorrowCal.get(Calendar.DAY_OF_MONTH)
    }

    // Format display string as explicit calendar date
    val currentDisplayString = remember(selectedYear, selectedMonth, selectedDayOfMonth) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
        }
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(c.time)
    }

    // Calendar month grid calculations
    val monthCal = remember(viewYear, viewMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthTitle = remember(monthCal) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
    }

    val daysInMonth = remember(monthCal) {
        monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // First day of week offset (Monday = 0, Sunday = 6)
    val firstDayOffset = remember(monthCal) {
        val dow = monthCal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday...
        (dow + 5) % 7
    }

    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val monthKey = viewYear * 12 + viewMonth

    PickerDesktopDialogWrapper(
        onDismiss = onDismiss,
        desktopMinWidth = 380.dp,
        desktopMaxWidth = 460.dp
    ) { dialogModifier ->
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = dialogModifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month & Year Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (viewMonth == 0) {
                                viewMonth = 11
                                viewYear -= 1
                            } else {
                                viewMonth -= 1
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }

                    AnimatedContent(
                        targetState = monthTitle,
                        transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
                        label = "picker_month_title"
                    ) { animatedTitle ->
                        Text(
                            text = animatedTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            if (viewMonth == 11) {
                                viewMonth = 0
                                viewYear += 1
                            } else {
                                viewMonth += 1
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Day of Week Labels (Each column gets equal weight(1f) for pixel-perfect vertical alignment)
                val dayHeaders = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dayHeaders.forEach { name ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calendar Days Grid (Centerpiece, using equal weight(1f) per column)
                val todayStartCal = remember {
                    (Calendar.getInstance().clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }

                AnimatedContent(
                    targetState = monthKey,
                    transitionSpec = {
                        MotionSpec.directionalSlideTransform(
                            forward = targetState > initialState,
                            reducedMotion = reducedMotion,
                            useFadeOnly = false
                        )
                    },
                    label = "picker_calendar_grid"
                ) { targetKey ->
                    val gridYear = targetKey / 12
                    val gridMonth = targetKey % 12
                    val gridMonthCal = remember(targetKey) {
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, gridYear)
                            set(Calendar.MONTH, gridMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    val gridDaysInMonth = remember(gridMonthCal) {
                        gridMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    }
                    val gridFirstDayOffset = remember(gridMonthCal) {
                        (gridMonthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        var dayCounter = 1
                        for (row in 0..5) {
                            if (dayCounter > gridDaysInMonth) break
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (col in 0..6) {
                                    val cellIndex = row * 7 + col
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (cellIndex >= gridFirstDayOffset && dayCounter <= gridDaysInMonth) {
                                            val currentDay = dayCounter
                                            val isCellSelected = (gridYear == selectedYear && gridMonth == selectedMonth && currentDay == selectedDayOfMonth)
                                            val isCellToday = (gridYear == todayCal.get(Calendar.YEAR) && gridMonth == todayCal.get(Calendar.MONTH) && currentDay == todayCal.get(Calendar.DAY_OF_MONTH))

                                            val cellCal = Calendar.getInstance().apply {
                                                set(gridYear, gridMonth, currentDay, 23, 59, 59)
                                            }
                                            val isPast = cellCal.before(todayStartCal)

                                            val cellBg by animateColorAsState(
                                                targetValue = when {
                                                    isCellSelected -> primaryColor
                                                    isCellToday -> primaryColor.copy(alpha = 0.14f)
                                                    else -> Color.Transparent
                                                },
                                                animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                                                label = "picker_day_bg"
                                            )
                                            val cellTextColor by animateColorAsState(
                                                targetValue = when {
                                                    isCellSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isCellToday -> primaryColor
                                                    isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                                                label = "picker_day_text"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .desktopHandCursor()
                                                    .clickable {
                                                        selectedYear = gridYear
                                                        selectedMonth = gridMonth
                                                        selectedDayOfMonth = currentDay
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(cellBg)
                                                        .then(
                                                            if (isCellToday && !isCellSelected) {
                                                                Modifier.border(1.5.dp, primaryColor, RoundedCornerShape(10.dp))
                                                            } else Modifier
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$currentDay",
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isCellSelected || isCellToday) FontWeight.Bold else FontWeight.Medium,
                                                        color = cellTextColor
                                                    )
                                                }
                                            }
                                            dayCounter++
                                        } else {
                                            Spacer(modifier = Modifier.height(48.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selection Preview Tile
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Due: $currentDisplayString",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onDateSelected(currentDisplayString)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Confirm Date", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Unified Schedule Picker Dialog that handles BOTH Day and Time atomically.
 * Prevents raw typing of "Tomorrow • 17:00" and ensures the time scheduler handles the whole flow.
 */
@Composable
fun DedicatedSchedulePickerDialog(
    initialSchedule: String = "Tomorrow • 17:00",
    title: String = "Set Due Schedule",
    onScheduleSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initialSchedule.split("•", "-").map { it.trim() }
    val parsedDay = parts.getOrNull(0)?.ifBlank { "Tomorrow" } ?: "Tomorrow"
    val parsedTime = parts.getOrNull(1)?.ifBlank { "17:00" } ?: "17:00"

    var selectedDay by remember { mutableStateOf(parsedDay) }
    var selectedTime by remember { mutableStateOf(parsedTime) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    val quickDays = listOf("Today", "Tomorrow", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val quickTimes = listOf("08:00", "10:00", "12:00", "14:00", "17:00", "20:00", "23:59")

    val ext = LocalExtendedColors.current

    PickerDesktopDialogWrapper(
        onDismiss = onDismiss,
        desktopMinWidth = 380.dp,
        desktopMaxWidth = 480.dp
    ) { dialogModifier ->
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = dialogModifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Unified Result Preview Tile (Clickable to open circular time picker)
                Surface(
                    onClick = { showTimeDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = primaryColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .desktopHandCursor()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SCHEDULED FOR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$selectedDay • $selectedTime",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { showTimeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .desktopHandCursor()
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Day Selection Strip
                Text(
                    text = "1. Choose Target Day",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickDays) { day ->
                        val isSelected = day.equals(selectedDay, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDay = day },
                            label = {
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.desktopHandCursor()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Times Strip
                Text(
                    text = "2. Quick Time Preset",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTimes) { t ->
                        val isSelected = t == selectedTime
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTime = t },
                            label = {
                                Text(
                                    text = t,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.desktopHandCursor()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onScheduleSelected("$selectedDay • $selectedTime")
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Apply Schedule ✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showTimeDialog) {
        DedicatedTimePickerDialog(
            initialTime = selectedTime,
            title = "Pick Time",
            onTimeSelected = { picked ->
                selectedTime = picked
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false }
        )
    }
}

/**
 * Dedicated Day & Date Picker Dialog.
 */
@Composable
fun DedicatedDayPickerDialog(
    selectedDay: String = "Monday",
    onDaySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val accentColor = MaterialTheme.colorScheme.primary
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()

    PickerDesktopDialogWrapper(
        onDismiss = onDismiss,
        desktopMinWidth = 360.dp,
        desktopMaxWidth = 440.dp
    ) { dialogModifier ->
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            modifier = dialogModifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Academic Day",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                daysOfWeek.forEach { day ->
                    val isSelected = day.equals(selectedDay, ignoreCase = true)
                    val rowBg by animateColorAsState(
                        targetValue = if (isSelected) accentColor.copy(alpha = 0.18f) else ext.surfaceBase,
                        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                        label = "day_picker_bg"
                    )
                    val rowBorder by animateColorAsState(
                        targetValue = if (isSelected) accentColor else ext.borderSubtle,
                        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                        label = "day_picker_border"
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = rowBg,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = rowBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .desktopHoverable(enabled = !isSelected)
                            .clickable {
                                onDaySelected(day)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

/**
 * Dedicated Course Picker Dialog with enrolled courses as default,
 * showing full course titles and code hints, plus search functionality.
 */
@Composable
fun DedicatedCoursePickerDialog(
    selectedCourse: String,
    availableCourses: List<Pair<String, String>>, // code to title
    onCourseSelected: (code: String, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, availableCourses) {
        if (searchQuery.isBlank()) availableCourses
        else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            availableCourses.filter { (code, title) ->
                code.lowercase(Locale.ROOT).contains(q) || title.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    PickerDesktopDialogWrapper(
        onDismiss = onDismiss,
        desktopMinWidth = 380.dp,
        desktopMaxWidth = 520.dp
    ) { dialogModifier ->
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            modifier = dialogModifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.School, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Choose Course Unit",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search course code or name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (filteredList.size == 1) {
                                val (code, title) = filteredList.first()
                                onCourseSelected(code, title)
                                onDismiss()
                            } else {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enrolled & Department Courses",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching courses found",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredList) { (code, title) ->
                            val isSelected = code.equals(selectedCourse, ignoreCase = true)
                            val rowBg by animateColorAsState(
                                targetValue = if (isSelected) accentColor.copy(alpha = 0.18f) else ext.surfaceBase,
                                animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                                label = "course_picker_bg"
                            )
                            val rowBorder by animateColorAsState(
                                targetValue = if (isSelected) accentColor else ext.borderSubtle,
                                animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                                label = "course_picker_border"
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = rowBg,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = rowBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .desktopHoverable(enabled = !isSelected)
                                    .clickable {
                                        onCourseSelected(code, title)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor else accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = code.take(3),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else accentColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = code,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (title.isNotBlank()) {
                                            Text(
                                                text = title,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                                color = if (isSelected) accentColor.copy(alpha = 0.85f) else ext.textMuted,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
