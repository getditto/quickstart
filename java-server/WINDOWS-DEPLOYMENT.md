# Windows Deployment Guide (Without App-V Sequencer)

This guide explains how to deploy the Ditto Quickstart Java Server as a standalone Windows application without using App-V Sequencer.

## Deployment Options

### Option 1: User Installation (Recommended for Testing)

- Install location: `%LOCALAPPDATA%\DittoQuickstart`
- No admin rights required
- Per-user installation
- Easy to install/uninstall

### Option 2: System-Wide Installation

- Install location: `%ProgramFiles%\DittoQuickstart`
- Requires admin rights
- Shared for all users
- More suitable for enterprise deployment

## Building the Package

### On Your Development Machine

**Important**: Edit `.env` with your Ditto credentials before building:

```env
DITTO_APP_ID=your-app-id-here
DITTO_PLAYGROUND_TOKEN=your-token-here
DITTO_AUTH_URL=https://cloud.ditto.live/
DITTO_WEBSOCKET_URL=wss://cloud.ditto.live/ws/v1
```

```bash
# Build deployment package and create ZIP archive
just zip-package
```

This creates:

- Directory: `deploy/DittoQuickstart/`
- ZIP archive: `build/DittoQuickstart-0.0.1-SNAPSHOT.zip`

The package contains:

```
DittoQuickstart/
├── jre-bundle/                          (Java Runtime ~50-70MB)
├── quickstart-java-0.0.1-SNAPSHOT.jar   (Application ~150MB)
├── launch.bat                           (Launcher script)
├── install.bat                          (Installation script)
├── uninstall.bat                        (Uninstallation script)
└── .env                                 (Ditto credentials - REQUIRED)
```

**Note**: The `zip-package` recipe automatically uses the correct archiving tool for your platform:

- **Unix/macOS**: Uses `zip` command
- **Windows**: Uses PowerShell `Compress-Archive`

## Installing on Windows

### Method 1: Interactive Installation (Recommended)

1. **Extract** `DittoQuickstart-0.0.1-SNAPSHOT.zip` to a temporary location
2. **Navigate** to the extracted `DittoQuickstart` folder
3. **Right-click** `install.bat` and select:
   - **"Run as administrator"** for system-wide install
   - **Double-click** for user install
4. **Follow prompts**:
   - Choose to create desktop shortcut (Y/N)
   - Choose to create Start Menu shortcut (Y/N)

The installer will copy files to:

- **With admin**: `C:\Program Files\DittoQuickstart`
- **Without admin**: `C:\Users\<username>\AppData\Local\DittoQuickstart`

### Method 2: Manual Installation

1. **Extract** `DittoQuickstart-0.0.1-SNAPSHOT.zip`
2. **Copy** the `DittoQuickstart` folder to your desired location:
   - `C:\DittoQuickstart` (simple path)
   - `C:\Program Files\DittoQuickstart` (system-wide, needs admin)
   - `%LOCALAPPDATA%\DittoQuickstart` (user-specific)
3. **Create shortcut** (optional):
   - Right-click `launch.bat` → Send to → Desktop (create shortcut)

### Method 3: Silent Installation (Scripted Deployment)

For automated deployment via GPO, SCCM, or scripts:

```batch
@echo off
REM Silent installation script

set INSTALL_DIR=%ProgramFiles%\DittoQuickstart

REM Extract package
powershell -Command "Expand-Archive -Path DittoQuickstart-0.0.1-SNAPSHOT.zip -DestinationPath C:\Temp"

REM Copy to installation directory
xcopy /E /I /Y C:\Temp\DittoQuickstart "%INSTALL_DIR%"

REM Create data directory
mkdir "%LOCALAPPDATA%\DittoQuickstart\data"

REM Create Start Menu shortcut for all users
set START_MENU=%ProgramData%\Microsoft\Windows\Start Menu\Programs

echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut.vbs"
echo sLinkFile = "%START_MENU%\Ditto Quickstart Server.lnk" >> "%TEMP%\CreateShortcut.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\CreateShortcut.vbs"
echo oLink.TargetPath = "%INSTALL_DIR%\launch.bat" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.WorkingDirectory = "%INSTALL_DIR%" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Save >> "%TEMP%\CreateShortcut.vbs"

cscript /nologo "%TEMP%\CreateShortcut.vbs"
del "%TEMP%\CreateShortcut.vbs"

REM Cleanup
rmdir /S /Q C:\Temp\DittoQuickstart

echo Installation complete
```

