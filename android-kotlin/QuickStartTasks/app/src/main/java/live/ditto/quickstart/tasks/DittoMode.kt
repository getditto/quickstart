package live.ditto.quickstart.tasks

/**
 * Identity-mode selection based on env vars.
 *
 * Non-empty `DITTO_OFFLINE_LICENSE_TOKEN` (after trim) selects [OFFLINE];
 * otherwise the app uses [ONLINE_PLAYGROUND].
 */
enum class DittoMode {
    ONLINE_PLAYGROUND,
    OFFLINE;

    companion object {
        fun select(offlineLicenseToken: String?): DittoMode =
            if (offlineLicenseToken.isNullOrBlank()) ONLINE_PLAYGROUND else OFFLINE
    }
}
