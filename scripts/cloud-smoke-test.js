#!/usr/bin/env node

/**
 * Cloud Smoke Test Script
 * 
 * Dispatches all BrowserStack workflows with custom Ditto configuration
 * and waits for completion, reporting results.
 * 
 * Usage: node scripts/cloud-smoke-test.js [options]
 * 
 * Options:
 *   --websocket-url <url>     Custom websocket URL (optional, defaults to env var)
 *   --app-id <id>            Custom app ID (optional, defaults to env var)
 *   --playground-token <token> Custom playground token (optional, defaults to env var)
 *   --auth-url <url>         Custom auth URL (optional, defaults to env var)
 *   --help                   Show this help message
 * 
 * Environment variables (used as defaults):
 *   DITTO_WEBSOCKET_URL      Default websocket URL
 *   DITTO_APP_ID             Default app ID
 *   DITTO_PLAYGROUND_TOKEN   Default playground token
 *   DITTO_AUTH_URL           Default auth URL
 */

const { execSync, spawn } = require('child_process');
const path = require('path');

// ANSI color codes for better output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m'
};

const workflows = [
  'android-kotlin-ci.yml',
  'swift-ci.yml', 
  'javascript-web-browserstack.yml',
  'android-cpp-browserstack.yml'
];

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

function execCommand(command, options = {}) {
  try {
    return execSync(command, { 
      encoding: 'utf8',
      stdio: options.silent ? 'pipe' : 'inherit',
      ...options
    }).trim();
  } catch (error) {
    if (!options.allowFailure) {
      log(`❌ Command failed: ${command}`, colors.red);
      log(error.message, colors.red);
      process.exit(1);
    }
    return null;
  }
}

function parseArguments() {
  const args = process.argv.slice(2);
  const config = {
    websocketUrl: process.env.DITTO_WEBSOCKET_URL,
    appId: process.env.DITTO_APP_ID,
    playgroundToken: process.env.DITTO_PLAYGROUND_TOKEN,
    authUrl: process.env.DITTO_AUTH_URL
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    
    if (arg === '--help' || arg === '-h') {
      showHelp();
      process.exit(0);
    } else if (arg === '--websocket-url') {
      config.websocketUrl = args[++i];
    } else if (arg === '--app-id') {
      config.appId = args[++i];
    } else if (arg === '--playground-token') {
      config.playgroundToken = args[++i];
    } else if (arg === '--auth-url') {
      config.authUrl = args[++i];
    } else {
      log(`❌ Unknown argument: ${arg}`, colors.red);
      log('Use --help for usage information', colors.yellow);
      process.exit(1);
    }
  }

  return config;
}

function showHelp() {
  console.log(`
🚭 Ditto Cloud Smoke Test

Dispatches all BrowserStack workflows with custom Ditto configuration
and waits for completion, reporting results.

Usage: node scripts/cloud-smoke-test.js [options]

Options:
  --websocket-url <url>     Custom websocket URL (optional, defaults to env var)
  --app-id <id>            Custom app ID (optional, defaults to env var)
  --playground-token <token> Custom playground token (optional, defaults to env var)
  --auth-url <url>         Custom auth URL (optional, defaults to env var)
  --help, -h               Show this help message

Environment variables (used as defaults):
  DITTO_WEBSOCKET_URL      Default websocket URL
  DITTO_APP_ID             Default app ID
  DITTO_PLAYGROUND_TOKEN   Default playground token
  DITTO_AUTH_URL           Default auth URL

Examples:
  # Test with all defaults from environment variables
  node scripts/cloud-smoke-test.js
  
  # Test with custom websocket URL only
  node scripts/cloud-smoke-test.js --websocket-url wss://test.example.com/ws
  
  # Test with custom websocket URL and app ID
  node scripts/cloud-smoke-test.js --websocket-url wss://test.example.com/ws --app-id my-app-id
  
  # Test with all custom values
  node scripts/cloud-smoke-test.js \\
    --websocket-url wss://test.example.com/ws \\
    --app-id my-app-id \\
    --playground-token my-token \\
    --auth-url https://auth.example.com
`);
}

