import React, { useEffect, useState } from 'react';
import { Text, Spacer, Box, useInput } from 'ink';

const dummyTask = {
	title: "Dummy",
	done: false,
	deleted: false,
};

export default function App({ ditto }) {
	const [tasks, setTasks] = useState([dummyTask]);

	useInput((input, key) => {
		console.log("Input: ", input, ", key: ", key);

		switch (input) {
			case 'c':
				(async () => {
					ditto.store.execute("INSERT INTO tasks DOCUMENTS (:task)", {
						task: {
							title: "Newer Task",
							done: false,
							deleted: false
						}
					});
					console.log("Inserted!");
				})();
			default:
		}
	});

	useEffect(() => {
		// Inline async context
		(async () => {
			ditto.store.registerObserver("SELECT * FROM tasks LIMIT 5", (result) => {
				console.log("queryResult: ", result);

				const tasks = result.items.map(item => item.value);
				console.log("tasks: ", tasks);
				setTasks(tasks);
			});
		})(); // End async
	}, [ditto]);

	return (
		<Box borderStyle="round" padding={2}>
			<Text>
				Hello, <Text color="green">World</Text>
			</Text>
			{Array.from(tasks).map((task) => {
				console.log("Task", task);
				return <>
					<Text>{task.title}</Text>
					<Spacer />
				</>
			})}
			<Text>
				End Tasks
			</Text>
		</Box>
	);
}
