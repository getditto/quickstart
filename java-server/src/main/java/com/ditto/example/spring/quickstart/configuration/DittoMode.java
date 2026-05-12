package com.ditto.example.spring.quickstart.configuration;

/**
 * Identity-mode selection based on env vars.
 *
 * <p>Non-empty {@code DITTO_OFFLINE_LICENSE_TOKEN} (after trim) selects {@link #OFFLINE};
 * otherwise the app uses {@link #ONLINE_PLAYGROUND}.
 */
public enum DittoMode {
    ONLINE_PLAYGROUND,
    OFFLINE;

    public static DittoMode select(String offlineLicenseToken) {
        if (offlineLicenseToken == null) {
            return ONLINE_PLAYGROUND;
        }
        return offlineLicenseToken.trim().isEmpty() ? ONLINE_PLAYGROUND : OFFLINE;
    }
}
