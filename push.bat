@echo off
chcp 65001 >nul
title Git 一键推送 - UTF-8 模式
setlocal enabledelayedexpansion

REM 一键 Push 到所有 Remote
REM 获取当前分支名
for /f "tokens=*" %%a in ('git rev-parse --abbrev-ref HEAD') do set CURRENT_BRANCH=%%a

REM 检查是否在 Git 仓库中
if not exist ".git" (
    echo 错误：当前目录不是一个 Git 仓库！
    pause
    exit /b 1
)

REM 遍历所有 remote 并执行 push
for /f "tokens=*" %%r in ('git remote') do (
    echo.
    echo 正在推送到远程仓库: %%r 分支: %CURRENT_BRANCH%
    git push %%r %CURRENT_BRANCH%
)

echo.
echo ✅ 已推送到所有远程仓库。
pause