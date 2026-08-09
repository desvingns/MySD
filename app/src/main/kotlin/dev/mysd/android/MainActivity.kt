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
import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.meta.RosterIntent
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
            var activeBattle by remember { mutableStateOf(session.activeBattleSnapshot()) }
            var enhancement by remember { mutableStateOf(session.enhancementSnapshot()) }
            var victory by remember { mutableStateOf(session.victorySnapshot()) }
            var roster by remember { mutableStateOf(session.rosterSnapshot()) }

            MySDTheme(dynamicColor = false) {
                CampaignScreen(
                    state = snapshot,
                    battleSetup = battleSetup,
                    activeBattle = activeBattle,
                    onIntent = { intent ->
                        session.submit(intent)
                        snapshot = session.snapshot()
                        battleSetup = session.battleSetupSnapshot()
                        activeBattle = session.activeBattleSnapshot()
                        enhancement = session.enhancementSnapshot()
                        victory = session.victorySnapshot()
                        roster = session.rosterSnapshot()
                    },
                    onActiveBattleIntent = { intent: ActiveBattleIntent ->
                        session.submit(intent)
                        snapshot = session.snapshot()
                        activeBattle = session.activeBattleSnapshot()
                        enhancement = session.enhancementSnapshot()
                        victory = session.victorySnapshot()
                    },
                    onEnhancementIntent = { intent: EnhancementIntent ->
                        session.submit(intent)
                        snapshot = session.snapshot()
                        activeBattle = session.activeBattleSnapshot()
                        enhancement = session.enhancementSnapshot()
                        victory = session.victorySnapshot()
                    },
                    onRosterIntent = { intent: RosterIntent ->
                        session.submit(intent)
                        roster = session.rosterSnapshot()
                    },
                    enhancement = enhancement,
                    victory = victory,
                    roster = roster,
                )
            }
        }
    }
}
