package biz.prpic.hackertubetv.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.ui.AppColors

/** The title above a screen's content, with an optional line under it. */
@Composable
fun ScreenHeading(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    kicker: String? = null,
) {
    Column(modifier.padding(start = 8.dp, top = 20.dp, bottom = 12.dp)) {
        if (kicker != null) {
            Text(text = kicker, modifier = Modifier.padding(bottom = 2.dp), color = AppColors.Secondary, fontSize = 14.sp)
        }
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 4.dp),
                color = AppColors.Secondary,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun ScreenHeading(title: String, modifier: Modifier = Modifier, subtitle: String? = null, kicker: String? = null) {
    ScreenHeading(AnnotatedString(title), modifier, subtitle, kicker)
}
