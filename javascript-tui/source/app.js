import React, { useEffect, useRef, useState } from 'react';
import { Text, Spacer, Box, useInput } from 'ink';
import { TasksRepository } from './tasks-repository.js';

const enterAltScreenCommand = '\x1b[?1049h';
const leaveAltScreenCommand = '\x1b[?1049l';

export default function App({ ditto, dittoManager }) {
  return (
    <FullScreen>
      <HelpPanel>
        <TodoApp ditto={ditto} dittoManager={dittoManager} />
      </HelpPanel>
    </FullScreen>
  );
}

// Helper component to make TUI fullscreen and wrap with a border
const FullScreen = (props) => {
  const [size, setSize] = useState({
    columns: process.stdout.columns,
    rows: process.stdout.rows,
  });

  useEffect(() => {
    function onResize() {
      setSize({
        columns: process.stdout.columns,
        rows: process.stdout.rows - 1,
      });
    }

    process.stdout.on('resize', onResize);
    process.stdout.write(enterAltScreenCommand);
    return () => {
      process.stdout.off('resize', onResize);
      process.stdout.write(leaveAltScreenCommand);
    };
  }, []);

  return (
    <Box
      flexDirection="column"
      width={size.columns}
      height={size.rows}
      borderStyle="round"
    >
      {props.children}
    </Box>
  );
};

const HelpPanel = (props) => {
  const [showHelp, setShowHelp] = useState(true);

  useInput((input, _key) => {
    if (input === '?') {
      setShowHelp(!showHelp);
    }
  });

  if (showHelp) {
    return (
      <>
        <Box flexDirection="row" height="100%">
          {props.children}
          <Spacer />
          <Box
            flexDirection="column"
            borderStyle="round"
            height="100%"
            width="50%"
          >
            <Text>? - toggle help</Text>
            <Text>↑/k - scroll up</Text>
            <Text>↓/j - scroll down</Text>
            <Text>c - create task</Text>
            <Text>d - delete task</Text>
            <Text>e - edit task</Text>
            <Text>s - toggle sync</Text>
            <Text>q - quit</Text>
            <Text>Enter - toggle done</Text>
          </Box>
        </Box>
      </>
    );
  }

  return <>{props.children}</>;
};

const LIST_MODE = 'list';
const CREATE_MODE = 'create';
const EDIT_MODE = 'edit';

const TodoApp = ({ ditto, dittoManager }) => {
  const [tasks, setTasks] = useState([]);
  const [mode, setMode] = useState(LIST_MODE);
  const [selected, setSelected] = useState(0);
  const [syncEnabled, setSyncEnabled] = useState(true);

  // Hold onto the tasks repository so the input handlers can drive CRUD.
  const repositoryRef = useRef(null);

  useInput((input, key) => {
    if (mode === LIST_MODE) {
      if (input === 'c') {
        setMode(CREATE_MODE);
        return;
      }
      if (input === 'e') {
        setMode(EDIT_MODE);
        return;
      }
      if (input === 's') {
        if (syncEnabled) {
          dittoManager.stopSync();
          setSyncEnabled(false);
        } else {
          dittoManager.startSync();
          setSyncEnabled(true);
        }
      }
    }

    if (key.escape) {
      setMode(LIST_MODE);
      return;
    }
  });

  useEffect(() => {
    // The repository registers the tasks subscription + observer and streams
    // the observed task list back into local state.
    const repository = new TasksRepository(ditto, setTasks);
    repository.start();
    repositoryRef.current = repository;

    // Cleanup on unmount: cancel subscription and observer
    return () => {
      repository.dispose();
      repositoryRef.current = null;
    };
  }, [ditto]);

  const Prompt = React.memo(({ edit }) => {
    const newTask = !edit;
    const initialText = newTask ? '' : edit.title;
    const [text, setText] = useState(initialText);

    useInput((input, key) => {
      if (key.backspace || key.delete) {
        // Chop off last char and set
        setText(text.slice(0, -1));
        return;
      }

      if (key.return) {
        if (newTask) {
          (async () => {
            await repositoryRef.current.createTask(text);
          })();
        } else {
          (async () => {
            await repositoryRef.current.editTask(edit._id, text);
          })();
        }

        // On submission:
        setText(''); // Reset input field
        setMode(LIST_MODE); // Set app back to "list" mode
        return;
      }

      const newContent = text + input;
      setText(newContent);
    });

    return <Text>Title: {text}</Text>;
  });

  const List = React.memo(({ tasks }) => {
    useInput((input, key) => {
      // Scroll up
      if (input === 'k' || key.upArrow) {
        if (selected > 0) {
          setSelected(selected - 1);
        }
        return;
      }

      // Scroll down
      if (input === 'j' || key.downArrow) {
        if (selected < tasks.length - 1) {
          setSelected(selected + 1);
        }
        return;
      }

      // Delete
      if (input === 'd') {
        if (tasks.length > 0) {
          (async () => {
            await repositoryRef.current.deleteTask(tasks[selected]);
          })();
        }
      }

      // Quit
      if (input === 'q') {
        process.stdout.write('\x1B[?25h'); // Make cursor visible
        process.exit(0);
      }

      if (key.return) {
        (async () => {
          await repositoryRef.current.toggleTask(tasks[selected]);
        })();
      }
    });

    return (
      <Box flexDirection="column">
        {tasks.map((task, i) => {
          const done = task.done ? ' 🟢 ' : ' ⚪️ ';
          const highlight = selected === i ? 'blue' : '';
          const cursor = selected === i ? '❯ ' : '  ';
          return (
            <Box key={task._id || i} flexDirection="row">
              <Text color={highlight}>
                <Text>{done} </Text>
                <Text>
                  {cursor}
                  {task.title}
                </Text>
              </Text>
            </Box>
          );
        })}
      </Box>
    );
  });

  const syncStatus = syncEnabled ? '🟢 Sync Active' : '🔴 Sync Inactive';
  const syncText = <Text>{syncStatus}</Text>;

  if (mode === LIST_MODE) {
    return (
      <Box flexDirection="column">
        {syncText}
        <Text> Done Title</Text>
        <List tasks={tasks} />
      </Box>
    );
  }

  if (mode === CREATE_MODE) {
    return (
      <Box flexDirection="column">
        {syncText}
        <Text> Create new Task</Text>
        <Prompt />
      </Box>
    );
  }

  if (mode === EDIT_MODE) {
    const selectedTask = tasks[selected];
    return (
      <Box flexDirection="column">
        {syncText}
        <Text> Edit Task</Text>
        <Prompt edit={selectedTask} />
      </Box>
    );
  }
};
