using DittoMauiTasksApp.Data;

namespace DittoMauiTasksApp;

public partial class App : Application
{
    public App()
    {
        InitializeComponent();
    }

    protected override Window CreateWindow(IActivationState activationState)
    {
        // Start async initialization
        _ = InitializeAsync();
        return new Window(new AppShell());
    }

     private async Task InitializeAsync()
    {
        try
        {
            if (Handler?.MauiContext?.Services.GetService<IDataService>() is DittoService dittoManager)
            {
                await dittoManager.Initialize(MauiProgram.GetDittoConfig());
            }
        }
        catch (Exception ex)
        {
            // Handle initialization errors
            if (Current?.Windows[0].Page != null)
            {
                await Current.Windows[0].Page.DisplayAlert("Initialization Error", ex.Message, "OK");
            }
        }
    }
}

