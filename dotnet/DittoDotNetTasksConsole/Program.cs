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
            using var peer = await DittoTasksPeer.CreateTasksPeer(
                EnvConstants.DITTO_APP_ID, EnvConstants.DITTO_PLAYGROUND_TOKEN);
            RunConsoleUi(peer);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine(ex);
        }
    }

    private static void RunConsoleUi(DittoTasksPeer peer)
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
