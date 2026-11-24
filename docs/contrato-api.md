# Contrato de APIs REST y WebSocket

> **Regla:** una vez aprobado por el equipo, no se cambia sin avisar a todos. Si cambias un endpoint, actualiza este archivo en el mismo commit.

## Puertos

| Servicio | Puerto | Base de datos |
|---|---|---|
| API Gateway | `8080` | — |
| AvalService | `8082` | `aval` |
| NotificarService | `8083` | `notificar` |
| IntegrationService | `8084` | `integracion` |
| MensajeriaService (REST + WebSocket) | `8085` | `mensajeria` |
| MoodleMock | `8091` | — |
| RegistroEscolarMock (SOAP) | `8092` | — |
| ServicioPlantilla | `8099` | — |
| RabbitMQ | `5672` (AMQP) · `15672` (panel) | — |
| PostgreSQL | `5432` | una por servicio |

## Reglas generales

- Todo es JSON (`Content-Type: application/json; charset=UTF-8`).
- Fechas en ISO-8601: `Instant` en UTC (`2026-11-24T07:15:00Z`), `LocalDateTime` sin zona.
- Los nombres de campos son los mismos que en `datos-semilla.json` y en el diagrama de clases.
- El cliente (Postman, y después la app web) entra **solo por el Gateway (8080)**, excepto el WebSocket del chat, que va directo a Mensajería (8085).

### Formato de error (todos los servicios)

```json
{ "error": "AVAL_YA_RESUELTO", "mensaje": "La entrega ENT-101 ya fue resuelta", "fecha": "2026-11-24T07:15:00Z" }
```

| Código HTTP | Cuándo |
|---|---|
| `200` | Consulta o actualización correcta |
| `201` | Se creó un recurso |
| `202` | Se aceptó y se procesa después |
| `400` | Faltan campos o tienen un valor inválido |
| `403` | El usuario no tiene permiso (por ejemplo, un tutor que no es del alumno) |
| `404` | No existe el recurso |
| `405` | Verbo HTTP equivocado |
| `409` | Conflicto de estado (por ejemplo, resolver un aval ya resuelto) |
| `502` | Un sistema externo respondió mal |
| `503` | Un servicio o sistema externo no responde |

---

## API Gateway (`8080`)

Reenvía cada prefijo a su servicio, sin cambiar la ruta:

| Prefijo | Destino |
|---|---|
| `/api/aval/**` | AvalService `8082` |
| `/api/notificar/**` | NotificarService `8083` |
| `/api/mensajeria/**` | MensajeriaService `8085` |
| `/api/tutores/**` | Gateway (lo responde él con los datos semilla) |

Si el destino no responde, regresa `503` con `{"error":"SERVICIO_NO_DISPONIBLE", ...}`. Tiene CORS habilitado.

### `GET /api/tutores/{idTutor}/hijos`
Hijos del tutor. `404` si el tutor no existe.
```json
[ { "matricula": "A001", "nombre": "Diego Ruiz" }, { "matricula": "A003", "nombre": "Mateo Ruiz" } ]
```

---

## AvalService (`8082`)

### `GET /api/aval/pendientes/{idTutor}`
Solicitudes en `PENDIENTE_AVAL` de los hijos del tutor.
```json
[ { "idEntrega": "ENT-101", "idTarea": "T-10", "matricula": "A001", "estado": "PENDIENTE_AVAL",
    "fechaSolicitud": "2026-11-24T06:00:00Z" } ]
```

### `GET /api/aval/solicitudes/{idEntrega}`
Una solicitud. `404` si no existe.
```json
{ "idEntrega": "ENT-101", "idTarea": "T-10", "matricula": "A001", "estado": "AVALADA",
  "fechaSolicitud": "2026-11-24T06:00:00Z", "fechaResolucion": "2026-11-24T07:15:00Z", "idTutor": "T001" }
```

### `POST /api/aval/resolver`
El tutor aprueba o rechaza. Publica `aval.resuelto`.
```json
{ "idEntrega": "ENT-101", "idTutor": "T001", "aprobado": true }
```
Respuesta `200`: la solicitud actualizada (igual que el GET).
Errores: `400` faltan campos · `403` el tutor no es del alumno · `404` no existe · `409` ya estaba resuelta.

> Las solicitudes se crean al consumir el evento `entrega.requiereAval`; no hay POST público para crearlas.

---

## NotificarService (`8083`) — buzón

### `GET /api/notificar/buzon/{idTutor}?matricula=A001&soloNoLeidas=true`
Alertas del tutor, de la más nueva a la más vieja. Los dos parámetros son opcionales.
```json
[ { "idAlerta": "AL-7f3a", "tipo": "CALIFICACION_REPROBATORIA", "matricula": "A002",
    "mensaje": "Sofia Valdez obtuvo 5.2 en Historia (P1)", "fechaEmision": "2026-11-24T07:20:00Z", "leida": false } ]
```

