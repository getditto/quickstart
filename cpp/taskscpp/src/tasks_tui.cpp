#ifdef DITTO_QUICKSTART_TUI

#include "tasks_tui.h"

#include <cstdio>

#include <unistd.h>

#include "ftxui/component/component.hpp"
#include "ftxui/component/loop.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"

class TasksTui::Impl {
private:
  std::vector<Task> tasks;
  ftxui::Component tasks_list;
  ftxui::ScreenInteractive screen;

  void update_tasks_list(std::vector<Task> new_tasks) {
    if (new_tasks == tasks) {
      return;
    }

    // TODO: Need to maintain currently active task in the list.

    tasks_list->DetachAllChildren();

    tasks = std::move(new_tasks);

    for (auto &task : tasks) {
      // TODO: add CheckboxOption.on_change callback to update task.done
      // TODO: customize checkbox appearance with CheckboxOption.transform
      auto checkbox = ftxui::Checkbox(task.title, &task.done);
      tasks_list->Add(checkbox);
    }

    // force redraw
    screen.RequestAnimationFrame();
  }

  // Render the UI until the user quits.
  void display_ui() {
    using namespace ftxui;

    auto top_bar = Renderer([] {
      // TODO: Use color and icon for Sync Active state
      return hbox({
          text("Ditto Tasks") | bold | flex,
          text("Sync Active (s: toggle sync)"),
      });
    });

    auto bottom_bar = Renderer([] {
      return hbox({text("(c: create) (d: delete) (e: edit) (q: quit)") | flex});
    });

    auto renderer = Renderer(tasks_list, [&, this] {
      return vbox({top_bar->Render(),                                       //
                   separator(),                                             //
                   tasks_list->Render() | vscroll_indicator | frame | flex, //
                   separator(),                                             //
                   bottom_bar->Render()})                                   //
             | border;
    });

    auto event_handler = CatchEvent(renderer, [&](Event event) {
      if (event == Event::Character('q')) {
        screen.ExitLoopClosure()();
        return true;
      }

      // TODO: add handlers for 's', 'c', 'd', and 'e' key presses

      return false;
    });

    screen.Loop(event_handler);
  }

public:
  Impl()
      : screen(ftxui::ScreenInteractive::Fullscreen()),
        tasks_list(ftxui::Container::Vertical({})) {}

  ~Impl() {}

  void run(TasksPeer &peer) {
    if (isatty(STDERR_FILENO)) {
      // Redirect stderr to /dev/null so it doesn't interfere with TUI output.
      std::freopen("/dev/null", "w", stderr);
    }

    auto observer = peer.register_tasks_observer(
        [this](const std::vector<Task> &new_tasks) {
          screen.Post(
              [this, new_tasks] { update_tasks_list(std::move(new_tasks)); });
        });

    display_ui();
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
