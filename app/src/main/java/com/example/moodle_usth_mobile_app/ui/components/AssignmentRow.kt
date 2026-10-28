package com.example.moodle_usth_mobile_app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.R
import androidx.compose.ui.res.stringResource
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoralLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreenLight
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

@Composable
fun AssignmentRow(
    assignment: Assignment,
    status: AssignmentStatus,
    timeRemainingText: String,
    showCourse: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = status == AssignmentStatus.DONE
    val isUrgent = status == AssignmentStatus.URGENT || status == AssignmentStatus.OVERDUE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) SurfaceLight else CardSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.dp),
        border = if (isDone) BorderStroke(1.dp, CardBorderLight) else null
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDone) AccentGreenLight
                        else if (isUrgent) AccentCoralLight
                        else PrimaryIndigoLight
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isDone) AccentGreen else if (isUrgent) AccentCoral else PrimaryIndigo,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignment.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) TextSecondary else TextPrimary
                )

                if (showCourse) {
                    Text(
                        text = assignment.courseShortTitle,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDone) {
                        Text(
                            text = stringResource(R.string.home_row_completed_nice),
                            fontSize = 11.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isUrgent) AccentCoral else TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = timeRemainingText,
                            fontSize = 11.sp,
                            color = if (isUrgent) AccentCoral else TextPrimary,
                            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                            text = " · ${Formatters.dueDate(assignment.dueAtEpochMs)}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (!compact) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Thành phần điểm & % đóng góp vào điểm môn (hoặc nhãn bonus)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isDone) {
                            BadgeItem(text = stringResource(R.string.home_row_completed), tone = "green")
                            if (assignment.late) BadgeItem(text = stringResource(R.string.common_late), tone = "coral")
                            if (assignment.isAutoGraded) BadgeItem(text = stringResource(R.string.common_auto_graded), tone = "sky")
                        } else if (assignment.isBonus) {
                            BadgeItem(
                                text = stringResource(R.string.common_bonus_up_to, formatScore(assignment.bonusMax ?: 0.0)),
                                tone = componentTone(null)
                            )
                        } else {
                            BadgeItem(
                                text = stringResource(
                                    R.string.home_row_component, assignment.componentName.orEmpty(), assignment.componentWeight ?: 0
                                ),
                                tone = componentTone(assignment.componentId)
                            )
                            val contribution = assignment.contribution
                            if (contribution != null) {
                                BadgeItem(text = stringResource(R.string.home_row_contribution, formatScore(contribution)), tone = "sky")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val score = assignment.score
            if (isDone && score != null) {
                Text(
                    text = buildAnnotatedString {
                        append(formatScore(score))
                        withStyle(SpanStyle(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)) {
                            append("/" + formatScore(assignment.maxScore))
                        }
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
