package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.model.SwipeDirection
import com.example.model.TaggingMode
import com.example.model.keyCodeToReadableLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TagSwipe", appName)
    }

    @Test
    fun `settings repository persists mode direction and 10 hotkeys`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)

        val initial = repo.settingsFlow.first()
        assertEquals(10, initial.hotkeys.size)
        assertEquals(TaggingMode.DATASET_SIDECAR, initial.mode)

        repo.setMode(TaggingMode.HEADER_METADATA)
        repo.setRandomOrder(true)
        repo.setDefaultDirection(SwipeDirection.UP)
        repo.updateDirectionTag(SwipeDirection.UP, "hero_shot")
        val updatedHotkey = initial.hotkeys.first().copy(keyCode = KeyEvent.KEYCODE_Q, keyLabel = "Q", tag = "cinematic")
        repo.updateHotkey(updatedHotkey)

        val saved = repo.settingsFlow.first()
        assertEquals(TaggingMode.HEADER_METADATA, saved.mode)
        assertTrue(saved.isRandomOrder)
        assertEquals(SwipeDirection.UP, saved.defaultDirection)
        assertEquals("hero_shot", saved.directionTags[SwipeDirection.UP])
        assertEquals("Q", saved.hotkeys.first().keyLabel)
        assertEquals("cinematic", saved.hotkeys.first().tag)
    }

    @Test
    fun `keycode label formatting produces clean keycaps`() {
        assertEquals("SPACE", keyCodeToReadableLabel(KeyEvent.KEYCODE_SPACE))
        assertEquals("1", keyCodeToReadableLabel(KeyEvent.KEYCODE_1))
        assertEquals("A", keyCodeToReadableLabel(KeyEvent.KEYCODE_A))
    }
}
