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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.ClassSession
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.toneColor
import com.example.moodle_usth_mobile_app.ui.components.toneLightColor
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val VI = Locale("vi", "VN")
private val WEEKDAY_LABELS = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
private val MONDAY_FIRST = listOf(1, 2, 3, 4, 5, 6, 0)

/** Các phép tính ngày cho thời khóa biểu (tuần bắt đầu từ thứ Hai, theo giờ thiết bị). */
private object Dates {
    fun startOfDay(ms: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun shiftDays(ms: Long, days: Int): Long =
        Calendar.getInstance().apply { timeInMillis = ms; add(Calendar.DAY_OF_MONTH, days) }.timeInMillis

    /** 0 = Chủ nhật … 6 = Thứ bảy (giống trường weekday của API). */
    fun weekday(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_WEEK) - 1

    fun dayOfMonth(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_MONTH)

    fun month(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.let { it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) }

    fun sameDay(a: Long, b: Long) = startOfDay(a) == startOfDay(b)

    fun weekDates(ms: Long): List<Long> {
        val monday = shiftDays(startOfDay(ms), -((weekday(ms) + 6) % 7))
        return List(7) { shiftDays(monday, it) }
    }

    /** Lưới tháng: đủ số tuần (T2→CN) để chứa trọn tháng của [ms]. */
    fun monthDates(ms: Long): List<Long> {
        val first = Calendar.getInstance().apply { timeInMillis = startOfDay(ms); set(Calendar.DAY_OF_MONTH, 1) }
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val leading = (weekday(first.timeInMillis) + 6) % 7
        val cells = ((leading + daysInMonth + 6) / 7) * 7
        val start = shiftDays(first.timeInMillis, -leading)
        return List(cells) { shiftDays(start, it) }
    }

    /** Lùi/tiến [amount] tháng, giữ ngày trong tháng nếu được (31/1 → 28/2). */
    fun shiftMonths(ms: Long, amount: Int): Long = Calendar.getInstance().apply {
        timeInMillis = ms
        val day = get(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, amount)
        set(Calendar.DAY_OF_MONTH, minOf(day, getActualMaximum(Calendar.DAY_OF_MONTH)))
    }.timeInMillis

    fun at(dayMs: Long, time: String): Long {
        val (h, m) = time.split(":").map { it.toInt() }
        return Calendar.getInstance().apply {
            timeInMillis = startOfDay(dayMs); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
        }.timeInMillis
    }
}

private fun ClassSession.hours(): Double {
    fun minutes(t: String) = t.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
    return (minutes(end) - minutes(start)) / 60.0
}

private fun formatHours(hours: Double): String =
    if (hours % 1.0 == 0.0) hours.toInt().toString() else String.format(VI, "%.1f", hours)

@Composable
fun TimetableScreen(
    viewModel: MainViewModel,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDateMs by viewModel.timetableSelectedDateMs.collectAsState()
    val calendarView by viewModel.timetableCalendarView.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val classSessions by viewModel.classSessions.collectAsState()
    val data by viewModel.data.collectAsState()
    val nowMs = viewModel.nowMs

    val semesterStart = data?.semesterStartsAtEpochMs?.let(Dates::startOfDay) ?: Long.MIN_VALUE
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
    val periodSessions = periodDays.flatMap(::sessionsFor)
    val totalHours = periodSessions.sumOf { it.hours() }
    val isCurrentPeriod = if (isMonth) Dates.month(selectedDateMs) == Dates.month(nowMs)
    else Dates.sameDay(days.first(), Dates.weekDates(nowMs).first())

    val daySessions = sessionsFor(selectedDateMs)
    val dayHours = daySessions.sumOf { it.hours() }
    val isToday = Dates.sameDay(selectedDateMs, nowMs)
    val nextSession = daySessions.firstOrNull { Dates.at(selectedDateMs, it.end) > nowMs }

    val shortDate = SimpleDateFormat("dd/MM", VI)
    val fullDate = SimpleDateFormat("EEEE, d 'tháng' M", VI)
    val monthLabel = SimpleDateFormat("'Tháng' M, yyyy", VI)

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
                Text(text = "SẮP XẾP VIỆC HỌC, THEO CÁCH CỦA BẠN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                Text(text = "Thời khóa biểu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isToday) Color(0xFFF1F5F9) else Color.White)
                    .clickable(enabled = !isToday) { viewModel.setTimetableDate(nowMs) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tint = if (isToday) TextSecondary else PrimaryIndigo
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Hôm nay", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BadgeItem(
                            text = when {
                                isMonth && isCurrentPeriod -> "THÁNG NÀY"
                                isMonth -> "THÁNG ĐÃ CHỌN"
                                isCurrentPeriod -> "TUẦN NÀY"
                                else -> "TUẦN ĐÃ CHỌN"
                            },
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
                        text = if (isMonth) "Lịch cả tháng, chủ động hơn." else "Một tuần học, thật rõ ràng.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OverviewStat(Modifier.weight(1f), "${periodSessions.size}", "buổi học", Icons.Default.Book, "${courses.size} môn học")
                        OverviewStat(Modifier.weight(1f), formatHours(totalHours), "giờ học", Icons.Default.Schedule, "Lý thuyết & thực hành")
                    }
                }
            }

            // Calendar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .padding(3.dp)
                    ) {
                        listOf("week" to "Tuần", "month" to "Tháng").forEach { (id, label) ->
                            val selected = calendarView == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color.White else Color.Transparent)
                                    .clickable { viewModel.setTimetableCalendarView(id) }
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
                            text = monthLabel.format(Date(selectedDateMs)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            viewModel.setTimetableDate(if (isMonth) Dates.shiftMonths(selectedDateMs, -1) else Dates.shiftDays(selectedDateMs, -7))
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = if (isMonth) "Tháng trước" else "Tuần trước", tint = TextPrimary)
                        }
                        IconButton(onClick = {
                            viewModel.setTimetableDate(if (isMonth) Dates.shiftMonths(selectedDateMs, 1) else Dates.shiftDays(selectedDateMs, 7))
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = if (isMonth) "Tháng sau" else "Tuần sau", tint = TextPrimary)
                        }
                    }

                    if (isMonth) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MONDAY_FIRST.forEach { weekday ->
                                Text(
                                    text = WEEKDAY_LABELS[weekday],
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        days.chunked(7).forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                week.forEach { day ->
                                    MonthDayCell(
                                        modifier = Modifier.weight(1f),
                                        day = Dates.dayOfMonth(day),
                                        selected = Dates.sameDay(day, selectedDateMs),
                                        today = Dates.sameDay(day, nowMs),
                                        outside = Dates.month(day) != Dates.month(selectedDateMs),
                                        tones = sessionsFor(day).mapNotNull { s -> courses.firstOrNull { it.id == s.courseId }?.tone },
                                        onClick = { viewModel.setTimetableDate(day) }
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
                                    label = WEEKDAY_LABELS[Dates.weekday(day)],
                                    day = Dates.dayOfMonth(day),
                                    selected = Dates.sameDay(day, selectedDateMs),
                                    today = Dates.sameDay(day, nowMs),
                                    hasClasses = sessionsFor(day).isNotEmpty(),
                                    onClick = { viewModel.setTimetableDate(day) }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(PrimaryIndigo, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Có lịch học", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text(text = "Giờ học theo giờ địa phương", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Agenda
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isToday) "HÔM NAY" else fullDate.format(Date(selectedDateMs)).uppercase(VI),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = when {
                                daySessions.isNotEmpty() -> "${daySessions.size} buổi học · ${formatHours(dayHours)} giờ"
                                isToday -> "Hôm nay bạn được nghỉ"
                                else -> "Không có lịch học"
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
                    val nextDay = (1..14).map { Dates.shiftDays(selectedDateMs, it) }.firstOrNull { sessionsFor(it).isNotEmpty() }
                    EmptyDayCard(
                        nextDayLabel = nextDay?.let { "Xem lịch ${fullDate.format(Date(it))}" },
                        onNextDay = { nextDay?.let(viewModel::setTimetableDate) }
                    )
                } else {
                    daySessions.forEach { session ->
                        val course = courses.firstOrNull { it.id == session.courseId } ?: return@forEach
                        val start = Dates.at(selectedDateMs, session.start)
                        val end = Dates.at(selectedDateMs, session.end)
                        SessionCard(
                            session = session,
                            course = course,
                            finished = end <= nowMs,
                            live = nowMs in start until end,
                            upNext = isToday && nextSession?.id == session.id && start > nowMs,
                            onClick = { onCourseClick(course) }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lịch học lặp lại hằng tuần trong học kỳ, đồng bộ từ máy chủ. Chạm vào một buổi học để mở lớp và bài tập.",
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
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (selected) Color.White.copy(alpha = 0.85f) else TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$day", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    when {
                        !hasClasses -> Color.Transparent
                        selected -> Color.White
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
            .clickable(onClick = onClick)
            .alpha(if (outside) 0.35f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "$day", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else TextPrimary)
        Spacer(modifier = Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
            tones.take(3).forEach { tone ->
                Box(modifier = Modifier.size(5.dp).background(if (selected) Color.White else toneColor(tone), CircleShape))
            }
        }
    }
}

@Composable
private fun EmptyDayCard(nextDayLabel: String?, onNextDay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
            Text(text = "Một chút thời gian cho chính mình.", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                text = "Không có lịch học vào ngày này. Bạn có thể ôn bài, hoàn thành bài tập hoặc nghỉ ngơi.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            if (nextDayLabel != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onNextDay)
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
            Text(text = "${formatHours(session.hours())} giờ", fontSize = 10.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(toneColor(course.tone)))
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BadgeItem(text = session.kind, tone = course.tone)
                        Spacer(modifier = Modifier.weight(1f))
                        when {
                            live -> Row(
                                modifier = Modifier
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(AccentGreen, CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(text = "Đang diễn ra", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                            }
                            finished -> Text(text = "Đã kết thúc", fontSize = 11.sp, color = TextSecondary)
                            upNext -> BadgeItem(text = "Sắp tới", tone = "sky")
                            else -> Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                        }
                    }
                    Text(text = course.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${course.code} · ", fontSize = 12.sp, color = TextSecondary)
                        Icon(Icons.Default.School, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "${course.credits} tín chỉ", fontSize = 12.sp, color = TextSecondary)
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
