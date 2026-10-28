package com.example.moodle_usth_mobile_app.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.data.model.formatScore
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import java.util.Locale

/** Indigo nhạt để link đủ tương phản trên nền tối của thẻ deadline. */
private val LinkOnDark = Color(0xFFA5B4FC)

/** Thẻ deadline luôn nền tối (ở cả chế độ sáng và tối) — chữ trên thẻ luôn sáng. */
private val HeroDarkBg = Color(0xFF0F172A)
private val HeroDarkCell = Color(0xFF1E293B)

@Composable
fun CountdownCard(
    firstName: String,
    nextAssignment: Assignment?,
    status: AssignmentStatus,
    onViewTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HeroDarkBg)
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
            Column {
                // Top Live indicator & Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (nextAssignment == null) AccentGreen else AccentCoral,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (nextAssignment == null) stringResource(R.string.home_countdown_none_label)
                            else if (status == AssignmentStatus.OVERDUE) stringResource(R.string.home_countdown_attention)
                            else stringResource(R.string.home_countdown_next),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                    }

                    BadgeItem(
                        text = if (nextAssignment == null) stringResource(R.string.home_countdown_badge_done)
                        else if (status == AssignmentStatus.OVERDUE) stringResource(R.string.time_left_overdue)
                        else if (status == AssignmentStatus.URGENT) stringResource(R.string.home_countdown_badge_24h)
                        else stringResource(R.string.home_countdown_badge_coming),
                        tone = if (nextAssignment == null) "green"
                        else if (status == AssignmentStatus.OVERDUE) "coral"
                        else "blue"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = nextAssignment?.title ?: stringResource(R.string.home_countdown_all_caught_up, firstName),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Countdown Timer or Done Banner, with the deadline illustration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (nextAssignment == null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.home_countdown_all_done),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                        } else if (status == AssignmentStatus.OVERDUE) {
                            Text(
                                text = stringResource(R.string.home_countdown_overdue_start),
                                color = AccentCoral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            LiveCountdownTimer(dueAtEpochMs = nextAssignment.dueAtEpochMs)
                        }
                    }
                    Image(
                        painter = painterResource(id = R.drawable.deadline),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // View task action link
                Row(
                    modifier = Modifier
                        .clickable { onViewTaskClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            nextAssignment == null -> stringResource(R.string.home_countdown_view_done)
                            nextAssignment.isBonus -> stringResource(
                                R.string.home_countdown_view_bonus, formatScore(nextAssignment.bonusMax ?: 0.0)
                            )
                            else -> stringResource(R.string.home_countdown_view_task, formatScore(nextAssignment.contribution ?: 0.0))
                        },
                        color = LinkOnDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = LinkOnDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCountdownTimer(dueAtEpochMs: Long) {
    var nowMs by remember(dueAtEpochMs) { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(dueAtEpochMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingSec = ((dueAtEpochMs - nowMs) / 1000L).coerceAtLeast(0L)
    val days = remainingSec / 86_400
    val hrs = (remainingSec % 86_400) / 3600
    val mins = (remainingSec % 3600) / 60
    val secs = remainingSec % 60

    // Còn từ 1 ngày trở lên: NGÀY : GIỜ : PHÚT; dưới 1 ngày: GIỜ : PHÚT : GIÂY.
    val parts = if (days > 0) {
        listOf(days to stringResource(R.string.home_countdown_days), hrs to stringResource(R.string.home_countdown_hrs),
            mins to stringResource(R.string.home_countdown_min))
    } else {
        listOf(hrs to stringResource(R.string.home_countdown_hrs), mins to stringResource(R.string.home_countdown_min),
            secs to stringResource(R.string.home_countdown_sec))
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        parts.forEachIndexed { index, (value, label) ->
            if (index > 0) Text(text = ":", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            CountdownPart(value = String.format(Locale.US, "%02d", value), label = label)
        }
    }
}

@Composable
private fun CountdownPart(value: String, label: String) {
    Box(
        modifier = Modifier
            .background(HeroDarkCell, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
