@echo off
REM Launcher script for Ditto Quickstart Java Server
REM Uses bundled JRE for App-V deployment

setlocal

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Set JAVA_HOME to bundled JRE
set JAVA_HOME=%SCRIPT_DIR%jre-bundle

REM Set application directory for relative paths
set APP_DIR=%SCRIPT_DIR%

REM Launch the application
echo Starting Ditto Quickstart Java Server...
echo Java Home: %JAVA_HOME%
echo Application Directory: %APP_DIR%

"%JAVA_HOME%\bin\java.exe" -jar "%SCRIPT_DIR%quickstart-java-0.0.1-SNAPSHOT.jar"

endlocal
