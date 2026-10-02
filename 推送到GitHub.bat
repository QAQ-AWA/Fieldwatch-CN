@echo off
chcp 65001 >nul
echo ========================================================
echo        Fieldwatch 汉化版 - 一键推送到 GitHub 
echo   目标仓库: https://github.com/QAQ-AWA/Fieldwatch-CN
echo ========================================================
echo.
set "PATH=C:\Program Files\Git\cmd;%PATH%"

echo [1/2] 正在连接 GitHub 仓库并推送汉化代码...
echo.
echo 注意：如果弹出 GitHub 登录验证窗口，请点击【Sign in with your browser】授权登录。
echo.

git branch -M main
git push -u origin main --force

echo.
if %ERRORLEVEL% EQU 0 (
    echo ========================================================
    echo  [成功] 汉化代码已全部成功推送到 GitHub！
    echo.
    echo  接下来只需 2 步：
    echo  1. 打开网页: https://github.com/QAQ-AWA/Fieldwatch-CN/actions
    echo  2. 看到正在运行的 "Android CI" 构建，等待 2-3 分钟变成绿勾后，
    echo     点击进入并下载【Fieldwatch-汉化版-debug-apk】即可安装到手机！
    echo ========================================================
) else (
    echo ========================================================
    echo  [提示] 推送未完成。如果遇到网络问题，请开启加速器或检查登录状态后重试。
    echo ========================================================
)
echo.
pause
