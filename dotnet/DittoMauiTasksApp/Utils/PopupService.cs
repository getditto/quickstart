using System;

namespace DittoMauiTasksApp.Utils
{
    public class PopupService : IPopupService
    {
        public Task<string> DisplayPromptAsync(string title, string message)
        {
            Page page = Application.Current?.MainPage;
            return page.DisplayPromptAsync(title, message);
        }
    }
}

