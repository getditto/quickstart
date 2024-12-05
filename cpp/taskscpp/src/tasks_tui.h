#ifndef DITTO_QUICKSTART_TASKS_TUI_H
#define DITTO_QUICKSTART_TASKS_TUI_H

#ifdef DITTO_QUICKSTART_TUI

#include "tasks_peer.h"

#include <memory>

/// Text-based interactive user interface for the Tasks application.
class TasksTui {
public:
  TasksTui();

  ~TasksTui();

  /// Run the TUI, using the specified TasksPeer object to access data.
  ///
  /// The `peer` reference must iremain valid until `run()` returns.
  void run(TasksPeer &peer);

private:
  class Impl;
  std::shared_ptr<Impl> impl; // private implementation
};

#endif // DITTO_QUICKSTART_TUI

#endif // DITTO_QUICKSTART_TASKS_TUI_H
