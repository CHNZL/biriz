package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTurquoiseLight
import com.example.ui.theme.BirizWarmKrem

@Composable
fun BirizLogoHeader(
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BirizNavyPrimary, BirizNavyLight, BirizNavyDark)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Resmi BİRİZ Amblemi (Yakınlaştırılmış ve tam ölçekli)
                BirizOfficialLogo(size = 92.dp)

                Spacer(modifier = Modifier.height(10.dp))

                // Logo Başlığı "BİRİZ"
                Text(
                    text = "BİRİZ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                // Alt Başlık
                Text(
                    text = "Dijital Sınıf ve Veli Asistanı",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = BirizTurquoiseLight
                )

                if (badgeText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = BirizWarmKrem,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BirizWarmKrem
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BirizOfficialLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 92.dp
) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier.size(size)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_biriz_emblem),
                contentDescription = "BİRİZ Resmi Amblemi",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

