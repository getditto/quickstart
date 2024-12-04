#ifndef DITTO_QUICKSTART_TASKS_TUI_H
#define DITTO_QUICKSTART_TASKS_TUI_H

#ifdef DITTO_QUICKSTART_TUI

#include "tasks_peer.h"

#include <vector>

/// Text-based interactive user interface for the Tasks application.
class TasksTui {
public:
  TasksTui();

  void run(TasksPeer *peer);

private:
  std::vector<Task> tasks;
};

#endif // DITTO_QUICKSTART_TUI

#endif // DITTO_QUICKSTART_TASKS_TUI_H
