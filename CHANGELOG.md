# Changelog

Todas las modificaciones relevantes de KODA PLATFORM se documentaran en este archivo.

El formato se basa en Keep a Changelog y el versionado seguira `0.<sprint>.<patch>` hasta la primera version comercial estable.

## [0.4.0] - Unreleased

### Added

- Agregada y aprobada base funcional de Sprint 4 en `docs/sprints/SPRINT_4_FUNCTIONAL_BASELINE.md`.
- Agregado plan de ejecucion de Sprint 4 en `docs/sprints/SPRINT_4_EXECUTION_PLAN.md`.
- Agregado filtro transversal `CorrelationIdFilter` para resolver `X-Correlation-ID`, reutilizar valores validos, generar UUID cuando falte o sea inseguro y devolver el header efectivo en cada respuesta.
- Agregado contexto MDC de observabilidad con `correlationId`, metodo HTTP, path normalizado, status, duracion, `tenantId`, `userId` y `platformAdmin` cuando correspondan.
- Agregada documentacion tecnica de correlation ID y logs estructurados en `docs/observability/CORRELATION_AND_LOGGING.md`.
- Agregadas pruebas unitarias de correlation ID, limpieza de MDC, evento de cierre HTTP y enriquecimiento de Tenant Context.
- Agregado health indicator `kodaSchema` para verificar estado Flyway/schema sin exponer mensajes internos.
- Agregados endpoints operativos documentados de liveness/readiness con componentes seguros.
- Agregada prueba de integracion `ActuatorHealthPostgresqlIT` con PostgreSQL 17 real para validar readiness con `db` y `kodaSchema`.
- Agregada documentacion de health checks operativos en `docs/observability/HEALTH_CHECKS.md`.
- Agregada configuracion base de metricas Actuator/Micrometer con tags comunes, histogramas HTTP y buckets SLO.
- Agregado guardrail tecnico para bloquear tags sensibles o de alta cardinalidad en metricas.
- Agregado limite de cardinalidad para el tag `uri` de `http.server.requests`.
- Agregada documentacion de metricas operativas en `docs/observability/METRICS.md`.
- Agregadas pruebas de endpoint protegido de metricas y filtros de cardinalidad.

### Changed

- Actualizado README y roadmap para iniciar Sprint 4 como Escalabilidad, Observabilidad y Endurecimiento SaaS Operativo.
- Habilitada inclusion explicita de MDC en logs JSON de Logstash Encoder.
- Enriquecido `TenantContextAuthenticationFilter` para publicar contexto tenant/user en MDC sin cambiar reglas de autorizacion.
- Configurado Actuator Health con `show-details: never`, `show-components: always`, validacion estricta de miembros, grupos `liveness` y `readiness`, y readiness dependiente de PostgreSQL y schema.
- Actualizada seguridad para permitir `/actuator/health/**` sin autenticacion y sin exponer detalles sensibles.
- Expuesto `/actuator/metrics` en Actuator manteniendolo protegido por autenticacion.
- Configurado `http.server.requests` con percentiles `p50`, `p95`, `p99` y SLO iniciales de latencia.

### Verified

- `mvn -B '-Dtest=CorrelationIdFilterTest,TenantContextAuthenticationFilterTest' test` ejecutado correctamente en backend con 9 pruebas.
- `mvn -B test` ejecutado correctamente en backend con 120 pruebas unitarias.
- `mvn -B '-Dtest=KodaSchemaHealthIndicatorTest,KodaPlatformApplicationTests' test` ejecutado correctamente en backend con 6 pruebas.
- `mvn -B verify` ejecutado correctamente en backend con 125 pruebas unitarias y 13 pruebas de integracion.
- `mvn -B "-Dtest=KodaMetricsConfigurationTest,KodaPlatformApplicationTests" test` ejecutado correctamente en backend con 7 pruebas.
- `mvn -B verify` ejecutado correctamente en backend con 130 pruebas unitarias y 13 pruebas de integracion.

## [0.3.0] - 2026-07-21

### Added

