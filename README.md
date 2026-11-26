# SistemaEscuela – Proyecto final de Sistemas Distribuidos

Sistema escolar con microservicios en Java (Quarkus), comunicados por REST, eventos en RabbitMQ, WebSocket y SOAP.

## Requisitos

- JDK 21 o superior
- Maven 3.9 (el que trae NetBeans sirve)
- Docker Desktop

## Arrancar todo con un comando

Desde la carpeta del proyecto:

```powershell
.\iniciar-todo.ps1
```

Enciende Docker si hace falta, levanta RabbitMQ y PostgreSQL, compila si el código cambió y abre los 7 servicios en orden, cada uno en su ventana. Al final revisa que las 4 colas tengan su consumidor.

| Opción | Para qué |
|---|---|
| `.\iniciar-todo.ps1 -Limpio` | Borra las bases y las colas antes de arrancar (para empezar una demo de cero) |
| `.\iniciar-todo.ps1 -Compilar` | Compila todo aunque parezca al día |
| `.\detener-todo.ps1` | Apaga los 7 servicios y cierra sus ventanas |
| `.\detener-todo.ps1 -Docker` | También apaga RabbitMQ y PostgreSQL (los datos se quedan) |
| `.\detener-todo.ps1 -Limpio` | Apaga todo y borra los datos |

Si Windows no deja correr el script, usa `powershell -ExecutionPolicy Bypass -File .\iniciar-todo.ps1`.

## Probar todo el sistema

1. `.\iniciar-todo.ps1 -Limpio`
2. En Postman importa `docs/postman/SistemaEscuela-Completo.postman_collection.json`.
3. Clic derecho en la colección → **Run collection** → **Run**.

Recorre el flujo completo por el Gateway (Moodle → Aval → Notificar → buzón → aval resuelto → Integration → chat) y las reglas de negocio (403, 404, 409 y que no se dupliquen alertas). Cada petición guarda en variables lo que necesita la siguiente. La descripción de la colección explica las pruebas de resiliencia, que se hacen a mano.

También hay colecciones por servicio en `docs/postman/` (Notificar y Mensajería, que incluye cómo probar el chat por WebSocket).

## Levantar la infraestructura a mano

```powershell
docker compose up -d
```

- RabbitMQ: `localhost:5672` · panel en http://localhost:15672 (usuario `escuela` / `escuela`)
- PostgreSQL: `localhost:5432` (usuario `escuela` / `escuela`), con una base por servicio: `aval`, `notificar`, `mensajeria`, `integracion`

## Compilar todo

```powershell
cd SistemaEscuela
mvn install
```

## Estructura

| Carpeta | Qué es |
|---|---|
| `SistemaEscuela/escuela-common` | Librería compartida: eventos, publicador y consumidor de RabbitMQ, enums y datos semilla |
| `SistemaEscuela/ServicioPlantilla` | Servicio Quarkus vacío. Cópialo para crear o migrar un servicio |
| `SistemaEscuela/*Service`, `*Mock` | Los microservicios y los simuladores de Moodle y Registro Escolar |
| `docs/contrato-eventos.md` | Eventos del broker: routing keys, JSON y colas |
| `docs/contrato-api.md` | Endpoints REST, WebSocket y tabla de puertos |
| `docs/PlanDeTrabajo.pdf` | Plan de trabajo y reparto del equipo |
| `SistemaEscuela.mdj` | Diagramas UML (StarUML) |

## Crear o migrar un servicio con la plantilla

1. Copia `SistemaEscuela/ServicioPlantilla` y cambia el `artifactId`, el paquete y el puerto (`application.properties`, ver la tabla de `docs/contrato-api.md`).
2. Agrega el módulo en `SistemaEscuela/pom.xml`.
3. En NetBeans: *Propiedades del proyecto → Actions → Run / Debug → Execute Goals:* `quarkus:dev`.

Para publicar o consumir eventos usa `PublicadorEventos` y `ConsumidorEventos` de `escuela-common` (hay ejemplos en su Javadoc).
