#ifndef DITTO_QUICKSTART_TASKS_TUI_H
#define DITTO_QUICKSTART_TASKS_TUI_H

#ifdef DITTO_QUICKSTART_TUI

#include "ditto_manager.h"
#include "tasks_repository.h"

#include <memory>

/// Text-based interactive user interface for the Tasks application.
///
/// The UI is handed the repository (for task data) and a shared DittoManager
/// (for sync control) directly. Both share ownership of the same manager via
/// `std::shared_ptr`, so there is a single source of truth for which manager
/// is in use.
class TasksTui {
public:
  TasksTui(TasksRepository &repository, std::shared_ptr<DittoManager> manager);

  ~TasksTui();

  void run();

private:
  class Impl;
  std::shared_ptr<Impl> impl; // private implementation
};

#endif // DITTO_QUICKSTART_TUI

#endif // DITTO_QUICKSTART_TASKS_TUI_H
