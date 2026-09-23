package dev.mysd.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import dev.mysd.android.persistence.AndroidProductPersistence
import dev.mysd.android.persistence.AndroidRunSaveStorage
import dev.mysd.android.campaign.CampaignScreen
import dev.mysd.android.product.MySdProductApp
import dev.mysd.android.product.ProductViewModel
import dev.mysd.android.ui.theme.ProductColors
import dev.mysd.android.product.ProductFeedbackHost
import dev.mysd.android.product.ProductRecoveryDialog
import dev.mysd.android.product.ProductSaveFailureBanner
import dev.mysd.android.product.ProductPrimaryButton
import dev.mysd.android.product.ProductUiAction
import dev.mysd.android.product.toProductUiModel
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleCommand
import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.CampaignSession
import dev.mysd.game.campaign.CampaignIntent
import dev.mysd.game.meta.RosterIntent
import dev.mysd.game.persistence.PersistenceException
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec
import dev.mysd.game.product.MySdAppSnapshot
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.simulation.SimulationClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal suspend fun runPlayableBattleTicker(
    snapshots: Flow<dev.mysd.game.simulation.PlayableBattleSnapshot?>,
    awaitNextTick: suspend () -> Unit = {
        delay(SimulationClock.TICK_DURATION_MILLIS)
    },
    onTick: suspend () -> Unit,
) {
    snapshots
        .map { snapshot ->
            snapshot != null &&
                snapshot.phase == PlayableBattlePhase.ACTIVE &&
                snapshot.terminalResult == null
        }
        .distinctUntilChanged()
        .collectLatest { shouldTick ->
            if (shouldTick) {
                while (true) {
                    awaitNextTick()
                    onTick()
                }
            }
        }
}

internal suspend fun runProductTicker(
    snapshots: Flow<MySdAppSnapshot>,
    awaitNextPulse: suspend () -> Unit = {
        delay(SimulationClock.TICK_DURATION_MILLIS)
    },
    onPulse: suspend () -> Unit,
) {
    snapshots
        .map { snapshot -> snapshot.clock.running }
        .distinctUntilChanged()
        .collectLatest { shouldTick ->
            if (shouldTick) {
                while (true) {
                    awaitNextPulse()
                    onPulse()
                }
            }
        }
}

