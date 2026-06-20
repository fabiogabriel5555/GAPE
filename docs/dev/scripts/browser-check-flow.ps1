param(
    [int] $Port = 18080,
    [string] $ContextPath = "GAPE",
    [Alias("Path")]
    [string] $Route = "/admin/admin-dashbord.jsp",
    [ValidateSet("Page", "AddContent")]
    [string] $Preset = "Page",
    [string] $PresetValue = "Video",
    [string] $Email = "admin@gape.local",
    [string] $Password = "Password#2026",
    [string] $Out = "target\browser-screenshots\browser-check-flow.png",
    [int] $Width = 1920,
    [int] $Height = 1200,
    [int] $TimeoutSeconds = 90,
    [string] $BrowserPath,
    [string[]] $ClickSelector = @(),
    [string[]] $ClickText = @(),
    [string[]] $ExpectSelector = @(),
    [string[]] $ExpectText = @(),
    [string[]] $RejectText = @("HTTP Status 500", "Internal Server Error", "Exception", "Stacktrace"),
    [int] $WaitBeforeActionsMs = 900,
    [int] $WaitAfterActionMs = 500,
    [switch] $Prepare,
    [switch] $AllowHorizontalOverflow,
    [switch] $NoScreenshot
)

$ErrorActionPreference = "Stop"

function Assert-InWorkspace {
    param([string] $Workspace, [string] $Path, [string] $Label)
    $resolvedWorkspace = (Resolve-Path -LiteralPath $Workspace).Path
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    if (-not $fullPath.StartsWith($resolvedWorkspace, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "$Label is outside the workspace: $fullPath"
    }
}

function Wait-ForApp {
    param([string] $BaseUrl, [int] $TimeoutSeconds)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri "$BaseUrl/login.jsp" -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 400
        }
    } while ((Get-Date) -lt $deadline)
    throw "The app did not respond at $BaseUrl. Run browser-prepare.ps1 -SkipPackage first, or pass -Prepare."
}

