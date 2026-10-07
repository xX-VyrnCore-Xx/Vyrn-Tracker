package com.vyrn.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {
    @Test
    fun acceptsOnlyKnownActionsAndCanBeConsumed() {
        DeepLink.consume()
        DeepLink.handle("android.intent.action.MAIN")
        assertNull(DeepLink.action)
        DeepLink.handle(DeepLink.NEW_TX)
        assertEquals(DeepLink.NEW_TX, DeepLink.action)
        DeepLink.consume()
        assertNull(DeepLink.action)
    }
}
