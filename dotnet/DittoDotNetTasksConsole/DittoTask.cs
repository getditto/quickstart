using System;
using System.Text.Json.Serialization;

/// <summary>
/// Representation of a document in the Ditto 'tasks' collection.
/// </summary>
public class DittoTask
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
