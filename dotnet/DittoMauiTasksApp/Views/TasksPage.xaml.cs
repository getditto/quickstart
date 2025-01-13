using DittoMauiTasksApp.ViewModels;

namespace DittoMauiTasksApp;

public partial class TasksPage : ContentPage
{
    public TasksPage(TasksPageviewModel viewModel)
    {
        InitializeComponent();

        BindingContext = viewModel;
    }
}
