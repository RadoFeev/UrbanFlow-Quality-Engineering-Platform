$ErrorActionPreference = "Stop"
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
Push-Location $workspace
try {
    & (Join-Path $workspace "mvnw.cmd") -pl :security-tests -Dgroups=infrastructure test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & git diff --check
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & k6 inspect performance-tests/k6/urbanflow.js | Out-Null
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}
