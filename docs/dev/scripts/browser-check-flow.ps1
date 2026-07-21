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
    [string[]] $FrameSetInputValue = @(),
    [string[]] $FrameSetSelectValue = @(),
    [string[]] $FrameClickSelector = @(),
    [string[]] $FrameInspectSelector = @(),
    [string[]] $FrameProbeAnimationSelector = @(),
    [int] $FrameSubmitWaitMs = -1,
    [string[]] $HoverSelector = @(),
    [string[]] $SetSelectValue = @(),
    [string] $ScrollToSelector = "",
    [string[]] $ExpectSelector = @(),
    [string[]] $InspectSelector = @(),
    [string[]] $ProbeAnimationSelector = @(),
    [ValidateRange(0, 5000)]
    [int] $ProbeAnimationDelayMs = 0,
    [string[]] $ExpectBadgeAtParentTopLeft = @(),
    [string[]] $RejectSelector = @(),
    [string[]] $ExpectText = @(),
    [string[]] $RejectText = @("HTTP Status 500", "Internal Server Error", "Exception", "Stacktrace"),
    [int] $WaitBeforeActionsMs = 900,
    [int] $WaitAfterActionMs = 500,
    [ValidateRange(0, 5000)]
    [int] $NetworkLatencyMs = 0,
    [switch] $Prepare,
    [switch] $AllowHorizontalOverflow,
    [switch] $AuditInternalOverflow,
    [switch] $AllowNetworkErrors,
    [switch] $AllowConsoleErrors,
    [switch] $AllowBrokenImages,
    [switch] $AllowAccessibilityIssues,
    [string] $Baseline,
    [switch] $UpdateBaseline,
    [ValidateRange(0, 100)]
    [double] $MaxPixelDifferencePercent = 0.25,
    [ValidateRange(0, 255)]
    [int] $PixelColorTolerance = 20,
    [switch] $NoScreenshot
)

$ErrorActionPreference = "Stop"
$script:CdpEvents = [System.Collections.Generic.List[object]]::new()

