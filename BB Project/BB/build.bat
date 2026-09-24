@echo off
REM Shared build step for run.bat and selftest.bat. Compiles every source file into bin\.
REM
REM The source list is written with forward slashes on purpose: javac treats a backslash
REM inside an @argfile as an escape character, so "C:\BB Project\..." arrives as
REM "C:BB ProjectBB..." and every file is reported as not found.

setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist bin mkdir bin

> "%TEMP%\bb_sources.txt" (
    for /f "delims=" %%f in ('dir /s /b /a-d src\*.java') do (
        set "P=%%f"
        echo "!P:\=/!"
    )
)

javac -nowarn -d bin "@%TEMP%\bb_sources.txt"
if errorlevel 1 (
    echo.
    echo Build failed.
    exit /b 1
)

exit /b 0
