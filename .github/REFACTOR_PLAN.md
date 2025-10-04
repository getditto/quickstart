# BrowserStack CI Workflows Refactor Plan

**Status**: In Progress
**Created**: 2025-10-03
**Branch**: `teodorciuraru/standardize-browserstack-ci`

## Objectives

Fix all high-priority issues and standardize workflows for production readiness.

## Changes to Apply

### 1. ✅ Device Configuration (DONE)
- [x] Created `.github/browserstack-devices.json` with centralized device lists
- Device sets defined:
  - `android-espresso`: 4 devices (full matrix)
  - `android-espresso-compact`: 3 devices
  - `android-appium`: 1 device
  - `android-maestro`: 2 devices
  - `ios-xcuitest`: 3 devices
  - `ios-xcuitest-compact`: 2 devices
  - `ios-appium`: 1 device
  - `ios-maestro`: 2 devices
  - `web-selenium`: 2 browsers (Chrome, Firefox)

### 2. Job Naming Standardization (TODO)
Rename all BrowserStack test jobs to `browserstack-{platform}` format:

| Workflow | Current Name | New Name |
|----------|-------------|----------|
| android-cpp-ci.yml | `browserstack-appium-test` | `browserstack-android` |
| android-java-ci.yml | `browserstack-test` | `browserstack-android` |
| android-kotlin-ci.yml | `browserstack-test` | `browserstack-android` |
| flutter-ci.yml | `browserstack-android`, `browserstack-ios` | ✅ Already standard |
| kotlin-multiplatform-ci.yml | `browserstack-android` | ✅ Already standard |
| dotnet-maui-ci.yml | `browserstack-android`, `browserstack-ios` | ✅ Already standard |
| react-native-ci.yml | `test-android-maestro`, `test-ios-maestro` | `browserstack-android`, `browserstack-ios` |
| react-native-expo-ci.yml | `test-android-maestro`, `test-ios-maestro` | `browserstack-android`, `browserstack-ios` |
| javascript-web-ci.yml | `browserstack-test` | `browserstack-web` |

### 3. Fix Summary Job Dependencies (TODO)
Add missing jobs to `needs` array in summary jobs:

**Files needing fixes:**
- `android-cpp-ci.yml` - Add `[lint, build, browserstack-android]`
- `android-java-ci.yml` - Add `[lint, build, browserstack-android]`
- `android-kotlin-ci.yml` - Add `[lint, build, browserstack-android]`
- `flutter-ci.yml` - Add `[lint, ...]` to needs array

### 4. BrowserStack Capacity Management (TODO)
Add shared concurrency control to prevent queue overload:

```yaml
concurrency:
  group: browserstack-tests-${{ github.workflow }}
  cancel-in-progress: false  # Queue them, don't cancel
```

Add to all 9 workflows at the top level (after existing concurrency block).

### 5. Standardize instrumentationOptions (TODO)
Change all to use consistent key name: `github_test_doc_id`

**Files:**
- kotlin-multiplatform-ci.yml: Change `github_test_doc_title` → `github_test_doc_id`

### 6. Standardize Wait Times (TODO)
Unify polling logic across all workflows:
- Max wait time: 30 minutes (1800 seconds)
- Check interval: 30 seconds
- Apply to: android-java, android-kotlin, flutter, kotlin-multiplatform

### 7. Update Devices to Use Configuration File (TODO)
Replace hardcoded device arrays with references to `.github/browserstack-devices.json`:

**Implementation approach:**
```yaml
- name: Load device configuration
  id: devices
  run: |
    DEVICES=$(jq -c '.["android-espresso"]' .github/browserstack-devices.json)
    echo "devices=$DEVICES" >> $GITHUB_OUTPUT
```

Then use `${{ steps.devices.outputs.devices }}` in API calls.

**Files to update:**
- android-java-ci.yml
- android-kotlin-ci.yml
- android-cpp-ci.yml
- flutter-ci.yml
- kotlin-multiplatform-ci.yml
- dotnet-maui-ci.yml (if applicable)
- react-native-ci.yml
- react-native-expo-ci.yml
- javascript-web-ci.yml

## Implementation Order

1. ✅ Create device configuration file
2. ⏳ Start with simple fixes first:
   - Fix summary job dependencies (4 files)
   - Add BrowserStack concurrency control (9 files)
   - Standardize instrumentationOptions (1 file)
3. Job renaming (6 workflows need renames)
4. Device parameterization (9 workflows)
5. Standardize wait times (4 workflows)

## Testing Strategy

- Test one workflow at a time
- Verify device lists match existing behavior
- Ensure all jobs still reference correct dependencies
- Check that concurrency doesn't block legitimate parallel runs

## Rollback Plan

All changes are in a single PR branch. Can revert entire branch if issues arise.

## Notes

- Device configuration is JSON for easy parsing with `jq`
- Job renames will require updating all `needs` references
- Concurrency group uses workflow name to allow different workflows to run in parallel
