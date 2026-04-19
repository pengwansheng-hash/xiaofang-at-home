package com.xiaofangathome.senior.platform

import android.app.PendingIntent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPendingIntentPolicyTest {
    @Test
    fun `flag no create allows missing pending intent`() {
        assertTrue(pendingIntentCanBeMissing(PendingIntent.FLAG_NO_CREATE))
    }

    @Test
    fun `update current still expects a created pending intent`() {
        assertFalse(pendingIntentCanBeMissing(PendingIntent.FLAG_UPDATE_CURRENT))
    }
}
