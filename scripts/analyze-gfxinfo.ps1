[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string[]]$InputPaths,
    [double]$FrameBudgetMillis = 16.7,
    [string]$CaptureSession = 'one-device-boot'
)

$ErrorActionPreference = 'Stop'
if ($FrameBudgetMillis -le 0 -or [double]::IsNaN($FrameBudgetMillis) -or [double]::IsInfinity($FrameBudgetMillis)) {
    throw 'FrameBudgetMillis must be positive and finite.'
}
if ([string]::IsNullOrWhiteSpace($CaptureSession)) { throw 'CaptureSession must identify one device boot.' }
$seen = [System.Collections.Generic.HashSet[string]]::new()
$durations = [System.Collections.Generic.List[double]]::new()
$reports = [System.Collections.Generic.List[object]]::new()
$flagged = 0
$incomplete = 0
$duplicates = 0
$preWindow = 0
foreach ($inputPath in $InputPaths) {
    $resolved = (Resolve-Path -LiteralPath $inputPath).Path
    $columns = $null
    $totalFrames = $null
    $jankyFrames = $null
    $jankyPercent = $null
    $fileFrames = 0
    $filePreWindow = 0
    $statsSince = $null
    $windowStatsSince = $null
    $statsSinceBoundaries = [System.Collections.Generic.List[object]]::new()
    $processIdentity = 'unknown-process'
    $windowIdentity = 'unknown-window'
    $missingScope = $false
    foreach ($line in Get-Content -LiteralPath $resolved) {
        if ($line -match 'Graphics info for pid\s+(\d+)\s+\[([^\]]+)\]') {
            $processIdentity = "$($Matches[1]):$($Matches[2])"
            $windowIdentity = 'unknown-window'
            $statsSince = $null
            $windowStatsSince = $null
            $columns = $null
        }
        if ($line -match '^\s*Window:\s*(.+)$') {
            $windowIdentity = $Matches[1].Trim()
            $windowStatsSince = $null
            $columns = $null
        }
        if ($line -match '^\s*Stats since:\s*(\d+)ns\s*$') {
            $boundary = [long]$Matches[1]
            $boundaryScope = if ($windowIdentity -eq 'unknown-window') { 'process' } else { 'window' }
            if ($boundaryScope -eq 'process') { $statsSince = $boundary } else { $windowStatsSince = $boundary }
            $statsSinceBoundaries.Add([ordered]@{
                scope = $boundaryScope
                process = $processIdentity
                window = if ($boundaryScope -eq 'window') { $windowIdentity } else { $null }
                stats_since_ns = $boundary
            })
        }
        if ($line -match '^Total frames rendered:\s*(\d+)') { $totalFrames = [long]$Matches[1] }
        if ($line -match '^Janky frames:\s*(\d+)\s*\(([\d.]+)%\)') {
            $jankyFrames = [long]$Matches[1]
            $jankyPercent = [double]::Parse($Matches[2], [Globalization.CultureInfo]::InvariantCulture)
        }
        if ($line.StartsWith('Flags,') -and $line.Contains('IntendedVsync') -and $line.Contains('FrameCompleted')) {
            $columns = $line.TrimEnd(',').Split(',')
            continue
        }
        if ($null -eq $columns -or $line -notmatch '^\d+,') { continue }
        $values = $line.TrimEnd(',').Split(',')
        if ($values.Count -ne $columns.Count) { throw "Malformed framestats row in $resolved" }
        $fields = @{}
        for ($index = 0; $index -lt $columns.Count; $index++) { $fields[$columns[$index]] = $values[$index] }
        if ([long]$fields.Flags -ne 0) { $flagged++; continue }
        $intended = [long]$fields.IntendedVsync
        $completed = [long]$fields.FrameCompleted
        if ($intended -le 0 -or $completed -le $intended -or $completed -eq [long]::MaxValue) {
            $incomplete++
            continue
        }
        # A reset can retain an in-flight frame that started before the measured window.
        # Window-local stats override the process boundary; equality belongs to this window.
        # Keep flagged/incomplete classification first and do not deduplicate excluded rows.
        $effectiveStatsSince = if ($null -ne $windowStatsSince) { $windowStatsSince } else { $statsSince }
        if ($null -ne $effectiveStatsSince -and $intended -lt $effectiveStatsSince) {
            $preWindow++
            $filePreWindow++
            continue
        }
        # Consecutive dumps overlap, but independent processes/windows must never collapse together.
        # If scope is missing, deduplicate only inside the same file and expose the limitation.
        $scope = "${CaptureSession}:${processIdentity}:${windowIdentity}"
        if ($processIdentity -eq 'unknown-process' -or $windowIdentity -eq 'unknown-window') {
            $scope += ":$resolved"
            $missingScope = $true
        }
        if (-not $seen.Add("${scope}:${intended}:${completed}")) { $duplicates++; continue }
        $durations.Add(($completed - $intended) / 1000000.0)
        $fileFrames++
    }
    $reports.Add([ordered]@{
        path = $resolved
        sha256 = (Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash.ToLowerInvariant()
        unique_complete_unflagged_frames = $fileFrames
        pre_window_rows_excluded = $filePreWindow
        stats_since_ns = $statsSince
        stats_since_boundaries = @($statsSinceBoundaries.ToArray())
        platform_total_frames = $totalFrames
        platform_janky_frames = $jankyFrames
        platform_janky_percent = $jankyPercent
        missing_process_or_window_scope = $missingScope
    })
}
if ($durations.Count -eq 0) { throw 'No complete unflagged framestats frames found.' }
$sorted = @($durations | Sort-Object)
$p95 = $sorted[[int][Math]::Ceiling($sorted.Count * 0.95) - 1]
$middle = [int][Math]::Floor($sorted.Count / 2)
$median = if ($sorted.Count % 2 -eq 0) { ($sorted[$middle - 1] + $sorted[$middle]) / 2 } else { $sorted[$middle] }
$overBudget = @($durations | Where-Object { $_ -gt $FrameBudgetMillis }).Count
[ordered]@{
    measurement = 'gfxinfo FrameCompleted minus IntendedVsync; not display refresh rate or FPS'
    capture_session = $CaptureSession
    scope_note = 'Input files must belong to one device boot; deduplication includes PID/package/window.'
    sample_frames = $durations.Count
    p95_frame_ms = $p95
    median_frame_ms = $median
    maximum_frame_ms = $sorted[-1]
    frame_budget_ms = $FrameBudgetMillis
    frames_over_budget = $overBudget
    over_budget_percent = 100.0 * $overBudget / $durations.Count
    flagged_rows_excluded = $flagged
    incomplete_rows_excluded = $incomplete
    duplicate_rows_excluded = $duplicates
    pre_window_rows_excluded = $preWindow
    window_note = 'Complete unflagged rows before Stats since are excluded before deduplication; window-local boundaries override process boundaries, equality is included, and absent boundaries impose no time filter.'
    platform_jank_note = 'Platform deadline/GPU jank is separate; per-dump counters can overlap and are not summed.'
    environment_note = 'Interpret alongside separately recorded build, device and compilation identity. Debug measurements are diagnostic; an AVD result does not prove physical release-device performance.'
    reports = @($reports.ToArray())
} | ConvertTo-Json -Depth 6
