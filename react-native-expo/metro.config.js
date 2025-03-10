const path = require("path");
const { getDefaultConfig } = require("@expo/metro-config");

const projectRoot = __dirname;
const externalProjectRoot = path.resolve(projectRoot, "../react-native");

const config = getDefaultConfig(projectRoot);

config.watchFolders = [externalProjectRoot];
config.resolver = {
  ...config.resolver,
  extraNodeModules: {
    react: path.resolve(projectRoot, "../react-native/node_modules/react"),
    "react-native": path.resolve(
      projectRoot,
      "../react-native/node_modules/react-native"
    ),
  },
};

module.exports = config;
