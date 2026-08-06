package com.example.dittotasks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DittoModeTest {
    @Test
    public void nullTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select(null));
    }

    @Test
    public void emptyTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select(""));
    }

    @Test
    public void whitespaceOnlyTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select("   \t\n  "));
    }

    @Test
    public void nonEmptyTokenSelectsOffline() {
        assertEquals(DittoMode.OFFLINE, DittoMode.select("any-real-license-token"));
    }
}
