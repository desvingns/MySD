package dev.mysd.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mysd.android.campaign.CampaignScreen
import dev.mysd.android.persistence.AndroidRunSaveStorage
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.persistence.PersistenceException
import dev.mysd.game.persistence.RunSaveCodec

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runSave = AndroidRunSaveStorage(this).loadEncodedSave()?.let { encodedSave ->
            try {
                RunSaveCodec.decode(encodedSave)
            } catch (_: PersistenceException) {
                null
            }
        }
        setContent {
            val session = remember { AcceptedCampaignFixture.createSession(runSave = runSave) }
            var snapshot by remember { mutableStateOf(session.snapshot()) }
            var battleSetup by remember { mutableStateOf(session.battleSetupSnapshot()) }

            MySDTheme(dynamicColor = false) {
                CampaignScreen(
                    state = snapshot,
                    battleSetup = battleSetup,
                    onIntent = { intent ->
                        session.submit(intent)
                        snapshot = session.snapshot()
                        battleSetup = session.battleSetupSnapshot()
                    },
                )
            }
        }
    }
}
