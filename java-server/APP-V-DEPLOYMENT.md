# App-V Deployment Guide for Ditto Quickstart Java Server

This guide explains how to package and deploy the Ditto Quickstart Java Server as a Microsoft App-V virtualized application.

## Prerequisites

- Windows 10/11 or Windows Server with App-V support
- App-V Sequencer installed
- Java 17+ installed (for building, not required on target machines)
- Gradle (or use the included Gradle wrapper)

## Building the Self-Contained Package

### Step 1: Configure Ditto Credentials

Edit `.env` in the project root with your Ditto credentials:

```env
DITTO_APP_ID=your-app-id
DITTO_PLAYGROUND_TOKEN=your-token
DITTO_AUTH_URL=https://cloud.ditto.live/
DITTO_WEBSOCKET_URL=wss://cloud.ditto.live/ws/v1
```

### Step 2: Build the Package

```bash
# Build deployment package (without ZIP archive)
just build-appv-package
```

This creates `deploy/DittoQuickstart/` containing:

- `jre-bundle/` - Minimal Java Runtime (~50-70 MB)
- `quickstart-java-0.0.1-SNAPSHOT.jar` - Application (~150 MB)
- `launch.bat` - Launcher script
- `install.bat` - Installation script
- `uninstall.bat` - Uninstallation script
- `.env` - Ditto credentials

The JRE bundle includes only necessary modules:

- `java.base` - Core Java functionality
- `java.sql` - Database access (H2)
- `java.naming` - JNDI support
- `java.desktop` - AWT/Swing (if needed)
- `java.management` - JMX support
- `java.instrument` - Instrumentation API
- `java.net.http` - HTTP client
- `java.security.jgss` - Security features
- `java.xml` - XML processing
- `jdk.crypto.ec` - Elliptic curve cryptography
- `jdk.unsupported` - Required for some native libraries

**Note**: For App-V sequencing, you only need the directory. If you need a ZIP archive for distribution, run `just zip-package` instead.

## App-V Sequencing

### Step 1: Launch App-V Sequencer

1. Open **Microsoft Application Virtualization Sequencer** as Administrator
2. Click **Create a New Virtual Application Package**
3. Choose **Create Package (default)**

### Step 2: Configure Package Information

1. **Package Name**: DittoQuickstart
2. **Installation Location**: `C:\DittoQuickstart` (temporary, will be virtualized)
3. Click **Next**

### Step 3: Install the Application

1. **Choose "I do not use an installer"** (we're using a pre-built package)
2. **Browse** to your `deploy/DittoQuickstart` folder
3. **Copy** the entire folder to `C:\DittoQuickstart`
4. Click **Next**

### Step 4: Configure Installation

1. Run `C:\DittoQuickstart\launch.bat` to test the application
2. Verify the application starts and listens on port 8080
3. Open browser to `http://localhost:8080` to verify UI loads
4. Stop the application
5. Click **Next** in the Sequencer

### Step 5: Configure Shortcuts

1. **Primary Application**: Select `launch.bat`
2. **Shortcut Name**: Ditto Quickstart Server
3. **Target**: `[{AppVPackageRoot}]\launch.bat`
4. Click **Next**

### Step 6: Configure Streaming

1. Choose **Local Installation and Streaming** (recommended)
2. Set **Target OS**: Windows 10/11
3. Click **Create**

### Step 7: Save the Package

1. Save as `DittoQuickstart.appv`
2. Location: Your distribution folder

## App-V Package Configuration

### Required Virtual File System Paths

The package needs these paths to be writable:

```
%LOCALAPPDATA%\DittoQuickstart\data\   (Ditto data storage)
```

### Required Virtual Registry Keys

None required for this application.

### Network Configuration

The application requires:

- **Outbound HTTP/HTTPS**: For Ditto cloud sync
- **Inbound TCP Port 8080**: For web UI (localhost only)
- **WebSocket connections**: For real-time sync

Ensure these are enabled in the App-V package policy.

### Environment Variables

The application reads from `.env` file in the package root. Alternatively, you can set these as system/user environment variables:

```
DITTO_APP_ID
DITTO_PLAYGROUND_TOKEN
DITTO_AUTH_URL
DITTO_WEBSOCKET_URL
```

## Deployment

### Publishing to App-V Server

1. **Upload** `DittoQuickstart.appv` to your App-V Management Server
2. **Add** the package to the Management Console
3. **Assign** to appropriate user groups
4. **Publish** the package

### Testing on Client Machines

1. Ensure client has App-V Client installed and configured
2. User logs in and receives the published package
3. Launch "Ditto Quickstart Server" from Start Menu
4. Open browser to `http://localhost:8080`
5. Verify application functionality

## Troubleshooting

### Application Won't Start

**Check JRE Bundle**:

```cmd
dir /s jre-bundle\bin\java.exe
```

If missing, rebuild with `./gradlew createJreBundle`

### Data Directory Issues

**Verify writable path**:

```cmd
echo %LOCALAPPDATA%\DittoQuickstart\data
```

This should resolve to: `C:\Users\<username>\AppData\Local\DittoQuickstart\data`

### Network Connectivity

**Test port 8080**:

```cmd
netstat -an | findstr :8080
```

Should show: `TCP    127.0.0.1:8080    0.0.0.0:0    LISTENING`

### Native Library Loading

If Ditto SDK fails to load native DLLs:

1. Check that `ditto-binaries` includes Windows x64 binaries
2. Verify `jdk.unsupported` module is in JRE bundle
3. Check Windows Event Viewer for DLL load errors

### Environment Variables Not Loading

If `.env` file isn't being read:

1. Verify `.env` is in the same directory as the JAR
2. Check file permissions in virtual file system
3. As fallback, set environment variables in App-V package configuration

## User Context and Permissions

- **User**: Application runs as the currently logged-in user
- **Permissions**: Standard user permissions (no admin required)
- **Data**: Stored in user's `%LOCALAPPDATA%` directory
- **Network**: Uses user's network credentials

## File Size Considerations

Approximate package sizes:

- JRE Bundle: ~50-70 MB
- Application JAR: ~150 MB (includes Ditto SDK native libraries)
- **Total Package**: ~200-220 MB

## Security Considerations

1. **Native Libraries**: Ditto SDK includes platform-specific native code
2. **Network Access**: Application requires internet access for sync
3. **Data Storage**: User data stored in `%LOCALAPPDATA%` (encrypted by Ditto)
4. **Credentials**: Stored in `.env` or environment variables

## Updates

To update the application:

1. Build new JAR with updated version
2. Re-sequence the package (or create upgrade package)
3. Publish updated package to App-V server
4. Users automatically receive updates based on policy

## Additional Resources

- [Ditto SDK Documentation](https://docs.ditto.live/)
- [App-V Documentation](https://docs.microsoft.com/en-us/windows/application-management/app-v/)
- [Spring Boot Deployment](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
