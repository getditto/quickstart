# PowerShell script to build deployment package for App-V
param(
    [string]$Version = "0.0.1-SNAPSHOT"
)

# Clean the version string of any whitespace or control characters
$Version = $Version.Trim() -replace '[\r\n]',''

Write-Host "Creating deployment package..."

# Remove existing deploy directory
if (Test-Path deploy\DittoQuickstart) {
    Remove-Item -Recurse -Force deploy\DittoQuickstart
}

# Create deploy directory
New-Item -ItemType Directory -Force -Path deploy\DittoQuickstart | Out-Null

# Copy JRE bundle
Copy-Item -Recurse -Force build\jre-bundle deploy\DittoQuickstart\

# Copy application JAR
$jarPath = Join-Path -Path "build" -ChildPath "libs" | Join-Path -ChildPath "quickstart-java-$Version.jar"
if (Test-Path -LiteralPath $jarPath) {
    Copy-Item -LiteralPath $jarPath -Destination deploy\DittoQuickstart\ -Force
} else {
    Write-Host "ERROR: JAR file not found at: $jarPath"
    exit 1
}

# Copy scripts
Copy-Item -Force launch.bat deploy\DittoQuickstart\
Copy-Item -Force install.bat deploy\DittoQuickstart\
Copy-Item -Force uninstall.bat deploy\DittoQuickstart\

# Copy .env if exists
if (Test-Path .env) {
    Copy-Item -Force .env deploy\DittoQuickstart\
} else {
    Write-Host "Warning: .env file not found. Copy manually to deploy\DittoQuickstart\"
}

Write-Host ""
Write-Host "Deployment package created: deploy/DittoQuickstart/"
Write-Host ""
Write-Host "Next step: Run 'just zip-package' to create distribution archive"
