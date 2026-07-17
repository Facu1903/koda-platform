# KODA PLATFORM

KODA PLATFORM es una plataforma SaaS profesional de gestion empresarial, multiempresa, modular y preparada para evolucionar durante la proxima decada.

El primer producto sera KODA ERP, usado inicialmente por KODA como cliente piloto, pero disenado desde el dia cero como producto comercial para multiples empresas.

## Estado actual

Sprint 1 cerrado. Sprint 2 en planificacion.

Sprint 1 dejo una base tecnica ejecutable con backend, frontend, PostgreSQL 17, migraciones Flyway, seed minimo aprobado, Tenant Context backend, autenticacion JWT con refresh tokens, API tenant-scoped de configuracion de empresa, CRUD backend de catalogos ERP, API tenant-scoped de stock, consulta controlada de eventos de auditoria y hardening tecnico de arquitectura, JWT y aislamiento multiempresa. La base ya camina; ahora hay que evitar que corra en ojotas.

Sprint 2 esta en progreso. La base funcional minima de operaciones comerciales fue aprobada por el Product Owner el 2026-07-17 y queda documentada en `docs/sprints/SPRINT_2_FUNCTIONAL_BASELINE.md`. El Hito 2 agrego CI/CD minimo en GitHub Actions y pruebas de persistencia con PostgreSQL 17 real mediante Testcontainers.

## Documentos principales

- [Project Blueprint](PROJECT_BLUEPRINT.md)
- [Architecture](ARCHITECTURE.md)
- [Coding Standards](CODING_STANDARDS.md)
- [Roadmap](ROADMAP.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Agents](AGENTS.md)
- [Sprint 1 Functional Baseline](docs/sprints/SPRINT_1_FUNCTIONAL_BASELINE.md)
- [Sprint 1 Execution Plan](docs/sprints/SPRINT_1_EXECUTION_PLAN.md)
- [Sprint 1 Hardening Report](docs/sprints/SPRINT_1_HARDENING_REPORT.md)
- [Sprint 1 Closure Report](docs/sprints/SPRINT_1_CLOSURE_REPORT.md)
- [Sprint 2 Execution Plan](docs/sprints/SPRINT_2_EXECUTION_PLAN.md)
- [Sprint 2 Functional Baseline](docs/sprints/SPRINT_2_FUNCTIONAL_BASELINE.md)
- [GitHub Actions CI](docs/ci/GITHUB_ACTIONS.md)
- [PostgreSQL Conventions](docs/database/POSTGRESQL_CONVENTIONS.md)
- [Tenant Context](docs/security/TENANT_CONTEXT.md)
- [Authentication](docs/security/AUTHENTICATION.md)
- [Company Settings](docs/configuration/COMPANY_SETTINGS.md)
- [ERP Catalogs](docs/catalogs/ERP_CATALOGS.md)
- [Stock Movements](docs/stock/STOCK_MOVEMENTS.md)
- [Audit Events](docs/audit/AUDIT_EVENTS.md)

## Stack obligatorio

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- Flyway
- Maven

### Base de datos

- PostgreSQL 17

### Frontend

- React
- TypeScript
- Vite
- Material UI

### Infraestructura

- Docker
- Docker Compose

### Versionado

- Git
- GitHub

## Principios

- Plataforma antes que ERP aislado.
- Multiempresa real desde el inicio.
- Modularidad para productos actuales y futuros.
- Seguridad por defecto.
- Testing desde el primer sprint de codigo.
- DTOs, no entidades expuestas.
- Auditoria y logs estructurados.
- Personalizacion por configuracion, no por forks.

## Sprint 1 previsto

Sprint 1 construira la primera base funcional:

- Autenticacion.
- Usuarios, roles y permisos.
- Empresas/Tenants.
- Sucursales.
- Configuracion de empresa.
- Productos, marcas, categorias, presentaciones y unidades.
- CRUD completo.
- Movimientos de stock.
- Auditoria.
- Tests.

La base funcional minima de Sprint 1 fue aprobada el 2026-07-15 y esta documentada en `docs/sprints/SPRINT_1_FUNCTIONAL_BASELINE.md`.

## Estructura inicial

```text
backend/              API Spring Boot 3 + Clean Architecture
frontend/             Web app React + TypeScript + Vite + Material UI
docker/               Configuracion auxiliar de infraestructura local
docs/                 Documentacion tecnica, sprints y decisiones
docker-compose.yml    PostgreSQL 17, backend y frontend para desarrollo
.env.example          Variables locales de ejemplo
```

## Desarrollo local

### Requisitos

- Java 21.
- Maven.
- Node.js 24 o compatible.
- npm.
- Docker Desktop.

En PowerShell, si `npm` queda bloqueado por politica de scripts, usar `npm.cmd`.

### Backend

```powershell
cd backend
$env:KODA_JWT_SECRET="change-me-with-at-least-32-bytes-local-only"
mvn test
mvn spring-boot:run
```

### Frontend

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

### Docker Compose

```powershell
copy .env.example .env
# Editar .env y definir KODA_JWT_SECRET antes de levantar backend.
docker compose up --build
```

