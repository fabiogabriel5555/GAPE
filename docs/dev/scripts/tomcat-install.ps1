param(
    [string] $Version = "10.1.24",
    [string] $InstallRoot
)

$ErrorActionPreference = "Stop"

function Assert-WorkspacePath {
    param([string] $Workspace, [string] $Path, [string] $Label)

    $workspacePath = [System.IO.Path]::GetFullPath($Workspace).TrimEnd('\', '/')
    $candidatePath = [System.IO.Path]::GetFullPath($Path)
    if (-not $candidatePath.StartsWith($workspacePath + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "$Label must stay inside the workspace: $candidatePath"
    }
}

$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..\..")).Path
if (-not $InstallRoot) {
    $InstallRoot = Join-Path $workspace "docs\dev\.tools"
} elseif (-not [System.IO.Path]::IsPathRooted($InstallRoot)) {
    $InstallRoot = Join-Path $workspace $InstallRoot
}

$installRootPath = [System.IO.Path]::GetFullPath($InstallRoot)
$tomcatHome = Join-Path $installRootPath "apache-tomcat-$Version"
$downloads = Join-Path $installRootPath "downloads"
$staging = Join-Path $installRootPath ".tomcat-$Version-staging"
$archive = Join-Path $downloads "apache-tomcat-$Version-windows-x64.zip"
$checksumFile = "$archive.sha512"

foreach ($path in @($installRootPath, $tomcatHome, $downloads, $staging, $archive, $checksumFile)) {
    Assert-WorkspacePath -Workspace $workspace -Path $path -Label "Tomcat installation path"
}

if (Test-Path -LiteralPath (Join-Path $tomcatHome "bin\catalina.bat") -PathType Leaf) {
    Write-Output $tomcatHome
    exit 0
}

New-Item -ItemType Directory -Path $downloads -Force | Out-Null
$baseUrl = "https://archive.apache.org/dist/tomcat/tomcat-10/v$Version/bin"
$archiveUrl = "$baseUrl/apache-tomcat-$Version-windows-x64.zip"
$checksumUrl = "$archiveUrl.sha512"

Write-Output "Downloading Apache Tomcat $Version..."
Invoke-WebRequest -Uri $archiveUrl -OutFile $archive -UseBasicParsing
Invoke-WebRequest -Uri $checksumUrl -OutFile $checksumFile -UseBasicParsing

$expectedHashMatch = [regex]::Match((Get-Content -LiteralPath $checksumFile -Raw), '(?i)\b[0-9a-f]{128}\b')
if (-not $expectedHashMatch.Success) {
    throw "The Tomcat SHA-512 file did not contain a valid checksum."
}
$expectedHash = $expectedHashMatch.Value.ToUpperInvariant()
$actualHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash.ToUpperInvariant()
if ($actualHash -ne $expectedHash) {
    throw "Tomcat checksum mismatch. Expected $expectedHash, got $actualHash."
}

if (Test-Path -LiteralPath $staging) {
    Remove-Item -LiteralPath $staging -Recurse -Force
}
New-Item -ItemType Directory -Path $staging -Force | Out-Null
Expand-Archive -LiteralPath $archive -DestinationPath $staging -Force

$expandedHome = Join-Path $staging "apache-tomcat-$Version"
if (-not (Test-Path -LiteralPath (Join-Path $expandedHome "bin\catalina.bat") -PathType Leaf)) {
    throw "The downloaded archive does not contain the expected Tomcat layout."
}
if (Test-Path -LiteralPath $tomcatHome) {
    Remove-Item -LiteralPath $tomcatHome -Recurse -Force
}
Move-Item -LiteralPath $expandedHome -Destination $tomcatHome
Remove-Item -LiteralPath $staging -Recurse -Force
Remove-Item -LiteralPath $archive, $checksumFile -Force

Write-Output $tomcatHome
