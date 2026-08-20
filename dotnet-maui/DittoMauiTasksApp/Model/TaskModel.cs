using System.Text.Json.Serialization;
using CommunityToolkit.Mvvm.ComponentModel;
using DittoSDK;

namespace DittoMauiTasksApp
{
    /// <summary>
    /// Representation of a document in the Ditto 'tasks' collection.
    /// </summary>
    /// <remarks>
    /// This class is named <c>TaskModel</c> rather than <c>Task</c> to avoid
    /// conflicts with the <c>System.Threading.Tasks.Task</c> class. It derives
    /// from <c>ObservableObject</c> so the MAUI UI is notified of in-place
    /// property changes (e.g. two-way binding on the "done" checkbox).
    /// </remarks>
    public partial class TaskModel : ObservableObject
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
    }
}
