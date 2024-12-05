#ifdef DITTO_QUICKSTART_TUI

#include "tasks_tui.h"

#include <cstdio>

#include <unistd.h>

#include "ftxui/component/component.hpp"
#include "ftxui/component/loop.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"

using namespace ftxui;

using lock_guard = std::lock_guard<std::mutex>;

class TasksTui::Impl {
private:
  std::mutex mutex;
  std::vector<::Task> tasks;
  std::vector<::Task> last_update_tasks;
  Component container;
  ScreenInteractive screen;

  void update_tasks(std::vector<::Task> new_tasks) {
    if (new_tasks == tasks) {
      return;
    }

    // TODO: Need to maintain currently selected task when updating.

    container->DetachAllChildren();

    tasks = new_tasks;

    for (auto &task : tasks) {
      auto checkbox = Checkbox(task.title, &task.done);
      container->Add(checkbox);
    }

    screen.RequestAnimationFrame();
  }

public:
  Impl()
      : screen(ScreenInteractive::Fullscreen()),
        container(Container::Vertical({})) {}

  ~Impl() {}

  void run(TasksPeer &peer) {
    if (isatty(STDERR_FILENO)) {
      // Redirect stderr to /dev/null so it doesn't interfere with TUI output.
      std::freopen("/dev/null", "w", stderr);
    }

    auto observer = peer.register_tasks_observer(
        [this](const std::vector<::Task> &new_tasks) {
          screen.Post([this, new_tasks] { update_tasks(new_tasks); });
        });

    auto renderer = Renderer(container, [this] {
      return window(text("Ditto Tasks"),
                    container->Render() | vscroll_indicator | frame);
    });

    screen.Loop(renderer);
  }
};

TasksTui::TasksTui() : impl(std::make_shared<Impl>()) {

  // Demo content
  //       {"50191411-4C46-4940-8B72-5F8017A04FA7", "Buy groceries"},
  //       {"6DA283DA-8CFE-4526-A6FA-D385089364E5", "Clean the kitchen"},
  //       {"5303DDF8-0E72-4FEB-9E82-4B007E5797F0", "Schedule dentist
  //       appointment"},
  //       {"38411F1B-6B49-4346-90C3-0B16CE97E174", "Pay bills"}};
}

TasksTui::~TasksTui() {}

void TasksTui::run(TasksPeer &peer) { impl->run(peer); }

#endif // DITTO_QUICKSTART_TUI
