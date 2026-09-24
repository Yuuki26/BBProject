@echo off
REM Builds and runs the game.
REM
REM lib is on the CLASSPATH, not just a dependency folder: every image is loaded through
REM getResource("/ships/..."), getResource("/background.png") and so on, so lib has to be a
REM classpath root or the game starts with no art at all.

cd /d "%~dp0"

call "%~dp0build.bat"
if errorlevel 1 exit /b 1

echo Starting Battleship...
java -cp "bin;lib" com.bb.Main
