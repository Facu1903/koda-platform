# Changelog

Todas las modificaciones relevantes de KODA PLATFORM se documentaran en este archivo.

El formato se basa en Keep a Changelog y el versionado seguira `0.<sprint>.<patch>` hasta la primera version comercial estable.

## [0.1.0] - Unreleased

### Added

- Iniciado Sprint 1 con scaffolding tecnico de backend, frontend e infraestructura local.
- Agregado backend Spring Boot 3.5.16 con Java 21, Actuator, Security, Validation, JPA, Flyway, PostgreSQL, OpenAPI y logs estructurados.
- Agregado frontend React 19, TypeScript 6.0.3, Vite y Material UI con app shell inicial de KODA ERP.
- Agregado Docker Compose con PostgreSQL, backend y frontend.
- Agregado plan de ejecucion de Sprint 1.

### Known Issues

- Java 21 y Maven no estan disponibles en PATH local; la validacion backend local queda pendiente hasta instalarlos o ejecutar via Docker.
- Docker Desktop no estaba corriendo durante la validacion, por lo que `docker compose build backend` no pudo ejecutarse.
- La instalacion completa de dependencias frontend en OneDrive supero el tiempo de espera; se genero `package-lock.json` con resolucion correcta y sin vulnerabilidades reportadas.
## [0.0.2] - 2026-07-15

### Added

- Documentada la base funcional aprobada para Sprint 1.
- Agregado detalle de tenant inicial KODA, roles, permisos y reglas iniciales de stock.
- Agregado `.gitattributes` para normalizar saltos de linea y archivos binarios.

### Changed

- Actualizado `ROADMAP.md` para reflejar que las decisiones funcionales minimas de Sprint 1 fueron aprobadas.
- Actualizado `README.md` con referencia al documento de base funcional de Sprint 1.
## [0.0.1] - 2026-07-15

### Added

- Agregado `.gitignore` inicial para Java, Maven, Spring Boot, React, Vite, Docker, IDEs, logs, archivos locales y secretos.

## [0.0.0] - 2026-07-14

### Added

- Creada constitucion inicial del proyecto.
- Definido objetivo de KODA PLATFORM como plataforma SaaS multiempresa.
- Definido KODA ERP como primer producto de la plataforma.
- Documentada arquitectura Clean Architecture.
- Documentado enfoque multi-tenant.
- Documentado stack tecnologico obligatorio.
- Documentadas convenciones de codigo.
- Documentado roadmap inicial por sprints.
- Documentado flujo de contribucion.
- Documentadas reglas de gobierno del proyecto.
