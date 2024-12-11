#include "env.h"

#include "task.h"
#include "tasks_log.h"
#include "tasks_peer.h"

#ifdef DITTO_QUICKSTART_TUI
#include "tasks_tui.h"
#endif

// We want to allow commas in command-line option values, so define
// CXXOPTS_VECTOR_DELIMITER to be something that is unlikely to appear in the
// values.
// NOLINTNEXTLINE(cppcoreguidelines-macro-usage)
#define CXXOPTS_VECTOR_DELIMITER '\n'
#include "cxxopts.hpp"

#include <chrono>
#include <csignal>
#include <cstdlib>
#include <exception>
#include <iostream>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

using namespace std;

// Flag set if Ctrl+C is pressed
// NOLINTNEXTLINE(cppcoreguidelines-avoid-non-const-global-variables)
static volatile sig_atomic_t sigint_caught = 0;

// Signal handler for Ctrl+C
extern "C" void taskscli_main_sigint_handler(int signal) {
  if (signal == SIGINT) {
    sigint_caught = 1;
  }
}

// When specifying a task ID, user must provide at least this many characters.
// (This is arbitrary, but we want to avoid accidental typos.)
static constexpr size_t TASK_ID_MIN_SIZE = 5;

/// Throw an exception if task ID substring is too short.
static void validate_task_substring(const string &task_id_substring) {
  if (task_id_substring.size() < TASK_ID_MIN_SIZE) {
    ostringstream oss;
    oss << "TASK_ID must be at least " << TASK_ID_MIN_SIZE << " characters";
    throw invalid_argument(oss.str());
  }
}

