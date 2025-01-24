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
            using var peer = await TasksPeer.CreateTasksPeer(
                EnvConstants.DITTO_APP_ID, EnvConstants.DITTO_PLAYGROUND_TOKEN);
            RunTerminalUi(peer);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine(ex);
        }
    }

    private static void RunTerminalUi(TasksPeer peer)
    {
        try
        {
            Application.Init();
            Application.Top.Add(new TasksWindow(peer));
            Application.Top.KeyPress += (keyEventArgs) =>
            {
                if (keyEventArgs.KeyEvent.Key == Key.Q)
                {
                    Application.RequestStop();
                }
            };
            Application.Run();
        }
        finally
        {
            Application.Shutdown();
        }
    }
}
