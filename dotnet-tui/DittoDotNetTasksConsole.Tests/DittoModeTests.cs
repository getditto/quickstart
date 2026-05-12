using DittoDotNetTasksConsole;

namespace DittoDotNetTasksConsole.Tests;

public class DittoModeTests
{
    [Fact]
    public void NullTokenSelectsOnline()
    {
        Assert.Equal(DittoMode.OnlinePlayground, DittoModeSelector.Select(null));
    }

    [Fact]
    public void EmptyTokenSelectsOnline()
    {
        Assert.Equal(DittoMode.OnlinePlayground, DittoModeSelector.Select(""));
    }

    [Fact]
    public void WhitespaceOnlyTokenSelectsOnline()
    {
        Assert.Equal(DittoMode.OnlinePlayground, DittoModeSelector.Select("   \t\n  "));
    }

    [Fact]
    public void NonEmptyTokenSelectsOffline()
    {
        Assert.Equal(DittoMode.Offline, DittoModeSelector.Select("any-real-license-token"));
    }
}
