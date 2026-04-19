package com.xiaofangathome.senior.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiLabelHelpersTest {
    @Test
    fun `preferred avatar label uses the first visible character`() {
        assertEquals("刘", preferredAvatarLabel("刘叔叔"))
    }

    @Test
    fun `contact badge prefers relation text`() {
        assertEquals("女儿", contactBadgeLabel("女儿", "刘小芳"))
    }

    @Test
    fun `normalize primary route strips nested segments`() {
        assertEquals("reminder_detail", normalizePrimaryRoute("reminder_detail/123"))
    }
}
