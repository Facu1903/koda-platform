# Sprint 1 - Execution Plan

## Estado

En progreso.

## Objetivo del Sprint 1

Construir el primer incremento ejecutable de KODA PLATFORM y KODA ERP con seguridad, multi-tenancy, configuracion inicial, catalogos, stock, auditoria y tests.

## Hitos internos

| Hito | Estado | Resultado esperado |
| --- | --- | --- |
| 1. Scaffolding tecnico | Completado | Backend, frontend, Docker Compose y estructura modular base. |
| 2. PostgreSQL y Flyway | Completado | Base local, migraciones iniciales y convenciones de datos. |
| 3. Tenant context | Completado | Resolucion segura de tenant desde contexto autenticado. |
| 4. Seguridad JWT | Completado | Login, refresh tokens, usuarios, roles y permisos. |
| 5. Configuracion de empresa | Completado | Configuracion visual/regional por tenant. |
| 6. Catalogos ERP | Completado | Productos, marcas, categorias, unidades y presentaciones. |
| 7. Stock | Completado | Movimientos IN, OUT y ADJUSTMENT sin stock negativo por defecto. |
| 8. Auditoria | Completado | Consulta segura de eventos auditables por tenant. |
| 9. Tests y hardening | Pendiente | Cobertura minima y validacion de aislamiento multiempresa. |

## Hito 1 - Scaffolding tecnico

### Incluye

- Backend Spring Boot 3.5.16 con Java 21.
- Frontend React, TypeScript 6.0.3, Vite y Material UI.
- Docker Compose con PostgreSQL 17, backend y frontend.
- Estructura inicial por capas y modulos.
- Configuracion base de seguridad, validacion, errores, logs estructurados y Actuator.
- App shell inicial para KODA ERP.

### No incluye

- Reglas de negocio finales.
- Entidades de tenant, usuarios, productos o stock.
- JWT productivo.
- CRUDs funcionales.
- Persistencia de auditoria.

## Hito 2 - PostgreSQL y Flyway

### Incluye

- PostgreSQL 17 como version objetivo local.
- Extensiones `pgcrypto` y `citext`.
- Migraciones Flyway versionadas hasta `v202607171550`.
- Tablas base para tenants, configuracion de empresa, productos/modulos de plataforma, entitlements, sucursales y depositos.
- Tablas base para usuarios globales, membresias tenant, roles, permisos, asignaciones y refresh tokens.
- Tablas base para marcas, categorias, unidades, presentaciones, productos, saldos de stock, movimientos de stock y auditoria persistente.
- Seed minimo aprobado: tenant KODA, KODA ERP, modulos base de Sprint 1, roles iniciales y permisos iniciales.
- Convenciones PostgreSQL documentadas en `docs/database/POSTGRESQL_CONVENTIONS.md`.

### No incluye

- Usuarios reales ni credenciales iniciales.
- Matriz rol-permiso.
- JWT productivo.
- Row Level Security.
- CRUDs funcionales.
- Reglas de aplicacion para stock o permisos.

## Hito 3 - Tenant Context

### Incluye

- `TenantId` como value object de dominio compartido.
- `TenantContext` como contexto de request con tenant, usuario, roles y permisos.
- `CurrentTenantProvider` como puerto para casos de uso.
- `TenantContextHolder` por thread/request en infraestructura.
- `TenantAwarePrincipal` y `KodaAuthenticatedPrincipal` como contrato para autenticacion KODA futura.
- `TenantContextAuthenticationFilter` conectado a Spring Security.
- Bloqueo de rutas tenant-scoped cuando existe autenticacion pero no existe contexto tenant compatible, manteniendo rutas de auth y plataforma como tenant-neutrales.
- Error estructurado `TENANT_CONTEXT_REQUIRED` para operaciones que exijan tenant.
- Documentacion en `docs/security/TENANT_CONTEXT.md`.
- Tests unitarios para resolucion, bloqueo y limpieza de contexto.

### No incluye

- Login.
- JWT productivo.
- Refresh token funcional.
- Matriz rol-permiso.
- Repositorios tenant-scoped.
- Row Level Security PostgreSQL.


## Hito 4 - Seguridad JWT

### Incluye

- Seguridad backend stateless con OAuth2 Resource Server y JWT HS256.
- Endpoints `/api/v1/auth/login`, `/api/v1/auth/refresh` y `/api/v1/auth/logout`.
- `AuthService` como capa de aplicacion para login, refresh y logout.
- `AuthRepository` como puerto y `JdbcAuthRepository` como adaptador PostgreSQL.
- Refresh tokens opacos, aleatorios, persistidos como hash SHA-256 y rotados en cada refresh.
- Access tokens con claims de usuario, tenant, roles, permisos y marca `platform_admin`.
- Conversion de JWT a `KodaAuthenticatedPrincipal` para alimentar Tenant Context.
- Password hashing con BCrypt.
- Bootstrap inicial opt-in para owner de tenant, sin credenciales hardcodeadas.
- Documentacion en `docs/security/AUTHENTICATION.md`.
- Tests unitarios de login, rechazo de credenciales, seleccion de tenant, rotacion de refresh token y conversion JWT.

