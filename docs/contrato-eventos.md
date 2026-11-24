# Contrato de eventos

Todos los eventos viajan por **RabbitMQ** (AMQP, puerto `5672`) en el exchange **`escuela.eventos`** de tipo **topic**.
Las clases Java de este contrato están en `escuela-common`:

- Routing keys: `com.itson.escuela.common.eventos.RoutingKeys`
- Contenido de cada evento: `com.itson.escuela.common.eventos.DatosEventos`
- Publicar: `PublicadorEventos` · Consumir: `ConsumidorEventos`

> **Regla:** si cambias un evento, actualiza este archivo y las clases de `escuela-common` en el mismo commit, y avisa al equipo.

## Sobre común (`Evento<T>`)

Todos los eventos tienen la misma forma; solo cambia `datos`.

```json
{
  "idEvento": "6f1c2e0a-8a3b-4c1e-9d55-2b7f0e4a1c90",
  "tipo": "tarea.vencida",
  "fecha": "2026-11-24T06:00:00Z",
  "origen": "integration-service",
  "datos": { }
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `idEvento` | String (UUID) | Único. El consumidor lo usa para no procesar dos veces el mismo evento |
| `tipo` | String | La routing key |
| `fecha` | Instant (ISO-8601, UTC) | Cuándo se generó |
| `origen` | String | Servicio que lo publicó |
| `datos` | Objeto | Contenido del evento (tablas de abajo) |

Propiedades del mensaje AMQP: `content_type=application/json`, `delivery_mode=2` (persistente), `message_id=idEvento`.
Fechas: `Instant` en UTC con `Z`; `LocalDateTime` sin zona (`2026-11-15T23:59:00`).

## Garantía de entrega

- **At-least-once:** el consumidor manda `ack` solo después de procesar bien el evento. Si se cae antes, RabbitMQ lo vuelve a entregar.
- **Idempotencia:** como un evento puede llegar repetido, cada consumidor guarda los `idEvento` procesados (tabla `evento_procesado`) y los ignora si vuelven. `ConsumidorEventos` ya lo hace si le pasas un `RegistroEventosProcesados`.
- Si el manejador lanza una excepción, el mensaje se rechaza sin reencolar (para no ciclarse) y queda en el log.

## Colas

Cada servicio tiene **una cola durable** con el nombre `servicio.eventos`, enlazada solo a lo que le interesa.

| Cola | Servicio | Routing keys que escucha |
|---|---|---|
| `aval.eventos` | AvalService | `entrega.requiereAval` |
| `notificar.eventos` | NotificarService | `calificacion.registrada`, `tarea.vencida`, `aval.solicitado` |
| `mensajeria.eventos` | MensajeriaService | `alerta.generada` |
| `integracion.eventos` | IntegrationService | `aval.resuelto` |

## Eventos

### `calificacion.registrada`
Publica **Integration** · Consume **Notificar** · Cuando llega una calificación nueva desde Moodle.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `idCalificacion` | String | `CAL-004` |
| `matricula` | String | `A002` |
| `idCurso` | String | `CUR-103` |
| `materia` | String | `Historia` |
| `numEmpleado` | String | `EMP-21` |
| `periodo` | String | `P1` |
| `calificacion` | double (0 a 10) | `5.2` |

```json
{ "idCalificacion": "CAL-004", "matricula": "A002", "idCurso": "CUR-103", "materia": "Historia",
  "numEmpleado": "EMP-21", "periodo": "P1", "calificacion": 5.2 }
```

### `tarea.vencida`
Publica **Integration** · Consume **Notificar** · Pasó la fecha límite y el alumno no entregó. Un evento por alumno.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `idTarea` | String | `T-12` |
| `idCurso` | String | `CUR-103` |
| `titulo` | String | `Ensayo Revolucion Mexicana` |
| `matricula` | String | `A004` |
| `fechaLimite` | LocalDateTime | `2026-11-15T23:59:00` |

```json
{ "idTarea": "T-12", "idCurso": "CUR-103", "titulo": "Ensayo Revolucion Mexicana",
  "matricula": "A004", "fechaLimite": "2026-11-15T23:59:00" }
