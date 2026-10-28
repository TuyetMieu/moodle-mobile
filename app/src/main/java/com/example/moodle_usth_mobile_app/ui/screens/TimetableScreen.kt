package com.example.moodle_usth_mobile_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.ClassSession
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.SessionKind
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.toneColor
import com.example.moodle_usth_mobile_app.ui.components.toneLightColor
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreenLight
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.OnAccent
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary
import android.text.format.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MONDAY_FIRST = listOf(1, 2, 3, 4, 5, 6, 0)

/** Tên thứ viết tắt theo ngôn ngữ đang dùng; chỉ số 0 = Chủ nhật … 6 = Thứ bảy (giống API). */
private fun weekdayLabels(locale: Locale): List<String> {
    val names = DateFormatSymbols.getInstance(locale).shortWeekdays // [1] = Chủ nhật … [7] = Thứ bảy
    return (1..7).map { names[it] }
}

/** Mẫu ngày giờ phù hợp với ngôn ngữ (VD "MMMMyyyy" → "October 2026" / "tháng 10, 2026"). */
private fun localizedFormat(skeleton: String, locale: Locale): SimpleDateFormat =
    SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

private fun capitalized(text: String, locale: Locale): String =
    text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

/** Các phép tính ngày cho thời khóa biểu (tuần bắt đầu từ thứ Hai, theo giờ của điện thoại). */
private object Dates {

    private fun calendarAt(ms: Long): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = ms
        return calendar
    }

    /** 0 giờ 00 của ngày chứa [ms]. */
    fun startOfDay(ms: Long): Long {
        val calendar = calendarAt(ms)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun shiftDays(ms: Long, days: Int): Long {
        val calendar = calendarAt(ms)
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.timeInMillis
    }

    /** 0 = Chủ nhật … 6 = Thứ bảy (giống trường weekday của API). */
    fun weekday(ms: Long): Int = calendarAt(ms).get(Calendar.DAY_OF_WEEK) - 1

    fun dayOfMonth(ms: Long): Int = calendarAt(ms).get(Calendar.DAY_OF_MONTH)

    /** Số tháng tính từ năm 0, dùng để so sánh hai ngày có cùng tháng không. */
    fun month(ms: Long): Int {
        val calendar = calendarAt(ms)
        return calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
    }

    fun sameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

    /** 7 ngày từ thứ Hai đến Chủ nhật của tuần chứa [ms]. */
    fun weekDates(ms: Long): List<Long> {
        val daysSinceMonday = (weekday(ms) + 6) % 7
        val monday = shiftDays(startOfDay(ms), -daysSinceMonday)
        val result = mutableListOf<Long>()
        for (i in 0 until 7) result.add(shiftDays(monday, i))
        return result
    }

    /** Các ô của lịch tháng: đủ số tuần (T2 → CN) để chứa trọn tháng của [ms]. */
    fun monthDates(ms: Long): List<Long> {
        val first = calendarAt(startOfDay(ms))
        first.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val emptyBefore = (weekday(first.timeInMillis) + 6) % 7 // số ô trống trước ngày 1
        val cellCount = ((emptyBefore + daysInMonth + 6) / 7) * 7 // làm tròn lên đủ tuần
        val start = shiftDays(first.timeInMillis, -emptyBefore)
        val result = mutableListOf<Long>()
        for (i in 0 until cellCount) result.add(shiftDays(start, i))
        return result
    }

    /** Lùi/tiến [amount] tháng, giữ ngày trong tháng nếu được (31/1 → 28/2). */
    fun shiftMonths(ms: Long, amount: Int): Long {
        val calendar = calendarAt(ms)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, amount)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, if (day < lastDay) day else lastDay)
        return calendar.timeInMillis
    }

    /** Thời điểm "HH:mm" của ngày [dayMs]. */
    fun at(dayMs: Long, time: String): Long {
        val parts = time.split(":")
        val calendar = calendarAt(startOfDay(dayMs))
        calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        calendar.set(Calendar.MINUTE, parts[1].toInt())
        return calendar.timeInMillis
    }
}

/** Đổi "HH:mm" thành số phút trong ngày. */
private fun toMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}

