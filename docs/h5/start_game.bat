@echo off
title Dungeon Explorer Launcher
echo Starting Dungeon Explorer...
echo.

:: 1. Try Python 3 (Most robust)
python --version >nul 2>&1
if %errorlevel% equ 0 (
    echo Python found. Starting server...
    echo.
    echo Game will run at http://localhost:8000
    echo Please keep this window open while playing.
    echo.
    start "" "http://localhost:8000"
    python -m http.server 8000
    goto :eof
)

:: 2. Try Python 2
python2 --version >nul 2>&1
if %errorlevel% equ 0 (
    echo Python 2 found. Starting server...
    start "" "http://localhost:8000"
    python2 -m SimpleHTTPServer 8000
    goto :eof
)

:: 3. Fallback to PowerShell (Built-in on Windows)
echo Python not found. Using PowerShell Web Server...
echo.
echo NOTE: You may need to allow permission if Windows Firewall asks.
echo.
PowerShell -NoProfile -ExecutionPolicy Bypass -File "%~dp0server.ps1"

if %errorlevel% neq 0 (
    echo.
    echo Failed to start server.
    echo Please install Python (https://www.python.org/) or ensure PowerShell is working.
    pause
)
