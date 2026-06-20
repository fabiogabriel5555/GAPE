param(
    [int] $Port = 18080,
    [string] $ContextPath = "GAPE",
    [long] $ClassGroupId = 53,
    [string] $ContentType = "Video",
    [string] $Email = "admin@gape.local",
    [string] $Password = "Password#2026",
    [string] $Out = "target\browser-screenshots\add-content-modal-fast.png",
    [int] $Width = 1920,
    [int] $Height = 1200,
    [int] $TimeoutSeconds = 90,
    [string] $BrowserPath,
    [switch] $Prepare,
    [switch] $NoScreenshot
)

$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "browser-check-flow.ps1") `
    -Port $Port `
    -ContextPath $ContextPath `
    -Route "/learning/class-groups/$ClassGroupId" `
    -Preset AddContent `
    -PresetValue $ContentType `
    -Email $Email `
    -Password $Password `
    -Out $Out `
    -Width $Width `
    -Height $Height `
    -TimeoutSeconds $TimeoutSeconds `
    -BrowserPath $BrowserPath `
    -Prepare:$Prepare `
    -NoScreenshot:$NoScreenshot
