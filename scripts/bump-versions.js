#!/usr/bin/env node

/**
 * bump-versions.js - Update Ditto SDK versions across all quickstart apps
 *
 * Usage: node scripts/bump-versions.js -t <version>
 *
 * Options:
 *   -t, --to <version>   New SDK version (required)
 *   -h, --help           Show help
 */

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

// ANSI color codes
const colors = {
  reset: "\x1b[0m",
  bright: "\x1b[1m",
  dim: "\x1b[2m",
  red: "\x1b[31m",
  green: "\x1b[32m",
  yellow: "\x1b[33m",
  blue: "\x1b[34m",
  cyan: "\x1b[36m",
};

const log = {
  info: (msg) => console.log(`${colors.cyan}ℹ${colors.reset} ${msg}`),
  success: (msg) => console.log(`${colors.green}✓${colors.reset} ${msg}`),
  error: (msg) => console.error(`${colors.red}✗${colors.reset} ${msg}`),
  warn: (msg) => console.warn(`${colors.yellow}⚠${colors.reset} ${msg}`),
  title: (msg) =>
    console.log(`\n${colors.bright}${colors.blue}${msg}${colors.reset}`),
  dim: (msg) => console.log(`${colors.dim}${msg}${colors.reset}`),
};

// Configuration for each app directory
const APP_CONFIGS = {
  "react-native": {
    skip: true,
    files: [
      {
        path: "package.json",
        regex:
          /("@dittolive\/ditto":\s*")[\^~]?[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: [
      "npm install --legacy-peer-deps --no-audit",
      "pod repo update",
      "(cd ios && bundle exec pod update)",
      "(cd macos && bundle exec pod update)",
    ],
  },

  "react-native-expo": {
    skip: true,
    files: [
      {
        path: "package.json",
        regex:
          /("@dittolive\/ditto":\s*")[\^~]?[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: ["npm install --no-audit", "(cd ios && pod update)"],
  },

  "javascript-tui": {
    skip: true,
    files: [
      {
        path: "package.json",
        regex:
          /("@dittolive\/ditto":\s*")[\^~]?[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: ["npm install"],
  },

  "javascript-web": {
    skip: true,
    files: [
      {
        path: "package.json",
        regex:
          /("@dittolive\/ditto":\s*")[\^~]?[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: ["npm install"],
  },

  "android-kotlin": {
    skip: true,
    files: [
      {
        path: "QuickStartTasks/gradle/libs.versions.toml",
        regex: /(ditto\s*=\s*")[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: ["(cd QuickStartTasks && ./gradlew --refresh-dependencies)"],
  },

  "android-java": {
    skip: true,
    files: [
      {
        path: "gradle/libs.versions.toml",
        regex: /(ditto\s*=\s*")[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: ["./gradlew --refresh-dependencies"],
  },

  "android-cpp": {
    skip: true,
    files: [
      {
        path: "QuickStartTasksCPP/app/build.gradle.kts",
        regex:
          /(implementation\("live\.ditto:ditto-cpp:)[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?("\))/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: [
      "(cd QuickStartTasksCPP && ./gradlew --refresh-dependencies)",
    ],
  },

  "cpp-tui": {
    skip: true,
    files: [
      {
        path: "taskscpp/Makefile",
        regex:
          /(DITTO_SDK_VERSION \?= )[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?/g,
        replacement: (match, prefix) => `${prefix}VERSION`,
      },
    ],
    lockCommands: [],
  },

  swift: {
    skip: true,
    files: [
      {
        path: "Tasks.xcodeproj/project.pbxproj",
        regex:
          /(minimumVersion\s*=\s*)[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?;/g,
        replacement: (match, prefix) => `${prefix}VERSION;`,
      },
    ],
    lockCommands: [
      "xcodebuild -resolvePackageDependencies -project Tasks.xcodeproj -scheme Tasks",
    ],
  },

  flutter_app: {
    skip: false,
    files: [
      {
        path: "pubspec.yaml",
        regex:
          /(ditto_live:\s*)[\^~]?[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?/g,
        replacement: (match, prefix) => `${prefix}VERSION`,
      },
    ],
    lockCommands: [
      "flutter clean",
      "flutter pub get",
      "pod repo update",
      "flutter precache --ios --macos",
      "(cd ios && pod update DittoFlutter)",
      "(cd macos && pod update DittoFlutter)",
    ],
  },

  "rust-tui": {
    skip: true,
    files: [
      {
        path: "Cargo.toml",
        regex:
          /(dittolive-ditto\s*=\s*")[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: ["cargo update"],
  },

  "dotnet-tui": {
    skip: true,
    files: [
      {
        path: "DittoDotNetTasksConsole/DittoDotNetTasksConsole.csproj",
        regex:
          /(PackageReference\s+Include=["']Ditto["']\s+Version=["'])[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(["'])/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
      {
        path: "DittoDotNetTasksConsole.Tests/DittoDotNetTasksConsole.Tests.csproj",
        regex:
          /(PackageReference\s+Include=["']Ditto["']\s+Version=["'])[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(["'])/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: [], // No restore needed - .csproj is source of truth
  },

  "dotnet-maui": {
    skip: true,
    files: [
      {
        path: "DittoMauiTasksApp/DittoMauiTasksApp.csproj",
        regex:
          /(PackageReference\s+Include=["']Ditto["']\s+Version=["'])[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(["'])/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: [], // No restore needed - .csproj is source of truth
  },

  "dotnet-winforms": {
    skip: true,
    files: [
      {
        path: "TasksApp/DittoTasksApp.csproj",
        regex:
          /(PackageReference\s+Include=["']Ditto["']\s+Version=["'])[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(["'])/g,
        replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
      },
    ],
    lockCommands: [], // No restore needed - .csproj is source of truth
  },

  // Excluded apps
  "kotlin-multiplatform": {
    skip: true,
    reason: "Requires Ditto v5+, not on synchronized releases",
  },

  "java-server": {
    skip: true,
    reason: "Requires Ditto v5+, not on synchronized releases",
  },
};

function printHelp() {
  console.log(`
${colors.bright}bump-versions.js${colors.reset} — update Ditto quickstart app versions

${colors.bright}Usage:${colors.reset}
  node scripts/bump-versions.js -t 4.12.2-rc.1

${colors.bright}Options:${colors.reset}
  -t, --to <version>    New version to set (required)
  -h, --help           Show help

${colors.bright}Notes:${colors.reset}
- Excludes: kotlin-multiplatform, java-server (not on synchronized releases)
- Updates lockfiles automatically for each app
- Stops on first error
`);
}

function parseArgs() {
  const args = process.argv.slice(2);
  const config = {
    version: null,
    help: false,
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    switch (arg) {
      case "-t":
      case "--to":
        config.version = args[++i];
        break;
      case "-h":
      case "--help":
        config.help = true;
        break;
      default:
        console.error(`Unknown argument: ${arg}`);
        process.exit(1);
    }
  }

  if (config.help) {
    printHelp();
    process.exit(0);
  }

  if (!config.version) {
    log.error("--to version is required");
    printHelp();
    process.exit(1);
  }

  return config;
}

function updateFile(appDir, fileConfig, version) {
  const filePath = path.join(process.cwd(), appDir, fileConfig.path);

  if (!fs.existsSync(filePath)) {
    log.error(
      `expected to find file "${fileConfig.path}" in directory "${appDir}"`
    );
    process.exit(1);
  }

  const content = fs.readFileSync(filePath, "utf8");
  let newContent = content;

  const matches = [...content.matchAll(fileConfig.regex)];

  if (matches.length === 0) {
    log.error(
      `expected to find at least one match for pattern in file "${appDir}/${fileConfig.path}"`
    );
    process.exit(1);
  }

  newContent = newContent.replace(fileConfig.regex, (match, ...groups) => {
    const replacement = fileConfig.replacement(match, ...groups);
    return replacement.replace(/\bVERSION\b/g, version);
  });

  if (newContent !== content) {
    fs.writeFileSync(filePath, newContent, "utf8");
    log.success(
      `Updated ${colors.dim}${appDir}/${fileConfig.path}${colors.reset} (${
        matches.length
      } occurrence${matches.length > 1 ? "s" : ""})`
    );
    return true;
  }

  return false;
}

function runLockCommands(appDir, commands) {
  const appPath = path.join(process.cwd(), appDir);

  for (const command of commands) {
    try {
      log.dim(`  Running: ${command}`);
      execSync(command, {
        cwd: appPath,
        stdio: "inherit",
      });
    } catch (error) {
      log.error(`Failed to run lock command in ${appDir}: ${command}`);
      throw error;
    }
  }
}

function main() {
  const { version } = parseArgs();

  log.title(`🌄 Bumping Ditto SDK to version ${version}`);

  let totalApps = 0;
  let updatedApps = 0;
  let skippedApps = 0;

  for (const [appDir, config] of Object.entries(APP_CONFIGS)) {
    totalApps++;

    if (config.skip) {
      skippedApps++;
      log.dim(`\n⊘ Skipping ${appDir} - ${config.reason}`);
      continue;
    }

    log.title(`📦 Processing ${appDir}`);

    let appModified = false;

    // Update all files for this app
    for (const fileConfig of config.files) {
      const wasModified = updateFile(appDir, fileConfig, version);
      if (wasModified) {
        appModified = true;
      }
    }

    // If any files were updated, run lock commands
    if (appModified) {
      updatedApps++;
      if (config.lockCommands && config.lockCommands.length > 0) {
        log.info("Updating lockfiles...");
        runLockCommands(appDir, config.lockCommands);
        log.success("Lockfiles updated");
      }
    } else {
      log.dim("  No changes needed");
    }
  }

  log.title("✨ Summary");
  console.log(
    `  Total apps:   ${totalApps}\n` +
      `  Updated:      ${colors.green}${updatedApps}${colors.reset}\n` +
      `  Skipped:      ${colors.dim}${skippedApps}${colors.reset}\n` +
      `  Version:      ${colors.cyan}${version}${colors.reset}`
  );
}

if (require.main === module) {
  try {
    main();
  } catch (error) {
    log.error(`Script failed: ${error.message}`);
    process.exit(1);
  }
}
