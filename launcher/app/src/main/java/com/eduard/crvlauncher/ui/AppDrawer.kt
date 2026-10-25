package com.eduard.crvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.eduard.crvlauncher.data.AppEntry

@Composable
fun AppDrawer(apps: List<AppEntry>, onLaunch: (AppEntry) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 60.u, vertical = 36.u)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.ArrowBack, "Volver", tint = Color.White,
                modifier = Modifier.size(64.u).clip(RoundedCornerShape(32.u)).clickable(onClick = onClose).padding(10.u),
            )
            Spacer(Modifier.width(20.u))
            Label("Aplicaciones", 40, weight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(30.u))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(170.u),
            verticalArrangement = Arrangement.spacedBy(26.u),
            horizontalArrangement = Arrangement.spacedBy(16.u),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.u))
                        .clickable { onLaunch(app) }
                        .padding(vertical = 14.u),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(app.icon, null, modifier = Modifier.size(96.u))
                    Spacer(Modifier.height(12.u))
                    Label(app.label, 21, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.u))
                }
            }
        }
    }
}
