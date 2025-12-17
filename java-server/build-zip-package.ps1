# PowerShell script to create ZIP archive for distribution
param(
    [string]$Version = "0.0.1-SNAPSHOT"
)

# Clean the version string of any whitespace or control characters
$Version = $Version.Trim() -replace '[\r\n]',''

Write-Host "Creating ZIP archive..."

# Ensure the build directory exists
if (-not (Test-Path build)) {
    New-Item -ItemType Directory -Force -Path build | Out-Null
}

# Create the ZIP file
$zipPath = "build\DittoQuickstart-$Version.zip"
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

Compress-Archive -Path deploy\DittoQuickstart -DestinationPath $zipPath -Force

Write-Host ""
Write-Host "Distribution package created:"
Write-Host "  ZIP: $zipPath"
Write-Host ""
Write-Host "Deployment options:"
Write-Host "  1. Windows (without App-V): See WINDOWS-DEPLOYMENT.md"
Write-Host "  2. App-V Sequencer: See APP-V-DEPLOYMENT.md"
