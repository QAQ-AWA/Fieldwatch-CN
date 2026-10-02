@echo off
chcp 65001 >nul
echo ========================================================
echo        Fieldwatch 汉化版 - 一键推送到 GitHub 脚本
echo ========================================================
echo.
set /p REPO_URL="请输入你的 GitHub 仓库地址 (例如 https://github.com/你的用户名/Fieldwatch.git): "
if "%REPO_URL%"=="" (
    echo [错误] 仓库地址不能为空！
    pause
    exit /b
)

echo.
echo [1/3] 配置远程仓库...
git remote remove origin >nul 2>&1
git remote add origin %REPO_URL%

echo [2/3] 推送汉化代码到 GitHub main 分支...
git branch -M main
git push -u origin main --force

echo.
if %ERRORLEVEL% EQU 0 (
    echo ========================================================
    echo  [成功] 代码已成功推送到 GitHub！
    echo  现在你可以打开你的 GitHub 仓库页面，点击顶部的 "Actions" 标签。
    echo  GitHub 云端正在全自动为你编译 APK，约 2 分钟后即可在构建任务里直接下载安装包！
    echo ========================================================
) else (
    echo ========================================================
    echo  [提示] 推送遇到问题，请检查网络或 GitHub 登录凭据（建议使用 Personal Access Token 或 GitHub 凭据）。
    echo ========================================================
)
echo.
pause
