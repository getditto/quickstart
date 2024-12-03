#ifndef DITTO_QUICKSTART_TASKS_TUI_H
#define DITTO_QUICKSTART_TASKS_TUI_H

#include "tasks_peer.h"

/// Text-based interactive user interface for the Tasks application.
class TasksTui {
public:
  void run(TasksPeer *peer);
};

#endif // DITTO_QUICKSTART_TASKS_TUI_H
