$ErrorActionPreference = 'Stop'

$listeners = Get-NetTCPConnection -LocalPort 5000 -State Listen -ErrorAction SilentlyContinue

if (-not $listeners) {
    Write-Host 'No backend process is listening on port 5000.'
    exit 0
}

$stopped = @()
foreach ($listener in $listeners) {
    try {
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction Stop
        $stopped += $listener.OwningProcess
    } catch {
        Write-Warning "Could not stop process $($listener.OwningProcess): $($_.Exception.Message)"
    }
}

if ($stopped.Count -gt 0) {
    $unique = $stopped | Sort-Object -Unique
    Write-Host ("Stopped backend process(es): " + ($unique -join ', '))
}
