import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

type Props = {
  appId: string,
  token: string,
}

const DittoInfo: React.FC<Props> = ({ appId, token }) => {
  return (
    <View style={styles.view}>
      <Text style={styles.title}>Ditto Tasks</Text>
      <Text style={styles.info}>{`AppID: ${appId}`}</Text>
      <Text style={styles.info}>{`Token: ${token}`}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  view: {
    paddingTop: 20,
    paddingBottom: 10,
    // borderWidth: 1,
  },
  title: {
    fontSize: 20,
    textAlign: 'center',
    fontWeight: 'bold',
  },
  info: {
    paddingTop: 10,
    textAlign: 'center',
  },
});

export default DittoInfo;
