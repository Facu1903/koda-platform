# GitHub Actions CI

## Objetivo

El pipeline minimo de CI valida que KODA PLATFORM compile y ejecute sus pruebas basicas antes de integrar cambios en `main`.

Este hito no reemplaza una estrategia completa de DevSecOps. Su objetivo es instalar una primera barrera automatica: si backend, migraciones o frontend se rompen, GitHub debe detectarlo temprano.

## Workflow

Archivo: `.github/workflows/ci.yml`.

Eventos:

- `push` sobre `main`.
- `pull_request` hacia `main`.

Controles base:

- Permiso GitHub minimo: `contents: read`.
- Concurrencia por referencia, cancelando ejecuciones viejas del mismo branch.
- Timeouts por job para evitar ejecuciones colgadas.

## Backend

Job: `Backend verify`.

Stack validado:

- Ubuntu latest.
- Java 21 con Temurin.
- Maven cacheado por `backend/pom.xml`.
- Variables JWT dummy solo para CI.

Comando ejecutado:

```bash
mvn -B verify
```

La fase `verify` ejecuta:

- Pruebas unitarias con Surefire.
- Empaquetado del backend.
- Pruebas de integracion con Failsafe.
- Testcontainers con PostgreSQL 17.
- Migraciones Flyway reales hasta `v202607171550`.

## Frontend

Job: `Frontend build`.

Stack validado:

- Ubuntu latest.
- Node.js 24.
- npm cacheado por `frontend/package-lock.json`.

Comandos ejecutados:

```bash
npm ci
npm run build
```

`npm ci` se usa para instalaciones reproducibles desde lockfile. No debe reemplazarse por `npm install` en CI.

## Prueba de persistencia PostgreSQL

Archivo: `backend/src/test/java/com/koda/platform/persistence/FlywayPostgresqlIT.java`.

La prueba levanta PostgreSQL 17 con Testcontainers y valida:

- Que Flyway aplique todas las migraciones actuales.
- Que la version final sea `202607171550`.
- Que existan datos semilla aprobados: tenant KODA, modulos base, permisos, roles y deposito `PRINCIPAL`.
- Que las tablas tenant-scoped mantengan columna `tenant_id`.
- Que el ledger de stock conserve columnas de trazabilidad: saldo anterior, saldo posterior y delta.

## Validacion local registrada

El 2026-07-17 se valido localmente:

- Backend: `mvn -B verify` exitoso con 47 pruebas unitarias y 3 pruebas de integracion.
- PostgreSQL Testcontainers: PostgreSQL 17.10, Flyway aplicado hasta `v202607171550`.
- Frontend: `npm.cmd run build` exitoso con TypeScript y Vite.

Observacion local: `npm.cmd ci` en carpeta OneDrive supero el tiempo de espera local, aunque no dejo procesos Node activos y el build posterior fue exitoso. La validacion limpia de `npm ci` queda cubierta por GitHub Actions en entorno Linux.

## Criterio de avance

A partir de este hito, ningun cambio relevante deberia considerarse cerrado si rompe:

- `mvn -B verify` en backend.
- `npm ci` y `npm run build` en frontend.
- Migraciones Flyway sobre PostgreSQL 17.