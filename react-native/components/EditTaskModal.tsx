import React from 'react';
import { useState, useEffect } from 'react';
import { Button, StyleSheet, Text, TextInput, View, TouchableOpacity } from 'react-native';

type EditTaskModalProps = {
  visible: boolean;
  task: { id: string, title: string } | null,
  onSubmit: (taskId: string, newTitle: string) => void,
  onClose?: () => void,
}

type Props = EditTaskModalProps;

const EditTaskModal: React.FC<Props> = ({ visible, task, onSubmit, onClose }) => {
  const [input, setInput] = useState('');

  useEffect(() => {
    if (task) {
      setInput(task.title);
    }
  }, [task]);

  const submit = () => {
    if (input !== '' && task) {
      onSubmit(task.id, input);
      setInput('');
    }
  };

  if (!visible) {
    return null;
  }

  return (
    <View style={styles.modalOverlay}>
      <TouchableOpacity 
        style={styles.backdrop} 
        activeOpacity={1} 
        onPress={onClose}
      />
      <View style={styles.modalContent}>
        <Text style={styles.modalTitle}>Edit Task</Text>
        <TextInput
          style={styles.input}
          value={input}
          onChangeText={setInput}
          autoFocus
        />
        <Button title="Save" onPress={submit} />
        <Button title="Cancel" onPress={onClose} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1000,
  },
  backdrop: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  modalContent: {
    width: '80%',
    maxWidth: 400,
    padding: 20,
    backgroundColor: '#fff',
    borderRadius: 10,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 15,
  },
  input: {
    width: '100%',
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 5,
    padding: 10,
    marginBottom: 15,
  },
});

export default EditTaskModal;