```

### `entrega.requiereAval`
Publica **Integration** · Consume **Aval** · Una entrega de una tarea con `requiereAvalPadre = true`.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `idEntrega` | String | `ENT-101` |
| `idTarea` | String | `T-10` |
| `idCurso` | String | `CUR-101` |
| `matricula` | String | `A001` |

```json
{ "idEntrega": "ENT-101", "idTarea": "T-10", "idCurso": "CUR-101", "matricula": "A001" }
```

### `aval.solicitado`
Publica **Aval** · Consume **Notificar** · La solicitud quedó en `PENDIENTE_AVAL`. Notificar genera `TAREA_PENDIENTE_AVAL`.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `idEntrega` | String | `ENT-101` |
| `idTarea` | String | `T-10` |
| `matricula` | String | `A001` |

```json
{ "idEntrega": "ENT-101", "idTarea": "T-10", "matricula": "A001" }
```

### `aval.resuelto`
Publica **Aval** · Consume **Integration** · El tutor aprobó o rechazó.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `idEntrega` | String | `ENT-101` |
| `matricula` | String | `A001` |
| `idTutor` | String | `T001` |
| `estado` | `EstadoEntrega` | `AVALADA` o `RECHAZADA_POR_TUTOR` |
| `fechaResolucion` | Instant | `2026-11-24T07:15:00Z` |

```json
{ "idEntrega": "ENT-101", "matricula": "A001", "idTutor": "T001",
  "estado": "AVALADA", "fechaResolucion": "2026-11-24T07:15:00Z" }
```

### `alerta.generada`
Publica **Notificar** · Consume **Mensajería** · Se creó una `AlertaRendimiento`. **Un evento por tutor** (si el alumno tiene 2 tutores, salen 2 eventos). Mensajería se lo manda al tutor por su WebSocket si está conectado.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `idAlerta` | String | `AL-7f3a` |
| `tipo` | `TipoAlerta` | `CALIFICACION_REPROBATORIA` |
| `matricula` | String | `A002` |
| `idTutor` | String | `T002` |
| `mensaje` | String | `Sofia Valdez obtuvo 5.2 en Historia (P1)` |
| `fechaEmision` | Instant | `2026-11-24T07:20:00Z` |

```json
{ "idAlerta": "AL-7f3a", "tipo": "CALIFICACION_REPROBATORIA", "matricula": "A002", "idTutor": "T002",
  "mensaje": "Sofia Valdez obtuvo 5.2 en Historia (P1)", "fechaEmision": "2026-11-24T07:20:00Z" }
```

## Enums compartidos (`com.itson.escuela.common.modelo`)

- `TipoAlerta`: `CALIFICACION_REPROBATORIA`, `DESCENSO_PROMEDIO`, `TAREA_NO_ENTREGADA`, `TAREA_PENDIENTE_AVAL`, `TAREAS_OMITIDAS_ACUMULADAS`
- `EstadoEntrega`: `PENDIENTE`, `ENTREGA_EN_TIEMPO`, `ENTREGA_TARDIA`, `PENDIENTE_AVAL`, `AVALADA`, `RECHAZADA_POR_TUTOR`, `CALIFICADA`

## Probar sin el otro servicio

1. `docker compose up -d` y abre `http://localhost:15672` (usuario `escuela` / `escuela`).
2. **Exchanges → `escuela.eventos` → Publish message.**
3. Routing key: la del evento. Properties: `message_id` = un id cualquiera, `content_type` = `application/json`.
4. Payload: el sobre completo con el `datos` de la tabla de arriba.
5. Si publicas dos veces con el mismo `message_id`, el consumidor debe ignorar la segunda.