/** Số giờ của một buổi học. */
private fun sessionHours(session: ClassSession): Double = (toMinutes(session.end) - toMinutes(session.start)) / 60.0

/** Màu của từng môn có buổi học trong ngày (để vẽ các chấm màu trên lịch tháng). */
private fun courseTones(sessions: List<ClassSession>, courses: List<Course>): List<String> {
    val tones = mutableListOf<String>()
    for (session in sessions) {
        val course = courses.find { it.id == session.courseId }
        if (course != null) tones.add(course.tone)
    }
    return tones
}

/** Tổng số giờ của nhiều buổi học. */
private fun totalHoursOf(sessions: List<ClassSession>): Double {
    var total = 0.0
    for (session in sessions) total += sessionHours(session)
    return total
}

private fun formatHours(hours: Double): String =
    if (hours % 1.0 == 0.0) hours.toInt().toString() else String.format(Locale.getDefault(), "%.1f", hours)

@Composable
fun TimetableScreen(
    viewModel: MainViewModel,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDateMs = viewModel.timetableSelectedDateMs
    val calendarView = viewModel.timetableCalendarView
    val courses = viewModel.courses
    val classSessions = viewModel.classSessions
    val data = viewModel.data
    val nowMs = viewModel.nowMs

    val semesterStart = if (data != null) Dates.startOfDay(data.semesterStartsAtEpochMs) else Long.MIN_VALUE
    val semesterEnd = data?.semesterEndsAtEpochMs ?: Long.MAX_VALUE

    /** Buổi học của một ngày: lịch lặp hằng tuần, chỉ trong thời gian học kỳ. */
    fun sessionsFor(dayMs: Long): List<ClassSession> {
        val day = Dates.startOfDay(dayMs)
        if (day < semesterStart || day > semesterEnd) return emptyList()
        val weekday = Dates.weekday(day)
        return classSessions.filter { it.weekday == weekday }.sortedBy { it.start }
    }

    val isMonth = calendarView == "month"
    val days = if (isMonth) Dates.monthDates(selectedDateMs) else Dates.weekDates(selectedDateMs)
    val periodDays = if (isMonth) days.filter { Dates.month(it) == Dates.month(selectedDateMs) } else days
    val periodSessions = mutableListOf<ClassSession>()
    for (day in periodDays) periodSessions.addAll(sessionsFor(day))
    val totalHours = totalHoursOf(periodSessions)
    val isCurrentPeriod = if (isMonth) Dates.month(selectedDateMs) == Dates.month(nowMs)
    else Dates.sameDay(days.first(), Dates.weekDates(nowMs).first())

    val daySessions = sessionsFor(selectedDateMs)
    val dayHours = totalHoursOf(daySessions)
    val isToday = Dates.sameDay(selectedDateMs, nowMs)
    val nextSession = daySessions.firstOrNull { Dates.at(selectedDateMs, it.end) > nowMs }

    // Đọc locale qua LocalConfiguration để màn hình vẽ lại khi đổi ngôn ngữ.
    val locale = LocalConfiguration.current.locales[0]
    val shortDate = localizedFormat("ddMM", locale)
    val fullDate = localizedFormat("EEEEdMMMM", locale)
    val monthLabel = localizedFormat("MMMMyyyy", locale)
    val weekdayNames = weekdayLabels(locale)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.tt_eyebrow).uppercase(locale), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                Text(text = stringResource(R.string.tt_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isToday) CardBorderLight else CardSurface)
                    .clickable(enabled = !isToday, role = Role.Button) { viewModel.timetableSelectedDateMs = nowMs }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tint = if (isToday) TextSecondary else PrimaryIndigo
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.tt_today), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BadgeItem(
                            text = when {
                                isMonth && isCurrentPeriod -> stringResource(R.string.tt_this_month)
                                isMonth -> stringResource(R.string.tt_selected_month)
                                isCurrentPeriod -> stringResource(R.string.tt_this_week)
                                else -> stringResource(R.string.tt_selected_week)
                            }.uppercase(locale),
                            tone = "sky"
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${shortDate.format(Date(periodDays.first()))} – ${shortDate.format(Date(periodDays.last()))}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = stringResource(if (isMonth) R.string.tt_month_tagline else R.string.tt_week_tagline),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OverviewStat(
                            Modifier.weight(1f), "${periodSessions.size}",
                            pluralStringResource(R.plurals.tt_sessions_unit, periodSessions.size),
                            Icons.Default.Book,
                            pluralStringResource(R.plurals.tt_courses_count, courses.size, courses.size)
                        )
                        OverviewStat(
                            Modifier.weight(1f), formatHours(totalHours), stringResource(R.string.tt_hours_unit),
                            Icons.Default.Schedule, stringResource(R.string.tt_lecture_and_lab)
                        )
                    }
                }
            }

            // Calendar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceLight, RoundedCornerShape(10.dp))
                            .padding(3.dp)
                    ) {
                        listOf("week" to stringResource(R.string.tt_view_week), "month" to stringResource(R.string.tt_view_month)).forEach { (id, label) ->
                            val selected = calendarView == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) CardSurface else Color.Transparent)
                                    .clickable(role = Role.Tab) { viewModel.timetableCalendarView = id }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = capitalized(monthLabel.format(Date(selectedDateMs)), locale),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            viewModel.timetableSelectedDateMs = if (isMonth) Dates.shiftMonths(selectedDateMs, -1) else Dates.shiftDays(selectedDateMs, -7)
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(if (isMonth) R.string.tt_prev_month else R.string.tt_prev_week), tint = TextPrimary)
                        }
                        IconButton(onClick = {
                            viewModel.timetableSelectedDateMs = if (isMonth) Dates.shiftMonths(selectedDateMs, 1) else Dates.shiftDays(selectedDateMs, 7)
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(if (isMonth) R.string.tt_next_month else R.string.tt_next_week), tint = TextPrimary)
                        }
                    }

                    if (isMonth) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MONDAY_FIRST.forEach { weekday ->
                                Text(
                                    text = weekdayNames[weekday],
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        // Chia các ô thành từng hàng 7 ngày.
                        val weekCount = days.size / 7
                        for (weekIndex in 0 until weekCount) {
                            val week = days.subList(weekIndex * 7, weekIndex * 7 + 7)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                week.forEach { day ->
                                    MonthDayCell(
                                        modifier = Modifier.weight(1f),
                                        day = Dates.dayOfMonth(day),
                                        selected = Dates.sameDay(day, selectedDateMs),
                                        today = Dates.sameDay(day, nowMs),
                                        outside = Dates.month(day) != Dates.month(selectedDateMs),
                                        tones = courseTones(sessionsFor(day), courses),
                                        onClick = { viewModel.timetableSelectedDateMs = day }
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            courses.forEach { course ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(7.dp).background(toneColor(course.tone), CircleShape))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(text = course.code, fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            days.forEach { day ->
                                WeekDayCell(
                                    modifier = Modifier.weight(1f),
                                    label = weekdayNames[Dates.weekday(day)],
                                    day = Dates.dayOfMonth(day),
                                    selected = Dates.sameDay(day, selectedDateMs),
                                    today = Dates.sameDay(day, nowMs),
                                    hasClasses = sessionsFor(day).isNotEmpty(),
                                    onClick = { viewModel.timetableSelectedDateMs = day }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(PrimaryIndigo, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.tt_has_classes), fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text(text = stringResource(R.string.tt_local_time), fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Agenda
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = (if (isToday) stringResource(R.string.tt_today) else fullDate.format(Date(selectedDateMs))).uppercase(locale),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = when {
                                daySessions.isNotEmpty() -> pluralStringResource(
                                    R.plurals.tt_day_summary, daySessions.size, daySessions.size, formatHours(dayHours)
                                )
                                isToday -> stringResource(R.string.tt_free_today)
                                else -> stringResource(R.string.tt_no_classes)
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    BadgeItem(text = shortDate.format(Date(selectedDateMs)), tone = "sky")
                }

                if (daySessions.isEmpty()) {
                    // Gợi ý ngày gần nhất phía sau có lịch học (trong vòng 2 tuần).
                    var nextDay: Long? = null
                    for (i in 1..14) {
                        val day = Dates.shiftDays(selectedDateMs, i)
                        if (sessionsFor(day).isNotEmpty()) {
                            nextDay = day
                            break
                        }
                    }
                    EmptyDayCard(
                        nextDayLabel = if (nextDay != null) stringResource(R.string.tt_see_day, fullDate.format(Date(nextDay))) else null,
                        onNextDay = { if (nextDay != null) viewModel.timetableSelectedDateMs = nextDay }
                    )
                } else {
                    for (session in daySessions) {
                        val course = courses.find { it.id == session.courseId }
                        if (course == null) continue
                        val start = Dates.at(selectedDateMs, session.start)
                        val end = Dates.at(selectedDateMs, session.end)
                        SessionCard(
                            session = session,
                            course = course,
                            finished = end <= nowMs,
                            live = nowMs >= start && nowMs < end,
                            upNext = isToday && nextSession != null && nextSession.id == session.id && start > nowMs,
                            onClick = { onCourseClick(course) }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tt_footer),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OverviewStat(modifier: Modifier, value: String, unit: String, icon: ImageVector, caption: String) {
    Column(modifier = modifier) {
        Text(
            text = buildAnnotatedString {
                append(value)
                withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)) { append(" $unit") }
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = caption, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun WeekDayCell(
    modifier: Modifier,
    label: String,
    day: Int,
    selected: Boolean,
    today: Boolean,
    hasClasses: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryIndigo else Color.Transparent)
            .then(if (today && !selected) Modifier.border(1.5.dp, PrimaryIndigo, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (selected) OnAccent.copy(alpha = 0.85f) else TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$day", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (selected) OnAccent else TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    when {
                        !hasClasses -> Color.Transparent
                        selected -> OnAccent
                        else -> PrimaryIndigo
                    },
                    CircleShape
                )
        )
    }
}

@Composable
private fun MonthDayCell(
    modifier: Modifier,
    day: Int,
    selected: Boolean,
    today: Boolean,
    outside: Boolean,
    tones: List<String>,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .aspectRatio(0.9f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PrimaryIndigo else Color.Transparent)
            .then(if (today && !selected) Modifier.border(1.5.dp, PrimaryIndigo, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .alpha(if (outside) 0.35f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "$day", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) OnAccent else TextPrimary)
        Spacer(modifier = Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
            tones.take(3).forEach { tone ->
                Box(modifier = Modifier.size(5.dp).background(if (selected) OnAccent else toneColor(tone), CircleShape))
            }
        }
    }
}

@Composable
private fun EmptyDayCard(nextDayLabel: String?, onNextDay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(PrimaryIndigoLight, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(28.dp))
            }
            Text(text = stringResource(R.string.tt_empty_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            Text(
                text = stringResource(R.string.tt_empty_body),
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            if (nextDayLabel != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onNextDay)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = nextDayLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: ClassSession,
    course: Course,
    finished: Boolean,
    live: Boolean,
    upNext: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (finished) 0.6f else 1f),
        verticalAlignment = Alignment.Top
    ) {
        // Time column
        Column(modifier = Modifier.width(52.dp).padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = session.start, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = session.end, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.width(2.dp).height(18.dp).background(toneLightColor(course.tone)))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = stringResource(R.string.tt_hours_short, formatHours(sessionHours(session))), fontSize = 10.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(toneColor(course.tone)))
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BadgeItem(
                            text = stringResource(if (session.kind == SessionKind.LAB) R.string.tt_kind_lab else R.string.tt_kind_lecture),
                            tone = course.tone
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        when {
                            live -> Row(
                                modifier = Modifier
                                    .background(AccentGreenLight, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(AccentGreen, CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(text = stringResource(R.string.tt_live), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                            }
                            finished -> Text(text = stringResource(R.string.tt_finished), fontSize = 11.sp, color = TextSecondary)
                            upNext -> BadgeItem(text = stringResource(R.string.tt_up_next), tone = "sky")
                            else -> Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                        }
                    }
                    Text(text = course.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${course.code} · ", fontSize = 12.sp, color = TextSecondary)
                        Icon(Icons.Default.School, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = pluralStringResource(R.plurals.tt_credits, course.credits, course.credits), fontSize = 12.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = toneColor(course.tone), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = session.room, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = course.teacher, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}
