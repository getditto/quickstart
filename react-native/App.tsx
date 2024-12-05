import React, { useState, useEffect, useRef } from 'react';
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
  Ditto,
  IdentityOnlinePlayground,
  StoreObserver,
  SyncSubscription,
  TransportConfig,
} from '@dittolive/ditto';

import Fab from './components/Fab';
import NewTaskModal from './components/NewTaskModal';
import DittoInfo from './components/DittoInfo';
import DittoSync from './components/DittoSync';

type Task = {
  id: string;
  title: string;
  done: boolean,
  deleted: boolean,
};

const identity: IdentityOnlinePlayground = {
  type: 'onlinePlayground',
  appID: '<YOUR APP ID>',
  token: '<YOUR PLAYGROUND TOKEN>',
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
    result => result === PermissionsAndroid.RESULTS.GRANTED,
  );
}

const App = () => {
  const ditto = useRef<Ditto | null>(null);
  const taskSubscription = useRef<SyncSubscription | null>(null);
  const taskObserver = useRef<StoreObserver | null>(null);

  const [modalVisible, setModalVisible] = useState(false);
  const [syncEnabled, setSyncEnabled] = useState(false);

  const [tasks, setTasks] = useState<Task[]>([
    { id: "1", title: "One", done: false, deleted: false },
    { id: "2", title: "Two", done: false, deleted: false },
    { id: "3", title: "Three", done: false, deleted: false },
  ]);

  const toggleSync = () => {
    if (syncEnabled) {
      ditto.current?.stopSync();
    } else {
      ditto.current?.startSync();
    }
    setSyncEnabled(!syncEnabled);
  }

  async function syncTasks() {
    try {
      ditto.current = new Ditto(identity);

      // Initialize transport config
      {
        const transportsConfig = new TransportConfig();
        transportsConfig.peerToPeer.bluetoothLE.isEnabled = true;
        transportsConfig.peerToPeer.lan.isEnabled = true;
        transportsConfig.peerToPeer.lan.isMdnsEnabled = true;

        if (Platform.OS === 'ios') {
          transportsConfig.peerToPeer.awdl.isEnabled = true;
        }
        ditto.current.setTransportConfig(transportsConfig);
      }

      ditto.current.startSync();
      taskSubscription.current = ditto.current.sync.registerSubscription(`SELECT * FROM tasks`);

      // Subscribe to task updates
      taskObserver.current = ditto.current.store.registerObserver(`SELECT * FROM tasks WHERE NOT deleted`, response => {
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
  }

  useEffect(() => {
    (async () => {
      const granted =
        Platform.OS === 'android' ? await requestPermissions() : true;
      if (granted) {
        syncTasks();
      } else {
        Alert.alert(
          'Permission Denied',
          'You need to grant all permissions to use this app.',
        );
      }
    })();
  }, []);

  const renderItem = ({ item }: { item: Task }) => (
    <View>
      <View>
        <Button title="Done" />
        <Text style={styles.title}>{item.title}</Text>
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
        onSubmit={(task) => Alert.alert(`New task ${task}`)}
        onClose={() => setModalVisible(false)}
      />
      <FlatList
        style={styles.list}
        contentContainerStyle={{ gap: 5 }}
        data={tasks}
        renderItem={renderItem}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#fff',
  },
  taskContainer: {
    padding: 10,
    backgroundColor: '#93C5FD',
    flex: 1,
    flexDirection: "row",
  },
  taskCheckbox: {

  },
  taskDeleteButton: {

  },
  title: {
    fontSize: 18,
  },
  list: {
    borderWidth: 5,
  }
});

export default App;
