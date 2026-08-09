<#
.SYNOPSIS
    Finaliza qualquer processo que esteja escutando na porta informada.

.DESCRIPTION
    Usado para evitar o erro "Web server failed to start. Port 8080 was already in use.",
    que acontece quando uma execucao anterior da aplicacao (ex.: debug desconectado na IDE
    sem matar o processo) fica presa ocupando a porta.

.PARAMETER Port
    Porta a ser liberada. Padrao: 8080 (porta padrao do Spring Boot / server.port).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/free-port.ps1
    powershell -ExecutionPolicy Bypass -File scripts/free-port.ps1 -Port 8080
#>
param(
    [int]$Port = 8080
)

$connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

if (-not $connections) {
    Write-Output "Porta $Port ja esta livre."
    exit 0
}

$processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique

foreach ($processId in $processIds) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        Write-Output "Finalizando processo '$($process.ProcessName)' (PID $processId) que ocupava a porta $Port..."
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

Start-Sleep -Milliseconds 500
Write-Output "Porta $Port liberada."
