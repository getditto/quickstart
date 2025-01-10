using System.Text.Json.Serialization;
using CommunityToolkit.Mvvm.ComponentModel;
using DittoSDK;

namespace DittoMauiTasksApp
{
    public partial class DittoTask : ObservableObject
    {
        [ObservableProperty]
        [property: JsonPropertyName("_id")]
        string id;

        [ObservableProperty]
        [property: JsonPropertyName("title")]
        string title;

        [ObservableProperty]
        [property: JsonPropertyName("done")]
        bool done;

        [ObservableProperty]
        [property: JsonPropertyName("deleted")]
        bool deleted;

        partial void OnDoneChanged(bool value)
        {
            var ditto = Utils.ServiceProvider.GetService<Ditto>();

            var updateQuery = $"UPDATE tasks " +
                $"SET done = {value} " +
                $"WHERE _id = '{Id}' AND done != {value}";
            ditto.Store.ExecuteAsync(updateQuery);
        }
    }
}
