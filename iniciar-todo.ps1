<#
.SYNOPSIS
    Levanta RabbitMQ, PostgreSQL y los 7 servicios de SistemaEscuela, cada uno en su propia ventana.

.DESCRIPTION
    1. Revisa que Docker este encendido y levanta docker-compose.
    2. Espera a que RabbitMQ acepte conexiones (AvalService se cae si no esta listo).
    3. Compila si falta algun servicio o si el codigo cambio desde la ultima compilacion (o siempre, con -Compilar).
    4. Abre los servicios en orden y espera a que cada uno escuche en su puerto antes del siguiente.
    5. Revisa que las 4 colas de RabbitMQ tengan su consumidor.

.PARAMETER Compilar
    Compila todo aunque los .jar parezcan al dia.

.PARAMETER Limpio
    Borra los datos de PostgreSQL y las colas de RabbitMQ antes de arrancar (para empezar la demo de cero).

.EXAMPLE
    .\iniciar-todo.ps1
    .\iniciar-todo.ps1 -Compilar
    .\iniciar-todo.ps1 -Limpio
#>
param(
    [switch]$Compilar,
    [switch]$Limpio
)

# En PowerShell 5.1, con 'Stop' cualquier texto que docker o mvn escriban en stderr detiene el script.
# Los errores se revisan a mano con $LASTEXITCODE.
$ErrorActionPreference = 'Continue'
$raiz = $PSScriptRoot
$modulos = Join-Path $raiz 'SistemaEscuela'

$servicios = @(
    @{ Nombre = 'MoodleMock';         Puerto = 8091; Modulo = 'MoodleMock' },
    @{ Nombre = 'RegistroEscolar';    Puerto = 8092; Modulo = 'RegistroEscolarMock'; Soap = $true },
    @{ Nombre = 'AvalService';        Puerto = 8082; Modulo = 'AvalService' },
    @{ Nombre = 'NotificarService';   Puerto = 8083; Modulo = 'NotificarService' },
    @{ Nombre = 'MensajeriaService';  Puerto = 8085; Modulo = 'MensajeriaService' },
    @{ Nombre = 'IntegrationService'; Puerto = 8084; Modulo = 'IntegrationService' },
    @{ Nombre = 'API Gateway';        Puerto = 8080; Modulo = 'Gateaway' }
)
$colas = 'aval.eventos', 'notificar.eventos', 'mensajeria.eventos', 'integracion.eventos'

function Escribir-Paso([string]$texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }
function Escribir-Ok([string]$texto) { Write-Host "    OK  $texto" -ForegroundColor Green }
function Escribir-Aviso([string]$texto) { Write-Host "    !!  $texto" -ForegroundColor Yellow }
function Detener-Con([string]$texto) { Write-Host "`n    ERROR  $texto`n" -ForegroundColor Red; exit 1 }

function Probar-Puerto([int]$puerto) {
    $cliente = New-Object System.Net.Sockets.TcpClient
    try {
        $intento = $cliente.BeginConnect('127.0.0.1', $puerto, $null, $null)
        return ($intento.AsyncWaitHandle.WaitOne(500) -and $cliente.Connected)
    } catch {
        return $false
    } finally {
        $cliente.Close()
    }
}

function Esperar([scriptblock]$condicion, [int]$segundos) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        if (& $condicion) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Buscar-Maven {
    $enPath = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($enPath) { return $enPath.Source }
    $candidatos = Get-ChildItem 'C:\Program Files\*NetBeans*\java\maven\bin\mvn.cmd', 'C:\Program Files\*NetBeans*\netbeans\java\maven\bin\mvn.cmd' -ErrorAction SilentlyContinue
    if ($candidatos) { return $candidatos[0].FullName }
    return $null
}

# ---------------------------------------------------------------- 1. Docker
Escribir-Paso 'Revisando Docker'
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    $docker = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
    if (-not (Test-Path $docker)) { Detener-Con 'Docker Desktop no esta instalado.' }
    Escribir-Aviso 'Docker no esta encendido; abriendo Docker Desktop (puede tardar un minuto)...'
    Start-Process $docker
    $listo = Esperar { docker info *> $null; $LASTEXITCODE -eq 0 } 180
    if (-not $listo) { Detener-Con 'Docker no arranco. Abre Docker Desktop a mano y vuelve a correr el script.' }
}
Escribir-Ok 'Docker encendido'

Set-Location $raiz
if ($Limpio) {
    Escribir-Paso 'Borrando datos anteriores (-Limpio)'
    docker compose down -v *> $null
    Escribir-Ok 'Bases y colas borradas'
}

Escribir-Paso 'Levantando RabbitMQ y PostgreSQL'
docker compose up -d *> $null
if ($LASTEXITCODE -ne 0) { Detener-Con 'Fallo docker compose up. Revisa docker-compose.yml.' }

$listo = Esperar {
    $estado = docker inspect -f '{{.State.Health.Status}}' escuela-rabbitmq escuela-postgres 2>$null
    ($estado -join ' ') -eq 'healthy healthy'
} 120
if (-not $listo) { Detener-Con 'RabbitMQ o PostgreSQL no quedaron saludables. Revisa con: docker compose logs' }

# El healthcheck pasa un poco antes de que RabbitMQ acepte conexiones AMQP.
$listo = Esperar {
    docker exec escuela-rabbitmq rabbitmq-diagnostics -q check_port_connectivity *> $null
    $LASTEXITCODE -eq 0
} 60
if (-not $listo) { Detener-Con 'RabbitMQ no acepta conexiones en el puerto 5672.' }
Start-Sleep -Seconds 3
Escribir-Ok 'RabbitMQ (5672) y PostgreSQL (5432) listos'

