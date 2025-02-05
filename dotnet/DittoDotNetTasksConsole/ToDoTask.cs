using System;
using System.Text.Json.Serialization;

/// <summary>
/// Representation of a document in the Ditto 'tasks' collection.
/// </summary>
/// <remarks>
/// This class is named <c>ToDoTask</c> rather than <c>Task</c> to avoid
/// conflicts with the <c>System.Threading.Tasks.Task</c> class.
/// </remarks>
public class ToDoTask
{
    [JsonPropertyName("_id")]
    public string Id { get; set; }

    [JsonPropertyName("title")]
    public string Title { get; set; }

    [JsonPropertyName("done")]
    public bool Done { get; set; }

    [JsonPropertyName("deleted")]
    public bool Deleted { get; set; }

    override public string ToString() => Title;
}
