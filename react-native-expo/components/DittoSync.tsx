import React from 'react';
import { StyleSheet, Switch, SwitchProps, Text, View } from 'react-native';

const DittoSync: React.FC<SwitchProps> = (props) => {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>Sync Enabled</Text>
      <Switch thumbColor="#fff" trackColor={{ true: '#6D28D9' }} {...props} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingVertical: 15,
    paddingHorizontal: 25,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'flex-start',
  },
  text: {
    flexGrow: 1,
    fontSize: 20,
  },
});

export default DittoSync;
