using DittoMauiTasksApp.ViewModels;

namespace DittoMauiTasksApp;

public partial class TasksPage : ContentPage
{
    public TasksPage(TasksPageviewModel viewModel)
    {
        InitializeComponent();

        BindingContext = viewModel; 
    }

    void SettingsClicked(System.Object sender, System.EventArgs e)
    {
        Shell.Current.GoToAsync("//PresenceViewerPage");
    }
}
