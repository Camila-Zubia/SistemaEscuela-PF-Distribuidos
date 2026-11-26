<#
.SYNOPSIS
    Apaga los 7 servicios de SistemaEscuela, cierra sus ventanas y, si se pide, tambien RabbitMQ y PostgreSQL.

.PARAMETER Docker
    Tambien apaga los contenedores (los datos se conservan).

.PARAMETER Limpio
    Apaga los contenedores y borra sus datos (bases y colas).

.EXAMPLE
    .\detener-todo.ps1
    .\detener-todo.ps1 -Docker
    .\detener-todo.ps1 -Limpio
#>
param(
    [switch]$Docker,
    [switch]$Limpio
)

$servicios = @(
    @{ Nombre = 'API Gateway';        Puerto = 8080 },
    @{ Nombre = 'IntegrationService'; Puerto = 8084 },
    @{ Nombre = 'MensajeriaService';  Puerto = 8085 },
    @{ Nombre = 'NotificarService';   Puerto = 8083 },
    @{ Nombre = 'AvalService';        Puerto = 8082 },
    @{ Nombre = 'RegistroEscolar';    Puerto = 8092 },
    @{ Nombre = 'MoodleMock';         Puerto = 8091 }
)

Write-Host "`n==> Apagando servicios" -ForegroundColor Cyan
foreach ($s in $servicios) {
    $procesos = Get-NetTCPConnection -State Listen -LocalPort $s.Puerto -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
    if (-not $procesos) {
        Write-Host "    --  $($s.Nombre) no estaba corriendo" -ForegroundColor DarkGray
        continue
    }
    $procesos | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
    Write-Host "    OK  $($s.Nombre) ($($s.Puerto))" -ForegroundColor Green
}

# Cierra las ventanas que abrio iniciar-todo.ps1 (se reconocen por el comando que ejecutan).
$ventanas = Get-CimInstance Win32_Process -Filter "Name = 'powershell.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -match 'quarkus-run\.jar|registroescolarmock\.RegistroEscolarEndpoint' }
$ventanas | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
if ($ventanas) { Write-Host "    OK  $(@($ventanas).Count) ventanas cerradas" -ForegroundColor Green }

if ($Docker -or $Limpio) {
    Set-Location $PSScriptRoot
    if ($Limpio) {
        Write-Host "`n==> Apagando RabbitMQ y PostgreSQL y borrando sus datos" -ForegroundColor Cyan
        docker compose down -v 2>&1 | Out-Null
    } else {
        Write-Host "`n==> Apagando RabbitMQ y PostgreSQL (los datos se conservan)" -ForegroundColor Cyan
        docker compose down 2>&1 | Out-Null
    }
    Write-Host '    OK' -ForegroundColor Green
}
Write-Host ''
