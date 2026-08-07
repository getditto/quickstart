using DittoMauiTasksApp.ViewModels;

namespace DittoMauiTasksApp;

public partial class TasksPage : ContentPage
{
    public TasksPage(TasksPageviewModel viewModel)
    {
        InitializeComponent();

        BindingContext = viewModel;

        // Dispose the view model (which cancels its Ditto store observer) when
        // the page is removed from the visual tree. Note: MAUI may raise
        // Unloaded/Loaded in cycles; in this single-page app the page is not
        // expected to reload, so disposing here does not strand a live page.
        Unloaded += (_, _) => (BindingContext as IDisposable)?.Dispose();
    }

    // Event handler invoked when a checkbox on the page is checked or unchecked.
    private void OnCheckBoxCheckedChanged(object sender, CheckedChangedEventArgs e)
    {
        if (sender is CheckBox checkBox && checkBox.BindingContext is TaskModel task)
        {
            if (!checkBox.IsVisible || !checkBox.IsEnabled)
            {
                return;
            }

            var viewModel = BindingContext as TasksPageviewModel;
            if (viewModel?.UpdateTaskDoneCommand.CanExecute(task) == true)
            {
                viewModel.UpdateTaskDoneCommand.Execute(task);
            }
        }
    }
}