- Agregada y aprobada base funcional de Sprint 3 en `docs/sprints/SPRINT_3_FUNCTIONAL_BASELINE.md`.
- Agregado plan de ejecucion de Sprint 3 en `docs/sprints/SPRINT_3_EXECUTION_PLAN.md`.
- Agregada migracion `V202607201500__create_saas_licensing_model.sql` para planes, suscripciones, limites, feature flags y seed `KODA_PILOT`.
- Agregada documentacion del modelo SaaS en `docs/licensing/SAAS_LICENSING_MODEL.md`.
- Agregado backend de capabilities tenant-scoped con servicio de aplicacion, puerto y repositorio JDBC.
- Agregado endpoint `GET /api/v1/capabilities` para productos, modulos, feature flags y limites efectivos del tenant autenticado.
- Agregadas pruebas unitarias de `TenantCapabilitiesService` y prueba de integracion del calculo real sobre PostgreSQL 17.
- Agregado guard backend `TenantLicenseAccessGuard` para bloquear operaciones por producto/modulo no habilitado.
- Agregado puerto `TenantLicenseAccessRepository` con consultas `EXISTS` para producto y modulo efectivo.
- Agregada documentacion de guards en `docs/licensing/TENANT_LICENSE_GUARDS.md`.
- Agregadas pruebas de bloqueo por licencia en servicios existentes y dependencias cruzadas de ventas/compras con stock/caja.
- Agregada API interna de administracion de licencias bajo `/api/v1/platform/tenants/{tenantId}/licenses`.
- Agregado repositorio JDBC de administracion para consultar suscripciones, entitlements, overrides y feature flags por tenant.
- Agregada migracion `V202607201600__seed_license_administration_permissions.sql` con permisos `license_admin:read` y `license_admin:update` para `PLATFORM_SUPER_ADMIN`.
- Agregada documentacion de administracion interna de licencias en `docs/licensing/TENANT_LICENSE_ADMINISTRATION.md`.
- Agregado shell frontend de capabilities con contexto React, cliente API y navegacion/rutas condicionadas por modulos habilitados.
- Agregada documentacion del shell frontend en `docs/licensing/FRONTEND_CAPABILITY_SHELL.md`.
- Agregados reportes de hardening y cierre tecnico de Sprint 3 en `docs/sprints/SPRINT_3_HARDENING_REPORT.md` y `docs/sprints/SPRINT_3_CLOSURE_REPORT.md`.
- Agregada aprobacion funcional final del Product Owner en `docs/sprints/SPRINT_3_APPROVAL.md`.

### Changed

- Actualizado README y roadmap para iniciar Sprint 3 como Fundacion SaaS Comercial.
- Agregados metadatos `core_module` y `commercially_toggleable` a `platform_modules`.
- Actualizado manejo global de excepciones para responder tenants sin capabilities disponibles.
- Aplicado control de licencia SaaS en configuracion, catalogos, stock, auditoria, clientes/proveedores, caja, ventas, compras y reportes comerciales.
- Ajustada autenticacion para incluir roles/permisos de plataforma en el principal JWT mediante contrato de aplicacion `KodaSecurityPrincipal`.
- Reemplazado el dashboard frontend estatico por un shell operativo que consume `GET /api/v1/capabilities`.
- Estabilizado test frontend del capability shell para evitar timeout por consultas de rol demasiado pesadas sobre Material UI.
- Actualizado el estado de Sprint 3 a cerrado y aprobado funcionalmente por el Product Owner.

### Verified

- `mvn -B test` ejecutado correctamente en backend con 115 pruebas unitarias.
- `mvn -B verify` ejecutado correctamente en backend con 115 pruebas unitarias y 11 pruebas de integracion.
- Flyway validado mediante Testcontainers contra PostgreSQL 17.10 hasta `v202607201600`.
- `npm.cmd run test` ejecutado correctamente en frontend con 3 pruebas.
- `npm.cmd run lint` ejecutado correctamente en frontend.
- `npm.cmd run build` ejecutado correctamente en frontend con TypeScript y Vite.
- Cierre tecnico de Sprint 3 validado con `mvn -B verify`, `npm.cmd run test`, `npm.cmd run lint` y `npm.cmd run build`.

## [0.2.0] - 2026-07-20

### Added

- Agregado plan inicial propuesto de Sprint 2 en `docs/sprints/SPRINT_2_EXECUTION_PLAN.md`.
- Agregada y aprobada base funcional de Sprint 2 en `docs/sprints/SPRINT_2_FUNCTIONAL_BASELINE.md`.
- Agregado pipeline minimo de GitHub Actions en `.github/workflows/ci.yml` para backend y frontend.
- Agregado Maven Failsafe para ejecutar pruebas de integracion durante `mvn verify`.
- Agregada prueba de persistencia `FlywayPostgresqlIT` con Testcontainers y PostgreSQL 17.
- Agregada documentacion de CI en `docs/ci/GITHUB_ACTIONS.md`.
- Agregado modulo backend commercial para clientes y proveedores tenant-scoped.
- Agregadas migraciones `V202607201000__create_commercial_partner_tables.sql` y `V202607201010__seed_commercial_partners_permissions.sql`.
- Agregado seed `Consumidor Final` como cliente sistema para KODA.
- Agregados permisos `customers:*` y `suppliers:*` con matriz rol-permiso aprobada.
- Agregada documentacion de clientes/proveedores en `docs/commercial/COMMERCIAL_PARTNERS.md`.
- Agregado modulo backend cash para caja inicial tenant-scoped.
- Agregadas migraciones `V202607201100__create_cash_tables.sql` y `V202607201110__seed_cash_permissions.sql`.
- Agregado seed `CAJA_PRINCIPAL` para KODA.
- Agregados permisos `cash_registers:*`, `cash_sessions:*` y `cash_movements:*` con matriz rol-permiso aprobada.
- Agregada documentacion de caja inicial en `docs/cash/CASH_SESSIONS.md`.
- Agregado modulo backend sales para ventas basicas tenant-scoped.
- Agregadas migraciones `V202607201200__create_sales_tables.sql` y `V202607201210__seed_sales_permissions.sql`.
- Agregada numeracion interna por tenant/sucursal mediante `sales_number_sequences`.
- Agregados permisos `sales:*` con matriz rol-permiso aprobada.
- Agregada integracion de ventas con stock y caja mediante puertos internos.
- Agregada documentacion de ventas basicas en `docs/sales/SALES.md`.
- Agregado modulo backend purchases para compras basicas tenant-scoped.
- Agregadas migraciones `V202607201300__create_purchase_tables.sql` y `V202607201310__seed_purchase_permissions.sql`.
- Agregada numeracion interna por tenant/sucursal mediante `purchase_number_sequences`.
- Agregados permisos `purchases:*` con matriz rol-permiso aprobada.
- Agregada integracion de compras con stock y caja mediante puertos internos.
- Agregada documentacion de compras basicas en `docs/purchases/PURCHASES.md`.
- Agregado modulo backend reports para reportes operativos y dashboard tenant-scoped.
- Agregada migracion `V202607201400__enable_commercial_reports.sql`.
- Agregado modulo SaaS `COMMERCIAL_REPORTS` y permiso `commercial_reports:read` con matriz rol-permiso aprobada.
- Agregados indices operativos para reportes por fecha en ventas, compras, caja y stock.
- Agregada API `/api/v1/reports` para ventas, compras, caja, top productos, stock bajo y dashboard.
- Agregada documentacion de reportes operativos en `docs/reports/OPERATIONAL_REPORTS.md`.
- Agregados reportes de hardening y cierre de Sprint 2 en `docs/sprints/SPRINT_2_HARDENING_REPORT.md` y `docs/sprints/SPRINT_2_CLOSURE_REPORT.md`.
- Agregada aprobacion funcional final del Product Owner en `docs/sprints/SPRINT_2_APPROVAL.md`.

