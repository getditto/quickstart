const { withProjectBuildGradle } = require('@expo/config-plugins');

const KOTLIN_VERSION = '2.3.0';
const KOTLIN_GRADLE_PLUGIN =
  /classpath\(\s*(['"])org\.jetbrains\.kotlin:kotlin-gradle-plugin(?:\:[^'"]+)?\1\s*\)/g;

function pinKotlinGradlePlugin(contents) {
  let matches = 0;
  const updated = contents.replace(KOTLIN_GRADLE_PLUGIN, (_match, quote) => {
    matches += 1;
    return `classpath(${quote}org.jetbrains.kotlin:kotlin-gradle-plugin:${KOTLIN_VERSION}${quote})`;
  });

  if (matches !== 1) {
    throw new Error(
      `Expected one Kotlin Gradle plugin classpath in android/build.gradle; found ${matches}`,
    );
  }
  return updated;
}

module.exports = function withKotlinGradlePlugin(config) {
  return withProjectBuildGradle(config, (modConfig) => {
    modConfig.modResults.contents = pinKotlinGradlePlugin(
      modConfig.modResults.contents,
    );
    return modConfig;
  });
};

module.exports.pinKotlinGradlePlugin = pinKotlinGradlePlugin;