### No incluye

- Recuperacion de password.
- MFA.
- Bloqueo por intentos fallidos.
- Politica avanzada de sesiones por dispositivo.
- Rotacion de llaves/JWKS o firma asimetrica RS256.
- APIs administrativas de usuarios.
- Row Level Security PostgreSQL.

## Hito 5 - Configuracion de empresa

### Incluye

- Endpoints tenant-scoped `GET /api/v1/company/settings` y `PUT /api/v1/company/settings`.
- Resolucion de tenant exclusivamente desde `TenantContext`.
- Validacion de permisos `company_settings:read` y `company_settings:update`.
- DTOs de request/response sin exponer entidades.
- Configuracion visual: logo, favicon, imagen de login, colores y tema.
- Configuracion regional: locale, moneda, zona horaria, formato de fecha, formato de hora, locale numerico y formato de moneda.
- Control optimista por `version`.
- Auditoria persistente de cambios con campos modificados.
- Documentacion en `docs/configuration/COMPANY_SETTINGS.md`.
- Tests unitarios de servicio.

### No incluye

- Upload/storage/CDN de assets.
- UI de configuracion.
- Cambio de nombre comercial, razon social o pais.
- Matriz rol-permiso aprobada para roles iniciales.
- Preferencias por usuario individual.


## Hito 6 - Catalogos ERP

### Incluye

- CRUD backend tenant-scoped para marcas, categorias, unidades de medida, presentaciones y productos.
- Endpoints bajo `/api/v1/catalog`.
- Marca opcional en productos.
- Categorias planas en Sprint 1.
- Un producto tiene una presentacion principal.
- SKU unico por tenant.
- Desactivacion de productos permitida con stock.
- Eliminacion por soft delete.
- Matriz rol-permiso aprobada para catalogos aplicada por Flyway.
- Validacion de referencias activas del mismo tenant.
- Auditoria persistente de creacion, actualizacion y eliminacion.
- Documentacion en `docs/catalogs/ERP_CATALOGS.md`.
- Tests unitarios de servicio.

### No incluye

- Busqueda avanzada, paginacion o filtros complejos.
- Jerarquia de categorias.
- Multiples presentaciones por producto con reglas comerciales avanzadas.
- Precios, impuestos o imagenes de producto.
- Validacion de stock para eliminar/desactivar.
- UI de catalogos.

## Hito 7 - Stock

### Incluye

- Endpoints tenant-scoped bajo `/api/v1/stock` para saldos y movimientos.
- Movimientos `IN`, `OUT` y `ADJUSTMENT`.
- `ADJUSTMENT` como conteo real que fija el saldo resultante.
- Ledger inmutable en `stock_movements` con `quantity_before`, `quantity_after` y `quantity_delta`.
- Saldo materializado en `stock_balances`.
- Bloqueo transaccional del saldo antes de confirmar movimientos.
- Regla de no stock negativo.
- Bloqueo de movimientos que dejen reservas sin cobertura.
- Validacion de deposito activo del mismo tenant.
- Validacion de producto activo, tipo `GOOD` y con seguimiento de stock.
- Matriz rol-permiso aprobada para stock aplicada por Flyway.
- Seed minimo de sucursal `CENTRAL` y deposito `PRINCIPAL` para KODA.
- Auditoria persistente de creacion de movimientos.
- Documentacion en `docs/stock/STOCK_MOVEMENTS.md`.
- Tests unitarios de servicio.

### No incluye

- Transferencias entre depositos.
- Reservas de stock.
- Lotes o vencimientos.
- Costos promedio o valorizacion contable.
- UI de stock.
- CRUD de sucursales/depositos.

## Hito 8 - Auditoria

### Incluye

- Endpoint tenant-scoped `GET /api/v1/audit/events`.
- Resolucion de tenant exclusivamente desde `TenantContext`.
- Validacion de permiso `audit:read`.
- Filtros por `actorUserId`, `resourceType`, `resourceId`, `action`, `outcome`, `from`, `to` y `limit`.
- Orden por eventos mas recientes primero.
- Limite maximo de 500 eventos por request.
- Matriz rol-permiso aprobada para auditoria aplicada por Flyway.
- Consulta read-only sin endpoints de creacion, edicion o eliminacion.
- Documentacion en `docs/audit/AUDIT_EVENTS.md`.
- Tests unitarios de servicio.