int main(int argc, const char *argv[]) {
  std::string export_log_path;

  try {
    cxxopts::Options options("taskscli",
                             "A utility for managing and synchronizing tasks");

    // clang-format off
    options.add_options("Command")
      ("h,help", "Print usage")
#ifdef DITTO_QUICKSTART_TUI
      ("tui", "Run the text-based user interface (default)")
#endif
      ("a,add", "Add a new task",
        cxxopts::value<vector<string>>(), "TITLE")
      ("c,complete", "Mark a task as completed",
        cxxopts::value<vector<string>>(), "TASK_ID")
      ("i,incomplete", "Mark a task as incomplete",
        cxxopts::value<vector<string>>(), "TASK_ID")
      ("t,toggle", "Toggle the completion status of a task",
        cxxopts::value<vector<string>>(), "TASK_ID")
      ("title", "Edit the title of a task",
        cxxopts::value<vector<string>>(), "TASK_ID,TITLE")
      ("d,delete", "Delete a task",
        cxxopts::value<vector<string>>(), "TASK_ID")
      ("l,list", "List tasks")
      ("list-all", "List all tasks, including those marked deleted")
      ("m,monitor", "Monitor tasks for changes")
      ("cleanup", "Evict all deleted tasks from local store")
      ("query", "Run a DQL query using the peer's Ditto instance",
        cxxopts::value<vector<string>>(), "STRING");

    options.add_options("Sync")
      ("pre", "Number of seconds to synchronize before the operation",
        cxxopts::value<unsigned>()->default_value("5"), "N")
      ("post", "Number of seconds to synchronize after the operation",
        cxxopts::value<unsigned>()->default_value("5"), "N")
      ("p,persistence-directory", "Persistence directory",
        cxxopts::value<string>(), "PATH")
      ("app-id", "Ditto App ID",
        cxxopts::value<string>(), "APP_ID")
      ("online-playground-token", "Ditto Online Playground token",
        cxxopts::value<string>(), "TOKEN")
      ("enable-cloud-sync", "Enable cloud synchronization");

    options.add_options("Logging")
      ("q,quiet", "Disable non-logging output")
      ("error", "Error-level logging")
      ("warning", "Warning-level logging (default)")
      ("info", "Info-level logging")
      ("debug", "Debug-level logging")
      ("v,verbose","Trace-level logging")
      ("log","Log file output path",
        cxxopts::value<string>(), "PATH")
      ("export", "Export-log file path",
        cxxopts::value<string>(), "PATH")
      ("ditto-sdk-version", "Print the Ditto SDK version");
    // clang-format on

    const auto opt_parse = options.parse(argc, argv);

    // If no other commands are specified, then "tui" is the default behavior.
    const vector<string> commands{"add",      "complete", "incomplete",
                                  "title",    "delete",   "list",
                                  "list-all", "monitor",  "cleanup",
                                  "query",    "toggle",   "ditto-sdk-version"};
    bool found_non_tui_command = false;
    for (const auto &command : commands) {
      if (opt_parse.count(command) > 0) {
        found_non_tui_command = true;
        break;
      }
    }

#ifdef DITTO_QUICKSTART_TUI
    bool found_tui_command = opt_parse.count("tui") > 0;

    if ((found_tui_command && found_non_tui_command) ||
        opt_parse.count("help") > 0) {
      cout << options.help() << endl;
      exit(EXIT_SUCCESS);
    }
#else
    if ((!found_non_tui_command) || opt_parse.count("help") > 0) {
      cout << options.help() << endl;
      exit(EXIT_SUCCESS);
    }
#endif

    if (opt_parse.count("ditto-sdk-version") > 0) {
      cout << "Ditto SDK version: " << TasksPeer::get_ditto_sdk_version()
           << endl;
      exit(EXIT_SUCCESS);
    }

    // Logging configuration
    ditto::LogLevel log_level = ditto::LogLevel::warning;
    if (opt_parse.count("error") > 0) {
      log_level = ditto::LogLevel::error;
    }
    if (opt_parse.count("warning") > 0) {
      log_level = ditto::LogLevel::warning;
    }
    if (opt_parse.count("info") > 0) {
      log_level = ditto::LogLevel::info;
    }
    if (opt_parse.count("debug") > 0) {
      log_level = ditto::LogLevel::debug;
    }
    if (opt_parse.count("verbose") > 0) {
      log_level = ditto::LogLevel::verbose;
    }
    set_minimum_log_level(log_level);

    if (opt_parse.count("log") > 0) {
      set_log_file(opt_parse["log"].as<string>());
    }

    if (opt_parse.count("export") > 0) {
      export_log_path = opt_parse["export"].as<string>();
    }

    // Ditto configuration and sync options
    const auto pre_sync_sec = opt_parse["pre"].as<unsigned>();
    const auto post_sync_sec = opt_parse["post"].as<unsigned>();
    const auto persistence_dir =
        opt_parse.count("persistence-directory") > 0
            ? opt_parse["persistence-directory"].as<string>()
            : "";
    const auto app_id = opt_parse.count("app-id") > 0
                            ? opt_parse["app-id"].as<string>()
                            : DITTO_APP_ID;
    const auto online_playground_token =
        opt_parse.count("online-playground-token") > 0
            ? opt_parse["online-playground-token"].as<string>()
            : DITTO_PLAYGROUND_TOKEN;
    const auto enable_cloud_sync = opt_parse.count("enable-cloud-sync") > 0;

    const auto quiet = opt_parse["quiet"].as<bool>();

    // Set this true if we make modifications and need to allow post-sync time.
    bool need_post_sync = false;

    // The peer is destroyed at the end of this scope
    {
      TasksPeer peer(app_id, online_playground_token, enable_cloud_sync,
                     persistence_dir);
      peer.insert_initial_tasks();
      peer.start_sync();

#ifdef DITTO_QUICKSTART_TUI
      if (found_tui_command || !found_non_tui_command) {
        TasksTui tui(peer);
        tui.run();
      } else
#endif
      {
        // A thread must hold mtx while using peer or writing output.
        mutex mtx;

        shared_ptr<TasksPeer::TasksObserver> tasks_observer;
        if (opt_parse.count("monitor") > 0) {
          tasks_observer = peer.register_tasks_observer(
              [quiet, &mtx](const vector<Task> &tasks) {
                if (!quiet && !tasks.empty()) {
                  lock_guard<mutex> lock(mtx);
                  cout << "-------------- Tasks Sync --------------" << endl;
                  for (const auto &task : tasks) {
                    cout << task._id << " | " << (task.done ? "X" : "O")
                         << " | " << task.title << endl;
                  }
                  cout << "----------------------------------------" << endl;
                }
              });
        }

        // Allow initial synchronization in background.
        if (pre_sync_sec > 0) {
          if (!quiet) {
            cout << "Synchronizing tasks..." << endl;
          }
          this_thread::sleep_for(chrono::seconds(pre_sync_sec));
        }

        if (opt_parse.count("add") > 0) {
          need_post_sync = true;
          for (const auto &title : opt_parse["add"].as<vector<string>>()) {
            try {
              if (title.empty()) {
                throw invalid_argument("TITLE must not be empty");
              }

              lock_guard<mutex> lock(mtx);
              const auto task_id = peer.add_task(title, false);

              if (!quiet) {
                cout << "Added task: " << task_id << ": " << title << endl;
              }
            } catch (const exception &err) {
              cerr << "error: add " << title << ": " << err.what() << endl;
            }
          }
        }

        if (opt_parse.count("complete") > 0) {
          need_post_sync = true;
          for (const auto &task_id_substring :
               opt_parse["complete"].as<vector<string>>()) {
            try {
              validate_task_substring(task_id_substring);

              lock_guard<mutex> lock(mtx);
              const auto task = peer.find_matching_task(task_id_substring);
              peer.mark_task_complete(task._id, true);

              if (!quiet) {
                cout << "Marked task complete: " << task._id << endl;
              }
            } catch (const exception &err) {
              cerr << "error: complete " << task_id_substring << ": "
                   << err.what() << endl;
            }
          }
        }

        if (opt_parse.count("incomplete") > 0) {
          need_post_sync = true;
          for (const auto &task_id_substring :
               opt_parse["incomplete"].as<vector<string>>()) {
            try {
              validate_task_substring(task_id_substring);

              lock_guard<mutex> lock(mtx);
              const auto task = peer.find_matching_task(task_id_substring);
              peer.mark_task_complete(task._id, false);

              if (!quiet) {
                cout << "Marked task incomplete: " << task._id << endl;
              }
            } catch (const exception &err) {
              cerr << "error: incomplete " << task_id_substring << ": "
                   << err.what() << endl;
            }
          }
        }

        if (opt_parse.count("toggle") > 0) {
          need_post_sync = true;
          for (const auto &task_id_substring :
               opt_parse["toggle"].as<vector<string>>()) {
            try {
              validate_task_substring(task_id_substring);

              lock_guard<mutex> lock(mtx);
              const auto task = peer.find_matching_task(task_id_substring);
              peer.mark_task_complete(task._id, !task.done);

              if (!quiet) {
                cout << "Toggled task completion: " << task._id << endl;
              }
            } catch (const exception &err) {
              cerr << "error: toggle " << task_id_substring << ": "
                   << err.what() << endl;
            }
          }
        }

        if (opt_parse.count("title") > 0) {
          need_post_sync = true;
          for (const auto &edit : opt_parse["title"].as<vector<string>>()) {
            try {
              // Split the string into task ID and title
              const auto comma_pos = edit.find(',');
              if (comma_pos == string::npos) {
                throw invalid_argument(
                    "Argument must be of the form 'TASK_ID,TITLE'");
              }

              const auto task_id_substring = edit.substr(0, comma_pos);
              validate_task_substring(task_id_substring);

              const auto title = edit.substr(comma_pos + 1);
              if (title.empty()) {
                throw invalid_argument("Title must not be empty");
              }

              lock_guard<mutex> lock(mtx);
              const auto task = peer.find_matching_task(task_id_substring);
              peer.update_task_title(task._id, title);

              if (!quiet) {
                cout << "Changed title of " << task._id << " to '" << title
                     << "'" << endl;
              }
            } catch (const exception &err) {
              cerr << "error: title " << edit << ": " << err.what() << endl;
            }
          }
        }

        if (opt_parse.count("delete") > 0) {
          need_post_sync = true;
          for (const auto &task_id_substring :
               opt_parse["delete"].as<vector<string>>()) {
            try {
              validate_task_substring(task_id_substring);

              lock_guard<mutex> lock(mtx);
              const auto task = peer.find_matching_task(task_id_substring);
              peer.delete_task(task._id);

              if (!quiet) {
                cout << "Deleted task: " << task._id << endl;
              }
            } catch (const exception &err) {
              cerr << "error: delete " << task_id_substring << ": "
                   << err.what() << endl;
            }
          }
        }

        if (opt_parse.count("cleanup") > 0) {
          need_post_sync = true;
          try {
            lock_guard<mutex> lock(mtx);
            peer.evict_deleted_tasks();
            if (!quiet) {
              cout << "Evicted all deleted tasks" << endl;
            }
          } catch (const exception &err) {
            cerr << "error: cleanup: " << err.what() << endl;
          }
        }

        if (opt_parse.count("query") > 0) {
          need_post_sync = true;
          for (const auto &query : opt_parse["query"].as<vector<string>>()) {
            try {
              lock_guard<mutex> lock(mtx);
              const auto result = peer.execute_dql_query(query);
              if (!quiet) {
                cout << "[" << query << "] result: \n" << result << endl;
              }
            } catch (const exception &err) {
              cerr << "error: query [" << query << "]: " << err.what() << endl;
            }
          }
        }

        auto include_deleted_tasks = opt_parse.count("list-all") > 0;
        if (opt_parse.count("list") > 0 || opt_parse.count("list-all") > 0) {
          lock_guard<mutex> lock(mtx);
          auto tasks = peer.get_tasks(include_deleted_tasks);
          if (tasks.empty()) {
            if (!quiet) {
              cout << "No tasks found" << endl;
            }
          } else {
            if (!quiet) {
              for (const auto &task : tasks) {
                cout << task._id << " | " << (task.done ? "X" : "O") << " | "
                     << task.title << (task.deleted ? " (deleted)" : "")
                     << endl;
              }
            }
          }
        }

        if (opt_parse.count("monitor") > 0) {
          if (!quiet) {
            lock_guard<mutex> lock(mtx);
            cout << "Monitoring tasks for changes. Press Ctrl+C to stop."
                 << endl;
          }
          signal(SIGINT, taskscli_main_sigint_handler);
          while (sigint_caught == 0) {
            this_thread::sleep_for(chrono::milliseconds(200));
          }
          signal(SIGINT, SIG_DFL);

          if (!quiet) {
            cout << "Monitoring canceled" << endl;
          }

          // Cancel final sync if user is watching changes.
          need_post_sync = false;
        }

        if (need_post_sync && post_sync_sec > 0) {
          if (!quiet) {
            cout << "Synchronizing tasks..." << endl;
          }
          this_thread::sleep_for(chrono::seconds(post_sync_sec));
        }

        tasks_observer.reset();
      } // !found_tui_command

      peer.stop_sync();
    } // peer destroyed

    if (!export_log_path.empty()) {
      export_log(export_log_path);
    }
  } catch (const std::exception &err) {
    cerr << "error: " << err.what() << endl;

    if (!export_log_path.empty()) {
      try {
        export_log(export_log_path);
      } catch (const std::exception &err) {
        cerr << "error: failed to export log: " << err.what() << endl;
      }
    }

    exit(EXIT_FAILURE);
  }

  return 0;
}
