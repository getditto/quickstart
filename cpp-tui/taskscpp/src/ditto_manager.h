#ifndef DITTO_QUICKSTART_DITTO_MANAGER_H
#define DITTO_QUICKSTART_DITTO_MANAGER_H

#include <memory>
#include <string>

#include "Ditto.h"

/// Owns everything about configuring and running the Ditto instance: opening
/// it, wiring up development-mode authentication, and controlling sync. It
/// knows nothing about tasks.
///
/// It vends the configured Ditto instance (via `get_ditto()`) so the rest of
/// the app can use the real Ditto API directly.
class DittoManager {
public:
  /// Returns a string identifying the version of the Ditto SDK.
  static std::string get_ditto_sdk_version();

  /// Open and configure a Ditto instance. Does not start sync; call
  /// `start_sync()` for that.
  DittoManager(std::string database_id, std::string ditto_development_token,
               std::string server_url, std::string offline_license_token,
               std::string ditto_persistence_dir);

  virtual ~DittoManager() noexcept;

  // DittoManager owns the underlying Ditto instance and its sync lifecycle, so
  // it is non-copyable and non-movable. Share it via `std::shared_ptr` when
  // more than one component needs it (see TasksRepository).
  DittoManager(const DittoManager &) = delete;
  DittoManager(DittoManager &&) = delete;

  DittoManager &operator=(const DittoManager &) = delete;
  DittoManager &operator=(DittoManager &&) = delete;

  /// Start syncing data with other devices.
  void start_sync();

  /// Stop syncing data with other devices.
  void stop_sync();

  /// Return true if currently syncing with other devices.
  bool is_sync_active() const;

  /// The configured Ditto instance, so callers can use the real Ditto API
  /// directly.
  std::shared_ptr<ditto::Ditto> get_ditto() const;

private:
  class Impl; // private implementation class ("pimpl pattern")
  std::shared_ptr<Impl> impl;
};

#endif // DITTO_QUICKSTART_DITTO_MANAGER_H
