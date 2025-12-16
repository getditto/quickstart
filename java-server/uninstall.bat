@echo off
REM Uninstallation script for Ditto Quickstart Java Server

setlocal enabledelayedexpansion

echo ========================================
echo Ditto Quickstart Server - Uninstall
echo ========================================
echo.

REM Get the directory where this script is located
set INSTALL_DIR=%~dp0

echo Installation directory: !INSTALL_DIR!
echo.
echo WARNING: This will remove the application but keep your data
echo Data location: %LOCALAPPDATA%\DittoQuickstart\data
echo.

set /p CONFIRM="Are you sure you want to uninstall? (Y/N): "
if /i not "!CONFIRM!"=="Y" (
    echo Uninstall cancelled.
    pause
    exit /b 0
)

echo.
echo Uninstalling...

REM Remove desktop shortcut if it exists
if exist "%USERPROFILE%\Desktop\Ditto Quickstart Server.lnk" (
    echo Removing desktop shortcut...
    del "%USERPROFILE%\Desktop\Ditto Quickstart Server.lnk"
)

REM Remove Start Menu shortcut if it exists
set "START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs"
if exist "!START_MENU!\Ditto Quickstart Server.lnk" (
    echo Removing Start Menu shortcut...
    del "!START_MENU!\Ditto Quickstart Server.lnk"
)

REM Ask about data removal
echo.
set /p REMOVE_DATA="Also remove application data? (Y/N): "
if /i "!REMOVE_DATA!"=="Y" (
    if exist "%LOCALAPPDATA%\DittoQuickstart\data" (
        echo Removing application data...
        rmdir /S /Q "%LOCALAPPDATA%\DittoQuickstart\data"
        rmdir "%LOCALAPPDATA%\DittoQuickstart" 2>nul
    )
)

REM Remove installation directory (cannot remove while running, so schedule deletion)
echo.
echo Scheduling removal of installation directory...
echo The installation directory will be removed after this window closes.
echo.

REM Create a temporary script to delete the installation directory
echo @echo off > "%TEMP%\DittoCleanup.bat"
echo timeout /t 2 /nobreak ^>nul >> "%TEMP%\DittoCleanup.bat"
echo rmdir /S /Q "!INSTALL_DIR!" >> "%TEMP%\DittoCleanup.bat"
echo del "%%~f0" >> "%TEMP%\DittoCleanup.bat"

REM Start the cleanup script and exit
start /min cmd /c "%TEMP%\DittoCleanup.bat"

echo ========================================
echo Uninstall Complete!
echo ========================================
echo.
echo The application has been removed.
echo.

timeout /t 3
endlocal
exit
