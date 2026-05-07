param(
    [switch]$Scheduler
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"

if (-not (Test-Path $envFile)) {
    throw ".env 파일을 찾을 수 없습니다."
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) {
        return
    }

    $parts = $line -split '=', 2
    if ($parts.Count -ne 2) {
        return
    }

    [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
}

[Environment]::SetEnvironmentVariable('RUN_MANUAL_CONTENT_IT', 'true', 'Process')

$testClass = if ($Scheduler) {
    'dev.camel.backendlab.scenario.content.scheduler.ContentSchedulerManualIntegrationTest'
} else {
    'dev.camel.backendlab.scenario.content.service.ContentManualIntegrationTest'
}

Push-Location $projectRoot
try {
    .\gradlew.bat test --tests $testClass --no-daemon --rerun-tasks
} finally {
    Pop-Location
}

