/* eslint-env jest */

// Mock the Ditto native SDK
jest.mock('@dittolive/ditto', () => ({
  Ditto: {
    open: jest.fn().mockResolvedValue({
      auth: {
        loginSupported: true,
        setExpirationHandler: jest.fn().mockResolvedValue(undefined),
        login: jest.fn().mockResolvedValue({clientInfo: null, error: null}),
      },
      sync: {
        start: jest.fn(),
        stop: jest.fn(),
        registerSubscription: jest.fn().mockReturnValue({cancel: jest.fn()}),
      },
      store: {
        execute: jest.fn().mockResolvedValue({items: []}),
        registerObserver: jest.fn().mockReturnValue({cancel: jest.fn()}),
      },
      close: jest.fn().mockResolvedValue(undefined),
    }),
  },
  DittoConfig: jest.fn(),
  Authenticator: {
    DEVELOPMENT_PROVIDER: 'development',
  },
}));

// Mock react-native-safe-area-context
jest.mock('react-native-safe-area-context', () => {
  const React = require('react');
  return {
    SafeAreaProvider: ({children}) =>
      React.createElement(React.Fragment, null, children),
    SafeAreaView: ({children}) =>
      React.createElement(React.Fragment, null, children),
    useSafeAreaInsets: () => ({top: 0, right: 0, bottom: 0, left: 0}),
  };
});
