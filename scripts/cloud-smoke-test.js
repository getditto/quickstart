#!/usr/bin/env node

/**
 * Cloud Smoke Test Script
 * 
 * Dispatches all BrowserStack workflows with a custom websocket URL
 * and waits for completion, reporting results.
 * 
 * Usage: node scripts/cloud-smoke-test.js <websocket-url>
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

function validateWebsocketUrl(url) {
  try {
    const parsed = new URL(url);
    if (!['ws:', 'wss:'].includes(parsed.protocol)) {
      throw new Error('URL must use ws:// or wss:// protocol');
    }
    return true;
  } catch (error) {
    log(`❌ Invalid websocket URL: ${error.message}`, colors.red);
    return false;
  }
}

function getCurrentBranch() {
  return execCommand('git branch --show-current', { silent: true });
}

function dispatchWorkflow(workflow, websocketUrl, branch) {
  log(`📤 Dispatching workflow: ${workflow}`, colors.blue);
  
  const command = `gh workflow run "${workflow}" --ref "${branch}" -f websocket_url="${websocketUrl}"`;
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
  const args = process.argv.slice(2);
  
  if (args.length !== 1) {
    log('Usage: node scripts/cloud-smoke-test.js <websocket-url>', colors.red);
    log('', colors.reset);
    log('Example:', colors.yellow);
    log('  node scripts/cloud-smoke-test.js wss://cloud.ditto.live/ws', colors.yellow);
    process.exit(1);
  }
  
  const websocketUrl = args[0];
  
  log('🚀 Ditto Cloud Smoke Test', colors.bright);
  log('========================', colors.bright);
  log('');
  
  // Validate websocket URL
  if (!validateWebsocketUrl(websocketUrl)) {
    process.exit(1);
  }
  
  log(`📋 Websocket URL: ${websocketUrl}`, colors.magenta);
  
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
    
    dispatchWorkflow(workflow, websocketUrl, branch);
    
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