#ifndef DITTO_QUICKSTART_TASKS_TUI_H
#define DITTO_QUICKSTART_TASKS_TUI_H

#ifdef DITTO_QUICKSTART_TUI

#include "tasks_repository.h"

#include <memory>

/// Text-based interactive user interface for the Tasks application.
///
/// The UI drives everything through the repository, including sync control,
/// which it reaches via `TasksRepository::ditto_manager()`. It is deliberately
/// not handed a separate DittoManager, so there is a single source of truth
/// for which manager is in use.
class TasksTui {
public:
  explicit TasksTui(TasksRepository &repository);

  ~TasksTui();

  void run();

private:
  class Impl;
  std::shared_ptr<Impl> impl; // private implementation
};

#endif // DITTO_QUICKSTART_TUI

#endif // DITTO_QUICKSTART_TASKS_TUI_H