# ---------------------------------------------------------------- 2. Puertos libres
Escribir-Paso 'Revisando que los puertos esten libres'
$ocupados = $servicios | Where-Object { Probar-Puerto $_.Puerto }
if ($ocupados) {
    $lista = ($ocupados | ForEach-Object { "$($_.Nombre) ($($_.Puerto))" }) -join ', '
    Detener-Con "Ya hay algo corriendo en: $lista. Usa .\detener-todo.ps1 primero."
}
Escribir-Ok 'Puertos 8080-8092 libres'

# ---------------------------------------------------------------- 3. Compilar
$maven = Buscar-Maven
if (-not $maven) { Detener-Con 'No encontre Maven (ni en el PATH ni en NetBeans).' }

function Necesita-Compilar($servicio) {
    $carpeta = Join-Path $modulos $servicio.Modulo
    if ($servicio.Soap) {
        $jar = Get-ChildItem (Join-Path $carpeta 'target\*.jar') -ErrorAction SilentlyContinue | Select-Object -First 1
    } else {
        $jar = Get-Item (Join-Path $carpeta 'target\quarkus-app\quarkus-run.jar') -ErrorAction SilentlyContinue
    }
    if (-not $jar) { return $true }
    # Si hay codigo mas nuevo que el .jar (por ejemplo, despues de un git pull), hay que recompilar.
    $fuentes = @(
        (Join-Path $carpeta 'src'), (Join-Path $carpeta 'pom.xml'),
        (Join-Path $modulos 'escuela-common\src'), (Join-Path $modulos 'pom.xml')
    )
    $masNuevo = Get-ChildItem $fuentes -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { $_.LastWriteTime -gt $jar.LastWriteTime } | Select-Object -First 1
    return [bool]$masNuevo
}

$faltan = $servicios | Where-Object { Necesita-Compilar $_ }
if ($faltan -and -not $Compilar) {
    Escribir-Aviso ("Cambios sin compilar en: " + (($faltan | ForEach-Object { $_.Nombre }) -join ', '))
}
if ($Compilar -or $faltan) {
    Escribir-Paso 'Compilando (puede tardar unos minutos)'
    Push-Location $modulos
    & $maven -B -q install -DskipTests
    $codigo = $LASTEXITCODE
    Pop-Location
    if ($codigo -ne 0) { Detener-Con 'Fallo la compilacion. Corre "mvn install" en SistemaEscuela para ver el error.' }
    Escribir-Ok 'Compilado'
} else {
    Escribir-Ok 'Ya estaba compilado y al dia'
}

# ---------------------------------------------------------------- 4. Servicios
Escribir-Paso 'Arrancando servicios (cada uno abre su ventana)'
foreach ($s in $servicios) {
    $titulo = "$($s.Nombre) :$($s.Puerto)"
    if ($s.Soap) {
        $carpeta = Join-Path $modulos $s.Modulo
        $comando = "`$host.UI.RawUI.WindowTitle = '$titulo'; & '$maven' -q exec:java '-Dexec.mainClass=com.itson.registroescolarmock.RegistroEscolarEndpoint'"
    } else {
        $carpeta = $modulos
        $comando = "`$host.UI.RawUI.WindowTitle = '$titulo'; java -jar '$($s.Modulo)\target\quarkus-app\quarkus-run.jar'"
    }
    Start-Process powershell -WorkingDirectory $carpeta -ArgumentList '-NoExit', '-NoProfile', '-Command', $comando | Out-Null

    $tiempo = 60
    if ($s.Soap) { $tiempo = 120 }
    $puerto = $s.Puerto
    if (Esperar { Probar-Puerto $puerto } $tiempo) {
        Escribir-Ok "$($s.Nombre) en http://localhost:$puerto"
    } else {
        Detener-Con "$($s.Nombre) no arranco en $tiempo s. Revisa su ventana ('$titulo')."
    }
}

# ---------------------------------------------------------------- 5. Colas
Escribir-Paso 'Revisando consumidores de RabbitMQ'
$credencial = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('escuela:escuela'))
$todos = Esperar {
    try {
        $info = Invoke-RestMethod 'http://localhost:15672/api/queues' -Headers @{ Authorization = "Basic $credencial" } -TimeoutSec 3
        $sinConsumidor = $colas | Where-Object { $c = $_; -not ($info | Where-Object { $_.name -eq $c -and $_.consumers -ge 1 }) }
        -not $sinConsumidor
    } catch { $false }
} 30
if ($todos) {
    Escribir-Ok ($colas -join ', ')
} else {
    Escribir-Aviso 'Alguna cola no tiene consumidor todavia. Revisa en http://localhost:15672 > Queues'
}

Write-Host ''
Write-Host '  Todo arriba.' -ForegroundColor Green
Write-Host '  Gateway:   http://localhost:8080/api'
Write-Host '  RabbitMQ:  http://localhost:15672  (escuela / escuela)'
Write-Host '  Chat:      ws://localhost:8085/ws/chat/{idUsuario}'
Write-Host '  Probar:    Invoke-RestMethod -Method Post -ContentType application/json http://localhost:8084/api/integracion/aval/sincronizar'
Write-Host '  Apagar:    .\detener-todo.ps1'
Write-Host ''
