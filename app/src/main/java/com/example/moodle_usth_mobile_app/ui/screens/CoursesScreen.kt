package com.example.moodle_usth_mobile_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.CourseCard
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun CoursesScreen(
    viewModel: MainViewModel,
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = viewModel.getStudySummary()
    val semester = viewModel.semester

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (semester != null) "${semester.label} · ${semester.academicYear}".uppercase() else "",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = stringResource(R.string.home_my_courses),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            IconButton(
                onClick = { viewModel.openDialog("planner") },
                modifier = Modifier
                    .size(42.dp)
                    .background(CardSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = stringResource(R.string.home_planner),
                    tint = PrimaryIndigo
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shortcuts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CardSurface, RoundedCornerShape(14.dp))
                        .clickable { viewModel.openDialog("classwork", "todo") }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.home_filter_todo),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        BadgeItem(text = summary.pendingCount.toString(), tone = "blue")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CardSurface, RoundedCornerShape(14.dp))
                        .clickable { viewModel.openDialog("classwork", "urgent") }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.home_filter_due_soon),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        BadgeItem(text = summary.urgentCount.toString(), tone = "coral")
                    }
                }
            }

            // In Progress Section Heading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_in_progress),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(CardBorderLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d", courses.size),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                val credits = totalCredits(courses)
                BadgeItem(text = pluralStringResource(R.plurals.home_credits, credits, credits), tone = "blue")
            }

            // Courses List
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                courses.forEach { course ->
                    CourseCard(
                        course = viewModel.getCourseWithCompletion(course),
                        onClick = { onCourseClick(course) }
                    )
                }
            }

            // Footer note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.home_courses_footer),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Tổng số tín chỉ của các môn. */
private fun totalCredits(courses: List<Course>): Int {
    var total = 0
    for (course in courses) total += course.credits
    return total
}
