[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$tracked = @(& git -C $repoRoot ls-files)
if ($LASTEXITCODE -ne 0) {
    throw "git ls-files failed"
}

$historyLines = @(& git -C $repoRoot rev-list --objects --all 2>$null)
$historyPaths = @($historyLines | ForEach-Object {
    $parts = $_ -split " ", 2
    if ($parts.Count -eq 2) { $parts[1] }
})
$allPaths = @($tracked + $historyPaths | Where-Object { $_ } | Sort-Object -Unique)

$forbidden = @(
    '(?i)(^|/)\.reference-local(/|$)',
    '(?i)(^|/)(reference-raw|ui-dumps|screen-recordings)(/|$)',
    '(?i)\.(apk|aab|apks|xapk|mp4|webm|mov)$',
    '(?i)^spec/evidence/.*\.(png|jpe?g|webp|gif|bmp|xml|html?|pdf|zip|bin|dex|arsc|ogg|mp3|wav|m4a)$',
    '(?i)(^|/)(ui|window|accessibility)[-_]?dump[^/]*\.(xml|json|txt)$'
)
$violations = @($allPaths | Where-Object {
    $path = $_
    $forbidden | Where-Object { $path -match $_ }
})

$oversized = @()
foreach ($relative in $tracked) {
    $absolute = Join-Path $repoRoot $relative
    if ((Test-Path -LiteralPath $absolute -PathType Leaf) -and
        (Get-Item -LiteralPath $absolute).Length -gt 5MB) {
        $oversized += $relative
    }
}

$provenancePath = Join-Path $repoRoot "assets/provenance.csv"
$provenanceRows = if (Test-Path -LiteralPath $provenancePath) {
    @(Import-Csv -LiteralPath $provenancePath)
} else {
    @()
}
$creativePaths = @($allPaths | Where-Object {
    $_ -match '(?i)\.(png|jpe?g|webp|gif|svg|ogg|mp3|wav|m4a|ttf|otf)$'
})
$provenanceViolations = [System.Collections.Generic.List[string]]::new()
foreach ($creativePath in $creativePaths) {
    $row = $provenanceRows | Where-Object { $_.path -eq $creativePath } | Select-Object -First 1
    if ($null -eq $row) {
        $provenanceViolations.Add("$creativePath (missing provenance row)")
        continue
    }
    if (@("original", "generated", "licensed") -notcontains $row.origin) {
        $provenanceViolations.Add("$creativePath (invalid origin: $($row.origin))")
    }
    if ($row.review_status -ne "approved") {
        $provenanceViolations.Add("$creativePath (review_status is not approved)")
    }
    if ([string]::IsNullOrWhiteSpace($row.license) -or $row.license -eq "unknown") {
        $provenanceViolations.Add("$creativePath (missing/unknown license)")
    }
}

if ($violations.Count -gt 0 -or $oversized.Count -gt 0 -or $provenanceViolations.Count -gt 0) {
    [pscustomobject]@{
        status = "fail"
        forbidden_paths = $violations
        oversized_tracked_files = $oversized
        creative_asset_provenance = $provenanceViolations
    } | ConvertTo-Json -Depth 4
    exit 1
}

[pscustomobject]@{
    status = "pass"
    tracked_files = $tracked.Count
    history_paths_checked = $historyPaths.Count
    creative_assets_checked = $creativePaths.Count
} | ConvertTo-Json -Compress