class MainActivity : ComponentActivity() {
    private lateinit var productViewModel: ProductViewModel
    private lateinit var legacyRunSaveStorage: AndroidRunSaveStorage
    private var legacyCampaignSession: CampaignSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        legacyRunSaveStorage = AndroidRunSaveStorage(applicationContext)
        val productPersistence = AndroidProductPersistence(applicationContext)
        val legacyRun = loadLegacyRunWhenProductProfileIsAbsent(productPersistence)
        if (legacyRun != null) {
            val session = AcceptedCampaignFixture.createSession(runSave = legacyRun)
            if (session.runSave() != null) {
                legacyCampaignSession = session
                installLegacyCampaign(session)
                return
            }
        }
        productViewModel = ViewModelProvider(
            this,
            ProductViewModel.Factory(productPersistence),
        )[ProductViewModel::class.java]
        installProductHost()
    }

    private fun installProductHost() {
        val activityLifecycle = lifecycle
        setContent {
            val snapshot by productViewModel.snapshot.collectAsState()
            val recoveryNotice by productViewModel.recoveryNotice.collectAsState()
            val saveFailed by productViewModel.saveFailed.collectAsState()

            LaunchedEffect(productViewModel, activityLifecycle) {
                activityLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    runProductTicker(
                        snapshots = productViewModel.snapshot,
                        onPulse = { productViewModel.pulse() },
                    )
                }
            }
            LaunchedEffect(productViewModel, activityLifecycle, "save-retry") {
                activityLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    productViewModel.saveFailed.collectLatest { failed ->
                        if (failed) {
                            while (productViewModel.saveFailed.value) {
                                delay(2_000L)
                                productViewModel.persistNow()
                            }
                        }
                    }
                }
            }

            MySDTheme(dynamicColor = false) {
                ProductFeedbackHost(productViewModel, activityLifecycle)
                Column(Modifier.fillMaxSize()) {
                    if (saveFailed) ProductSaveFailureBanner { productViewModel.persistNow() }
                    MySdProductApp(
                        state = snapshot.toProductUiModel(),
                        onAction = productViewModel::submit,
                        modifier = Modifier.weight(1f),
                    )
                }
                recoveryNotice?.let { notice ->
                    ProductRecoveryDialog(notice, productViewModel::dismissRecoveryNotice)
                }
            }
        }
    }

    private fun installLegacyCampaign(session: CampaignSession) {
        val activityLifecycle = lifecycle
        setContent {
            var snapshot by remember { mutableStateOf(session.snapshot()) }
            var battleSetup by remember { mutableStateOf(session.battleSetupSnapshot()) }
            var activeBattle by remember { mutableStateOf(session.activeBattleSnapshot()) }
            var playableBattle by remember { mutableStateOf(session.playableBattleSnapshot()) }
            var enhancement by remember { mutableStateOf(session.enhancementSnapshot()) }
            var victory by remember { mutableStateOf(session.victorySnapshot()) }
            var roster by remember { mutableStateOf(session.rosterSnapshot()) }
            var arena by remember { mutableStateOf(session.arenaSnapshot()) }

            fun publishSnapshots() {
                snapshot = session.snapshot()
                battleSetup = session.battleSetupSnapshot()
                activeBattle = session.activeBattleSnapshot()
                playableBattle = session.playableBattleSnapshot()
                enhancement = session.enhancementSnapshot()
                victory = session.victorySnapshot()
                roster = session.rosterSnapshot()
                arena = session.arenaSnapshot()
            }

            LaunchedEffect(session, activityLifecycle) {
                activityLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    runPlayableBattleTicker(
                        snapshots = snapshotFlow { playableBattle },
                        onTick = {
                            val currentBattle = session.playableBattleSnapshot()
                            if (
                                currentBattle != null &&
                                currentBattle.phase == PlayableBattlePhase.ACTIVE &&
                                currentBattle.terminalResult == null
                            ) {
                                session.advance(SimulationClock.TICK_DURATION_MILLIS)
                            }
                            publishSnapshots()
                        },
                    )
                }
            }

            MySDTheme(dynamicColor = false) {
                Column(Modifier.fillMaxSize().background(ProductColors.Void)) {
                CampaignScreen(
                    modifier = Modifier.weight(1f),
                    state = snapshot,
                    battleSetup = battleSetup,
                    activeBattle = activeBattle,
                    playableBattle = playableBattle,
                    onIntent = { intent ->
                        session.submit(intent)
                        publishSnapshots()
                        if (intent == CampaignIntent.CancelUnfinishedRun && session.runSave() == null) {
                            openProductAfterLegacyRun()
                        }
                    },
                    onActiveBattleIntent = { intent: ActiveBattleIntent ->
                        session.submit(intent)
                        publishSnapshots()
                    },
                    onPlayableBattleCommand = { command: PlayableBattleCommand ->
                        session.submit(command)
                        publishSnapshots()
                    },
                    onEnhancementIntent = { intent: EnhancementIntent ->
                        session.submit(intent)
                        publishSnapshots()
                    },
                    onRosterIntent = { intent: RosterIntent ->
                        session.submit(intent)
                        publishSnapshots()
                    },
                    enhancement = enhancement,
                    victory = victory,
                    roster = roster,
                    arena = arena,
                )
                if (playableBattle?.terminalResult != null || victory != null) {
                    ProductPrimaryButton(
                        label = stringResource(R.string.product_legacy_continue),
                        onClick = ::openProductAfterLegacyRun,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        testTag = "product-legacy-continue",
                    )
                }
                }
            }
        }
    }

    private fun openProductAfterLegacyRun() {
        val legacy = legacyCampaignSession ?: return
        val encoded = legacy.runSave()?.let(RunSaveCodec::encode)
            ?: legacyRunSaveStorage.loadEncodedSave()
        val persistence = AndroidProductPersistence(applicationContext)
        // Preserve the compatibility document in the same atomic commit that introduces product state.
        if (!persistence.save(MySdAppFactory.create().saveBundle(), archivedLegacyRun = encoded)) return
        legacyCampaignSession = null
        productViewModel = ViewModelProvider(this, ProductViewModel.Factory(persistence))[ProductViewModel::class.java]
        productViewModel.submit(ProductUiAction.EnterCampaign)
        installProductHost()
    }

    private fun loadLegacyRunWhenProductProfileIsAbsent(
        productPersistence: AndroidProductPersistence,
    ): RunSave? {
        if (productPersistence.hasProductProfile()) return null
        val encoded = legacyRunSaveStorage.loadEncodedSave() ?: return null
        return try {
            RunSaveCodec.decode(encoded)
        } catch (_: PersistenceException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        persistProductState()
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        if (::productViewModel.isInitialized) productViewModel.refreshEnergy()
    }

    override fun onResume() {
        super.onResume()
        if (::productViewModel.isInitialized) productViewModel.refreshEnergy()
    }

    override fun onStop() {
        persistProductState()
        super.onStop()
    }

    private fun persistProductState() {
        val legacySession = legacyCampaignSession
        if (legacySession != null) {
            legacyRunSaveStorage.saveEncodedSave(legacySession.runSave()?.let(RunSaveCodec::encode))
        } else if (::productViewModel.isInitialized) {
            productViewModel.persistNow()
        }
    }
}
