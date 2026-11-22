param(
    [switch]$Check
)

# AniLink-AlgorithmEngine launcher
# 说明保持英文：Windows PowerShell 5.1 读取无 BOM 的 .ps1 会按 ANSI 解析，中文会乱码。
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Test-PythonDeps {
    param([string]$Exe)
    if (-not $Exe -or -not (Test-Path $Exe)) { return $false }
    try {
        & $Exe -c "import flask, redis, pika" 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    }
}

function Find-Python {
    $candidates = @(
        (Join-Path $Root ".venv\Scripts\python.exe"),
        (Join-Path $Root "venv\Scripts\python.exe"),
        (Join-Path $env:USERPROFILE "anaconda3\python.exe"),
        (Join-Path $env:USERPROFILE "miniconda3\python.exe")
    )
    if ($env:CONDA_PREFIX) { $candidates += (Join-Path $env:CONDA_PREFIX "python.exe") }
    $onPath = Get-Command python -ErrorAction SilentlyContinue
    if ($onPath) { $candidates += $onPath.Source }

    foreach ($exe in $candidates) {
        if (Test-PythonDeps $exe) { return $exe }
    }
    return $null
}

$pythonExe = Find-Python
if (-not $pythonExe) {
    Write-Host "[ERROR] 没有找到已安装依赖的 Python（需要 flask / redis / pika）。" -ForegroundColor Red
    Write-Host "        先运行 scripts\install.bat 创建 .venv 并安装 requirements.txt，" -ForegroundColor Yellow
    Write-Host "        或用已有 conda 环境（conda activate <env> 后再双击 start.bat）。" -ForegroundColor Yellow
    exit 1
}

$env:PYTHONPATH = "$Root;$Root\src"
# 代码里有 emoji 日志，GBK 控制台会直接抛 UnicodeEncodeError，强制 Python 用 UTF-8 输出
$env:PYTHONIOENCODING = "utf-8"
$env:PYTHONUTF8 = "1"

$port = 5000
$listening = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
if ($listening) {
    $pids = ($listening | Select-Object -ExpandProperty OwningProcess -Unique) -join ", "
    Write-Host "[SKIP] 端口 $port 已被占用（PID: $pids），服务可能已在运行。" -ForegroundColor Yellow
    Write-Host "       需要重启请先运行 scripts\stop.bat。" -ForegroundColor Yellow
    exit 1
}

if ($Check) {
    Write-Host "[OK] python : $pythonExe"
    Write-Host "[OK] root   : $Root"
    Write-Host "[OK] PYTHONPATH : $env:PYTHONPATH"
    Write-Host "[OK] port $port is free"
    if (-not $env:DASHSCOPE_API_KEY) {
        Write-Host "[WARN] DASHSCOPE_API_KEY 未设置：向量化/推荐相关功能运行时会失败。" -ForegroundColor Yellow
    }
    exit 0
}

Write-Host "=============================================="
Write-Host " AniLink-AlgorithmEngine (Flask, 用户画像/推荐)"
Write-Host "=============================================="
Write-Host " python : $pythonExe"
Write-Host " root   : $Root"
Write-Host " 接口   : http://localhost:$port  (GET /recommendations/<uid>)"
Write-Host " 依赖   : MySQL / Redis / RabbitMQ / Elasticsearch（内置库需已启动）"
if (-not $env:DASHSCOPE_API_KEY) {
    Write-Host " [WARN] DASHSCOPE_API_KEY 未设置，LLM 向量化功能会失败" -ForegroundColor Yellow
}
Write-Host " 停止   : Ctrl+C，或关闭本窗口，或运行 scripts\stop.bat"
Write-Host "----------------------------------------------"

& $pythonExe "src\main.py"
