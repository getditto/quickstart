import React, { useEffect, useState } from 'react';
import { Text, Spacer, Box, useInput } from 'ink';

const enterAltScreenCommand = '\x1b[?1049h';
const leaveAltScreenCommand = '\x1b[?1049l';

export default function App({ ditto }) {
	return (
		<FullScreen>
			<HelpPanel>
				<TodoApp ditto={ditto} />
			</HelpPanel>
		</FullScreen>
	)
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
				rows: process.stdout.rows,
			});
		}

		process.stdout.on("resize", onResize);
		process.stdout.write(enterAltScreenCommand);
		return () => {
			process.stdout.off("resize", onResize);
			process.stdout.write(leaveAltScreenCommand);
		};
	}, []);

	return (
		<Box flexDirection="column" width={size.columns} height={size.rows} borderStyle="round">
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
				<Box flexDirection="row">
					{props.children}
					<Spacer />
					<Box flexDirection="column" borderStyle="round">
						<Text>? - toggle help</Text>
						<Text>k - scroll up</Text>
						<Text>j - scroll down</Text>
						<Text>c - create task</Text>
						<Text>d - delete task</Text>
						<Text>e - edit task</Text>
						<Text>Enter - toggle done</Text>
					</Box>
				</Box>
			</>
		);
	}

	return (
		<>
			{props.children}
		</>
	);
};

const LIST_MODE = "list";
const CREATE_MODE = "create";
const EDIT_MODE = "edit";

const TodoApp = ({ ditto }) => {
	const [tasks, setTasks] = useState([]);
	const [mode, setMode] = useState(LIST_MODE);
	const [selected, setSelected] = useState(0);
	const [_observer, setObserver] = useState(null);

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
		}

		if (key.escape) {
			setMode(LIST_MODE);
			return;
		}
	});

	useEffect(() => {
		// Inline async context
		(async () => {
			const observer = ditto.store.registerObserver("SELECT * FROM tasks WHERE NOT deleted ORDER BY _id", (result) => {
				const tasks = result.items.map(item => item.value);
				setTasks(tasks);
			});
			setObserver(observer);
		})(); // End async
	}, [ditto, mode]);

	const Prompt = React.memo(({ edit }) => {
		const newTask = !edit;
		const initialText = newTask ? "" : edit.title;
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
						const _result = await ditto.store.execute("INSERT INTO tasks DOCUMENTS (:task)", {
							task: {
								title: text,
								done: false,
								deleted: false,
							}
						});
					})();
				} else {
					debugger;
					(async () => {
						const _result = await ditto.store.execute("UPDATE tasks SET title=:title WHERE _id=:id", {
							id: edit._id,
							title: text,
						});
					})();
				}

				// On submission:
				setText(""); // Reset input field
				setMode(LIST_MODE); // Set app back to "list" mode
				return;
			}

			const newContent = text + input;
			setText(newContent);
		});

		return (
			<Text>Title: {text}</Text>
		)
	});

	const List = React.memo(({ tasks }) => {
		useInput((input, key) => {
			// Scroll up
			if (input === 'k') {
				if (selected > 0) {
					setSelected(selected - 1);
				}
				return;
			}

			// Scroll down
			if (input === 'j') {
				if (selected < tasks.length - 1) {
					setSelected(selected + 1);
				}
				return;
			}

			// Delete
			if (input === 'd') {
				if (tasks.length > 0) {
					(async () => {
						deleteTask(ditto, tasks[selected]);
					})();
				}
			}

			if (key.return) {
				(async () => {
					await toggleDone(ditto, tasks[selected]);
				})();
			}
		});

		return (
			<Box flexDirection="column">
				{Array.from(tasks).map((task, i) => {
					const done = task.done ? " 🟢 " : " ⚪️ ";
					const highlight = selected === i ? "blue" : "";
					return (
						<Box flexDirection="row">
							<Text color={highlight}>
								<Text>{done}    </Text>
								<Text>{task.title}</Text>
							</Text>
						</Box>
					)
				})}
			</Box>
		);
	});

	if (mode === LIST_MODE) {
		return <Box flexDirection="column">
			<Text> Done  Title</Text>
			<List tasks={tasks} />
		</Box>
	}

	if (mode === CREATE_MODE) {
		return <Box flexDirection="column">
			<Text> Create new Task</Text>
			<Prompt />
		</Box>
	}

	if (mode === EDIT_MODE) {
		const selectedTask = tasks[selected];
		return <Box flexDirection="column">
			<Text> Create new Task</Text>
			<Prompt edit={selectedTask} />
		</Box>
	}
};

const toggleDone = async (ditto, task) => {
	console.log("Toggling task!", task);
	await ditto.store.execute("UPDATE tasks SET done=:done WHERE _id=:id", {
		id: task._id,
		done: !task.done,
	});
}

const deleteTask = async (ditto, task) => {
	await ditto.store.execute("UPDATE tasks SET deleted=true WHERE _id=:id", {
		id: task._id,
	});
}

const updateTask = async (ditto, task, title) => {
	await ditto.store.execute("UPDATE tasks SET title=:title WHERE _id=:id", {
		id: task._id,
		title,
	});
}
