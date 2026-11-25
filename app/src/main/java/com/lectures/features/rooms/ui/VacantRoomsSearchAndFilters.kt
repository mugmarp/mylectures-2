package com.lectures.features.rooms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.features.rooms.Building
import com.lectures.features.rooms.Campus
import com.lectures.features.rooms.UniversityDirectory
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.rememberLecturesWindow

@Composable
internal fun VacantRoomsSearchAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCampus: String?,
    selectedBuilding: String?,
    defaultScope: ProgrammeLocationDefault,
    campusBuildings: List<Building>,
    campusDropdownExpanded: Boolean,
    onCampusDropdownExpandedChange: (Boolean) -> Unit,
    buildingDropdownExpanded: Boolean,
    onBuildingDropdownExpandedChange: (Boolean) -> Unit,
    onCampusSelected: (campus: String?, building: String?) -> Unit,
    onBuildingSelected: (building: String?, inferredCampus: String?) -> Unit,
    freeOnly: Boolean,
    onToggleFreeOnly: () -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    isDefaultResetDismissed: Boolean,
    onResetToDefaultScope: () -> Unit,
    isDark: Boolean,
    isNarrow: Boolean
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val ext = LocalExtendedColors.current
    val surfaceColor = ext.surfaceCard
    val textPrimary = ext.textPrimary
    val textMuted = ext.textMuted
    val borderSubtle = ext.borderSubtle
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Sleek Compact Pill-shaped Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search room (e.g. L1, LR2, Lab...)",
                            fontSize = 13.sp,
                            color = textMuted
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        textStyle = TextStyle(
                            fontSize = 13.5.sp,
                            color = textPrimary,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(48.dp).desktopHandCursor()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. Prominent Dual-Segment Location Card (Campus | Building)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = BorderStroke(
                1.dp,
                if (selectedCampus != null || selectedBuilding != null) accentColor.copy(alpha = 0.45f) else borderSubtle
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Segment: Campus Selector
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .desktopHoverable()
                        .clickable { onCampusDropdownExpandedChange(true) }
                        .padding(horizontal = if (isNarrow) 4.dp else 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 5.dp else 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isNarrow) 22.dp else 26.dp)
                                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(if (isNarrow) 13.dp else 15.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy((-1).dp)
                        ) {
                            Text(
                                text = "Campus",
                                fontSize = 9.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textMuted,
                                letterSpacing = 0.2.sp
                            )
                            val campusShort = selectedCampus?.let { cName ->
                                Campus.entries.firstOrNull { it.displayName == cName }?.shortName ?: cName
                            } ?: if (isNarrow) "All" else "All Campuses"
                            Text(
                                text = campusShort,
                                fontSize = 12.5.sp,
                                lineHeight = 15.sp,
                                fontWeight = if (selectedCampus != null) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (selectedCampus != null) accentColor else textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select campus",
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = campusDropdownExpanded,
                        onDismissRequest = { onCampusDropdownExpandedChange(false) },
                        modifier = Modifier
                            .widthIn(min = 230.dp, max = 290.dp)
                            .heightIn(max = 280.dp)
                            .background(surfaceColor, RoundedCornerShape(14.dp))
                    ) {
                        Campus.entries.forEach { campus ->
                            val isSelected = selectedCampus == campus.displayName
                            val isDefault = campus.displayName.equals(defaultScope.campus, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = campus.displayName,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isSelected) accentColor else textPrimary
                                            )
                                            Text(
                                                text = if (campus == Campus.KIHUMURO) "FAST, FCI · 3 Buildings" else "Medicine, Science, Business · 4 Buildings",
                                                fontSize = 10.5.sp,
                                                color = if (isSelected) accentColor.copy(alpha = 0.8f) else textMuted
                                            )
                                        }
                                        if (isDefault) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Default",
                                                    fontSize = 10.sp,
                                                    color = accentColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    val nextBuilding = if (campus.displayName.equals(defaultScope.campus, ignoreCase = true)) {
                                        defaultScope.buildingCode
                                    } else {
                                        UniversityDirectory.buildingsForCampus(campus.displayName).firstOrNull()?.code
                                    }
                                    onCampusSelected(campus.displayName, nextBuilding)
                                    onCampusDropdownExpandedChange(false)
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        val isAllCampusesSelected = selectedCampus == null
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "All Campuses",
                                    fontSize = 13.sp,
                                    fontWeight = if (isAllCampusesSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isAllCampusesSelected) accentColor else textPrimary
                                )
                            },
                            onClick = {
                                onCampusSelected(null, null)
                                onCampusDropdownExpandedChange(false)
                            }
                        )
                    }
                }

                VerticalDivider(modifier = Modifier.height(22.dp), color = borderSubtle)

                // Right Segment: Building Selector
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .desktopHoverable()
                        .clickable { onBuildingDropdownExpandedChange(true) }
                        .padding(horizontal = if (isNarrow) 4.dp else 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 5.dp else 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isNarrow) 22.dp else 26.dp)
                                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Business,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(if (isNarrow) 13.dp else 15.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy((-1).dp)
                        ) {
                            Text(
                                text = "Building",
                                fontSize = 9.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textMuted,
                                letterSpacing = 0.2.sp
                            )
                            val currentBuildingObj = UniversityDirectory.BUILDINGS.firstOrNull { it.code == selectedBuilding }
                            val buildingLabel = currentBuildingObj?.code ?: if (isNarrow) "All" else "All Buildings"
                            Text(
                                text = buildingLabel,
                                fontSize = 12.5.sp,
                                lineHeight = 15.sp,
                                fontWeight = if (selectedBuilding != null) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (selectedBuilding != null) accentColor else textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select building",
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = buildingDropdownExpanded,
                        onDismissRequest = { onBuildingDropdownExpandedChange(false) },
                        modifier = Modifier
                            .widthIn(min = 230.dp, max = 290.dp)
                            .heightIn(max = 280.dp)
                            .background(surfaceColor, RoundedCornerShape(14.dp))
                    ) {
                        campusBuildings.forEach { b ->
                            val isSelected = selectedBuilding == b.code
                            val isMyFaculty = b.code == defaultScope.buildingCode &&
                                (selectedCampus == null || selectedCampus == defaultScope.campus)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = b.name,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isSelected) accentColor else textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${b.totalRooms} rooms on catalog",
                                                fontSize = 10.5.sp,
                                                color = if (isSelected) accentColor.copy(alpha = 0.8f) else textMuted
                                            )
                                        }
                                        if (isMyFaculty) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Faculty ★",
                                                    fontSize = 10.sp,
                                                    color = accentColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    onBuildingSelected(b.code, if (selectedCampus == null) b.campus else null)
                                    onBuildingDropdownExpandedChange(false)
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        val isAllBuildingsSelected = selectedBuilding == null
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (selectedCampus != null) "All $selectedCampus Buildings" else "All Buildings",
                                    fontSize = 13.sp,
                                    fontWeight = if (isAllBuildingsSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isAllBuildingsSelected) accentColor else textPrimary
                                )
                            },
                            onClick = {
                                onBuildingSelected(null, null)
                                onBuildingDropdownExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }

        val windowInfo = rememberLecturesWindow()
        val isAwayFromDefaults = !isDefaultResetDismissed && (defaultScope.campus != selectedCampus ||
            (defaultScope.buildingCode != null && selectedBuilding != defaultScope.buildingCode))

        if (windowInfo.isTablet) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = freeOnly,
                    onClick = onToggleFreeOnly,
                    label = {
                        Text(
                            text = "Free now",
                            fontSize = 11.5.sp,
                            fontWeight = if (freeOnly) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (freeOnly) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ext.successBg,
                        selectedLabelColor = ext.successText,
                        selectedLeadingIconColor = ext.successText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = freeOnly,
                        borderColor = if (freeOnly) ext.successBorder else borderSubtle
                    ),
                    modifier = Modifier.desktopHandCursor()
                )

                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(cat) },
                        label = {
                            Text(
                                text = when (cat) {
                                    "Room" -> "Lecture Room"
                                    "Lab" -> "Lab"
                                    "Library" -> "Library"
                                    else -> "All Spaces"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.15f),
                            selectedLabelColor = accentColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) accentColor else borderSubtle
                        ),
                        modifier = Modifier.desktopHandCursor()
                    )
                }

                if (isAwayFromDefaults) {
                    SuggestionChip(
                        onClick = onResetToDefaultScope,
                        label = {
                            Text(
                                text = "Reset Faculty (${defaultScope.buildingCode ?: "Default"})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = accentColor.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.desktopHandCursor()
                    )
                }
            }
        } else {
            // 3. Horizontal Chip Row for Quick Status Attributes
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item(key = "chip_free_only") {
                    FilterChip(
                        selected = freeOnly,
                        onClick = onToggleFreeOnly,
                        label = {
                            Text(
                                text = "Free now",
                                fontSize = 11.5.sp,
                                fontWeight = if (freeOnly) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = if (freeOnly) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                        } else null,
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ext.successBg,
                            selectedLabelColor = ext.successText,
                            selectedLeadingIconColor = ext.successText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = freeOnly,
                            borderColor = if (freeOnly) ext.successBorder else borderSubtle
                        ),
                        modifier = Modifier.desktopHandCursor()
                    )
                }

                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(cat) },
                        label = {
                            Text(
                                text = when (cat) {
                                    "Room" -> "Lecture Room"
                                    "Lab" -> "Lab"
                                    "Library" -> "Library"
                                    else -> "All Spaces"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.15f),
                            selectedLabelColor = accentColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) accentColor else borderSubtle
                        ),
                        modifier = Modifier.desktopHandCursor()
                    )
                }

                if (isAwayFromDefaults) {
                    item(key = "chip_reset_faculty") {
                        SuggestionChip(
                            onClick = onResetToDefaultScope,
                            label = {
                                Text(
                                    text = "Reset Faculty (${defaultScope.buildingCode ?: "Default"})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.RestartAlt,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = accentColor.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                            modifier = Modifier.desktopHandCursor()
                        )
                    }
                }
            }
        }
    }
}
