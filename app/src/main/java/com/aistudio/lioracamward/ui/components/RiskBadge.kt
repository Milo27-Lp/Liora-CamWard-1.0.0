package com.aistudio.lioracamward.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.lioracamward.data.model.RiskLevel
import com.aistudio.lioracamward.ui.theme.DangerRed
import com.aistudio.lioracamward.ui.theme.DangerRedDim
import com.aistudio.lioracamward.ui.theme.InfoBlue
import com.aistudio.lioracamward.ui.theme.InfoBlueDim
import com.aistudio.lioracamward.ui.theme.RadarGreenDark
import com.aistudio.lioracamward.ui.theme.RadarNeonGreen
import com.aistudio.lioracamward.ui.theme.WarningAmber
import com.aistudio.lioracamward.ui.theme.WarningAmberDim

@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor) = when (riskLevel) {
        RiskLevel.CLEAR -> Triple(RadarGreenDark, RadarNeonGreen, RadarNeonGreen)
        RiskLevel.LOW -> Triple(InfoBlueDim, InfoBlue, InfoBlue)
        RiskLevel.MEDIUM -> Triple(WarningAmberDim, WarningAmber, WarningAmber)
        RiskLevel.HIGH -> Triple(DangerRedDim, DangerRed, DangerRed)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(borderColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = riskLevel.label.uppercase(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
        }
    }
}
