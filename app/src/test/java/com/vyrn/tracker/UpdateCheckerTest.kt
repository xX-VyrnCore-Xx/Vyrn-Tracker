package com.vyrn.tracker

import com.vyrn.tracker.update.UpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun comparesDottedVersionsNumerically() {
        assertTrue(UpdateChecker.isNewer("1.0.12", "1.0.10"))
        assertTrue(UpdateChecker.isNewer("2.0", "1.9.9"))
        assertFalse(UpdateChecker.isNewer("1.0.10", "1.0.10"))
        assertFalse(UpdateChecker.isNewer("1.0.9", "1.0.10"))
        assertFalse(UpdateChecker.isNewer("1.0", "1.0.0"))
    }
}
