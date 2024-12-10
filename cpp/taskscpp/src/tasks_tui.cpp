#ifdef DITTO_QUICKSTART_TUI

#include "tasks_tui.h"
#include "env.h"
#include "tasks_log.h"

#include <algorithm>
#include <cstdio>

#include <unistd.h>

#include "ftxui/component/component.hpp"
#include "ftxui/component/loop.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"

class TasksTui::Impl {
private:
  TasksPeer &peer;
  std::vector<Task> tasks;
  ftxui::Component tasks_list;
  ftxui::ScreenInteractive screen;
  std::string status_text;

  // Return a pointer to the task that is currently active in the task list, or
  // nullptr if none.
  //
  // The returned pointer is only valid until the next call to
  // update_tasks_list().
  const Task *active_task() const {
    for (auto i = 0; i < std::min(tasks.size(), tasks_list->ChildCount());
         i++) {
      auto checkbox = tasks_list->ChildAt(i);
      if (checkbox->Active()) {
        return &tasks[i];
      }
    }
    return nullptr;
  }

  // Return the ID of the task that is currently active in the task list, or
  // empty string if none.
  std::string active_task_id() const {
    auto task = active_task();
    if (task) {
      return task->_id;
    } else {
      return "";
    }
  }

  // Set the contents of the task list to the given tasks.
  void update_tasks_list(std::vector<Task> new_tasks) {
    if (new_tasks == tasks) {
      return;
    }

    // Maintain the existing selection.
    auto task_id = active_task_id();

    tasks_list->DetachAllChildren();

    tasks = std::move(new_tasks);

    ftxui::Component active_checkbox;
    for (auto &task : tasks) {
      auto checkbox =
          ftxui::Checkbox(task.title, &task.done,
                          ftxui::CheckboxOption{.on_change = [this, &task] {
                            try {
                              peer.mark_task_complete(task._id, task.done);
                            } catch (const std::exception &err) {
                              log_error("Failed to mark task complete: " +
                                        std::string(err.what()));
                            }
                          }});
      tasks_list->Add(checkbox);
      if (task._id == active_task_id()) {
        active_checkbox = checkbox;
      }
    }
    if (active_checkbox) {
      tasks_list->SetActiveChild(active_checkbox);
    }

    // force redraw
    screen.RequestAnimationFrame();
  }

  // Toggle sync on/off
  void toggle_sync() {
    try {
      if (peer.is_sync_active()) {
        peer.stop_sync();
      } else {
        peer.start_sync();
      }
    } catch (const std::exception &err) {
      log_error("Failed to toggle sync: " + std::string(err.what()));
    }

    // force redraw
    screen.RequestAnimationFrame();
  }

  // Render text UI until the user quits.
  void display_ui() {
    using namespace ftxui;

    enum class Mode { Normal, Create, Edit } mode = Mode::Normal;

    // Main screen layout with list of tasks and sync on/off
    auto top_bar = Renderer([this] {
      return vbox({
          hbox({text("Ditto Tasks") | bold | flex,
                (peer.is_sync_active()
                     ? text("🟢 Sync Active") | color(Color::Green)
                     : text("🔴 Sync Inactive") | color(Color::Red)) |
                    bold,
                text(" (s: toggle sync)")}),
          text("App ID: " DITTO_APP_ID) | center,
          text("Playground Token: " DITTO_PLAYGROUND_TOKEN) | center,
      });
    });
    auto bottom_bar = Renderer([this] {
      return hbox({text("(j↑) (k↓) (Space/Enter: toggle)"
                        " (c: create) (d: delete) (e: edit) (q: quit)") |
                       flex,
                   text(status_text)});
    });
    auto main_ui = Renderer(tasks_list, [this, &top_bar, &bottom_bar] {
      return vbox({top_bar->Render(),                                       //
                   separator(),                                             //
                   tasks_list->Render() | vscroll_indicator | frame | flex, //
                   separator(),                                             //
                   bottom_bar->Render()})                                   //
             | border;
    });

    // Modal dialog allows text entry
    bool show_modal = false;
    std::string modal_text;
    std::string modal_task_id;
    auto modal_input = Input(&modal_text, "Enter task title");
    auto modal_dialog = Renderer(modal_input, [this, &mode, &modal_input] {
      return vbox({
                 text(mode == Mode::Create ? "New Task" : "Edit Task") | bold,
                 separator(),
                 modal_input->Render(),
                 filler(),
                 separator(),
                 text("(Esc: back) (Return/Enter: save)"),
             }) |
             size(WIDTH, EQUAL, screen.dimx() - 6) |
             size(HEIGHT, EQUAL, screen.dimy() - 4) | border;
    });
    main_ui |= Modal(modal_dialog, &show_modal);

    auto event_handler = CatchEvent(main_ui, [this, &mode, &show_modal,
                                              &modal_text,
                                              &modal_task_id](Event event) {
      switch (mode) {
      case Mode::Normal:
        if (event == Event::Character('c')) {
          mode = Mode::Create;
          modal_text = "";
          show_modal = true;
          return true;
        } else if (event == Event::Character('d')) {
          auto task_id = active_task_id();
          if (!task_id.empty()) {
            try {
              peer.delete_task(task_id);
            } catch (const std::exception &err) {
              log_error("Failed to delete task: " + std::string(err.what()));
            }
          }
        } else if (event == Event::Character('e')) {
          auto task = active_task();
          if (task != nullptr) {
            mode = Mode::Edit;
            modal_task_id = task->_id;
            modal_text = task->title;
            show_modal = true;
            return true;
          }
        } else if (event == Event::Character('s')) {
          toggle_sync();
        } else if (event == Event::Character('q')) {
          screen.ExitLoopClosure()();
          return true;
        }
        break;

      case Mode::Create:
        if (event == Event::Escape) {
          show_modal = false;
          mode = Mode::Normal;
          return true;
        } else if (event == Event::Return) {
          if (!modal_text.empty()) {
            show_modal = false;
            mode = Mode::Normal;
            try {
              peer.add_task(modal_text, false);
            } catch (const std::exception &err) {
              log_error("Failed to add task: " + std::string(err.what()));
            }
          }
          return true;
        }
        break;

      case Mode::Edit:
        if (event == Event::Escape) {
          show_modal = false;
          mode = Mode::Normal;
          return true;
        } else if (event == Event::Return) {
          if (!modal_text.empty()) {
            show_modal = false;
            mode = Mode::Normal;
            try {
              peer.update_task_title(modal_task_id, modal_text);
            } catch (const std::exception &err) {
              log_error("Failed to update task title: " +
                        std::string(err.what()));
            }
          }
          return true;
        }
        break;
      }

      return false;
    });

    screen.Loop(event_handler);
  }

public:
  Impl(TasksPeer &peer)
      : peer(peer), screen(ftxui::ScreenInteractive::Fullscreen()),
        tasks_list(ftxui::Container::Vertical({})) {}

  ~Impl() {}

  void run() {
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

TasksTui::TasksTui(TasksPeer &peer) : impl(std::make_shared<Impl>(peer)) {}

TasksTui::~TasksTui() {}

void TasksTui::run() { impl->run(); }

#endif // DITTO_QUICKSTART_TUI
