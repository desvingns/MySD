package dev.mysd.game

import kotlin.test.Test
import kotlin.test.assertEquals

class FoundationStatusTest {
    @Test
    fun resolvesPinnedEngineCoreThroughCompositeBuild() {
        assertEquals(1L, FoundationStatus.engineTickAfterStart())
    }
}
