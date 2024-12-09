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
  std::vector<Task> tasks;
  ftxui::Component tasks_list;
  ftxui::ScreenInteractive screen;
  std::string new_task_id;
  std::string debug_text; // displayed at the bottom of the UI, if not empty

  // Return the ID of the task that is currently focused in the task list, or
  // empty string if none.
  std::string selected_task_id() {
    for (auto i = 0; i < std::min(tasks.size(), tasks_list->ChildCount());
         i++) {
      auto checkbox = tasks_list->ChildAt(i);
      if (checkbox->Active()) {
        return tasks[i]._id;
      }
    }
    return "";
  }

  // Update the task list with new tasks.
  void update_tasks_list(std::vector<Task> new_tasks) {
    if (new_tasks == tasks) {
      return;
    }

    // If a new task was just added, select it in the list; otherwise, maintain
    // the existing selection.
    std::string task_id;
    if (!new_task_id.empty()) {
      task_id = new_task_id;
      new_task_id.clear();

    } else {
      task_id = selected_task_id();
    }

    tasks_list->DetachAllChildren();

    tasks = std::move(new_tasks);

    ftxui::Component selected_checkbox;
    for (auto &task : tasks) {
      // TODO: add CheckboxOption.on_change callback to update task.done
      // TODO: customize checkbox appearance with CheckboxOption.transform
      auto checkbox = ftxui::Checkbox(task.title, &task.done);
      tasks_list->Add(checkbox);
      if (task._id == selected_task_id()) {
        selected_checkbox = checkbox;
      }
    }
    if (selected_checkbox) {
      tasks_list->SetActiveChild(selected_checkbox);
    }

    // force redraw
    screen.RequestAnimationFrame();
  }

  // Render text UI until the user quits.
  void display_ui(TasksPeer &peer) {
    using namespace ftxui;

    enum class Mode { Normal, Create, Edit } mode = Mode::Normal;

    // Main screen layout with list of tasks and sync on/off
    auto top_bar = Renderer([] {
      // TODO: Use color and icon for Sync Active state
      return vbox({
          hbox({text("Ditto Tasks") | bold | flex,
                text("Sync Active (s: toggle sync)")}),
          text("App ID: " DITTO_APP_ID) | center,
          text("Playground Token: " DITTO_PLAYGROUND_TOKEN) | center,
      });
    });
    auto bottom_bar = Renderer([this] {
      return hbox({text("(j,↑: down) (k,↓: up) (Space/Enter: toggle)"
                        " (c: create) (d: delete) (e: edit) (q: quit)") |
                       flex,
                   text(debug_text)});
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

    auto event_handler = CatchEvent(main_ui, [this, &peer, &mode, &show_modal,
                                              &modal_text](Event event) {
      switch (mode) {
      case Mode::Normal:
        if (event == Event::Character('c')) {
          mode = Mode::Create;
          modal_text = "";
          show_modal = true;
          return true;
        } else if (event == Event::Character('d')) {
          // TODO: delete task
        } else if (event == Event::Character('e')) {
          mode = Mode::Edit;
          modal_text = "TODO: put selected task title here (not implemented)";
          show_modal = true;
          return true;
        } else if (event == Event::Character('s')) {
          // TODO: toggle sync
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
              new_task_id = peer.add_task(modal_text, false);
            } catch (const std::exception &err) {
              // TODO: display this error in the UI
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
            // TODO: update task title using modal_text
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

    display_ui(peer);
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
