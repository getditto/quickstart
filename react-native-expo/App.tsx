import React, { useState, useEffect, useRef } from "react";
import {
  Text,
  StyleSheet,
  PermissionsAndroid,
  Platform,
  View,
  SafeAreaView,
  FlatList,
  Button,
} from "react-native";
import {
  Ditto,
  IdentityOnlinePlayground,
  StoreObserver,
  SyncSubscription,
  TransportConfig,
} from "@dittolive/ditto";
import { DITTO_APP_ID, DITTO_PLAYGROUND_TOKEN, DITTO_WEBSOCKET_URL } from "@env";

import Fab from "./components/Fab";
import NewTaskModal from "./components/NewTaskModal";
import DittoInfo from "./components/DittoInfo";
import DittoSync from "./components/DittoSync";
import TaskDone from "./components/TaskDone";
import EditTaskModal from "./components/EditTaskModal";

type Task = {
  id: string;
  title: string;
  done: boolean;
  deleted: boolean;
};

const identity: IdentityOnlinePlayground = {
  type: "onlinePlayground",
  appID: DITTO_APP_ID,
  token: DITTO_PLAYGROUND_TOKEN,
};

async function requestPermissions() {
  const permissions = [
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
    PermissionsAndroid.PERMISSIONS.NEARBY_WIFI_DEVICES,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
  ];

  const granted = await PermissionsAndroid.requestMultiple(permissions);
  return Object.values(granted).every(
    (result) => result === PermissionsAndroid.RESULTS.GRANTED
  );
}

const App = () => {
  const ditto = useRef<Ditto | null>(null);
  const taskSubscription = useRef<SyncSubscription | null>(null);
  const taskObserver = useRef<StoreObserver | null>(null);

  const [modalVisible, setModalVisible] = useState(false);
  const [syncEnabled, setSyncEnabled] = useState(true);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);

  const toggleSync = () => {
    if (syncEnabled) {
      ditto.current?.stopSync();
    } else {
      ditto.current?.startSync();
    }
    setSyncEnabled(!syncEnabled);
  };

  const createTask = async (title: string) => {
    if (title === "") {
      return;
    }
    await ditto.current?.store.execute("INSERT INTO tasks DOCUMENTS (:task)", {
      task: {
        title,
        done: false,
        deleted: false,
      },
    });
  };

  const toggleTask = async (task: Task) => {
    await ditto.current?.store.execute(
      "UPDATE tasks SET done=:done WHERE _id=:id",
      {
        id: task.id,
        done: !task.done,
      }
    );
  };

  const deleteTask = async (task: Task) => {
    await ditto.current?.store.execute(
      "UPDATE tasks SET deleted=true WHERE _id=:id",
      {
        id: task.id,
      }
    );
  };

  const updateTaskTitle = async (taskId: string, newTitle: string) => {
    await ditto.current?.store.execute(
      "UPDATE tasks SET title=:title WHERE _id=:id",
      {
        id: taskId,
        title: newTitle,
      }
    );
  };

  const initDitto = async () => {
    try {
      ditto.current = new Ditto(identity);

      // Initialize transport config
      {
        const transportsConfig = new TransportConfig();
        transportsConfig.peerToPeer.bluetoothLE.isEnabled = true;
        transportsConfig.peerToPeer.lan.isEnabled = true;
        transportsConfig.peerToPeer.lan.isMdnsEnabled = true;
        
        // Configure websocket URL for transport
        transportsConfig.connect.websocketURLs = [DITTO_WEBSOCKET_URL];

        if (Platform.OS === "ios") {
          transportsConfig.peerToPeer.awdl.isEnabled = true;
        }
        ditto.current.setTransportConfig(transportsConfig);
      }

      ditto.current.startSync();

      // Register a subscription, which determines what data syncs to this peer
      // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
      taskSubscription.current = ditto.current.sync.registerSubscription(
        "SELECT * FROM tasks"
      );

      // Register observer, which runs against the local database on this peer
      // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
      taskObserver.current = ditto.current.store.registerObserver(
        "SELECT * FROM tasks WHERE NOT deleted ORDER BY title ASC",
        (response) => {
          const fetchedTasks: Task[] = response.items.map((doc) => ({
            id: doc.value._id,
            title: doc.value.title as string,
            done: doc.value.done,
            deleted: doc.value.deleted,
          }));

          setTasks(fetchedTasks);
        }
      );
    } catch (error) {
      console.error("Error syncing tasks:", error);
    }
  };

  useEffect(() => {
    (async () => {
      const granted =
        Platform.OS === "android" ? await requestPermissions() : true;
      
      initDitto();
    })();
  }, []);

  const renderItem = ({ item }: { item: Task }) => (
    <View key={item.id} style={styles.taskContainer}>
      <TaskDone checked={item.done} onPress={() => toggleTask(item)} />
      <Text style={styles.taskTitle} onLongPress={() => setEditingTask(item)}>
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
    <SafeAreaView style={styles.container} testID="main-screen">
      <DittoInfo appId={identity.appID} token={identity.token} />
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
        keyExtractor={(item) => item.id}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    height: "100%",
    padding: 20,
    backgroundColor: "#fff",
  },
  listContainer: {
    gap: 5,
  },
  taskContainer: {
    flex: 1,
    gap: 5,
    flexDirection: "row",
    paddingVertical: 10,
    paddingHorizontal: 20,
  },
  taskTitle: {
    fontSize: 20,
    alignSelf: "center",
    flexGrow: 1,
    flexShrink: 1,
  },
  taskButton: {
    flexShrink: 1,
    alignSelf: "center",
  },
  permissionBanner: {
    backgroundColor: "#FEF3C7",
    borderWidth: 1,
    borderColor: "#D97706",
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
  },
  permissionText: {
    color: "#92400E",
    fontSize: 14,
    textAlign: "center",
    fontWeight: "500",
  },
});

export default App;
