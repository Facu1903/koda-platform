# Changelog

Todas las modificaciones relevantes de KODA PLATFORM se documentaran en este archivo.

El formato se basa en Keep a Changelog y el versionado seguira `0.<sprint>.<patch>` hasta la primera version comercial estable.

## [0.2.0] - Unreleased

### Added

- Agregado plan inicial propuesto de Sprint 2 en `docs/sprints/SPRINT_2_EXECUTION_PLAN.md`.

## [0.1.0] - 2026-07-17

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
- Agregada autenticacion JWT stateless con login, refresh y logout.
- Agregados `AuthService`, puerto `AuthRepository`, adaptador JDBC, generador de refresh tokens, servicio JWT y conversion de JWT a principal KODA.
- Agregado bootstrap inicial opt-in para owner de tenant sin credenciales hardcodeadas.
- Agregada documentacion de autenticacion en `docs/security/AUTHENTICATION.md`.
- Agregados tests unitarios para login, seleccion de tenant, rotacion de refresh token y conversion JWT.
- Agregada API tenant-scoped de configuracion de empresa en `/api/v1/company/settings`.
- Agregados servicio, puerto de repositorio y adaptador JDBC para configuracion visual/regional.
- Agregada auditoria persistente de actualizaciones `company_settings.update`.
- Agregada documentacion de configuracion de empresa en `docs/configuration/COMPANY_SETTINGS.md`.
- Agregados tests unitarios para permisos, normalizacion, auditoria y versionado de configuracion de empresa.
- Agregado CRUD backend tenant-scoped de catalogos ERP bajo `/api/v1/catalog`.
- Agregados servicio, puerto de repositorio y adaptador JDBC para marcas, categorias, unidades, presentaciones y productos.
- Agregada migracion `V202607171530__assign_catalog_role_permissions.sql` con matriz rol-permiso aprobada para catalogos.
- Agregada documentacion de catalogos en `docs/catalogs/ERP_CATALOGS.md`.
- Agregada decision funcional aprobada de catalogos en `docs/sprints/SPRINT_1_FUNCTIONAL_BASELINE.md`.
- Agregados tests unitarios para permisos, referencias, versionado, soft delete y auditoria de catalogos.
- Agregada API tenant-scoped de stock bajo `/api/v1/stock` para saldos y movimientos.
- Agregados servicio, puerto de repositorio y adaptador JDBC para stock.
- Agregada migracion `V202607171540__enable_stock_operations.sql` con ledger enriquecido, permiso `stock_balances:read`, matriz rol-permiso de stock y seed minimo de sucursal/deposito KODA.
- Agregada documentacion de stock en `docs/stock/STOCK_MOVEMENTS.md`.
- Agregada decision funcional aprobada de stock en `docs/sprints/SPRINT_1_FUNCTIONAL_BASELINE.md`.
- Agregados tests unitarios para permisos, stock negativo, ajustes a cero, reservas, productos no stockeables y auditoria de stock.
- Agregada API tenant-scoped de auditoria en `/api/v1/audit/events`.
- Agregados servicio, puerto de repositorio y adaptador JDBC para consulta de eventos auditables.
- Agregada migracion `V202607171550__assign_audit_role_permissions.sql` con matriz rol-permiso aprobada para auditoria.
- Agregada documentacion de auditoria en `docs/audit/AUDIT_EVENTS.md`.
- Agregada decision funcional aprobada de auditoria en `docs/sprints/SPRINT_1_FUNCTIONAL_BASELINE.md`.
- Agregados tests unitarios para permisos, filtros, limites y validacion temporal de auditoria.
- Agregadas reglas automatizadas de arquitectura con ArchUnit.
- Agregados puertos `AccessTokenIssuer`, `RefreshTokenService` y `AuthTokenPolicy` para desacoplar autenticacion de infraestructura concreta.
- Agregado adaptador `KodaAuthTokenPolicy` para politica de tokens desde configuracion.
- Agregados tests de seguridad JWT para secreto obligatorio, secreto minimo, issuer esperado e issuer invalido.
- Agregados tests de aislamiento multiempresa en catalogos y stock.
- Agregada documentacion de hardening en `docs/sprints/SPRINT_1_HARDENING_REPORT.md`.
- Agregado cierre formal de Sprint 1 en `docs/sprints/SPRINT_1_CLOSURE_REPORT.md`.

### Changed

- Corregida configuracion TypeScript/Vite del frontend para build Docker.
- Cambiado Docker Compose de PostgreSQL 18 a PostgreSQL 17 por compatibilidad probada con Flyway.
- Actualizado README con estado real de Sprint 1 Hito 2 y base de datos inicial.
- Conectado `TenantContextAuthenticationFilter` a Spring Security despues de Basic Auth.
- Agregado manejo de error `TENANT_CONTEXT_REQUIRED` para operaciones que exijan tenant.
- Agregado manejo de error `MALFORMED_REQUEST_BODY` para JSON invalido o ilegible.
- Spring Security ahora usa sesiones stateless y JWT Bearer tokens.
- `/api/v1/auth/login`, `/api/v1/auth/refresh` y `/api/v1/auth/logout` quedan como rutas tenant-neutrales.
- Docker Compose requiere `KODA_JWT_SECRET` para iniciar backend.
- Agregado error `PERMISSION_DENIED` para permisos backend insuficientes.
- Agregados errores `COMPANY_SETTINGS_NOT_FOUND` y `COMPANY_SETTINGS_VERSION_CONFLICT`.
- Agregados errores `CATALOG_ITEM_NOT_FOUND`, `CATALOG_REFERENCE_NOT_FOUND` y `CATALOG_VERSION_CONFLICT`.
- Agregados errores `STOCK_ITEM_NOT_FOUND`, `STOCK_REFERENCE_NOT_FOUND` y `STOCK_MOVEMENT_REJECTED`.
- `AuthService` ahora depende de puertos de aplicacion en lugar de clases de infraestructura JWT/refresh/configuracion.
- El decoder JWT ahora valida explicitamente `KODA_JWT_ISSUER`.
- Actualizado diagrama de capas para reflejar que `api` no depende de `infrastructure`.

