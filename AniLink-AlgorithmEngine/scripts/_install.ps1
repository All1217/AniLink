# Create .venv and install requirements (optional; run once)
# 说明保持英文：Windows PowerShell 5.1 读取无 BOM 的 .ps1 会按 ANSI 解析，中文会乱码。
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$venv = Join-Path $Root ".venv"
$venvPy = Join-Path $venv "Scripts\python.exe"

if (-not (Test-Path $venvPy)) {
    Write-Host "Creating virtualenv at $venv ..."
    $py = Get-Command py -ErrorAction SilentlyContinue
    if ($py) {
        & py -3 -m venv $venv
    } else {
        $python = Get-Command python -ErrorAction SilentlyContinue
        if (-not $python) {
            Write-Host "[ERROR] python not found; install Python 3.11+ first." -ForegroundColor Red
            exit 1
        }
        & python -m venv $venv
    }
}

if (-not (Test-Path $venvPy)) {
    Write-Host "[ERROR] virtualenv creation failed." -ForegroundColor Red
    exit 1
}

Write-Host "Installing dependencies from requirements.txt ..."
& $venvPy -m pip install --upgrade pip
& $venvPy -m pip install -r "requirements.txt"

Write-Host ""
Write-Host "[OK] 依赖安装完成，现在可以双击 scripts\start.bat 启动。" -ForegroundColor Green
