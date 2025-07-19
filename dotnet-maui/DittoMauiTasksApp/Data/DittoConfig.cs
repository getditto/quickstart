namespace DittoMauiTasksApp.Data;

public record DittoConfig
{
    public string AppId { get; set; } = "";
    public string PlaygroundToken { get; set; } = "";
    public string AuthUrl { get; set; } = "";
    public string WebsocketUrl { get; set; } = "";
}