@echo off
setlocal enabledelayedexpansion

REM 一个 git 仓库里面有多个 remotes，如何批量一键 pull
REM 获取当前分支名
for /f "tokens=*" %%a in ('git rev-parse --abbrev-ref HEAD') do set CURRENT_BRANCH=%%a

REM 遍历所有 remote 并执行 pull
for /f "tokens=*" %%r in ('git remote') do (
    echo.
    echo Pulling from remote: %%r branch: %CURRENT_BRANCH%
    git pull %%r %CURRENT_BRANCH%
)

echo.
echo All remotes pulled.
pause