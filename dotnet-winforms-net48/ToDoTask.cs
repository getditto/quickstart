using Newtonsoft.Json;
using System;

namespace Taskapp.WinForms.Net48
{
    /// <summary>
    /// Representation of a document in the Ditto 'tasks' collection.
    /// </summary>
    /// <remarks>
    /// This class is named <c>ToDoTask</c> rather than <c>Task</c> to avoid
    /// conflicts with the <c>System.Threading.Tasks.Task</c> class.
    /// </remarks>
    public class ToDoTask
    {
        [JsonProperty("_id")]
        public string Id { get; set; }

        [JsonProperty("title")]
        public string Title { get; set; }

        [JsonProperty("done")]
        public bool Done { get; set; }

        [JsonProperty("deleted")]
        public bool Deleted { get; set; }

        override public string ToString() => Title;
    }
}
