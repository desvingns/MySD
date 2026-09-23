[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$manifestPath = Join-Path $repoRoot "spec/00_manifest.yaml"
$graphPath = Join-Path $repoRoot "spec/evidence/state-graph.v1.json"
$requirementsPath = Join-Path $repoRoot "spec/requirements.md"
$storiesPath = Join-Path $repoRoot "spec/user-stories.md"
$featurePath = Join-Path $repoRoot "spec/acceptance/foundation.feature"
$tracePath = Join-Path $repoRoot "spec/traceability.csv"
$fitPath = Join-Path $repoRoot "spec/fit/registry.csv"
$errors = [System.Collections.Generic.List[string]]::new()

$requiredFiles = @(
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
foreach ($relative in $requiredFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $relative) -PathType Leaf)) {
        $errors.Add("missing Gate 2 artifact: $relative")
    }
}

$manifest = Get-Content -Raw -LiteralPath $manifestPath
if ($manifest -notmatch '(?ms)^  gate1:\s*\r?\n\s{4}status:\s*accepted\s*$') {
    $errors.Add("Gate 2 requires accepted Gate 1")
}
if ($manifest -notmatch '(?ms)^  gate1:.*?\r?\n\s{4}human_decision:\s*accepted\s*$') {
    $errors.Add("Gate 2 requires accepted Gate 1 human decision")
}
$gate2Status = ([regex]::Match($manifest, '(?ms)^  gate2:\s*\r?\n\s{4}status:\s*(\S+)')).Groups[1].Value
if (@("pending_evaluation", "accepted_relaxed") -notcontains $gate2Status) {
    $errors.Add("unexpected Gate 2 status before/after evaluation: $gate2Status")
}

$graph = Get-Content -Raw -LiteralPath $graphPath | ConvertFrom-Json
$scopeById = @{}
foreach ($scope in @($graph.coverage.scope_decisions)) {
    $scopeById[$scope.inventory_id] = $scope
    if ($scope.human_lock -ne "accepted") {
        $errors.Add("scope $($scope.inventory_id) is not human-locked accepted")
    }
}
$acceptedScope = @("INV-001", "INV-002", "INV-003", "INV-006", "INV-007")
$deferredScope = @("INV-004", "INV-005")
$excludedScope = @("INV-008")
foreach ($scopeId in ($acceptedScope + $deferredScope + $excludedScope)) {
    if (-not $scopeById.ContainsKey($scopeId)) {
        $errors.Add("missing required scope decision: $scopeId")
    }
}
foreach ($scopeId in $deferredScope) {
    if ($scopeById[$scopeId].decision -ne "defer") {
        $errors.Add("deferred scope $scopeId has non-defer decision")
    }
}
foreach ($scopeId in $excludedScope) {
    if ($scopeById[$scopeId].decision -ne "exclude") {
        $errors.Add("excluded scope $scopeId has non-exclude decision")
    }
}

$requirementsText = Get-Content -Raw -LiteralPath $requirementsPath
$storiesText = Get-Content -Raw -LiteralPath $storiesPath
$featureText = Get-Content -Raw -LiteralPath $featurePath
$trace = @(Import-Csv -LiteralPath $tracePath)
$fit = @(Import-Csv -LiteralPath $fitPath)

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

$gameplaySections = @([regex]::Matches(
    $requirementsText,
    '(?ms)^### (FR-1[0-9]{2})\b.*?(?=^## |\z)'
))
foreach ($section in $gameplaySections) {
    $requirementId = $section.Groups[1].Value
    $inventoryRefs = @([regex]::Matches($section.Value, 'INV-[0-9]{3}') |
        ForEach-Object { $_.Value } | Sort-Object -Unique)
    if ($inventoryRefs.Count -eq 0) {
        $errors.Add("gameplay requirement $requirementId has no inventory reference")
        continue
    }
    foreach ($inventoryRef in $inventoryRefs) {
        if ($acceptedScope -notcontains $inventoryRef) {
            $errors.Add("gameplay requirement $requirementId references deferred/excluded scope $inventoryRef")
        }
    }
    $traceRow = $trace | Where-Object { $_.requirement_id -eq $requirementId } | Select-Object -First 1
    if ($null -eq $traceRow -or $traceRow.status -ne "gate2_relaxed_accepted") {
        $errors.Add("gameplay requirement $requirementId lacks gate2_relaxed_accepted trace row")
    }
}

$graphNodeIds = @($graph.nodes | ForEach-Object { $_.id })
$fitNodeIds = @($fit | ForEach-Object { $_.reference_state_id })
foreach ($nodeId in $graphNodeIds) {
    if ($fitNodeIds -notcontains $nodeId) {
        $errors.Add("fit registry missing graph node: $nodeId")
    }
}
foreach ($row in $fit) {
    if ($graphNodeIds -notcontains $row.reference_state_id) {
        $errors.Add("fit registry has unknown graph node: $($row.reference_state_id)")
    }
    if ($row.status -in @("deferred", "excluded")) {
        if (-not [string]::IsNullOrWhiteSpace($row.requirements) -or
            -not [string]::IsNullOrWhiteSpace($row.acceptance_ids)) {
            $errors.Add("deferred/excluded fit row $($row.reference_state_id) has production links")
        }
    } elseif ([string]::IsNullOrWhiteSpace($row.requirements) -or
        [string]::IsNullOrWhiteSpace($row.acceptance_ids)) {
        $errors.Add("accepted fit row $($row.reference_state_id) lacks requirement/acceptance links")
    }
}
if (@($fitNodeIds | Sort-Object -Unique).Count -ne $graphNodeIds.Count) {
    $errors.Add("fit registry contains duplicate or incomplete graph node coverage")
}

if ($errors.Count -gt 0) {
    [pscustomobject]@{
        status = "fail"
        mode = "relaxed_gate2"
        errors = $errors
    } | ConvertTo-Json -Depth 5
    exit 1
}

[pscustomobject]@{
    status = "pass"
    mode = "relaxed_gate2"
    requirements = $requirementIds.Count
    stories = $storyIds.Count
    acceptance = $acceptanceIds.Count
    trace_rows = $trace.Count
    fit_registry_nodes = $fit.Count
    accepted_scope = $acceptedScope
    deferred_scope = $deferredScope
    excluded_scope = $excludedScope
    visual_fit = "deferred_per_surface"
    human_gate2 = if ($gate2Status -eq "accepted_relaxed") { "accepted" } else { "pending" }
} | ConvertTo-Json -Compress
