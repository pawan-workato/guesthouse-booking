package com.guesthouse.booking.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (dark) {
                    Brush.verticalGradient(listOf(GlassDarkBase, GlassDarkBaseEnd))
                } else {
                    Brush.verticalGradient(listOf(GlassLightBase, GlassLightBaseEnd))
                }
            )
    ) {
        Box(
            Modifier
                .offset((-80).dp, (-40).dp)
                .size(320.dp)
                .blur(90.dp)
                .background(
                    if (dark) GlassOrbTealDark else GlassOrbTealLight,
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            Modifier
                .offset(180.dp, 120.dp)
                .size(280.dp)
                .blur(80.dp)
                .background(
                    if (dark) GlassOrbBlueDark else GlassOrbBlueLight,
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            Modifier
                .offset(40.dp, 420.dp)
                .size(260.dp)
                .blur(70.dp)
                .background(
                    if (dark) GlassOrbGreenDark else GlassOrbGreenLight,
                    shape = RoundedCornerShape(50)
                )
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        colors = glassTopAppBarColors(),
        title = title,
        navigationIcon = navigationIcon,
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        content = content
    )
}

@Composable
fun glassSurfaceColor(): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f)
}

@Composable
fun glassBorderColor(): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.65f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun glassTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = glassSurfaceColor(),
    scrolledContainerColor = glassSurfaceColor().copy(alpha = if (isSystemInDarkTheme()) 0.14f else 0.72f)
)

@Composable
fun glassNavigationBarContainerColor(): Color = glassSurfaceColor()

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Card(
        modifier = modifier
            .clip(shape)
            .border(1.dp, glassBorderColor(), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor ?: glassSurfaceColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}
