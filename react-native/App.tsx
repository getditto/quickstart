import React, {useState, useEffect, useRef} from 'react';
import {
  Text,
  StyleSheet,
  PermissionsAndroid,
  Platform,
  View,
  SafeAreaView,
  Alert,
  FlatList,
  Button,
} from 'react-native';
import {
  Authenticator,
  Ditto,
  DittoConfig,
  DittoConfigConnect,
  IdentityOnlinePlayground,
  StoreObserver,
  SyncSubscription,
} from '@dittolive/ditto';
import {
  DITTO_APP_ID,
  DITTO_PLAYGROUND_TOKEN,
  DITTO_AUTH_URL,
  DITTO_WEBSOCKET_URL,
} from '@env';

import Fab from './components/Fab';
import NewTaskModal from './components/NewTaskModal';
import DittoInfo from './components/DittoInfo';
import DittoSync from './components/DittoSync';
import TaskDone from './components/TaskDone';
import EditTaskModal from './components/EditTaskModal';

type Task = {
  id: string;
  title: string;
  done: boolean;
  deleted: boolean;
};

// https://docs.ditto.live/sdk/latest/install-guides/react-native#onlineplayground
const dittoCloudIdentity: IdentityOnlinePlayground = {
  type: 'onlinePlayground',
  appID: DITTO_APP_ID,
  token: DITTO_PLAYGROUND_TOKEN,
};

const customBYOC: IdentityOnlinePlayground = {
  type: 'onlinePlayground',
  appID: DITTO_APP_ID,
  token: DITTO_PLAYGROUND_TOKEN,
  customAuthURL: DITTO_AUTH_URL,
  enableDittoCloudSync: false,
};

var identity;
if (typeof DITTO_AUTH_URL === 'string' && DITTO_AUTH_URL.length > 0) {
  identity = customBYOC;
} else {
  identity = dittoCloudIdentity;
}

async function requestPermissions() {
  const permissions = [
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
    PermissionsAndroid.PERMISSIONS.NEARBY_WIFI_DEVICES,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
  ];

  const granted = await PermissionsAndroid.requestMultiple(permissions);
  return Object.values(granted).every(
    result => result === PermissionsAndroid.RESULTS.GRANTED,
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

  // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
  const toggleSync = () => {
    if (syncEnabled) {
      ditto.current?.sync.stop();
    } else {
      ditto.current?.sync.start();
    }
    setSyncEnabled(!syncEnabled);
  };

  // https://docs.ditto.live/sdk/latest/crud/create
  const createTask = async (title: string) => {
    if (title === '') {
      return;
    }
    await ditto.current?.store.execute('INSERT INTO tasks DOCUMENTS (:task)', {
      task: {
        title,
        done: false,
        deleted: false,
      },
    });
  };

  // https://docs.ditto.live/sdk/latest/crud/update
  const toggleTask = async (task: Task) => {
    await ditto.current?.store.execute(
      'UPDATE tasks SET done=:done WHERE _id=:id',
      {
        id: task.id,
        done: !task.done,
      },
    );
  };

  // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
  const deleteTask = async (task: Task) => {
    await ditto.current?.store.execute(
      'UPDATE tasks SET deleted=true WHERE _id=:id',
      {
        id: task.id,
      },
    );
  };

  const updateTaskTitle = async (taskId: string, newTitle: string) => {
    await ditto.current?.store.execute(
      'UPDATE tasks SET title=:title WHERE _id=:id',
      {
        id: taskId,
        title: newTitle,
      },
    );
  };

  const initDitto = async () => {
    try {
      // https://docs.ditto.live/sdk/latest/install-guides/react-native#onlineplayground
      const databaseId = DITTO_APP_ID;
      const playgroundToken = DITTO_PLAYGROUND_TOKEN;

      const connectConfig: DittoConfigConnect = {
        mode: 'server',
        url: `https://${databaseId}.cloud.ditto.live`,
      };

      const config = new DittoConfig(databaseId, connectConfig, "newfodlerxx");

      ditto.current = await Ditto.open(config);

      ditto.current.updateTransportConfig(config => {
        if (typeof DITTO_WEBSOCKET_URL === 'string' && DITTO_WEBSOCKET_URL.length > 0) {
          config.connect.websocketURLs = [DITTO_WEBSOCKET_URL];
        }
        return config;
      });


      if (connectConfig.mode === 'server') {
        await ditto.current.auth.setExpirationHandler(async (ditto, timeUntilExpiration) => {
          console.log('Authentication expiring soon, time until expiration:', timeUntilExpiration);

          if (ditto.auth.loginSupported) {
            const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
            const reLoginResult = await ditto.auth.login(playgroundToken, devProvider);
            if (reLoginResult.error) {
              console.error('Re-authentication failed:', reLoginResult.error);
            } else {
              console.log('Successfully re-authenticated with info:', reLoginResult);
            }
          }
        });

        if (ditto.current.auth.loginSupported) {
          // Use the development provider constant from Ditto
          const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
          console.log('Using development provider:', devProvider);

          const loginResult = await ditto.current.auth.login(playgroundToken, devProvider);
          if (loginResult.error) {
            console.error('Login failed:', loginResult.error);
          } else {
            console.log('Successfully logged in with info:', loginResult);
          }
        }
      }

      ditto.current.sync.start();

      await ditto.current.store.execute('ALTER SYSTEM SET DQL_STRICT_MODE = false');

      taskSubscription.current = ditto.current.sync.registerSubscription('SELECT * FROM tasks');

      taskObserver.current = ditto.current.store.registerObserver('SELECT * FROM tasks WHERE NOT deleted', response => {
        const fetchedTasks: Task[] = response.items.map(doc => ({
          id: doc.value._id,
          title: doc.value.title as string,
          done: doc.value.done,
          deleted: doc.value.deleted,
        }));

        setTasks(fetchedTasks);
      });
    } catch (error) {
      console.error('Error syncing tasks:', error);
    }
  };

  useEffect(() => {
    (async () => {
      const granted =
        Platform.OS === 'android' ? await requestPermissions() : true;
      if (granted) {
        initDitto();
      } else {
        Alert.alert(
          'Permission Denied',
          'You need to grant all permissions to use this app.',
        );
      }
    })();
  }, []);

  const renderItem = ({item}: {item: Task}) => (
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
    <SafeAreaView style={styles.container}>
      <DittoInfo appId={identity.appID} token={identity.token} />
      <DittoSync value={syncEnabled} onChange={toggleSync} />
      <Fab onPress={() => setModalVisible(true)} />
      <NewTaskModal
        visible={modalVisible}
        onRequestClose={() => setModalVisible(false)}
        onSubmit={task => {
          createTask(task);
          setModalVisible(false);
        }}
        onClose={() => setModalVisible(false)}
      />
      <EditTaskModal
        visible={editingTask !== null}
        task={editingTask}
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
        keyExtractor={item => item.id}
      />
    </SafeAreaView>
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
});

export default App;
