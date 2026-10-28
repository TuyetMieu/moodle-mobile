package com.example.moodle_usth_mobile_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

@Composable
fun AppHeader(
    firstName: String,
    initials: String,
    urgentCount: Int = 0,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(PrimaryIndigo),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // App Wordmark and Greeting
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append("moodle")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)) {
                        append("USTH")
                    }
                },
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = stringResource(R.string.home_greeting, firstName),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Notification Bell Icon with Badge
        Box {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(CardBorderLight, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = if (urgentCount > 0) pluralStringResource(R.plurals.home_notifications_unread, urgentCount, urgentCount)
                    else stringResource(R.string.home_notifications),
                    tint = TextPrimary
                )
            }

            if (urgentCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .background(AccentCoral, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (urgentCount > 9) "9+" else urgentCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
