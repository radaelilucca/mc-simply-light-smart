@echo off
setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0apply-complementary-simply-light-dimming.ps1"
if errorlevel 1 (
    echo.
    echo The shader patch was not applied. Check that the extracted Complementary
    echo Reimagined and Unbound folders are in shaderpacks next to these scripts.
    pause
    exit /b 1
)

echo.
echo Simply Light - Smart! shader patches applied successfully.
pause
