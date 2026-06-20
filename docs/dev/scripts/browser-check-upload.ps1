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
        [string] $File
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
        -F "file=@$File;type=application/octet-stream"

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

$cookieJar = Join-Path $testRoot "cookies.txt"
$pagePath = Join-Path $testRoot "class-group.html"
$pdfPath = if ($PdfFile) {
    (Resolve-Path -LiteralPath $PdfFile).Path
} else {
    New-TestFile -Directory $testRoot -Name "sample.pdf" -Content "%PDF-1.4`n1 0 obj`n<<>>`nendobj`n%%EOF`n"
}
$txtPath = New-TestFile -Directory $testRoot -Name "sample.txt" -Content "conteudo textual de teste" -Encoding ([System.Text.Encoding]::UTF8)
$mp3Path = New-TestFile -Directory $testRoot -Name "sample.mp3" -Content "audio placeholder"
$mp4Path = New-TestFile -Directory $testRoot -Name "sample.mp4" -Content "video placeholder"
$badPdfPath = New-TestFile -Directory $testRoot -Name "broken.pdf" -Content "not a pdf"
$badImagePath = New-TestFile -Directory $testRoot -Name "broken.png" -Content "not an image"
$imagePath = Find-SampleImage

& curl.exe -s -L -c $cookieJar -b $cookieJar -X POST "$baseUrl/auth/login" `
    --data-urlencode "email=$Email" `
    --data-urlencode "password=$Password" | Out-Null

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
    @{ Name = "pdf"; Format = "pdf"; Role = "support_material"; File = $pdfPath },
    @{ Name = "text"; Format = "text"; Role = "text"; File = $txtPath },
    @{ Name = "image"; Format = "image"; Role = "image"; File = $imagePath },
    @{ Name = "audio"; Format = "audio"; Role = "audio"; File = $mp3Path },
    @{ Name = "video"; Format = "video"; Role = "video"; File = $mp4Path }
)

$results = foreach ($case in $cases) {
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
        -File $case.File
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
    @{ Name = "invalid-pdf"; Format = "pdf"; Role = "support_material"; File = $badPdfPath },
    @{ Name = "invalid-image"; Format = "image"; Role = "image"; File = $badImagePath }
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
        -File $case.File
}

$negativeResults | Format-Table -AutoSize

$unexpectedNegative = @($negativeResults | Where-Object {
    $_.Status -ne "400" -or $_.Body -match "<!DOCTYPE|<html|HTTP Status 500|Exception|Stacktrace"
})

if ($unexpectedNegative.Count -gt 0) {
    throw "One or more invalid upload checks returned an unexpected response."
}
