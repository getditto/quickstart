import React, { useState, useEffect, useRef } from 'react';
import {
  FlatList,
  Text,
  StyleSheet,
  PermissionsAndroid,
  Platform,
  View,
  TextInput,
  Button,
  SafeAreaView,
  Alert,
} from 'react-native';
import {
  Ditto,
  IdentityOnlinePlayground,
  TransportConfig,
} from '@dittolive/ditto';

type Task = {
  id: string;
  title: string;
};

const App = () => {
  const [task, setTask] = useState<string>('');
  const [tasks, setTasks] = useState<Task[]>([]);

  const ditto = useRef<Ditto | null>(null);

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

  async function syncTasks() {
    try {
      const identity: IdentityOnlinePlayground = {
        type: 'onlinePlayground',
        appID: '<YOUR APP ID>',
        token: '<YOUR PLAYGROUND TOKEN>',
      };

      ditto.current = new Ditto(identity);
      const transportsConfig = new TransportConfig();
      transportsConfig.peerToPeer.bluetoothLE.isEnabled = true;
      transportsConfig.peerToPeer.lan.isEnabled = true;
      transportsConfig.peerToPeer.lan.isMdnsEnabled = true;

      if (Platform.OS === 'ios') {
        transportsConfig.peerToPeer.awdl.isEnabled = true;
      }
      ditto.current.setTransportConfig(transportsConfig);

      ditto.current.startSync();

      ditto.current.sync.registerSubscription(`SELECT * FROM tasks`);

      // Subscribe to task updates
      ditto.current.store.registerObserver(`SELECT * FROM tasks`, response => {
        const fetchedTasks: Task[] = response.items.map(doc => ({
          id: doc.value._id,
          title: doc.value.title as string,
        }));

        setTasks(fetchedTasks);
      });
    } catch (error) {
      console.error('Error syncing tasks:', error);
    }
  }

  useEffect(() => {
    const setupDitto = async () => {
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
    };
    setupDitto();
  }, []);

  async function handleAddTask() {
    if (ditto.current === null) return;

    if (task.trim().length === 0) return;

    const result = await ditto.current.store.execute(
      `INSERT INTO tasks DOCUMENTS ({ 'title': '${task}' })`,
    );
    const newId = result.mutatedDocumentIDs().map(id => id.value)[0];

    const newTask: Task = {
      title: task,
      id: newId,
    };
    setTasks(currentTasks => [...currentTasks, newTask]);
    setTask('');
  }

  const renderItem = ({ item }: { item: Task }) => (
    <View style={styles.item}>
      <Text style={styles.title}>{item.title}</Text>
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Enter a task"
        value={task}
        onChangeText={setTask}
      />
      <Button title="Add Task" onPress={handleAddTask} />
      <FlatList
        data={tasks}
        renderItem={renderItem}
        keyExtractor={item => item.id}
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
  input: {
    marginBottom: 10,
    padding: 10,
    borderWidth: 1,
    borderColor: '#ccc',
  },
  item: {
    padding: 10,
    marginVertical: 8,
    backgroundColor: '#f9c2ff',
    borderRadius: 5,
  },
  title: {
    fontSize: 18,
  },
});

export default App;
