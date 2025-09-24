import React from 'react';
import {StyleSheet} from 'react-native';
import BouncyCheckbox from 'react-native-bouncy-checkbox';

type Props = {
  checked: boolean;
  onPress: () => void;
  testID?: string;
};

const TaskDone: React.FC<Props> = ({checked, onPress, testID}) => {
  return (
    <BouncyCheckbox
      style={styles.button}
      isChecked={checked}
      onPress={onPress}
      fillColor="#7C3AED"
      useBuiltInState={false}
      testID={testID}
    />
  );
};

const styles = StyleSheet.create({
  button: {
    flexShrink: 1,
    marginRight: 8,
  },
});

export default TaskDone;
