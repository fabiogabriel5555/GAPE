param(
    [int] $Port = 18080
)

$ErrorActionPreference = "Stop"
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..\..")).Path
$expectedBase = Join-Path $workspace "target\browser-tomcat10"

$listeners = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
if ($listeners.Count -eq 0) {
    Write-Output "No Browser Tomcat is listening on port $Port."
    exit 0
}

foreach ($listener in $listeners) {
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
    $commandLine = [string] $processInfo.CommandLine
    if ($commandLine -like "*browser-tomcat10*" -or $commandLine -like "*apache-tomcat-10.1.24*") {
        Stop-Process -Id $listener.OwningProcess -Force
        Write-Output "Stopped Browser Tomcat process $($listener.OwningProcess)."
        continue
    }

    throw "Port $Port is used by another process and was not stopped: $commandLine"
}
