package com.vxncius.authx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vxncius.authx.ui.theme.AuthXColors
import com.vxncius.authx.ui.theme.Poppins

@Composable
fun AuthXHeader(title: String) {
    AuthXHeader(title = title, bottomPadding = 8.dp)
}

@Composable
fun AuthXHeader(
    title: String,
    bottomPadding: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthXColors.BgBase)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding)
    ) {
        Text(
            text = title,
            color = AuthXColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AuthXHeaderRow(
    topPadding: Dp = 8.dp,
    bottomPadding: Dp = 14.dp,
    title: @Composable RowScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthXColors.BgBase)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = topPadding, bottom = bottomPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            title()
            actions()
        }
    }
}
