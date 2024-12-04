#ifdef DITTO_QUICKSTART_TUI

#include "tasks_tui.h"

#include <cstdio>
#include <iostream>

#include "ftxui/component/component.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"

using namespace ftxui;

TasksTui::TasksTui() {
  // Demo content
  // TODO: Get data via Ditto
  tasks = {
      {"50191411-4C46-4940-8B72-5F8017A04FA7", "Buy groceries"},
      {"6DA283DA-8CFE-4526-A6FA-D385089364E5", "Clean the kitchen"},
      {"5303DDF8-0E72-4FEB-9E82-4B007E5797F0", "Schedule dentist appointment"},
      {"38411F1B-6B49-4346-90C3-0B16CE97E174", "Pay bills"}};
}

void TasksTui::run(TasksPeer *peer) {
  // Redirect stderr to /dev/null so it doesn't interfere with TUI output.
  std::freopen("/dev/null", "w", stderr);

  auto container = Container::Vertical({});
  for (auto &task : tasks) {
    container->Add(Checkbox(task.title, &task.done));
  }

  auto renderer = Renderer(container, [&] {
    return window(text("Ditto Tasks"),
                  container->Render() | vscroll_indicator | frame);
  });

  auto screen = ScreenInteractive::Fullscreen();
  screen.Loop(renderer);
}

#endif // DITTO_QUICKSTART_TUI
