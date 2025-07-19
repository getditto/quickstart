using DittoMauiTasksApp.ViewModels;
using DittoMauiTasksApp.Model;

namespace DittoMauiTasksApp;

public partial class TasksPage : ContentPage
{
    public TasksPage(TasksPageViewModel viewModel)
    {
        InitializeComponent();

        BindingContext = viewModel;
    }

    // Event handler invoked when a checkbox on the page is checked or unchecked.
    private void OnCheckBoxCheckedChanged(object sender, CheckedChangedEventArgs e)
    {
        if (sender is CheckBox checkBox && checkBox.BindingContext is DittoTask task)
        {
            if (!checkBox.IsVisible || !checkBox.IsEnabled)
            {
                return;
            }

            var viewModel = BindingContext as TasksPageViewModel;
            if (viewModel?.UpdateTaskDoneCommand.CanExecute(task) == true)
            {
                viewModel.UpdateTaskDoneCommand.Execute(task);
            }
        }
    }
}
