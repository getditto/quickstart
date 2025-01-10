using System;
namespace DittoMauiTasksApp.Utils
{
    public interface IPopupService
    {
        Task<string> DisplayPromptAsync(string title, string message);
    }
}

