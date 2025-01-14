using System;

namespace DittoMauiTasksApp.Utils
{
    public class PopupService : IPopupService
    {
        public Task<string> DisplayPromptAsync(string title, string message, string placeholder, string initialValue = "")
        {
            Page page = Application.Current?.MainPage;
            return page.DisplayPromptAsync(title, message, placeholder: placeholder, initialValue: initialValue);
        }
    }
}

