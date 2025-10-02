#!/usr/bin/env node

/**
 * bump-versions.js - Update Ditto SDK versions across all quickstart apps
 *
 * Usage: node scripts/bump-versions.js -t <version> [options]
 *
 * Options:
 *   -t, --to <version>   New SDK version (required)
 *   -n, --dry-run        Preview changes without modifying files
 *   -h, --help           Show help
 *
 * Excludes: Kotlin Multiplatform, Java Spring (not on synchronized releases)
 */

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

// File paths that match these patterns will not be processed
const EXCLUSIONS = [
  // Not yet on synchronized releases
  "/kotlin-multiplatform",
  "/java-spring",

  // Common exclusions
  "/node_modules",
];

// Configuration for different project types
const PROJECT_CONFIGS = [
  {
    name: "JavaScript/TypeScript (package.json)",
    pattern: "**/package.json",
    regex:
      /("@dittolive\/ditto":\s*")[\^~]?[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
    replacement: (match, prefix, suffix) => `${prefix}^VERSION${suffix}`,
    lockfileUpdate: ["npm install --legacy-peer-deps", "npm install"],
  },
  {
    name: "Android libs.versions.toml",
    pattern: "**/libs.versions.toml",
    regex: /(ditto\s*=\s*")[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
    replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
  },
  {
    name: "Android build.gradle.kts",
    pattern: "**/build.gradle.kts",
    regex: [
      /(implementation\(["\']live\.ditto:ditto-cpp:)[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(["\'])/g,
      /(implementation\(["\']com\.ditto:ditto-java:)[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(["\'])/g,
      /(implementation\(["\']com\.ditto:ditto-binaries:)[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(["\'])/g,
    ],
    replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
  },
  {
    name: "Flutter pubspec.yaml",
    pattern: "**/pubspec.yaml",
    regex: /(ditto_live:\s*)[\^~]?[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?/g,
    replacement: (match, prefix) => `${prefix}^VERSION`,
    lockfileUpdate: ["flutter pub get"],
  },
  {
    name: "Rust Cargo.toml",
    pattern: "**/Cargo.toml",
    regex:
      /(dittolive-ditto\s*=\s*")[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(")/g,
    replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
    lockfileUpdate: ["cargo update"],
  },
  {
    name: ".NET .csproj",
    pattern: "**/*.csproj",
    regex:
      /(PackageReference\s+Include=["']Ditto["']\s+Version=["'])[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?(["'])/g,
    replacement: (match, prefix, suffix) => `${prefix}VERSION${suffix}`,
    lockfileUpdate: ["dotnet restore"],
  },
];

// Lockfile update commands for different project directories
const LOCKFILE_COMMANDS = {
  "react-native": [
    "npm install --legacy-peer-deps",
    "(cd ios && pod update DittoReactNativeIOS)",
  ],
  "react-native-expo": [
    "npm install",
    "(cd ios && pod update DittoReactNativeIOS)",
  ],
  "javascript-tui": "npm install",
  "javascript-web": "npm install",
  flutter_app: "flutter pub get",
  "rust-tui": "cargo update",
  "dotnet-tui/DittoDotNetTasksConsole": "dotnet restore",
};

function printHelp() {
  console.log(`
bump-versions.js — update Ditto quickstart app versions

Usage:
  node scripts/bump-versions.js -t 4.12.2-rc.1 [options]

Options:
  -t, --to <version>    New version to set (required)
  -n, --dry-run        Show what would change; do not write files
  -h, --help           Show help

Notes:
- Automatically detects current versions using regex patterns
- Uses platform-specific version patterns for each project type  
- Excludes Kotlin Multiplatform and Java Spring (not yet on synchronized releases)
- Updates lockfiles automatically unless --dry-run is specified
`);
}

function parseArgs() {
  const args = process.argv.slice(2);
  const config = {
    version: null,
    dryRun: false,
    help: false,
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    switch (arg) {
      case "-t":
      case "--to":
        config.version = args[++i];
        break;
      case "-n":
      case "--dry-run":
        config.dryRun = true;
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
    console.error("Error: --to version is required");
    printHelp();
    process.exit(1);
  }

  return config;
}

function findFiles(pattern) {
  try {
    // Convert glob pattern to find pattern
    const findPattern = pattern.replace("**/", "");
    let cmd = `find . -name "${findPattern}" -type f`;

    const output = execSync(cmd, { encoding: "utf8" }).trim();
    return output ? output.split("\n").filter((f) => f.trim()) : [];
  } catch (error) {
    return [];
  }
}

function updateFile(filePath, config, version, dryRun) {
  try {
    const content = fs.readFileSync(filePath, "utf8");
    let modified = false;
    let newContent = content;
    let matchCount = 0;

    // Handle multiple regex patterns
    const regexList = Array.isArray(config.regex)
      ? config.regex
      : [config.regex];

    for (const regex of regexList) {
      const matches = [...content.matchAll(regex)];
      matchCount += matches.length;

      if (matches.length > 0) {
        modified = true;
        newContent = newContent.replace(regex, (match, ...groups) => {
          const replacement = config.replacement(match, ...groups);
          return replacement.replace("VERSION", version);
        });
      }
    }

    if (modified) {
      if (dryRun) {
        console.log(`Would update ${filePath} (${matchCount} occurrence(s))`);
        // Show the actual matches
        for (const regex of regexList) {
          const matches = [...content.matchAll(regex)];
          matches.forEach((match, index) => {
            const lineNum = content
              .substring(0, match.index)
              .split("\n").length;
            console.log(`  Line ${lineNum}: ${match[0]}`);
          });
        }
      } else {
        fs.writeFileSync(filePath, newContent, "utf8");
        console.log(`Updated ${filePath} (${matchCount} occurrence(s))`);
      }
    }

    return modified;
  } catch (error) {
    console.error(`Error processing ${filePath}: ${error.message}`);
    return false;
  }
}

function updateLockfiles(updatedProjects, dryRun) {
  if (dryRun) return;

  const lockfileUpdates = new Set();

  // Determine which lockfiles need updating based on updated files
  for (const filePath of updatedProjects) {
    if (
      filePath.includes("package.json") &&
      !filePath.includes("node_modules")
    ) {
      if (filePath.includes("react-native/"))
        lockfileUpdates.add("react-native");
      else if (filePath.includes("react-native-expo/"))
        lockfileUpdates.add("react-native-expo");
      else if (filePath.includes("javascript-tui/"))
        lockfileUpdates.add("javascript-tui");
      else if (filePath.includes("javascript-web/"))
        lockfileUpdates.add("javascript-web");
    } else if (filePath.includes("pubspec.yaml")) {
      lockfileUpdates.add("flutter_app");
    } else if (filePath.includes("Cargo.toml")) {
      lockfileUpdates.add("rust-tui");
    } else if (filePath.includes(".csproj")) {
      lockfileUpdates.add("dotnet-tui/DittoDotNetTasksConsole");
    }
  }

  if (lockfileUpdates.size > 0) {
    console.log("Updating lockfiles...");

    for (const project of lockfileUpdates) {
      const commands = LOCKFILE_COMMANDS[project];
      if (!commands) continue;

      const commandArray = Array.isArray(commands) ? commands : [commands];

      try {
        const projectPath = path.join(process.cwd(), project);
        if (fs.existsSync(projectPath)) {
          for (const command of commandArray) {
            console.log(`Running: cd ${project} && ${command}`);

            execSync(command, {
              cwd: projectPath,
              stdio: "inherit",
            });
          }
        }
      } catch (error) {
        console.warn(
          `Warning: Failed to update lockfile for ${project}: ${error.message}`
        );
        throw error;
      }
    }

    console.log("Lockfile updates completed.");
  }
}

function main() {
  const { version, dryRun } = parseArgs();

  if (dryRun) {
    console.log("-- DRY RUN --");
  }

  let totalFiles = 0;
  let updatedFiles = [];

  // Process each project type
  for (const config of PROJECT_CONFIGS) {
    console.log(`\nProcessing ${config.name}...`);

    const files = findFiles(config.pattern);

    for (const filePath of files) {
      // Skip excluded paths
      if (EXCLUSIONS.some((pattern) => filePath.includes(pattern))) {
        continue;
      }

      const wasUpdated = updateFile(filePath, config, version, dryRun);
      if (wasUpdated) {
        updatedFiles.push(filePath);
      }
    }

    totalFiles += files.length;
  }

  console.log(
    `\nProcessed ${totalFiles} files, updated ${updatedFiles.length} file(s) to version ${version}.`
  );

  if (dryRun) {
    console.log("Total files that would be updated:", updatedFiles.length);
  } else {
    updateLockfiles(updatedFiles, dryRun);
  }
}

if (require.main === module) {
  main();
}
