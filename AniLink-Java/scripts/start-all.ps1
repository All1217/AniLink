# =====================================================================
# AniLink 后端一键启动脚本（Windows PowerShell 7+）
#
# 设计目标：
#   1. 一条命令拉起 11 个可启动服务，网关最后启动；
#   2. 同一时刻最多并行启动 3 个模块（默认 3，可调低），避免 11 个 JVM 同时
#      初始化把内存打爆（本机实测 11 × 160m 串行启动峰值约 1.8GB）；
#   3. 每个服务独立日志、独立 PID，失败会打印日志尾部；
#   4. `-Stop` 按 PID 文件逆序停止，不会误杀其它 Java 进程。
#
# 用法：
#   .\scripts\start-all.ps1                       # 构建 + 最多 3 个并发启动
#   .\scripts\start-all.ps1 -SkipBuild            # 跳过 mvn install（jar 已存在）
#   .\scripts\start-all.ps1 -MaxParallel 2        # 更保守，最多 2 个并发
#   .\scripts\start-all.ps1 -Only AL-user,AL-video
#   .\scripts\start-all.ps1 -Stop                 # 停止本脚本启动的服务
#
# 注意：MINIO_* 需要预先在环境变量里设置（AL-video / AL-admin 使用）；
#       AUTH_JWT_SECRET 必须与网关、AL-user 一致。
# =====================================================================

[CmdletBinding()]
param(
    # 同时启动的模块上限，硬限制 1..3，防止内存被瞬时打爆
    [ValidateRange(1, 3)]
    [int]$MaxParallel = 3,

    # 每个服务 JVM 堆大小（MB）
    [ValidateRange(96, 1024)]
    [int]$HeapMb = 160,

    # 跳过 mvn install
    [switch]$SkipBuild,

    # 停止本脚本启动的服务
    [switch]$Stop,

    # 只启动指定模块（模块名，如 AL-user,AL-video）
    [string[]]$Only,

    # 单服务端口就绪超时（秒）
    [ValidateRange(30, 600)]
    [int]$TimeoutSec = 120,

    # Maven 命令；留空时自动从 PATH / MAVEN_HOME / .m2 wrapper 里查找
    [string]$MavenCmd = ''
)

$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
$BackendRoot = Split-Path -Parent $ScriptDir
$RepoRoot = Split-Path -Parent $BackendRoot
$LogDir = Join-Path (Join-Path $RepoRoot 'logs') 'start-all'
$PidFile = Join-Path $LogDir 'pids.json'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

# 启动顺序：业务服务之间无强依赖；网关最后启动，避免服务未注册时 503
$AllServices = @(
    [pscustomobject]@{ Module = 'AL-user';          Port = 8025;  Name = 'userservice' }
    [pscustomobject]@{ Module = 'AL-video';         Port = 8026;  Name = 'videoservice' }
    [pscustomobject]@{ Module = 'AL-playback';      Port = 8027;  Name = 'playbackservice' }
    [pscustomobject]@{ Module = 'AL-social';        Port = 8031;  Name = 'socialservice' }
    [pscustomobject]@{ Module = 'AL-comment';       Port = 8028;  Name = 'commentservice' }
    [pscustomobject]@{ Module = 'AL-danmaku';       Port = 8029;  Name = 'danmakuservice' }
    [pscustomobject]@{ Module = 'AL-datacenter';    Port = 8032;  Name = 'datacenterservice' }
    [pscustomobject]@{ Module = 'AL-admin';         Port = 8040;  Name = 'adminservice' }
    [pscustomobject]@{ Module = 'AL-remark';        Port = 8021;  Name = 'remarkservice' }
    [pscustomobject]@{ Module = 'AL-communication'; Port = 8023;  Name = 'communicationservice' }
    [pscustomobject]@{ Module = 'AL-gateway';       Port = 8090;  Name = 'gatewayservice' }
)

function Write-Step([string]$Message) {
    Write-Host ("[{0:HH:mm:ss}] {1}" -f (Get-Date), $Message) -ForegroundColor Cyan
}

function Test-PortOpen([int]$Port) {
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $task = $client.ConnectAsync('127.0.0.1', $Port)
        if ($task.Wait(400) -and $client.Connected) { $client.Close(); return $true }
        $client.Close()
        return $false
    } catch {
        return $false
    }
}

function Wait-Port([int]$Port, [int]$Timeout) {
    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortOpen $Port) { return $true }
        Start-Sleep -Milliseconds 800
    }
    return $false
}

function Get-JarPath([string]$Module) {
    $target = Join-Path (Join-Path $BackendRoot $Module) 'target'
    # 优先 Maven 默认命名，其次兼容 <finalName>${project.artifactId}</finalName> 的模块
    foreach ($name in @(("{0}-1.0-SNAPSHOT.jar" -f $Module), ("{0}.jar" -f $Module))) {
        $candidate = Join-Path $target $name
        if (Test-Path $candidate) { return $candidate }
    }
    if (Test-Path $target) {
        $hit = Get-ChildItem $target -Filter ("{0}*.jar" -f $Module) -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike '*.original' } |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }
    return $null
}