## Running the Application

### Start the Server

**Option 1**: Use shortcut

- Double-click desktop or Start Menu shortcut

**Option 2**: Run directly

```batch
cd C:\Program Files\DittoQuickstart
launch.bat
```

**Option 3**: Run from anywhere

```batch
"%ProgramFiles%\DittoQuickstart\launch.bat"
```

### Access the Web UI

Once started, open a browser to:

```
http://localhost:8080
```

You should see the Ditto Quickstart Tasks application.

## Verifying Installation

### Check Files

Verify these files exist:

```batch
dir "%ProgramFiles%\DittoQuickstart"
```

Should show:

- `jre-bundle\` directory
- `quickstart-java-0.0.1-SNAPSHOT.jar`
- `launch.bat`
- `.env`
- `uninstall.bat`

### Check Data Directory

```batch
dir "%LOCALAPPDATA%\DittoQuickstart\data"
```

This directory is created on first run and stores Ditto sync data.

### Test Java Runtime

```batch
"%ProgramFiles%\DittoQuickstart\jre-bundle\bin\java.exe" -version
```

Should output: `openjdk version "17.x.x"`

## Uninstalling

### Method 1: Using Uninstaller

Navigate to installation directory and run:

```batch
C:\Program Files\DittoQuickstart\uninstall.bat
```

The uninstaller will:

1. Remove desktop shortcut
2. Remove Start Menu shortcut
3. Optionally remove user data
4. Schedule removal of installation directory

### Method 2: Manual Removal

1. **Delete installation directory**:

```batch
rmdir /S /Q "%ProgramFiles%\DittoQuickstart"
```

2. **Delete data directory** (optional):

```batch
rmdir /S /Q "%LOCALAPPDATA%\DittoQuickstart"
```

3. **Remove shortcuts**:

- Delete from Desktop
- Delete from Start Menu

## Enterprise Deployment Methods

### 1. Group Policy (GPO)

Create a startup/logon script:

1. Share the package on a network location: `\\server\software\DittoQuickstart`
2. Create a GPO startup script that runs the silent installation script
3. Apply to target computers or OUs

### 2. SCCM/ConfigMgr

1. Create an SCCM Application
2. Set detection method: Check for `%ProgramFiles%\DittoQuickstart\quickstart-java-0.0.1-SNAPSHOT.jar`
3. Set installation command: `install.bat /silent`
4. Set uninstall command: `uninstall.bat /silent`
5. Deploy to device collections

### 3. Intune (MDM)

1. Package as `.intunewin` file using Microsoft Win32 Content Prep Tool
2. Upload to Intune
3. Configure install/uninstall commands
4. Assign to device groups

### 4. PowerShell Remoting

Deploy to multiple machines:

```powershell
$computers = @("PC1", "PC2", "PC3")
$source = "\\server\software\DittoQuickstart-0.0.1-SNAPSHOT.zip"

