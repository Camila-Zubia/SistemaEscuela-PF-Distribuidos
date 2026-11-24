# SistemaEscuela – Proyecto final de Sistemas Distribuidos

Sistema escolar con microservicios en Java (Quarkus), comunicados por REST, eventos en RabbitMQ, WebSocket y SOAP.

## Requisitos

- JDK 21 o superior
- Maven 3.9 (el que trae NetBeans sirve)
- Docker Desktop

## Levantar la infraestructura

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
