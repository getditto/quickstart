package com.ditto.example.spring.quickstart.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DittoModeTest {
    @Test
    void nullTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select(null));
    }

    @Test
    void emptyTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select(""));
    }

    @Test
    void whitespaceOnlyTokenSelectsOnline() {
        assertEquals(DittoMode.ONLINE_PLAYGROUND, DittoMode.select("   \t\n  "));
    }

    @Test
    void nonEmptyTokenSelectsOffline() {
        assertEquals(DittoMode.OFFLINE, DittoMode.select("any-real-license-token"));
    }
}
