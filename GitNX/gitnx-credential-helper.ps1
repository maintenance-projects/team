# GitNX Credential Helper - Custom Login Window
# GCM 대신 GitNX 전용 로그인 창을 띄워서 인증합니다.

param()

$operation = $args[0]
if ($operation -ne "get") { exit 0 }

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

$protocol = $inputData["protocol"]
$hostPort = $inputData["host"]
$baseUrl = "${protocol}://${hostPort}"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# ── 로그인 창 생성 ──
$form = New-Object System.Windows.Forms.Form
$form.Text = "GitNX Login"
$form.Size = New-Object System.Drawing.Size(360, 320)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = "FixedDialog"
$form.MaximizeBox = $false
$form.MinimizeBox = $false
$form.TopMost = $true
$form.BackColor = [System.Drawing.Color]::FromArgb(30, 30, 30)
$form.ForeColor = [System.Drawing.Color]::White

# 제목
$titleLabel = New-Object System.Windows.Forms.Label
$titleLabel.Text = "GitNX Login"
$titleLabel.Font = New-Object System.Drawing.Font("Segoe UI", 14, [System.Drawing.FontStyle]::Bold)
$titleLabel.AutoSize = $true
$titleLabel.Location = New-Object System.Drawing.Point(120, 15)
$form.Controls.Add($titleLabel)

# Username
$usernameLabel = New-Object System.Windows.Forms.Label
$usernameLabel.Text = "Username or Email"
$usernameLabel.Font = New-Object System.Drawing.Font("Segoe UI", 9)
$usernameLabel.Location = New-Object System.Drawing.Point(30, 55)
$usernameLabel.AutoSize = $true
$form.Controls.Add($usernameLabel)

$usernameBox = New-Object System.Windows.Forms.TextBox
$usernameBox.Location = New-Object System.Drawing.Point(30, 75)
$usernameBox.Size = New-Object System.Drawing.Size(285, 25)
$usernameBox.Font = New-Object System.Drawing.Font("Segoe UI", 10)
$usernameBox.BackColor = [System.Drawing.Color]::FromArgb(50, 50, 50)
$usernameBox.ForeColor = [System.Drawing.Color]::White
$usernameBox.BorderStyle = "FixedSingle"
$form.Controls.Add($usernameBox)

# Password
$passwordLabel = New-Object System.Windows.Forms.Label
$passwordLabel.Text = "Password"
$passwordLabel.Font = New-Object System.Drawing.Font("Segoe UI", 9)
$passwordLabel.Location = New-Object System.Drawing.Point(30, 110)
$passwordLabel.AutoSize = $true
$form.Controls.Add($passwordLabel)

$passwordBox = New-Object System.Windows.Forms.TextBox
$passwordBox.Location = New-Object System.Drawing.Point(30, 130)
$passwordBox.Size = New-Object System.Drawing.Size(285, 25)
$passwordBox.Font = New-Object System.Drawing.Font("Segoe UI", 10)
$passwordBox.UseSystemPasswordChar = $true
$passwordBox.BackColor = [System.Drawing.Color]::FromArgb(50, 50, 50)
$passwordBox.ForeColor = [System.Drawing.Color]::White
$passwordBox.BorderStyle = "FixedSingle"
$form.Controls.Add($passwordBox)

# 에러 메시지
$errorLabel = New-Object System.Windows.Forms.Label
$errorLabel.Text = ""
$errorLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 100, 100)
$errorLabel.Font = New-Object System.Drawing.Font("Segoe UI", 8)
$errorLabel.Location = New-Object System.Drawing.Point(30, 160)
$errorLabel.Size = New-Object System.Drawing.Size(285, 18)
$form.Controls.Add($errorLabel)

# Login 버튼
$loginButton = New-Object System.Windows.Forms.Button
$loginButton.Text = "Login"
$loginButton.Location = New-Object System.Drawing.Point(30, 182)
$loginButton.Size = New-Object System.Drawing.Size(285, 35)
$loginButton.Font = New-Object System.Drawing.Font("Segoe UI", 10, [System.Drawing.FontStyle]::Bold)
$loginButton.BackColor = [System.Drawing.Color]::FromArgb(25, 135, 84)
$loginButton.ForeColor = [System.Drawing.Color]::White
$loginButton.FlatStyle = "Flat"
$loginButton.Cursor = [System.Windows.Forms.Cursors]::Hand
$form.Controls.Add($loginButton)

