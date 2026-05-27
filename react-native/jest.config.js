module.exports = {
  preset: 'react-native',
  setupFiles: ['./jest.setup.js'],
  transformIgnorePatterns: [
    'node_modules/(?!(react-native|@react-native|react-native-bouncy-checkbox|react-native-safe-area-context)/)',
  ],
  moduleNameMapper: {
    '^@env$': '<rootDir>/__mocks__/@env.js',
  },
};
