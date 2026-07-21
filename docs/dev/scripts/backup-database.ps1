[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $OutputDirectory,

    [ValidateRange(7, 3650)]
    [int] $RetentionDays = 14,

    [string] $MySqlDumpPath = $env:GAPE_MYSQLDUMP_PATH,
    [string] $Database = $env:GAPE_BACKUP_DB_NAME,
    [string] $DatabaseHost = $env:GAPE_BACKUP_DB_HOST,
    [int] $DatabasePort = $(if ($env:GAPE_BACKUP_DB_PORT) { [int] $env:GAPE_BACKUP_DB_PORT } else { 3306 }),
    [string] $DatabaseUser = $env:GAPE_BACKUP_DB_USER,
    [switch] $ValidateOnly
)

$ErrorActionPreference = "Stop"

function Require-Value {
    param([string] $Value, [string] $Label)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Label is required. Configure it outside the repository."
    }
    return $Value.Trim()
}

function Assert-SafeDatabaseIdentifier {
    param([string] $Value, [string] $Label)

    if ($Value -notmatch '^[A-Za-z0-9_]+$') {
        throw "$Label contains unsupported characters."
    }
}

function Resolve-MySqlDump {
    param([string] $ConfiguredPath)

    if (-not [string]::IsNullOrWhiteSpace($ConfiguredPath)) {
        $resolved = Resolve-Path -LiteralPath $ConfiguredPath -ErrorAction Stop
        if (-not (Test-Path -LiteralPath $resolved.Path -PathType Leaf)) {
            throw "mysqldump was not found at $ConfiguredPath"
        }
        return $resolved.Path
    }

    $command = Get-Command mysqldump.exe -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        $command = Get-Command mysqldump -ErrorAction SilentlyContinue
    }
    if ($null -eq $command) {
        throw "mysqldump was not found. Set GAPE_MYSQLDUMP_PATH to its executable path."
    }
    return $command.Source
}

function Restore-EnvironmentValue {
    param([string] $Name, [string] $PreviousValue, [bool] $WasPresent)

    if ($WasPresent) {
        Set-Item -LiteralPath "Env:$Name" -Value $PreviousValue
    } else {
        Remove-Item -LiteralPath "Env:$Name" -ErrorAction SilentlyContinue
    }
}

function Test-BackupChecksum {
    param([System.IO.FileInfo] $Backup)

    $checksumPath = $Backup.FullName + ".sha256"
    if (-not (Test-Path -LiteralPath $checksumPath -PathType Leaf)) {
        return $false
    }
    $checksumLine = Get-Content -LiteralPath $checksumPath -Raw
    $match = [regex]::Match($checksumLine, '(?i)^\s*([0-9a-f]{64})\s{2,}[^\r\n]+\s*$')
    if (-not $match.Success) {
        return $false
    }
    $actual = (Get-FileHash -LiteralPath $Backup.FullName -Algorithm SHA256).Hash
    return $actual.Equals($match.Groups[1].Value, [System.StringComparison]::OrdinalIgnoreCase)
}

$Database = Require-Value -Value $Database -Label "GAPE_BACKUP_DB_NAME"
$DatabaseUser = Require-Value -Value $DatabaseUser -Label "GAPE_BACKUP_DB_USER"
$DatabasePassword = Require-Value -Value $env:GAPE_BACKUP_DB_PASSWORD -Label "GAPE_BACKUP_DB_PASSWORD"
if ([string]::IsNullOrWhiteSpace($DatabaseHost)) {
    $DatabaseHost = "localhost"
} else {
    $DatabaseHost = $DatabaseHost.Trim()
}
Assert-SafeDatabaseIdentifier -Value $Database -Label "Database name"
if ($DatabaseHost -notmatch '^[A-Za-z0-9.-]+$') {
    throw "Database host contains unsupported characters."
}
if ($DatabaseUser -notmatch '^[A-Za-z0-9._-]+$') {
    throw "Database user contains unsupported characters."
}
if ($DatabasePort -lt 1 -or $DatabasePort -gt 65535) {
    throw "Database port must be between 1 and 65535."
}

