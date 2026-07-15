param(
    [int] $Port = 18080,
    [string] $ContextPath = "GAPE",
    [long] $ClassGroupId = 53,
    [string] $Email = "admin@gape.local",
    [string] $Password = "Password#2026",
    [string] $PdfFile,
    [switch] $Prepare
)

$ErrorActionPreference = "Stop"

function New-TestFile {
    param(
        [string] $Directory,
        [string] $Name,
        [string] $Content,
        [System.Text.Encoding] $Encoding = [System.Text.Encoding]::ASCII
    )

    $path = Join-Path $Directory $Name
    [System.IO.File]::WriteAllText($path, $Content, $Encoding)
    return $path
}

function Find-SampleImage {
    $workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..\..")).Path
    $imageRoot = Join-Path $workspace "src\main\webapp\assets\images"
    $image = Get-ChildItem -Path $imageRoot -Recurse -File -Include *.png,*.jpg,*.jpeg,*.webp |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $image) {
        throw "No sample image was found under $imageRoot."
    }
    return $image
}

function Get-ImageMimeType {
    param([string] $Path)
    switch ([System.IO.Path]::GetExtension($Path).ToLowerInvariant()) {
        ".png" { "image/png" }
        ".jpg" { "image/jpeg" }
        ".jpeg" { "image/jpeg" }
        ".webp" { "image/webp" }
        default { throw "Unsupported sample image extension: $Path" }
    }
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

function Invoke-Upload {
    param(
        [string] $BaseUrl,
        [string] $CookieJar,
        [string] $ResponsePath,
        [string] $CsrfToken,
        [string] $BlockId,
        [string] $Name,
        [string] $Format,
        [string] $Role,
        [string] $File,
        [string] $MimeType
    )

    $status = & curl.exe -s -L -b $CookieJar -c $CookieJar -o $ResponsePath -w "%{http_code}" -X POST "$BaseUrl/contents/upload" `
        -F "csrfToken=$CsrfToken" `
        -F "title=Upload smoke $Name" `
        -F "description=Multipart validation" `
        -F "mandatory=false" `
        -F "format=$Format" `
        -F "contextType=content_block" `
        -F "contextId=$BlockId" `
        -F "role=$Role" `
        -F "file=@$File;type=$MimeType"

    $body = ""
    if (Test-Path -LiteralPath $ResponsePath) {
        $body = (Get-Content -LiteralPath $ResponsePath -Raw).Trim()
    }

    return [PSCustomObject]@{
        Type = $Name
        Status = $status
        Body = $body
    }
}

$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..\..")).Path
$baseUrl = "http://localhost:$Port/$ContextPath"

if ($Prepare) {
    & (Join-Path $PSScriptRoot "browser-prepare.ps1") -Port $Port -ContextPath $ContextPath -Email $Email -Password $Password -SkipPackage
}

$testRoot = Join-Path $env:TEMP ("gape-upload-test-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$createdContentItems = [System.Collections.Generic.List[object]]::new()
$csrfToken = $null

$cookieJar = Join-Path $testRoot "cookies.txt"
$loginPagePath = Join-Path $testRoot "login.html"
$loginHeadersPath = Join-Path $testRoot "login.headers"
$pagePath = Join-Path $testRoot "class-group.html"
$pdfPath = if ($PdfFile) {
    (Resolve-Path -LiteralPath $PdfFile).Path
} else {
    (Resolve-Path -LiteralPath (Join-Path $workspace "uploads\contents\guide-prj.pdf")).Path
}
$txtPath = New-TestFile -Directory $testRoot -Name "sample.txt" -Content "conteudo textual de teste" -Encoding ([System.Text.Encoding]::UTF8)
$audioPath = (Resolve-Path -LiteralPath (Join-Path $workspace "uploads\contents\audio\review.m4a")).Path
$videoPath = (Resolve-Path -LiteralPath (Join-Path $workspace "uploads\contents\videos\normalization.mp4")).Path
$badPdfPath = New-TestFile -Directory $testRoot -Name "broken.pdf" -Content "not a pdf"
$badImagePath = New-TestFile -Directory $testRoot -Name "broken.png" -Content "not an image"
$imagePath = Find-SampleImage

try {
& curl.exe -s -L -c $cookieJar -b $cookieJar "$baseUrl/login.jsp" -o $loginPagePath | Out-Null
$loginCsrfToken = Get-CsrfTokenFromHtml -Html (Get-Content -LiteralPath $loginPagePath -Raw)

$loginStatus = & curl.exe -s -c $cookieJar -b $cookieJar -D $loginHeadersPath -o $loginPagePath -w "%{http_code}" "$baseUrl/auth/login" `
    --data-urlencode "csrfToken=$loginCsrfToken" `
    --data-urlencode "email=$Email" `
    --data-urlencode "password=$Password"
if ($LASTEXITCODE -ne 0) {
    throw "Login request failed with exit code $LASTEXITCODE."
}
$loginLocation = Select-String -LiteralPath $loginHeadersPath -Pattern '(?i)^Location:\s*(.+?)\s*$' |
    Select-Object -Last 1 |
    ForEach-Object { $_.Matches[0].Groups[1].Value.Trim() }
if ($loginStatus -notin @("302", "303") -or [string]::IsNullOrWhiteSpace($loginLocation) -or $loginLocation -match '(?i)/login\.jsp') {
    throw "Authentication failed for $Email (HTTP $loginStatus)."
}

& curl.exe -s -L -c $cookieJar -b $cookieJar "$baseUrl/learning/class-groups/$ClassGroupId" -o $pagePath | Out-Null

$html = Get-Content -LiteralPath $pagePath -Raw
$csrfMatch = [regex]::Match($html, 'name="csrfToken"\s+value="([^"]+)"')
if (-not $csrfMatch.Success) {
    throw "csrfToken not found in class group page."
}

$blockMatches = [regex]::Matches($html, 'name="contextId"\s+value="(\d+)"')
if ($blockMatches.Count -eq 0) {
    throw "content block contextId not found in class group page."
}

$csrfToken = $csrfMatch.Groups[1].Value
$blockId = $blockMatches[0].Groups[1].Value
$repositoryBlockId = if ($blockMatches.Count -gt 1) { $blockMatches[1].Groups[1].Value } else { $blockId }

$cases = @(
    @{ Name = "pdf"; Format = "pdf"; Role = "support_material"; File = $pdfPath; MimeType = "application/pdf" },
    @{ Name = "text"; Format = "text"; Role = "text"; File = $txtPath; MimeType = "text/plain" },
    @{ Name = "image"; Format = "image"; Role = "image"; File = $imagePath; MimeType = (Get-ImageMimeType $imagePath) },
    @{ Name = "audio"; Format = "audio"; Role = "audio"; File = $audioPath; MimeType = "audio/mp4" },
    @{ Name = "video"; Format = "video"; Role = "video"; File = $videoPath; MimeType = "video/mp4" }
)

$results = foreach ($case in $cases) {
    $responsePath = Join-Path $testRoot ($case.Name + ".response.txt")
    $result = Invoke-Upload `
        -BaseUrl $baseUrl `
        -CookieJar $cookieJar `
        -ResponsePath $responsePath `
        -CsrfToken $csrfToken `
        -BlockId $blockId `
        -Name $case.Name `
        -Format $case.Format `
        -Role $case.Role `
        -File $case.File `
        -MimeType $case.MimeType
    if ($result.Status -eq "201" -and $result.Body -match '^\d+$') {
        $createdContentItems.Add([PSCustomObject]@{ BlockId = $blockId; ContentItemId = [long] $result.Body })
    }
    $result
}

$results | Format-Table -AutoSize

$failed = @($results | Where-Object {
    $_.Status -ne "201" -or $_.Body -match "<!DOCTYPE|<html|HTTP Status 500|Exception|Stacktrace"
})

if ($failed.Count -gt 0) {
    throw "One or more upload smoke tests failed."
}

foreach ($result in $results) {
    $contentItemId = [long] $result.Body
    $processedDirectory = Join-Path $workspace "uploads\contents\items\$contentItemId\processed"
    $originalDirectory = Join-Path $workspace "uploads\contents\items\$contentItemId\original"
    if (-not (Test-Path -LiteralPath $processedDirectory -PathType Container)) {
        throw "Processed directory was not created for $($result.Type): $processedDirectory"
    }
    $storedFiles = @(Get-ChildItem -LiteralPath $processedDirectory -File -Filter "content.*")
    if ($storedFiles.Count -ne 1) {
        throw "Expected one processed content file for $($result.Type), found $($storedFiles.Count)."
    }
    if (Test-Path -LiteralPath $originalDirectory) {
        throw "Original directory must not be created for $($result.Type): $originalDirectory"
    }
}

$repositoryContentItemId = [long] $results[0].Body
$repositoryResponsePath = Join-Path $testRoot "repository.response.txt"
$repositoryStatus = & curl.exe -s -L -b $cookieJar -c $cookieJar -o $repositoryResponsePath -w "%{http_code}" -X POST "$baseUrl/contents/upload" `
    -F "csrfToken=$csrfToken" `
    -F "contextType=content_block" `
    -F "contextId=$repositoryBlockId" `
    -F "role=support_material" `
    -F "mandatory=true" `
    -F "title=Repository reused file smoke" `
    -F "description=New pedagogical item using an existing file" `
    -F "repositoryMetadataMode=custom" `
    -F "repositorySourceContentItemId=$repositoryContentItemId"
$repositoryBody = (Get-Content -LiteralPath $repositoryResponsePath -Raw).Trim()
[PSCustomObject]@{ Type = "repository"; Status = $repositoryStatus; Body = $repositoryBody } | Format-Table -AutoSize
if ($repositoryStatus -ne "201" -or $repositoryBody -match "\D") {
    throw "Repository file reuse failed."
}
$repositoryNewContentItemId = [long] $repositoryBody
$createdContentItems.Add([PSCustomObject]@{ BlockId = $repositoryBlockId; ContentItemId = $repositoryNewContentItemId })
if ($repositoryNewContentItemId -eq $repositoryContentItemId) {
    throw "Repository file reuse must create a new pedagogical item instead of associating item $repositoryContentItemId again."
}
$repositoryProcessedDirectory = Join-Path $workspace "uploads\contents\items\$repositoryContentItemId\processed"
$repositoryStoredFiles = @(Get-ChildItem -LiteralPath $repositoryProcessedDirectory -File -Filter "content.*")
if ($repositoryStoredFiles.Count -ne 1) {
    throw "Repository file reuse must not duplicate files for source item $repositoryContentItemId."
}
$repositoryNewProcessedDirectory = Join-Path $workspace "uploads\contents\items\$repositoryNewContentItemId\processed"
if (Test-Path -LiteralPath $repositoryNewProcessedDirectory) {
    throw "Repository file reuse must not create a processed file directory for new item $repositoryNewContentItemId."
}

$negativeCases = @(
    @{ Name = "invalid-pdf"; Format = "pdf"; Role = "support_material"; File = $badPdfPath; MimeType = "application/pdf" },
    @{ Name = "invalid-image"; Format = "image"; Role = "image"; File = $badImagePath; MimeType = "image/png" }
)

$negativeResults = foreach ($case in $negativeCases) {
    $responsePath = Join-Path $testRoot ($case.Name + ".response.txt")
    Invoke-Upload `
        -BaseUrl $baseUrl `
        -CookieJar $cookieJar `
        -ResponsePath $responsePath `
        -CsrfToken $csrfToken `
        -BlockId $blockId `
        -Name $case.Name `
        -Format $case.Format `
        -Role $case.Role `
        -File $case.File `
        -MimeType $case.MimeType
}

$negativeResults | Format-Table -AutoSize

$unexpectedNegative = @($negativeResults | Where-Object {
    $_.Status -ne "400" -or $_.Body -match "<!DOCTYPE|<html|HTTP Status 500|Exception|Stacktrace"
})

if ($unexpectedNegative.Count -gt 0) {
    throw "One or more invalid upload checks returned an unexpected response."
}
} finally {
    $cleanupFailures = [System.Collections.Generic.List[string]]::new()
    if ($csrfToken -and (Test-Path -LiteralPath $cookieJar)) {
        foreach ($created in @($createdContentItems | Sort-Object ContentItemId -Descending)) {
            $deleteUrl = "$baseUrl/learning/class-groups/$ClassGroupId/blocks/$($created.BlockId)/contents/$($created.ContentItemId)/delete"
            $deleteStatus = & curl.exe -s -b $cookieJar -c $cookieJar -o "NUL" -w "%{http_code}" `
                -X POST $deleteUrl `
                --data-urlencode "csrfToken=$csrfToken"
            if ($deleteStatus -notin @("200", "204", "302", "303")) {
                $cleanupFailures.Add("delete $($created.ContentItemId) returned HTTP $deleteStatus")
            }
        }
        foreach ($created in $createdContentItems) {
            $probeStatus = & curl.exe -s -b $cookieJar -o "NUL" -w "%{http_code}" `
                "$baseUrl/contents/download/$($created.ContentItemId)"
            if ($probeStatus -notin @("400", "404", "410")) {
                $cleanupFailures.Add("content $($created.ContentItemId) is still downloadable (HTTP $probeStatus)")
            }
            $storedDirectory = Join-Path $workspace "uploads\contents\items\$($created.ContentItemId)"
            if (Test-Path -LiteralPath $storedDirectory) {
                $cleanupFailures.Add("stored directory remains for content $($created.ContentItemId): $storedDirectory")
            }
        }
    }
    if (Test-Path -LiteralPath $testRoot) {
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    }
    if ($cleanupFailures.Count -gt 0) {
        throw "Upload smoke cleanup failed: $($cleanupFailures -join '; ')"
    }
}