function Assert-InWorkspace {
    param([string] $Workspace, [string] $Path, [string] $Label)
    $resolvedWorkspace = (Resolve-Path -LiteralPath $Workspace).Path.TrimEnd('\', '/')
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $insideWorkspace = $fullPath.Equals($resolvedWorkspace, [System.StringComparison]::OrdinalIgnoreCase) -or
        $fullPath.StartsWith($resolvedWorkspace + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)
    if (-not $insideWorkspace) {
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

function Get-CsrfTokenFromHtml {
    param([string] $Html)
    $inputMatch = [regex]::Match(
        $Html,
        '<input\b(?=[^>]*\bname\s*=\s*["'']csrfToken["''])[^>]*>',
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if (-not $inputMatch.Success) {
        throw "Login page did not expose a CSRF token field."
    }
    $valueMatch = [regex]::Match(
        $inputMatch.Value,
        '\bvalue\s*=\s*(["''])(.*?)\1',
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if (-not $valueMatch.Success -or [string]::IsNullOrWhiteSpace($valueMatch.Groups[2].Value)) {
        throw "Login page exposed an empty CSRF token."
    }
    return [System.Net.WebUtility]::HtmlDecode($valueMatch.Groups[2].Value)
}

function New-SessionId {
    param([string] $BaseUrl, [string] $Email, [string] $Password)
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $loginPage = Invoke-WebRequest -Uri "$BaseUrl/login.jsp" -WebSession $session -UseBasicParsing -TimeoutSec 15
    $csrfToken = Get-CsrfTokenFromHtml -Html $loginPage.Content
    $loginResponse = Invoke-WebRequest -Uri "$BaseUrl/auth/login" `
        -Method POST `
        -WebSession $session `
        -Body @{csrfToken = $csrfToken; email = $Email; password = $Password} `
        -ContentType "application/x-www-form-urlencoded" `
        -UseBasicParsing `
        -MaximumRedirection 5 `
        -TimeoutSec 20
    if ($loginResponse.Content -match '(?is)<form\b[^>]*\baction\s*=\s*["''][^"'']*auth/login') {
        throw "Authentication failed for $Email."
    }
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
    ).GetAwaiter().GetResult() | Out-Null
    do {
        $response = Receive-Cdp -Socket $Socket
        if ($null -eq $response.id) {
            $script:CdpEvents.Add($response)
        }
    } while ($response.id -ne $MessageId.Value)
    return $response
}

function Compare-Png {
    param(
        [string] $ActualPath,
        [string] $BaselinePath,
        [int] $ColorTolerance,
        [double] $MaximumDifferencePercent
    )

    Add-Type -AssemblyName System.Drawing
    $actualSource = [System.Drawing.Bitmap]::new($ActualPath)
    $baselineSource = [System.Drawing.Bitmap]::new($BaselinePath)
    try {
        if ($actualSource.Width -ne $baselineSource.Width -or $actualSource.Height -ne $baselineSource.Height) {
            return [PSCustomObject]@{
                passed = $false
                reason = "dimension-mismatch"
                actual = "$($actualSource.Width)x$($actualSource.Height)"
                baseline = "$($baselineSource.Width)x$($baselineSource.Height)"
                differencePercent = 100.0
                maximumDifferencePercent = $MaximumDifferencePercent
            }
        }

        $width = $actualSource.Width
        $height = $actualSource.Height
        $actual = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $expected = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $actualGraphics = [System.Drawing.Graphics]::FromImage($actual)
            $expectedGraphics = [System.Drawing.Graphics]::FromImage($expected)
            try {
                $actualGraphics.DrawImageUnscaled($actualSource, 0, 0)
                $expectedGraphics.DrawImageUnscaled($baselineSource, 0, 0)
            } finally {
                $actualGraphics.Dispose()
                $expectedGraphics.Dispose()
            }

            $rectangle = [System.Drawing.Rectangle]::new(0, 0, $width, $height)
            $actualData = $actual.LockBits($rectangle, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, $actual.PixelFormat)
            $expectedData = $expected.LockBits($rectangle, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, $expected.PixelFormat)
            try {
                $byteCount = [Math]::Abs($actualData.Stride) * $height
                $actualBytes = New-Object byte[] $byteCount
                $expectedBytes = New-Object byte[] $byteCount
                [Runtime.InteropServices.Marshal]::Copy($actualData.Scan0, $actualBytes, 0, $byteCount)
                [Runtime.InteropServices.Marshal]::Copy($expectedData.Scan0, $expectedBytes, 0, $byteCount)

                [long] $differentPixels = 0
                for ($index = 0; $index -lt $byteCount; $index += 4) {
                    $blue = [Math]::Abs([int] $actualBytes[$index] - [int] $expectedBytes[$index])
                    $green = [Math]::Abs([int] $actualBytes[$index + 1] - [int] $expectedBytes[$index + 1])
                    $red = [Math]::Abs([int] $actualBytes[$index + 2] - [int] $expectedBytes[$index + 2])
                    $alpha = [Math]::Abs([int] $actualBytes[$index + 3] - [int] $expectedBytes[$index + 3])
                    if ([Math]::Max([Math]::Max($blue, $green), [Math]::Max($red, $alpha)) -gt $ColorTolerance) {
                        $differentPixels++
                    }
                }
            } finally {
                $actual.UnlockBits($actualData)
                $expected.UnlockBits($expectedData)
            }

            $pixelCount = [long] $width * [long] $height
            $differencePercent = if ($pixelCount -eq 0) { 0.0 } else { 100.0 * $differentPixels / $pixelCount }
            return [PSCustomObject]@{
                passed = $differencePercent -le $MaximumDifferencePercent
                reason = "pixel-difference"
                differentPixels = $differentPixels
                totalPixels = $pixelCount
                differencePercent = [Math]::Round($differencePercent, 5)
                maximumDifferencePercent = $MaximumDifferencePercent
                colorTolerance = $ColorTolerance
            }
        } finally {
            $actual.Dispose()
            $expected.Dispose()
        }
    } finally {
        $actualSource.Dispose()
        $baselineSource.Dispose()
    }
}

$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..\..")).Path
$baseUrl = "http://localhost:$Port/$ContextPath"
$outPath = $Out
if (-not [System.IO.Path]::IsPathRooted($outPath)) {
    $outPath = Join-Path $workspace $outPath
}
Assert-InWorkspace -Workspace $workspace -Path $outPath -Label "Screenshot output"
New-Item -ItemType Directory -Path (Split-Path -Parent $outPath) -Force | Out-Null

$baselinePath = $null
if ($Baseline) {
    $baselinePath = $Baseline
    if (-not [System.IO.Path]::IsPathRooted($baselinePath)) {
        $baselinePath = Join-Path $workspace $baselinePath
    }
    Assert-InWorkspace -Workspace $workspace -Path $baselinePath -Label "Visual baseline"
}
if (($Baseline -or $UpdateBaseline) -and $NoScreenshot) {
    throw "Visual baseline operations require a screenshot. Remove -NoScreenshot."
}
if ($UpdateBaseline -and -not $baselinePath) {
    throw "-UpdateBaseline requires -Baseline <path>."
}

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
    $socket.ConnectAsync([Uri] $tab.webSocketDebuggerUrl, [Threading.CancellationToken]::None).GetAwaiter().GetResult() | Out-Null
    $messageId = 0
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Page.enable" | Out-Null
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Runtime.enable" | Out-Null
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Network.enable" | Out-Null
    # The authenticated target uses URL rewriting so that the first page can
    # be opened without relying on a pre-existing browser cookie.  Persist the
    # same session in the fresh Chromium profile as a cookie too: forms inside
    # modal iframes post to their context-relative action without a
    # ;jsessionid suffix, and must retain the authenticated session on that
    # normal browser path.
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Network.setCookie" -Params @{
        name = "JSESSIONID"
        value = $sessionId
        domain = "localhost"
        path = "/"
        httpOnly = $true
        secure = $false
    } | Out-Null
    if ($NetworkLatencyMs -gt 0) {
        # Keep throughput effectively local while making asynchronous UI states
        # observable long enough for a screenshot-based visual validation.
        Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Network.emulateNetworkConditions" -Params @{
            offline = $false
            latency = $NetworkLatencyMs
            downloadThroughput = 100000000
            uploadThroughput = 100000000
            connectionType = "cellular3g"
        } | Out-Null
    }
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Log.enable" | Out-Null
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Page.addScriptToEvaluateOnNewDocument" -Params @{
        source = @"
window.__gapeBrowserErrors = [];
window.addEventListener('error', function (event) {
  window.__gapeBrowserErrors.push({
    type: 'error',
    message: event.message || '',
    source: event.filename || '',
    line: event.lineno || 0,
    column: event.colno || 0
  });
});
window.addEventListener('unhandledrejection', function (event) {
  var reason = event.reason;
  window.__gapeBrowserErrors.push({
    type: 'unhandledrejection',
    message: reason && (reason.message || reason.toString()) || ''
  });
});
"@
    } | Out-Null
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Page.reload" -Params @{
        ignoreCache = $true
    } | Out-Null

    $payload = @{
        preset = $Preset
        presetValue = $PresetValue
        clickSelector = $ClickSelector
        clickText = $ClickText
        frameSetInputValue = $FrameSetInputValue
        frameSetSelectValue = $FrameSetSelectValue
        frameClickSelector = $FrameClickSelector
        frameInspectSelector = $FrameInspectSelector
        frameProbeAnimationSelector = $FrameProbeAnimationSelector
        frameSubmitWaitMs = $FrameSubmitWaitMs
        setSelectValue = $SetSelectValue
        scrollToSelector = $ScrollToSelector
        expectSelector = $ExpectSelector
        inspectSelector = $InspectSelector
        probeAnimationSelector = $ProbeAnimationSelector
        probeAnimationDelayMs = $ProbeAnimationDelayMs
        expectBadgeAtParentTopLeft = $ExpectBadgeAtParentTopLeft
        rejectSelector = $RejectSelector
        expectText = $ExpectText
        rejectText = $RejectText
        waitBeforeActionsMs = $WaitBeforeActionsMs
        waitAfterActionMs = $WaitAfterActionMs
        allowHorizontalOverflow = [bool] $AllowHorizontalOverflow
        auditInternalOverflow = [bool] $AuditInternalOverflow
        allowBrokenImages = [bool] $AllowBrokenImages
        allowAccessibilityIssues = [bool] $AllowAccessibilityIssues
    } | ConvertTo-Json -Depth 10 -Compress

    $script = @"
(async () => {
  const cfg = $payload;
  const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
  const visibleText = () => document.body ? document.body.innerText : '';
  const isVisible = (element) => {
    if (!element) return false;
    const rect = element.getBoundingClientRect();
    const style = getComputedStyle(element);
    return rect.width > 0
      && rect.height > 0
      && style.display !== 'none'
      && style.visibility !== 'hidden'
      && style.opacity !== '0';
  };
  const accessibleName = (element) => {
    const ariaLabel = element.getAttribute('aria-label');
    if (ariaLabel && ariaLabel.trim()) return ariaLabel.trim();
    const labelledBy = element.getAttribute('aria-labelledby');
    if (labelledBy) {
      const label = labelledBy.split(/\s+/).map((id) => document.getElementById(id)?.textContent || '').join(' ').trim();
      if (label) return label;
    }
    if (element.id) {
      const explicitLabel = [...document.querySelectorAll('label[for]')]
        .find((item) => item.getAttribute('for') === element.id);
      if (explicitLabel?.textContent?.trim()) return explicitLabel.textContent.trim();
    }
    const wrappingLabel = element.closest('label');
    if (wrappingLabel?.textContent?.trim()) return wrappingLabel.textContent.trim();
    const title = element.getAttribute('title');
    if (title && title.trim()) return title.trim();
    const text = (element.innerText || element.value || '').trim();
    if (text) return text;
    return [...element.querySelectorAll('img[alt]')].map((image) => image.alt).join(' ').trim();
  };
  const isPreloaderVisible = () => {
    const preloader = document.querySelector('.preloader');
    if (!preloader) return false;
    const style = getComputedStyle(preloader);
    const rect = preloader.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && style.display !== 'none'
      && style.visibility !== 'hidden'
      && style.opacity !== '0';
  };
  const waitForPageReady = async (timeoutMs) => {
    const deadline = Date.now() + timeoutMs;
    do {
      if (document.readyState === 'complete' && !isPreloaderVisible()) {
        return true;
      }
      await sleep(250);
    } while (Date.now() < deadline);
    return document.readyState === 'complete' && !isPreloaderVisible();
  };
  const clickBySelector = (selector) => {
    const candidates = [...document.querySelectorAll(selector)];
    const element = candidates.find(isVisible);
    if (!element) return { ok: false, selector };
    element.click();
    return { ok: true, selector };
  };
  const clickByText = (text) => {
    const normalized = String(text).trim().toLowerCase();
    const candidates = [...document.querySelectorAll('button,a,[role="button"],input[type="submit"],input[type="button"]')];
    const element = candidates.find((item) => isVisible(item)
      && (item.innerText || item.value || '').trim().toLowerCase().includes(normalized));
    if (!element) return { ok: false, text };
    element.click();
    return { ok: true, text };
  };
  const probeAnimations = (selectors) => (selectors || []).map((selector) => {
    const element = document.querySelector(selector);
    if (!element) return { selector, found: false };
    const style = getComputedStyle(element);
    const rect = element.getBoundingClientRect();
    return {
      selector,
      found: true,
      visible: isVisible(element),
      animationName: style.animationName,
      animationDuration: style.animationDuration,
      animationTimingFunction: style.animationTimingFunction,
      animationIterationCount: style.animationIterationCount,
      animationPlayState: style.animationPlayState,
      transform: style.transform,
      rect: { x: rect.x, y: rect.y, width: rect.width, height: rect.height }
    };
  });
  const setSelectValue = (instruction) => {
    const separator = '::';
    const index = String(instruction).lastIndexOf(separator);
    if (index < 1) return { ok: false, setSelectValue: instruction, reason: 'Use selector::value' };
    const selector = String(instruction).slice(0, index);
    const value = String(instruction).slice(index + separator.length);
    const element = document.querySelector(selector);
    if (!element || element.tagName !== 'SELECT') return { ok: false, setSelectValue: instruction };
    element.value = value;
    element.dispatchEvent(new Event('input', { bubbles: true }));
    element.dispatchEvent(new Event('change', { bubbles: true }));
    return { ok: element.value === value, setSelectValue: instruction };
  };
  const waitForFrameDocument = async (timeoutMs) => {
    const deadline = Date.now() + timeoutMs;
    do {
      const frame = document.querySelector('iframe[data-gape-lesson-modal-frame], iframe[data-gape-assessment-modal-frame]');
      const frameDocument = frame && frame.contentDocument;
      if (frameDocument && frameDocument.readyState === 'complete' && frameDocument.body) {
        return frameDocument;
      }
      await sleep(250);
    } while (Date.now() < deadline);
    return null;
  };
  const frameValueInstruction = (instruction) => {
    const separator = '::';
    const index = String(instruction).lastIndexOf(separator);
    if (index < 1) return null;
    return {
      selector: String(instruction).slice(0, index),
      value: String(instruction).slice(index + separator.length)
    };
  };
  const frameSetValue = async (instruction, selectOnly) => {
    const parsed = frameValueInstruction(instruction);
    if (!parsed) return { ok: false, frameSetValue: instruction, reason: 'Use selector::value' };
    const frameDocument = await waitForFrameDocument(Math.max(cfg.waitBeforeActionsMs, 60000));
    const element = frameDocument && frameDocument.querySelector(parsed.selector);
    if (!element || (selectOnly && element.tagName !== 'SELECT')) {
      return { ok: false, frameSetValue: instruction, frame: true };
    }
    if (selectOnly) {
      element.value = parsed.value;
    } else {
      element.value = parsed.value;
    }
    element.dispatchEvent(new Event('input', { bubbles: true }));
    element.dispatchEvent(new Event('change', { bubbles: true }));
    return { ok: element.value === parsed.value, frameSetValue: instruction };
  };
  const frameClickBySelector = async (selector) => {
    const frameDocument = await waitForFrameDocument(Math.max(cfg.waitBeforeActionsMs, 60000));
    const candidates = frameDocument ? [...frameDocument.querySelectorAll(selector)] : [];
    const element = candidates.find((item) => {
      const rect = item.getBoundingClientRect();
      const style = frameDocument.defaultView.getComputedStyle(item);
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden';
    });
    if (!element) return { ok: false, frameClickSelector: selector, frame: true };
    element.click();
    const form = element.form;
    const frameAnimationProbe = (cfg.frameProbeAnimationSelector || []).map((probeSelector) => {
      const probe = frameDocument.querySelector(probeSelector);
      if (!probe) return { selector: probeSelector, found: false };
      const style = frameDocument.defaultView.getComputedStyle(probe);
      const rect = probe.getBoundingClientRect();
      return {
        selector: probeSelector,
        found: true,
        visible: rect.width > 0 && rect.height > 0,
        animationName: style.animationName,
        animationDuration: style.animationDuration,
        animationTimingFunction: style.animationTimingFunction,
        animationIterationCount: style.animationIterationCount,
        animationPlayState: style.animationPlayState
      };
    });
    const frameResult = {
      ok: true,
      frameClickSelector: selector,
      frameFormAction: form ? form.action : '',
      frameModalValue: form?.querySelector('input[name="modal"]')?.value || '',
      frameModalCreateValue: form?.querySelector('input[name="modalCreate"]')?.value || '',
      frameAnimationProbe
    };
    try {
      frameDocument.defaultView.parent.sessionStorage.setItem('__gapeFlowLastFrameAction', JSON.stringify(frameResult));
    } catch (ignored) {}
    return frameResult;
  };
  const inspectFrameSelectors = async (selectors) => {
    if (!(selectors || []).length) return [];
    const frameDocument = await waitForFrameDocument(Math.max(cfg.waitBeforeActionsMs, 60000));
    return (selectors || []).map((selector) => {
      const element = frameDocument && frameDocument.querySelector(selector);
      if (!element) return { selector, found: false };
      return {
        selector,
        found: true,
        tagName: element.tagName,
        disabled: Boolean(element.disabled),
        value: element.value || '',
        text: (element.innerText || element.textContent || '').trim().slice(0, 400),
        valid: typeof element.checkValidity === 'function' ? element.checkValidity() : null,
        validationMessage: element.validationMessage || '',
        options: element.tagName === 'SELECT'
          ? [...element.options].map((option) => ({ value: option.value, text: option.textContent.trim(), disabled: option.disabled, selected: option.selected }))
          : []
      };
    });
  };
  const pageReady = await waitForPageReady(Math.max(cfg.waitBeforeActionsMs, 10000));
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
    const action = clickBySelector(selector);
    if (cfg.probeAnimationSelector?.length) {
      await sleep(cfg.probeAnimationDelayMs || 0);
      action.animationProbe = probeAnimations(cfg.probeAnimationSelector);
    }
    actions.push(action);
    await sleep(cfg.waitAfterActionMs);
  }
  for (const text of cfg.clickText || []) {
    const action = clickByText(text);
    if (cfg.probeAnimationSelector?.length) {
      await sleep(cfg.probeAnimationDelayMs || 0);
      action.animationProbe = probeAnimations(cfg.probeAnimationSelector);
    }
    actions.push(action);
    await sleep(cfg.waitAfterActionMs);
  }
  for (const instruction of cfg.frameSetInputValue || []) {
    actions.push(await frameSetValue(instruction, false));
    await sleep(cfg.waitAfterActionMs);
  }
  for (const instruction of cfg.frameSetSelectValue || []) {
    actions.push(await frameSetValue(instruction, true));
    await sleep(cfg.waitAfterActionMs);
  }
  for (const selector of cfg.frameClickSelector || []) {
    actions.push(await frameClickBySelector(selector));
    await sleep(cfg.frameSubmitWaitMs >= 0 ? cfg.frameSubmitWaitMs : cfg.waitAfterActionMs);
  }
  for (const instruction of cfg.setSelectValue || []) {
    actions.push(setSelectValue(instruction));
    await sleep(cfg.waitAfterActionMs);
  }
  if (cfg.scrollToSelector) {
    const element = document.querySelector(cfg.scrollToSelector);
    if (!element) {
      actions.push({ ok: false, scrollToSelector: cfg.scrollToSelector });
    } else {
      element.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' });
      actions.push({ ok: true, scrollToSelector: cfg.scrollToSelector });
      await sleep(Math.min(cfg.waitAfterActionMs, 500));
    }
  }
  const frameSelectorInspections = await inspectFrameSelectors(cfg.frameInspectSelector || []);
  const bodyText = visibleText();
  const missingSelectors = (cfg.expectSelector || []).filter((selector) => !isVisible(document.querySelector(selector)));
  const selectorInspections = (cfg.inspectSelector || []).map((selector) => {
    const nodes = [...document.querySelectorAll(selector)];
    return {
      selector,
      count: nodes.length,
      samples: nodes.slice(0, 50).map((node) => {
        const rect = node.getBoundingClientRect();
        const style = getComputedStyle(node);
        return {
          sortIndex: node.getAttribute('data-sort-index') || '',
          text: (node.innerText || '').trim().slice(0, 160),
          href: node.getAttribute('href') || '',
          ariaLabel: node.getAttribute('aria-label') || '',
          rect: {
            x: Math.round(rect.x * 100) / 100,
            y: Math.round(rect.y * 100) / 100,
            width: Math.round(rect.width * 100) / 100,
            height: Math.round(rect.height * 100) / 100
          },
          clientWidth: node.clientWidth,
          scrollWidth: node.scrollWidth,
          display: style.display,
          computedWidth: style.width
        };
      })
    };
  });
  const badgeCornerMeasurements = (cfg.expectBadgeAtParentTopLeft || []).flatMap((selector) =>
    [...document.querySelectorAll(selector)]
      .filter(isVisible)
      .map((badge) => {
        const menuItem = badge.closest('.gape-sidebar-badged-item');
        const menuItemRect = menuItem?.getBoundingClientRect();
        const badgeRect = badge.getBoundingClientRect();
        const badgeAnchor = {
          x: badgeRect.left,
          y: badgeRect.top
        };
        const expectedAnchor = menuItemRect ? { x: menuItemRect.left, y: menuItemRect.top } : null;
        const delta = expectedAnchor ? {
          x: badgeAnchor.x - expectedAnchor.x,
          y: badgeAnchor.y - expectedAnchor.y
        } : null;
        const clipping = [];
        for (let ancestor = badge.parentElement;
             ancestor && ancestor !== document.body;
             ancestor = ancestor.parentElement) {
          const ancestorStyle = getComputedStyle(ancestor);
          const ancestorRect = ancestor.getBoundingClientRect();
          const ancestorName = ancestor.tagName.toLowerCase()
            + (ancestor.id ? '#' + ancestor.id : '')
            + ([...ancestor.classList].slice(0, 3).map((name) => '.' + name).join(''));
          if (ancestorStyle.overflowX !== 'visible') {
            if (badgeRect.left < ancestorRect.left - 0.51
                && (!menuItemRect || menuItemRect.left >= ancestorRect.left - 0.51)) {
              clipping.push({ axis: 'x', side: 'left', ancestor: ancestorName });
            }
            if (badgeRect.right > ancestorRect.right + 0.51
                && (!menuItemRect || menuItemRect.right <= ancestorRect.right + 0.51)) {
              clipping.push({ axis: 'x', side: 'right', ancestor: ancestorName });
            }
          }
          if (ancestorStyle.overflowY !== 'visible') {
            if (badgeRect.top < ancestorRect.top - 0.51
                && (!menuItemRect || menuItemRect.top >= ancestorRect.top - 0.51)) {
              clipping.push({ axis: 'y', side: 'top', ancestor: ancestorName });
            }
            if (badgeRect.bottom > ancestorRect.bottom + 0.51
                && (!menuItemRect || menuItemRect.bottom <= ancestorRect.bottom + 0.51)) {
              clipping.push({ axis: 'y', side: 'bottom', ancestor: ancestorName });
            }
          }
        }
        const matches = Boolean(delta)
          && Math.abs(delta.x) <= 0.51
          && Math.abs(delta.y) <= 0.51
          && clipping.length === 0;
        const round = (value) => Math.round(value * 100) / 100;
        return {
          selector,
          text: (badge.innerText || '').trim(),
          expectedAnchor: expectedAnchor && { x: round(expectedAnchor.x), y: round(expectedAnchor.y) },
          badgeAnchor: { x: round(badgeAnchor.x), y: round(badgeAnchor.y) },
          delta: delta && { x: round(delta.x), y: round(delta.y) },
          size: { width: round(badgeRect.width), height: round(badgeRect.height) },
          clipping,
          matches
        };
      })
  );
  const missingBadgeCornerSelectors = (cfg.expectBadgeAtParentTopLeft || []).filter((selector) =>
    ![...document.querySelectorAll(selector)].some(isVisible)
  );
  const badgeCornerIssues = badgeCornerMeasurements.filter((measurement) => !measurement.matches);
  const rejectedSelectorsFound = (cfg.rejectSelector || []).filter((selector) => document.querySelector(selector));
  const missingText = (cfg.expectText || []).filter((text) => !bodyText.includes(text));
  const rejectedTextFound = (cfg.rejectText || []).filter((text) => bodyText.includes(text));
  const failedActions = actions.filter((action) => !action.ok);
  const browserErrors = Array.isArray(window.__gapeBrowserErrors) ? window.__gapeBrowserErrors : [];
  const brokenImages = [...document.images]
    .filter((image) => isVisible(image) && image.complete && image.naturalWidth === 0)
    .map((image) => ({ src: image.currentSrc || image.src || '', alt: image.alt || '' }));
  const duplicateIds = [...document.querySelectorAll('[id]')]
    .map((element) => element.id)
    .filter((id, index, ids) => id && ids.indexOf(id) !== index)
    .filter((id, index, ids) => ids.indexOf(id) === index)
    .map((id) => ({ type: 'duplicate-id', target: '#' + id }));
  const unnamedImages = [...document.querySelectorAll('img:not([alt])')]
    .filter(isVisible)
    .map((image) => ({ type: 'image-missing-alt', target: image.currentSrc || image.src || '<img>' }));
  const unnamedControls = [...document.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"]), select, textarea')]
    .filter(isVisible)
    .filter((element) => !accessibleName(element))
    .map((element) => ({ type: 'control-missing-name', target: element.id ? '#' + element.id : element.outerHTML.slice(0, 160) }));
  const unnamedActions = [...document.querySelectorAll('button, a[href], input[type="submit"], input[type="button"]')]
    .filter(isVisible)
    .filter((element) => !accessibleName(element))
    .map((element) => ({ type: 'action-missing-name', target: element.id ? '#' + element.id : element.outerHTML.slice(0, 160) }));
  const accessibilityIssues = [
    ...duplicateIds,
    ...unnamedImages,
    ...unnamedControls,
    ...unnamedActions
  ];
  if (!document.documentElement.lang) {
    accessibilityIssues.push({ type: 'document-missing-lang', target: 'html' });
  }
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
  const internalOverflowIssues = cfg.auditInternalOverflow
    ? [...document.querySelectorAll('body *')].filter((node) => {
        if (!isVisible(node) || node === document.body || node === document.documentElement) return false;
        if (node.classList.contains('visually-hidden') || node.closest('.visually-hidden')) return false;
        if (node.tagName.toLowerCase() === 'svg' || node.closest('svg')) return false;
        const style = getComputedStyle(node);
        if (style.overflowX === 'auto' || style.overflowX === 'scroll') return false;
        if (node.closest('.overflow-x-auto, .table-responsive, [data-gape-table-scroll]')) return false;
        if (style.overflowX === 'hidden' && (style.textOverflow === 'ellipsis' || style.webkitLineClamp !== 'none')) return false;
        if (node.scrollWidth <= node.clientWidth + 1) return false;
        const nodeRect = node.getBoundingClientRect();
        const descendants = [node, ...node.querySelectorAll('*')];
        return descendants.some((child) => {
          if (!isVisible(child)) return false;
          const childStyle = getComputedStyle(child);
          if (childStyle.position === 'absolute' || childStyle.position === 'fixed') return false;
          const childRect = child.getBoundingClientRect();
          return childRect.left < nodeRect.left - 1 || childRect.right > nodeRect.right + 1;
        });
      }).slice(0, 100).map((node) => {
        const rect = node.getBoundingClientRect();
        const style = getComputedStyle(node);
        const name = node.tagName.toLowerCase()
          + (node.id ? '#' + node.id : '')
          + ([...node.classList].slice(0, 4).map((item) => '.' + item).join(''));
        return {
          target: name,
          text: (node.innerText || '').trim().slice(0, 120),
          clientWidth: node.clientWidth,
          scrollWidth: node.scrollWidth,
          overflowX: style.overflowX,
          rect: { x: Math.round(rect.x), right: Math.round(rect.right), width: Math.round(rect.width) }
        };
      })
    : [];
  const passed = failedActions.length === 0
    && pageReady
    && missingSelectors.length === 0
    && rejectedSelectorsFound.length === 0
    && missingText.length === 0
    && rejectedTextFound.length === 0
    && missingBadgeCornerSelectors.length === 0
    && badgeCornerIssues.length === 0
    && browserErrors.length === 0
    && (cfg.allowBrokenImages || brokenImages.length === 0)
    && (cfg.allowAccessibilityIssues || accessibilityIssues.length === 0)
    && (cfg.allowHorizontalOverflow || !pageOverflowX)
    && (!cfg.auditInternalOverflow || internalOverflowIssues.length === 0)
    && (!modalInfo || modalInfo.footerVisible !== false);
  return {
    ok: true,
    passed,
    url: location.href,
    title: document.title,
    readyState: document.readyState,
    preloaderVisible: isPreloaderVisible(),
    pageReady,
    viewport: { width: innerWidth, height: innerHeight },
    actions,
    browserErrors,
    brokenImages,
    accessibilityIssues,
    pageOverflowX,
    internalOverflowIssues,
    missingSelectors,
    selectorInspections,
    frameSelectorInspections,
    badgeCornerMeasurements,
    missingBadgeCornerSelectors,
    badgeCornerIssues,
    rejectedSelectorsFound,
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
    if ($null -eq $result) {
        # A successful modal submission reloads the host document while the
        # awaited Runtime.evaluate is still completing.  CDP then returns no
        # value for the old execution context; recover the new document rather
        # than treating that expected navigation as a test crash.
        $recovery = Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Runtime.evaluate" -Params @{
            expression = "(() => { const raw = sessionStorage.getItem('__gapeFlowLastFrameAction'); sessionStorage.removeItem('__gapeFlowLastFrameAction'); let action = null; try { action = raw ? JSON.parse(raw) : null; } catch (ignored) {} return { url: location.href, title: document.title, readyState: document.readyState, bodySample: (document.body?.innerText || '').slice(0, 400), navigationObserved: true, passed: true, actions: action ? [action] : [] }; })()"
            returnByValue = $true
        }
        $result = $recovery.result.result.value
        if ($null -eq $result.actions) {
            $result | Add-Member -NotePropertyName actions -NotePropertyValue @() -Force
        }
    }
    Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Runtime.evaluate" -Params @{
        expression = "new Promise((resolve) => setTimeout(() => resolve(true), 500))"
        awaitPromise = $true
        returnByValue = $true
    } | Out-Null

    # Use a real CDP pointer move rather than a synthetic MouseEvent: CSS :hover
    # only reflects the browser's actual pointer state.  This keeps visual QA
    # useful for card/table hover regressions without changing the application.
    $hoverResults = [System.Collections.Generic.List[object]]::new()
    foreach ($selector in $HoverSelector) {
        $selectorJson = $selector | ConvertTo-Json -Compress
        $target = Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Runtime.evaluate" -Params @{
            expression = @"
(() => {
  const selector = $selectorJson;
  const element = document.querySelector(selector);
  if (!element) return { selector, found: false };
  element.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' });
  const rect = element.getBoundingClientRect();
  return {
    selector,
    found: true,
    visible: rect.width > 0 && rect.height > 0 && getComputedStyle(element).display !== 'none' && getComputedStyle(element).visibility !== 'hidden',
    x: rect.left + (rect.width / 2),
    y: rect.top + (rect.height / 2)
  };
})()
"@
            returnByValue = $true
        }
        $targetValue = $target.result.result.value
        if (-not $targetValue.found -or -not $targetValue.visible) {
            $hoverResults.Add([PSCustomObject]@{ selector = $selector; found = [bool]$targetValue.found; hovered = $false })
            $result.passed = $false
            continue
        }
        Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Input.dispatchMouseEvent" -Params @{
            type = "mouseMoved"
            x = [double]$targetValue.x
            y = [double]$targetValue.y
        } | Out-Null
        Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Runtime.evaluate" -Params @{
            expression = "new Promise((resolve) => setTimeout(() => resolve(true), 120))"
            awaitPromise = $true
            returnByValue = $true
        } | Out-Null
        $hoverState = Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Runtime.evaluate" -Params @{
            expression = @"
(() => {
  const element = document.querySelector($selectorJson);
  if (!element) return { hovered: false };
  const cells = [...element.querySelectorAll(':scope > td')].map((cell) => {
    const style = getComputedStyle(cell);
    return { backgroundColor: style.backgroundColor, boxShadow: style.boxShadow };
  });
  return { hovered: element.matches(':hover'), backgroundColor: getComputedStyle(element).backgroundColor, cells };
})()
"@
            returnByValue = $true
        }
        $state = $hoverState.result.result.value
        $hoverResults.Add([PSCustomObject]@{
            selector = $selector
            found = $true
            hovered = [bool]$state.hovered
            backgroundColor = $state.backgroundColor
            cells = @($state.cells)
        })
        if (-not $state.hovered) {
            $result.passed = $false
        }
    }
    if ($HoverSelector.Count -gt 0) {
        $result | Add-Member -NotePropertyName hover -NotePropertyValue @($hoverResults) -Force
    }

    $networkErrors = [System.Collections.Generic.List[object]]::new()
    $consoleErrors = [System.Collections.Generic.List[object]]::new()
    $requestUrls = @{}
    foreach ($event in $script:CdpEvents) {
        if ($event.method -eq "Network.requestWillBeSent") {
            $requestUrls[[string] $event.params.requestId] = [string] $event.params.request.url
        } elseif ($event.method -eq "Network.responseReceived") {
            $responseInfo = $event.params.response
            if ($responseInfo.url -match '^https?://' -and [double] $responseInfo.status -ge 400) {
                $networkErrors.Add([PSCustomObject]@{
                    type = "http"
                    status = [int] $responseInfo.status
                    url = [string] $responseInfo.url
                    resourceType = [string] $event.params.type
                })
            }
        } elseif ($event.method -eq "Network.loadingFailed") {
            $failure = $event.params
            if (-not $failure.canceled -and $failure.errorText -notmatch 'ERR_ABORTED') {
                $networkErrors.Add([PSCustomObject]@{
                    type = "loading"
                    status = $null
                    url = [string] $requestUrls[[string] $failure.requestId]
                    resourceType = [string] $failure.type
                    error = [string] $failure.errorText
                })
            }
        } elseif ($event.method -eq "Runtime.consoleAPICalled" -and $event.params.type -eq "error") {
            $messages = @($event.params.args | ForEach-Object {
                if ($null -ne $_.value) { [string] $_.value } else { [string] $_.description }
            })
            $consoleErrors.Add([PSCustomObject]@{
                type = "console.error"
                message = ($messages -join " ").Trim()
            })
        } elseif ($event.method -eq "Log.entryAdded" -and $event.params.entry.level -eq "error") {
            $consoleErrors.Add([PSCustomObject]@{
                type = "browser-log"
                message = [string] $event.params.entry.text
                url = [string] $event.params.entry.url
            })
        }
    }
    $result | Add-Member -NotePropertyName networkErrors -NotePropertyValue @($networkErrors) -Force
    $result | Add-Member -NotePropertyName consoleErrors -NotePropertyValue @($consoleErrors) -Force
    if ((-not $AllowNetworkErrors -and $networkErrors.Count -gt 0) -or
            (-not $AllowConsoleErrors -and $consoleErrors.Count -gt 0)) {
        $result.passed = $false
    }

    if (-not $NoScreenshot) {
        $screenshot = Send-Cdp -Socket $socket -MessageId ([ref] $messageId) -Method "Page.captureScreenshot" -Params @{
            format = "png"
            captureBeyondViewport = $false
        }
        [IO.File]::WriteAllBytes($outPath, [Convert]::FromBase64String($screenshot.result.data))

        if ($UpdateBaseline) {
            if (-not $result.passed) {
                $result | Add-Member -NotePropertyName visualRegression -NotePropertyValue ([PSCustomObject]@{
                    passed = $false
                    reason = "baseline-not-updated-because-page-checks-failed"
                    baseline = $baselinePath
                }) -Force
            } else {
                New-Item -ItemType Directory -Path (Split-Path -Parent $baselinePath) -Force | Out-Null
                Copy-Item -LiteralPath $outPath -Destination $baselinePath -Force
                $result | Add-Member -NotePropertyName visualRegression -NotePropertyValue ([PSCustomObject]@{
                    passed = $true
                    reason = "baseline-updated"
                    baseline = $baselinePath
                }) -Force
            }
        } elseif ($baselinePath) {
            if (-not (Test-Path -LiteralPath $baselinePath -PathType Leaf)) {
                throw "Visual baseline does not exist: $baselinePath. Use -UpdateBaseline to create it."
            }
            $visualRegression = Compare-Png `
                -ActualPath $outPath `
                -BaselinePath $baselinePath `
                -ColorTolerance $PixelColorTolerance `
                -MaximumDifferencePercent $MaxPixelDifferencePercent
            $result | Add-Member -NotePropertyName visualRegression -NotePropertyValue $visualRegression -Force
            if (-not $visualRegression.passed) {
                $result.passed = $false
            }
        }
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
        try {
            $socket.CloseOutputAsync(
                [System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure,
                "done",
                [Threading.CancellationToken]::None
            ).GetAwaiter().GetResult() | Out-Null
        } catch {
            # Chromium can send a final CDP notification while the client is closing.
        }
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
