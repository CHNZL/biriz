package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkBackground
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise

/**
 * Bento Grid Mimarisi için Apple Design ve FinTech seviyesinde Yumuşak Cam (Glassmorphism) Kartı
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    shape: Shape = RoundedCornerShape(24.dp),
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = glowColor ?: if (isDark) BirizDarkCardBorder else Color(0x1F13294B)
    val cardBackground = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xCC172649),
                Color(0xAA0E1A38)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFF9FBFC).copy(alpha = 0.90f)
            )
        )
    }

    val baseModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Surface(
        modifier = baseModifier
            .shadow(
                elevation = if (isDark) 0.dp else 6.dp,
                shape = shape,
                ambientColor = Color(0x1513294B),
                spotColor = Color(0x1A13294B)
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(if (glowColor != null) 1.5.dp else 1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .background(cardBackground)
                .padding(contentPadding)
        ) {
            Column(content = content)
        }
    }
}

/**
 * Açık / Koyu Tema Değiştirici Buton (Her arka planda daima görünür yüksek kontrastlı tasarım)
 */
@Composable
fun ThemeToggleIconButton(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    androidx.compose.material3.IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(size)
            .testTag("theme_toggle_button")
    ) {
        Surface(
            shape = CircleShape,
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(
                width = 1.2.dp,
                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFCBD5E1)
            ),
            shadowElevation = if (isDark) 3.dp else 2.dp,
            modifier = Modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDark) "Açık Temaya Geç" else "Koyu Temaya Geç",
                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF13294B),
                    modifier = Modifier.size((size.value * 0.52f).dp)
                )
            }
        }
    }
}

/**
 * Modern Havada Süzülen Gezinti Çubuğu (Floating Dock Bar)
 * bottom-4 mx-4 rounded-3xl backdrop-blur-xl bg-[#13294B]/95 text-white shadow-2xl
 */
data class DockNavItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun FloatingDockBar(
    items: List<DockNavItem>,
    selectedId: String,
    onItemSelected: (String) -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dockBg = if (isDark) {
        Color(0xF00B152B)
    } else {
        Color(0xF213294B)
    }
    val dockBorder = if (isDark) {
        Color(0x3564748B)
    } else {
        Color(0x20FFFFFF)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = if (isDark) Color(0x40000000) else Color(0x4013294B),
                spotColor = if (isDark) Color(0x60000000) else Color(0x6013294B)
            ),
        shape = RoundedCornerShape(28.dp),
        color = dockBg,
        border = BorderStroke(1.dp, dockBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.id == selectedId
                val itemBgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        BirizTurquoise.copy(alpha = if (isDark) 0.35f else 0.25f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "dock_item_bg"
                )
                val itemIconColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (isDark) Color(0xFF38BDF8) else Color(0xFF48C9B0)
                    } else {
                        Color.White.copy(alpha = 0.88f)
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "dock_item_color"
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onItemSelected(item.id) }
                        .testTag(item.testTag),
                    shape = RoundedCornerShape(16.dp),
                    color = itemBgColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = itemIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 9.sp,
                                maxLines = 2,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = itemIconColor
                            )
                        }
                    }
                }
            }
        }
    }
}