function Resolve-BrowserExecutable {
    param([string] $BrowserPath)
    if ($BrowserPath) {
        if (Test-Path -LiteralPath $BrowserPath) {
            return (Resolve-Path -LiteralPath $BrowserPath).Path
        }
        throw "The requested browser was not found: $BrowserPath"
    }

    $candidates = @(
        "C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe",
        "C:\Program Files (x86)\BraveSoftware\Brave-Browser\Application\brave.exe",
        "C:\Program Files\Microsoft\Edge\Application\msedge.exe",
        "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
        "C:\Program Files\Google\Chrome\Application\chrome.exe",
        "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
    )
    foreach ($name in @("brave.exe", "msedge.exe", "chrome.exe")) {
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
    throw "No Chromium browser was found. Install Brave, Edge or Chrome."
}

function New-SessionId {
    param([string] $BaseUrl, [string] $Email, [string] $Password)
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    Invoke-WebRequest -Uri "$BaseUrl/login.jsp" -WebSession $session -UseBasicParsing -TimeoutSec 15 | Out-Null
    Invoke-WebRequest -Uri "$BaseUrl/auth/login" `
        -Method POST `
        -WebSession $session `
        -Body @{email = $Email; password = $Password} `
        -ContentType "application/x-www-form-urlencoded" `
        -UseBasicParsing `
        -MaximumRedirection 5 `
        -TimeoutSec 20 | Out-Null
    $cookie = $session.Cookies.GetCookies($BaseUrl) |
        Where-Object { $_.Name -eq "JSESSIONID" } |
        Select-Object -First 1
    if (-not $cookie) {
        throw "Could not create an authenticated session for $Email."
    }
    return $cookie.Value
}

function Receive-Cdp {
    param([System.Net.WebSockets.ClientWebSocket] $Socket)
    $buffer = New-Object byte[] 1048576
    $segment = [ArraySegment[byte]]::new($buffer)
    $builder = New-Object System.Text.StringBuilder
    do {
        $result = $Socket.ReceiveAsync($segment, [Threading.CancellationToken]::None).GetAwaiter().GetResult()
        if ($result.Count -gt 0) {
            [void] $builder.Append([Text.Encoding]::UTF8.GetString($buffer, 0, $result.Count))
        }
    } while (-not $result.EndOfMessage)
    return ($builder.ToString() | ConvertFrom-Json)
}

function Send-Cdp {
    param(
        [System.Net.WebSockets.ClientWebSocket] $Socket,
        [ref] $MessageId,
        [string] $Method,
        [object] $Params = $null
    )
    $MessageId.Value++
    $message = @{id = $MessageId.Value; method = $Method}
    if ($null -ne $Params) {
        $message.params = $Params
    }
    $json = $message | ConvertTo-Json -Depth 30 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $Socket.SendAsync(
        [ArraySegment[byte]]::new($bytes),
        [System.Net.WebSockets.WebSocketMessageType]::Text,
        $true,
        [Threading.CancellationToken]::None
    ).GetAwaiter().GetResult()
    do {
        $response = Receive-Cdp -Socket $Socket
    } while ($response.id -ne $MessageId.Value)
    return $response
}

$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..\..")).Path
$baseUrl = "http://localhost:$Port/$ContextPath"
$outPath = $Out
if (-not [System.IO.Path]::IsPathRooted($outPath)) {
    $outPath = Join-Path $workspace $outPath
}
Assert-InWorkspace -Workspace $workspace -Path $outPath -Label "Screenshot output"
New-Item -ItemType Directory -Path (Split-Path -Parent $outPath) -Force | Out-Null

if ($Prepare) {
    & (Join-Path $PSScriptRoot "browser-prepare.ps1") -Port $Port -ContextPath $ContextPath -Email $Email -Password $Password -SkipPackage
}

Wait-ForApp -BaseUrl $baseUrl -TimeoutSeconds $TimeoutSeconds
$browserExe = Resolve-BrowserExecutable -BrowserPath $BrowserPath
$sessionId = New-SessionId -BaseUrl $baseUrl -Email $Email -Password $Password
$normalizedRoute = if ($Route.StartsWith("/")) { $Route } else { "/$Route" }
$targetUrl = "$($baseUrl.TrimEnd('/'));jsessionid=$sessionId$normalizedRoute"

$debugPort = Get-Random -Minimum 19000 -Maximum 19900
$profile = Join-Path $env:TEMP ("gape-flow-cdp-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $profile -Force | Out-Null

$browserArguments = @(
    "--headless=new",
    "--disable-gpu",
    "--no-first-run",
    "--no-default-browser-check",
    "--disable-extensions",
    "--disable-dev-shm-usage",
    "--remote-debugging-port=$debugPort",
    "--window-size=$Width,$Height",
    "--user-data-dir=$profile",
    $targetUrl
)

$process = Start-Process -FilePath $browserExe -ArgumentList $browserArguments -PassThru -WindowStyle Hidden
$socket = $null
try {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $tabs = Invoke-RestMethod -Uri "http://127.0.0.1:$debugPort/json/list" -TimeoutSec 2
            break
        } catch {
            Start-Sleep -Milliseconds 300
        }
    } while ((Get-Date) -lt $deadline)
    if (-not $tabs) {
        throw "CDP endpoint did not become available on port $debugPort."
    }

    $tab = $tabs | Where-Object { $_.url -like "*$normalizedRoute*" } | Select-Object -First 1
    if (-not $tab) {
        $tab = $tabs | Select-Object -First 1
    }

    $socket = [System.Net.WebSockets.ClientWebSocket]::new()
    $socket.ConnectAsync([Uri] $tab.webSocketDebuggerUrl, [Threading.CancellationToken]::None).GetAwaiter().GetResult()
    $messageId = 0
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Page.enable" | Out-Null
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Runtime.enable" | Out-Null

    $payload = @{
        preset = $Preset
        presetValue = $PresetValue
        clickSelector = $ClickSelector
        clickText = $ClickText
        expectSelector = $ExpectSelector
        expectText = $ExpectText
        rejectText = $RejectText
        waitBeforeActionsMs = $WaitBeforeActionsMs
        waitAfterActionMs = $WaitAfterActionMs
        allowHorizontalOverflow = [bool] $AllowHorizontalOverflow
    } | ConvertTo-Json -Depth 10 -Compress

    $script = @"
(async () => {
  const cfg = $payload;
  const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
  const visibleText = () => document.body ? document.body.innerText : '';
  const clickBySelector = (selector) => {
    const element = document.querySelector(selector);
    if (!element) return { ok: false, selector };
    element.click();
    return { ok: true, selector };
  };
  const clickByText = (text) => {
    const normalized = String(text).trim().toLowerCase();
    const candidates = [...document.querySelectorAll('button,a,[role="button"],input[type="submit"],input[type="button"]')];
    const element = candidates.find((item) => (item.innerText || item.value || '').trim().toLowerCase().includes(normalized));
    if (!element) return { ok: false, text };
    element.click();
    return { ok: true, text };
  };
  await sleep(cfg.waitBeforeActionsMs);
  const actions = [];
  if (cfg.preset === 'AddContent') {
    const trigger = document.querySelector('[data-bs-target^="#addContent"]');
    if (!trigger) return { ok: false, passed: false, reason: 'add content trigger not found', url: location.href, title: document.title };
    trigger.click();
    actions.push({ ok: true, preset: 'AddContent.open' });
    await sleep(cfg.waitAfterActionMs);
    const modal = document.querySelector('.cg-activity-modal.show');
    const option = modal ? [...modal.querySelectorAll('[data-content-option]')]
      .find((item) => item.getAttribute('data-title') === cfg.presetValue) : null;
    if (!modal || !option) return { ok: false, passed: false, reason: 'add content option not found', presetValue: cfg.presetValue, url: location.href, title: document.title };
    option.click();
    actions.push({ ok: true, preset: 'AddContent.select', value: cfg.presetValue });
    await sleep(cfg.waitAfterActionMs);
  }
  for (const selector of cfg.clickSelector || []) {
    actions.push(clickBySelector(selector));
    await sleep(cfg.waitAfterActionMs);
  }
  for (const text of cfg.clickText || []) {
    actions.push(clickByText(text));
    await sleep(cfg.waitAfterActionMs);
  }
  const bodyText = visibleText();
  const missingSelectors = (cfg.expectSelector || []).filter((selector) => !document.querySelector(selector));
  const missingText = (cfg.expectText || []).filter((text) => !bodyText.includes(text));
  const rejectedTextFound = (cfg.rejectText || []).filter((text) => bodyText.includes(text));
  const failedActions = actions.filter((action) => !action.ok);
  const modal = document.querySelector('.modal.show');
  let modalInfo = null;
  if (modal) {
    const dialog = modal.querySelector('.modal-dialog');
    const footer = modal.querySelector('.modal-footer');
    const main = modal.querySelector('.cg-activity-main') || modal.querySelector('.modal-body');
    const file = modal.querySelector('input[name="file"]');
    const fileWrapper = modal.querySelector('[data-file-wrapper]');
    const selectedRepositoryFile = modal.querySelector('[data-selected-repository-file]');
    const dialogRect = dialog ? dialog.getBoundingClientRect() : null;
    const footerRect = footer ? footer.getBoundingClientRect() : null;
    const fileRect = file ? file.getBoundingClientRect() : null;
    const isVisible = (element) => {
      if (!element) {
        return false;
      }
      const rect = element.getBoundingClientRect();
      const style = getComputedStyle(element);
      return rect.width > 0
        && rect.height > 0
        && style.display !== 'none'
        && style.visibility !== 'hidden'
        && style.opacity !== '0';
    };
    modalInfo = {
      title: modal.querySelector('.modal-title')?.innerText.trim() || '',
      dialog: dialogRect ? {
        width: Math.round(dialogRect.width),
        height: Math.round(dialogRect.height),
        top: Math.round(dialogRect.top),
        bottom: Math.round(dialogRect.bottom)
      } : null,
      mainScroll: main ? {
        clientHeight: main.clientHeight,
        scrollHeight: main.scrollHeight,
        hasVerticalScroll: main.scrollHeight > main.clientHeight + 1
      } : null,
      footerVisible: footerRect ? footerRect.bottom <= innerHeight && footerRect.top >= 0 : null,
      fileVisible: file && footerRect ? isVisible(file) && fileRect.bottom <= footerRect.top && fileRect.top >= 0 : null,
      fileWrapperVisible: fileWrapper ? isVisible(fileWrapper) : null,
      selectedTitle: modal.querySelector('[data-content-option].is-selected')?.getAttribute('data-title') || '',
      repositoryContentItemId: modal.querySelector('[data-repository-content-id]')?.value || '',
      repositoryMetadataMode: modal.querySelector('[data-repository-metadata-mode]')?.value || '',
      repositoryPreviewCount: modal.querySelectorAll('.cg-repository-preview').length,
      selectedRepositoryFileVisible: selectedRepositoryFile ? isVisible(selectedRepositoryFile) : null,
      selectedRepositoryPreviewHref: modal.querySelector('[data-selected-repository-preview]')?.getAttribute('href') || '',
      titleValue: modal.querySelector('input[name="title"]')?.value || '',
      titleDisabled: modal.querySelector('input[name="title"]')?.disabled || false,
      mandatoryValue: modal.querySelector('[data-mandatory-input]')?.value || '',
      mandatoryDisabled: modal.querySelector('[data-mandatory-input]')?.disabled || false,
      format: modal.querySelector('[data-content-format]')?.value || '',
      fileLabel: modal.querySelector('[data-file-label]')?.innerText.trim() || '',
      fileAccept: file?.getAttribute('accept') || ''
    };
  }
  const pageOverflowX = document.documentElement.scrollWidth > innerWidth;
  const passed = failedActions.length === 0
    && missingSelectors.length === 0
    && missingText.length === 0
    && rejectedTextFound.length === 0
    && (cfg.allowHorizontalOverflow || !pageOverflowX)
    && (!modalInfo || modalInfo.footerVisible !== false);
  return {
    ok: true,
    passed,
    url: location.href,
    title: document.title,
    viewport: { width: innerWidth, height: innerHeight },
    actions,
    pageOverflowX,
    missingSelectors,
    missingText,
    rejectedTextFound,
    modal: modalInfo,
    bodySample: bodyText.slice(0, 400)
  };
})()
"@

    $evaluation = Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Runtime.evaluate" -Params @{
        expression = $script
        awaitPromise = $true
        returnByValue = $true
    }
    $result = $evaluation.result.result.value

    if (-not $NoScreenshot) {
        $screenshot = Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Page.captureScreenshot" -Params @{
            format = "png"
            captureBeyondViewport = $false
        }
        [IO.File]::WriteAllBytes($outPath, [Convert]::FromBase64String($screenshot.result.data))
    }

    $result | ConvertTo-Json -Depth 20
    if (-not $NoScreenshot) {
        Write-Output "Screenshot: $outPath"
    }
    if (-not $result.passed) {
        throw "Browser flow check failed."
    }
} finally {
    if ($socket -and $socket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        $socket.CloseAsync(
            [System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure,
            "done",
            [Threading.CancellationToken]::None
        ).GetAwaiter().GetResult() | Out-Null
    }
    if ($process -and -not $process.HasExited) {
        $process.Kill()
        Start-Sleep -Milliseconds 1000
    }
    if (Test-Path -LiteralPath $profile) {
        try {
            Remove-Item -LiteralPath $profile -Recurse -Force -ErrorAction Stop
        } catch {
            # Chromium can keep profile files locked briefly on Windows. The profile is under %TEMP%.
        }
    }
}
