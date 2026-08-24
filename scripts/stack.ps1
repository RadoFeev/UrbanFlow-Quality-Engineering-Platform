param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("Up", "Down", "Status", "Logs", "Validate")]
    [string]$Action,
    [ValidateSet("minimal", "integration", "grid", "observability", "full")]
    [string]$Profile = "minimal",
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$compose = Join-Path $workspace "infrastructure\compose.yml"
$arguments = @("compose", "-f", $compose, "--profile", $Profile)

switch ($Action) {
    "Validate" { & docker @arguments config --quiet; break }
    "Status" { & docker @arguments ps; break }
    "Logs" { & docker @arguments logs --no-color; break }
    "Down" { & docker @arguments down --remove-orphans; break }
    "Up" {
        if ($Build) {
            Push-Location $workspace
            try { & (Join-Path $workspace "mvnw.cmd") -DskipTests package } finally { Pop-Location }
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
        $upArguments = @("up", "-d", "--wait")
        if ($Build) { $upArguments += "--build" }
        & docker @arguments @upArguments
    }
}

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
