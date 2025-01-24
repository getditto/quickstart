using System;
using System.Threading.Tasks;

using Generated;
using Terminal.Gui;

public static class Program
{
    public static async Task Main(string[] args)
    {
        try
        {
            using var peer = await TasksPeer.Create(
                EnvConstants.DITTO_APP_ID, EnvConstants.DITTO_PLAYGROUND_TOKEN);
            RunTerminalGui(peer);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine(ex);
        }
    }

    private static void RunTerminalGui(TasksPeer peer)
    {
        try
        {
            Application.Init();
            Application.Top.Add(new TasksWindow(peer));
            Application.Run();
        }
        finally
        {
            Application.Shutdown();
        }
    }
}
