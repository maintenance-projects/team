# GitNX Credential Helper
# 설치: git config --global credential.helper "!'C:/path/to/gitnx-credential-helper.ps1'"
# 특정 호스트만: git config --global credential.http://125.131.105.208:8082.helper "!'C:/path/to/gitnx-credential-helper.ps1'"

param()

# git이 보내는 입력 읽기
$inputData = @{}
while ($true) {
    $line = [Console]::ReadLine()
    if ([string]::IsNullOrEmpty($line)) { break }
    $parts = $line -split "=", 2
    if ($parts.Length -eq 2) {
        $inputData[$parts[0]] = $parts[1]
    }
}

# get 요청만 처리 (store, erase는 무시)
$operation = $args[0]
if ($operation -ne "get") { exit 0 }

$protocol = $inputData["protocol"]
$hostPort = $inputData["host"]
$baseUrl = "${protocol}://${hostPort}"

try {
    # 1. 인증 세션 생성
    $response = Invoke-RestMethod -Uri "$baseUrl/api/git/auth/start" -Method POST -ContentType "application/json"
    $sessionId = $response.sessionId

    # 2. 브라우저 열기
    Write-Host "" -NoNewline
    Write-Host "GitNX: 브라우저에서 로그인해주세요..." -ForegroundColor Cyan
    Start-Process "$baseUrl/git-auth/$sessionId"

    # 3. 인증 완료 대기 (최대 2분)
    $maxWait = 120
    $elapsed = 0
    while ($elapsed -lt $maxWait) {
        Start-Sleep -Seconds 2
        $elapsed += 2

        try {
            $poll = Invoke-RestMethod -Uri "$baseUrl/api/git/auth/poll/$sessionId" -Method GET
            if ($poll.status -eq "complete") {
                Write-Host "GitNX: 인증 완료!" -ForegroundColor Green
                # git credential 형식으로 출력
                Write-Output "username=$($poll.username)"
                Write-Output "password=gitnx-oauth-authenticated"
                exit 0
            }
        } catch {
            # 폴링 실패 시 계속 시도
        }
    }

    Write-Host "GitNX: 인증 시간이 초과되었습니다." -ForegroundColor Red
    exit 1
} catch {
    Write-Host "GitNX: 인증 서버에 연결할 수 없습니다: $_" -ForegroundColor Red
    exit 1
}