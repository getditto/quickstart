import React from 'react';
import {useState} from 'react';
import {
  Button,
  Modal,
  ModalProps,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

type NewTaskModalProps = {
  onSubmit: (taskName: string) => void;
  onClose?: () => void;
};

type Props = NewTaskModalProps;

const NewTaskModal: React.FC<Props> = ({onSubmit, onClose, ...props}) => {
  const [input, setInput] = useState('');

  const submit = () => {
    if (input !== '') {
      onSubmit(input);
      setInput('');
    }
  };

  if (!visible) {
    return null;
  }

  return (
    <Modal animationType="slide" {...props}>
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>New Task</Text>
          <TextInput
            style={styles.input}
            value={input}
            onChangeText={setInput}
          />
          <Button title="Submit" onPress={submit} />
          <Button title="Close" onPress={onClose} />
        </View>
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

export default NewTaskModal;
