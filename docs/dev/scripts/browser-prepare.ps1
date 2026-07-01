param(
    [int] $Port = 18080,
    [string] $ContextPath = "GAPE",
    [string] $Email = "admin@gape.local",
    [string] $Password = "Password#2026",
    [switch] $SkipPackage
)

$ErrorActionPreference = "Stop"

function Assert-InWorkspace {
    param(
        [string] $Workspace,
        [string] $Path,
        [string] $Label
    )

    $resolvedWorkspace = (Resolve-Path -LiteralPath $Workspace).Path
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    if (-not $fullPath.StartsWith($resolvedWorkspace, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "$Label is outside the workspace: $fullPath"
    }
}

function Stop-ExistingBrowserTomcat {
    param(
        [int] $Port,
        [string] $ExpectedBase
    )

    $listeners = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($listener in $listeners) {
        $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
        $commandLine = [string] $processInfo.CommandLine
        if ($commandLine -like "*browser-tomcat10*" -or $commandLine -like "*apache-tomcat-10.1.24*") {
            Stop-Process -Id $listener.OwningProcess -Force
            Start-Sleep -Seconds 2
            continue
        }

        throw "Port $Port is already used by process $($listener.OwningProcess): $commandLine"
    }
}

function Initialize-TomcatBase {
    param(
        [string] $TomcatHome,
        [string] $TomcatBase,
        [int] $Port
    )

    if (-not (Test-Path -LiteralPath $TomcatHome)) {
        throw "Tomcat was not found at $TomcatHome. Run mvn package once or restore target/tools/apache-tomcat-10.1.24."
    }

    New-Item -ItemType Directory -Path $TomcatBase -Force | Out-Null
    foreach ($name in @("conf", "logs", "temp", "webapps", "work", "bin")) {
        $target = Join-Path $TomcatBase $name
        if ($name -eq "conf") {
            if (-not (Test-Path -LiteralPath $target)) {
                Copy-Item -LiteralPath (Join-Path $TomcatHome "conf") -Destination $target -Recurse -Force
            }
        } else {
            New-Item -ItemType Directory -Path $target -Force | Out-Null
        }
    }

    $serverXmlPath = Join-Path $TomcatBase "conf\server.xml"
    [xml] $serverXml = Get-Content -LiteralPath $serverXmlPath -Raw
    $serverXml.Server.port = [string] ($Port + 5)
    foreach ($connector in $serverXml.Server.Service.Connector) {
        if ($connector.protocol -eq "HTTP/1.1" -or $connector.protocol -like "*Http11*") {
            $connector.port = [string] $Port
            if ($connector.redirectPort) {
                $connector.redirectPort = [string] ($Port + 363)
            }
        } elseif ($connector.protocol -like "*AJP*") {
            $connector.port = [string] ($Port + 9)
        }
    }
    $serverXml.Save($serverXmlPath)
}

function Deploy-App {
    param(
        [string] $Workspace,
        [string] $TomcatBase,
        [string] $ContextPath
    )

    $sourceApp = Join-Path $Workspace "target\gape"
    if (-not (Test-Path -LiteralPath $sourceApp)) {
        throw "Exploded app target\gape was not found. Run mvn package without -DskipTests before preparing Browser QA."
    }

    $targetApp = Join-Path (Join-Path $TomcatBase "webapps") $ContextPath
    Assert-InWorkspace -Workspace $Workspace -Path $targetApp -Label "Tomcat app deployment"
    if (Test-Path -LiteralPath $targetApp) {
        Remove-Item -LiteralPath $targetApp -Recurse -Force
    }
    $workDir = Join-Path $TomcatBase "work"
    Assert-InWorkspace -Workspace $Workspace -Path $workDir -Label "Tomcat JSP work cache"
    if (Test-Path -LiteralPath $workDir) {
        Remove-Item -LiteralPath $workDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $workDir -Force | Out-Null
    Copy-Item -LiteralPath $sourceApp -Destination $targetApp -Recurse -Force
}

function Wait-ForApp {
    param(
        [string] $BaseUrl,
        [int] $TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri "$BaseUrl/login.jsp" -UseBasicParsing -TimeoutSec 4
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 750
        }
    } while ((Get-Date) -lt $deadline)

    throw "The app did not respond at $BaseUrl within $TimeoutSeconds seconds."
}

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath $env:JAVA_HOME)) {
        return (Resolve-Path -LiteralPath $env:JAVA_HOME).Path
    }

    $javaCommand = Get-Command java -ErrorAction Stop
    $javaExe = $javaCommand.Source
    $binDirectory = Split-Path -Parent $javaExe
    $candidate = Split-Path -Parent $binDirectory
    if (Test-Path -LiteralPath (Join-Path $candidate "bin\java.exe")) {
        return (Resolve-Path -LiteralPath $candidate).Path
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $javaSettings = & java -XshowSettings:properties -version 2>&1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    foreach ($line in $javaSettings) {
        if ($line -match '^\s*java\.home\s*=\s*(.+)\s*$') {
            $runtimeHome = $Matches[1].Trim()
            if (Test-Path -LiteralPath (Join-Path $runtimeHome "bin\java.exe")) {
                return (Resolve-Path -LiteralPath $runtimeHome).Path
            }
        }
    }

    throw "Could not resolve JAVA_HOME from java command: $javaExe"
}