### `PATCH /api/notificar/buzon/{idAlerta}`
Marca la alerta como leída.
```json
{ "leida": true }
```
Respuesta `200`: la alerta actualizada. `404` si no existe.

### `GET /api/notificar/buzon/{idTutor}/no-leidas/total`
```json
{ "idTutor": "T002", "noLeidas": 3 }
```

---

## MensajeriaService (`8085`) — chat

### `GET /api/mensajeria/conversaciones/{idUsuario}`
Chats del usuario (tutor o profesor), del más reciente al más viejo.
```json
[ { "idConversacion": "CONV-1", "idTutor": "T001", "numEmpleado": "EMP-10", "matricula": "A001",
    "fechaCreacion": "2026-11-24T07:00:00Z", "ultimaActividad": "2026-11-24T07:30:00Z",
    "ultimoMensaje": "Gracias, maestro", "noLeidosTutor": 0, "noLeidosProfesor": 1, "activa": true } ]
```

### `POST /api/mensajeria/conversaciones`
Abre un chat (o regresa el que ya existe para ese tutor, profesor y alumno).
```json
{ "idTutor": "T001", "numEmpleado": "EMP-10", "matricula": "A001" }
```
Respuesta `201` (nueva) o `200` (ya existía). `403` si el tutor no es del alumno o el profesor no le da clase.

### `GET /api/mensajeria/conversaciones/{idConversacion}/mensajes?desde=2026-11-24T00:00:00Z`
Historial, del más viejo al más nuevo. `desde` es opcional.
```json
[ { "idMensaje": "MSG-1", "idConversacion": "CONV-1", "emisor": "T001", "contenido": "Buenas tardes",
    "fechaHora": "2026-11-24T07:30:00Z", "estadoMensaje": "LEIDO" } ]
```

### WebSocket `ws://localhost:8085/ws/chat/{idUsuario}`

Todos los mensajes son JSON con un campo `tipo`.

**Cliente → servidor**

| `tipo` | Campos | Para qué |
|---|---|---|
| `ENVIAR` | `idConversacion`, `contenido`, `idTemporal` | Mandar un mensaje |
| `ENTREGADO` | `idMensaje` | Confirmar que le llegó |
| `LEIDO` | `idConversacion` | Marcar como leídos todos los mensajes de la conversación |

**Servidor → cliente**

| `tipo` | Campos | Cuándo |
|---|---|---|
| `MENSAJE` | `idMensaje`, `idConversacion`, `emisor`, `contenido`, `fechaHora` | Llega un mensaje nuevo (o uno que estaba `EN_COLA` al conectarse) |
| `ESTADO` | `idMensaje`, `idTemporal`, `estadoMensaje` | Cambió el estado de un mensaje que mandaste (`ENVIADO`, `ENTREGADO`, `LEIDO`, `FALLIDO`) |
| `ALERTA` | los campos de `alerta.generada` | Notificar creó una alerta para este tutor |
| `ERROR` | `error`, `mensaje` | Algo salió mal (por ejemplo, conversación ajena) |

Ejemplo:
```json
{ "tipo": "ENVIAR", "idConversacion": "CONV-1", "contenido": "Buenas tardes", "idTemporal": "tmp-1" }
```

---

## IntegrationService (`8084`)

Uso interno y para pruebas (no pasa por el Gateway).

| Método y ruta | Qué hace |
|---|---|
| `POST /api/integracion/aval/sincronizar` | Lee Moodle y publica `entrega.requiereAval` de lo nuevo |
| `POST /api/integracion/calificaciones/sincronizar` | Lee Moodle, publica `calificacion.registrada` y `tarea.vencida`, y manda el reporte a Registro Escolar |
| `GET /api/integracion/incidencias` | Profesores que no entregaron calificaciones a tiempo |

Respuesta de las sincronizaciones:
```json
{ "eventosPublicados": 4, "timestamp": "2026-11-24T07:00:00Z" }
```
Si Moodle o Registro Escolar no responden: `503`.

---

## MoodleMock (`8091`) — REST

Responde con los datos de `datos-semilla.json`.

| Método y ruta | Regresa |
|---|---|
| `GET /mock/moodle/cursos` | Cursos con profesor y alumnos inscritos |
| `GET /mock/moodle/tareas?idCurso=` | Tareas con `fechaLimite` y `requiereAvalPadre` |
| `GET /mock/moodle/entregas?idTarea=` | Entregas de los alumnos |
| `GET /mock/moodle/calificaciones?periodo=` | Calificaciones por alumno |
| `GET /mock/moodle/entrega-calificaciones?periodo=` | Si cada profesor subió calificaciones a tiempo |

## RegistroEscolarMock (`8092`) — SOAP

- WSDL: `http://localhost:8092/registro-escolar?wsdl`
- Operación `enviarReporte(ReporteEntregaCalificaciones)` → `AcuseRecibo { idReporte, estatus, fechaRecepcion }`
- El XML del reporte se valida contra `reporte-calificaciones.xsd` (lo define la Persona 2 en la Etapa 1).
