# Script to set up Java 21 environment variables
$javaHome = "C:\Program Files\Java\jdk-21"

# Check if Java 21 is installed in the default location
if (-not (Test-Path $javaHome)) {
    Write-Host "Java 21 not found in $javaHome"
    Write-Host "Please enter the path where you installed Java 21:"
    $javaHome = Read-Host
    if (-not (Test-Path $javaHome)) {
        Write-Host "Invalid path. Please install Java 21 first."
        exit 1
    }
}

# Set JAVA_HOME for the current session
$env:JAVA_HOME = $javaHome
Write-Host "Set JAVA_HOME to: $javaHome"

# Add Java to PATH for the current session
$env:Path = "$javaHome\bin;$env:Path"
Write-Host "Added Java to PATH"

# Verify Java version
Write-Host "`nVerifying Java installation:"
java -version

# Set environment variables permanently
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, [System.EnvironmentVariableTarget]::User)
[System.Environment]::SetEnvironmentVariable("Path", "$javaHome\bin;" + [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::User), [System.EnvironmentVariableTarget]::User)

Write-Host "`nJava 21 environment variables have been set up successfully!"
Write-Host "Please restart your terminal/IDE for the changes to take effect." 