### Changed

- Agregado manejo global de errores `PURCHASE_NOT_FOUND`, `PURCHASE_REFERENCE_NOT_FOUND`, `PURCHASE_VERSION_CONFLICT` y `PURCHASE_OPERATION_REJECTED`.
- Endurecido el manejo global de errores para parametros URL faltantes, invalidos o fuera de validacion.
- Actualizado el estado de Sprint 2 a cerrado y aprobado funcionalmente por el Product Owner.

### Verified

- `mvn -B verify` ejecutado correctamente en backend con 47 pruebas unitarias y 3 pruebas de integracion.
- Flyway validado mediante Testcontainers contra PostgreSQL 17.10 hasta `v202607171550`.
- `mvn -B test` ejecutado correctamente en backend con 54 pruebas unitarias.
- `mvn -B verify` ejecutado correctamente en backend con 54 pruebas unitarias y 3 pruebas de integracion.
- Flyway validado mediante Testcontainers contra PostgreSQL 17.10 hasta `v202607201010`.
- `mvn -B test` ejecutado correctamente en backend con 62 pruebas unitarias.
- `mvn -B verify` ejecutado correctamente en backend con 62 pruebas unitarias y 3 pruebas de integracion.
- Flyway validado mediante Testcontainers contra PostgreSQL 17.10 hasta `v202607201110`.
- `mvn -B test` ejecutado correctamente en backend con 70 pruebas unitarias.
- `mvn -B verify` ejecutado correctamente en backend con 70 pruebas unitarias y 4 pruebas de integracion.
- Flyway validado mediante Testcontainers contra PostgreSQL 17.10 hasta `v202607201210`.
- `mvn -B test` ejecutado correctamente en backend con 79 pruebas unitarias.
- `mvn -B verify` ejecutado correctamente en backend con 79 pruebas unitarias y 5 pruebas de integracion.
- Flyway validado mediante Testcontainers contra PostgreSQL 17.10 hasta `v202607201310`.
- `mvn -B test` ejecutado correctamente en backend con 86 pruebas unitarias.
- `mvn -B verify` ejecutado correctamente en backend con 86 pruebas unitarias y 6 pruebas de integracion.
- Flyway validado mediante Testcontainers contra PostgreSQL 17.10 hasta `v202607201400`.
- `mvn -B test` ejecutado correctamente en backend con 89 pruebas unitarias.
- `mvn -B verify` ejecutado correctamente en backend con 89 pruebas unitarias y 6 pruebas de integracion.
- `npm.cmd run build` ejecutado correctamente en frontend con TypeScript y Vite.

### Known Issues

- `npm.cmd ci` en carpeta OneDrive supero el tiempo de espera local; no dejo procesos Node activos y el build posterior fue exitoso. GitHub Actions valida `npm ci` en Linux.
- Los reportes usan permiso unico `commercial_reports:read`; la separacion fina por rol queda pendiente de decision funcional futura.

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
- Agregado Tenant Context backend con `TenantId`, `TenantContext`, `CurrentTenantProvider`, `KodaSecurityPrincipal` y filtro por request.
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
- Agregados errores `CASH_ITEM_NOT_FOUND`, `CASH_VERSION_CONFLICT` y `CASH_OPERATION_REJECTED`.
- Agregados errores `SALE_NOT_FOUND`, `SALE_REFERENCE_NOT_FOUND`, `SALE_VERSION_CONFLICT` y `SALE_OPERATION_REJECTED`.
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
