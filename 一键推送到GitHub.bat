@echo off
set "PATH=C:\Program Files\Git\cmd;%PATH%"
cd /d "%~dp0"
echo.
echo ========================================================
echo   Fieldwatch Push to GitHub
echo   Target: https://github.com/QAQ-AWA/Fieldwatch-CN
echo ========================================================
echo.
echo Connecting to GitHub and pushing Chinese localized code...
echo.

git branch -M main
git push -u origin main --force

echo.
if %ERRORLEVEL% EQU 0 (
    echo ========================================================
    echo  [SUCCESS] Code pushed successfully!
    echo.
    echo  Now open your browser:
    echo  https://github.com/QAQ-AWA/Fieldwatch-CN/actions
    echo.
    echo  Wait 2-3 minutes for the build to complete,
    echo  then download [Fieldwatch-Chinese-debug-apk].
    echo ========================================================
) else (
    echo ========================================================
    echo  [FAILED] Push failed. 
    echo  If a GitHub login window popped up, please complete the login.
    echo ========================================================
)
echo.
pause
