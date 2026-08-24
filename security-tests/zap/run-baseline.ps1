param(
    [string]$Target = "http://host.docker.internal:8080"
)

$ErrorActionPreference = "Stop"
$targetUri = [Uri]$Target
$allowedHosts = @("localhost", "127.0.0.1", "host.docker.internal")
if ($targetUri.Scheme -notin @("http", "https") -or $targetUri.Host -notin $allowedHosts) {
    throw "ZAP baseline target must be an allowlisted local HTTP(S) host."
}

$evidence = Join-Path $PSScriptRoot "..\target\zap"
New-Item -ItemType Directory -Force -Path $evidence | Out-Null
$resolvedEvidence = (Resolve-Path -LiteralPath $evidence).Path

docker run --rm `
    --add-host host.docker.internal:host-gateway `
    --volume "${resolvedEvidence}:/zap/wrk:rw" `
    ghcr.io/zaproxy/zaproxy:2.16.1 `
    zap-baseline.py -t $Target -c /zap/wrk/zap-baseline.conf -r report.html -J report.json
