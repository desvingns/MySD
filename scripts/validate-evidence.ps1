[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$graphPath = Join-Path $repoRoot "spec/evidence/state-graph.v1.json"
$claimsPath = Join-Path $repoRoot "spec/evidence/mechanic-claims.csv"
$evidencePath = Join-Path $repoRoot "spec/evidence/evidence-index.csv"

$graph = Get-Content -Raw -LiteralPath $graphPath | ConvertFrom-Json
$claims = @(Import-Csv -LiteralPath $claimsPath)
$evidenceRows = @(Import-Csv -LiteralPath $evidencePath)
$errors = [System.Collections.Generic.List[string]]::new()

if ($graph.schema_version -ne "state-graph.v1") {
    $errors.Add("schema_version must equal state-graph.v1")
}

$allowedKinds = @("screen", "overlay", "battle_phase", "meta_state")
$nodeIds = @($graph.nodes | ForEach-Object { $_.id })
if (($nodeIds | Sort-Object -Unique).Count -ne $nodeIds.Count) {
    $errors.Add("node ids must be unique")
}

$affordanceIds = [System.Collections.Generic.List[string]]::new()
foreach ($node in $graph.nodes) {
    if ($node.id -notmatch '^ST-[0-9]{4}$') {
        $errors.Add("invalid node id: $($node.id)")
    }
    if ($allowedKinds -notcontains $node.kind) {
        $errors.Add("invalid node kind at $($node.id): $($node.kind)")
    }
    if ($null -ne $node.parent -and $nodeIds -notcontains $node.parent) {
        $errors.Add("missing parent $($node.parent) for $($node.id)")
    }
    if ($node.confidence -lt 0 -or $node.confidence -gt 1) {
        $errors.Add("confidence out of range at $($node.id)")
    }
    if (@("observed", "inferred") -notcontains $node.source) {
        $errors.Add("invalid node source at $($node.id): $($node.source)")
    }
    if ($null -eq $node.signatures.structural -or
        $null -eq $node.signatures.visual -or
        $null -eq $node.signatures.semantic) {
        $errors.Add("node $($node.id) is missing one or more signatures")
    }
    foreach ($affordance in @($node.visible_affordances)) {
        $affordanceIds.Add($affordance.id)
        if ($affordance.id -notmatch '^AF-[0-9]{4}$') {
            $errors.Add("invalid affordance id at $($node.id): $($affordance.id)")
        }
        if (@("edge", "deviation", "blocker", "unmatched") -notcontains $affordance.coverage_status) {
            $errors.Add("invalid affordance coverage at $($affordance.id)")
        }
        if ($affordance.coverage_status -ne "unmatched" -and
            [string]::IsNullOrWhiteSpace($affordance.coverage_ref)) {
            $errors.Add("affordance $($affordance.id) is missing coverage_ref")
        }
    }
}
if (($affordanceIds | Sort-Object -Unique).Count -ne $affordanceIds.Count) {
    $errors.Add("affordance ids must be unique")
}

$edgeIds = @($graph.edges | ForEach-Object { $_.id })
if (($edgeIds | Sort-Object -Unique).Count -ne $edgeIds.Count) {
    $errors.Add("edge ids must be unique")
}
foreach ($edge in $graph.edges) {
    if ($edge.id -notmatch '^ED-[0-9]{4}$') {
        $errors.Add("invalid edge id: $($edge.id)")
    }
    if ($nodeIds -notcontains $edge.from) {
        $errors.Add("edge $($edge.id) has unknown from node $($edge.from)")
    }
    if ($null -ne $edge.to -and $nodeIds -notcontains $edge.to) {
        $errors.Add("edge $($edge.id) has unknown to node $($edge.to)")
    }
    if (@("observed", "inferred") -notcontains $edge.source) {
        $errors.Add("edge $($edge.id) has invalid source $($edge.source)")
    }
    if ($edge.confidence -lt 0 -or $edge.confidence -gt 1) {
        $errors.Add("edge confidence out of range at $($edge.id)")
    }
    if (@("navigation", "phase", "meta", "service_adapter", "blocked") -notcontains $edge.classification) {
        $errors.Add("edge $($edge.id) has invalid classification $($edge.classification)")
    }
    if ($edge.wait_ms.samples -gt 0 -and
        ($null -eq $edge.wait_ms.min -or $null -eq $edge.wait_ms.max -or
         $edge.wait_ms.min -gt $edge.wait_ms.max)) {
        $errors.Add("edge $($edge.id) has invalid wait_ms range")
    }
    if ($edge.source -eq "observed" -and
        (@($edge.before_evidence_ids).Count -eq 0 -or @($edge.after_evidence_ids).Count -eq 0)) {
        $errors.Add("observed edge $($edge.id) requires before and after evidence")
    }
}

foreach ($node in $graph.nodes) {
    foreach ($affordance in @($node.visible_affordances)) {
        if ($affordance.coverage_status -eq "edge" -and $edgeIds -notcontains $affordance.coverage_ref) {
            $errors.Add("affordance $($affordance.id) references unknown edge $($affordance.coverage_ref)")
        }
    }
}

$observationIds = @($graph.observations | ForEach-Object { $_.id })
if (($observationIds | Sort-Object -Unique).Count -ne $observationIds.Count) {
    $errors.Add("observation ids must be unique")
}
foreach ($observation in $graph.observations) {
    if ($observation.id -notmatch '^OB-[0-9]{4}$') {
        $errors.Add("invalid observation id: $($observation.id)")
    }
    if ($nodeIds -notcontains $observation.node_id) {
        $errors.Add("observation $($observation.id) references unknown node $($observation.node_id)")
    }
}
foreach ($edge in $graph.edges) {
    foreach ($costObservationId in @($edge.cost_observation_ids)) {
        if ($observationIds -notcontains $costObservationId) {
            $errors.Add("edge $($edge.id) references unknown cost observation $costObservationId")
        }
    }
}

$expectedClaimsHeader = "claim_id,claim,hypothesis,controlled_variables,sample_count,supporting_evidence_ids,contradicting_evidence_ids,confidence,status,future_fr_links,future_eng_links,notes"
$actualClaimsHeader = Get-Content -LiteralPath $claimsPath -TotalCount 1
if ($actualClaimsHeader -ne $expectedClaimsHeader) {
    $errors.Add("mechanic-claims.csv header does not match v1 contract")
}

$expectedEvidenceHeader = "evidence_id,source_type,local_relative_path,sha256,captured_at_utc,sanitized_summary,ip_privacy_review,status"
$actualEvidenceHeader = Get-Content -LiteralPath $evidencePath -TotalCount 1
if ($actualEvidenceHeader -ne $expectedEvidenceHeader) {
    $errors.Add("evidence-index.csv header does not match v1 contract")
}

$evidenceIds = @($evidenceRows | ForEach-Object { $_.evidence_id })
if (($evidenceIds | Sort-Object -Unique).Count -ne $evidenceIds.Count) {
    $errors.Add("evidence ids must be unique")
}
foreach ($evidence in $evidenceRows) {
    if ($evidence.evidence_id -notmatch '^EV-[0-9]{4}$') {
        $errors.Add("invalid evidence id: $($evidence.evidence_id)")
    }
    if ($evidence.sha256 -notmatch '^[a-fA-F0-9]{64}$') {
        $errors.Add("evidence $($evidence.evidence_id) has invalid sha256")
    }
    if ($evidence.local_relative_path -notmatch '^(\.reference-local/|\.reference-local\\)') {
        $errors.Add("evidence $($evidence.evidence_id) must point into .reference-local")
    }
    if (@("pass", "pending", "fail") -notcontains $evidence.ip_privacy_review) {
        $errors.Add("evidence $($evidence.evidence_id) has invalid ip_privacy_review")
    }
}

function Test-EvidenceReference {
    param(
        [string]$EvidenceId,
        [string]$Context
    )
    if ($evidenceIds -notcontains $EvidenceId) {
        $errors.Add("$Context references unknown evidence $EvidenceId")
        return
    }
    $row = $evidenceRows | Where-Object { $_.evidence_id -eq $EvidenceId } | Select-Object -First 1
    if ($row.ip_privacy_review -ne "pass") {
        $errors.Add("$Context references evidence without passed IP/privacy review: $EvidenceId")
    }
}

if ($null -ne $graph.reference.capture_meta_evidence_id) {
    Test-EvidenceReference -EvidenceId $graph.reference.capture_meta_evidence_id -Context "reference capture metadata"
}
foreach ($node in $graph.nodes) {
    foreach ($evidenceId in @($node.evidence_ids)) {
        Test-EvidenceReference -EvidenceId $evidenceId -Context "node $($node.id)"
    }
}
foreach ($edge in $graph.edges) {
    foreach ($evidenceId in @($edge.before_evidence_ids) + @($edge.after_evidence_ids)) {
        Test-EvidenceReference -EvidenceId $evidenceId -Context "edge $($edge.id)"
    }
}
foreach ($observation in $graph.observations) {
    Test-EvidenceReference -EvidenceId $observation.evidence_id -Context "observation $($observation.id)"
}

$claimIds = @($claims | ForEach-Object { $_.claim_id })
if (($claimIds | Sort-Object -Unique).Count -ne $claimIds.Count) {
    $errors.Add("mechanic claim ids must be unique")
}
foreach ($claim in $claims) {
    if ($claim.claim_id -notmatch '^CL-[0-9]{4}$') {
        $errors.Add("invalid mechanic claim id: $($claim.claim_id)")
    }
    if (@("candidate", "testing", "open_question", "supported", "contradicted", "accepted") -notcontains $claim.status) {
        $errors.Add("claim $($claim.claim_id) has invalid status $($claim.status)")
    }
    $claimConfidence = 0.0
    if (-not [double]::TryParse(
        $claim.confidence,
        [Globalization.NumberStyles]::Float,
        [Globalization.CultureInfo]::InvariantCulture,
        [ref]$claimConfidence
    ) -or $claimConfidence -lt 0 -or $claimConfidence -gt 1) {
        $errors.Add("claim $($claim.claim_id) has invalid confidence")
        continue
    }
    $supportingEvidenceIds = @($claim.supporting_evidence_ids -split ';' |
        ForEach-Object { $_.Trim() } | Where-Object { $_ })
    $contradictingEvidenceIds = @($claim.contradicting_evidence_ids -split ';' |
        ForEach-Object { $_.Trim() } | Where-Object { $_ })
    foreach ($evidenceId in $supportingEvidenceIds + $contradictingEvidenceIds) {
        Test-EvidenceReference -EvidenceId $evidenceId -Context "claim $($claim.claim_id)"
    }
    if ($claimConfidence -ge 0.8) {
        $sampleCount = 0
        if (-not [int]::TryParse($claim.sample_count, [ref]$sampleCount) -or $sampleCount -lt 1) {
            $errors.Add("high-confidence claim $($claim.claim_id) requires sample_count >= 1")
        }
        if ([string]::IsNullOrWhiteSpace($claim.controlled_variables)) {
            $errors.Add("high-confidence claim $($claim.claim_id) requires controlled_variables")
        }
        if ($supportingEvidenceIds.Count -eq 0) {
            $errors.Add("high-confidence claim $($claim.claim_id) requires supporting evidence")
        }
    }
}

$gateReady = @("ready", "accepted") -contains $graph.coverage.gate1_status
if ($gateReady) {
    if ([string]::IsNullOrWhiteSpace($graph.reference.capture_meta_evidence_id)) {
        $errors.Add("Gate 1 requires reference.capture_meta_evidence_id")
    }
    $missingRoots = @($graph.coverage.root_routes_expected |
        Where-Object { $graph.coverage.root_routes_seen -notcontains $_ })
    if ($missingRoots.Count -gt 0) {
        $errors.Add("Gate 1 missing root routes: $($missingRoots -join ',')")
    }
    $missingTerminalStates = @("victory", "defeat" |
        Where-Object { $graph.coverage.terminal_states_seen -notcontains $_ })
    if ($missingTerminalStates.Count -gt 0) {
        $errors.Add("Gate 1 missing terminal states: $($missingTerminalStates -join ',')")
    }
    if (@($graph.coverage.unmatched_affordance_ids).Count -gt 0) {
        $errors.Add("Gate 1 has unmatched affordances: $($graph.coverage.unmatched_affordance_ids -join ',')")
    }
    $nodeUnmatched = @($graph.nodes.visible_affordances |
        Where-Object { $_.coverage_status -eq "unmatched" })
    if ($nodeUnmatched.Count -gt 0) {
        $errors.Add("Gate 1 contains node affordances marked unmatched")
    }
    if ($graph.coverage.plateau_iterations -lt 6) {
        $errors.Add("Gate 1 requires at least six discovery-plateau iterations")
    }
    foreach ($claim in $claims) {
        $confidence = 0.0
        if ([double]::TryParse(
            $claim.confidence,
            [Globalization.NumberStyles]::Float,
            [Globalization.CultureInfo]::InvariantCulture,
            [ref]$confidence
        ) -and $confidence -lt 0.8 -and $claim.status -ne "open_question") {
            $errors.Add("low-confidence claim $($claim.claim_id) must be open_question at Gate 1")
        }
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
    nodes = $graph.nodes.Count
    edges = $graph.edges.Count
    observations = $graph.observations.Count
    gate1_status = $graph.coverage.gate1_status
} | ConvertTo-Json -Compress