function validateConfig(config) {
  // Validate websocket URL format if provided
  if (config.websocketUrl) {
    try {
      const parsed = new URL(config.websocketUrl);
      if (!['ws:', 'wss:'].includes(parsed.protocol)) {
        throw new Error('URL must use ws:// or wss:// protocol');
      }
    } catch (error) {
      log(`❌ Invalid websocket URL: ${error.message}`, colors.red);
      return false;
    }
  }

  // Warn about missing values - all are optional now
  if (!config.websocketUrl) {
    log('⚠️  No websocket URL specified (using workflow default)', colors.yellow);
  }
  if (!config.appId) {
    log('⚠️  No app ID specified (using workflow default)', colors.yellow);
  }
  if (!config.playgroundToken) {
    log('⚠️  No playground token specified (using workflow default)', colors.yellow);
  }
  if (!config.authUrl) {
    log('⚠️  No auth URL specified (using workflow default)', colors.yellow);
  }

  return true;
}

function getCurrentBranch() {
  return execCommand('git branch --show-current', { silent: true });
}

function dispatchWorkflow(workflow, config, branch) {
  log(`📤 Dispatching workflow: ${workflow}`, colors.blue);
  
  let command = `gh workflow run "${workflow}" --ref "${branch}"`;
  
  // Add optional parameters if provided
  if (config.websocketUrl) {
    command += ` -f websocket_url="${config.websocketUrl}"`;
  }
  if (config.appId) {
    command += ` -f app_id="${config.appId}"`;
  }
  if (config.playgroundToken) {
    command += ` -f playground_token="${config.playgroundToken}"`;
  }
  if (config.authUrl) {
    command += ` -f auth_url="${config.authUrl}"`;
  }
  
  execCommand(command, { silent: true });
  
  // Small delay to ensure workflow appears in listing
  execCommand('sleep 2', { silent: true });
}

function getLatestWorkflowRun(workflow) {
  const command = `gh run list --workflow="${workflow}" --limit=1 --json databaseId,status,conclusion,headBranch,createdAt`;
  const output = execCommand(command, { silent: true });
  const runs = JSON.parse(output);
  return runs.length > 0 ? runs[0] : null;
}

function getWorkflowRunDetails(runId) {
  const command = `gh run view ${runId} --json status,conclusion,url,workflowName,jobs`;
  const output = execCommand(command, { silent: true });
  return JSON.parse(output);
}

function waitForWorkflowCompletion(runIds, maxWaitMinutes = 45) {
  log(`⏳ Waiting for ${runIds.length} workflows to complete (max ${maxWaitMinutes} minutes)...`, colors.yellow);
  
  const startTime = Date.now();
  const maxWaitMs = maxWaitMinutes * 60 * 1000;
  const checkIntervalMs = 30 * 1000; // Check every 30 seconds
  
  let completed = new Set();
  let lastStatus = new Map();
  
  while (completed.size < runIds.length && (Date.now() - startTime) < maxWaitMs) {
    for (const runId of runIds) {
      if (completed.has(runId)) continue;
      
      const details = getWorkflowRunDetails(runId);
      const status = `${details.status}${details.conclusion ? `:${details.conclusion}` : ''}`;
      
      // Only log status changes
      if (lastStatus.get(runId) !== status) {
        log(`  ${details.workflowName}: ${status}`, colors.cyan);
        lastStatus.set(runId, status);
      }
      
      if (details.status === 'completed') {
        completed.add(runId);
      }
    }
    
    if (completed.size < runIds.length) {
      // Sleep for check interval
      execCommand(`sleep ${checkIntervalMs / 1000}`, { silent: true });
    }
  }
  
  const elapsedMinutes = Math.round((Date.now() - startTime) / 60000);
  
  if (completed.size === runIds.length) {
    log(`✅ All workflows completed after ${elapsedMinutes} minutes`, colors.green);
    return true;
  } else {
    log(`⏰ Timeout after ${elapsedMinutes} minutes. ${completed.size}/${runIds.length} workflows completed`, colors.yellow);
    return false;
  }
}