### No incluye

- Auditoria global de plataforma para `PLATFORM_SUPER_ADMIN`.
- Exportacion de auditoria.
- Retencion, archivado o particionamiento por volumen.
- UI de auditoria.
- Busqueda full-text o analitica avanzada.

## Herramientas locales detectadas

- Git: disponible.
- Docker CLI: disponible.
- Node/npm: disponible usando `npm.cmd` en PowerShell.
- Java 21: instalado y verificado con JDK 21.0.10.
- Maven: instalado y verificado con Apache Maven 3.9.16.
- Docker Desktop daemon: validado con `docker compose up -d`.
- Frontend dependencies: `package-lock.json` generado con `npm install --package-lock-only`; instalacion completa de `node_modules` dentro de OneDrive supero el tiempo de espera.
- Backend tests: `mvn test` ejecutado correctamente con Java 21.0.10 y Maven 3.9.16.
- Docker stack: PostgreSQL 17.10 healthy, backend Actuator `UP` y frontend HTTP 200.
- Flyway: 10 migraciones validadas y schema actual en `v202607171550`.
- PostgreSQL seed: 25 tablas, 41 permisos, 7 roles, 73 asignaciones rol-permiso, tenant KODA y deposito piloto `PRINCIPAL`.
- Tenant Context tests: mvn test ejecutado correctamente con 9 tests, 0 fallos.
- Tenant Context runtime: jar backend actual validado contra PostgreSQL 17 con Actuator UP.
- Seguridad JWT: `mvn test` ejecutado correctamente con 14 tests, 0 fallos.
- Seguridad JWT runtime: jar backend actual validado contra PostgreSQL 17 con Actuator `UP` y error estructurado de auth `400`.
- Configuracion de empresa: `mvn test` ejecutado correctamente con 19 tests, 0 fallos.
- Configuracion de empresa runtime: jar backend actual validado contra PostgreSQL 17 con Actuator `UP` y endpoint tenant-scoped protegido con `401` sin autenticacion.
- Catalogos ERP: `mvn test` ejecutado correctamente con 25 tests, 0 fallos.
- Catalogos ERP runtime: jar backend actual validado contra PostgreSQL 17 con Actuator `UP`, Flyway `v202607171530`, endpoint tenant-scoped protegido con `401` sin autenticacion y matriz rol-permiso aplicada.
- Stock: `mvn test` ejecutado correctamente con 32 tests, 0 fallos.
- Stock runtime: jar backend actual validado contra PostgreSQL 17 con Actuator `UP`, Flyway `v202607171540`, endpoint tenant-scoped protegido con `401` sin autenticacion, 41 permisos, 70 asignaciones rol-permiso y deposito piloto `PRINCIPAL`.
- Auditoria: `mvn test` ejecutado correctamente con 38 tests, 0 fallos.
- Auditoria runtime: jar backend actual validado contra PostgreSQL 17 con Actuator `UP`, Flyway `v202607171550`, endpoint tenant-scoped protegido con `401` sin autenticacion y 73 asignaciones rol-permiso.
- Auditoria Docker: imagen backend reconstruida, contenedor reiniciado y validado en `http://localhost:8080` con Actuator `UP`, Flyway `v202607171550` y endpoint audit events protegido con `401` sin autenticacion.

## Riesgo actual

La base tecnica local esta validada. Los riesgos abiertos son controlados:

- La instalacion completa de `node_modules` dentro de OneDrive puede superar el tiempo de espera; Docker build funciona correctamente y queda como camino confiable de validacion.
- El primer build Docker del backend tarda varios minutos porque Maven descarga dependencias dentro de la imagen builder.
- Mockito emite advertencia por carga dinamica de Java agent; debe revisarse antes de endurecer la matriz de Java futura.
- `KODA_JWT_SECRET` es obligatorio para ejecutar backend real o Docker Compose; esto es una proteccion deliberada, no una incomodidad accidental.
- HS256 es suficiente para este hito; antes de multi-nodo productivo debe evaluarse rotacion de llaves y firma asimetrica/JWKS.
- Las matrices rol-permiso de catalogos, stock y auditoria fueron aprobadas y aplicadas. Siguen pendientes matrices finas para seguridad y configuracion de empresa.

## Siguiente paso tecnico

Avanzar al Hito 9: Tests y hardening. El objetivo sera endurecer aislamiento multiempresa, ampliar regresiones criticas y revisar riesgos tecnicos antes de cerrar Sprint 1.

## Decision tecnica: PostgreSQL 17

Se usa `postgres:17-alpine` en desarrollo local porque Flyway en la version actual del stack declara soporte probado hasta PostgreSQL 17. PostgreSQL 18 se evaluara cuando Flyway lo soporte oficialmente en la linea utilizada por el proyecto.