# 解析 Maven：显式参数 → PATH → MAVEN_HOME/M2_HOME → .m2 wrapper → Program Files
function Resolve-Maven([string]$Explicit) {
    if ($Explicit) {
        $cmd = Get-Command $Explicit -ErrorAction SilentlyContinue
        if ($cmd) { return $cmd.Source }
        if (Test-Path $Explicit) { return (Resolve-Path $Explicit).Path }
        throw "指定的 Maven 不存在：$Explicit"
    }

    $onPath = Get-Command 'mvn' -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }

    foreach ($mavenHome in @($env:MAVEN_HOME, $env:M2_HOME)) {
        if (-not $mavenHome) { continue }
        foreach ($name in @('mvn.cmd', 'mvn.bat', 'mvn')) {
            $candidate = Join-Path $mavenHome (Join-Path 'bin' $name)
            if (Test-Path $candidate) { return (Resolve-Path $candidate).Path }
        }
    }

    $wrapperDist = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
    if (Test-Path $wrapperDist) {
        $hit = Get-ChildItem $wrapperDist -Recurse -Filter 'mvn.cmd' -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }

    foreach ($root in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
        if (-not $root -or -not (Test-Path $root)) { continue }
        $hit = Get-ChildItem $root -Filter 'mvn.cmd' -Recurse -Depth 3 -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }

    return $null
}

function Stop-LaunchedServices {
    $targets = @{}

    if (Test-Path $PidFile) {
        $entries = Get-Content $PidFile -Raw -Encoding UTF8 | ConvertFrom-Json
        foreach ($entry in $entries) {
            if ($entry.Pid) { $targets[[int]$entry.Pid] = $entry.Module }
        }
    }

    # 兜底：即使 pids.json 丢失/被覆盖，也按命令行找出所有 AniLink 服务进程
    # 兼容 java -jar 与 IDEA 的 -classpath "...\AL-xxx\target\classes" 启动方式
    $pattern = 'AniLink-Java\\AL-[^\\]+\\target\\'
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue | ForEach-Object {
        if ($_.CommandLine -and $_.CommandLine -match $pattern) {
            $procId = [int]$_.ProcessId
            if (-not $targets.ContainsKey($procId)) {
                $label = if ($_.CommandLine -match 'com\.video\.(\w+Application)') {
                    $Matches[1]
                } elseif ($_.CommandLine -match 'AniLink-Java\\(AL-[^\\]+)\\target') {
                    $Matches[1]
                } else {
                    'AL-service'
                }
                $targets[$procId] = $label
            }
        }
    }

    if ($targets.Count -eq 0) {
        Write-Step '没有发现 AniLink 服务进程'
        if (Test-Path $PidFile) { Remove-Item $PidFile -Force }
        return
    }

    foreach ($procId in $targets.Keys) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Step "停止 $($targets[$procId]) (PID $procId)"
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
    }

    if (Test-Path $PidFile) { Remove-Item $PidFile -Force }

    # 等端口释放，最多 20 秒
    $ports = 8021, 8023, 8025, 8026, 8027, 8028, 8029, 8030, 8031, 8032, 8040, 8090
    $deadline = (Get-Date).AddSeconds(20)
    do {
        $busy = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
            Where-Object { $ports -contains $_.LocalPort })
        if ($busy.Count -eq 0) { break }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)

    if ($busy.Count -gt 0) {
        Write-Host '以下端口仍被占用（可能是非 AniLink 进程，请手动确认）：' -ForegroundColor Yellow
        $busy | Select-Object LocalPort, OwningProcess | Format-Table -AutoSize | Out-Host
    } else {
        Write-Step '全部停止完成（12 个端口均已释放）'
    }
}

if ($Stop) {
    Stop-LaunchedServices
    return
}

# ---------------------------------------------------------------------
# 1. 前置检查
# ---------------------------------------------------------------------
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { throw '找不到 java，请先安装 JDK 17 并加入 PATH' }

$services = $AllServices
if ($Only) {
    $services = $AllServices | Where-Object { $Only -contains $_.Module }
    if (-not $services) { throw "没有匹配的模块：$($Only -join ', ')" }
}

foreach ($svc in $services) {
    $jar = Get-JarPath $svc.Module
    if (-not $jar -and $SkipBuild) {
        throw "缺少 jar：$($svc.Module)/target/$($svc.Module)-1.0-SNAPSHOT.jar，去掉 -SkipBuild 重新构建"
    }
}