function main() {
  const config = parseArguments();
  
  log('🚀 Ditto Cloud Smoke Test', colors.bright);
  log('========================', colors.bright);
  log('');
  
  // Validate configuration
  if (!validateConfig(config)) {
    process.exit(1);
  }
  
  log('📋 Configuration:', colors.magenta);
  log(`   Websocket URL: ${config.websocketUrl}`, colors.magenta);
  if (config.appId) {
    log(`   App ID: ${config.appId}`, colors.magenta);
  }
  if (config.playgroundToken) {
    log(`   Playground Token: ${config.playgroundToken.substring(0, 8)}...`, colors.magenta);
  }
  if (config.authUrl) {
    log(`   Auth URL: ${config.authUrl}`, colors.magenta);
  }
  
  // Check if gh CLI is available
  try {
    execCommand('gh --version', { silent: true });
  } catch {
    log('❌ GitHub CLI (gh) is required but not found', colors.red);
    log('Install it from: https://github.com/cli/cli#installation', colors.yellow);
    process.exit(1);
  }
  
  // Get current branch
  const branch = getCurrentBranch();
  log(`🌿 Using branch: ${branch}`, colors.magenta);
  log('');
  
  // Dispatch all workflows
  log('📤 Dispatching workflows...', colors.blue);
  const dispatchedRuns = new Map();
  let dispatchFailures = 0;
  
  for (const workflow of workflows) {
    // Get the run count before dispatch to identify our run
    const beforeRuns = execCommand(`gh run list --workflow="${workflow}" --limit=1 --json databaseId`, { silent: true });
    const beforeCount = JSON.parse(beforeRuns).length;
    
    dispatchWorkflow(workflow, config, branch);
    
    // Find the new run
    let attempts = 0;
    let newRun = null;
    
    while (attempts < 10 && !newRun) {
      execCommand('sleep 2', { silent: true });
      const afterRuns = execCommand(`gh run list --workflow="${workflow}" --limit=5 --json databaseId,status,headBranch,createdAt`, { silent: true });
      const runs = JSON.parse(afterRuns);
      
      // Find the newest run for our branch
      newRun = runs.find(run => 
        run.headBranch === branch && 
        new Date(run.createdAt) > new Date(Date.now() - 2 * 60 * 1000) // Within last 2 minutes
      );
      
      attempts++;
    }
    
    if (newRun) {
      dispatchedRuns.set(workflow, newRun.databaseId);
      log(`  ✅ ${workflow} → Run #${newRun.databaseId}`, colors.green);
    } else {
      log(`  ❌ Failed to find dispatched run for ${workflow}`, colors.red);
      dispatchFailures++;
    }
  }
  
  if (dispatchedRuns.size === 0) {
    log('❌ No workflows were successfully dispatched', colors.red);
    process.exit(1);
  }
  
  log('');
  
  // Wait for completion
  const runIds = Array.from(dispatchedRuns.values());
  const allCompleted = waitForWorkflowCompletion(runIds);
  
  log('');
  log('📊 Final Results:', colors.bright);
  log('================', colors.bright);
  
  let hasFailures = dispatchFailures > 0;
  
  for (const [workflow, runId] of dispatchedRuns.entries()) {
    const details = getWorkflowRunDetails(runId);
    const success = details.conclusion === 'success';
    const icon = success ? '✅' : '❌';
    const color = success ? colors.green : colors.red;
    
    if (!success) hasFailures = true;
    
    log(`${icon} ${workflow}: ${details.conclusion || details.status}`, color);
    log(`   URL: ${details.url}`, colors.cyan);
    
    if (!success && details.jobs) {
      // Show failed jobs
      const failedJobs = details.jobs.filter(job => job.conclusion === 'failure');
      if (failedJobs.length > 0) {
        log(`   Failed jobs:`, colors.red);
        failedJobs.forEach(job => {
          log(`     - ${job.name}`, colors.red);
        });
      }
    }
    log('');
  }
  
  if (!allCompleted) {
    log('⚠️  Some workflows may still be running. Check the URLs above for latest status.', colors.yellow);
    hasFailures = true;
  }
  
  if (hasFailures) {
    log('❌ Some tests failed. Check the workflow runs for details.', colors.red);
    process.exit(1);
  } else {
    log('🎉 All smoke tests passed!', colors.green);
    process.exit(0);
  }
}

if (require.main === module) {
  try {
    main();
  } catch (error) {
    log(`💥 Unexpected error: ${error.message}`, colors.red);
    console.error(error.stack);
    process.exit(1);
  }
}