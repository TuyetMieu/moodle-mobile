package com.example.moodle_usth_mobile_app.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.PostAudience
import com.example.moodle_usth_mobile_app.data.model.PostKind
import com.example.moodle_usth_mobile_app.data.model.TeacherData
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.TeacherViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.SubjectIcon
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/** Đăng tin cho lớp / ngành / toàn trường và xem lại các tin đã đăng cho ngành, toàn trường. */
@Composable
fun TeacherPostsScreen(data: TeacherData, viewModel: TeacherViewModel) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TeacherHeader(
                overline = stringResource(R.string.teacher_posts_overline),
                title = stringResource(R.string.teacher_posts_title)
            )
        }
        item {
            AudienceButton(Icons.Default.School, "blue", stringResource(R.string.teacher_audience_course),
                stringResource(R.string.teacher_audience_course_help)) { viewModel.openComposer(PostAudience.COURSE) }
        }
        item {
            AudienceButton(Icons.Default.Groups, "purple", stringResource(R.string.teacher_audience_program, data.profile.programCode),
                stringResource(R.string.teacher_audience_program_help, data.profile.programName)) { viewModel.openComposer(PostAudience.PROGRAM) }
        }
        item {
            AudienceButton(Icons.Default.Apartment, "amber", stringResource(R.string.teacher_audience_all),
                stringResource(R.string.teacher_audience_all_help)) { viewModel.openComposer(PostAudience.ALL) }
        }

        item { SectionTitle(stringResource(R.string.teacher_my_posts)) }
        item { Text(stringResource(R.string.teacher_my_posts_help), fontSize = 12.sp, color = TextSecondary) }
        if (data.posts.isEmpty()) {
            item { Text(stringResource(R.string.teacher_no_posts), fontSize = 13.sp, color = TextSecondary) }
        }
        items(data.posts, key = { it.id }) { post ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BadgeItem(
                        if (post.audience == PostAudience.PROGRAM) stringResource(R.string.teacher_audience_program, data.profile.programCode)
                        else stringResource(R.string.teacher_audience_all),
                        if (post.audience == PostAudience.PROGRAM) "purple" else "amber"
                    )
                    BadgeItem(
                        stringResource(if (post.kind == PostKind.EVENT) R.string.teacher_kind_event else R.string.teacher_kind_notice),
                        if (post.kind == PostKind.EVENT) "green" else "blue"
                    )
                }
                Text(post.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (post.kind == PostKind.EVENT && post.eventAtEpochMs != null) {
                    val whenText = Formatters.dateTime(post.eventAtEpochMs)
                    Text(
                        text = if (post.location.isNullOrBlank()) whenText else stringResource(R.string.notif_event_when, whenText, post.location),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                    )
                }
                Text(post.body, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
                Text(Formatters.relative(context, post.createdAtEpochMs, viewModel.nowMs), fontSize = 11.sp, color = TextSecondary)
                ConfirmDeleteButton(
                    question = stringResource(R.string.teacher_delete_post_question, post.title),
                    enabled = !viewModel.isSaving
                ) { viewModel.deletePost(post.id) }
            }
        }
    }
}

@Composable
private fun AudienceButton(icon: ImageVector, tone: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubjectIcon(icon = icon, tone = tone)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}