# 구분선 라벨
$orLabel = New-Object System.Windows.Forms.Label
$orLabel.Text = "───────────  OR  ───────────"
$orLabel.Font = New-Object System.Drawing.Font("Segoe UI", 8)
$orLabel.ForeColor = [System.Drawing.Color]::Gray
$orLabel.Location = New-Object System.Drawing.Point(42, 225)
$orLabel.AutoSize = $true
$form.Controls.Add($orLabel)

# GitHub OAuth 버튼
$githubButton = New-Object System.Windows.Forms.Button
$githubButton.Text = "  Sign in with GitHub"
$githubButton.Location = New-Object System.Drawing.Point(30, 248)
$githubButton.Size = New-Object System.Drawing.Size(285, 35)
$githubButton.Font = New-Object System.Drawing.Font("Segoe UI", 10)
$githubButton.BackColor = [System.Drawing.Color]::FromArgb(45, 45, 45)
$githubButton.ForeColor = [System.Drawing.Color]::White
$githubButton.FlatStyle = "Flat"
$githubButton.Cursor = [System.Windows.Forms.Cursors]::Hand
$form.Controls.Add($githubButton)

# ── 결과 저장용 변수 ──
$script:authResult = $null

# ── Login 버튼 클릭 ──
$loginButton.Add_Click({
    $username = $usernameBox.Text.Trim()
    $password = $passwordBox.Text

    if ([string]::IsNullOrEmpty($username) -or [string]::IsNullOrEmpty($password)) {
        $errorLabel.Text = "Username and password are required."
        return
    }

    $errorLabel.Text = ""
    $loginButton.Enabled = $false
    $loginButton.Text = "Logging in..."

    try {
        # GitNX 서버에 인증 요청
        $body = @{ username = $username; password = $password } | ConvertTo-Json
        $response = Invoke-RestMethod -Uri "$baseUrl/api/git/auth/verify" -Method POST -Body $body -ContentType "application/json"

        if ($response.status -eq "ok") {
            $script:authResult = @{ username = $response.username; password = $password }
            $form.Close()
        } else {
            $errorLabel.Text = "Invalid username or password."
            $loginButton.Enabled = $true
            $loginButton.Text = "Login"
        }
    } catch {
        $errorLabel.Text = "Invalid username or password."
        $loginButton.Enabled = $true
        $loginButton.Text = "Login"
    }
})

# ── GitHub OAuth 버튼 클릭 ──
$githubButton.Add_Click({
    $githubButton.Enabled = $false
    $githubButton.Text = "  Waiting for browser..."

    try {
        # 인증 세션 생성
        $response = Invoke-RestMethod -Uri "$baseUrl/api/git/auth/start" -Method POST -ContentType "application/json"
        $sessionId = $response.sessionId

        # 브라우저에서 GitHub OAuth 페이지 열기
        Start-Process "$baseUrl/git-auth/$sessionId"

        # 백그라운드에서 폴링
        $timer = New-Object System.Windows.Forms.Timer
        $timer.Interval = 2000
        $script:pollCount = 0

        $timer.Add_Tick({
            $script:pollCount++
            if ($script:pollCount -gt 60) {  # 2분 타임아웃
                $timer.Stop()
                $errorLabel.Text = "Authentication timed out."
                $githubButton.Enabled = $true
                $githubButton.Text = "  Sign in with GitHub"
                return
            }

            try {
                $poll = Invoke-RestMethod -Uri "$baseUrl/api/git/auth/poll/$sessionId" -Method GET
                if ($poll.status -eq "complete") {
                    $timer.Stop()
                    $script:authResult = @{ username = $poll.username; password = "gitnx-oauth-authenticated" }
                    $form.Close()
                }
            } catch {}
        })

        $timer.Start()
    } catch {
        $errorLabel.Text = "Cannot connect to GitNX server."
        $githubButton.Enabled = $true
        $githubButton.Text = "  Sign in with GitHub"
    }
})

# Enter 키로 로그인
$form.AcceptButton = $loginButton

# 창 표시
[void]$form.ShowDialog()

# ── 결과 출력 ──
if ($null -ne $script:authResult) {
    Write-Output "username=$($script:authResult.username)"
    Write-Output "password=$($script:authResult.password)"
    exit 0
} else {
    exit 1
}