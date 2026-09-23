package dev.mysd.android

import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdRoute
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductTickerTest {
    @Test
    fun inactiveSnapshotSuspendsTickerAndRunningBattleWakesIt() = runBlocking {
        val session = MySdAppFactory.create(seed = 99L)
        val inactive = session.snapshot()
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage("stage-ember-path")).accepted)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
        val running = session.snapshot()
        val snapshots = MutableStateFlow(inactive)
        val pulseGate = Channel<Unit>(Channel.RENDEZVOUS)
        val pulses = Channel<Unit>(Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            runProductTicker(
                snapshots = snapshots,
                awaitNextPulse = { pulseGate.receive() },
                onPulse = { pulses.send(Unit) },
            )
        }

        assertNull(withTimeoutOrNull(100L) { pulseGate.send(Unit) })
        snapshots.value = running
        withTimeout(1_000L) { pulseGate.send(Unit) }
        withTimeout(1_000L) { pulses.receive() }
        snapshots.value = inactive
        yield()
        assertNull(withTimeoutOrNull(100L) { pulseGate.send(Unit) })
        job.cancelAndJoin()
    }
}