### Verified

- Java 21.0.10 y Apache Maven 3.9.16 verificados para backend.
- `mvn test` ejecutado correctamente en backend.
- Docker Compose validado con PostgreSQL 17.10 healthy, backend Actuator `UP` y frontend HTTP 200.
- Flyway validado contra PostgreSQL 17.10 sin advertencia de version no soportada.
- Backend local empaquetado con Maven y validado contra PostgreSQL 17 aplicando 6 migraciones nuevas.
- Imagen Docker backend reconstruida y backend Docker validado con schema Flyway en `v202607171520`.
- Base validada con 25 tablas, 40 permisos, 7 roles y tenant KODA seed.
- `mvn test` ejecutado correctamente con 9 tests: contexto Spring, TenantId y filtro Tenant Context.
- Jar backend actual empaquetado y validado en runtime local contra PostgreSQL 17 con Actuator UP.
- `mvn test` ejecutado correctamente con 14 tests: contexto Spring, Tenant Context, AuthService y conversion JWT.
- Jar backend Hito 4 empaquetado y validado en runtime temporal contra PostgreSQL 17 con Actuator `UP` y error auth `400` estructurado.
- `mvn test` ejecutado correctamente con 19 tests: contexto Spring, Tenant Context, AuthService, conversion JWT y CompanySettingsService.
- Jar backend Hito 5 empaquetado y validado en runtime temporal contra PostgreSQL 17 con Actuator `UP` y endpoint company settings protegido con `401` anonimo.
- `mvn test` ejecutado correctamente con 25 tests: contexto Spring, Tenant Context, AuthService, conversion JWT, CompanySettingsService y CatalogService.
- Jar backend Hito 6 empaquetado y validado en runtime temporal contra PostgreSQL 17 con Actuator `UP`, Flyway `v202607171530`, 55 asignaciones rol-permiso y endpoint catalog products protegido con `401` anonimo.
- `mvn test` ejecutado correctamente con 32 tests: contexto Spring, Tenant Context, AuthService, conversion JWT, CompanySettingsService, CatalogService y StockService.
- Jar backend Hito 7 empaquetado y validado en runtime temporal contra PostgreSQL 17 con Actuator `UP`, Flyway `v202607171540`, 41 permisos, 70 asignaciones rol-permiso, deposito piloto `PRINCIPAL` y endpoint stock balances protegido con `401` anonimo.
- `mvn test` ejecutado correctamente con 38 tests: contexto Spring, Tenant Context, AuthService, conversion JWT, CompanySettingsService, CatalogService, StockService y AuditService.
- Jar backend Hito 8 empaquetado y validado en runtime temporal contra PostgreSQL 17 con Actuator `UP`, Flyway `v202607171550`, 73 asignaciones rol-permiso y endpoint audit events protegido con `401` anonimo.
- Imagen Docker backend reconstruida y backend Docker validado con Actuator `UP`, Flyway `v202607171550` y endpoint audit events protegido con `401` anonimo.
- `mvn test` ejecutado correctamente con 47 tests: contexto Spring, Tenant Context, AuthService, conversion JWT, configuracion JWT, CompanySettingsService, CatalogService, StockService, AuditService y reglas ArchUnit.
- Jar backend Hito 9 empaquetado y validado en runtime temporal contra PostgreSQL 17 con Actuator `UP`, Flyway `v202607171550`, 73 asignaciones rol-permiso y endpoint audit events protegido con `401` anonimo.
- Imagen Docker backend Hito 9 reconstruida y backend Docker validado con Actuator `UP`, Flyway `v202607171550` y endpoint audit events protegido con `401` anonimo.

### Known Issues

- La instalacion completa de dependencias frontend en OneDrive supero el tiempo de espera; se genero `package-lock.json` con resolucion correcta y sin vulnerabilidades reportadas.
- El primer build Docker del backend puede tardar varios minutos porque Maven descarga dependencias dentro de la imagen builder.
- Mockito emite advertencia por carga dinamica de Java agent; no bloquea actualmente, pero debe revisarse antes de endurecer la matriz de Java futura.
- HS256 se acepta para Sprint 1; antes de produccion multi-nodo debe evaluarse rotacion de llaves, RS256/JWKS y politica operacional de secretos.
- Las matrices rol-permiso de catalogos, stock y auditoria fueron aprobadas y aplicadas; quedan pendientes matrices finas de seguridad y configuracion de empresa.
- Stock no incluye transferencias, reservas, lotes, vencimientos, costos promedio ni UI en Hito 7.
- Auditoria Hito 8 no incluye auditoria global de plataforma, exportacion, retencion, particionamiento ni UI.
- Hito 9 no incluye Row Level Security, tests Testcontainers de repositorios por defecto, SAST/DAST, pruebas de carga, CI/CD ni rotacion avanzada de llaves.

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
