package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FileCategory
import com.example.data.model.ThemeMode
import com.example.util.FileFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Orion Explorer", appName)
    }

    @Test
    fun `theme modes include system default`() {
        val modes = ThemeMode.entries
        assertTrue(modes.contains(ThemeMode.SYSTEM))
        assertTrue(modes.contains(ThemeMode.PURE_AMOLED))
        assertTrue(modes.contains(ThemeMode.LIGHT))
    }

    @Test
    fun `text viewable extensions are classified properly`() {
        assertTrue(FileFormatters.isTextViewable("kt", "text/plain"))
        assertTrue(FileFormatters.isTextViewable("json", "application/json"))
        assertTrue(FileFormatters.isTextViewable("txt", "text/plain"))
        assertTrue(FileFormatters.isTextViewable("md", "text/markdown"))
        assertFalse(FileFormatters.isTextViewable("mp4", "video/mp4"))
        assertFalse(FileFormatters.isTextViewable("png", "image/png"))
    }

    @Test
    fun `large files category is present`() {
        assertTrue(FileCategory.entries.contains(FileCategory.LARGE_FILES))
    }
}