function New-AuthenticatedBrowserUrls {
    param(
        [string] $BaseUrl,
        [string] $TomcatBase,
        [string] $Email,
        [string] $Password
    )

    $cookieJar = Join-Path $TomcatBase "temp\browser-cookies.txt"
    $loginBody = Join-Path $TomcatBase "temp\browser-login.html"
    if (Test-Path -LiteralPath $cookieJar) {
        Remove-Item -LiteralPath $cookieJar -Force
    }
    if (Test-Path -LiteralPath $loginBody) {
        Remove-Item -LiteralPath $loginBody -Force
    }

    & curl.exe -s -L -c $cookieJar -b $cookieJar -o $loginBody `
        -X POST "$BaseUrl/auth/login" `
        --data-urlencode "email=$Email" `
        --data-urlencode "password=$Password" | Out-Null

    $sessionLine = Select-String -LiteralPath $cookieJar -Pattern "JSESSIONID" -ErrorAction SilentlyContinue |
        Select-Object -Last 1
    if ($null -eq $sessionLine) {
        return @{
            SessionBase = $BaseUrl
            SessionId = $null
        }
    }

    $sessionId = (($sessionLine.Line -split "`t") | Select-Object -Last 1).Trim()
    return @{
        SessionBase = $BaseUrl -replace "/$",""
        SessionId = $sessionId
    }
}

$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..\..")).Path
$tomcatHome = Join-Path $workspace "target\tools\apache-tomcat-10.1.24"
$tomcatBase = Join-Path $workspace "target\browser-tomcat10"
$baseUrl = "http://localhost:$Port/$ContextPath"

Assert-InWorkspace -Workspace $workspace -Path $tomcatBase -Label "Tomcat base"

if (-not $SkipPackage) {
    & mvn -q -DskipTests package
}

Stop-ExistingBrowserTomcat -Port $Port -ExpectedBase $tomcatBase
Initialize-TomcatBase -TomcatHome $tomcatHome -TomcatBase $tomcatBase -Port $Port
Deploy-App -Workspace $workspace -TomcatBase $tomcatBase -ContextPath $ContextPath

$launcherPath = Join-Path $tomcatBase "bin\run-browser-gape.ps1"
$stdoutPath = Join-Path $tomcatBase "logs\browser-stdout.log"
$stderrPath = Join-Path $tomcatBase "logs\browser-stderr.log"
$uploadDir = Join-Path $workspace "uploads"
$webpDir = Join-Path $workspace "docs\dev\.gape-webp-native"
$javaHome = Resolve-JavaHome
New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null
New-Item -ItemType Directory -Path $webpDir -Force | Out-Null

$launcher = @"
`$env:JAVA_HOME = '$javaHome'
`$env:JRE_HOME = '$javaHome'
`$env:CATALINA_HOME = '$tomcatHome'
`$env:CATALINA_BASE = '$tomcatBase'
`$env:JAVA_OPTS = '-Dfile.encoding=UTF-8 -Dgape.upload.dir="$uploadDir" -Dgape.webp.native.dir="$webpDir"'
& '$tomcatHome\bin\catalina.bat' run
"@
Set-Content -LiteralPath $launcherPath -Value $launcher -Encoding UTF8

Start-Process -FilePath "powershell.exe" `
    -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$launcherPath`"") `
    -WorkingDirectory $workspace `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath | Out-Null

Wait-ForApp -BaseUrl $baseUrl
$auth = New-AuthenticatedBrowserUrls -BaseUrl $baseUrl -TomcatBase $tomcatBase -Email $Email -Password $Password

$sessionPrefix = $auth.SessionBase
if ($auth.SessionId) {
    $sessionPrefix = "$($auth.SessionBase);jsessionid=$($auth.SessionId)"
}

Write-Output "Browser QA is ready."
Write-Output "Base URL: $baseUrl"
Write-Output "Session base: $sessionPrefix"
Write-Output "Recommended URLs:"
Write-Output "  $sessionPrefix/admin/admin-dashbord.jsp"
Write-Output "  $sessionPrefix/admin/organizations"
Write-Output "  $sessionPrefix/admin/courses"
Write-Output "  $sessionPrefix/admin/subjects"
Write-Output "  $sessionPrefix/admin/users"
Write-Output "  $sessionPrefix/account/profile"
Write-Output "Logs:"
Write-Output "  $stdoutPath"
Write-Output "  $stderrPath"
