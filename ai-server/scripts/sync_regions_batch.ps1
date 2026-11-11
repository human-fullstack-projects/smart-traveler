<#
.SYNOPSIS
Syncs a list of regions (read from a queue file) via Spring's admin API
(/api/admin/tour-data/sync), then loads each into Chroma via sync_mariadb_to_chroma.py.

.DESCRIPTION
Each line of the queue file is one region code (5-digit SIGUNGU code), optionally followed
by a "# comment" for readability. Blank lines and comment-only lines are ignored.

Before syncing a region, this script checks the SHARED MariaDB (not the local Chroma store)
for whether that region already has real place data (address or description present on at
least a handful of rows). This matters when several team members run this script against the
same shared MariaDB with their own separate local ai-server/Chroma: skipping must be based on
shared DB state, not on what happens to be embedded locally, otherwise everyone re-syncs the
same regions. So re-running this script (by you or a teammate) is safe either way.

If the TourAPI daily call quota is hit (or any sync step fails), the script stops where it is;
the next run (e.g. via Windows Task Scheduler, once per day) resumes from the first region in
the queue file that is still not done.

Admin credentials are read from ai-server/.env (ADMIN_LOGIN_ID / ADMIN_LOGIN_PW) -- these are
shared team credentials. Each team member should still use their OWN personal TourAPI key
(env var TOUR_API_KEY, or %USERPROFILE%\.tourapi_key.txt) so everyone's daily quota is separate.

.PARAMETER QueueFile
Path to the region list to process. Defaults to region_queue_member1.txt next to this script.
Give each team member a different queue file (region_queue_member1.txt .. member5.txt).
#>
param(
    [string]$QueueFile = "$PSScriptRoot\region_queue_member1.txt"
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot   # ai-server folder
$Py = Join-Path $Root ".venv\Scripts\python.exe"
$LogFile = Join-Path $PSScriptRoot "region_sync.log"
Set-Location $Root

function Write-Log([string]$Message) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
    Add-Content -Path $LogFile -Value $line -Encoding utf8
    Write-Host $line
}

function Get-EnvVars([string]$Path) {
    $vars = @{}
    Get-Content $Path -Encoding UTF8 | ForEach-Object {
        if ($_ -match '^\s*([A-Z_]+)=(.*)$') { $vars[$matches[1]] = $matches[2] }
    }
    return $vars
}

function Get-QueueCodes([string]$Path) {
    # -Encoding UTF8 matters here: without it, Windows PowerShell 5.1 reads the file using the
    # system codepage, which can misdecode the UTF-8 Korean comments badly enough to swallow
    # newlines and merge lines together (silently dropping most of the queue).
    $codes = @()
    Get-Content $Path -Encoding UTF8 | ForEach-Object {
        $line = ($_ -split '#')[0].Trim()
        if ($line -match '^[0-9]{5}$') { $codes += $line }
    }
    return $codes
}

# Real completion check against the SHARED MariaDB (not local Chroma - see header comment).
function Get-RealPlaceCount([string]$Code) {
    $script = @"
from dotenv import load_dotenv
load_dotenv('.env')
from app import db
rows = db.read_places('$Code')
print(sum(1 for r in rows if r['address'] or r['description']))
"@
    # Do not redirect native stderr here (e.g. "2>$null") - with $ErrorActionPreference='Stop'
    # that turns any stderr line (even a harmless warning) into a terminating NativeCommandError.
    $out = & $Py -c $script
    return [int]($out | Select-Object -Last 1)
}

if (-not (Test-Path $QueueFile)) {
    Write-Log "Queue file not found: $QueueFile - aborting"
    exit 1
}

$envVars = Get-EnvVars (Join-Path $Root ".env")
$Base = $envVars['SPRING_BASE_URL']
$LoginId = $envVars['ADMIN_LOGIN_ID']
$LoginPw = $envVars['ADMIN_LOGIN_PW']
if (-not $Base) { $Base = "http://127.0.0.1:8080" }
if (-not $LoginId -or -not $LoginPw) {
    Write-Log "ADMIN_LOGIN_ID/ADMIN_LOGIN_PW missing in .env - aborting"
    exit 1
}

$Codes = Get-QueueCodes $QueueFile
Write-Log "=== batch start (queue: $QueueFile, $($Codes.Count) region(s)) ==="

# Real completion threshold: a genuine regional sync yields far more than the handful of
# leftover "hub ranking" rows (typically 3-16) that already exist for most regions.
$DoneThreshold = 20

foreach ($code in $Codes) {
    $realCount = Get-RealPlaceCount $code
    if ($realCount -ge $DoneThreshold) {
        Write-Log "$code already has $realCount usable place(s) in MariaDB - skipping (done by someone already)"
        continue
    }

    Write-Log "$code sync starting (currently $realCount usable place(s) in MariaDB)"

    try {
        $csrf1 = Invoke-RestMethod -Uri "$Base/api/auth/csrf" -Method Get -SessionVariable sess
        $escId = [Uri]::EscapeDataString($LoginId)
        $escPw = [Uri]::EscapeDataString($LoginPw)
        $loginBody = "loginId=$escId" + "&password=$escPw"
        Invoke-RestMethod -Uri "$Base/api/auth/login" -Method Post -WebSession $sess `
            -Headers @{ $csrf1.headerName = $csrf1.token } `
            -ContentType "application/x-www-form-urlencoded" -Body $loginBody | Out-Null

        $csrf2 = Invoke-RestMethod -Uri "$Base/api/auth/csrf" -Method Get -WebSession $sess

        $syncBody = (@{ region = $code } | ConvertTo-Json)
        $result = Invoke-RestMethod -Uri "$Base/api/admin/tour-data/sync" -Method Post -WebSession $sess `
            -Headers @{ $csrf2.headerName = $csrf2.token } `
            -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($syncBody))
    }
    catch {
        Write-Log "$code request failed: $($_.Exception.Message)"
        Write-Log "=== batch stopped (will retry this region on next run) ==="
        exit 0
    }

    $failedSteps = @($result.steps | Where-Object { $_.status -ne "SUCCESS" })
    if ($failedSteps.Count -gt 0) {
        foreach ($s in $failedSteps) {
            Write-Log "$code step failed: provider=$($s.provider) dataType=$($s.dataType) message=$($s.message)"
        }
        Write-Log "$code`: $($failedSteps.Count) of $($result.steps.Count) step(s) failed (often the daily TourAPI call quota) - stopping here, next run resumes this region"
        Write-Log "=== batch stopped (sync incomplete) ==="
        exit 0
    }

    Write-Log "$code sync done: all $($result.steps.Count) step(s) succeeded"
    Write-Log "$code loading into local Chroma"
    & $Py -m scripts.sync_mariadb_to_chroma $code 2>&1 | ForEach-Object { Write-Log "  $_" }
    Write-Log "$code Chroma load done"
}

Write-Log "=== batch end (all regions in $QueueFile processed, nothing left to do) ==="
