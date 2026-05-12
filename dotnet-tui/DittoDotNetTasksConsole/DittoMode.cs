namespace DittoDotNetTasksConsole;

/// <summary>
/// Identity-mode selection based on env vars.
///
/// Non-empty <c>DITTO_OFFLINE_LICENSE_TOKEN</c> (after trim) selects
/// <see cref="Offline"/>; otherwise the app uses <see cref="OnlinePlayground"/>.
/// </summary>
public enum DittoMode
{
    OnlinePlayground,
    Offline,
}

public static class DittoModeSelector
{
    public static DittoMode Select(string? offlineLicenseToken)
    {
        return string.IsNullOrWhiteSpace(offlineLicenseToken)
            ? DittoMode.OnlinePlayground
            : DittoMode.Offline;
    }
}
