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
| 4. Seguridad JWT | Pendiente | Login, refresh tokens, usuarios, roles y permisos. |
| 5. Configuracion de empresa | Pendiente | Configuracion visual/regional por tenant. |
| 6. Catalogos ERP | Pendiente | Productos, marcas, categorias, unidades y presentaciones. |
| 7. Stock | Pendiente | Movimientos IN, OUT y ADJUSTMENT sin stock negativo por defecto. |
| 8. Auditoria | Pendiente | Registro persistente de operaciones sensibles. |
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
- Migraciones Flyway versionadas hasta `v202607171520`.
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
- Flyway: 7 migraciones validadas y schema actual en `v202607171520`.
- PostgreSQL seed: 25 tablas, 40 permisos, 7 roles y tenant KODA.
- Tenant Context tests: mvn test ejecutado correctamente con 9 tests, 0 fallos.
- Tenant Context runtime: jar backend actual validado contra PostgreSQL 17 con Actuator UP.

## Riesgo actual

La base tecnica local esta validada. Los riesgos abiertos son controlados:

- La instalacion completa de `node_modules` dentro de OneDrive puede superar el tiempo de espera; Docker build funciona correctamente y queda como camino confiable de validacion.
- El primer build Docker del backend tarda varios minutos porque Maven descarga dependencias dentro de la imagen builder.
- Mockito emite advertencia por carga dinamica de Java agent; debe revisarse antes de endurecer la matriz de Java futura.

## Siguiente paso tecnico

Avanzar al Hito 4: Seguridad JWT. El objetivo sera reemplazar la autenticacion temporal por login/JWT, construir el principal autenticado de KODA y alimentar el Tenant Context desde tokens emitidos por backend.

## Decision tecnica: PostgreSQL 17

Se usa `postgres:17-alpine` en desarrollo local porque Flyway en la version actual del stack declara soporte probado hasta PostgreSQL 17. PostgreSQL 18 se evaluara cuando Flyway lo soporte oficialmente en la linea utilizada por el proyecto.