param(
    [ValidateSet("all", "smoke", "regression", "api", "ui", "contract", "component", "integration", "event", "database", "e2e", "accessibility", "visual", "performance", "security")]
    [string]$Suite = "smoke",
    [ValidateSet("chrome", "firefox", "edge")]
    [string]$Browser = "chrome",
    [int]$Parallelism = 4,
    [string]$RemoteUrl = "",
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$jar = Join-Path $workspace "test-orchestrator\target\test-orchestrator-0.1.0.jar"
if (-not (Test-Path -LiteralPath $jar)) {
    Push-Location $workspace
    try { & (Join-Path $workspace "mvnw.cmd") -pl :test-orchestrator -am -DskipTests package } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$arguments = @(
    "--environment=local",
    "--suite=$Suite",
    "--browser=$Browser",
    "--headless=true",
    "--parallelism=$Parallelism",
    "--retries=0",
    "--video=false",
    "--cleanup=true",
    "--report-directory=target\unified-report",
    "--execute=$($Execute.IsPresent.ToString().ToLowerInvariant())"
)
if ($RemoteUrl) { $arguments += "--remote-url=$RemoteUrl" }

Push-Location $workspace
try { & java -jar $jar @arguments } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
