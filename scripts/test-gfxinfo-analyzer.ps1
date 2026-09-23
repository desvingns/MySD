$ErrorActionPreference = 'Stop'
$fixture = Join-Path $PSScriptRoot 'fixtures/gfxinfo-synthetic.txt'
$analyzer = Join-Path $PSScriptRoot 'analyze-gfxinfo.ps1'
$result = (& $analyzer -InputPaths $fixture | ConvertFrom-Json)
if ($result.sample_frames -ne 4 -or $result.p95_frame_ms -ne 30 -or $result.median_frame_ms -ne 15) {
    throw 'Named-column duration/quantile calculation failed.'
}
if ($result.flagged_rows_excluded -ne 1 -or $result.incomplete_rows_excluded -ne 1 -or $result.duplicate_rows_excluded -ne 1) {
    throw 'Frame filtering/deduplication failed.'
}
if ($result.frames_over_budget -ne 2 -or $result.over_budget_percent -ne 50) {
    throw 'Fixed-budget fraction calculation failed.'
}
if ($result.reports[0].platform_janky_frames -ne 2 -or $result.reports[0].platform_janky_percent -ne 33.33) {
    throw 'Platform jank must be preserved separately.'
}
$repeated = (& $analyzer -InputPaths @($fixture, $fixture) | ConvertFrom-Json)
if ($repeated.sample_frames -ne 4 -or $repeated.duplicate_rows_excluded -ne 6) {
    throw 'Overlapping report windows must not double-count frames.'
}
$boundaryFixture = Join-Path $PSScriptRoot 'fixtures/gfxinfo-window-boundary-synthetic.txt'
$bounded = (& $analyzer -InputPaths $boundaryFixture | ConvertFrom-Json)
if ($bounded.sample_frames -ne 3 -or $bounded.p95_frame_ms -ne 30 -or $bounded.median_frame_ms -ne 20) {
    throw 'Pre-reset frames must be excluded and frames exactly at either boundary must be included.'
}
if ($bounded.pre_window_rows_excluded -ne 2 -or $bounded.reports[0].pre_window_rows_excluded -ne 2) {
    throw 'Pre-window exclusions must be reported globally and per file.'
}
if ($bounded.reports[0].stats_since_ns -ne 2000000000 -or
    $bounded.reports[0].stats_since_boundaries.Count -ne 2 -or
    $bounded.reports[0].stats_since_boundaries[0].scope -ne 'process' -or
    $bounded.reports[0].stats_since_boundaries[1].scope -ne 'window' -or
    $bounded.reports[0].stats_since_boundaries[1].stats_since_ns -ne 1000000000) {
    throw 'Process and overriding window Stats since boundaries must be preserved.'
}
if ($bounded.flagged_rows_excluded -ne 1 -or $bounded.incomplete_rows_excluded -ne 1 -or
    $bounded.duplicate_rows_excluded -ne 1 -or $bounded.frame_budget_ms -ne 16.7 -or
    $bounded.reports[0].platform_total_frames -ne 8 -or $bounded.reports[0].platform_janky_frames -ne 4) {
    throw 'Window filtering must preserve existing filter counters, budget, and raw platform counters.'
}
[ordered]@{status = 'pass'; command = 'gfxinfo analyzer synthetic self-test'; scenarios = 3; device_evidence = $false} | ConvertTo-Json -Compress
