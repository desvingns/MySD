[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$requiredFiles = @(
    "assets/provenance.csv",
    "spec/00_manifest.yaml",
    "spec/product-brief.md",
    "spec/requirements.md",
    "spec/user-stories.md",
    "spec/acceptance/foundation.feature",
    "spec/design.md",
    "spec/platform/android.md",
    "spec/content-plan.md",
    "spec/balance-plan.md",
    "spec/engine-gap-analysis.md",
    "spec/nfr.md",
    "spec/a11y.md",
    "spec/security-privacy.md",
    "spec/analytics.md",
    "spec/i18n.md",
    "spec/risks.md",
    "spec/estimate.md",
    "spec/deviations.md",
    "spec/traceability.csv",
    "spec/fit/registry.csv"
)
$errors = [System.Collections.Generic.List[string]]::new()

foreach ($relative in $requiredFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $relative) -PathType Leaf)) {
        $errors.Add("missing artifact: $relative")
    }
}

if ($errors.Count -eq 0) {
    $requirementsText = Get-Content -Raw -LiteralPath (Join-Path $repoRoot "spec/requirements.md")
    $storiesText = Get-Content -Raw -LiteralPath (Join-Path $repoRoot "spec/user-stories.md")
    $featureText = Get-Content -Raw -LiteralPath (Join-Path $repoRoot "spec/acceptance/foundation.feature")
    $trace = @(Import-Csv -LiteralPath (Join-Path $repoRoot "spec/traceability.csv"))

    $requirementIds = @([regex]::Matches($requirementsText, '(?m)^### (FR-[0-9]{3})') |
        ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
    $storyIds = @([regex]::Matches($storiesText, '(?m)^## (US-[0-9]{3})') |
        ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
    $acceptanceIds = @([regex]::Matches($featureText, '@(AC-[0-9]{3})') |
        ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)

    foreach ($id in $requirementIds) {
        if ($trace.requirement_id -notcontains $id) { $errors.Add("untraced requirement: $id") }
    }
    foreach ($id in $storyIds) {
        if ($trace.user_story_id -notcontains $id) { $errors.Add("untraced story: $id") }
    }
    foreach ($id in $acceptanceIds) {
        if ($trace.acceptance_id -notcontains $id) { $errors.Add("untraced acceptance: $id") }
    }

    $manifest = Get-Content -Raw -LiteralPath (Join-Path $repoRoot "spec/00_manifest.yaml")
    if ($manifest -match 'status:\s*gate1_blocked' -and $requirementsText -match '(?m)^### FR-1[0-9]{2}') {
        $errors.Add("gameplay-range FR-100+ exists while Gate 1 is blocked")
    }
}

if ($errors.Count -gt 0) {
    [pscustomobject]@{
        status = "fail"
        errors = $errors
    } | ConvertTo-Json -Depth 4
    exit 1
}

[pscustomobject]@{
    status = "pass"
    requirements = $requirementIds.Count
    stories = $storyIds.Count
    acceptance = $acceptanceIds.Count
    trace_rows = $trace.Count
    gate = "pre_gate1_baseline"
} | ConvertTo-Json -Compress
