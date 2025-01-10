using System.Text.Json.Serialization;
using CommunityToolkit.Mvvm.ComponentModel;
using DittoSDK;

namespace DittoMauiTasksApp
{
    public partial class DittoTask : ObservableObject
    {
        public const string CollectionName = "tasks";

        [ObservableProperty]
        [property: JsonPropertyName("_id")]
        string id;

        [ObservableProperty]
        [property: JsonPropertyName("body")]
        string body;

        [ObservableProperty]
        [property: JsonPropertyName("isCompleted")]
        bool isCompleted;

        partial void OnIsCompletedChanged(bool value)
        {
            var ditto = Utils.ServiceProvider.GetService<Ditto>();

            var updateQuery = $"UPDATE {CollectionName} " +
                $"SET isCompleted = {value} " +
                $"WHERE _id = '{Id}' AND isCompleted != {value}";
            ditto.Store.ExecuteAsync(updateQuery);
        }
    }
}
