#include "ditto_manager.h"
#include "tasks_log.h"

#include "Ditto.h"

#include <cstdint>
#include <iostream>
#include <stdexcept>

using namespace std;

/// Initialize a Ditto instance.
static shared_ptr<ditto::Ditto> init_ditto(string database_id,
                                           string development_token,
                                           string server_url,
                                           string persistence_dir) {
  try {
    auto config = ditto::DittoConfig::default_config()
                      .set_database_id(std::move(database_id))
                      .set_persistence_directory(std::move(persistence_dir))
                      .set_server_connect(std::move(server_url));

    auto ditto = ditto::Ditto::open(std::move(config));

    // Log in with the development token using the built-in development
    // authentication provider. The expiration handler re-authenticates
    // automatically when the credential is about to expire.
    const auto provider = ditto::Authenticator::get_development_provider();
    if (auto auth = ditto->get_auth()) {
      auth->set_expiration_handler(
          [development_token, provider](ditto::Ditto &d, uint32_t) {
            if (auto a = d.get_auth()) {
              a->login(development_token, provider,
                       [](std::unique_ptr<std::string>,
                          std::unique_ptr<ditto::DittoError> err) {
                         if (err) {
                           log_error("Failed to re-authenticate: " +
                                     string(err->what()));
                         }
                       });
            }
          });

      auth->login(development_token, provider,
                  [](std::unique_ptr<std::string>,
                     std::unique_ptr<ditto::DittoError> err) {
                    if (err) {
                      log_error("Failed to authenticate: " +
                                string(err->what()));
                    }
                  });
    }

    return ditto;
  } catch (const exception &err) {
    throw runtime_error("unable to initialize Ditto: " + string(err.what()));
  }
}

// Private implementation of the DittoManager class.
// NOLINTNEXTLINE(cppcoreguidelines-special-member-functions)
class DittoManager::Impl {
public:
  shared_ptr<ditto::Ditto> ditto;

  Impl(string database_id, string development_token, string server_url,
       string persistence_dir)
      : ditto(init_ditto(std::move(database_id), std::move(development_token),
                         std::move(server_url), std::move(persistence_dir))) {}

  ~Impl() noexcept {
    try {
      stop_sync();
    } catch (const exception &err) {
      std::cerr << "Failed to destroy Ditto manager instance: " +
                       string(err.what())
                << std::endl;
    }
  }

  void start_sync() {
    if (is_sync_active()) {
      return;
    }

    ditto->get_sync().start();
  }

  void stop_sync() {
    if (!is_sync_active()) {
      return;
    }

    ditto->get_sync().stop();
  }

  bool is_sync_active() const { return ditto->get_sync().is_active(); }

}; // class DittoManager::Impl

DittoManager::DittoManager(string database_id, string development_token,
                           string server_url, string persistence_dir)
    : impl(std::make_shared<Impl>(
          std::move(database_id), std::move(development_token),
          std::move(server_url), std::move(persistence_dir))) {}

DittoManager::~DittoManager() noexcept {
  try {
    log_debug("Destroying DittoManager instance");
  } catch (const std::exception &) { // NOLINT(bugprone-empty-catch)
  }
}

void DittoManager::start_sync() { impl->start_sync(); }

void DittoManager::stop_sync() { impl->stop_sync(); }

bool DittoManager::is_sync_active() const { return impl->is_sync_active(); }

shared_ptr<ditto::Ditto> DittoManager::get_ditto() const { return impl->ditto; }

string DittoManager::get_ditto_sdk_version() {
  return ditto::Ditto::get_version();
}
