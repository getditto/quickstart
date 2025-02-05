using System;

namespace DittoMauiTasksApp.Utils
{
    public class PopupService : IPopupService
    {
        public Task<string> DisplayPromptAsync(string title, string message, string placeholder, string initialValue = "")
        {
            Page page = Application.Current?.Windows[0].Page;
            return page.DisplayPromptAsync(title, message, placeholder: placeholder, initialValue: initialValue);
        }
    }
}

