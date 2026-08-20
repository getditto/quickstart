import React, { useState } from 'react';
import {
  Text,
  StyleSheet,
  View,
  FlatList,
  Button,
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { DITTO_DATABASE_ID, DITTO_DEVELOPMENT_TOKEN } from '@env';

import Fab from './components/Fab';
import NewTaskModal from './components/NewTaskModal';
import DittoInfo from './components/DittoInfo';
import DittoSync from './components/DittoSync';
import TaskDone from './components/TaskDone';
import EditTaskModal from './components/EditTaskModal';
import { useDitto } from './hooks/useDitto';
import { useTasks } from './hooks/useTasks';

export type Task = {
  _id: string;
  title: string;
  done: boolean;
  deleted: boolean;
};

const App = () => {
  const { ditto, syncEnabled, toggleSync, hasPermissions } = useDitto();
  const { tasks, createTask, toggleTask, deleteTask, updateTaskTitle } =
    useTasks(ditto);

  const [modalVisible, setModalVisible] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);

  const renderItem = ({ item }: { item: Task }) => (
    <View key={item._id} style={styles.taskContainer}>
      <TaskDone checked={item.done} onPress={() => toggleTask(item)} />
      <Text
        style={styles.taskTitle}
        onLongPress={() => setEditingTask(item)}
        testID={item.title}
      >
        {item.title}
      </Text>
      <View style={styles.taskButton}>
        <Button
          title="Delete"
          color="#DC2626"
          onPress={() => deleteTask(item)}
        />
      </View>
    </View>
  );

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        {!hasPermissions && (
          <View style={styles.permissionBanner}>
            <Text style={styles.permissionText}>
              ⚠️ Limited functionality: Grant Bluetooth & WiFi permissions for
              peer-to-peer sync
            </Text>
          </View>
        )}
        <DittoInfo databaseId={DITTO_DATABASE_ID} token={DITTO_DEVELOPMENT_TOKEN} />
        <DittoSync value={syncEnabled} onChange={toggleSync} />
        <Fab onPress={() => setModalVisible(true)} />
        <NewTaskModal
          visible={modalVisible}
          onSubmit={(task) => {
            createTask(task);
            setModalVisible(false);
          }}
          onClose={() => setModalVisible(false)}
        />
        <EditTaskModal
          visible={editingTask !== null}
          task={editingTask}
          onRequestClose={() => setEditingTask(null)}
          onSubmit={(taskId, newTitle) => {
            updateTaskTitle(taskId, newTitle);
            setEditingTask(null);
          }}
          onClose={() => setEditingTask(null)}
        />
        <FlatList
          contentContainerStyle={styles.listContainer}
          data={tasks}
          renderItem={renderItem}
          keyExtractor={(item) => item._id}
        />
      </SafeAreaView>
    </SafeAreaProvider>
  );
};

const styles = StyleSheet.create({
  container: {
    height: '100%',
    padding: 20,
    backgroundColor: '#fff',
  },
  listContainer: {
    gap: 5,
  },
  taskContainer: {
    flex: 1,
    gap: 5,
    flexDirection: 'row',
    paddingVertical: 10,
    paddingHorizontal: 20,
  },
  taskTitle: {
    fontSize: 20,
    alignSelf: 'center',
    flexGrow: 1,
    flexShrink: 1,
  },
  taskButton: {
    flexShrink: 1,
    alignSelf: 'center',
  },
  permissionBanner: {
    backgroundColor: '#FEF3C7',
    borderWidth: 1,
    borderColor: '#D97706',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
  },
  permissionText: {
    color: '#92400E',
    fontSize: 14,
    textAlign: 'center',
    fontWeight: '500',
  },
});

export default App;
