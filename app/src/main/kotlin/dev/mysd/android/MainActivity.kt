package dev.mysd.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mysd.android.campaign.CampaignScreen
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.campaign.AcceptedCampaignFixture

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val session = remember { AcceptedCampaignFixture.createSession(runSave = null) }
            var snapshot by remember { mutableStateOf(session.snapshot()) }

            MySDTheme(dynamicColor = false) {
                CampaignScreen(
                    state = snapshot,
                    onIntent = { intent -> snapshot = session.submit(intent) },
                )
            }
        }
    }
}