Servicios esperados:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`

## Base de datos inicial

Flyway deja el esquema en `v202607171550` con:

- Tenant piloto KODA.
- Producto `KODA_ERP`.
- Modulos base de Sprint 1.
- Roles y permisos iniciales aprobados.
- Tablas base de tenants, configuracion, sucursales, depositos, usuarios, RBAC, catalogos, stock y auditoria.

La matriz inicial rol-permiso ya existe para la base del Sprint 1 y se ampliara por modulo. La creacion de usuarios reales se controla por bootstrap opt-in o por APIs futuras de administracion.

## Tenant Context

El backend ya tiene una base tecnica para resolver tenant desde el principal autenticado de KODA:

- No se acepta `tenant_id` libre desde el frontend para operaciones tenant-scoped.
- Las rutas tenant-scoped quedan preparadas para exigir un principal con tenant; auth y plataforma quedan neutrales.
- El contexto se limpia al finalizar cada request.
- Los casos de uso podran depender de `CurrentTenantProvider` para obtener el tenant actual.

JWT, login y refresh tokens ya alimentan este Tenant Context desde el principal autenticado de KODA.

## Autenticacion JWT

El backend ya expone `/api/v1/auth/login`, `/api/v1/auth/refresh` y `/api/v1/auth/logout`.

- `KODA_JWT_SECRET` es obligatorio y debe tener al menos 32 bytes.
- El access token es JWT HS256 con TTL corto.
- El refresh token es opaco, se guarda hasheado y rota en cada refresh.
- El usuario inicial no se crea por defecto; puede activarse con variables `KODA_BOOTSTRAP_OWNER_*`.

Ver detalle en `docs/security/AUTHENTICATION.md`.


## Configuracion de empresa

El backend ya expone `/api/v1/company/settings` para consultar y actualizar configuracion visual/regional del tenant autenticado.

- El tenant se resuelve desde JWT/Tenant Context.
- La lectura requiere `company_settings:read`.
- La actualizacion requiere `company_settings:update`.
- Las actualizaciones usan version optimista y registran auditoria.

La matriz rol-permiso para habilitar estos permisos en roles iniciales requiere aprobacion funcional explicita.

Ver detalle en `docs/configuration/COMPANY_SETTINGS.md`.

## Catalogos ERP

El backend ya expone CRUD tenant-scoped bajo `/api/v1/catalog` para:

- marcas,
- categorias planas,
- unidades de medida,
- presentaciones,
- productos.

Reglas aprobadas: marca opcional, SKU unico por tenant, una presentacion principal por producto, desactivacion permitida con stock y eliminacion por soft delete. La matriz aprobada de catalogos fue aplicada por Flyway para KODA: `TENANT_OWNER`/`TENANT_ADMIN` CRUD completo, `MANAGER` lectura/actualizacion y `READ_ONLY` lectura.

Ver detalle en `docs/catalogs/ERP_CATALOGS.md`.

## Stock

El backend ya expone endpoints tenant-scoped bajo `/api/v1/stock` para:

- consulta de saldos por deposito/producto,
- consulta de movimientos confirmados,
- creacion de movimientos `IN`, `OUT` y `ADJUSTMENT`.

Reglas aprobadas: no stock negativo, movimientos confirmados inmutables, correcciones mediante nuevos movimientos, productos stockeables solo si son `GOOD`, activos y con seguimiento de stock. El ledger guarda saldo anterior, saldo posterior y delta para trazabilidad real. La matriz aprobada de stock fue aplicada por Flyway para KODA.

Ver detalle en `docs/stock/STOCK_MOVEMENTS.md`.

## Auditoria

El backend ya expone `/api/v1/audit/events` para consultar eventos auditables del tenant autenticado.

- La consulta es solo lectura.
- El tenant se resuelve desde JWT/Tenant Context.
- Requiere `audit:read`.
- Devuelve eventos recientes primero.
- Permite filtros por usuario actor, recurso, accion, resultado y rango de fechas.
- El limite maximo aprobado es 500 eventos por request.
- La auditoria global de plataforma queda fuera del Hito 8.

La matriz aprobada habilita lectura de auditoria para `TENANT_OWNER`, `TENANT_ADMIN` y `MANAGER`. `READ_ONLY`, `SALES_USER` y `STOCK_USER` no acceden a auditoria en Sprint 1.

Ver detalle en `docs/audit/AUDIT_EVENTS.md`.

## CI/CD

El repositorio ya incluye un pipeline minimo en `.github/workflows/ci.yml` para validar cambios en `main` y pull requests.

- Backend: `mvn -B verify`, incluyendo pruebas unitarias, empaquetado y pruebas de integracion.
- Persistencia: Testcontainers levanta PostgreSQL 17 y Flyway aplica todas las migraciones reales.
- Frontend: `npm ci` y `npm run build` con Node.js 24.

Ver detalle en `docs/ci/GITHUB_ACTIONS.md`.

## Tests y hardening

El backend ya incorpora hardening tecnico de cierre de Sprint 1:

- Reglas automatizadas de arquitectura con ArchUnit.
- Bloqueo de dependencias `application -> api/infrastructure`.
- Bloqueo de dependencias `api -> infrastructure`.
- Validacion de issuer JWT en el decoder.
- Puertos de aplicacion para emision de access tokens, refresh tokens y politica de tokens.
- Pruebas de aislamiento tenant en catalogos y stock.
- Suite backend actual: 47 tests unitarios y 3 tests de integracion, 0 fallos en `mvn -B verify`.

Ver detalle en `docs/sprints/SPRINT_1_HARDENING_REPORT.md`.

## Nota de entorno

Java 21 y Maven 3.9.16 fueron verificados para ejecutar tests backend. Docker Desktop fue validado con PostgreSQL 17, backend y frontend activos.
