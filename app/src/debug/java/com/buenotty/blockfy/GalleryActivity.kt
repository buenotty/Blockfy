package com.buenotty.blockfy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.feature_progress.Medals
import com.buenotty.blockfy.feature_progress.Ranks
import com.buenotty.blockfy.feature_progress.ui.MedalBadge
import com.buenotty.blockfy.feature_progress.ui.RankBadge
import com.buenotty.blockfy.feature_progress.ui.rankTitle
import com.buenotty.blockfy.ui.theme.BlockfyTheme

class GalleryActivity : ComponentActivity() {
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockfyTheme(darkTheme = true) {
                Column(
                    Modifier.background(Color(0xFF14161C)).verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val levels = listOf(0, 1, 2, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 31, 61, 181)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
                        levels.forEach { l ->
                            val rank = Ranks.of(Ranks.xpForLevel(l))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                RankBadge(rank, size = 84.dp)
                                Text(rankTitle(rank), style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
                        Medals.all.forEachIndexed { i, def -> MedalBadge(def, unlocked = i % 4 != 3, size = 84.dp) }
                    }
                }
            }
        }
    }
}
