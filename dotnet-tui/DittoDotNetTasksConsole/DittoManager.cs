using System;
using System.Threading.Tasks;

using DittoSDK;
using DittoSDK.Auth;

/// <summary>
/// Manages the lifecycle and configuration of a Ditto instance: config, open,
/// auth, and starting/stopping sync. It vends a configured, already-running
/// <see cref="Ditto"/> instance (via the <see cref="Ditto"/> property) for the
/// tasks repository to use directly.
/// </summary>
/// <remarks>
/// This class deliberately does NOT wrap Ditto's APIs (store/sync/etc.). The
/// repository calls the real Ditto API — e.g. <c>dittoManager.Ditto.Store.ExecuteAsync(...)</c>
/// — so the quickstart shows how to use Ditto directly rather than modeling an
/// abstraction layer over it.
/// </remarks>
public class DittoManager : IDisposable
{
    public string DatabaseId { get; private set; }
    public string DevelopmentToken { get; private set; }
    public string ServerUrl { get; private set; }

    public bool IsSyncActive => _ditto.Sync.IsActive;

    private readonly Ditto _ditto;

    /// <summary>
    /// The configured, already-running Ditto instance. The tasks repository uses
    /// this to call the real Ditto API directly.
    /// </summary>
    public Ditto Ditto => _ditto;

    /// <summary>
    /// Creates a new synchronizing DittoManager instance.
    /// </summary>
    public static Task<DittoManager> Create(
        string databaseId,
        string developmentToken,
        string serverUrl)
    {
        var dittoManager = new DittoManager(databaseId, developmentToken, serverUrl);
        dittoManager.Authenticate();
        dittoManager.StartSync();

        return Task.FromResult(dittoManager);
    }

    private void Authenticate()
    {
        _ditto.Auth.ExpirationHandler = async (ditto, secondsRemaining) =>
        {
            // Authenticate when token is expiring
            try
            {
                await ditto.Auth.LoginAsync(
                    // Your development token, replace with your actual token
                    DevelopmentToken,
                    // Use DittoAuthenticationProvider.Development, or your actual provider
                    DittoAuthenticationProvider.Development
                );
            }
            catch (Exception error)
            {
                Console.WriteLine($"Authentication failed: {error}");
            }
        };
    }

    /// <summary>
    /// Constructor
    /// </summary>
    /// <param name="databaseId">Ditto database ID</param>
    /// <param name="developmentToken">Ditto development token</param>
    /// <param name="serverUrl">Ditto Server URL</param>
    private DittoManager(string databaseId, string developmentToken, string serverUrl)
    {
        DatabaseId = databaseId;
        DevelopmentToken = developmentToken;
        ServerUrl = serverUrl;

        var config = new DittoConfig(
            DatabaseId,
            new DittoConfigConnect.Server(new Uri(serverUrl))
        );

        _ditto = Ditto.Open(config);
    }

    public void Dispose()
    {
        _ditto.Dispose();
        GC.SuppressFinalize(this);
    }

    /// <summary>
    /// Start synchronizing with other peers and the Ditto server.
    /// </summary>
    public void StartSync()
    {
        _ditto.Sync.Start();
    }

    /// <summary>
    /// Stop synchronizing.
    /// </summary>
    public void StopSync()
    {
        // Stop the sync engine only. The subscription is owned by TasksRepository
        // and stays registered across a sync on/off toggle.
        _ditto.Sync.Stop();
    }
}
