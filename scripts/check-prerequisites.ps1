$ErrorActionPreference = "Stop"

$tools = @(
    @{ Name = "Java 25"; Command = "java"; Arguments = @("-version") },
    @{ Name = "Git"; Command = "git"; Arguments = @("--version") },
    @{ Name = "Docker"; Command = "docker"; Arguments = @("--version"); Optional = $true },
    @{ Name = "kubectl"; Command = "kubectl"; Arguments = @("version", "--client"); Optional = $true },
    @{ Name = "Helm"; Command = "helm"; Arguments = @("version", "--short"); Optional = $true },
    @{ Name = "k6"; Command = "k6"; Arguments = @("version"); Optional = $true }
)

$missingRequired = $false
foreach ($tool in $tools) {
    $resolved = Get-Command $tool.Command -ErrorAction SilentlyContinue
    if ($null -eq $resolved) {
        $kind = if ($tool.Optional) { "optional" } else { "required" }
        Write-Host "[missing/$kind] $($tool.Name)"
        if (-not $tool.Optional) { $missingRequired = $true }
        continue
    }
    Write-Host "[available] $($tool.Name): $($resolved.Source)"
}

if ($missingRequired) { exit 1 }
