@echo off
setlocal enabledelayedexpansion

REM ?????????????????????????????????????????????
REM NeoGit ?ㅽ뻾 ?ㅽ겕由쏀듃 (Windows)
REM ?????????????????????????????????????????????
set APP_NAME=NeoGit
set PROJECT_DIR=%~dp0
set DB_CONTAINER=neogit-db
set DB_NAME=neogit
set DB_USER=root
set SERVER_PORT=8080
set PID_FILE=%PROJECT_DIR%.neogit.pid
set LOG_FILE=%PROJECT_DIR%neogit.log

REM ?? ?됱긽 肄붾뱶 (Windows 10+) ??
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "CYAN=[96m"
set "NC=[0m"

REM ?? ?ъ슜踰???
if "%1"=="" goto cmd_start
if /i "%1"=="start" goto cmd_start
if /i "%1"=="stop" goto cmd_stop
if /i "%1"=="restart" goto cmd_restart
if /i "%1"=="status" goto cmd_status
if /i "%1"=="build" goto cmd_build
if /i "%1"=="clean" goto cmd_clean
if /i "%1"=="logs" goto cmd_logs
if /i "%1"=="db-check" goto cmd_db_check
if /i "%1"=="help" goto cmd_help
if /i "%1"=="-h" goto cmd_help

