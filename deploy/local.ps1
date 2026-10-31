param(
    [ValidateSet('Deploy', 'Stop', 'Status')]
    [string]$Action = 'Deploy',
    [ValidateSet('All', 'Backend', 'Frontend', 'Web', 'Admin')]
    [string]$Target = 'All',
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$cacheRoot = Join-Path $projectRoot '.cache'
$runtimeRoot = Join-Path $cacheRoot 'backend'
$processFile = Join-Path $runtimeRoot 'process.json'
$runtimeJar = Join-Path $runtimeRoot 'hongshu-server.jar'
$composeFile = Join-Path $projectRoot 'compose.yaml'
$environmentFile = Join-Path $projectRoot '.env'
$savedEnvironment = @{}
$environmentNames = @(
    'JAVA_HOME', 'TEMP', 'TMP', 'NPM_CONFIG_CACHE', 'NGINX_IMAGE',
    'SPRING_CONFIG_LOCATION', 'SPRING_CONFIG_IMPORT', 'DB_URL', 'DB_USERNAME', 'REDIS_HOST', 'SERVER_PORT',
    'SERVER_ADDRESS', 'HONGSHU_PROFILE', 'LOGGING_LEVEL_COM_HONGSHU',
    'SPRING_DEVTOOLS_RESTART_ENABLED', 'SERVER_FORWARD_HEADERS_STRATEGY',
    'SWAGGER_CONFIG_URL', 'SWAGGER_API_URL', 'SWAGGER_PATHMAPPING'
)
foreach ($name in $environmentNames) {
    $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

function Invoke-Checked {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Command failed (exit $LASTEXITCODE)." }
}

function Invoke-Compose {
    param([string[]]$Arguments)
    Invoke-Checked 'docker' (@('compose', '-f', $composeFile) + $Arguments)
}

function Get-Setting {
    param([string]$Name, [string]$Default)
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if (-not [string]::IsNullOrWhiteSpace($value)) { return $value }
    if (Test-Path -LiteralPath $environmentFile) {
        foreach ($line in Get-Content -LiteralPath $environmentFile -Encoding UTF8) {
            if ($line -match ('^\s*' + [regex]::Escape($Name) + '\s*=\s*(.*?)\s*$')) {
                return $Matches[1].Trim([char[]]@('"', "'"))
            }
        }
    }
    return $Default
}

function Get-BackendProcess {
    if (-not (Test-Path -LiteralPath $processFile)) { return $null }
    $record = Get-Content -LiteralPath $processFile -Raw | ConvertFrom-Json
    $runningProcess = Get-Process -Id $record.processId -ErrorAction SilentlyContinue
    if ($runningProcess -and $runningProcess.ProcessName -eq 'java' -and
        $runningProcess.StartTime.ToUniversalTime().Ticks.ToString() -eq $record.startTicks) {
        return $runningProcess
    }
    return $null
}

function Stop-Backend {
    $runningProcess = Get-BackendProcess
    if ($runningProcess) { Stop-Process -Id $runningProcess.Id -Force; $runningProcess.WaitForExit() }
    if (Test-Path -LiteralPath $processFile) { Remove-Item -LiteralPath $processFile }
}

function Use-LocalJava {
    if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/javac.exe'))) {
        throw 'Set JAVA_HOME to your local JDK 21, or pass -JavaHome <JDK path>.'
    }
    $env:JAVA_HOME = $JavaHome
    $release = Get-Content -LiteralPath (Join-Path $JavaHome 'release') -Raw
    if ($release -notmatch 'JAVA_VERSION="21(?:\.|\")') {
        throw 'This project requires JDK 21. Confirm your JDK version before deploying.'
    }
}

function Build-Backend {
    $resources = Join-Path $projectRoot 'HongShu-backend/hongshu-server/src/main/resources'
    foreach ($config in @('application', 'application-druid')) {
        $destination = Join-Path $resources "$config.yml"
        if (-not (Test-Path -LiteralPath $destination)) {
            Copy-Item -LiteralPath (Join-Path $resources "$config.example.yml") -Destination $destination
        }
    }
    Push-Location (Join-Path $projectRoot 'HongShu-backend')
    try {
        Invoke-Checked 'mvn.cmd' @('-B', '-Dmaven.test.skip=true', "-Dmaven.repo.local=$cacheRoot/maven-repository", 'package')
    } finally { Pop-Location }
}

function Build-Frontend {
    param([string]$Service)
    $sourceRoot = Join-Path $projectRoot "HongShu-Ui/hongshu-$Service"
    Push-Location $sourceRoot
    try {
        if (-not (Test-Path -LiteralPath 'node_modules/.bin/vite.cmd')) {
            Invoke-Checked 'npm.cmd' @('ci')
        }
        $buildCommand = if ($Service -eq 'web') { 'build' } else { 'build:prod' }
        Invoke-Checked 'npm.cmd' @('run', $buildCommand)
    } finally { Pop-Location }
}

function Select-NginxImage {
    # Existing application images already contain Nginx; bind mounts supply the new frontend files.
    $configuredImage = Get-Setting 'NGINX_IMAGE' ''
    if ($configuredImage) { $env:NGINX_IMAGE = $configuredImage; return }
    foreach ($candidate in @('nginx:1.28-alpine', 'hongshu-web:local', 'hongshu-admin:local')) {
        $imageId = & docker image ls --quiet $candidate
        if ($LASTEXITCODE -ne 0) { throw 'Cannot query local Docker images.' }
        if ($imageId) {
            $env:NGINX_IMAGE = $candidate
            Write-Host "Using local Nginx image: $candidate"
            return
        }
    }
    $env:NGINX_IMAGE = 'nginx:1.28-alpine'
}

function Start-Backend {
    $builtJar = Join-Path $projectRoot 'HongShu-backend/hongshu-server/target/hongshu-server.jar'
    if (-not (Test-Path -LiteralPath $builtJar)) { throw 'Backend JAR is missing. Deploy without -SkipBuild first.' }
    if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) {
        throw 'Set JAVA_HOME to your local JDK 21, or pass -JavaHome <JDK path>.'
    }
    $apiPort = Get-Setting 'API_PORT' '8080'
    if ($apiPort -notmatch '^\d+$' -or [int]$apiPort -lt 1 -or [int]$apiPort -gt 65535) {
        throw 'API_PORT must be between 1 and 65535.'
    }
    # Stop the former Docker backend during migration so the host Java process can use its port.
    $oldContainers = @(& docker ps -q --filter 'label=com.docker.compose.project=hongshu' --filter 'label=com.docker.compose.service=backend')
    if ($LASTEXITCODE -ne 0) { throw 'Cannot query Docker containers.' }
    if ($oldContainers.Count) { Invoke-Checked 'docker' (@('stop') + $oldContainers) }
    Stop-Backend
    Copy-Item -LiteralPath $builtJar -Destination $runtimeJar -Force
    $resources = Join-Path $projectRoot 'HongShu-backend/hongshu-server/src/main/resources'
    foreach ($config in @('application', 'application-druid')) {
        Copy-Item -LiteralPath (Join-Path $resources "$config.example.yml") -Destination (Join-Path $runtimeRoot "$config.yml") -Force
    }
    # Use deployment templates and .env, independently of existing development YAML files in the JAR.
    $env:SPRING_CONFIG_LOCATION = ([uri](Join-Path $runtimeRoot 'application.yml')).AbsoluteUri
    $env:SPRING_CONFIG_IMPORT = 'optional:' + ([uri]$environmentFile).AbsoluteUri + '[.properties]'
    $env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/hongshu_springboot?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true&useSSL=false'
    $env:DB_USERNAME = 'hongshu'
    $env:REDIS_HOST = '127.0.0.1'
    $env:SERVER_PORT = $apiPort
    $env:SERVER_ADDRESS = '0.0.0.0'
    $env:HONGSHU_PROFILE = Join-Path $projectRoot 'HongShu-backend/upload'
    $env:LOGGING_LEVEL_COM_HONGSHU = 'info'
    $env:SPRING_DEVTOOLS_RESTART_ENABLED = 'false'
    $env:SERVER_FORWARD_HEADERS_STRATEGY = 'framework'
    $env:SWAGGER_CONFIG_URL = '/prod-api/v3/api-docs/swagger-config'
    $env:SWAGGER_API_URL = '/prod-api/v3/api-docs'
    $env:SWAGGER_PATHMAPPING = '/prod-api'
    $logRoot = Join-Path $cacheRoot 'logs/backend'
    New-Item -ItemType Directory -Path $logRoot -Force | Out-Null
    $javaArguments = @("-Djava.io.tmpdir=`"$cacheRoot/tmp`"", '-jar', "`"$runtimeJar`"")
    $runningProcess = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList $javaArguments `
        -WorkingDirectory $runtimeRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logRoot 'stdout.log') -RedirectStandardError (Join-Path $logRoot 'stderr.log')
    @{
        processId = $runningProcess.Id
        startTicks = $runningProcess.StartTime.ToUniversalTime().Ticks.ToString()
    } | ConvertTo-Json | Set-Content -LiteralPath $processFile -Encoding UTF8
    $deadline = (Get-Date).AddSeconds(180)
    do {
        $runningProcess.Refresh()
        if ($runningProcess.HasExited) { break }
        try {
            $response = Invoke-WebRequest -Uri "http://127.0.0.1:$apiPort/" -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) { Write-Host "Backend started with local Java (PID $($runningProcess.Id))."; return }
        } catch { }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    Stop-Backend
    if ($oldContainers.Count) { Invoke-Checked 'docker' (@('start') + $oldContainers) }
    throw "Backend did not start. See $logRoot/stdout.log and stderr.log."
}

try {
    Push-Location $projectRoot
    New-Item -ItemType Directory -Path $cacheRoot, $runtimeRoot, (Join-Path $cacheRoot 'tmp') -Force | Out-Null
    $env:TEMP = Join-Path $cacheRoot 'tmp'
    $env:TMP = $env:TEMP
    $env:NPM_CONFIG_CACHE = Join-Path $cacheRoot 'npm'
    if ($Action -eq 'Status') {
        Invoke-Compose @('ps')
        $runningProcess = Get-BackendProcess
        if ($runningProcess) { Write-Host "Local backend: running (PID $($runningProcess.Id))" }
        else { Write-Host 'Local backend: stopped' }
        return
    }
    if ($Action -eq 'Stop') {
        if ($Target -in @('All', 'Backend')) { Stop-Backend }
        if ($Target -eq 'All') { Invoke-Compose @('stop') }
        elseif ($Target -in @('Frontend', 'Web', 'Admin')) {
            $services = if ($Target -eq 'Frontend') { @('web', 'admin') } else { @($Target.ToLowerInvariant()) }
            Invoke-Compose (@('stop') + $services)
        }
        return
    }
    if (-not (Test-Path -LiteralPath $environmentFile)) { throw 'Copy .env.example to .env and fill in your configuration first.' }
    $deployBackend = $Target -in @('All', 'Backend')
    if ($deployBackend) { Use-LocalJava }
    $frontends = @()
    if ($Target -in @('All', 'Frontend')) { $frontends = @('web', 'admin') }
    elseif ($Target -in @('Web', 'Admin')) { $frontends = @($Target.ToLowerInvariant()) }
    if (-not $SkipBuild) {
        if ($deployBackend) { Build-Backend }
        foreach ($frontend in $frontends) { Build-Frontend $frontend }
    }
    if ($deployBackend) {
        Invoke-Compose @('up', '-d', '--wait', '--wait-timeout', '180', 'mysql', 'redis')
        Start-Backend
    }
    if ($frontends.Count) {
        foreach ($frontend in $frontends) {
            if (-not (Test-Path -LiteralPath (Join-Path $projectRoot "HongShu-Ui/hongshu-$frontend/dist/index.html"))) {
                throw "Frontend $frontend is missing. Deploy without -SkipBuild first."
            }
        }
        Select-NginxImage
        Invoke-Compose (@('up', '-d', '--no-build', '--no-deps') + $frontends)
    }
    Write-Host 'Deployment complete.'
} finally {
    Pop-Location
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
    }
}
