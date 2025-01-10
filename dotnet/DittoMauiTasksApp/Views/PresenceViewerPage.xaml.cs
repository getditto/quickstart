using System;
using DittoSDK;
using Microsoft.Maui.Controls;

namespace DittoMauiTasksApp;

public partial class PresenceViewerPage : ContentPage
{
    public Ditto Ditto { get; set; }

    public PresenceViewerPage(Ditto ditto)
    {
        this.Ditto = ditto;

        InitializeComponent();
    }

    async void ToolbarItem_Clicked(System.Object sender, System.EventArgs e)
    {
        await Shell.Current.GoToAsync("//MainPage");
    }
}
