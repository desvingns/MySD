package dev.mysd.game.battle.playable

import dev.myengine.core.CommandId
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import dev.mysd.game.content.ContentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayableBattleCommandTest {
    @Test
    fun `all playable commands have stable wire representations and decode symmetrically`() {
        val target = ContentId.of("build-slot-ash-left")
        val cases = listOf<Triple<PlayableBattleCommand, String, String>>(
            Triple(
                PlayableBattleCommand.Pause,
                PlayableBattleCommandCodec.PAUSE_TYPE,
                "",
            ),
            Triple(
                PlayableBattleCommand.Resume,
                PlayableBattleCommandCodec.RESUME_TYPE,
                "",
            ),
            Triple(
                PlayableBattleCommand.SpendResource(targetSlotId = null, cost = 7),
                PlayableBattleCommandCodec.SPEND_RESOURCE_TYPE,
                "|7",
            ),
            Triple(
                PlayableBattleCommand.SpendResource(targetSlotId = target, cost = 7),
                PlayableBattleCommandCodec.SPEND_RESOURCE_TYPE,
                "${target.value}|7",
            ),
            Triple(
                PlayableBattleCommand.BuildTower(target),
                PlayableBattleCommandCodec.BUILD_TOWER_TYPE,
                target.value,
            ),
            Triple(
                PlayableBattleCommand.UpgradeTower(target),
                PlayableBattleCommandCodec.UPGRADE_TOWER_TYPE,
                target.value,
            ),
        )

        cases.forEachIndexed { index, (command, expectedType, expectedPayload) ->
            assertEquals(expectedType, PlayableBattleCommandCodec.type(command))
            assertEquals(expectedPayload, PlayableBattleCommandCodec.payload(command))
            assertEquals(
                command,
                PlayableBattleCommandCodec.decode(
                    TextCommand(
                        id = CommandId(index.toLong()),
                        scheduledTick = Tick(4),
                        type = expectedType,
                        payload = expectedPayload,
                    ),
                ),
            )
        }
    }

    @Test
    fun `unknown command type is ignored by playable decoder`() {
        val decoded = PlayableBattleCommandCodec.decode(
            TextCommand(
                id = CommandId(1),
                scheduledTick = Tick(2),
                type = "playable-battle.unknown",
                payload = "ignored",
            ),
        )

        assertEquals(null, decoded)
    }

    @Test
    fun `tower commands expose their target through the slot id alias`() {
        val target = ContentId.of("build-slot-ash-left")

        assertEquals(target, PlayableBattleCommand.BuildTower(target).slotId)
        assertEquals(target, PlayableBattleCommand.UpgradeTower(target).slotId)
    }

    @Test
    fun `decoder rejects malformed payloads instead of inventing command state`() {
        assertFailsWith<IllegalArgumentException> {
            PlayableBattleCommandCodec.decode(
                TextCommand(
                    id = CommandId(1),
                    scheduledTick = Tick(2),
                    type = PlayableBattleCommandCodec.PAUSE_TYPE,
                    payload = "unexpected",
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlayableBattleCommandCodec.decode(
                TextCommand(
                    id = CommandId(2),
                    scheduledTick = Tick(2),
                    type = PlayableBattleCommandCodec.SPEND_RESOURCE_TYPE,
                    payload = "slot|7|extra",
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlayableBattleCommandCodec.decode(
                TextCommand(
                    id = CommandId(3),
                    scheduledTick = Tick(2),
                    type = PlayableBattleCommandCodec.SPEND_RESOURCE_TYPE,
                    payload = "slot|not-an-integer",
                ),
            )
        }
    }
}
