const { getDefaultConfig } = require("@expo/metro-config");

const projectRoot = __dirname;

const config = getDefaultConfig(projectRoot);

module.exports = config;