if (-not $SkipBuild) {
    $maven = Resolve-Maven $MavenCmd
    if (-not $maven) {
        throw @'
找不到 Maven（mvn）。三种解决方式任选：
  1. 把 mvn 加入 PATH；
  2. 设置 MAVEN_HOME 指向 Maven 安装目录；
  3. 启动时显式指定：.\start-all.bat -MavenCmd "C:\path\to\mvn.cmd"
已有 jar 时也可以直接双击/运行：.\start-all.bat -SkipBuild
'@
    }
    Write-Step "构建后端（$maven）..."
    Push-Location $BackendRoot
    try {
        & $maven -q -DskipTests install
        if ($LASTEXITCODE -ne 0) { throw "mvn install 失败，退出码 $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
    Write-Step '构建完成'
}

if (-not $env:MINIO_ENDPOINT) {
    Write-Host '提示：未设置 MINIO_ENDPOINT / MINIO_ACCESSKEY / MINIO_SECRETKEY，AL-video 与 AL-admin 的上传/转码会不可用' -ForegroundColor Yellow
}

# ---------------------------------------------------------------------
# 2. 分批启动：每批最多 MaxParallel 个，端口就绪后再进入下一批
# ---------------------------------------------------------------------
$pids = @()
$order = 0
$failed = @()

# 已占用端口的视为已在运行，直接跳过
$pending = @()
foreach ($svc in $services) {
    if (Test-PortOpen $svc.Port) {
        Write-Step "$($svc.Module) 端口 $($svc.Port) 已被占用，视为已启动，跳过"
    } else {
        $pending += $svc
    }
}

while ($pending.Count -gt 0) {
    $batch = @($pending | Select-Object -First $MaxParallel)
    $pending = @($pending | Select-Object -Skip $batch.Count)

    $launched = @()
    foreach ($svc in $batch) {
        $jar = Get-JarPath $svc.Module
        if (-not $jar) { $failed += $svc.Module; continue }

        $order++
        $outLog = Join-Path $LogDir ("{0}.out.log" -f $svc.Module)
        $errLog = Join-Path $LogDir ("{0}.err.log" -f $svc.Module)
        $argString = "-Xmx${HeapMb}m -jar `"$jar`""

        Write-Step "启动 $($svc.Module)（端口 $($svc.Port)，堆 ${HeapMb}m）"
        $proc = Start-Process -FilePath $java.Source -ArgumentList $argString `
            -WorkingDirectory (Join-Path $BackendRoot $svc.Module) `
            -RedirectStandardOutput $outLog -RedirectStandardError $errLog `
            -PassThru -WindowStyle Hidden

        $launched += [pscustomobject]@{ Module = $svc.Module; Port = $svc.Port; Proc = $proc; OutLog = $outLog; Order = $order }
        $pids += [pscustomobject]@{ Module = $svc.Module; Port = $svc.Port; Pid = $proc.Id; Order = $order }
    }

    foreach ($item in $launched) {
        if (Wait-Port $item.Port $TimeoutSec) {
            Write-Step "$($item.Module) 已就绪（http://localhost:$($item.Port)）"
            $item.Proc.WaitForExit(0) | Out-Null
            if ($item.Proc.HasExited) {
                $failed += $item.Module
                Write-Host "  $($item.Module) 启动后立即退出，日志尾部：" -ForegroundColor Red
                Get-Content $item.OutLog -Tail 15 -Encoding UTF8 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    $_" }
            $errLog = $item.OutLog -replace "\.out\.log$", ".err.log"; Get-Content $errLog -Tail 15 -Encoding UTF8 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    $_" }
            }
        } else {
            $failed += $item.Module
            Write-Host "  $($item.Module) 在 ${TimeoutSec}s 内未监听 $($item.Port)，日志尾部：" -ForegroundColor Red
            Get-Content $item.OutLog -Tail 15 -Encoding UTF8 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    $_" }
            $errLog = $item.OutLog -replace "\.out\.log$", ".err.log"; Get-Content $errLog -Tail 15 -Encoding UTF8 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    $_" }
        }
    }
}

# ---------------------------------------------------------------------
# 3. 结果汇总
# ---------------------------------------------------------------------
$pids | ConvertTo-Json -Depth 3 | Set-Content -Path $PidFile -Encoding UTF8

Write-Host ''
Write-Host '========== 启动结果 ==========' -ForegroundColor Cyan
foreach ($svc in $services) {
    $ok = Test-PortOpen $svc.Port
    $status = if ($ok) { 'OK  ' } else { 'FAIL' }
    $color = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("  [{0}] {1,-20} {2,5}  {3}" -f $status, $svc.Module, $svc.Port, $svc.Name) -ForegroundColor $color
}
Write-Host "日志目录：$LogDir"
Write-Host "停止命令：.\scripts\start-all.ps1 -Stop"

if ($failed.Count -gt 0) {
    Write-Host "失败模块：$($failed -join ', ')" -ForegroundColor Red
    exit 1
}
if ($services | Where-Object { $_.Module -eq 'AL-gateway' }) {
    Write-Host '网关：http://localhost:8090' -ForegroundColor Green
}
