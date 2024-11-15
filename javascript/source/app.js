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
		if (input === 'h') {
			setShowHelp(!showHelp);
		}
	});

	if (showHelp) {
		return (
			<>
				<Box flexDirection="row">
					<Box flexDirection="column">
						<Text>h - toggle help</Text>
						<Text>k - scroll up</Text>
						<Text>j - scroll down</Text>
						<Text>c - create task</Text>
						<Text>Enter - toggle done</Text>
					</Box>
					{props.children}
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

const TodoApp = ({ ditto }) => {
	const [tasks, setTasks] = useState([]);
	const [mode, setMode] = useState("list");
	const [selected, setSelected] = useState(0);
	const [_observer, setObserver] = useState(null);

	useInput((input, key) => {
		if (mode === "list") {
			if (input === 'c') {
				setMode("create");
				return;
			}
			if (input === 'e') {
				setMode("edit");
				return;
			}
		}

		if (key.escape) {
			setMode("list");
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

	const Prompt = React.memo(() => {
		const [content, setContent] = useState("");
		useInput((input, key) => {
			if (key.backspace || key.delete) {
				// Chop off last char and set
				setContent(content.slice(0, -1));
				return;
			}

			if (key.return) {
				(async () => {
					const _result = await ditto.store.execute("INSERT INTO tasks DOCUMENTS (:task)", {
						task: {
							title: content,
							done: false,
							deleted: false,
						}
					});
				})();

				setContent(""); // Reset input field
				setMode("list");
				return;
			}

			const newContent = content + input;
			setContent(newContent);
		});

		return (
			<Text>Title: {content}</Text>
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

	if (mode === "list") {
		return <Box flexDirection="column">
			<Text> Done  Title</Text>
			<List tasks={tasks} />
		</Box>
	}

	if (mode === "create") {
		return <Box flexDirection="column">
			<Text> Create new Task</Text>
			<Prompt />
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
