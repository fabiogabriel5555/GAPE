param(
    [int] $Port = 18080,
    [string] $ContextPath = "GAPE",
    [Alias("Path")]
    [string] $Route = "/admin/admin-dashbord.jsp",
    [string] $Email = "admin@gape.local",
    [string] $Password = "Password#2026",
    [string] $Out = "target\browser-screenshots\browser-screenshot.png",
    [int] $Width = 1920,
    [int] $Height = 1200,
    [int] $DeviceScaleFactor = 1,
    [int] $WaitBeforeCaptureMs = 5000,
    [int] $TimeoutSeconds = 300,
    [string] $BrowserPath
)

$ErrorActionPreference = "Stop"

function Assert-InWorkspace {
    param(
        [string] $Workspace,
        [string] $Path,
        [string] $Label
    )

    $resolvedWorkspace = (Resolve-Path -LiteralPath $Workspace).Path.TrimEnd('\', '/')
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $insideWorkspace = $fullPath.Equals($resolvedWorkspace, [System.StringComparison]::OrdinalIgnoreCase) -or
        $fullPath.StartsWith($resolvedWorkspace + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)
    if (-not $insideWorkspace) {
        throw "$Label is outside the workspace: $fullPath"
    }
}

function Wait-ForApp {
    param(
        [string] $BaseUrl,
        [int] $TimeoutSeconds = 180
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

function Resolve-BrowserExecutable {
    param([string] $BrowserPath)

    if ($BrowserPath) {
        if (Test-Path -LiteralPath $BrowserPath) {
            return (Resolve-Path -LiteralPath $BrowserPath).Path
        }
        throw "The requested browser was not found: $BrowserPath"
    }

    $candidates = @()
    $candidates += "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
    $candidates += "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
    $candidates += "C:\Program Files\Google\Chrome\Application\chrome.exe"
    $candidates += "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
    $candidates += "C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe"
    $candidates += "C:\Program Files (x86)\BraveSoftware\Brave-Browser\Application\brave.exe"

    if ($env:ProgramFiles) {
        $candidates += Join-Path $env:ProgramFiles "Microsoft\Edge\Application\msedge.exe"
        $candidates += Join-Path $env:ProgramFiles "Google\Chrome\Application\chrome.exe"
        $candidates += Join-Path $env:ProgramFiles "BraveSoftware\Brave-Browser\Application\brave.exe"
    }
    if (${env:ProgramFiles(x86)}) {
        $candidates += Join-Path ${env:ProgramFiles(x86)} "Microsoft\Edge\Application\msedge.exe"
        $candidates += Join-Path ${env:ProgramFiles(x86)} "Google\Chrome\Application\chrome.exe"
        $candidates += Join-Path ${env:ProgramFiles(x86)} "BraveSoftware\Brave-Browser\Application\brave.exe"
    }
    if ($env:LOCALAPPDATA) {
        $candidates += Join-Path $env:LOCALAPPDATA "Microsoft\Edge\Application\msedge.exe"
        $candidates += Join-Path $env:LOCALAPPDATA "Google\Chrome\Application\chrome.exe"
        $candidates += Join-Path $env:LOCALAPPDATA "BraveSoftware\Brave-Browser\Application\brave.exe"
    }

    foreach ($name in @("msedge.exe", "chrome.exe", "brave.exe")) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($command -and $command.Source) {
            $candidates += $command.Source
        }
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw "No Chromium browser was found. Install Brave, Chrome or Edge to use the screenshot fallback."
}

function Get-CsrfTokenFromHtml {
    param([string] $Html)
    $inputMatch = [regex]::Match(
        $Html,
        '<input\b(?=[^>]*\bname\s*=\s*["'']csrfToken["''])[^>]*>',
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    $valueMatch = if ($inputMatch.Success) {
        [regex]::Match($inputMatch.Value, '\bvalue\s*=\s*(["''])(.*?)\1')
    } else {
        $null
    }
    if ($null -eq $valueMatch -or -not $valueMatch.Success -or [string]::IsNullOrWhiteSpace($valueMatch.Groups[2].Value)) {
        throw "Login page did not expose a non-empty CSRF token."
    }
    return [System.Net.WebUtility]::HtmlDecode($valueMatch.Groups[2].Value)
}

function New-SessionId {
    param(
        [string] $BaseUrl,
        [string] $TomcatBase,
        [string] $Email,
        [string] $Password
    )

    $tempDirectory = Join-Path $TomcatBase "temp"
    New-Item -ItemType Directory -Path $tempDirectory -Force | Out-Null
    $loginRunId = [System.Guid]::NewGuid().ToString("N")
    $cookieJar = Join-Path $tempDirectory "browser-screenshot-cookies-$loginRunId.txt"
    $loginBody = Join-Path $tempDirectory "browser-screenshot-login-$loginRunId.html"
    $loginHeaders = Join-Path $tempDirectory "browser-screenshot-login-$loginRunId.headers"

    if (Test-Path -LiteralPath $cookieJar) {
        Remove-Item -LiteralPath $cookieJar -Force
    }
    if (Test-Path -LiteralPath $loginBody) {
        Remove-Item -LiteralPath $loginBody -Force
    }
    if (Test-Path -LiteralPath $loginHeaders) {
        Remove-Item -LiteralPath $loginHeaders -Force
    }

    & curl.exe -s -L -c $cookieJar -b $cookieJar -o $loginBody "$BaseUrl/login.jsp"
    if ($LASTEXITCODE -ne 0) {
        throw "Login page request failed with exit code $LASTEXITCODE."
    }
    $csrfToken = Get-CsrfTokenFromHtml -Html (Get-Content -LiteralPath $loginBody -Raw)

    $loginStatus = & curl.exe -s -c $cookieJar -b $cookieJar -D $loginHeaders -o $loginBody -w "%{http_code}" "$BaseUrl/auth/login" `
        --data-urlencode "csrfToken=$csrfToken" `
        --data-urlencode "email=$Email" `
        --data-urlencode "password=$Password"

    if ($LASTEXITCODE -ne 0) {
        throw "Login request failed with exit code $LASTEXITCODE."
    }
    $location = Select-String -LiteralPath $loginHeaders -Pattern '(?i)^Location:\s*(.+?)\s*$' |
        Select-Object -Last 1 |
        ForEach-Object { $_.Matches[0].Groups[1].Value.Trim() }
    if ($loginStatus -notin @("302", "303") -or [string]::IsNullOrWhiteSpace($location) -or $location -match '(?i)/login\.jsp') {
        throw "Authentication failed for $Email (HTTP $loginStatus)."
    }

    $sessionLine = Select-String -LiteralPath $cookieJar -Pattern "JSESSIONID" -ErrorAction SilentlyContinue |
        Select-Object -Last 1
    if ($null -eq $sessionLine) {
        throw "Could not create an authenticated Browser QA session for $Email."
    }

    return (($sessionLine.Line -split "`t") | Select-Object -Last 1).Trim()
}

$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..\..")).Path
$tomcatBase = Join-Path $workspace "target\browser-tomcat10"
$baseUrl = "http://localhost:$Port/$ContextPath"

$outPath = $Out
if (-not [System.IO.Path]::IsPathRooted($outPath)) {
    $outPath = Join-Path $workspace $outPath
}
Assert-InWorkspace -Workspace $workspace -Path $outPath -Label "Screenshot output"
$outDirectory = Split-Path -Parent $outPath
New-Item -ItemType Directory -Path $outDirectory -Force | Out-Null

Wait-ForApp -BaseUrl $baseUrl -TimeoutSeconds $TimeoutSeconds
$browserExe = Resolve-BrowserExecutable -BrowserPath $BrowserPath
$sessionId = New-SessionId -BaseUrl $baseUrl -TomcatBase $tomcatBase -Email $Email -Password $Password

if ($Route -match '^https?://') {
    $targetUrl = $Route
} else {
    $normalizedRoute = $Route
    if (-not $normalizedRoute.StartsWith("/")) {
        $normalizedRoute = "/$normalizedRoute"
    }
    $targetUrl = "$($baseUrl.TrimEnd('/'));jsessionid=$sessionId$normalizedRoute"
}

$profileName = [System.IO.Path]::GetFileNameWithoutExtension($browserExe)
$profileRunId = [System.Guid]::NewGuid().ToString("N")
$publicDirectory = if ($env:PUBLIC) { $env:PUBLIC } else { "C:\Users\Public" }
$runtimeRoot = Join-Path $publicDirectory "gape-browser-screenshot"
$runtimeDirectory = Join-Path $runtimeRoot $profileRunId
$profileDir = Join-Path $runtimeDirectory "profile-$profileName"
$temporaryScreenshotPath = Join-Path $runtimeDirectory "screenshot.png"
New-Item -ItemType Directory -Path $profileDir -Force | Out-Null

$browserArguments = @(
    "--headless=new",
    "--disable-gpu",
    "--disable-software-rasterizer",
    "--disable-background-networking",
    "--disable-extensions",
    "--disable-dev-shm-usage",
    "--disable-features=CalculateNativeWinOcclusion",
    "--no-default-browser-check",
    "--no-first-run",
    "--no-sandbox",
    "--user-data-dir=$profileDir",
    "--window-size=$Width,$Height",
    "--force-device-scale-factor=$DeviceScaleFactor"
)
if ($WaitBeforeCaptureMs -gt 0) {
    $browserArguments += "--virtual-time-budget=$WaitBeforeCaptureMs"
}
$browserArguments += "--screenshot=$temporaryScreenshotPath"
$browserArguments += $targetUrl

Write-Output "Using browser: $browserExe"
Write-Output "Screenshot target: $targetUrl"

$process = Start-Process -FilePath $browserExe -ArgumentList $browserArguments -PassThru -WindowStyle Hidden
if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
    try {
        $process.Kill()
    } catch {
        # Ignore cleanup errors after timeout.
    }
    throw "Browser screenshot timed out after $TimeoutSeconds seconds."
}

if ($process.ExitCode -ne 0) {
    throw "Browser screenshot failed with exit code $($process.ExitCode)."
}

$fileDeadline = (Get-Date).AddSeconds(10)
while (-not (Test-Path -LiteralPath $temporaryScreenshotPath) -and (Get-Date) -lt $fileDeadline) {
    Start-Sleep -Milliseconds 250
}
if (-not (Test-Path -LiteralPath $temporaryScreenshotPath)) {
    throw "Browser did not create the screenshot at $temporaryScreenshotPath."
}
Copy-Item -LiteralPath $temporaryScreenshotPath -Destination $outPath -Force

Write-Output "Screenshot saved: $outPath"
Write-Output "URL: $targetUrl"
