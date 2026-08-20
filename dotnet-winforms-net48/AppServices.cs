using System;
using System.Threading.Tasks;

namespace Taskapp.WinForms.Net48
{
    /// <summary>
    /// Singleton composition root that owns the <see cref="DittoManager"/> and
    /// <see cref="TasksRepository"/> and vends them for application-wide access.
    /// </summary>
    /// <remarks>
    /// This holder deliberately does NOT wrap the manager or repository with
    /// pass-through methods. Callers use the exposed <see cref="DittoManager"/> and
    /// <see cref="TasksRepository"/> directly, keeping the quickstart free of an
    /// abstraction layer over Ditto's API.
    /// </remarks>
    public sealed class AppServices : IDisposable
    {
        private static readonly Lazy<AppServices> _instance =
            new Lazy<AppServices>(() => new AppServices());

        public static AppServices Instance => _instance.Value;

        private DittoManager _dittoManager;
        private TasksRepository _tasksRepository;
        private bool _isInitialized;

        private AppServices()
        {
        }

        /// <summary>
        /// Initializes the Ditto instance and tasks repository with configuration values.
        /// </summary>
        public async Task InitializeAsync(string databaseId, string developmentToken, string serverUrl)
        {
            if (_isInitialized)
                throw new InvalidOperationException("AppServices is already initialized");

            _dittoManager = await DittoManager.Create(databaseId, developmentToken, serverUrl);
            _tasksRepository = new TasksRepository(_dittoManager);
            _isInitialized = true;
        }

        /// <summary>
        /// Gets whether the service has been initialized.
        /// </summary>
        public bool IsInitialized => _isInitialized;

        /// <summary>
        /// The Ditto instance manager. Use it for instance/config/sync concerns.
        /// </summary>
        public DittoManager DittoManager
        {
            get
            {
                EnsureInitialized();
                return _dittoManager;
            }
        }

        /// <summary>
        /// The tasks repository. Use it for subscription/observer/CRUD concerns.
        /// </summary>
        public TasksRepository TasksRepository
        {
            get
            {
                EnsureInitialized();
                return _tasksRepository;
            }
        }

        /// <summary>
        /// Ensures the service has been initialized.
        /// </summary>
        private void EnsureInitialized()
        {
            if (!_isInitialized)
                throw new InvalidOperationException("AppServices has not been initialized. Call InitializeAsync first.");
        }

        public void Dispose()
        {
            _tasksRepository = null;

            if (_dittoManager != null)
            {
                _dittoManager.Dispose();
                _dittoManager = null;
            }

            _isInitialized = false;
        }
    }
}
