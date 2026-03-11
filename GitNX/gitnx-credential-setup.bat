@echo off
chcp 65001 >nul
echo ============================================
echo   GitNX Credential Helper 설치
echo ============================================
echo.

:: 스크립트 경로
set SCRIPT_DIR=%~dp0
set HELPER_PATH=%SCRIPT_DIR%gitnx-credential-helper.ps1

:: GitNX 서버 호스트
set GITNX_HOST=http://125.131.105.208:8082

:: 기존 GCM 비활성화 (GitNX 서버에 대해서만)
git config --global credential.%GITNX_HOST%.provider generic
git config --global credential.%GITNX_HOST%.helper "!'%HELPER_PATH%'"

echo [OK] GitNX credential helper가 설치되었습니다.
echo.
echo 이제 git clone/push 시 GitNX 로그인 창이 뜹니다.
echo (GitHub OAuth 로그인도 지원)
echo.
pause