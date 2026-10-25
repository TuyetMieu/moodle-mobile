package com.example.moodle_usth_mobile_app.ui.components

import com.example.moodle_usth_mobile_app.data.model.GradeComponent
import com.example.moodle_usth_mobile_app.data.model.GradeStatus
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
import com.example.moodle_usth_mobile_app.ui.theme.AccentBlue
import com.example.moodle_usth_mobile_app.ui.theme.AccentPurple
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

@Composable
fun GradeWeightsCard(
    detailed: Boolean = false,
    components: List<GradeComponent> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Know what counts",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (detailed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your final grade is a weighted average of these three components.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented weight bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.1f)
                        .height(8.dp)
                        .background(AccentBlue)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .height(8.dp)
                        .background(AccentAmber)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .height(8.dp)
                        .background(AccentPurple)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeightLegendItem("10%", "Attendance", AccentBlue)
                WeightLegendItem("30%", "Mid-term & labs", AccentAmber)
                WeightLegendItem("60%", "Final project", AccentPurple)
            }

            if (detailed) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    components.forEach { component ->
                        val score = component.score
                        val (text, tone) = when {
                            component.status == GradeStatus.GRADED && score != null -> "${formatGrade(score)} / 10" to "green"
                            component.status == GradeStatus.IN_PROGRESS && score != null -> "In progress · ${formatGrade(score)}" to "amber"
                            else -> "Not graded yet" to "purple"
                        }
                        WeightDetailRow(component.category.label, text, tone)
                    }
                    Text(
                        text = "Project proposals are a milestone of the final project, not an additional 60% assessment.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightLegendItem(percent: String, label: String, color: Color) {
    Column {
        Text(
            text = percent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun WeightDetailRow(label: String, statusText: String, tone: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextPrimary)
        BadgeItem(text = statusText, tone = tone)
    }
}
