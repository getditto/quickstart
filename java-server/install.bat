@echo off
REM Installation script for Ditto Quickstart Java Server
REM Deploys the application to a standard Windows location

setlocal enabledelayedexpansion

echo ========================================
echo Ditto Quickstart Server - Installation
echo ========================================
echo.

REM Get the directory where this script is located
set "SOURCE_DIR=%~dp0"
cd /d "%SOURCE_DIR%"

echo Source directory: %SOURCE_DIR%
echo.

REM Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo WARNING: Not running as Administrator
    echo Installation will proceed to user directory
    set "INSTALL_DIR=%LOCALAPPDATA%\DittoQuickstart"
) else (
    echo Running as Administrator
    set "INSTALL_DIR=%ProgramFiles%\DittoQuickstart"
)

echo Installation directory: !INSTALL_DIR!
echo.

REM Create installation directory
if not exist "!INSTALL_DIR!" (
    echo Creating installation directory...
    mkdir "!INSTALL_DIR!"
)

REM Copy files
echo Copying application files...

if not exist "jre-bundle" (
    echo ERROR: jre-bundle directory not found!
    echo Please run: just build-appv-package
    echo Or: gradlew createJreBundle
    pause
    exit /b 1
)

if not exist "quickstart-java-0.0.1-SNAPSHOT.jar" (
    echo ERROR: JAR file not found!
    echo Please run: just build
    echo Or: gradlew bootJar
    pause
    exit /b 1
)

xcopy /E /I /Y jre-bundle "!INSTALL_DIR!\jre-bundle" >nul
copy /Y quickstart-java-0.0.1-SNAPSHOT.jar "!INSTALL_DIR!\" >nul
copy /Y launch.bat "!INSTALL_DIR!\" >nul

if exist .env (
    copy /Y .env "!INSTALL_DIR!\" >nul
    echo Copied .env file
) else (
    echo WARNING: .env file not found
    echo You must create !INSTALL_DIR!\.env with Ditto credentials
)

REM Create data directory
if not exist "%LOCALAPPDATA%\DittoQuickstart\data" (
    echo Creating data directory...
    mkdir "%LOCALAPPDATA%\DittoQuickstart\data"
)

REM Create desktop shortcut (optional)
set /p CREATE_SHORTCUT="Create desktop shortcut? (Y/N): "
if /i "!CREATE_SHORTCUT!"=="Y" (
    echo Creating desktop shortcut...

    REM Create VBS script to generate shortcut
    echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut.vbs"
    echo sLinkFile = "%USERPROFILE%\Desktop\Ditto Quickstart Server.lnk" >> "%TEMP%\CreateShortcut.vbs"
    echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\CreateShortcut.vbs"
    echo oLink.TargetPath = "!INSTALL_DIR!\launch.bat" >> "%TEMP%\CreateShortcut.vbs"
    echo oLink.WorkingDirectory = "!INSTALL_DIR!" >> "%TEMP%\CreateShortcut.vbs"
    echo oLink.Description = "Ditto Quickstart Java Server" >> "%TEMP%\CreateShortcut.vbs"
    echo oLink.Save >> "%TEMP%\CreateShortcut.vbs"

    cscript /nologo "%TEMP%\CreateShortcut.vbs"
    del "%TEMP%\CreateShortcut.vbs"
)

REM Create Start Menu shortcut (optional)
set /p CREATE_START_MENU="Create Start Menu shortcut? (Y/N): "
if /i "!CREATE_START_MENU!"=="Y" (
    echo Creating Start Menu shortcut...

    set "START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs"

    REM Create VBS script to generate shortcut
    echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut.vbs"
    echo sLinkFile = "!START_MENU!\Ditto Quickstart Server.lnk" >> "%TEMP%\CreateShortcut.vbs"
    echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\CreateShortcut.vbs"
    echo oLink.TargetPath = "!INSTALL_DIR!\launch.bat" >> "%TEMP%\CreateShortcut.vbs"
    echo oLink.WorkingDirectory = "!INSTALL_DIR!" >> "%TEMP%\CreateShortcut.vbs"
    echo oLink.Description = "Ditto Quickstart Java Server" >> "%TEMP%\CreateShortcut.vbs"
    echo oLink.Save >> "%TEMP%\CreateShortcut.vbs"

    cscript /nologo "%TEMP%\CreateShortcut.vbs"
    del "%TEMP%\CreateShortcut.vbs"
)

echo.
echo ========================================
echo Installation Complete!
echo ========================================
echo.
echo Installation location: !INSTALL_DIR!
echo Data directory: %LOCALAPPDATA%\DittoQuickstart\data
echo.
echo To start the server:
echo   1. Run: !INSTALL_DIR!\launch.bat
echo   2. Open browser to: http://localhost:8080
echo.
echo To uninstall, run: !INSTALL_DIR!\uninstall.bat
echo.

pause
endlocal
