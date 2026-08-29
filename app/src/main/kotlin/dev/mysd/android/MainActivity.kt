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
    private lateinit var runSaveStorage: AndroidRunSaveStorage
    private lateinit var campaignSession: dev.mysd.game.campaign.CampaignSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runSaveStorage = AndroidRunSaveStorage(this)
        val runSave = runSaveStorage.loadEncodedSave()?.let { encodedSave ->
            try {
                RunSaveCodec.decode(encodedSave)
            } catch (_: PersistenceException) {
                null
            }
        }
        campaignSession = AcceptedCampaignFixture.createSession(runSave = runSave)
        setContent {
            val session = campaignSession
            var snapshot by remember { mutableStateOf(session.snapshot()) }
            var battleSetup by remember { mutableStateOf(session.battleSetupSnapshot()) }
            var activeBattle by remember { mutableStateOf(session.activeBattleSnapshot()) }
            var enhancement by remember { mutableStateOf(session.enhancementSnapshot()) }
            var victory by remember { mutableStateOf(session.victorySnapshot()) }
            var roster by remember { mutableStateOf(session.rosterSnapshot()) }
            var arena by remember { mutableStateOf(session.arenaSnapshot()) }

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
                        arena = session.arenaSnapshot()
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
                    arena = arena,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        persistRunSave()
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        persistRunSave()
        super.onStop()
    }

    private fun persistRunSave() {
        if (!::runSaveStorage.isInitialized || !::campaignSession.isInitialized) return
        runSaveStorage.saveEncodedSave(campaignSession.runSave()?.let(RunSaveCodec::encode))
    }
}
