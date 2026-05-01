package com.vxncius.authx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AuthXHeader(title: String) {
    AuthXHeader(title = title, bottomPadding = 8.dp)
}

@Composable
fun AuthXHeader(
    title: String,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    Text(
        text = title,
        color = Color.White,
        style = MaterialTheme.typography.titleLarge,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = bottomPadding)
    )
}

@Composable
fun AuthXHeaderRow(
    topPadding: androidx.compose.ui.unit.Dp = 28.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 18.dp,
    title: @Composable RowScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = topPadding, bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        title()
        actions()
    }
}
