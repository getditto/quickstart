import React from 'react';
import { useState, useEffect } from 'react';
import { Button, Modal, ModalProps, StyleSheet, Text, TextInput, View } from 'react-native';

type EditTaskModalProps = {
  task: { id: string, title: string } | null,
  onSubmit: (taskId: string, newTitle: string) => void,
  onClose?: () => void,
}

type Props = EditTaskModalProps & ModalProps;

const EditTaskModal: React.FC<Props> = ({ task, onSubmit, onClose, ...props }) => {
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

  return (
    <Modal animationType="slide" {...props}>
      <View style={styles.modalOverlay}>
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
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  modalContent: {
    width: '80%',
    padding: 20,
    backgroundColor: '#fff',
    borderRadius: 10,
    alignItems: 'center',
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
