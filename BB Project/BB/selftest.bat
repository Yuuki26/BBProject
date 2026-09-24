@echo off
REM Builds the game and runs the built-in self-check.
REM Prints one PASS/FAIL line per behaviour and exits non-zero if anything regressed.

cd /d "%~dp0"

call "%~dp0build.bat"
if errorlevel 1 exit /b 1

java -cp "bin;lib" Test.SelfTest
