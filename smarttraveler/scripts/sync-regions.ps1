<#
.SYNOPSIS
TourAPI에서 아직 받아오지 않은 지역(시군구)을 순서대로 동기화한다.

.DESCRIPTION
`region` 테이블에서 is_supported=TRUE인 시군구 중 place 테이블에 실제로 쌓인 장소 수가
DoneThreshold 미만인 지역만 골라, region_code 순서대로 하나씩 Spring 관리자 동기화
(POST /admin/tour-data/sync)를 호출한다.

완료 여부는 매 실행마다 MariaDB를 직접 조회해서 판단한다(로컬 상태 파일 없음). 그래서
- 중간에 멈춰도 다음 실행이 그 지역부터 이어서 처리하고,
- 여러 사람이 같은 공유 DB에 대해 각자 실행해도 서로 중복 동기화하지 않는다.

TourAPI 일일 호출 한도 등으로 한 단계라도 실패하면 그 지역에서 멈추고 스크립트를
종료한다(다음 실행에서 같은 지역부터 재시도). 정상 흐름에서는 모든 대상 지역을 처리할
때까지 계속한다.

.PARAMETER BaseUrl
Spring 서버 주소. 기본값 http://127.0.0.1:8080

.PARAMETER DoneThreshold
이 값 이상의 place 행이 있으면 "이미 동기화됨"으로 보고 건너뛴다. 기본 20
(관광지 연관 순위만 들어간 지역은 보통 20개 미만이라 실제 동기화와 구분된다).

.PARAMETER DelaySeconds
지역 간 호출 사이에 쉬는 시간(초). TourAPI에 과도한 연속 호출을 피하기 위함. 기본 3.

.PARAMETER MaxRegions
이번 실행에서 처리할 최대 지역 수. 0이면 제한 없음(대상 전체 처리). 기본 0.

.EXAMPLE
.\sync-regions.ps1
.\sync-regions.ps1 -MaxRegions 5
#>
param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [int]$DoneThreshold = 20,
    [int]$DelaySeconds = 3,
    [int]$MaxRegions = 0
)

$ErrorActionPreference = 'Stop'

$AdminLoginId = if ($env:ADMIN_LOGIN_ID) { $env:ADMIN_LOGIN_ID } else { "admin" }
$AdminLoginPw = if ($env:ADMIN_LOGIN_PW) { $env:ADMIN_LOGIN_PW } else { "123456" }
$DbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }
$DbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$DbName = if ($env:DB_NAME) { $env:DB_NAME } else { "smart_travel_db" }
$DbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "sixon" }
$DbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$LogFile = Join-Path $PSScriptRoot "region_sync.log"

function Write-Log([string]$Message) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
    Add-Content -Path $LogFile -Value $line -Encoding utf8
    Write-Host $line
}

function Invoke-Sql([string]$Query) {
    $args = @("-h", $DbHost, "-P", $DbPort, "-u", $DbUser, "-p$DbPassword", $DbName, "-N", "-e", $Query)
    $out = & mysql.exe @args 2>&1
    if ($LASTEXITCODE -ne 0) { throw "MariaDB 조회 실패: $out" }
    return $out
}

function Get-PendingRegionCodes {
    $query = @"
SELECT r.region_code FROM region r LEFT JOIN place p ON p.region_id = r.region_id
WHERE r.region_level='SIGUNGU' AND r.is_supported=TRUE
GROUP BY r.region_id HAVING COUNT(p.place_id) < $DoneThreshold
ORDER BY r.region_code;
"@
    return @(Invoke-Sql $query | Where-Object { $_ -match '^[0-9]{5}$' })
}

function Get-SyncLogStatus([string]$RegionCode, [datetime]$Since) {
    $sinceStr = $Since.ToString("yyyy-MM-dd HH:mm:ss")
    $query = @"
SELECT provider, data_type, status, error_message FROM data_sync_log
WHERE region_id = (SELECT region_id FROM region WHERE region_code='$RegionCode')
  AND started_at >= '$sinceStr';
"@
    return @(Invoke-Sql $query)
}

function New-Session {
    $csrf = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/api/auth/csrf" -Method Get -SessionVariable sess
    $token = ($csrf.Content | ConvertFrom-Json)
    Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/api/auth/login" -Method Post -WebSession $sess `
        -Headers @{ $token.headerName = $token.token } `
        -Body @{ loginId = $AdminLoginId; password = $AdminLoginPw } | Out-Null
    return $sess
}

function Sync-Region([Microsoft.PowerShell.Commands.WebRequestSession]$Session, [string]$RegionCode) {
    $csrf = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/api/auth/csrf" -Method Get -WebSession $Session
    $token = ($csrf.Content | ConvertFrom-Json)
    Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/admin/tour-data/sync" -Method Post -WebSession $Session `
        -Headers @{ $token.headerName = $token.token } `
        -Body @{ region = $RegionCode } | Out-Null
}

try {
    Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/api/auth/csrf" -Method Get | Out-Null
} catch {
    Write-Log "Spring 서버($BaseUrl)가 응답하지 않습니다. 먼저 서버를 실행하세요. ($($_.Exception.Message))"
    exit 1
}

$codes = Get-PendingRegionCodes
if ($MaxRegions -gt 0 -and $codes.Count -gt $MaxRegions) { $codes = $codes[0..($MaxRegions - 1)] }

Write-Log "=== 배치 시작: 대상 $($codes.Count)개 지역 (threshold=$DoneThreshold) ==="

if ($codes.Count -eq 0) {
    Write-Log "=== 처리할 지역이 없습니다. 모두 동기화되었습니다. ==="
    exit 0
}

$session = New-Session
$processed = 0

foreach ($code in $codes) {
    Write-Log "$code 동기화 시작"
    $startedAt = Get-Date

    try {
        Sync-Region -Session $session -RegionCode $code
    } catch {
        Write-Log "$code 요청 실패: $($_.Exception.Message)"
        Write-Log "=== 배치 중단 (다음 실행에서 $code 부터 재시도) ==="
        exit 0
    }

    Start-Sleep -Milliseconds 500
    $rows = Get-SyncLogStatus -RegionCode $code -Since $startedAt
    $failed = @($rows | Where-Object { $_ -notmatch "SUCCESS" -and $_.Trim() -ne "" })

    if ($rows.Count -eq 0) {
        Write-Log "$code 결과를 확인할 수 없습니다(동기화 로그 없음) - 중단"
        exit 0
    }

    if ($failed.Count -gt 0) {
        foreach ($line in $failed) { Write-Log "  미반영: $line" }
        Write-Log "$code 일부 단계 미반영 (TourAPI 일일 한도 등) - 배치 중단, 다음 실행에서 $code 부터 재시도"
        exit 0
    }

    Write-Log "$code 동기화 완료 ($($rows.Count)개 단계 모두 SUCCESS)"
    $processed++

    if ($code -ne $codes[-1]) { Start-Sleep -Seconds $DelaySeconds }
}

Write-Log "=== 배치 종료: $processed 개 지역 처리 완료 (대상 전부 처리) ==="
