package live.ditto.quickstart.tasks

import org.junit.Assert.assertEquals
import org.junit.Test

class DittoModeTest {
    @Test
    fun nullTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select(null))
    }

    @Test
    fun emptyTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select(""))
    }

    @Test
    fun whitespaceOnlyTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select("   \t\n  "))
    }

    @Test
    fun nonEmptyTokenSelectsOffline() {
        assertEquals(DittoMode.OFFLINE, DittoMode.select("any-real-license-token"))
    }
}