$DumpExecutable = Resolve-MySqlDump -ConfiguredPath $MySqlDumpPath
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)

if ($ValidateOnly) {
    Write-Output "Backup configuration is valid. mysqldump=$DumpExecutable; destination=$OutputDirectory; retentionDays=$RetentionDays"
    exit 0
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupFileName = "$Database-$timestamp.sql"
$backupPath = Join-Path $OutputDirectory $backupFileName
$partialPath = "$backupPath.partial"
$stderrPath = "$backupPath.stderr"
$checksumPath = "$backupPath.sha256"

if (Test-Path -LiteralPath $partialPath) {
    Remove-Item -LiteralPath $partialPath -Force
}
if (Test-Path -LiteralPath $stderrPath) {
    Remove-Item -LiteralPath $stderrPath -Force
}

$wasPasswordPresent = Test-Path -LiteralPath "Env:MYSQL_PWD"
$previousPassword = if ($wasPasswordPresent) { $env:MYSQL_PWD } else { $null }
try {
    # MYSQL_PWD is scoped to the child process invocation and avoids exposing a
    # password in the process command line or Task Scheduler arguments.
    $env:MYSQL_PWD = $DatabasePassword
    $arguments = @(
        "--host=$DatabaseHost",
        "--port=$DatabasePort",
        "--user=$DatabaseUser",
        "--single-transaction",
        "--routines",
        "--events",
        "--triggers",
        "--hex-blob",
        "--no-tablespaces",
        "--set-gtid-purged=OFF",
        "--default-character-set=utf8mb4",
        $Database
    )
    & $DumpExecutable @arguments 1> $partialPath 2> $stderrPath
    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump failed with exit code $LASTEXITCODE. Inspect $stderrPath with an authorized operator."
    }
} finally {
    Restore-EnvironmentValue -Name "MYSQL_PWD" -PreviousValue $previousPassword -WasPresent $wasPasswordPresent
}

if ((-not (Test-Path -LiteralPath $partialPath -PathType Leaf)) -or ((Get-Item -LiteralPath $partialPath).Length -le 0)) {
    throw "mysqldump did not produce a usable backup file."
}

Move-Item -LiteralPath $partialPath -Destination $backupPath -Force
Remove-Item -LiteralPath $stderrPath -Force -ErrorAction SilentlyContinue
$hash = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash.ToLowerInvariant()
[System.IO.File]::WriteAllText($checksumPath, "$hash  $backupFileName`n", [System.Text.Encoding]::ASCII)
if (-not (Test-BackupChecksum -Backup (Get-Item -LiteralPath $backupPath))) {
    throw "The generated backup checksum could not be verified."
}

# Remove only backups past the configured age and never remove the seven most
# recent complete backups, even if a scheduler outage caused irregular runs.
$completeBackups = Get-ChildItem -LiteralPath $OutputDirectory -File -Filter "$Database-*.sql" |
    Where-Object { Test-BackupChecksum -Backup $_ } |
    Sort-Object LastWriteTimeUtc -Descending
$protectedNames = @($completeBackups | Select-Object -First 7 | ForEach-Object Name)
$cutoff = (Get-Date).ToUniversalTime().AddDays(-$RetentionDays)
$completeBackups |
    Where-Object { $_.LastWriteTimeUtc -lt $cutoff -and $_.Name -notin $protectedNames } |
    ForEach-Object {
        Remove-Item -LiteralPath $_.FullName -Force
        Remove-Item -LiteralPath ($_.FullName + ".sha256") -Force -ErrorAction SilentlyContinue
    }

Write-Output "Backup completed: $backupPath (SHA-256 verified; retention >= 7 complete backups)."
