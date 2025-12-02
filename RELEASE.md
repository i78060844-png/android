# Release and Signing Guide

## Release Process

This project uses GitHub Actions to automate the build and release process.

### Debug Builds

Debug builds are automatically created on every push to the `main` branch (unless the commit message starts with "release").

- Workflow: `.github/workflows/debug-release.yml`
- Output: Debug APK signed with debug keystore
- Pre-release tag format: `v{version}-build.{run_number}`

### Production Releases

Production releases can be triggered in two ways:

1. **Manual trigger**: Use the GitHub Actions "Run workflow" button
2. **Commit message**: Push a commit with message starting with "release v{version}"

- Workflow: `.github/workflows/publish.yml`
- Output: Release APK signed with production keystore
- Release tag format: `v{version}`

## APK Signing Configuration

### Critical: Maintaining Signature Consistency

**IMPORTANT**: The app signature MUST remain consistent across all releases. Changing the keystore will:
- Invalidate all previous releases
- Prevent users from updating the app
- Require users to uninstall and reinstall the app (losing all data)

### Keystore Setup

The production keystore is stored as a GitHub Secret:

1. **KEYSTORE_FILE**: Base64-encoded keystore file
   ```bash
   # To encode a keystore:
   base64 -w 0 your-keystore.jks > keystore-base64.txt
   ```

2. **KEYSTORE_PASSWORD**: Password for the keystore
3. **KEYSTORE_ALIAS**: Alias of the key in the keystore
4. **KEY_PASSWORD**: Password for the specific key

### Important Notes

- **Never** regenerate or replace the keystore unless you understand the consequences
- **Always** keep a secure backup of the keystore file and passwords
- **Never** commit the keystore file to git (it's in `.gitignore`)
- The keystore secret should only be set once and never changed

### If Keystore is Lost

If you lose the keystore:
1. You cannot update the existing app on user devices
2. You must publish under a new package name
3. Users must uninstall the old app and install the new one

## Build Performance Optimizations

The following optimizations have been enabled to speed up builds:

### Gradle Properties (`gradle.properties`)
- Parallel execution: `org.gradle.parallel=true`
- Build cache: `org.gradle.caching=true`
- Configuration on demand: `org.gradle.configureondemand=true`
- Kotlin incremental compilation: `kotlin.incremental=true`

### GitHub Actions Optimizations
- Gradle wrapper caching
- Gradle build cache
- Configuration cache
- Parallel task execution

These optimizations significantly reduce build times, especially for incremental builds.
