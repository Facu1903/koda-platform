# Changelog

Todas las modificaciones relevantes de KODA PLATFORM se documentaran en este archivo.

El formato se basa en Keep a Changelog y el versionado seguira `0.<sprint>.<patch>` hasta la primera version comercial estable.

## [0.1.0] - Unreleased

### Added

- Iniciado Sprint 1 con scaffolding tecnico de backend, frontend e infraestructura local.
- Agregado backend Spring Boot 3.5.16 con Java 21, Actuator, Security, Validation, JPA, Flyway, PostgreSQL, OpenAPI y logs estructurados.
- Ajustado `springdoc-openapi` a 2.8.17 para compatibilidad con Spring Boot 3.
- Agregado frontend React 19, TypeScript 6.0.3, Vite y Material UI con app shell inicial de KODA ERP.
- Agregado Docker Compose con PostgreSQL 17, backend y frontend.
- Agregado plan de ejecucion de Sprint 1.
- Agregadas migraciones Flyway iniciales para extensiones PostgreSQL, nucleo SaaS, seguridad/RBAC, catalogos ERP, stock y auditoria.
- Agregado seed inicial aprobado para tenant KODA, producto KODA ERP, modulos base, roles y permisos.
- Agregada documentacion de convenciones PostgreSQL.
- Agregado Tenant Context backend con `TenantId`, `TenantContext`, `CurrentTenantProvider`, `TenantAwarePrincipal` y filtro por request.
- Agregada documentacion de Tenant Context.
- Agregados tests unitarios para resolucion y limpieza de Tenant Context.

### Changed

- Corregida configuracion TypeScript/Vite del frontend para build Docker.
- Cambiado Docker Compose de PostgreSQL 18 a PostgreSQL 17 por compatibilidad probada con Flyway.
- Actualizado README con estado real de Sprint 1 Hito 2 y base de datos inicial.
- Conectado `TenantContextAuthenticationFilter` a Spring Security despues de Basic Auth.
- Agregado manejo de error `TENANT_CONTEXT_REQUIRED` para operaciones que exijan tenant.

### Verified

- Java 21.0.10 y Apache Maven 3.9.16 verificados para backend.
- `mvn test` ejecutado correctamente en backend.
- Docker Compose validado con PostgreSQL 17.10 healthy, backend Actuator `UP` y frontend HTTP 200.
- Flyway validado contra PostgreSQL 17.10 sin advertencia de version no soportada.
- Backend local empaquetado con Maven y validado contra PostgreSQL 17 aplicando 6 migraciones nuevas.
- Imagen Docker backend reconstruida y backend Docker validado con schema Flyway en `v202607171520`.
- Base validada con 25 tablas, 40 permisos, 7 roles y tenant KODA seed.
- mvn test ejecutado correctamente con 9 tests: contexto Spring, TenantId y filtro Tenant Context.
- Jar backend actual empaquetado y validado en runtime local contra PostgreSQL 17 con Actuator UP.

### Known Issues

- La instalacion completa de dependencias frontend en OneDrive supero el tiempo de espera; se genero `package-lock.json` con resolucion correcta y sin vulnerabilidades reportadas.
- El primer build Docker del backend puede tardar varios minutos porque Maven descarga dependencias dentro de la imagen builder.
- Mockito emite advertencia por carga dinamica de Java agent; no bloquea actualmente, pero debe revisarse antes de endurecer la matriz de Java futura.

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