using System;
namespace DittoMauiTasksApp.Utils
{
    public static class ServiceProvider
    {
        public static TService GetService<TService>()
            => Current.GetService<TService>();

        public static IServiceProvider Current
            =>
#if WINDOWS10_0_17763_0_OR_GREATER
            MauiWinUIApplication.Current.Services;
#elif ANDROID || IOS || MACCATALYST
            IPlatformApplication.Current.Services;
#else
            null;
#endif
    }
}