echo %RED%[??%NC% ?????녿뒗 紐낅졊: %1
goto cmd_help

:cmd_help
echo.
echo %CYAN%Usage:%NC% run.bat [command]
echo.
echo   start       ???ㅽ뻾 (湲곕낯媛?
echo   stop        ?ㅽ뻾 以묒씤 ??醫낅즺
echo   restart     ?ъ떆??
echo   status      ?곹깭 ?뺤씤
echo   build       鍮뚮뱶留??섑뻾
echo   clean       鍮뚮뱶 ?뺣━ ???щ퉴??
echo   logs        理쒓렐 濡쒓렇 ?뺤씤
echo   db-check    DB ?곌껐 ?뺤씤
echo   help        ?꾩?留?
echo.
goto :eof

REM ?? Docker / PostgreSQL ?뺤씤 ??
:check_db
echo %CYAN%[%APP_NAME%]%NC% PostgreSQL ?곹깭 ?뺤씤 以?..

where docker >nul 2>&1
if errorlevel 1 (
    echo %RED%[??%NC% Docker媛 ?ㅼ튂?섏뼱 ?덉? ?딆뒿?덈떎.
    exit /b 1
)

docker ps -a --format "{{.Names}}" | findstr /x "%DB_CONTAINER%" >nul 2>&1
if errorlevel 1 (
    echo %YELLOW%[!]%NC% 而⑦뀒?대꼫 '%DB_CONTAINER%'媛 ?놁뒿?덈떎. ?앹꽦 諛??쒖옉?⑸땲??..
    docker-compose up -d postgres
    if errorlevel 1 (
        echo %RED%[??%NC% 而⑦뀒?대꼫瑜??앹꽦?????놁뒿?덈떎. 'docker-compose up -d postgres'瑜??섎룞 ?ㅽ뻾?섏꽭??
        exit /b 1
    )
    timeout /t 5 /nobreak >nul
) else (
    docker ps --format "{{.Names}}" | findstr /x "%DB_CONTAINER%" >nul 2>&1
    if errorlevel 1 (
        echo %YELLOW%[!]%NC% 而⑦뀒?대꼫 '%DB_CONTAINER%'媛 ?ㅽ뻾 以묒씠 ?꾨떃?덈떎. ?쒖옉?⑸땲??..
        docker start %DB_CONTAINER% >nul 2>&1
        if errorlevel 1 (
            echo %RED%[??%NC% 而⑦뀒?대꼫瑜??쒖옉?????놁뒿?덈떎. 'docker start %DB_CONTAINER%'瑜??섎룞 ?ㅽ뻾?섏꽭??
            exit /b 1
        )
        timeout /t 3 /nobreak >nul
    )
)
echo %GREEN%[??%NC% PostgreSQL 而⑦뀒?대꼫 ?ㅽ뻾 以?

REM DB 議댁옱 ?뺤씤
docker exec %DB_CONTAINER% psql -U %DB_USER% -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='%DB_NAME%'" 2>nul | findstr "1" >nul 2>&1
if errorlevel 1 (
    echo %YELLOW%[!]%NC% ?곗씠?곕쿋?댁뒪 '%DB_NAME%'媛 ?놁뒿?덈떎. ?앹꽦?⑸땲??..
    docker exec %DB_CONTAINER% psql -U %DB_USER% -d postgres -c "CREATE DATABASE %DB_NAME%;" >nul 2>&1
    echo %GREEN%[??%NC% ?곗씠?곕쿋?댁뒪 '%DB_NAME%' ?앹꽦 ?꾨즺
) else (
    echo %GREEN%[??%NC% ?곗씠?곕쿋?댁뒪 '%DB_NAME%' ?뺤씤 ?꾨즺
)
goto :eof

REM ?? ?ы듃 ?ъ슜 ?뺤씤 ??
:check_port
netstat -ano | findstr ":%SERVER_PORT%" | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%SERVER_PORT%" ^| findstr "LISTENING"') do (
        echo %YELLOW%[!]%NC% ?ы듃 %SERVER_PORT%???대? ?ъ슜 以묒엯?덈떎 (PID: %%a)
        exit /b 1
    )
)
exit /b 0

REM ?? 鍮뚮뱶 ??
:do_build
echo %CYAN%[%APP_NAME%]%NC% ?꾨줈?앺듃 鍮뚮뱶 以?..
cd /d "%PROJECT_DIR%"
call gradlew.bat build -x test --quiet >nul 2>&1
if errorlevel 1 (
    echo %RED%[??%NC% 鍮뚮뱶 ?ㅽ뙣
    exit /b 1
)
echo %GREEN%[??%NC% 鍮뚮뱶 ?깃났
goto :eof

REM ?? ?쒖옉 ??
:cmd_start
echo %CYAN%[%APP_NAME%]%NC% ?쒖옉 以?..

call :check_db
if errorlevel 1 exit /b 1

call :check_port
if errorlevel 1 (
    set /p answer="湲곗〈 ?꾨줈?몄뒪瑜?醫낅즺?섍퀬 ?쒖옉?좉퉴?? (y/N): "
    if /i "!answer!"=="y" (
        call :do_stop
        timeout /t 2 /nobreak >nul
    ) else (
        echo %RED%[??%NC% 醫낅즺?⑸땲??
        exit /b 1
    )
)

call :do_build
if errorlevel 1 exit /b 1

cd /d "%PROJECT_DIR%"
echo %CYAN%[%APP_NAME%]%NC% ?쒕쾭 ?쒖옉 以?(port: %SERVER_PORT%)...

REM Start gradlew bootRun in background
start /b cmd /c gradlew.bat bootRun --quiet ^> "%LOG_FILE%" 2^>^&1

REM Get PID of the Java process
timeout /t 3 /nobreak >nul
for /f "tokens=2" %%a in ('tasklist /fi "imagename eq java.exe" /fo list ^| findstr "PID:"') do (
    set JAVA_PID=%%a
    goto :found_pid
)
:found_pid
echo !JAVA_PID! > "%PID_FILE%"

REM ?쒕쾭 湲곕룞 ?湲?
echo %CYAN%[%APP_NAME%]%NC% ?쒕쾭 湲곕룞 ?湲?以?..
set /a count=0
:wait_loop
if !count! geq 30 goto timeout_error
curl -s -o nul http://localhost:%SERVER_PORT%/login >nul 2>&1
if not errorlevel 1 (
    echo.
    echo %GREEN%[??%NC% %APP_NAME% ?쒕쾭媛 ?쒖옉?섏뿀?듬땲??
    echo.
    echo   %CYAN%URL:%NC%  http://localhost:%SERVER_PORT%
    echo   %CYAN%PID:%NC%  !JAVA_PID!
    echo   %CYAN%Log:%NC%  %LOG_FILE%
    echo.
    goto :eof
)
echo|set /p="."
timeout /t 1 /nobreak >nul
set /a count+=1
goto wait_loop

:timeout_error
echo.
echo %RED%[??%NC% ?쒕쾭 ?쒖옉 ??꾩븘??(30珥?. 濡쒓렇瑜??뺤씤?섏꽭?? %LOG_FILE%
exit /b 1

REM ?? 醫낅즺 ??
:cmd_stop
:do_stop
echo %CYAN%[%APP_NAME%]%NC% 醫낅즺 以?..

REM PID ?뚯씪濡?醫낅즺
if exist "%PID_FILE%" (
    set /p APP_PID=<"%PID_FILE%"
    tasklist /fi "pid eq !APP_PID!" 2>nul | findstr "!APP_PID!" >nul 2>&1
    if not errorlevel 1 (
        taskkill /pid !APP_PID! /f >nul 2>&1
        echo %GREEN%[??%NC% ?꾨줈?몄뒪 醫낅즺 (PID: !APP_PID!)
    )
    del "%PID_FILE%" >nul 2>&1
)

REM bootRun 諛?Java ?꾨줈?몄뒪 ?뺣━
taskkill /fi "windowtitle eq *gradlew*bootRun*" /f >nul 2>&1
wmic process where "commandline like '%%com.neogit.NeoGitApplication%%'" delete >nul 2>&1

echo %GREEN%[??%NC% %APP_NAME% 醫낅즺 ?꾨즺
goto :eof

REM ?? ?ъ떆????
:cmd_restart
call :cmd_stop
timeout /t 2 /nobreak >nul
call :cmd_start
goto :eof

REM ?? ?곹깭 ??
:cmd_status
echo.
echo %CYAN%?먥븧??%APP_NAME% Status ?먥븧??NC%
echo.

REM App
if exist "%PID_FILE%" (
    set /p APP_PID=<"%PID_FILE%"
    tasklist /fi "pid eq !APP_PID!" 2>nul | findstr "!APP_PID!" >nul 2>&1
    if not errorlevel 1 (
        echo %GREEN%[??%NC% ?? ?ㅽ뻾 以?(PID: !APP_PID!)
    ) else (
        echo %YELLOW%[!]%NC% ?? 以묒???(PID ?뚯씪? 議댁옱)
    )
) else (
    netstat -ano | findstr ":%SERVER_PORT%" | findstr "LISTENING" >nul 2>&1
    if not errorlevel 1 (
        echo %GREEN%[??%NC% ?? ?ы듃 %SERVER_PORT% ?쒖꽦
    ) else (
        echo %YELLOW%[!]%NC% ?? 以묒???
    )
)

REM DB
docker ps --format "{{.Names}}" 2>nul | findstr /x "%DB_CONTAINER%" >nul 2>&1
if not errorlevel 1 (
    echo %GREEN%[??%NC% DB: %DB_CONTAINER% ?ㅽ뻾 以?
) else (
    echo %YELLOW%[!]%NC% DB: %DB_CONTAINER% 以묒???
)

REM HTTP check
for /f %%i in ('curl -s -o nul -w "%%{http_code}" http://localhost:%SERVER_PORT%/login 2^>nul') do set HTTP_CODE=%%i
if not defined HTTP_CODE set HTTP_CODE=000
if "%HTTP_CODE%"=="200" (
    echo %GREEN%[??%NC% HTTP: http://localhost:%SERVER_PORT% ?묐떟 ?뺤긽 (%HTTP_CODE%)
) else (
    echo %YELLOW%[!]%NC% HTTP: ?묐떟 ?놁쓬 (%HTTP_CODE%)
)

echo.
goto :eof

REM ?? 鍮뚮뱶 ??
:cmd_build
call :do_build
goto :eof

REM ?? ?대┛ 鍮뚮뱶 ??
:cmd_clean
echo %CYAN%[%APP_NAME%]%NC% ?대┛ 鍮뚮뱶...
cd /d "%PROJECT_DIR%"
call gradlew.bat clean build -x test --quiet
echo %GREEN%[??%NC% ?대┛ 鍮뚮뱶 ?꾨즺
goto :eof

REM ?? 濡쒓렇 ??
:cmd_logs
if exist "%LOG_FILE%" (
    powershell -command "Get-Content '%LOG_FILE%' -Tail 100"
) else (
    echo %YELLOW%[!]%NC% 濡쒓렇 ?뚯씪???놁뒿?덈떎.
)
goto :eof

REM ?? DB 泥댄겕 ??
:cmd_db_check
call :check_db
goto :eof