foreach ($computer in $computers) {
    Invoke-Command -ComputerName $computer -ScriptBlock {
        param($packagePath)

        # Copy package
        Copy-Item $packagePath -Destination C:\Temp\

        # Extract
        Expand-Archive -Path C:\Temp\DittoQuickstart-0.0.1-SNAPSHOT.zip -Destination C:\Temp

        # Install
        xcopy /E /I /Y C:\Temp\DittoQuickstart "$env:ProgramFiles\DittoQuickstart"

        # Cleanup
        Remove-Item C:\Temp\DittoQuickstart* -Recurse -Force
    } -ArgumentList $source
}
```

## Troubleshooting

### Application Won't Start

**Check Java Runtime**:

```batch
"%ProgramFiles%\DittoQuickstart\jre-bundle\bin\java.exe" -version
```

If this fails, the JRE bundle is missing or corrupted. Rebuild with:

```bash
just jre-bundle
```

### .env File Missing

Error: `DITTO_APP_ID must be provided`

**Solution**: Create `.env` file in installation directory:

```batch
notepad "%ProgramFiles%\DittoQuickstart\.env"
```

Add:

```env
DITTO_APP_ID=your-app-id
DITTO_PLAYGROUND_TOKEN=your-token
DITTO_AUTH_URL=https://cloud.ditto.live/
DITTO_WEBSOCKET_URL=wss://cloud.ditto.live/ws/v1
```

### Port 8080 Already in Use

Check what's using port 8080:

```batch
netstat -ano | findstr :8080
```

Either:

1. Stop the conflicting application
2. Change the server port by creating `application.properties` in install directory:
   ```properties
   server.port=8081
   ```

### Permission Errors

If you get "Access Denied" errors:

1. **Check installation location**: User installations should go to `%LOCALAPPDATA%`
2. **Run as admin**: For system-wide installations
3. **Check file permissions**: Ensure user has read/execute permissions

### Native Library Loading Errors

If you see errors about `ditto` DLL loading:

1. **Check architecture**: Ensure you're on x64 Windows (ARM not supported)
2. **Check antivirus**: Some AV software blocks native DLLs
3. **Check Windows Event Viewer**: Look for detailed DLL load errors

### Data Directory Issues

Verify data directory exists and is writable:

```batch
echo %LOCALAPPDATA%\DittoQuickstart\data
mkdir "%LOCALAPPDATA%\DittoQuickstart\data"
echo test > "%LOCALAPPDATA%\DittoQuickstart\data\test.txt"
```

## Updating the Application

### Update Process

1. **Stop** the running application
2. **Backup** data directory (optional):
   ```batch
   xcopy /E /I "%LOCALAPPDATA%\DittoQuickstart" C:\Backup\DittoData
   ```
3. **Run uninstaller** (keeps data by default)
4. **Install new version** using installation steps above
5. **Start** the application

Data is preserved in `%LOCALAPPDATA%\DittoQuickstart\data` across updates.

## Security Considerations

### File Permissions

The application requires:

- **Read/Execute**: Installation directory
- **Read/Write**: `%LOCALAPPDATA%\DittoQuickstart\data`

### Network Access

The application requires:

- **Outbound HTTPS (443)**: Ditto cloud sync
- **Outbound WebSocket (443)**: Real-time sync
- **Inbound TCP (8080)**: Web UI (localhost only)

### Firewall Rules

Windows Firewall may prompt on first run. Allow:

- **Java.exe**: From installation directory
- **Port 8080**: Localhost access only (not public)

### Credentials Security

The `.env` file contains sensitive credentials:

- **Restrict permissions**: Right-click → Properties → Security → Edit
- **Remove inheritance**: Allow only System and Administrators
- **Or use environment variables**: Set system-wide env vars instead

## Performance Considerations

### Disk Space

- Installation: ~220 MB
- Runtime data: Varies (depends on Ditto sync data)
- Recommend: 500 MB free space

### Memory

- JVM heap: Default 256MB-2GB (auto-scaled)
- Typical usage: 200-500 MB
- Recommend: 1 GB available RAM

### CPU

- Idle: <1%
- Active sync: 5-15%
- Minimum: Dual-core processor

## Support

For issues specific to:

- **Ditto SDK**: https://docs.ditto.live/
- **Spring Boot**: https://docs.spring.io/spring-boot/
- **Windows deployment**: Contact your IT department

## Next Steps

After successful deployment without App-V:

1. **Test functionality**: Verify sync works across multiple machines
2. **Monitor performance**: Check resource usage over time
3. **Consider App-V**: For better isolation and centralized management

When ready for App-V:

- Use the `APP-V-DEPLOYMENT.md` guide
- Sequence this working installation
- Deploy via App-V server infrastructure
